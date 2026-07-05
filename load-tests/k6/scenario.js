/**
 * Нагрузочный тест ключевого пользовательского сценария Satellite Management System.
 *
 * СЦЕНАРИЙ: «Оператор Центра управления полётами разворачивает новую
 * спутниковую группировку и следит за её статусом»
 *
 *   1. POST   /api/constellations               — создать группировку (уникальную на VU+итерацию)
 *   2. POST   /api/add-satellites                — добавить 2 спутника (связь + съёмка)
 *   3. POST   /api/missions                      — активировать и выполнить миссию
 *   4. GET    /api/constellations/{name}/report  — проверить статус своей группировки
 *   5. GET    /api/satellites/active             — посмотреть общий список активных спутников
 *   6. DELETE /api/constellations/{name}/satellites/{sat} — вывести спутник из эксплуатации
 *   7. GET    /api/telemetry                     — проверить телеметрию перед завершением
 *
 * Каждая виртуальная итерация работает со своей уникальной группировкой
 * (имя строится из VU id + iteration + timestamp), поэтому параллельные
 * пользователи не конфликтуют друг с другом на уникальности имени
 * (API отвечает 422, если группировка с таким именем уже существует).
 *
 * Каждый HTTP-запрос явно помечен тегами iteration_id и step_name.
 * Это сделано намеренно, а не на системных тегах k6 (vu/iter), потому что
 * набор системных тегов, включаемых по умолчанию в JSON-вывод, зависит от
 * версии k6 и не документирован как стабильный — явные пользовательские
 * теги гарантированно попадают в каждую точку данных и дают надёжный ключ
 * для конвертера в Allure (см. load-tests/allure-adapter/convert.js),
 * который группирует точки по iteration_id в один allure-тесткейс,
 * а по step_name — в шаги внутри него.
 *
 * Запуск:
 *   k6 run --out json=results/raw-results.json load-tests/k6/scenario.js
 *
 * Переменные окружения (необязательные, есть дефолты):
 *   BASE_URL   — адрес сервиса, по умолчанию http://localhost:8080
 *   VUS_MAX    — пиковое число виртуальных пользователей, по умолчанию 30
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS_MAX = parseInt(__ENV.VUS_MAX || '30', 10);

// ─────────────────────────── Кастомные метрики ───────────────────────────
const createConstellationDuration = new Trend('step_1_create_constellation', true);
const addSatellitesDuration = new Trend('step_2_add_satellites', true);
const executeMissionDuration = new Trend('step_3_execute_mission', true);
const getReportDuration = new Trend('step_4_get_report', true);
const getActiveSatellitesDuration = new Trend('step_5_get_active_satellites', true);
const decommissionDuration = new Trend('step_6_decommission_satellite', true);
const getTelemetryDuration = new Trend('step_7_get_telemetry', true);

const businessErrors = new Counter('business_errors_total');
const scenarioSuccessRate = new Rate('scenario_success_rate');

// ─────────────────────────── Профиль нагрузки ───────────────────────────
// Разгон → удержание → спад. Удержание пиковой нагрузки — 60с ровно,
// как явно требует задание («не менее 60 секунд непрерывно»); разгон и
// спад добавляют время сверху, так что весь прогон длиннее 60с суммарно.
export const options = {
    scenarios: {
        satellite_operator_journey: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: VUS_MAX },                 // разгон
                { duration: '60s', target: VUS_MAX },                 // удержание пиковой нагрузки
                { duration: '15s', target: Math.ceil(VUS_MAX / 2) },  // частичный спад
                { duration: '10s', target: 0 },                       // плавное завершение
            ],
            gracefulRampDown: '5s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1500'],
        http_req_failed: ['rate<0.01'],
        scenario_success_rate: ['rate>0.95'],
    },
};

/** Уникальный идентификатор одной прогонки сценария (одного "тесткейса" для Allure). */
function uniqueIterationId() {
    return `VU${__VU}-Iter${__ITER}-${Date.now()}`;
}

/**
 * Обёртка над http.* с обязательными тегами iteration_id/step_name.
 * Все запросы сценария идут через неё — это единственное место, где
 * теги проставляются, что исключает риск забыть протегировать шаг.
 */
function taggedRequest(method, url, body, iterationId, stepName) {
    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { iteration_id: iterationId, step_name: stepName },
    };
    if (method === 'GET') return http.get(url, params);
    if (method === 'POST') return http.post(url, body, params);
    if (method === 'DELETE') return http.del(url, null, params);
    throw new Error(`Неподдерживаемый метод: ${method}`);
}

export default function () {
    const iterationId = uniqueIterationId();
    const constellationName = `LoadTest-${iterationId}`;
    const commSatName = `${constellationName}-COMM`;
    const imgSatName = `${constellationName}-IMG`;

    let scenarioOk = true;

    // ── Шаг 1: создать группировку ──────────────────────────────────────
    group('01_create_constellation', function () {
        const res = taggedRequest(
            'POST',
            `${BASE_URL}/api/constellations?name=${encodeURIComponent(constellationName)}`,
            null, iterationId, '01_create_constellation'
        );
        createConstellationDuration.add(res.timings.duration);
        const ok = check(res, {
            'создание группировки: статус 201': (r) => r.status === 201,
            'создание группировки: тело содержит id': (r) => {
                try { return JSON.parse(r.body).id !== undefined; } catch (e) { return false; }
            },
        });
        if (!ok) { businessErrors.add(1); scenarioOk = false; }
    });

    sleep(0.3);

    // ── Шаг 2: добавить спутники (связь + съёмка) ───────────────────────
    group('02_add_satellites', function () {
        const payload = JSON.stringify({
            constellationName: constellationName,
            satelliteParams: [
                { type: 'COMMUNICATION', name: commSatName, batteryLevel: 0.9, bandwidth: 150.0 },
                { type: 'IMAGE', name: imgSatName, batteryLevel: 0.85, resolution: 0.5 },
            ],
        });
        const res = taggedRequest(
            'POST', `${BASE_URL}/api/add-satellites`, payload, iterationId, '02_add_satellites'
        );
        addSatellitesDuration.add(res.timings.duration);
        const ok = check(res, {
            'добавление спутников: статус 200': (r) => r.status === 200,
            'добавление спутников: возвращены оба имени': (r) => {
                try {
                    const names = JSON.parse(r.body);
                    return names.includes(commSatName) && names.includes(imgSatName);
                } catch (e) { return false; }
            },
        });
        if (!ok) { businessErrors.add(1); scenarioOk = false; }
    });

    sleep(0.3);

    // ── Шаг 3: выполнить миссию (активация + отправка данных/снимки) ───
    group('03_execute_mission', function () {
        const payload = JSON.stringify({
            constellationName: constellationName,
            activateBeforeMission: true,
        });
        const res = taggedRequest(
            'POST', `${BASE_URL}/api/missions`, payload, iterationId, '03_execute_mission'
        );
        executeMissionDuration.add(res.timings.duration);
        const ok = check(res, {
            'выполнение миссии: статус 200': (r) => r.status === 200,
        });
        if (!ok) { businessErrors.add(1); scenarioOk = false; }
    });

    sleep(0.3);

    // ── Шаг 4: получить сводку по своей группировке ─────────────────────
    group('04_get_constellation_report', function () {
        const res = taggedRequest(
            'GET',
            `${BASE_URL}/api/constellations/${encodeURIComponent(constellationName)}/report`,
            null, iterationId, '04_get_constellation_report'
        );
        getReportDuration.add(res.timings.duration);
        const ok = check(res, {
            'отчёт по группировке: статус 200': (r) => r.status === 200,
            'отчёт по группировке: activeSatellites === 2': (r) => {
                try { return JSON.parse(r.body).activeSatellites === 2; } catch (e) { return false; }
            },
        });
        if (!ok) { businessErrors.add(1); scenarioOk = false; }
    });

    sleep(0.2);

    // ── Шаг 5: посмотреть все активные спутники в системе (дашборд) ────
    group('05_get_active_satellites', function () {
        const res = taggedRequest(
            'GET', `${BASE_URL}/api/satellites/active`, null, iterationId, '05_get_active_satellites'
        );
        getActiveSatellitesDuration.add(res.timings.duration);
        const ok = check(res, {
            'активные спутники: статус 200': (r) => r.status === 200,
            'активные спутники: ответ массив': (r) => {
                try { return Array.isArray(JSON.parse(r.body)); } catch (e) { return false; }
            },
        });
        if (!ok) { businessErrors.add(1); scenarioOk = false; }
    });

    sleep(0.2);

    // ── Шаг 6: вывести коммуникационный спутник из эксплуатации ────────
    group('06_decommission_satellite', function () {
        const res = taggedRequest(
            'DELETE',
            `${BASE_URL}/api/constellations/${encodeURIComponent(constellationName)}/satellites/${encodeURIComponent(commSatName)}`,
            null, iterationId, '06_decommission_satellite'
        );
        decommissionDuration.add(res.timings.duration);
        const ok = check(res, {
            'деактивация спутника: статус 200': (r) => r.status === 200,
        });
        if (!ok) { businessErrors.add(1); scenarioOk = false; }
    });

    sleep(0.2);

    // ── Шаг 7: проверить телеметрию перед завершением смены ────────────
    group('07_get_telemetry', function () {
        const res = taggedRequest(
            'GET', `${BASE_URL}/api/telemetry`, null, iterationId, '07_get_telemetry'
        );
        getTelemetryDuration.add(res.timings.duration);
        const ok = check(res, {
            'телеметрия: статус 200': (r) => r.status === 200,
        });
        if (!ok) { businessErrors.add(1); scenarioOk = false; }
    });

    scenarioSuccessRate.add(scenarioOk);

    sleep(1);
}

/**
 * Нативный HTML-отчёт k6 (без внешних зависимостей) — дублирует Allure
 * как быстрый способ посмотреть агрегированные цифры сразу после прогона,
 * без отдельного шага генерации.
 */
export function handleSummary(data) {
    return {
        'load-tests/results/summary.html': htmlReport(data),
        'load-tests/results/summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}

function htmlReport(data) {
    const metrics = data.metrics;
    const fmt = (n) => (n === undefined || n === null ? '—' : Number(n).toFixed(2));
    const row = (name, m) => {
        if (!m) return '';
        const v = m.values || {};
        return `<tr>
      <td>${name}</td>
      <td>${fmt(v.avg)}</td>
      <td>${fmt(v['p(95)'])}</td>
      <td>${fmt(v.max)}</td>
      <td>${v.count !== undefined ? v.count : '—'}</td>
    </tr>`;
    };

    const stepRows = [
        row('1. Создание группировки', metrics.step_1_create_constellation),
        row('2. Добавление спутников', metrics.step_2_add_satellites),
        row('3. Выполнение миссии', metrics.step_3_execute_mission),
        row('4. Отчёт по группировке', metrics.step_4_get_report),
        row('5. Активные спутники', metrics.step_5_get_active_satellites),
        row('6. Деактивация спутника', metrics.step_6_decommission_satellite),
        row('7. Телеметрия', metrics.step_7_get_telemetry),
    ].join('');

    const httpDuration = metrics.http_req_duration ? metrics.http_req_duration.values : {};
    const httpFailed = metrics.http_req_failed ? metrics.http_req_failed.values : {};
    const successRate = metrics.scenario_success_rate ? metrics.scenario_success_rate.values : {};
    const iterations = metrics.iterations ? metrics.iterations.values.count : '—';
    const vusMax = metrics.vus_max ? metrics.vus_max.values.max : '—';
    const businessErr = metrics.business_errors_total ? metrics.business_errors_total.values.count : 0;

    return `<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<title>k6 — Satellite Management System — отчёт нагрузочного теста</title>
<style>
  body { font-family: -apple-system, Segoe UI, Roboto, sans-serif; margin: 0; padding: 32px; background: #0f1115; color: #e6e6e6; }
  h1 { font-size: 22px; margin-bottom: 4px; }
  .sub { color: #9aa0a6; margin-bottom: 24px; }
  .cards { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 32px; }
  .card { background: #1a1d24; border: 1px solid #2a2e37; border-radius: 10px; padding: 16px 20px; min-width: 160px; }
  .card .label { color: #9aa0a6; font-size: 12px; text-transform: uppercase; letter-spacing: .04em; }
  .card .value { font-size: 26px; font-weight: 600; margin-top: 4px; }
  .ok { color: #4ade80; }
  .warn { color: #facc15; }
  .bad { color: #f87171; }
  table { width: 100%; border-collapse: collapse; margin-top: 8px; }
  th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #2a2e37; font-size: 14px; }
  th { color: #9aa0a6; font-weight: 500; text-transform: uppercase; font-size: 11px; letter-spacing: .04em; }
  h2 { font-size: 16px; margin-top: 36px; margin-bottom: 8px; color: #cfd3da; }
</style>
</head>
<body>
  <h1>Satellite Management System — нагрузочный тест</h1>
  <div class="sub">Сценарий: оператор ЦУП разворачивает группировку и следит за её статусом · сгенерировано ${new Date().toISOString()}</div>

  <div class="cards">
    <div class="card"><div class="label">Итераций сценария</div><div class="value">${iterations}</div></div>
    <div class="card"><div class="label">Пик виртуальных пользователей</div><div class="value">${vusMax}</div></div>
    <div class="card"><div class="label">p(95) длительности запроса, мс</div><div class="value">${fmt(httpDuration['p(95)'])}</div></div>
    <div class="card"><div class="label">Доля HTTP-ошибок</div><div class="value ${(httpFailed.rate || 0) < 0.01 ? 'ok' : 'bad'}">${fmt((httpFailed.rate || 0) * 100)}%</div></div>
    <div class="card"><div class="label">Успешность сценария</div><div class="value ${(successRate.rate || 0) > 0.95 ? 'ok' : 'warn'}">${fmt((successRate.rate || 0) * 100)}%</div></div>
    <div class="card"><div class="label">Бизнес-ошибок (check failed)</div><div class="value ${businessErr === 0 ? 'ok' : 'bad'}">${businessErr}</div></div>
  </div>

  <h2>Длительность по шагам сценария (мс)</h2>
  <table>
    <thead><tr><th>Шаг</th><th>avg</th><th>p95</th><th>max</th><th>вызовов</th></tr></thead>
    <tbody>${stepRows}</tbody>
  </table>

  <h2>Полные метрики HTTP</h2>
  <table>
    <thead><tr><th>Метрика</th><th>avg</th><th>p95</th><th>max</th><th>count</th></tr></thead>
    <tbody>${row('http_req_duration', metrics.http_req_duration)}${row('http_req_waiting', metrics.http_req_waiting)}${row('http_req_connecting', metrics.http_req_connecting)}</tbody>
  </table>
</body>
</html>`;
}

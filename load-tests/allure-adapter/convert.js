#!/usr/bin/env node
/**
 * Конвертер результатов k6 (--out json=...) в формат allure-results,
 * пригодный для генерации отчёта командой `allure generate`.
 *
 * ПОЧЕМУ ТАК УСТРОЕНО
 * --------------------
 * k6 не имеет собственной интеграции с Allure. Мы читаем построчный
 * (JSON Lines) файл, который k6 пишет через `--out json=path`: каждая
 * строка — это либо объявление метрики ({"type":"Metric",...}), либо
 * точка данных ({"type":"Point","metric":"...","data":{value,tags,time}}).
 *
 * Каждая точка данных типа http_req_duration несёт теги iteration_id и
 * step_name — они проставлены явно в load-tests/k6/scenario.js на каждый
 * http.* вызов (см. функцию taggedRequest в скрипте). Это осознанный
 * выбор: набор системных тегов k6 (vu/iter) включённых в JSON-вывод по
 * умолчанию не документирован как стабильный между версиями, поэтому
 * мы не полагаемся на него и используем собственные явные теги.
 *
 * Один iteration_id = один allure-тесткейс («одна отработка сценария
 * одним виртуальным пользователем»). Внутри него все точки с разными
 * step_name, отсортированные по времени, становятся allure-шагами.
 *
 * Статус шага определяется по HTTP-коду ответа (тег `status` на точке
 * http_req_duration) в сравнении с ожидаемым кодом для этого шага
 * (таблица EXPECTED_STATUS ниже) — это самый надёжный источник, так как
 * тег status на http_req_duration задокументирован и виден в каждом
 * официальном примере JSON-вывода k6, в отличие от внутреннего формата
 * отдельной метрики checks.
 *
 * Запуск:
 *   node load-tests/allure-adapter/convert.js results/raw-results.json allure-results
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const STEP_NAMES = {
    '01_create_constellation': 'Создать группировку',
    '02_add_satellites': 'Добавить спутники в группировку',
    '03_execute_mission': 'Выполнить миссию группировки',
    '04_get_constellation_report': 'Получить отчёт по группировке',
    '05_get_active_satellites': 'Получить список активных спутников',
    '06_decommission_satellite': 'Вывести спутник из эксплуатации',
    '07_get_telemetry': 'Проверить телеметрию',
};

// Ожидаемый HTTP-код для каждого шага сценария — используется, чтобы
// определить passed/failed шага без обращения к внутреннему формату
// метрики checks (см. пояснение в шапке файла).
const EXPECTED_STATUS = {
    '01_create_constellation': 201,
    '02_add_satellites': 200,
    '03_execute_mission': 200,
    '04_get_constellation_report': 200,
    '05_get_active_satellites': 200,
    '06_decommission_satellite': 200,
    '07_get_telemetry': 200,
};

const STEP_ORDER = Object.keys(STEP_NAMES);

function uuidv4() {
    return crypto.randomUUID();
}

function readJsonLines(filePath) {
    const raw = fs.readFileSync(filePath, 'utf8');
    const lines = raw.split('\n').filter((l) => l.trim().length > 0);
    const points = [];
    for (const line of lines) {
        let obj;
        try {
            obj = JSON.parse(line);
        } catch (e) {
            continue; // пропускаем повреждённые/неполные строки, если файл писался во время прогона
        }
        if (obj.type === 'Point' && obj.metric === 'http_req_duration') {
            points.push(obj);
        }
    }
    return points;
}

/**
 * Группирует точки по iteration_id, внутри — по step_name.
 * Возвращает Map<iteration_id, Map<step_name, point[]>>.
 */
function groupByIterationAndStep(points) {
    const iterations = new Map();
    for (const p of points) {
        const tags = p.data.tags || {};
        const iterationId = tags.iteration_id;
        const stepName = tags.step_name;
        if (!iterationId || !stepName) continue; // точки без наших тегов (не должно случаться, но на всякий случай)

        if (!iterations.has(iterationId)) iterations.set(iterationId, new Map());
        const steps = iterations.get(iterationId);
        if (!steps.has(stepName)) steps.set(stepName, []);
        steps.get(stepName).push(p);
    }
    return iterations;
}

function toUnixMs(isoTime) {
    return new Date(isoTime).getTime();
}

/**
 * Строит один allure-результат (один тесткейс) для одной итерации сценария.
 */
function buildAllureResult(iterationId, stepsMap) {
    const steps = [];
    let overallStatus = 'passed';
    let firstStart = null;
    let lastStop = null;
    let failureMessages = [];

    for (const stepKey of STEP_ORDER) {
        const points = stepsMap.get(stepKey);
        if (!points || points.length === 0) continue; // шаг не был вызван в этой итерации (не должно случаться при полном прохождении)

        // Если по какой-то причине шаг вызывался больше раза (retry) — берём последнюю точку.
        const point = points[points.length - 1];
        const tags = point.data.tags || {};
        const actualStatus = parseInt(tags.status, 10);
        const expectedStatus = EXPECTED_STATUS[stepKey];
        const stepPassed = actualStatus === expectedStatus;

        const start = toUnixMs(point.data.time);
        const stop = start + Math.round(point.data.value); // value = длительность запроса в мс

        if (firstStart === null || start < firstStart) firstStart = start;
        if (lastStop === null || stop > lastStop) lastStop = stop;

        const stepStatus = stepPassed ? 'passed' : 'failed';
        if (!stepPassed) {
            overallStatus = 'failed';
            failureMessages.push(
                `${STEP_NAMES[stepKey]}: ожидался HTTP ${expectedStatus}, получен ${actualStatus || 'нет ответа'}`
            );
        }

        steps.push({
            name: `${STEP_NAMES[stepKey]} (${tags.method || '?'} ${tags.url || ''})`,
            status: stepStatus,
            stage: 'finished',
            start,
            stop,
            parameters: [
                { name: 'HTTP статус', value: String(actualStatus || 'нет ответа') },
                { name: 'Длительность, мс', value: point.data.value.toFixed(1) },
            ],
        });
    }

    const result = {
        uuid: uuidv4(),
        historyId: crypto.createHash('md5').update('satellite_operator_journey').digest('hex'),
        fullName: 'k6.satellite-management.SatelliteOperatorJourney',
        name: `Сценарий оператора ЦУП — итерация ${iterationId}`,
        status: overallStatus,
        stage: 'finished',
        start: firstStart || Date.now(),
        stop: lastStop || Date.now(),
        labels: [
            { name: 'suite', value: 'Нагрузочное тестирование Satellite Management System' },
            { name: 'feature', value: 'Развёртывание и мониторинг спутниковой группировки' },
            { name: 'story', value: 'k6 load test' },
            { name: 'tag', value: 'load-test' },
            { name: 'host', value: 'k6' },
        ],
        steps,
        attachments: [],
        parameters: [{ name: 'iteration_id', value: iterationId }],
    };

    if (overallStatus === 'failed') {
        result.statusDetails = {
            message: failureMessages.join('; '),
            trace: failureMessages.join('\n'),
        };
    }

    return result;
}

function main() {
    const [, , inputPath, outputDir] = process.argv;

    if (!inputPath || !outputDir) {
        console.error('Использование: node convert.js <raw-k6-results.json> <allure-results-dir>');
        process.exit(1);
    }

    if (!fs.existsSync(inputPath)) {
        console.error(`Файл не найден: ${inputPath}`);
        console.error('Убедитесь, что k6 запускался с флагом --out json=<путь>');
        process.exit(1);
    }

    fs.mkdirSync(outputDir, { recursive: true });

    console.log(`Чтение точек данных из ${inputPath}...`);
    const points = readJsonLines(inputPath);
    console.log(`Найдено ${points.length} точек http_req_duration`);

    if (points.length === 0) {
        console.error('Не найдено ни одной точки http_req_duration с тегами iteration_id/step_name.');
        console.error('Проверьте, что load-tests/k6/scenario.js использует taggedRequest() для всех запросов.');
        process.exit(1);
    }

    const iterations = groupByIterationAndStep(points);
    console.log(`Сгруппировано в ${iterations.size} итераций сценария (allure-тесткейсов)`);

    let written = 0;
    let passedCount = 0;
    let failedCount = 0;

    for (const [iterationId, stepsMap] of iterations.entries()) {
        const result = buildAllureResult(iterationId, stepsMap);
        const filePath = path.join(outputDir, `${result.uuid}-result.json`);
        fs.writeFileSync(filePath, JSON.stringify(result, null, 2), 'utf8');
        written++;
        if (result.status === 'passed') passedCount++;
        else failedCount++;
    }

    // executor.json — опциональный файл, помогает Allure показать
    // информацию о том, чем и когда сгенерирован прогон.
    const executor = {
        name: 'k6',
        type: 'k6',
        buildName: `Satellite Management System — нагрузочный тест ${new Date().toISOString()}`,
    };
    fs.writeFileSync(
        path.join(outputDir, 'executor.json'),
        JSON.stringify(executor, null, 2),
        'utf8'
    );

    // environment.properties — отображается в Allure на вкладке Environment.
    const envLines = [
        `Tool=k6`,
        `Target=${process.env.BASE_URL || 'http://localhost:8080'}`,
        `Iterations=${written}`,
    ];
    fs.writeFileSync(
        path.join(outputDir, 'environment.properties'),
        envLines.join('\n'),
        'utf8'
    );

    console.log(`\nГотово: записано ${written} allure-результатов в ${outputDir}`);
    console.log(`  passed: ${passedCount}, failed: ${failedCount}`);
    console.log(`\nДалее: allure generate ${outputDir} --clean -o allure-report && allure open allure-report`);
}

main();

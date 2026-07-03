#!/usr/bin/env bash
#
# Полный цикл нагрузочного теста: запуск k6 → конвертация результатов
# в формат Allure → генерация HTML-отчёта.
#
# Использование:
#   ./load-tests/run.sh                       # дефолты: localhost:8080, 30 VU
#   BASE_URL=http://localhost:8080 VUS_MAX=50 ./load-tests/run.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

BASE_URL="${BASE_URL:-http://localhost:8080}"
VUS_MAX="${VUS_MAX:-30}"

RESULTS_DIR="load-tests/results"
ALLURE_RESULTS_DIR="load-tests/allure-results"
ALLURE_REPORT_DIR="load-tests/allure-report"
RAW_JSON="$RESULTS_DIR/raw-results.json"

echo "════════════════════════════════════════════════════════════"
echo " Нагрузочный тест Satellite Management System"
echo " BASE_URL=$BASE_URL   VUS_MAX=$VUS_MAX"
echo "════════════════════════════════════════════════════════════"

# ── Проверка доступности сервиса перед стартом ──────────────────────────
echo ""
echo "→ Проверка доступности $BASE_URL/api/constellations ..."
if ! curl -sf -o /dev/null "$BASE_URL/api/constellations"; then
    echo "✘ Сервис недоступен по адресу $BASE_URL"
    echo "  Убедитесь, что приложение запущено (см. README.md, раздел «Запуск проекта»)"
    exit 1
fi
echo "✔ Сервис отвечает"

# ── Проверка наличия k6 ──────────────────────────────────────────────────
if ! command -v k6 &> /dev/null; then
    echo "✘ k6 не найден в PATH."
    echo "  Установка: https://grafana.com/docs/k6/latest/set-up/install-k6/"
    exit 1
fi

# ── Очистка результатов предыдущего прогона ──────────────────────────────
rm -rf "$RESULTS_DIR" "$ALLURE_RESULTS_DIR" "$ALLURE_REPORT_DIR"
mkdir -p "$RESULTS_DIR" "$ALLURE_RESULTS_DIR"

# ── Шаг 1: запуск k6 ──────────────────────────────────────────────────────
echo ""
echo "→ Запуск k6 (разгон → 60с удержания пика → спад)..."
BASE_URL="$BASE_URL" VUS_MAX="$VUS_MAX" k6 run \
    --out json="$RAW_JSON" \
    load-tests/k6/scenario.js

echo ""
echo "✔ k6 завершил прогон. Нативный отчёт: $RESULTS_DIR/summary.html"

# ── Шаг 2: конвертация в Allure ───────────────────────────────────────────
echo ""
echo "→ Конвертация результатов в формат Allure..."
node load-tests/allure-adapter/convert.js "$RAW_JSON" "$ALLURE_RESULTS_DIR"

# ── Шаг 3: генерация Allure-отчёта ────────────────────────────────────────
echo ""
echo "→ Генерация Allure-отчёта..."
if command -v allure &> /dev/null; then
    allure generate "$ALLURE_RESULTS_DIR" --clean -o "$ALLURE_REPORT_DIR"
else
    npx --yes allure-commandline generate "$ALLURE_RESULTS_DIR" --clean -o "$ALLURE_REPORT_DIR"
fi

echo ""
echo "════════════════════════════════════════════════════════════"
echo " Готово!"
echo "════════════════════════════════════════════════════════════"
echo " Нативный HTML-отчёт k6:  $RESULTS_DIR/summary.html"
echo " Allure-отчёт:            $ALLURE_REPORT_DIR/index.html"
echo ""
echo " Открыть Allure-отчёт в браузере:"
if command -v allure &> /dev/null; then
    echo "   allure open $ALLURE_REPORT_DIR"
else
    echo "   npx allure-commandline open $ALLURE_REPORT_DIR"
fi
echo "════════════════════════════════════════════════════════════"

# Полный цикл нагрузочного теста: запуск k6 -> конвертация результатов
# в формат Allure -> генерация HTML-отчёта. Версия для Windows PowerShell.
#
# Использование:
#   .\load-tests\run.ps1
#   $env:VUS_MAX=50; .\load-tests\run.ps1
#
# Если PowerShell блокирует запуск непроверенных скриптов, один раз разрешите
# выполнение (в текущей сессии, без изменения политики системы навсегда):
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $ScriptDir "..")

$BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }
$VusMax  = if ($env:VUS_MAX)  { $env:VUS_MAX }  else { "30" }

$ResultsDir       = "load-tests\results"
$AllureResultsDir = "load-tests\allure-results"
$AllureReportDir  = "load-tests\allure-report"
$RawJson          = Join-Path $ResultsDir "raw-results.json"

Write-Host "════════════════════════════════════════════════════════════"
Write-Host " Нагрузочный тест Satellite Management System"
Write-Host " BASE_URL=$BaseUrl   VUS_MAX=$VusMax"
Write-Host "════════════════════════════════════════════════════════════"

# --- Проверка доступности сервиса перед стартом ---------------------------
Write-Host ""
Write-Host "-> Проверка доступности $BaseUrl/api/constellations ..."
try {
    $null = curl.exe -sf -o NUL "$BaseUrl/api/constellations"
    if ($LASTEXITCODE -ne 0) { throw "curl вернул код $LASTEXITCODE" }
} catch {
    Write-Host "X Сервис недоступен по адресу $BaseUrl"
    Write-Host "  Убедитесь, что приложение запущено (см. README.md, раздел «Запуск проекта»)"
    exit 1
}
Write-Host "OK Сервис отвечает"

# --- Проверка наличия k6 ----------------------------------------------------
if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
    Write-Host "X k6 не найден в PATH."
    Write-Host "  Установка: winget install k6 --source winget"
    Write-Host "  (или https://grafana.com/docs/k6/latest/set-up/install-k6/)"
    exit 1
}

# --- Проверка наличия Node.js ------------------------------------------------
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "X node не найден в PATH. Установите Node.js 16+: https://nodejs.org"
    exit 1
}

# --- Очистка результатов предыдущего прогона ---------------------------------
Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $ResultsDir, $AllureResultsDir, $AllureReportDir
New-Item -ItemType Directory -Force -Path $ResultsDir | Out-Null
New-Item -ItemType Directory -Force -Path $AllureResultsDir | Out-Null

# --- Шаг 1: запуск k6 --------------------------------------------------------
Write-Host ""
Write-Host "-> Запуск k6 (разгон -> 60с удержания пика -> спад)..."
$env:BASE_URL = $BaseUrl
$env:VUS_MAX = $VusMax
k6 run --out "json=$RawJson" "load-tests\k6\scenario.js"
if ($LASTEXITCODE -ne 0) {
    Write-Host "X k6 завершился с ошибкой (код $LASTEXITCODE). Смотрите вывод выше."
    exit 1
}

Write-Host ""
Write-Host "OK k6 завершил прогон. Нативный отчёт: $ResultsDir\summary.html"

# --- Шаг 2: конвертация в Allure ---------------------------------------------
Write-Host ""
Write-Host "-> Конвертация результатов в формат Allure..."
node "load-tests\allure-adapter\convert.js" $RawJson $AllureResultsDir

# --- Шаг 3: генерация Allure-отчёта ------------------------------------------
Write-Host ""
Write-Host "-> Генерация Allure-отчёта..."
if (Get-Command allure -ErrorAction SilentlyContinue) {
    allure generate $AllureResultsDir --clean -o $AllureReportDir
} else {
    npx --yes allure-commandline generate $AllureResultsDir --clean -o $AllureReportDir
}

Write-Host ""
Write-Host "════════════════════════════════════════════════════════════"
Write-Host " Готово!"
Write-Host "════════════════════════════════════════════════════════════"
Write-Host " Нативный HTML-отчёт k6:  $ResultsDir\summary.html"
Write-Host " Allure-отчёт:            $AllureReportDir\index.html"
Write-Host ""
Write-Host " Открыть Allure-отчёт в браузере:"
if (Get-Command allure -ErrorAction SilentlyContinue) {
    Write-Host "   allure open $AllureReportDir"
} else {
    Write-Host "   npx allure-commandline open $AllureReportDir"
}
Write-Host "════════════════════════════════════════════════════════════"

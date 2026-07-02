-- V2 — Добавление полей телеметрии температуры
--
-- Поля nullable: спутник может существовать в БД до того,
-- как от него придут данные телеметрии через gRPC-стрим.
ALTER TABLE satellites
    ADD COLUMN internal_temperature DOUBLE PRECISION,
    ADD COLUMN external_temperature DOUBLE PRECISION;

COMMENT ON COLUMN satellites.internal_temperature
    IS 'Внутренняя температура отсеков в °C (норма: 15–35)';

COMMENT ON COLUMN satellites.external_temperature
    IS 'Внешняя температура корпуса в °C (орбита: -150 до +120)';

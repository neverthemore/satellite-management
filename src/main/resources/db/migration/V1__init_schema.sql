-- ============================================================
-- V1 — Начальная схема спутниковой группировки
-- ============================================================
-- Ответ на вопрос: @Embedded vs @OneToOne
--
-- @Embedded использован для SatelliteState и EnergySystem, потому что:
--   1. Они не могут существовать независимо от спутника
--   2. Нет потребности обращаться к ним напрямую через отдельный репозиторий
--   3. Нет требований нормализации: их данные функционально зависят только
--      от первичного ключа спутника (то есть не нарушают 3NF при включении
--      в ту же таблицу)
--
-- @OneToOne был бы уместен если бы:
--   • EnergySystem имел свой жизненный цикл и менялся независимо
--   • Требовался отдельный API-эндпоинт для управления системой энергии
--   • Таблица satellites из-за числа embedded-полей стала бы слишком широкой
-- ============================================================

-- Таблица группировок спутников
CREATE TABLE satellite_constellations
(
    id                 BIGSERIAL    PRIMARY KEY,
    constellation_name VARCHAR(255) NOT NULL UNIQUE
);

-- Индекс для быстрого поиска по имени группировки.
-- Имена добавляются редко, читаются очень часто — индекс оправдан.
CREATE UNIQUE INDEX idx_constellation_name
    ON satellite_constellations (constellation_name);

-- Базовая таблица спутников (JOINED inheritance).
-- Каждый спутник в подтаблице ссылается на эту строку по id.
-- EnergySystem и SatelliteState хранятся здесь (@Embedded — одна таблица).
CREATE TABLE satellites
(
    id                    BIGSERIAL    PRIMARY KEY,
    name                  VARCHAR(255) NOT NULL,
    -- Дискриминатор для иерархии JPA (@DiscriminatorColumn)
    satellite_type        VARCHAR(31)  NOT NULL,
    constellation_id      BIGINT REFERENCES satellite_constellations (id) ON DELETE SET NULL,

    -- SatelliteState (@Embedded)
    is_active             BOOLEAN      NOT NULL DEFAULT FALSE,
    status_message        VARCHAR(255) NOT NULL DEFAULT 'Не активирован',

    -- EnergySystem (@Embedded)
    battery_level         DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    max_battery           DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    min_battery           DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    low_battery_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.2
);

-- Быстрое чтение активных спутников (частый фильтр: "сколько сейчас работает")
CREATE INDEX idx_satellite_is_active    ON satellites (is_active);
-- Быстрое получение спутников группировки (@OneToMany lazy-load)
CREATE INDEX idx_satellite_constellation ON satellites (constellation_id);

-- Таблица спутников связи (JOINED: только специфичные поля)
CREATE TABLE communication_satellites
(
    id        BIGINT          PRIMARY KEY REFERENCES satellites (id) ON DELETE CASCADE,
    bandwidth DOUBLE PRECISION NOT NULL
);

-- Таблица спутников ДЗЗ (JOINED: только специфичные поля)
CREATE TABLE imaging_satellites
(
    id           BIGINT           PRIMARY KEY REFERENCES satellites (id) ON DELETE CASCADE,
    resolution   DOUBLE PRECISION NOT NULL,
    photos_taken INTEGER          NOT NULL DEFAULT 0
);

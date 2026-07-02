package seminars.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Юнит-тесты EnergySystem: и бизнес-логики (consume/recharge/пороги),
 * и самого Builder'а — что он умеет собирать разные "варианты" конфигурации
 * и валидирует входные данные при build().
 */
@DisplayName("EnergySystem: юнит-тесты builder'а и бизнес-логики")
class EnergySystemTest {

    private static final double CUSTOM_BATTERY_LEVEL = 0.5;
    private static final double CUSTOM_MAX_BATTERY = 2.0;
    private static final double CUSTOM_MIN_BATTERY = 0.1;
    private static final double CUSTOM_LOW_BATTERY_THRESHOLD = 0.3;

    @Test
    @DisplayName("builder() без явного batteryLevel заряжает энергосистему по умолчанию до maxBattery (полный заряд)")
    void build_withoutExplicitBatteryLevel_defaultsToMaxBattery() {
        EnergySystem energySystem = EnergySystem.builder().build();

        assertEquals(EnergySystem.DEFAULT_MAX_BATTERY, energySystem.getBatteryLevel());
    }

    @Test
    @DisplayName("builder() с явно заданным batteryLevel устанавливает именно его, а не дефолт")
    void build_withExplicitBatteryLevel_usesProvidedValue() {
        EnergySystem energySystem = EnergySystem.builder()
                .batteryLevel(CUSTOM_BATTERY_LEVEL)
                .build();

        assertEquals(CUSTOM_BATTERY_LEVEL, energySystem.getBatteryLevel());
    }

    @Test
    @DisplayName("builder() позволяет собрать полностью кастомный вариант конфигурации со всеми границами")
    void build_withAllCustomBounds_appliesEveryProvidedValue() {
        EnergySystem energySystem = EnergySystem.builder()
                .batteryLevel(CUSTOM_BATTERY_LEVEL)
                .maxBattery(CUSTOM_MAX_BATTERY)
                .minBattery(CUSTOM_MIN_BATTERY)
                .lowBatteryThreshold(CUSTOM_LOW_BATTERY_THRESHOLD)
                .build();

        assertEquals(CUSTOM_BATTERY_LEVEL, energySystem.getBatteryLevel());
        assertEquals(CUSTOM_MAX_BATTERY, energySystem.getMaxBattery());
        assertEquals(CUSTOM_MIN_BATTERY, energySystem.getMinBattery());
        assertEquals(CUSTOM_LOW_BATTERY_THRESHOLD, energySystem.getLowBatteryThreshold());
    }

    @Test
    @DisplayName("build() выбрасывает исключение, если minBattery больше maxBattery")
    void build_minBatteryGreaterThanMaxBattery_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                EnergySystem.builder()
                        .minBattery(0.9)
                        .maxBattery(0.5)
                        .build());
    }

    @Test
    @DisplayName("build() выбрасывает исключение, если batteryLevel выходит за границы [minBattery, maxBattery]")
    void build_batteryLevelOutOfBounds_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                EnergySystem.builder()
                        .maxBattery(1.0)
                        .batteryLevel(1.5)
                        .build());
    }

    @Test
    @DisplayName("build() выбрасывает исключение, если lowBatteryThreshold выходит за границы [minBattery, maxBattery]")
    void build_lowBatteryThresholdOutOfBounds_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                EnergySystem.builder()
                        .maxBattery(1.0)
                        .lowBatteryThreshold(1.2)
                        .build());
    }

    @Test
    @DisplayName("consume() уменьшает заряд на запрошенную величину и возвращает true, если хватило энергии")
    void consume_enoughCharge_reducesLevelAndReturnsTrue() {
        EnergySystem energySystem = EnergySystem.builder().batteryLevel(0.8).build();

        boolean fullyConsumed = energySystem.consume(0.3);

        assertTrue(fullyConsumed);
        assertEquals(0.5, energySystem.getBatteryLevel(), 1e-9);
    }

    @Test
    @DisplayName("consume() не даёт уровню упасть ниже minBattery и возвращает false, если энергии не хватило")
    void consume_notEnoughCharge_clampsAtMinBatteryAndReturnsFalse() {
        EnergySystem energySystem = EnergySystem.builder().batteryLevel(0.1).build();

        boolean fullyConsumed = energySystem.consume(0.5);

        assertFalse(fullyConsumed);
        assertEquals(EnergySystem.DEFAULT_MIN_BATTERY, energySystem.getBatteryLevel());
    }

    @Test
    @DisplayName("consume() выбрасывает исключение при отрицательном количестве энергии")
    void consume_negativeAmount_throwsException() {
        EnergySystem energySystem = EnergySystem.builder().build();

        assertThrows(IllegalArgumentException.class, () -> energySystem.consume(-0.1));
    }

    @Test
    @DisplayName("recharge() увеличивает заряд и возвращает true, если не превышен maxBattery")
    void recharge_withinBounds_increasesLevelAndReturnsTrue() {
        EnergySystem energySystem = EnergySystem.builder()
                .maxBattery(1.0)
                .batteryLevel(0.4)
                .build();

        boolean fullyRecharged = energySystem.recharge(0.3);

        assertTrue(fullyRecharged);
        assertEquals(0.7, energySystem.getBatteryLevel(), 1e-9);
    }

    @Test
    @DisplayName("recharge() не даёт уровню превысить maxBattery и возвращает false, если упёрлись в потолок")
    void recharge_exceedsMaxBattery_clampsAtMaxBatteryAndReturnsFalse() {
        EnergySystem energySystem = EnergySystem.builder()
                .maxBattery(1.0)
                .batteryLevel(0.9)
                .build();

        boolean fullyRecharged = energySystem.recharge(0.5);

        assertFalse(fullyRecharged);
        assertEquals(1.0, energySystem.getBatteryLevel(), 1e-9);
    }

    @Test
    @DisplayName("hasSufficientPower() и isCritical() корректно отражают положение заряда относительно порога")
    void hasSufficientPowerAndIsCritical_reflectThresholdBoundary() {
        EnergySystem aboveThreshold = EnergySystem.builder()
                .lowBatteryThreshold(0.2)
                .batteryLevel(0.21)
                .build();
        EnergySystem atOrBelowThreshold = EnergySystem.builder()
                .lowBatteryThreshold(0.2)
                .batteryLevel(0.2)
                .build();

        assertTrue(aboveThreshold.hasSufficientPower());
        assertFalse(aboveThreshold.isCritical());

        assertFalse(atOrBelowThreshold.hasSufficientPower());
        assertTrue(atOrBelowThreshold.isCritical());
    }
}

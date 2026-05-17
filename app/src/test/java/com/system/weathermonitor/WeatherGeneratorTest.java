package com.system.weathermonitor;

import org.junit.After;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link WeatherGenerator} and {@link WeatherData}.
 *
 * Uses a seeded {@link Random} so results are deterministic.
 * Clears the session singleton before/after each test to avoid cross-test pollution.
 */
public class WeatherGeneratorTest {

    @After
    public void tearDown() {
        WeatherGenerator.clearSession();
    }

    // ── Temperature ────────────────────────────────────────────────────────

    @Test
    public void temperature_isInRange_30to40() {
        for (int seed = 0; seed < 200; seed++) {
            WeatherData d = WeatherGenerator.generate(new Random(seed));
            assertTrue("temp out of range for seed " + seed,
                    d.temperatureCelsius >= 30 && d.temperatureCelsius <= 40);
        }
    }

    // ── Condition ──────────────────────────────────────────────────────────

    @Test
    public void conditionType_isValidIndex() {
        for (int seed = 0; seed < 200; seed++) {
            WeatherData d = WeatherGenerator.generate(new Random(seed));
            assertTrue("conditionType out of range for seed " + seed,
                    d.conditionType >= 0
                            && d.conditionType < WeatherData.CONDITION_LABELS.length);
        }
    }

    @Test
    public void allSixConditions_areReachable() {
        boolean[] seen = new boolean[6];
        for (int seed = 0; seed < 2000; seed++) {
            WeatherData d = WeatherGenerator.generate(new Random(seed));
            seen[d.conditionType] = true;
        }
        for (int i = 0; i < 6; i++) {
            assertTrue("Condition " + i + " was never generated", seen[i]);
        }
    }

    // ── Secondary metrics ──────────────────────────────────────────────────

    @Test
    public void humidity_isInRange_0to100() {
        for (int seed = 0; seed < 200; seed++) {
            WeatherData d = WeatherGenerator.generate(new Random(seed));
            assertTrue("humidity out of range",
                    d.humidityPercent >= 0 && d.humidityPercent <= 100);
        }
    }

    @Test
    public void windSpeed_isNonNegative() {
        for (int seed = 0; seed < 200; seed++) {
            WeatherData d = WeatherGenerator.generate(new Random(seed));
            assertTrue("wind speed negative", d.windSpeedKmh >= 0);
        }
    }

    @Test
    public void uvIndex_isInRange_0to11() {
        for (int seed = 0; seed < 200; seed++) {
            WeatherData d = WeatherGenerator.generate(new Random(seed));
            assertTrue("UV index out of range",
                    d.uvIndex >= 0 && d.uvIndex <= 11);
        }
    }

    @Test
    public void windDirection_isValidCardinal() {
        String[] valid = {"N","NE","E","SE","S","SW","W","NW"};
        for (int seed = 0; seed < 200; seed++) {
            WeatherData d = WeatherGenerator.generate(new Random(seed));
            boolean found = false;
            for (String v : valid) {
                if (v.equals(d.windDirection)) { found = true; break; }
            }
            assertTrue("Invalid wind direction: " + d.windDirection, found);
        }
    }

    // ── Forecast ───────────────────────────────────────────────────────────

    @Test
    public void forecast_hasExactlyFiveDays() {
        WeatherData d = WeatherGenerator.generate(new Random(42));
        assertNotNull(d.forecast);
        assertEquals(5, d.forecast.length);
    }

    @Test
    public void forecast_highIsAlwaysAboveLow() {
        for (int seed = 0; seed < 200; seed++) {
            WeatherData d = WeatherGenerator.generate(new Random(seed));
            for (WeatherData.ForecastDay day : d.forecast) {
                assertTrue("Forecast high <= low for seed " + seed,
                        day.high > day.low);
            }
        }
    }

    @Test
    public void forecast_dayLabels_areNotEmpty() {
        WeatherData d = WeatherGenerator.generate(new Random(7));
        for (WeatherData.ForecastDay day : d.forecast) {
            assertNotNull(day.dayLabel);
            assertFalse(day.dayLabel.isEmpty());
        }
    }

    @Test
    public void forecast_conditionLabels_areNotEmpty() {
        WeatherData d = WeatherGenerator.generate(new Random(7));
        for (WeatherData.ForecastDay day : d.forecast) {
            assertNotNull(day.conditionLabel);
            assertFalse(day.conditionLabel.isEmpty());
        }
    }

    // ── Session singleton ──────────────────────────────────────────────────

    @Test
    public void getSession_returnsSameInstance() {
        WeatherData first  = WeatherGenerator.getSession();
        WeatherData second = WeatherGenerator.getSession();
        assertSame("getSession() must return the same instance", first, second);
    }

    @Test
    public void clearSession_allowsNewGeneration() {
        WeatherData first = WeatherGenerator.getSession();
        WeatherGenerator.clearSession();
        WeatherData second = WeatherGenerator.getSession();
        // After clearing, a new instance must be created (not the same reference)
        assertNotSame("clearSession() should allow a new instance", first, second);
    }

    // ── ConditionProfile ───────────────────────────────────────────────────

    @Test
    public void conditionProfile_metricsMatchProfile() {
        // For every seed, verify that generated metrics fall within the
        // profile bounds for the chosen condition.
        for (int seed = 0; seed < 100; seed++) {
            WeatherData d = WeatherGenerator.generate(new Random(seed));
            ConditionProfile p = ConditionProfile.forCondition(d.conditionType);

            assertTrue("humidity below profile min",
                    d.humidityPercent >= p.humidityMin);
            assertTrue("humidity above profile max",
                    d.humidityPercent <= p.humidityMax);

            assertTrue("wind below profile min",
                    d.windSpeedKmh >= p.windMin);
            assertTrue("wind above profile max",
                    d.windSpeedKmh <= p.windMax);

            assertTrue("UV below profile min",
                    d.uvIndex >= p.uvMin);
            assertTrue("UV above profile max",
                    d.uvIndex <= p.uvMax);

            assertTrue("pressure below profile min",
                    d.pressureHpa >= p.pressureMin);
            assertTrue("pressure above profile max",
                    d.pressureHpa <= p.pressureMax);

            assertTrue("visibility below profile min",
                    d.visibilityKm >= p.visibilityMin);
            assertTrue("visibility above profile max",
                    d.visibilityKm <= p.visibilityMax);
        }
    }
}

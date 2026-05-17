package com.system.weathermonitor;

import java.util.Calendar;
import java.util.Random;

/**
 * WeatherGenerator — session-scoped weather data factory.
 *
 * <h3>Session stability</h3>
 * {@link #getSession()} generates a {@link WeatherData} snapshot exactly once
 * per process lifetime.  Subsequent calls return the same cached instance, so
 * all UI components — main card, metrics strip, forecast row — always display
 * the same coherent data regardless of how many times they are re-bound.
 *
 * <h3>Realism</h3>
 * Each condition type has a dedicated {@link ConditionProfile} that constrains
 * every secondary metric to a realistic range.  For example, "Hot & Humid"
 * always produces high humidity and a positive feels-like offset, while
 * "Sunny" always produces high UV and low humidity.
 *
 * <h3>Randomisation</h3>
 * A single {@link Random} instance is seeded from {@link System#currentTimeMillis()}
 * so each app launch produces a different snapshot.
 */
public final class WeatherGenerator {

    // ── Singleton session cache ────────────────────────────────────────────
    private static volatile WeatherData sSession = null;

    private WeatherGenerator() { /* static utility — do not instantiate */ }

    /**
     * Returns the session-scoped {@link WeatherData} snapshot.
     * Thread-safe via double-checked locking; safe to call from any thread.
     */
    public static WeatherData getSession() {
        if (sSession == null) {
            synchronized (WeatherGenerator.class) {
                if (sSession == null) {
                    sSession = generate(new Random());
                }
            }
        }
        return sSession;
    }

    /**
     * Clears the cached session.  Useful for testing or if the host process
     * wants to simulate a "new day" without restarting.
     * Not called during normal app operation.
     */
    static void clearSession() {
        synchronized (WeatherGenerator.class) {
            sSession = null;
        }
    }

    // ── Internal generation ────────────────────────────────────────────────

    /**
     * Builds a fully-populated {@link WeatherData} from a {@link Random} source.
     * Package-private so unit tests can inject a seeded RNG for determinism.
     */
    static WeatherData generate(Random rng) {

        // ── Pick condition ─────────────────────────────────────────────────
        int conditionType = rng.nextInt(6);   // 0–5 inclusive
        ConditionProfile profile = ConditionProfile.forCondition(conditionType);

        // ── Temperature (30–40°C) ──────────────────────────────────────────
        int temp = 30 + rng.nextInt(11);

        // ── Feels-like (temp + profile offset) ────────────────────────────
        int offsetRange = profile.feelsLikeOffsetMax - profile.feelsLikeOffsetMin + 1;
        int feelsLike   = temp + profile.feelsLikeOffsetMin + rng.nextInt(offsetRange);

        // ── Secondary metrics from profile ranges ──────────────────────────
        int humidity   = randInRange(rng, profile.humidityMin,   profile.humidityMax);
        int wind       = randInRange(rng, profile.windMin,       profile.windMax);
        int uv         = randInRange(rng, profile.uvMin,         profile.uvMax);
        int pressure   = randInRange(rng, profile.pressureMin,   profile.pressureMax);
        int visibility = randInRange(rng, profile.visibilityMin, profile.visibilityMax);

        // ── Dew point — approximated from temp and humidity ────────────────
        // Magnus formula approximation: Td ≈ T - ((100 - RH) / 5)
        int dewPoint = temp - ((100 - humidity) / 5);

        // ── Wind direction ─────────────────────────────────────────────────
        String windDir = WIND_DIRECTIONS[rng.nextInt(WIND_DIRECTIONS.length)];

        // ── 5-day forecast ─────────────────────────────────────────────────
        Calendar cal = Calendar.getInstance();
        WeatherData.ForecastDay[] forecast = new WeatherData.ForecastDay[5];

        for (int i = 0; i < 5; i++) {
            cal.add(Calendar.DAY_OF_YEAR, 1);

            String dayLabel   = DAY_LABELS[cal.get(Calendar.DAY_OF_WEEK)];
            int    fCondition = rng.nextInt(6);
            ConditionProfile fp = ConditionProfile.forCondition(fCondition);

            int fHigh = 30 + rng.nextInt(11);                          // 30–40
            int fLow  = 24 + rng.nextInt(7);                           // 24–30
            int fHum  = randInRange(rng, fp.humidityMin, fp.humidityMax);

            forecast[i] = new WeatherData.ForecastDay(
                    dayLabel,
                    fp.iconType,
                    WeatherData.CONDITION_LABELS[fCondition],
                    fHigh,
                    fLow,
                    fHum
            );
        }

        return new WeatherData(
                temp,
                feelsLike,
                conditionType,
                profile.iconType,
                humidity,
                wind,
                windDir,
                uv,
                pressure,
                visibility,
                dewPoint,
                forecast
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Returns a random int in [min, max] inclusive. */
    private static int randInRange(Random rng, int min, int max) {
        if (min == max) return min;
        return min + rng.nextInt(max - min + 1);
    }

    /** Day-of-week labels; index matches {@link Calendar#DAY_OF_WEEK} (1-based, Sunday=1). */
    private static final String[] DAY_LABELS = {
            "", "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"
    };

    private static final String[] WIND_DIRECTIONS = {
            "N", "NE", "E", "SE", "S", "SW", "W", "NW"
    };
}

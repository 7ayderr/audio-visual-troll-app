package com.system.weathermonitor;

/**
 * ConditionProfile — per-condition meteorological range table.
 *
 * Each profile defines realistic min/max ranges for every secondary metric
 * so that generated values are internally consistent.  For example, a
 * "Hot & Humid" day has high humidity and a high feels-like offset, while
 * "Sunny" has low humidity and high UV.
 *
 * Usage:
 *   ConditionProfile p = ConditionProfile.forCondition(WeatherData.CONDITION_SUNNY);
 *   int humidity = p.humidityMin + rng.nextInt(p.humidityMax - p.humidityMin + 1);
 */
public final class ConditionProfile {

    // ── Humidity (%) ───────────────────────────────────────────────────────
    public final int humidityMin;
    public final int humidityMax;

    // ── Wind speed (km/h) ──────────────────────────────────────────────────
    public final int windMin;
    public final int windMax;

    // ── UV index ──────────────────────────────────────────────────────────
    public final int uvMin;
    public final int uvMax;

    // ── Pressure (hPa) ────────────────────────────────────────────────────
    public final int pressureMin;
    public final int pressureMax;

    // ── Visibility (km) ───────────────────────────────────────────────────
    public final int visibilityMin;
    public final int visibilityMax;

    // ── Feels-like offset from actual temp (°C, can be negative) ──────────
    public final int feelsLikeOffsetMin;
    public final int feelsLikeOffsetMax;

    // ── Icon type ─────────────────────────────────────────────────────────
    public final int iconType;

    private ConditionProfile(
            int humidityMin, int humidityMax,
            int windMin,     int windMax,
            int uvMin,       int uvMax,
            int pressureMin, int pressureMax,
            int visibilityMin, int visibilityMax,
            int feelsLikeOffsetMin, int feelsLikeOffsetMax,
            int iconType) {

        this.humidityMin        = humidityMin;
        this.humidityMax        = humidityMax;
        this.windMin            = windMin;
        this.windMax            = windMax;
        this.uvMin              = uvMin;
        this.uvMax              = uvMax;
        this.pressureMin        = pressureMin;
        this.pressureMax        = pressureMax;
        this.visibilityMin      = visibilityMin;
        this.visibilityMax      = visibilityMax;
        this.feelsLikeOffsetMin = feelsLikeOffsetMin;
        this.feelsLikeOffsetMax = feelsLikeOffsetMax;
        this.iconType           = iconType;
    }

    // ── Profile table (index == CONDITION_* constant) ─────────────────────

    private static final ConditionProfile[] PROFILES = {

        // 0 — SUNNY
        // Clear sky: low humidity, moderate wind, very high UV, high pressure,
        //            excellent visibility, feels close to actual temp.
        new ConditionProfile(
            /*humidity*/    25, 45,
            /*wind*/         8, 22,
            /*uv*/           9, 11,
            /*pressure*/  1010, 1022,
            /*visibility*/  18, 25,
            /*feelsOffset*/ -1,  2,
            WeatherData.ICON_SUN),

        // 1 — MOSTLY SUNNY
        // Predominantly clear: slightly higher humidity than pure sunny.
        new ConditionProfile(
            /*humidity*/    35, 55,
            /*wind*/         6, 20,
            /*uv*/           7, 10,
            /*pressure*/  1008, 1020,
            /*visibility*/  15, 25,
            /*feelsOffset*/ -1,  3,
            WeatherData.ICON_SUN),

        // 2 — PARTLY CLOUDY
        // Mix of sun and cloud: moderate humidity, lower UV.
        new ConditionProfile(
            /*humidity*/    45, 65,
            /*wind*/        10, 25,
            /*uv*/           5,  8,
            /*pressure*/  1005, 1016,
            /*visibility*/  12, 20,
            /*feelsOffset*/ -2,  2,
            WeatherData.ICON_SUN_CLOUD),

        // 3 — MOSTLY CLOUDY
        // Heavy cloud cover: higher humidity, reduced UV, lower pressure.
        new ConditionProfile(
            /*humidity*/    60, 80,
            /*wind*/        12, 30,
            /*uv*/           2,  5,
            /*pressure*/  1000, 1012,
            /*visibility*/   8, 15,
            /*feelsOffset*/ -3,  1,
            WeatherData.ICON_SUN_CLOUD),

        // 4 — HAZY SUNSHINE
        // Sun through haze: moderate humidity, reduced visibility, moderate UV.
        new ConditionProfile(
            /*humidity*/    50, 70,
            /*wind*/         4, 15,
            /*uv*/           5,  8,
            /*pressure*/  1006, 1018,
            /*visibility*/   5, 12,
            /*feelsOffset*/  0,  4,
            WeatherData.ICON_SUN),

        // 5 — HOT & HUMID
        // Oppressive heat: very high humidity, high feels-like offset, lower UV
        // due to moisture haze, lower pressure.
        new ConditionProfile(
            /*humidity*/    75, 95,
            /*wind*/         3, 14,
            /*uv*/           6,  9,
            /*pressure*/   998, 1010,
            /*visibility*/   6, 14,
            /*feelsOffset*/  3,  7,
            WeatherData.ICON_SUN_CLOUD)
    };

    /**
     * Returns the {@link ConditionProfile} for the given CONDITION_* constant.
     * Falls back to SUNNY profile for any out-of-range value.
     */
    public static ConditionProfile forCondition(int conditionType) {
        if (conditionType >= 0 && conditionType < PROFILES.length) {
            return PROFILES[conditionType];
        }
        return PROFILES[WeatherData.CONDITION_SUNNY];
    }
}

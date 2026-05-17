package com.system.weathermonitor;

/**
 * WeatherData — immutable snapshot of one app session's weather state.
 *
 * Generated once by {@link WeatherGenerator#getSession()} and held for the
 * lifetime of the process.  Every field is public-final; there are no setters.
 *
 * Conditions (CONDITION_*):
 *   0  Sunny
 *   1  Mostly Sunny
 *   2  Partly Cloudy
 *   3  Mostly Cloudy
 *   4  Hazy Sunshine
 *   5  Hot & Humid
 *
 * Icons (ICON_*):
 *   0  ic_sun          — clear sky
 *   1  ic_sun_cloud    — sun with cloud
 */
public final class WeatherData {

    // ── Condition constants ────────────────────────────────────────────────
    public static final int CONDITION_SUNNY         = 0;
    public static final int CONDITION_MOSTLY_SUNNY  = 1;
    public static final int CONDITION_PARTLY_CLOUDY = 2;
    public static final int CONDITION_MOSTLY_CLOUDY = 3;
    public static final int CONDITION_HAZY_SUNSHINE = 4;
    public static final int CONDITION_HOT_HUMID     = 5;

    /** Human-readable label for each CONDITION_* constant (index == constant). */
    public static final String[] CONDITION_LABELS = {
            "Sunny",
            "Mostly Sunny",
            "Partly Cloudy",
            "Mostly Cloudy",
            "Hazy Sunshine",
            "Hot & Humid"
    };

    // ── Icon constants ─────────────────────────────────────────────────────
    public static final int ICON_SUN       = 0;
    public static final int ICON_SUN_CLOUD = 1;

    // ══════════════════════════════════════════════════════════════════════
    // TODAY'S FIELDS
    // ══════════════════════════════════════════════════════════════════════

    /** Current temperature in °C (30–40). */
    public final int temperatureCelsius;

    /** Feels-like temperature in °C (may differ from actual by ±3). */
    public final int feelsLikeCelsius;

    /** One of the CONDITION_* constants. */
    public final int conditionType;

    /** One of the ICON_* constants — derived from conditionType. */
    public final int iconType;

    /** Relative humidity percentage (0–100). */
    public final int humidityPercent;

    /** Wind speed in km/h. */
    public final int windSpeedKmh;

    /** Cardinal wind direction: "N", "NE", "E", "SE", "S", "SW", "W", "NW". */
    public final String windDirection;

    /** UV index (0–11+). */
    public final int uvIndex;

    /** Atmospheric pressure in hPa. */
    public final int pressureHpa;

    /** Visibility in km. */
    public final int visibilityKm;

    /** Dew point in °C. */
    public final int dewPointCelsius;

    // ══════════════════════════════════════════════════════════════════════
    // FORECAST
    // ══════════════════════════════════════════════════════════════════════

    /** Five-day forecast; index 0 = tomorrow, index 4 = five days out. */
    public final ForecastDay[] forecast;

    // ── Constructor ────────────────────────────────────────────────────────
    public WeatherData(
            int temperatureCelsius,
            int feelsLikeCelsius,
            int conditionType,
            int iconType,
            int humidityPercent,
            int windSpeedKmh,
            String windDirection,
            int uvIndex,
            int pressureHpa,
            int visibilityKm,
            int dewPointCelsius,
            ForecastDay[] forecast) {

        this.temperatureCelsius = temperatureCelsius;
        this.feelsLikeCelsius   = feelsLikeCelsius;
        this.conditionType      = conditionType;
        this.iconType           = iconType;
        this.humidityPercent    = humidityPercent;
        this.windSpeedKmh       = windSpeedKmh;
        this.windDirection      = windDirection;
        this.uvIndex            = uvIndex;
        this.pressureHpa        = pressureHpa;
        this.visibilityKm       = visibilityKm;
        this.dewPointCelsius    = dewPointCelsius;
        this.forecast           = forecast;
    }

    // ══════════════════════════════════════════════════════════════════════
    // NESTED: ForecastDay
    // ══════════════════════════════════════════════════════════════════════

    /** Immutable value object for a single forecast day. */
    public static final class ForecastDay {

        /** Three-letter day abbreviation: "MON", "TUE", etc. */
        public final String dayLabel;

        /** One of the ICON_* constants. */
        public final int iconType;

        /** Condition label string (e.g. "Partly Cloudy"). */
        public final String conditionLabel;

        /** Forecast high temperature in °C. */
        public final int high;

        /** Forecast low temperature in °C. */
        public final int low;

        /** Forecast humidity percentage. */
        public final int humidityPercent;

        public ForecastDay(String dayLabel, int iconType, String conditionLabel,
                           int high, int low, int humidityPercent) {
            this.dayLabel        = dayLabel;
            this.iconType        = iconType;
            this.conditionLabel  = conditionLabel;
            this.high            = high;
            this.low             = low;
            this.humidityPercent = humidityPercent;
        }
    }
}

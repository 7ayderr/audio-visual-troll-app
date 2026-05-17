package com.system.weathermonitor;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.system.weathermonitor.databinding.ActivityMainBinding;
import com.system.weathermonitor.databinding.ItemMetricTileBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MainActivity — Weather Decoy UI (Phase 4)
 *
 * Binds the session-fixed {@link WeatherData} snapshot from
 * {@link WeatherGenerator#getSession()} to three UI zones:
 *
 *   1. Circular main card  — icon, temperature, date, condition
 *   2. Secondary metrics strip — humidity, feels-like, wind, UV, pressure, visibility
 *   3. 5-day forecast row  — day label, icon, high/low temps
 *
 * Also hosts the hidden access system:
 *   • Invisible tap zone bottom-left → shows {@link AccessPopup}
 *   • Popup letter buttons → close app
 *   • Popup hidden bottom-right zone → opens {@link ControlPanelActivity}
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Dialog accessDialog;
    private ExecutionEngine executionEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive — hide status + nav bars for dashboard look
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //noinspection deprecation
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        executionEngine = WeatherMonitorApp.from(getApplication()).getExecutionEngine();
        executionEngine.bindHost(
                this,
                binding.rootLayout,
                binding.effectOverlayHost);

        // Retrieve (or generate) the session-fixed snapshot
        WeatherData data = WeatherGenerator.getSession();

        bindMainCard(data);
        bindMetricsStrip(data);
        setupForecast(data);
        setupHiddenAccess();
    }

    // ══════════════════════════════════════════════════════════════════════
    // HIDDEN ACCESS SYSTEM
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Wires the invisible bottom-left tap zone to the {@link AccessPopup}.
     * No visual feedback — the zone is fully transparent.
     */
    private void setupHiddenAccess() {
        binding.hiddenTapZone.setOnClickListener(v -> showAccessPopup());
    }

    private void showAccessPopup() {
        // Dismiss any existing instance before creating a new one
        if (accessDialog != null && accessDialog.isShowing()) {
            accessDialog.dismiss();
        }

        accessDialog = AccessPopup.create(this, () -> {
            // Hidden zone tapped inside popup → open Control Panel
            Intent intent = new Intent(this, ControlPanelActivity.class);
            startActivity(intent);
        });

        accessDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Dismiss popup if activity goes to background (e.g. ControlPanel launched)
        if (accessDialog != null && accessDialog.isShowing()) {
            accessDialog.dismiss();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. CIRCULAR MAIN CARD
    // ══════════════════════════════════════════════════════════════════════

    private void bindMainCard(WeatherData data) {
        binding.ivWeatherIcon.setImageResource(iconRes(data.iconType));
        binding.tvTemperature.setText(data.temperatureCelsius + "°C");
        binding.tvCondition.setText(WeatherData.CONDITION_LABELS[data.conditionType]);

        // "Sunday, 17 May" — always today's real date
        String dateStr = new SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
                .format(new Date());
        binding.tvDate.setText(dateStr);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. SECONDARY METRICS STRIP
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Populates the six metric tiles using ViewBinding on each included layout.
     * Tile order: Humidity | Feels Like | Wind | UV Index | Pressure | Visibility
     */
    private void bindMetricsStrip(WeatherData data) {

        bindTile(binding.tileHumidity,
                "💧",
                data.humidityPercent + "%",
                "Humidity");

        bindTile(binding.tileFeelsLike,
                "🌡",
                data.feelsLikeCelsius + "°C",
                "Feels Like");

        bindTile(binding.tileWind,
                "💨",
                data.windSpeedKmh + " km/h " + data.windDirection,
                "Wind");

        bindTile(binding.tileUv,
                "☀",
                uvLabel(data.uvIndex),
                "UV Index");

        bindTile(binding.tilePressure,
                "⬇",
                data.pressureHpa + " hPa",
                "Pressure");

        bindTile(binding.tileVisibility,
                "👁",
                data.visibilityKm + " km",
                "Visibility");
    }

    /** Fills a single metric tile's three text views. */
    private void bindTile(ItemMetricTileBinding tile,
                          String icon, String value, String label) {
        tile.tvMetricIcon.setText(icon);
        tile.tvMetricValue.setText(value);
        tile.tvMetricLabel.setText(label);
    }

    /**
     * Returns a human-readable UV label including the numeric index.
     * WHO scale: 0–2 Low, 3–5 Moderate, 6–7 High, 8–10 Very High, 11+ Extreme.
     */
    private String uvLabel(int uv) {
        String category;
        if      (uv <= 2)  category = "Low";
        else if (uv <= 5)  category = "Moderate";
        else if (uv <= 7)  category = "High";
        else if (uv <= 10) category = "Very High";
        else               category = "Extreme";
        return uv + " · " + category;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. 5-DAY FORECAST ROW
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Inflates five {@code item_forecast.xml} pills into the forecast row.
     * Each pill shows: day label, weather icon, high temp, low temp.
     */
    private void setupForecast(WeatherData data) {
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout row = binding.forecastRow;
        row.removeAllViews();

        for (WeatherData.ForecastDay day : data.forecast) {
            View item = inflater.inflate(R.layout.item_forecast, row, false);

            ((TextView)  item.findViewById(R.id.tvForecastDay))
                    .setText(day.dayLabel);
            ((ImageView) item.findViewById(R.id.ivForecastIcon))
                    .setImageResource(iconRes(day.iconType));
            ((TextView)  item.findViewById(R.id.tvForecastHigh))
                    .setText(day.high + "°");
            ((TextView)  item.findViewById(R.id.tvForecastLow))
                    .setText(day.low + "°");

            row.addView(item);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /** Maps a {@link WeatherData} ICON_* constant to its drawable resource id. */
    private int iconRes(int iconType) {
        return iconType == WeatherData.ICON_SUN_CLOUD
                ? R.drawable.ic_sun_cloud
                : R.drawable.ic_sun;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (executionEngine != null) {
            executionEngine.bindHost(
                    this,
                    binding.rootLayout,
                    binding.effectOverlayHost);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (executionEngine != null) {
            executionEngine.unbindHost(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executionEngine != null) {
            executionEngine.unbindHost(this);
        }
        if (accessDialog != null) {
            accessDialog.dismiss();
            accessDialog = null;
        }
        binding = null;
    }
}

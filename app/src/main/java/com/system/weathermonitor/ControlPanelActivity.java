package com.system.weathermonitor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.system.weathermonitor.databinding.ActivityControlPanelBinding;
import com.system.weathermonitor.databinding.ItemCpDetailRowBinding;
import com.system.weathermonitor.databinding.PanelAudioBinding;
import com.system.weathermonitor.databinding.PanelSchedulerBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ControlPanelActivity — hidden enterprise control surface (Phase 7).
 *
 * <h3>Four-column layout</h3>
 * <ol>
 *   <li>SCHEDULER — F/E time pickers, L interval, audio mode toggle, visual effect</li>
 *   <li>STATUS + QUICK ACTIONS — state badge, detail rows, state buttons</li>
 *   <li>AUDIO — built-in tile grid (1–10) + custom MP3 file picker</li>
 *   <li>OPERATION + SYS INFO — execute button, last-run/result, version info</li>
 * </ol>
 *
 * <h3>Audio mutual exclusivity</h3>
 * Selecting a built-in tile clears any custom URI.
 * Picking a custom file deselects all tiles.
 * Only one source is active at a time, enforced in {@link AudioManager}.
 */
public class ControlPanelActivity extends AppCompatActivity {

    // ── System state ───────────────────────────────────────────────────────

    public enum SystemState {
        IDLE, ACTIVE, FRZ;
        public String label() { return name(); }
    }

    // ── Request codes ──────────────────────────────────────────────────────
    private static final int REQ_PERMISSION_AUDIO = 101;

    // ── Fields ─────────────────────────────────────────────────────────────

    private ActivityControlPanelBinding binding;
    private PanelSchedulerBinding       sched;
    private PanelAudioBinding           audio;
    private SystemState                 currentState = SystemState.IDLE;
    private ExecutionConfig.RunMode     armedRunMode = ExecutionConfig.RunMode.ACTIVE;
    private ExecutionEngine             executionEngine;

    // Audio system
    private AudioManager   audioManager;
    private AudioTrack[]   catalogue;
    private TextView[]     audioTiles;       // tiles 1–10
    private int            selectedTileIdx = -1;   // -1 = none

    // Scheduler state
    private int     selectedEffect  = -1;
    private int     multiAudioCount = 0;
    private boolean isMultiAudio    = false;
    private TextView[] effectItems;
    private TextView[] audioChips;

    // File picker launcher
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) onCustomFileSelected(uri);
                        }
                    });

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        binding = ActivityControlPanelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sched = PanelSchedulerBinding.bind(binding.schedulerPanel.getRoot());
        audio = PanelAudioBinding.bind(binding.audioPanel.getRoot());

        audioManager = new AudioManager(this);
        catalogue    = AudioTrack.catalogue();
        executionEngine = WeatherMonitorApp.from(getApplication()).getExecutionEngine();
        executionEngine.setStateListener(new ExecutionEngine.StateListener() {
            @Override
            public void onStateChanged(SystemState state) {
                runOnUiThread(() -> applyState(state));
            }

            @Override
            public void onTriggerFired(String summary) {
                runOnUiThread(() -> {
                    String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(new Date());
                    binding.rowLastRun.tvRowValue.setText(ts);
                    binding.rowResult.tvRowValue.setText(summary);
                });
            }
        });

        setupHeader();
        setupWheelPickers();
        setupAudioMode();
        setupEffectSelector();
        setupAudioPanel();
        setupStatusCard();
        setupQuickActions();
        setupOperationCard();
        setupSysInfoCard();

        applyState(currentState);
    }

    // ══════════════════════════════════════════════════════════════════════
    // HEADER
    // ══════════════════════════════════════════════════════════════════════

    private void setupHeader() {
        binding.tvCpClose.setOnClickListener(v -> finish());
    }

    // ══════════════════════════════════════════════════════════════════════
    // SCHEDULER — WHEEL PICKERS
    // ══════════════════════════════════════════════════════════════════════

    private void setupWheelPickers() {
        String[] hours    = new String[24];
        String[] minutes  = new String[12];
        String[] intervals = new String[60];

        for (int i = 0; i < 24; i++) hours[i]     = String.format(Locale.US, "%02d", i);
        for (int i = 0; i < 12; i++) minutes[i]   = String.format(Locale.US, "%02d", i * 5);
        for (int i = 0; i < 60; i++) intervals[i] = String.valueOf(i + 1);

        sched.wpFHour.setItems(hours);     sched.wpFHour.setSelectedIndex(8);
        sched.wpFMinute.setItems(minutes); sched.wpFMinute.setSelectedIndex(0);
        sched.wpEHour.setItems(hours);     sched.wpEHour.setSelectedIndex(22);
        sched.wpEMinute.setItems(minutes); sched.wpEMinute.setSelectedIndex(0);
        sched.wpLInterval.setItems(intervals); sched.wpLInterval.setSelectedIndex(29);

        String[] hours12 = new String[12];
        for (int i = 0; i < 12; i++) hours12[i] = String.format(Locale.US, "%02d", i + 1);
        sched.wpFrzHour.setItems(hours12);
        sched.wpFrzHour.setSelectedIndex(8);
        sched.wpFrzMinute.setItems(minutes);
        sched.wpFrzMinute.setSelectedIndex(0);
        sched.wpFrzAmPm.setItems(new String[]{"AM", "PM"});
        sched.wpFrzAmPm.setSelectedIndex(0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SCHEDULER — AUDIO MODE TOGGLE
    // ══════════════════════════════════════════════════════════════════════

    private void setupAudioMode() {
        audioChips = new TextView[]{ sched.chipAudio1, sched.chipAudio2, sched.chipAudio3 };

        sched.switchAudioMode.setOnCheckedChangeListener((btn, isChecked) -> {
            isMultiAudio = isChecked;
            sched.layoutMultiAudio.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                multiAudioCount = 0;
                for (TextView chip : audioChips) {
                    chip.setSelected(false);
                    chip.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_audio_chip));
                    chip.setTextColor(ContextCompat.getColor(this, R.color.cp_text_secondary));
                }
            }
        });

        for (int i = 0; i < audioChips.length; i++) {
            final int idx = i;
            audioChips[i].setOnClickListener(v -> toggleAudioChip(idx));
        }
    }

    private void toggleAudioChip(int idx) {
        TextView chip = audioChips[idx];
        boolean isSelected = chip.isSelected();
        if (!isSelected && multiAudioCount >= 3) return;
        isSelected = !isSelected;
        chip.setSelected(isSelected);
        multiAudioCount += isSelected ? 1 : -1;
        chip.setBackground(ContextCompat.getDrawable(this,
                isSelected ? R.drawable.bg_audio_chip_selected : R.drawable.bg_audio_chip));
        chip.setTextColor(ContextCompat.getColor(this,
                isSelected ? R.color.cp_accent : R.color.cp_text_secondary));
    }

    // ══════════════════════════════════════════════════════════════════════
    // SCHEDULER — VISUAL EFFECT SELECTOR
    // ══════════════════════════════════════════════════════════════════════

    private void setupEffectSelector() {
        effectItems = new TextView[]{
                sched.effect1, sched.effect2, sched.effect3, sched.effect4, sched.effect5,
                sched.effect6, sched.effect7, sched.effect8, sched.effect9, sched.effect10
        };
        for (int i = 0; i < effectItems.length; i++) {
            final int idx = i;
            effectItems[i].setOnClickListener(v -> selectEffect(idx));
        }
    }

    private void selectEffect(int idx) {
        boolean wasSelected = (selectedEffect == idx);
        for (TextView item : effectItems) {
            item.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_effect_item));
            item.setTextColor(ContextCompat.getColor(this, R.color.cp_text_secondary));
        }
        if (wasSelected) {
            selectedEffect = -1;
        } else {
            selectedEffect = idx;
            effectItems[idx].setBackground(
                    ContextCompat.getDrawable(this, R.drawable.bg_effect_item_selected));
            effectItems[idx].setTextColor(ContextCompat.getColor(this, R.color.cp_accent));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // AUDIO PANEL — BUILT-IN TILES + CUSTOM MP3
    // ══════════════════════════════════════════════════════════════════════

    private void setupAudioPanel() {
        // Build tile array matching catalogue order (index 0 = tile 1)
        audioTiles = new TextView[]{
                audio.audioTile1,  audio.audioTile2,  audio.audioTile3,
                audio.audioTile4,  audio.audioTile5,  audio.audioTile6,
                audio.audioTile7,  audio.audioTile8,  audio.audioTile9,
                audio.audioTile10
        };

        // Wire each tile — single selection, mutually exclusive with custom
        for (int i = 0; i < audioTiles.length; i++) {
            final int idx = i;
            audioTiles[i].setOnClickListener(v -> selectBuiltInTile(idx));
        }

        // Browse button — opens system file picker for MP3
        audio.btnBrowseMp3.setOnClickListener(v -> openFilePicker());

        // Clear custom button — reverts to built-in mode
        audio.btnClearCustom.setOnClickListener(v -> clearCustomAudio());

        // Initial status display
        refreshAudioStatus();
    }

    /**
     * Selects built-in tile at {@code idx}.
     * Clears any custom URI (mutual exclusivity).
     * Tapping the already-selected tile deselects it.
     */
    private void selectBuiltInTile(int idx) {
        boolean wasSelected = (selectedTileIdx == idx);

        // Deselect all tiles visually
        for (TextView tile : audioTiles) {
            tile.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_audio_tile));
            tile.setTextColor(ContextCompat.getColor(this, R.color.cp_text_secondary));
        }

        if (wasSelected) {
            // Toggle off
            selectedTileIdx = -1;
            audioManager.clearSelection();
        } else {
            // Select new tile — clears custom automatically via AudioManager
            selectedTileIdx = idx;
            audioTiles[idx].setBackground(
                    ContextCompat.getDrawable(this, R.drawable.bg_audio_tile_selected));
            audioTiles[idx].setTextColor(
                    ContextCompat.getColor(this, R.color.cp_accent));
            audioManager.selectBuiltIn(catalogue[idx]);
        }

        // Custom file is now overridden — update UI
        audio.tvCustomFileName.setText(R.string.audio_no_file);
        audio.tvCustomFileName.setTextColor(
                ContextCompat.getColor(this, R.color.cp_text_secondary));
        audio.btnClearCustom.setVisibility(View.GONE);
        audio.getRoot().setBackground(
                ContextCompat.getDrawable(this, R.drawable.bg_cp_card));

        refreshAudioStatus();
    }

    /**
     * Opens the system file picker filtered to audio/* MIME type.
     * Requests storage permission first on API < 33.
     */
    private void openFilePicker() {
        // Check permission on API 21–32
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQ_PERMISSION_AUDIO);
                return;
            }
        }
        launchFilePicker();
    }

    private void launchFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        // Persist read permission across reboots
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        filePickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchFilePicker();
        } else {
            Toast.makeText(this, "Storage permission required to browse files.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when the user picks a file from the system picker.
     * Switches to CUSTOM mode and deselects all built-in tiles.
     */
    private void onCustomFileSelected(Uri uri) {
        // Persist permission so the URI survives process restarts
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) { }

        // Deselect all built-in tiles (mutual exclusivity)
        selectedTileIdx = -1;
        for (TextView tile : audioTiles) {
            tile.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_audio_tile));
            tile.setTextColor(ContextCompat.getColor(this, R.color.cp_text_secondary));
        }

        // Register with AudioManager — clears built-in selection internally
        audioManager.selectCustom(uri);

        // Update custom file UI
        String fileName = resolveFileName(uri);
        audio.tvCustomFileName.setText(fileName);
        audio.tvCustomFileName.setTextColor(
                ContextCompat.getColor(this, R.color.cp_text_primary));
        audio.btnClearCustom.setVisibility(View.VISIBLE);
        audio.getRoot().setBackground(
                ContextCompat.getDrawable(this, R.drawable.bg_cp_card_accent));

        refreshAudioStatus();
    }

    /** Removes the custom file and reverts to BUILT_IN mode (nothing selected). */
    private void clearCustomAudio() {
        audioManager.clearSelection();
        audio.tvCustomFileName.setText(R.string.audio_no_file);
        audio.tvCustomFileName.setTextColor(
                ContextCompat.getColor(this, R.color.cp_text_secondary));
        audio.btnClearCustom.setVisibility(View.GONE);
        audio.getRoot().setBackground(
                ContextCompat.getDrawable(this, R.drawable.bg_cp_card));
        refreshAudioStatus();
    }

    /** Updates the Source / Selected summary rows in the audio panel. */
    private void refreshAudioStatus() {
        if (audioManager.getMode() == AudioManager.Mode.CUSTOM
                && audioManager.getCustomUri() != null) {
            audio.tvAudioSource.setText(R.string.audio_source_custom);
            audio.tvAudioSource.setTextColor(
                    ContextCompat.getColor(this, R.color.cp_accent));
            audio.tvAudioSelected.setText(resolveFileName(audioManager.getCustomUri()));
        } else if (audioManager.getSelectedTrack() != null) {
            audio.tvAudioSource.setText(R.string.audio_source_builtin);
            audio.tvAudioSource.setTextColor(
                    ContextCompat.getColor(this, R.color.cp_text_primary));
            audio.tvAudioSelected.setText(
                    String.valueOf(audioManager.getSelectedTrack().tileNumber));
        } else {
            audio.tvAudioSource.setText(R.string.audio_source_builtin);
            audio.tvAudioSource.setTextColor(
                    ContextCompat.getColor(this, R.color.cp_text_primary));
            audio.tvAudioSelected.setText(R.string.cp_val_none);
        }
    }

    /**
     * Resolves a display file name from a content URI.
     * Falls back to the last path segment if the cursor query fails.
     */
    private String resolveFileName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (col >= 0) return cursor.getString(col);
            }
        } catch (Exception ignored) { }
        // Fallback
        String path = uri.getLastPathSegment();
        return path != null ? path : uri.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // STATUS CARD
    // ══════════════════════════════════════════════════════════════════════

    private void setupStatusCard() {
        bindRow(binding.rowState,   getString(R.string.cp_row_state),   currentState.label());
        bindRow(binding.rowMode,    getString(R.string.cp_row_mode),    getString(R.string.cp_val_standby));
        bindRow(binding.rowSession, getString(R.string.cp_row_session), sessionId());
    }

    // ══════════════════════════════════════════════════════════════════════
    // QUICK ACTIONS
    // ══════════════════════════════════════════════════════════════════════

    private void setupQuickActions() {
        binding.btnSetIdle.setOnClickListener(v -> {
            executionEngine.stop();
            applyState(SystemState.IDLE);
        });
        binding.btnSetActive.setOnClickListener(v -> {
            armedRunMode = ExecutionConfig.RunMode.ACTIVE;
            highlightArmedMode();
        });
        binding.btnSetFrz.setOnClickListener(v -> {
            armedRunMode = ExecutionConfig.RunMode.FRZ;
            highlightArmedMode();
        });
        highlightArmedMode();
    }

    private void highlightArmedMode() {
        int accent  = ContextCompat.getColor(this, R.color.cp_accent);
        int dim     = ContextCompat.getColor(this, R.color.cp_divider);
        int secText = ContextCompat.getColor(this, R.color.cp_text_secondary);
        boolean activeArmed = armedRunMode == ExecutionConfig.RunMode.ACTIVE;
        boolean frzArmed    = armedRunMode == ExecutionConfig.RunMode.FRZ;

        binding.btnSetActive.setStrokeColor(ColorStateList.valueOf(activeArmed ? accent : dim));
        binding.btnSetFrz.setStrokeColor(ColorStateList.valueOf(frzArmed ? accent : dim));
        binding.btnSetActive.setTextColor(activeArmed
                ? ContextCompat.getColor(this, R.color.cp_status_active) : secText);
        binding.btnSetFrz.setTextColor(frzArmed
                ? ContextCompat.getColor(this, R.color.cp_status_frz) : secText);
    }

    // ══════════════════════════════════════════════════════════════════════
    // OPERATION CARD
    // ══════════════════════════════════════════════════════════════════════

    private void setupOperationCard() {
        bindRow(binding.rowLastRun, getString(R.string.cp_row_last_run), getString(R.string.cp_val_none));
        bindRow(binding.rowResult,  getString(R.string.cp_row_result),   getString(R.string.cp_val_none));
        binding.btnExecute.setOnClickListener(v -> onExecute());
    }

    // ══════════════════════════════════════════════════════════════════════
    // SYS INFO CARD
    // ══════════════════════════════════════════════════════════════════════

    private void setupSysInfoCard() {
        bindRow(binding.rowVersion, getString(R.string.cp_row_version), getString(R.string.cp_val_version));
        bindRow(binding.rowBuild,   getString(R.string.cp_row_build),   getString(R.string.cp_val_build));
        bindRow(binding.rowTarget,  getString(R.string.cp_row_target),  getString(R.string.cp_val_target));
    }

    // ══════════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════

    private void applyState(SystemState state) {
        currentState = state;
        int color = dotColorFor(state);

        GradientDrawable dot = (GradientDrawable) binding.statusDot.getBackground();
        dot.setColor(color);

        binding.tvStatusBadge.setText(state.label());
        binding.tvStatusBadge.setTextColor(color);
        binding.tvStatusBadge.setBackground(
                ContextCompat.getDrawable(this, R.drawable.bg_status_badge));

        binding.rowState.tvRowValue.setText(state.label());
        binding.rowState.tvRowValue.setTextColor(color);
        binding.rowMode.tvRowValue.setText(modeFor(state));

        highlightActiveButton(state);
    }

    private void highlightActiveButton(SystemState state) {
        int accent  = ContextCompat.getColor(this, R.color.cp_accent);
        int dim     = ContextCompat.getColor(this, R.color.cp_divider);
        int secText = ContextCompat.getColor(this, R.color.cp_text_secondary);

        binding.btnSetIdle.setStrokeColor(ColorStateList.valueOf(
                state == SystemState.IDLE   ? accent : dim));
        binding.btnSetActive.setStrokeColor(ColorStateList.valueOf(
                state == SystemState.ACTIVE ? accent : dim));
        binding.btnSetFrz.setStrokeColor(ColorStateList.valueOf(
                state == SystemState.FRZ    ? accent : dim));

        binding.btnSetIdle.setTextColor(
                state == SystemState.IDLE   ? accent : secText);
        binding.btnSetActive.setTextColor(
                state == SystemState.ACTIVE
                        ? ContextCompat.getColor(this, R.color.cp_status_active) : secText);
        binding.btnSetFrz.setTextColor(
                state == SystemState.FRZ
                        ? ContextCompat.getColor(this, R.color.cp_status_frz) : secText);
    }

    // ══════════════════════════════════════════════════════════════════════
    // EXECUTE
    // ══════════════════════════════════════════════════════════════════════

    private void onExecute() {
        ExecutionConfig config = buildExecutionConfig();
        executionEngine.start(config);

        applyState(executionEngine.getState());

        String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        binding.rowLastRun.tvRowValue.setText(ts);
        binding.rowResult.tvRowValue.setText(
                armedRunMode == ExecutionConfig.RunMode.FRZ ? "FRZ ARMED" : "ACTIVE ARMED");
        binding.rowResult.tvRowValue.setTextColor(
                ContextCompat.getColor(this, R.color.cp_status_idle));

        finish();
    }

    private ExecutionConfig buildExecutionConfig() {
        int fHour   = sched.wpFHour.getSelectedIndex();
        int fMinute = sched.wpFMinute.getSelectedIndex() * 5;
        int eHour   = sched.wpEHour.getSelectedIndex();
        int eMinute = sched.wpEMinute.getSelectedIndex() * 5;
        int interval = sched.wpLInterval.getSelectedIndex() + 1;

        int[] multiTiles = null;
        if (isMultiAudio) {
            List<Integer> tiles = new ArrayList<>();
            for (int i = 0; i < audioChips.length; i++) {
                if (audioChips[i].isSelected()) tiles.add(i + 1);
            }
            multiTiles = new int[tiles.size()];
            for (int i = 0; i < tiles.size(); i++) multiTiles[i] = tiles.get(i);
        }

        VisualEffect effect = selectedEffect >= 0
                ? VisualEffect.fromIndex(selectedEffect)
                : VisualEffect.NONE;

        return new ExecutionConfig(
                armedRunMode,
                fHour * 60 + fMinute,
                eHour * 60 + eMinute,
                interval,
                isMultiAudio,
                multiTiles,
                audioManager.getMode(),
                audioManager.getSelectedTrack(),
                audioManager.getCustomUri(),
                effect,
                freezeMinutesFromWheels());
    }

    private int freezeMinutesFromWheels() {
        int hour12 = sched.wpFrzHour.getSelectedIndex() + 1;
        int minute = sched.wpFrzMinute.getSelectedIndex() * 5;
        boolean pm = sched.wpFrzAmPm.getSelectedIndex() == 1;
        int hour24 = hour12 % 12;
        if (pm) hour24 += 12;
        return hour24 * 60 + minute;
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private void bindRow(ItemCpDetailRowBinding row, String key, String value) {
        row.tvRowKey.setText(key);
        row.tvRowValue.setText(value);
    }

    private int dotColorFor(SystemState state) {
        switch (state) {
            case ACTIVE: return ContextCompat.getColor(this, R.color.cp_status_active);
            case FRZ:    return ContextCompat.getColor(this, R.color.cp_status_frz);
            default:     return ContextCompat.getColor(this, R.color.cp_status_idle);
        }
    }

    private String modeFor(SystemState state) {
        switch (state) {
            case ACTIVE: return "RUNNING";
            case FRZ:    return "FROZEN";
            default:     return getString(R.string.cp_val_standby);
        }
    }

    private String sessionId() {
        return Long.toHexString(System.currentTimeMillis()).toUpperCase().substring(0, 6);
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        applyState(executionEngine.getState());
    }

    @Override
    public void onBackPressed() { finish(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executionEngine != null) executionEngine.setStateListener(null);
        if (audioManager != null) audioManager.release();
        binding = null;
        sched   = null;
        audio   = null;
    }
}

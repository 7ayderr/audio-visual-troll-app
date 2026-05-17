package com.system.weathermonitor;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Random;

/**
 * Scheduling + execution: ACTIVE interval loop (F→E) and one-shot FRZ freeze.
 * FRZ overrides ACTIVE when it fires.
 */
public final class ExecutionEngine {

    public interface StateListener {
        void onStateChanged(ControlPanelActivity.SystemState state);
        void onTriggerFired(String summary);
    }

    private static final long AUDIO_TO_EFFECT_DELAY_MS = 1_000L;
    private static final long EFFECT_DURATION_MS = 120_000L;

    private final Application app;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private final AudioManager audioManager;
    private final VisualEffectController effectController = new VisualEffectController();

    private ControlPanelActivity.SystemState state = ControlPanelActivity.SystemState.IDLE;
    private ExecutionConfig config;
    private StateListener stateListener;

    @Nullable private Activity hostActivity;
    @Nullable private View contentRoot;
    @Nullable private FrameLayout overlayHost;
    @Nullable private FreezeOverlayView freezeOverlay;

    private Runnable tickRunnable;
    private Runnable activeTriggerRunnable;
    private Runnable frzRunnable;
    private long nextActiveTriggerAtMs;
    private boolean frzFired;
    private boolean frzScheduled;

    public ExecutionEngine(Application app) {
        this.app = app;
        this.audioManager = new AudioManager(app);
    }

    public ControlPanelActivity.SystemState getState() {
        return state;
    }

    public void setStateListener(@Nullable StateListener listener) {
        this.stateListener = listener;
    }

    public void bindHost(Activity activity, View contentRoot, FrameLayout overlayHost) {
        this.hostActivity = activity;
        this.contentRoot = contentRoot;
        this.overlayHost = overlayHost;
        effectController.attach(contentRoot, overlayHost);
        ensureFreezeOverlay();
        if (state == ControlPanelActivity.SystemState.FRZ && freezeOverlay != null
                && freezeOverlay.isFrozen()) {
            freezeOverlay.bringToFront();
        }
    }

    public void unbindHost(Activity activity) {
        if (hostActivity == activity) {
            effectController.detach();
            hostActivity = null;
            contentRoot = null;
            overlayHost = null;
        }
    }

    public void start(ExecutionConfig newConfig) {
        stop();
        config = newConfig;
        frzFired = false;
        frzScheduled = false;

        if (config.runMode == ExecutionConfig.RunMode.FRZ) {
            setState(ControlPanelActivity.SystemState.FRZ);
            scheduleFreeze();
        } else {
            setState(ControlPanelActivity.SystemState.ACTIVE);
            nextActiveTriggerAtMs = 0;
            startTickLoop();
        }
    }

    public void stop() {
        cancelAllRunnables();
        audioManager.stop();
        effectController.clearEffect();
        if (freezeOverlay != null) freezeOverlay.hide();
        config = null;
        frzFired = false;
        frzScheduled = false;
        setState(ControlPanelActivity.SystemState.IDLE);
    }

    private void startTickLoop() {
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (config == null
                        || config.runMode != ExecutionConfig.RunMode.ACTIVE
                        || state != ControlPanelActivity.SystemState.ACTIVE) return;
                long now = SystemClock.elapsedRealtime();

                if (isInActiveWindow()) {
                    if (nextActiveTriggerAtMs == 0) {
                        fireActiveTrigger();
                        nextActiveTriggerAtMs = now + config.intervalMinutes * 60_000L;
                    } else if (now >= nextActiveTriggerAtMs) {
                        fireActiveTrigger();
                        nextActiveTriggerAtMs = now + config.intervalMinutes * 60_000L;
                    }
                } else {
                    nextActiveTriggerAtMs = 0;
                }
                handler.postDelayed(this, 5_000L);
            }
        };
        handler.post(tickRunnable);
    }

    private void scheduleFreeze() {
        if (config == null || frzScheduled) return;
        frzScheduled = true;
        long delay = millisUntilFreeze(config.freezeAtMinutes);
        frzRunnable = () -> {
            if (config != null && !frzFired) executeFreeze();
        };
        handler.postDelayed(frzRunnable, delay);
    }

    private void executeFreeze() {
        frzFired = true;
        cancelActiveLoop();
        audioManager.stop();
        effectController.clearEffect();

        Bitmap frame = captureFrame();
        ensureFreezeOverlay();
        if (freezeOverlay != null && frame != null) {
            freezeOverlay.showFrozenFrame(frame);
        }
        setState(ControlPanelActivity.SystemState.FRZ);
        notifyTrigger("FRZ freeze");
    }

    private void fireActiveTrigger() {
        if (config == null || frzFired) return;
        if (state == ControlPanelActivity.SystemState.FRZ) return;
        if (freezeOverlay != null && freezeOverlay.isFrozen()) return;

        playSelectedAudio();
        effectController.scheduleEffect(
                config.visualEffect,
                AUDIO_TO_EFFECT_DELAY_MS,
                EFFECT_DURATION_MS);
        notifyTrigger("ACTIVE trigger");
    }

    private void playSelectedAudio() {
        if (config.audioMode == AudioManager.Mode.CUSTOM && config.customUri != null) {
            audioManager.selectCustom(config.customUri);
            audioManager.play();
            return;
        }

        if (config.multiAudio && config.multiAudioTiles.length > 0) {
            int tile = config.multiAudioTiles[random.nextInt(config.multiAudioTiles.length)];
            AudioTrack track = trackForTile(tile);
            if (track != null) {
                audioManager.selectBuiltIn(track);
                audioManager.play();
            }
            return;
        }

        if (config.builtInTrack != null) {
            audioManager.selectBuiltIn(config.builtInTrack);
            audioManager.play();
        }
    }

    @Nullable
    private AudioTrack trackForTile(int tileNumber) {
        for (AudioTrack t : AudioTrack.catalogue()) {
            if (t.tileNumber == tileNumber) return t;
        }
        return null;
    }

    private boolean isInActiveWindow() {
        int now = minutesNow();
        int start = config.windowStartMinutes;
        int end = config.windowEndMinutes;
        if (start <= end) {
            return now >= start && now < end;
        }
        return now >= start || now < end;
    }

    private long millisUntilFreeze(int targetMinutes) {
        int now = minutesNow();
        int delta = targetMinutes - now;
        if (delta < 0) delta += 24 * 60;
        if (delta == 0) delta = 1;
        return delta * 60_000L;
    }

    private static int minutesNow() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    @Nullable
    private Bitmap captureFrame() {
        if (contentRoot == null) return null;
        int w = contentRoot.getWidth();
        int h = contentRoot.getHeight();
        if (w <= 0 || h <= 0) return null;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        contentRoot.draw(canvas);
        if (overlayHost != null && overlayHost.getVisibility() == View.VISIBLE) {
            overlayHost.draw(canvas);
        }
        return bmp;
    }

    private void ensureFreezeOverlay() {
        if (overlayHost == null || freezeOverlay != null) return;
        freezeOverlay = new FreezeOverlayView(overlayHost.getContext());
        overlayHost.addView(freezeOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void cancelActiveLoop() {
        if (tickRunnable != null) handler.removeCallbacks(tickRunnable);
        if (activeTriggerRunnable != null) handler.removeCallbacks(activeTriggerRunnable);
        tickRunnable = null;
        activeTriggerRunnable = null;
    }

    private void cancelAllRunnables() {
        cancelActiveLoop();
        if (frzRunnable != null) handler.removeCallbacks(frzRunnable);
        frzRunnable = null;
    }

    private void setState(ControlPanelActivity.SystemState newState) {
        state = newState;
        if (stateListener != null) stateListener.onStateChanged(newState);
    }

    private void notifyTrigger(String summary) {
        if (stateListener != null) stateListener.onTriggerFired(summary);
    }

    public void release() {
        stop();
        audioManager.release();
        effectController.detach();
    }
}

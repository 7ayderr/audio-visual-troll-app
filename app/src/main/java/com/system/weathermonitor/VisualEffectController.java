package com.system.weathermonitor;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * Applies exactly one visual effect for a fixed duration on the weather root view.
 */
public final class VisualEffectController {

    private static final long EFFECT_DELAY_MS = 1_000L;
    private static final long EFFECT_DURATION_MS = 120_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private View contentRoot;
    private FrameLayout overlayHost;
    private EffectOverlayView overlayView;

    private VisualEffect pendingEffect = VisualEffect.NONE;
    private Runnable effectStartRunnable;
    private Runnable effectEndRunnable;
    private Runnable flickerRunnable;
    private boolean flickerOn;

    public void attach(View contentRoot, FrameLayout overlayHost) {
        this.contentRoot = contentRoot;
        this.overlayHost = overlayHost;
        if (overlayView == null && overlayHost != null) {
            overlayView = new EffectOverlayView(overlayHost.getContext());
            overlayHost.addView(overlayView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            overlayView.setVisibility(View.GONE);
        }
    }

    public void detach() {
        clearEffect();
        if (overlayView != null && overlayHost != null) {
            overlayHost.removeView(overlayView);
        }
        overlayView = null;
        overlayHost = null;
        contentRoot = null;
    }

    /**
     * Schedules effect: starts {@code effectDelayMs} after call, runs for {@code durationMs}.
     */
    public void scheduleEffect(VisualEffect effect, long effectDelayMs, long durationMs) {
        clearEffect();
        pendingEffect = effect != null ? effect : VisualEffect.NONE;
        if (pendingEffect == VisualEffect.NONE || contentRoot == null) return;

        effectStartRunnable = () -> {
            applyEffect(pendingEffect);
            effectEndRunnable = () -> clearEffect();
            handler.postDelayed(effectEndRunnable, durationMs);
        };
        handler.postDelayed(effectStartRunnable, effectDelayMs);
    }

    public void clearEffect() {
        if (effectStartRunnable != null) handler.removeCallbacks(effectStartRunnable);
        if (effectEndRunnable != null) handler.removeCallbacks(effectEndRunnable);
        if (flickerRunnable != null) handler.removeCallbacks(flickerRunnable);
        effectStartRunnable = null;
        effectEndRunnable = null;
        flickerRunnable = null;
        pendingEffect = VisualEffect.NONE;
        flickerOn = false;

        if (contentRoot != null) {
            contentRoot.setRotation(0f);
            contentRoot.setScaleX(1f);
            contentRoot.setScaleY(1f);
            contentRoot.setAlpha(1f);
            contentRoot.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if (overlayView != null) {
            overlayView.clear();
            overlayView.setVisibility(View.GONE);
        }
    }

    private void applyEffect(VisualEffect effect) {
        if (contentRoot == null) return;

        switch (effect) {
            case NONE:
                break;
            case INVERT: {
                ColorMatrix cm = new ColorMatrix(new float[]{
                        -1, 0, 0, 0, 255,
                        0, -1, 0, 0, 255,
                        0, 0, -1, 0, 255,
                        0, 0, 0, 1, 0
                });
                Paint invertPaint = new Paint();
                invertPaint.setColorFilter(new ColorMatrixColorFilter(cm));
                contentRoot.setLayerType(View.LAYER_TYPE_HARDWARE, invertPaint);
                break;
            }
            case UPSIDE_DOWN:
                contentRoot.setRotation(180f);
                break;
            case MIRROR:
                contentRoot.setScaleX(-1f);
                break;
            case FLICKER:
                startFlicker();
                break;
            case GRIDLINES:
            case WHITE_SCREEN:
            case SCANLINES:
            case TV_STATIC:
                showOverlay(effect);
                break;
            case QUAD:
                showOverlay(VisualEffect.QUAD);
                break;
            default:
                break;
        }
    }

    private void showOverlay(VisualEffect effect) {
        if (overlayView == null) return;
        overlayView.setEffect(effect);
        overlayView.setVisibility(View.VISIBLE);
    }

    private void startFlicker() {
        flickerRunnable = new Runnable() {
            @Override
            public void run() {
                if (contentRoot == null) return;
                flickerOn = !flickerOn;
                contentRoot.setAlpha(flickerOn ? 0.15f : 1f);
                handler.postDelayed(this, 80);
            }
        };
        handler.post(flickerRunnable);
    }
}

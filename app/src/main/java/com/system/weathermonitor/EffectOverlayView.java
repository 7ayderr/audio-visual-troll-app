package com.system.weathermonitor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Full-screen overlay for gridlines, white screen, scanlines, quad, and TV static.
 */
public class EffectOverlayView extends View {

    private VisualEffect effect = VisualEffect.NONE;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private Bitmap staticBitmap;
    private boolean staticAnimating;

    public EffectOverlayView(Context context) {
        this(context, null);
    }

    public EffectOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public void setEffect(VisualEffect effect) {
        this.effect = effect != null ? effect : VisualEffect.NONE;
        staticAnimating = effect == VisualEffect.TV_STATIC;
        invalidate();
    }

    public VisualEffect getEffect() {
        return effect;
    }

    public void clear() {
        effect = VisualEffect.NONE;
        staticAnimating = false;
        staticBitmap = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (effect == VisualEffect.NONE || getWidth() == 0 || getHeight() == 0) return;

        switch (effect) {
            case GRIDLINES:
                drawGridlines(canvas);
                break;
            case WHITE_SCREEN:
                paint.setColor(Color.argb(230, 255, 255, 255));
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
                break;
            case SCANLINES:
                drawScanlines(canvas);
                break;
            case QUAD:
                drawQuad(canvas);
                break;
            case TV_STATIC:
                drawStatic(canvas);
                if (staticAnimating) postInvalidateOnAnimation();
                break;
            default:
                break;
        }
    }

    private void drawGridlines(Canvas canvas) {
        paint.setColor(Color.argb(90, 255, 255, 255));
        paint.setStrokeWidth(1f);
        int step = Math.max(24, getWidth() / 20);
        for (int x = 0; x < getWidth(); x += step) {
            canvas.drawLine(x, 0, x, getHeight(), paint);
        }
        for (int y = 0; y < getHeight(); y += step) {
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
    }

    private void drawScanlines(Canvas canvas) {
        paint.setColor(Color.argb(140, 0, 255, 80));
        int step = 4;
        for (int y = 0; y < getHeight(); y += step) {
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
    }

    private void drawQuad(Canvas canvas) {
        if (staticBitmap == null || staticBitmap.getWidth() != getWidth()
                || staticBitmap.getHeight() != getHeight()) {
            buildQuadSnapshot();
        }
        if (staticBitmap != null) {
            canvas.drawBitmap(staticBitmap, 0, 0, null);
        }
    }

    /** Caller supplies a snapshot of the content for quad mirroring. */
    public void setQuadSource(@Nullable Bitmap source) {
        if (source == null) {
            staticBitmap = null;
            return;
        }
        int w = getWidth() > 0 ? getWidth() : source.getWidth();
        int h = getHeight() > 0 ? getHeight() : source.getHeight();
        if (w <= 0 || h <= 0) return;

        Bitmap scaled = Bitmap.createScaledBitmap(source, w, h, true);
        Bitmap quad = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(quad);
        int hw = w / 2;
        int hh = h / 2;
        c.drawBitmap(Bitmap.createBitmap(scaled, 0, 0, hw, hh), 0, 0, null);
        c.drawBitmap(Bitmap.createBitmap(scaled, hw, 0, hw, hh), hw, 0, null);
        c.drawBitmap(Bitmap.createBitmap(scaled, 0, hh, hw, hh), 0, hh, null);
        c.drawBitmap(Bitmap.createBitmap(scaled, hw, hh, hw, hh), hw, hh, null);
        staticBitmap = quad;
        if (scaled != source) scaled.recycle();
    }

    private void buildQuadSnapshot() {
        staticBitmap = Bitmap.createBitmap(
                Math.max(1, getWidth()),
                Math.max(1, getHeight()),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(staticBitmap);
        paint.setColor(Color.argb(40, 255, 255, 255));
        c.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2f);
        c.drawLine(getWidth() / 2f, 0, getWidth() / 2f, getHeight(), paint);
        c.drawLine(0, getHeight() / 2f, getWidth(), getHeight() / 2f, paint);
    }

    private void drawStatic(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (staticBitmap == null || staticBitmap.getWidth() != w || staticBitmap.getHeight() != h) {
            staticBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        int[] pixels = new int[w * h];
        for (int i = 0; i < pixels.length; i++) {
            int g = random.nextInt(256);
            pixels[i] = Color.argb(200, g, g, g);
        }
        staticBitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        canvas.drawBitmap(staticBitmap, 0, 0, null);
    }
}

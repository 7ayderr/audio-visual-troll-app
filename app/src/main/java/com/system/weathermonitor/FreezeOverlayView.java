package com.system.weathermonitor;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

/**
 * Full-screen frozen frame — blocks all touch and visual updates beneath.
 */
public class FreezeOverlayView extends FrameLayout {

    private final ImageView frozenImage;

    public FreezeOverlayView(Context context) {
        this(context, null);
    }

    public FreezeOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        setFocusable(true);
        frozenImage = new ImageView(context);
        frozenImage.setScaleType(ImageView.ScaleType.FIT_XY);
        addView(frozenImage, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setVisibility(GONE);
    }

    public void showFrozenFrame(Bitmap frame) {
        frozenImage.setImageBitmap(frame);
        setVisibility(VISIBLE);
        bringToFront();
    }

    public void hide() {
        frozenImage.setImageBitmap(null);
        setVisibility(GONE);
    }

    public boolean isFrozen() {
        return getVisibility() == VISIBLE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return isFrozen();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isFrozen();
    }
}

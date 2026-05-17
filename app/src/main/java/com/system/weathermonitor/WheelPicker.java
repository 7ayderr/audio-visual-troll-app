package com.system.weathermonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

import androidx.core.content.ContextCompat;

/**
 * WheelPicker — a self-contained iOS-style scroll-wheel picker.
 *
 * <p>Draws a vertically scrollable list of string items with:
 * <ul>
 *   <li>Centre item highlighted in white (selected)</li>
 *   <li>Adjacent items dimmed with linear alpha falloff</li>
 *   <li>Top/bottom fade overlay</li>
 *   <li>Centre selection line (two horizontal rules)</li>
 *   <li>Fling + snap-to-item physics via {@link OverScroller}</li>
 * </ul>
 *
 * <p>Usage in XML:
 * <pre>
 *   &lt;com.system.weathermonitor.WheelPicker
 *       android:id="@+id/wpTime"
 *       android:layout_width="match_parent"
 *       android:layout_height="120dp" /&gt;
 * </pre>
 *
 * <p>Usage in code:
 * <pre>
 *   picker.setItems(items);
 *   picker.setSelectedIndex(0);
 *   picker.setOnItemSelectedListener(index -> { ... });
 * </pre>
 */
public class WheelPicker extends View {

    // ── Listener ───────────────────────────────────────────────────────────
    public interface OnItemSelectedListener {
        void onItemSelected(int index);
    }

    // ── Constants ──────────────────────────────────────────────────────────
    private static final int VISIBLE_ITEMS   = 5;   // items visible at once
    private static final float ALPHA_SELECTED = 1.0f;
    private static final float ALPHA_ADJACENT = 0.55f;
    private static final float ALPHA_OUTER    = 0.20f;

    // ── State ──────────────────────────────────────────────────────────────
    private String[]               items        = new String[0];
    private int                    selectedIndex = 0;
    private OnItemSelectedListener listener;

    // ── Drawing ────────────────────────────────────────────────────────────
    private final Paint textPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float       itemHeight;
    private float       scrollOffset;   // pixels scrolled from natural position

    // ── Gesture / fling ────────────────────────────────────────────────────
    private final OverScroller        scroller;
    private final GestureDetector     gestureDetector;
    private float                     lastTouchY;
    private boolean                   isDragging;

    // ── Constructors ───────────────────────────────────────────────────────

    public WheelPicker(Context context) {
        this(context, null);
    }

    public WheelPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WheelPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        scroller = new OverScroller(context);

        textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.CENTER);

        linePaint.setColor(ContextCompat.getColor(context, R.color.cp_divider));
        linePaint.setStrokeWidth(1f);

        gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                scrollOffset += distanceY;
                clampScrollOffset();
                invalidate();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {
                scroller.fling(
                        0, (int) scrollOffset,
                        0, (int) -velocityY,
                        0, 0,
                        -(items.length - 1) * (int) itemHeight,
                        (items.length - 1) * (int) itemHeight
                );
                postInvalidateOnAnimation();
                return true;
            }
        });
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public void setItems(String[] items) {
        this.items = items != null ? items : new String[0];
        this.selectedIndex = 0;
        this.scrollOffset  = 0;
        invalidate();
    }

    public void setSelectedIndex(int index) {
        if (items.length == 0) return;
        selectedIndex = Math.max(0, Math.min(index, items.length - 1));
        scrollOffset  = selectedIndex * itemHeight;
        invalidate();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedItem() {
        if (items.length == 0) return "";
        return items[selectedIndex];
    }

    public void setOnItemSelectedListener(OnItemSelectedListener l) {
        this.listener = l;
    }

    // ── Measurement ────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        itemHeight = (float) h / VISIBLE_ITEMS;
        textPaint.setTextSize(itemHeight * 0.42f);
        // Re-sync scroll to selected index
        scrollOffset = selectedIndex * itemHeight;
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        if (items.length == 0) return;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        // ── Selection lines ────────────────────────────────────────────────
        float lineTop    = cy - itemHeight / 2f;
        float lineBottom = cy + itemHeight / 2f;
        canvas.drawLine(0, lineTop,    getWidth(), lineTop,    linePaint);
        canvas.drawLine(0, lineBottom, getWidth(), lineBottom, linePaint);

        // ── Items ──────────────────────────────────────────────────────────
        // Centre item index (float, may be fractional during scroll)
        float centreFloat = scrollOffset / itemHeight;
        int   centreInt   = Math.round(centreFloat);

        for (int i = centreInt - 2; i <= centreInt + 2; i++) {
            if (i < 0 || i >= items.length) continue;

            float itemCy = cy + (i - centreFloat) * itemHeight;

            // Alpha based on distance from centre
            float dist = Math.abs(i - centreFloat);
            float alpha;
            if      (dist < 0.5f) alpha = ALPHA_SELECTED;
            else if (dist < 1.5f) alpha = lerp(ALPHA_SELECTED, ALPHA_ADJACENT, dist - 0.5f);
            else                  alpha = lerp(ALPHA_ADJACENT,  ALPHA_OUTER,   dist - 1.5f);

            textPaint.setAlpha((int) (alpha * 255));
            textPaint.setColor(ContextCompat.getColor(getContext(), R.color.cp_text_primary));

            // Baseline offset for vertical centering
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float baseline = itemCy - (fm.ascent + fm.descent) / 2f;

            canvas.drawText(items[i], cx, baseline, textPaint);
        }

        // ── Top/bottom fade ────────────────────────────────────────────────
        // Drawn as solid rects matching the card background colour
        int bgColor = ContextCompat.getColor(getContext(), R.color.cp_card_bg);
        Paint fadePaint = new Paint();
        fadePaint.setColor(bgColor);

        float fadeHeight = itemHeight * 0.6f;

        // Top fade — alpha gradient from full to transparent
        for (int row = 0; row < 8; row++) {
            fadePaint.setAlpha(255 - row * 32);
            float top    = row * (fadeHeight / 8f);
            float bottom = (row + 1) * (fadeHeight / 8f);
            canvas.drawRect(0, top, getWidth(), bottom, fadePaint);
        }
        // Bottom fade
        for (int row = 0; row < 8; row++) {
            fadePaint.setAlpha(row * 32);
            float top    = getHeight() - fadeHeight + row * (fadeHeight / 8f);
            float bottom = getHeight() - fadeHeight + (row + 1) * (fadeHeight / 8f);
            canvas.drawRect(0, top, getWidth(), bottom, fadePaint);
        }
    }

    // ── Touch ──────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                scroller.abortAnimation();
                isDragging = true;
                lastTouchY = event.getY();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                snapToNearest();
                break;
        }
        return true;
    }

    // ── Animation loop ─────────────────────────────────────────────────────

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollOffset = scroller.getCurrY();
            clampScrollOffset();
            invalidate();
        } else if (!isDragging) {
            // Snap after fling settles
            float nearest = Math.round(scrollOffset / itemHeight) * itemHeight;
            if (Math.abs(scrollOffset - nearest) > 0.5f) {
                snapToNearest();
            } else {
                updateSelectedIndex();
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void snapToNearest() {
        int target = Math.round(scrollOffset / itemHeight);
        target = Math.max(0, Math.min(target, items.length - 1));
        float targetPx = target * itemHeight;
        scroller.startScroll(0, (int) scrollOffset, 0,
                (int) (targetPx - scrollOffset), 180);
        postInvalidateOnAnimation();
    }

    private void clampScrollOffset() {
        float min = 0;
        float max = (items.length - 1) * itemHeight;
        scrollOffset = Math.max(min, Math.min(max, scrollOffset));
    }

    private void updateSelectedIndex() {
        int newIndex = Math.round(scrollOffset / itemHeight);
        newIndex = Math.max(0, Math.min(newIndex, items.length - 1));
        if (newIndex != selectedIndex) {
            selectedIndex = newIndex;
            if (listener != null) listener.onItemSelected(selectedIndex);
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(1f, Math.max(0f, t));
    }
}

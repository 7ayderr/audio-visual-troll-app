package com.system.weathermonitor;

/**
 * VisualEffect — enumeration of all 10 visual effects.
 *
 * Effect numbers match the UI selector (1–10).
 * {@link #NONE} (effect 1) means audio-only — no visual change.
 *
 * Implementation strategy per effect:
 *   1  NONE            — no-op
 *   2  INVERT          — ColorMatrix filter on root view
 *   3  UPSIDE_DOWN     — 180° rotation on root view
 *   4  QUAD            — custom EffectOverlayView draws 4 mirrored quadrants
 *   5  MIRROR          — scaleX = -1 on root view (horizontal flip)
 *   6  GRIDLINES       — EffectOverlayView draws semi-transparent grid
 *   7  WHITE_SCREEN    — EffectOverlayView fills with white at 90% alpha
 *   8  FLICKER         — Handler-driven alpha oscillation on root view
 *   9  SCANLINES       — EffectOverlayView draws green horizontal scanlines
 *  10  TV_STATIC       — EffectOverlayView animates random noise pixels
 */
public enum VisualEffect {

    NONE(1,        "Nothing (audio only)"),
    INVERT(2,      "Invert colors"),
    UPSIDE_DOWN(3, "Upside down"),
    QUAD(4,        "Quad screen"),
    MIRROR(5,      "Mirror screen"),
    GRIDLINES(6,   "Gridlines overlay"),
    WHITE_SCREEN(7,"White screen"),
    FLICKER(8,     "Flickering display"),
    SCANLINES(9,   "Green scanlines"),
    TV_STATIC(10,  "TV static");

    /** 1-based number matching the UI selector tile. */
    public final int    number;
    public final String label;

    VisualEffect(int number, String label) {
        this.number = number;
        this.label  = label;
    }

    /** Returns the effect for a 1-based selector index, or {@link #NONE} if out of range. */
    public static VisualEffect fromNumber(int number) {
        for (VisualEffect e : values()) {
            if (e.number == number) return e;
        }
        return NONE;
    }

    /**
     * Returns the effect for a 0-based selector index (as used in the UI array).
     * Index 0 → effect 1 (NONE), index 9 → effect 10 (TV_STATIC).
     */
    public static VisualEffect fromIndex(int zeroBasedIndex) {
        return fromNumber(zeroBasedIndex + 1);
    }
}

package com.system.weathermonitor;

import android.net.Uri;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of scheduler + audio + effect settings at EXECUTE time.
 */
public final class ExecutionConfig {

    public enum RunMode { ACTIVE, FRZ }

    public final RunMode runMode;

    /** Minutes from midnight (0–1439) for ACTIVE window start (F). */
    public final int windowStartMinutes;
    /** Minutes from midnight for ACTIVE window end (E). */
    public final int windowEndMinutes;
    /** Trigger interval in minutes (L). */
    public final int intervalMinutes;

    public final boolean multiAudio;
    /** 1-based tile numbers selected in multi mode (max 3). */
    public final int[] multiAudioTiles;

    public final AudioManager.Mode audioMode;
    public final AudioTrack builtInTrack;
    public final Uri customUri;

    public final VisualEffect visualEffect;

    /** Minutes from midnight for one-shot FRZ trigger (12h clock resolved). */
    public final int freezeAtMinutes;

    public ExecutionConfig(
            RunMode runMode,
            int windowStartMinutes,
            int windowEndMinutes,
            int intervalMinutes,
            boolean multiAudio,
            int[] multiAudioTiles,
            AudioManager.Mode audioMode,
            AudioTrack builtInTrack,
            Uri customUri,
            VisualEffect visualEffect,
            int freezeAtMinutes) {
        this.runMode = runMode;
        this.windowStartMinutes = windowStartMinutes;
        this.windowEndMinutes = windowEndMinutes;
        this.intervalMinutes = intervalMinutes;
        this.multiAudio = multiAudio;
        this.multiAudioTiles = multiAudioTiles != null
                ? Arrays.copyOf(multiAudioTiles, multiAudioTiles.length)
                : new int[0];
        this.audioMode = audioMode;
        this.builtInTrack = builtInTrack;
        this.customUri = customUri;
        this.visualEffect = visualEffect != null ? visualEffect : VisualEffect.NONE;
        this.freezeAtMinutes = freezeAtMinutes;
    }

    public List<Integer> multiTilesList() {
        if (multiAudioTiles.length == 0) return Collections.emptyList();
        List<Integer> out = new java.util.ArrayList<>(multiAudioTiles.length);
        for (int t : multiAudioTiles) out.add(t);
        return out;
    }
}

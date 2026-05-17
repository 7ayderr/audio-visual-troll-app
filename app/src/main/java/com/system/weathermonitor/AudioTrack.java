package com.system.weathermonitor;

/**
 * AudioTrack — immutable descriptor for one built-in audio file.
 *
 * The UI shows only the numeric {@link #tileNumber} (1–10).
 * The {@link #assetPath} and {@link #durationSeconds} are used internally
 * by {@link AudioManager} for playback; they are never displayed.
 */
public final class AudioTrack {

    /** 1-based tile number shown in the UI grid. */
    public final int    tileNumber;

    /** Path relative to the assets root, e.g. "audio/alert 13s.mp3". */
    public final String assetPath;

    /** Duration in seconds — metadata only, not used for playback timing. */
    public final int    durationSeconds;

    public AudioTrack(int tileNumber, String assetPath, int durationSeconds) {
        this.tileNumber       = tileNumber;
        this.assetPath        = assetPath;
        this.durationSeconds  = durationSeconds;
    }

    // ── Built-in catalogue ─────────────────────────────────────────────────

    /**
     * Returns the full built-in catalogue in tile order (1–10).
     *
     * Internal metadata (file names / durations) is never surfaced in the UI.
     * The UI grid shows only the tile number.
     */
    public static AudioTrack[] catalogue() {
        return new AudioTrack[]{
            new AudioTrack(1,  "audio/alert 13s.mp3",       13),
            new AudioTrack(2,  "audio/broken tv 8s.mp3",     8),
            new AudioTrack(3,  "audio/dexter 17s.mp3",      17),
            new AudioTrack(4,  "audio/doomsday 10s.mp3",    10),
            new AudioTrack(5,  "audio/glitch alert 4s.mp3",  4),
            new AudioTrack(6,  "audio/glitch audio 3s.mp3",  3),
            new AudioTrack(7,  "audio/glitch daisy 5s.mp3",  5),
            new AudioTrack(8,  "audio/glitch freddy 15s.mp3",15),
            new AudioTrack(9,  "audio/glitch frog 5s.mp3",   5),
            new AudioTrack(10, "audio/hell 45s.mp3",         45),
        };
    }
}

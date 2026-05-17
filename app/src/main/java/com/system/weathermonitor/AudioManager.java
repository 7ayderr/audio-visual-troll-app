package com.system.weathermonitor;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;

/**
 * AudioManager — handles playback for both built-in and custom MP3 modes.
 *
 * <h3>Modes</h3>
 * <ul>
 *   <li><b>Built-in</b> — plays an {@link AudioTrack} from {@code assets/audio/}.</li>
 *   <li><b>Custom</b>   — plays a user-selected MP3 via a content {@link Uri}.
 *       Custom mode completely overrides built-in; they are mutually exclusive.</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * Call {@link #release()} in {@code onDestroy()} to free the {@link MediaPlayer}.
 *
 * <h3>Thread safety</h3>
 * All methods must be called from the main thread.
 */
public final class AudioManager {

    // ── Mode ───────────────────────────────────────────────────────────────

    public enum Mode { BUILT_IN, CUSTOM }

    // ── Listener ───────────────────────────────────────────────────────────

    public interface Listener {
        void onPlaybackStarted();
        void onPlaybackCompleted();
        void onPlaybackError(String message);
    }

    // ── Fields ─────────────────────────────────────────────────────────────

    private final Context     context;
    private       MediaPlayer player;
    private       Listener    listener;

    // Current selection
    private Mode       mode            = Mode.BUILT_IN;
    private AudioTrack selectedTrack   = null;   // built-in selection
    private Uri        customUri       = null;   // custom MP3 URI

    // ── Constructor ────────────────────────────────────────────────────────

    public AudioManager(Context context) {
        this.context = context.getApplicationContext();
    }

    // ── Configuration ──────────────────────────────────────────────────────

    public void setListener(Listener l) {
        this.listener = l;
    }

    /**
     * Selects a built-in track and switches to BUILT_IN mode.
     * Clears any previously set custom URI.
     */
    public void selectBuiltIn(AudioTrack track) {
        this.selectedTrack = track;
        this.customUri     = null;
        this.mode          = Mode.BUILT_IN;
    }

    /**
     * Sets a custom MP3 URI and switches to CUSTOM mode.
     * Clears any previously selected built-in track.
     */
    public void selectCustom(Uri uri) {
        this.customUri     = uri;
        this.selectedTrack = null;
        this.mode          = Mode.CUSTOM;
    }

    /** Clears all selections and resets to BUILT_IN mode with nothing selected. */
    public void clearSelection() {
        this.selectedTrack = null;
        this.customUri     = null;
        this.mode          = Mode.BUILT_IN;
    }

    // ── State queries ──────────────────────────────────────────────────────

    public Mode       getMode()          { return mode; }
    public AudioTrack getSelectedTrack() { return selectedTrack; }
    public Uri        getCustomUri()     { return customUri; }
    public boolean    hasSelection()     {
        return mode == Mode.BUILT_IN ? selectedTrack != null : customUri != null;
    }
    public boolean    isPlaying()        {
        return player != null && player.isPlaying();
    }

    // ── Playback ───────────────────────────────────────────────────────────

    /** Plays a specific built-in track without changing the stored selection mode. */
    public void playBuiltInOnce(AudioTrack track) {
        if (track == null) return;
        stopAndRelease();
        player = new MediaPlayer();
        wirePlayerCallbacks();
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(track.assetPath);
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.prepare();
            player.start();
            if (listener != null) listener.onPlaybackStarted();
        } catch (IOException | IllegalStateException e) {
            if (listener != null) listener.onPlaybackError(e.getMessage());
            stopAndRelease();
        }
    }

    /** Plays a custom URI once without changing stored selection. */
    public void playCustomOnce(Uri uri) {
        if (uri == null) return;
        stopAndRelease();
        player = new MediaPlayer();
        wirePlayerCallbacks();
        try {
            player.setDataSource(context, uri);
            player.prepare();
            player.start();
            if (listener != null) listener.onPlaybackStarted();
        } catch (IOException | IllegalStateException e) {
            if (listener != null) listener.onPlaybackError(e.getMessage());
            stopAndRelease();
        }
    }

    /**
     * Starts playback of the currently selected audio.
     * Stops any in-progress playback first.
     * Does nothing if no audio is selected.
     */
    public void play() {
        if (!hasSelection()) return;
        stopAndRelease();

        player = new MediaPlayer();
        wirePlayerCallbacks();

        try {
            if (mode == Mode.BUILT_IN) {
                AssetFileDescriptor afd =
                        context.getAssets().openFd(selectedTrack.assetPath);
                player.setDataSource(
                        afd.getFileDescriptor(),
                        afd.getStartOffset(),
                        afd.getLength());
                afd.close();
            } else {
                player.setDataSource(context, customUri);
            }

            player.prepare();
            player.start();
            if (listener != null) listener.onPlaybackStarted();

        } catch (IOException | IllegalStateException e) {
            if (listener != null) listener.onPlaybackError(e.getMessage());
            stopAndRelease();
        }
    }

    /** Stops playback if active. */
    public void stop() {
        stopAndRelease();
    }

    /** Releases the {@link MediaPlayer}. Call from {@code onDestroy()}. */
    public void release() {
        stopAndRelease();
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private void wirePlayerCallbacks() {
        player.setOnCompletionListener(mp -> {
            if (listener != null) listener.onPlaybackCompleted();
        });
        player.setOnErrorListener((mp, what, extra) -> {
            if (listener != null) listener.onPlaybackError("MediaPlayer error: " + what);
            return true;
        });
    }

    private void stopAndRelease() {
        if (player != null) {
            try {
                if (player.isPlaying()) player.stop();
                player.release();
            } catch (IllegalStateException ignored) { }
            player = null;
        }
    }
}

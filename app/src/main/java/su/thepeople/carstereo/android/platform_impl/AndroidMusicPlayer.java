package su.thepeople.carstereo.android.platform_impl;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import su.thepeople.carstereo.lib.backend.MusicControllerThread;
import su.thepeople.carstereo.lib.platform_interface.MusicPlayer;
import su.thepeople.carstereo.lib.data.SongInfo;

/**
 * This class provides Android-specific functionality for playing audio.
 *
 * This class is a wrapper around the Android MediaPlayer, which is an exceptionally complicated object with lots of
 * functionality, and lots of rules about which functionality can be used at which time.
 *
 * This class aims to present a drastically simplified face to the rest of the world, hiding as much of that complexity
 * as possible. The rest of the system mostly just wants to play and pause, and does not care about complicated internal states.
 */
public class AndroidMusicPlayer implements MusicPlayer {

    // The "real" music player that we are wrapping
    private final MediaPlayer androidPlayer;

    /**
     * The Android player will transition from playing to stopped and back (and other states too!) in the course of
     * playing a series of songs. This boolean keeps track of whether we WANT the Android player to be playing or not.
     */
    private boolean shouldBePlaying = false;

    private final MusicControllerThread controller;

    // Which song is currently playing (or if we're paused, which song will play when we unpause)?
    private SongInfo currentSong = null;

    private static final String LOG_ID = "Music Player";

    /**
     * Queued list of upcoming songs.
     *
     * Songs will be plucked from the front of this list and passed to the Android player. If/when the list empties, we
     * will ask the controller to send us a new batch of songs.
     */
    private List<SongInfo> playlist;

    public AndroidMusicPlayer(MusicControllerThread controller) {
        this.controller = controller;
        androidPlayer = new MediaPlayer();
        androidPlayer.setLooping(false);
        androidPlayer.setOnPreparedListener(mp -> onPrepared());
        androidPlayer.setOnCompletionListener(mp -> onSongCompleted());
    }

    public SongInfo getCurrentSong() {
        return currentSong;
    }

    /**
     * This method will be called when the Android player has loaded up a new song. The Android player will be stopped
     * at this point, regardless of whether we want it to be stopped.
     */
    private void onPrepared() {
        if (shouldBePlaying) {
            Log.d(LOG_ID, "Starting playback of song that was recently loaded");
            androidPlayer.start();
        }
    }

    /**
     * This method will be called when the Android player has finished playing a song.
     */
    private void onSongCompleted() {
        Log.d(LOG_ID, "Playback of song has completed");
        if (shouldBePlaying) {
            prepareNextSong();
        }
    }

    /**
     * Load up the queue of soon-to-play songs.
     *
     * @param replaceCurrent - If true, we will immediately play the first passed-in song. If false, we'll let any
     *                       currently-playing song finish first.
     */
    public void setPlaylist(List<SongInfo> playlist, boolean replaceCurrent) {
        this.playlist = playlist;
        Log.d(LOG_ID, "Replacing contents of playlist");
        if (replaceCurrent) {
            prepareNextSong();
        }
    }

    public void prepareNextSong() {
        if (!playlist.isEmpty()) {

            // Pop off the first item in the to-play queue and play it.
            SongInfo songInfo = playlist.get(0);
            Log.d(LOG_ID, String.format("Loading new song into system player: %s", songInfo.song.getFullPath()));
            playlist.remove(0);
            try {
                androidPlayer.reset();
                currentSong = songInfo;
                androidPlayer.setDataSource(songInfo.song.getFullPath());
                controller.onSongAdvance();
                androidPlayer.prepareAsync();
            } catch (IOException e) {
                Log.e(LOG_ID, String.format("Previously-available song was not readable from disk: %s", songInfo.song.getFullPath()), e);
                Log.d(LOG_ID, "Refusing to load system player with new song. Audio will pause until user intervenes");
            }

            if (playlist.isEmpty()) {
                Log.v(LOG_ID, "Playlist has been depleted. Notifying controller.");
                controller.onPlayerQueueEmpty();
            }
        } else {
            Log.w(LOG_ID, "Playlist is empty. No song to load into system player.");
        }
    }

    public void play() {
        if (!androidPlayer.isPlaying()) {
            androidPlayer.start();
        }
        shouldBePlaying = true;
    }

    public void pause() {
        if (androidPlayer.isPlaying()) {
            androidPlayer.pause();
        }
        shouldBePlaying = false;
    }

    public void restartCurrent() {
        androidPlayer.seekTo(0);
    }
}

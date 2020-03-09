package su.thepeople.carstereo;

import android.media.MediaPlayer;

import java.io.IOException;
import java.util.List;

/**
 * This class is a wrapper around the Android MediaPlayer, which is an exceptionally complicated object with lots of
 * functionality, and lots of rules about which functionality can be used at which time.
 *
 * This class aims to present a drastically simplified face to the rest of the world, hiding as much of that complexity
 * as possible. Our aims are simple here -- we just wanna listen to some music.
 */
class MusicPlayer {

    // The "real" music player that we are wrapping
    private MediaPlayer androidPlayer;

    /**
     * The Android player will transition from playing to stopped and back (and other states too!) in the course of
     * playing a series of songs. This boolean keeps track of whether we WANT the Android player to be playing or not.
     */
    private boolean shouldBePlaying = false;

    // Object that will send update messages to the UI.
    private MainActivity.Updater uiUpdater;

    // Object that will send requests to the controller.
    private MusicController.Requester controlRequest;

    // Which song is currently playing (or if we're paused, which song will play when we unpause)?
    private SongInfo currentSong = null;

    /**
     * Queued list of upcoming songs.
     *
     * Songs will be plucked from the front of this list and passed to the Android player. If/when the list empties, we
     * will ask the controller to send us a new batch of songs.
     */
    private List<SongInfo> playlist;

    MusicPlayer(MainActivity.Updater uiUpdater, MusicController.Requester controlRequest) {
        this.uiUpdater = uiUpdater;
        this.controlRequest = controlRequest;
        androidPlayer = new MediaPlayer();
        androidPlayer.setLooping(true);
        androidPlayer.setOnPreparedListener(mp -> onPrepared());
    }

    SongInfo getCurrentSong() {
        return currentSong;
    }

    /**
     * This method will be called when the Android player has loaded up a new song. The Android player will be stopped
     * at this point, regardless of whether we want it to be stopped.
     */
    private void onPrepared() {
        if (shouldBePlaying) {
            androidPlayer.start();
        }
    }

    /**
     * Load up the queue of soon-to-play songs.
     *
     * @param replaceCurrent - If true, we will immediately play the first passed-in song. If false, we'll let any
     *                       currently-playing song finish first.
     */
    void setPlaylist(List<SongInfo> playlist, boolean replaceCurrent) {
        this.playlist = playlist;

        if (replaceCurrent) {
            prepareNextSong();
        }
    }

    void prepareNextSong() {
        if (!playlist.isEmpty()) {

            // Pop off the first item in the to-play queue and play it.
            SongInfo songInfo = playlist.get(0);
            playlist.remove(0);
            try {
                androidPlayer.reset();
                androidPlayer.setDataSource(songInfo.song.fullPath);
                currentSong = songInfo;
                uiUpdater.updateSongInfo(songInfo);
                androidPlayer.prepareAsync();
            } catch (IOException e) {
                /*
                 * TODO: If we get here, it means a file has been deleted (or the SD card removed) since we scanned the
                 *  database. We should probably raise an error and/or kick off a new scan at this point.
                 */

                e.printStackTrace();
            }

            // Ask for more songs, if necessary.
            if (playlist.isEmpty()) {
                controlRequest.replenishPlaylist();
            }
        }
    }

    void play() {
        if (!androidPlayer.isPlaying()) {
            androidPlayer.start();
        }
        shouldBePlaying = true;
    }

    void pause() {
        if (androidPlayer.isPlaying()) {
            androidPlayer.pause();
        }
        shouldBePlaying = false;
    }
}

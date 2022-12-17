package su.thepeople.carstereo.android.ui;

import android.util.Log;

import su.thepeople.carstereo.lib.data.Album;
import su.thepeople.carstereo.lib.data.Band;
import su.thepeople.carstereo.lib.interthread.MusicControllerAPI;

/*
 * This class is meant to handle all direct user input.
 *
 * This class doesn't care what specific widget on which specific screen was involved.  It just
 * converts user requests into commands for either the main UI or the backend controller.
 */
public class UserInputHandler {

    private final MusicControllerAPI musicController;
    private final MainUI mainUI;
    private static final String LOG_ID = "User Input Handler";


    public UserInputHandler(MusicControllerAPI controller, MainUI mainUI) {
        this.musicController = controller;
        this.mainUI = mainUI;
    }

    /*
     * These methods are intended to be hooked up to UI widgets.
     *
     * Many UI widgets require a boolean return value (indicating whether or not the user event was
     * "handled".  Therefore, some of these methods return a true boolean, so that no wrapper is required.
     */
    public void bandModeToggleRequest() {
        Log.d(LOG_ID, "User requested band mode toggle");
        musicController.toggleBandMode();
    }

    public boolean bandChooserRequest() {
        Log.d(LOG_ID, "User requested list of bands");
        musicController.requestBandList();
        return true;
    }

    public void specificBandRequest(Band band) {
        Log.d(LOG_ID, String.format("User requested specific band %s:%s", band.getUid(), band.getName()));
        musicController.lockSpecificBand(band.getUid());
    }

    public void albumModeToggleRequest() {
        Log.d(LOG_ID, "User requested album mode toggle");
        musicController.toggleAlbumMode();
    }

    public boolean albumChooserRequest() {
        Log.d(LOG_ID, "User requested list of albums");
        musicController.requestAlbumList();
        return true;
    }

    public void specificAlbumRequest(Album album) {
        Log.d(LOG_ID, String.format("User requested specific album %s:%s", album.getUid(), album.getName()));
        musicController.lockSpecificAlbum(album.getUid());
    }

    public void yearModeToggleRequest() {
        Log.d(LOG_ID, "User requested year mode toggle");
        musicController.toggleYearMode();
    }

    public boolean yearChooserRequest() {
        Log.d(LOG_ID, "User requested list of years");
        musicController.requestYearList();
        return true;
    }

    public void specificYearRequest(int year) {
        Log.d(LOG_ID, String.format("User requested specific year %s", year));
        musicController.lockSpecificYear(year);
    }

    public void playPauseToggleRequest() {
        Log.d(LOG_ID, "User requested play/pause toggle");
        musicController.togglePlayPause();
    }

    public boolean skipBackwardRequest() {
        Log.d(LOG_ID, "User requested to skip backwards");
        musicController.skipBackward();
        return true;
    }

    public boolean restartSongRequest() {
        Log.d(LOG_ID, "User requested to restart song");
        musicController.restartCurrentSong();
        return true;
    }

    public boolean nextSongRequest() {
        Log.d(LOG_ID, "User requested to advance to the next song");
        musicController.nextSong();
        return true;
    }

    public boolean skipForwardRequest() {
        Log.d(LOG_ID, "User requested to skip forwards");
        musicController.skipForward();
        return true;
    }

    public boolean songNavigationRequest() {
        Log.d(LOG_ID, "User requested song navigation UI");
        mainUI.openSongNavigationUI();
        return true;
    }

    public boolean changeSubModeRequest() {
        Log.d(LOG_ID, "User requested change to sub-mode");
        musicController.changeSubMode();
        return true;
    }
}

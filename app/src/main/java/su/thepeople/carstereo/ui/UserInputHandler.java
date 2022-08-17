package su.thepeople.carstereo.ui;

import android.util.Log;

import su.thepeople.carstereo.interthread.MusicControllerAPI;
import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;

/*
 * This class is meant to handle all direct user input.
 *
 * This class doesn't care what specific widget on which specific screen was involved.  It just
 * converts user requests into commands for either the main UI or the backend controller.
 */
public class UserInputHandler {

    private final RecursionLock context;
    private final MusicControllerAPI musicController;
    private final MainUI mainUI;
    private static final String LOG_ID = "User Input Handler";


    public UserInputHandler(RecursionLock context, MusicControllerAPI controller, MainUI mainUI) {
        this.context = context;
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
        context.run(musicController::toggleBandMode);
    }

    public boolean bandChooserRequest() {
        Log.d(LOG_ID, "User requested list of bands");
        musicController.requestBandList();
        return true;
    }

    public void specificBandRequest(Band band) {
        Log.d(LOG_ID, String.format("User requested specific band %s:%s", band.uid, band.name));
        musicController.lockSpecificBand(band.uid);
    }

    public void albumModeToggleRequest() {
        Log.d(LOG_ID, "User requested album mode toggle");
        context.run(musicController::toggleAlbumMode);
    }

    public boolean albumChooserRequest() {
        Log.d(LOG_ID, "User requested list of albums");
        context.run(musicController::requestAlbumList);
        return true;
    }

    public void specificAlbumRequest(Album album) {
        Log.d(LOG_ID, String.format("User requested specific album %s:%s", album.uid, album.name));
        context.run(() -> musicController.lockSpecificAlbum(album.uid));
    }

    public void yearModeToggleRequest() {
        Log.d(LOG_ID, "User requested year mode toggle");
        context.run(musicController::toggleYearMode);
    }

    public boolean yearChooserRequest() {
        Log.d(LOG_ID, "User requested list of years");
        context.run(musicController::requestYearList);
        return true;
    }

    public void specificYearRequest(int year) {
        Log.d(LOG_ID, String.format("User requested specific year %s", year));
        context.run(() -> musicController.lockSpecificYear(year));
    }

    public void playPauseToggleRequest() {
        Log.d(LOG_ID, "User requested play/pause toggle");
        context.run(musicController::togglePlayPause);
    }

    public boolean skipBackwardRequest() {
        Log.d(LOG_ID, "User requested to skip backwards");
        context.run(musicController::skipBackward);
        return true;
    }

    public boolean restartSongRequest() {
        Log.d(LOG_ID, "User requested to restart song");
        context.run(musicController::restartCurrentSong);
        return true;
    }

    public boolean nextSongRequest() {
        Log.d(LOG_ID, "User requested to advance to the next song");
        context.run(musicController::nextSong);
        return true;
    }

    public boolean skipForwardRequest() {
        Log.d(LOG_ID, "User requested to skip forwards");
        context.run(musicController::skipForward);
        return true;
    }

    public boolean songNavigationRequest() {
        Log.d(LOG_ID, "User requested song navigation UI");
        mainUI.openSongNavigationUI();
        return true;
    }

    public boolean changeSubModeRequest() {
        Log.d(LOG_ID, "User requested change to sub-mode");
        context.run(musicController::changeSubMode);
        return true;
    }
}

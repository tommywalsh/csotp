package su.thepeople.carstereo.lib.interthread;

/**
 * This class defines the API through which other objects can send requests/commands to the Music Controller.
 *
 * The Music Controller is required to be run on its own thread, and that thread is where it does all of its work.
 *
 * Clients of this class can call any of the public methods from any thread. The call will automatically
 * be routed so that the request is serviced from the Music Controller's thread.
 */
@SuppressWarnings("CanBeFinal")
public abstract class MusicControllerAPI extends InterThreadAPI {

    protected MusicControllerAPI() {
        cb_playPause = registerCallback(this::onTogglePlayPause);
        cb_nextSong = registerCallback(this::onSkipAhead);
        cb_forcePause = registerCallback(this::onForcePause);
        cb_bandMode = registerCallback(this::onToggleBandMode);
        cb_albumMode = registerCallback(this::onToggleAlbumMode);
        cb_yearMode = registerCallback(this::onToggleYearMode);
        cb_lockSpecificBand = registerCallback(this::onLockSpecificBand);
        cb_lockSpecificAlbum = registerCallback(this::onLockSpecificAlbum);
        cb_lockSpecificYear = registerCallback(this::onLockSpecificYear);
        cb_requestBands = registerCallback(this::onRequestBandList);
        cb_requestAlbums = registerCallback(this::onRequestAlbumList);
        cb_requestYears = registerCallback(this::onRequestYearList);
        cb_restartCurrentSong = registerCallback(this::onRestartCurrentSong);
        cb_skipBackward = registerCallback(this::onSkipBackward);
        cb_skipForward = registerCallback(this::onSkipForward);
        cb_changeSubMode = registerCallback(this::onChangeSubMode);
    }

    // Pauses or unpauses the player.
    public void togglePlayPause() { callInterThread(cb_playPause); }

    // Methods to jump around in playlist
    public void restartCurrentSong() { callInterThread(cb_restartCurrentSong); }
    public void nextSong() {
        callInterThread(cb_nextSong);
    }
    public void skipBackward() { callInterThread(cb_skipBackward); }
    public void skipForward() { callInterThread(cb_skipForward); }

    // These methods toggle the various specialty modes on or off
    public void toggleBandMode() {
        callInterThread(cb_bandMode);
    }
    public void toggleAlbumMode() {
        callInterThread(cb_albumMode);
    }
    public void toggleYearMode() {
        callInterThread(cb_yearMode);
    }

    // Each specialty mode can define its own sub-modes. This method jumps to the next one.
    public void changeSubMode() { callInterThread(cb_changeSubMode); }

    // Ensures player is paused.
    public void forcePause() { callInterThread(cb_forcePause); }

    // "Locks" on the band specified (regardless of which band is currently playing)
    public void lockSpecificBand(long bandId) { callInterThread(cb_lockSpecificBand, bandId); }

    // "Locks" on the era specified (regardless of which year is currently playing)
    public void lockSpecificYear(int year) { callInterThread(cb_lockSpecificYear, year); }

    // "Locks" on the album specified (regardless of which album is currently playing)
    public void lockSpecificAlbum(long albumId) { callInterThread(cb_lockSpecificAlbum, albumId); }

    // Sends UI a list of all bands in the collection.
    public void requestBandList() { callInterThread(cb_requestBands); }

    // Sends UI a list of all albums by the currently-playing band.
    public void requestAlbumList() { callInterThread(cb_requestAlbums); }

    // Sends UI a list of the available years that may be locked on.
    public void requestYearList() { callInterThread(cb_requestYears); }

    // These methods are implements by the Music Controller itself, and will only ever be called on the controller's own thread.
    protected abstract void onTogglePlayPause();
    protected abstract void onSkipAhead();
    protected abstract void onForcePause();
    protected abstract void onToggleBandMode();
    protected abstract void onToggleAlbumMode();
    protected abstract void onToggleYearMode();
    protected abstract void onLockSpecificBand(long bandId);
    protected abstract void onLockSpecificAlbum(long albumId);
    protected abstract void onLockSpecificYear(int year);
    protected abstract void onRequestBandList();
    protected abstract void onRequestAlbumList();
    protected abstract void onRequestYearList();
    protected abstract void onRestartCurrentSong();
    protected abstract void onSkipBackward();
    protected abstract void onSkipForward();
    protected abstract void onChangeSubMode();

    private final int cb_playPause;
    private final int cb_nextSong;
    private final int cb_forcePause;
    private final int cb_bandMode;
    private final int cb_albumMode;
    private final int cb_yearMode;
    private final int cb_lockSpecificBand;
    private final int cb_lockSpecificAlbum;
    private final int cb_lockSpecificYear;
    private final int cb_requestBands;
    private final int cb_requestAlbums;
    private final int cb_requestYears;
    private final int cb_restartCurrentSong;
    private final int cb_skipBackward;
    private final int cb_skipForward;
    private final int cb_changeSubMode;
}

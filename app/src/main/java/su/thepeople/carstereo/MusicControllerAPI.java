package su.thepeople.carstereo;

/**
 * This class defines the API through with other objects can send requests/commands to the Music Controller.
 *
 * Clients of this class can call any of the non-private/protected methods from any thread. The call will automatically
 * be routed so that the request is serviced from the correct thread.
 */
@SuppressWarnings("CanBeFinal")
public abstract class MusicControllerAPI extends InterThreadAPI {

    protected MusicControllerAPI() {
        cb_playPause = registerCallback(this::onTogglePlayPause);
        cb_skip = registerCallback(this::onSkipAhead);
        cb_forcePause = registerCallback(this::onForcePause);
        cb_bandMode = registerCallback(this::onToggleBandMode);
        cb_albumMode = registerCallback(this::onToggleAlbumMode);
        cb_yearMode = registerCallback(this::onToggleYearMode);
        cb_replenish = registerCallback(this::onReplenishPlaylist);
        cb_lockSpecificBand = registerCallback(this::onLockSpecificBand, Long.class);
        cb_lockSpecificAlbum = registerCallback(this::onLockSpecificAlbum, Long.class);
        cb_lockSpecificEra = registerCallback(this::onLockSpecificEra, MainActivity.Era.class);
        cb_requestBands = registerCallback(this::onRequestBandList);
        cb_requestAlbums = registerCallback(this::onRequestAlbumList);
        cb_requestYears = registerCallback(this::onRequestYearList);
        cb_restartCurrentSong = registerCallback(this::onRestartCurrentSong);
        cb_restartCurrentAlbum = registerCallback(this::onRestartCurrentAlbum);
        cb_doubleShot = registerCallback(this::onToggleDoubleShotMode);
    }

    // Pauses or unpauses the player.
    public void togglePlayPause() { callInterThread(cb_playPause); }

    public void restartCurrentSong() { callInterThread(cb_restartCurrentSong); }

    public void restartCurrentAlbum() { callInterThread(cb_restartCurrentAlbum); }

    public void toggleDoubleShotMode() { callInterThread(cb_doubleShot); }

    // Moves ahead to next song.
    public void skipAhead() {
        callInterThread(cb_skip);
    }

    // Ensures player is paused.
    public void forcePause() { callInterThread(cb_forcePause); }

    // "Locks" on the currently-playing band.
    public void toggleBandMode() {
        callInterThread(cb_bandMode);
    }

    // "Locks" in the currently-playing album (if there is one).
    public void toggleAlbumMode() {
        callInterThread(cb_albumMode);
    }

    public void toggleYearMode() {
        callInterThread(cb_yearMode);
    }

    // Generates a new batch of songs and sends them to the music player.
    public void replenishPlaylist() {
        callInterThread(cb_replenish);
    }

    // "Locks" on the band specified (regardless of which band is currently playing)
    public void lockSpecificBand(long bandId) { callInterThread(cb_lockSpecificBand, bandId); }

    // "Locks" on the era specified (regardless of which ear is currently playing)
    public void lockSpecificEra(MainActivity.Era era) { callInterThread(cb_lockSpecificEra, era); }

    // "Locks" on the album specified (regardless of which album is currently playing)
    public void lockSpecificAlbum(long albumId) { callInterThread(cb_lockSpecificAlbum, albumId); }

    // Sends UI a list of all bands in the collection.
    public void requestBandList() { callInterThread(cb_requestBands); }

    // Sends UI a list of all albums by the currently-playing band.
    public void requestAlbumList() { callInterThread(cb_requestAlbums); }

    // Sends UI a list of the available years that may be locked on.
    public void requestYearList() { callInterThread(cb_requestYears); }

    protected abstract void onTogglePlayPause();
    protected abstract void onSkipAhead();
    protected abstract void onForcePause();
    protected abstract void onToggleBandMode();
    protected abstract void onToggleAlbumMode();
    protected abstract void onToggleYearMode();
    protected abstract void onReplenishPlaylist();
    protected abstract void onLockSpecificBand(long bandId);
    protected abstract void onLockSpecificAlbum(long albumId);
    protected abstract void onLockSpecificEra(MainActivity.Era era);
    protected abstract void onRequestBandList();
    protected abstract void onRequestAlbumList();
    protected abstract void onRequestYearList();
    protected abstract void onRestartCurrentSong();
    protected abstract void onRestartCurrentAlbum();
    protected abstract void onToggleDoubleShotMode();

    private final int cb_playPause;
    private final int cb_skip;
    private final int cb_forcePause;
    private final int cb_bandMode;
    private final int cb_albumMode;
    private final int cb_yearMode;
    private final int cb_replenish;
    private final int cb_lockSpecificBand;
    private final int cb_lockSpecificAlbum;
    private final int cb_lockSpecificEra;
    private final int cb_requestBands;
    private final int cb_requestAlbums;
    private final int cb_requestYears;
    private final int cb_restartCurrentSong;
    private final int cb_restartCurrentAlbum;
    private final int cb_doubleShot;
}

package su.thepeople.carstereo.lib.platform_interface;

import java.util.List;

import su.thepeople.carstereo.lib.data.SongInfo;

/**
 * This interface handles playing audio in whatever platform-specific way is required.
 */
public interface MusicPlayer {
    // Returns details about the currently-playing song (or if no song is playing, then the one that will be played next)
    SongInfo getCurrentSong();

    // Loads a list of songs to play. If replaceCurrent is true, then any existing playlist should be thrown away (including any currently-playing song)
    void setPlaylist(List<SongInfo> playlist, boolean replaceCurrent);

    // Do whatever tasks are necessary to get the next song ready to play.
    void prepareNextSong();

    // Start playing audio (if not already doing so)
    void play();

    // Pause playing audio (if not already paused)
    void pause();

    // Restart the currently-playing song from the beginning.
    void restartCurrent();
}

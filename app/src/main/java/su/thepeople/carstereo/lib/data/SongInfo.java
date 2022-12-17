package su.thepeople.carstereo.lib.data;

import su.thepeople.carstereo.lib.util.NonNull;
import su.thepeople.carstereo.lib.util.Nullable;

/**
 * Convenience class for representing all information about a song.
 */
public class SongInfo {
    @NonNull public final Band band;
    @Nullable public final Album album;
    @NonNull public final Song song;

    public SongInfo(@NonNull Band band, @NonNull Song song, @Nullable Album album) {
        this.band = band;
        this.song = song;
        this.album = album;
    }

    public SongInfo(@NonNull Band band, @NonNull Song song) {
        this.band = band;
        this.song = song;
        this.album = null;
    }
}

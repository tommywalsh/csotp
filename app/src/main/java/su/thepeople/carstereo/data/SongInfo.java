package su.thepeople.carstereo.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Song;

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

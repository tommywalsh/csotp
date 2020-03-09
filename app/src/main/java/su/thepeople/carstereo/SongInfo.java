package su.thepeople.carstereo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Song;

/**
 * Convenience class for representing all information about a song.
 */
public class SongInfo {
    @NonNull public Band band;
    @Nullable public Album album;
    @NonNull public Song song;

    SongInfo(@NonNull Band band, @NonNull Song song, @Nullable Album album) {
        this.band = band;
        this.song = song;
        this.album = album;
    }

    SongInfo(@NonNull Band band, @NonNull Song song) {
        this.band = band;
        this.song = song;
        this.album = null;
    }
}

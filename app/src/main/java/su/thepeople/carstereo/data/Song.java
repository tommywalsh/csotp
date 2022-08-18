package su.thepeople.carstereo.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Simple POJO type representing a Song, as backed by an on-disk file.
 */
@Entity
public class Song {
    @PrimaryKey(autoGenerate = true)
    public long uid;

    @NonNull
    public final String name;

    @NonNull
    public final String fullPath;

    @NonNull
    public final Long bandId;

    public final Long albumId;

    public final Integer year;

    public Song(@NonNull String name, @NonNull String fullPath, @NonNull Long bandId, @Nullable Long albumId, @Nullable Integer year) {
        this.name = name;
        this.fullPath = fullPath;
        this.bandId = bandId;
        this.albumId = albumId;
        this.year = year;
    }
}

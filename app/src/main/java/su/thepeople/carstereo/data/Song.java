package su.thepeople.carstereo.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Simple POJO type representing a Song, as backed by an on-disk file.
 */
@Entity
public class Song {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @NonNull
    public String name;

    @NonNull
    public String fullPath;

    @NonNull
    public Long bandId;

    public Long albumId;

    public Song(@NonNull String name, @NonNull String fullPath, @NonNull Long bandId, Long albumId) {
        this.name = name;
        this.fullPath = fullPath;
        this.bandId = bandId;
        this.albumId = albumId;
    }
}

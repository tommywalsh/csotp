package su.thepeople.carstereo.android.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Simple POJO type representing a Song, as backed by an on-disk file.
 */
@Entity
public class DBSong {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @NonNull
    private final String name;

    @NonNull
    private final String fullPath;

    @NonNull
    private final Long bandId;

    public final Long albumId;

    public final Integer year;

    public DBSong(@NonNull String name, @NonNull String fullPath, @NonNull Long bandId, @Nullable Long albumId, @Nullable Integer year) {
        this.name = name;
        this.fullPath = fullPath;
        this.bandId = bandId;
        this.albumId = albumId;
        this.year = year;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getFullPath() {
        return fullPath;
    }

    @NonNull
    public Long getBandId() {
        return bandId;
    }

    public final Long getAlbumId() {
        return albumId;
    }

    public final Integer getYear() {
        return year;
    }
}

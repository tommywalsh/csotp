package su.thepeople.carstereo.lib.data;

import su.thepeople.carstereo.lib.util.NonNull;
import su.thepeople.carstereo.lib.util.Nullable;

public class Song {

    private final long uid;
    @NonNull private final String name;
    @NonNull private final String fullPath;
    private final long bandId;
    @Nullable private final Long albumId;
    @Nullable private final Integer year;

    public Song(long uid, @NonNull String name, @NonNull String fullPath, long bandId, @Nullable Long albumId, @Nullable Integer year) {
        this.uid = uid;
        this.name = name;
        this.fullPath = fullPath;
        this.bandId = bandId;
        this.albumId = albumId;
        this.year = year;
    }

    public long getUid() {
        return uid;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getFullPath() {
        return fullPath;
    }

    public long getBandId() {
        return bandId;
    }

    @Nullable
    public Long getAlbumId() {
        return albumId;
    }

    @Nullable
    public Integer getYear() {
        return year;
    }
}

package su.thepeople.carstereo.lib.data;


import java.io.Serializable;

import su.thepeople.carstereo.lib.util.NonNull;
import su.thepeople.carstereo.lib.util.Nullable;

public class Album implements Serializable {

    private final long uid;

    @NonNull
    private final String name;

    private final long bandId;

    @Nullable
    private final Integer year;

    public Album(long uid, @NonNull String name, long bandId, @Nullable Integer year) {
        this.uid = uid;
        this.name = name;
        this.bandId = bandId;
        this.year = year;
    }

    public long getUid() {
        return uid;
    }

    @Nullable
    public Integer getYear() {
        return year;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public long getBandId() {
        return bandId;
    }
}

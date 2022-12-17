package su.thepeople.carstereo.lib.data;


import java.io.Serializable;

import su.thepeople.carstereo.lib.util.NonNull;

public class Band implements Serializable {

    private final long uid;

    private final String name;

    public Band(long uid, @NonNull String name) {
        this.uid = uid;
        this.name = name;
    }

    public long getUid() {
        return uid;
    }

    @NonNull
    public String getName() {
        return name;
    }
}

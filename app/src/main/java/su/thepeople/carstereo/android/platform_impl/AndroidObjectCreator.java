package su.thepeople.carstereo.android.platform_impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import su.thepeople.carstereo.android.database.AndroidDatabase;
import su.thepeople.carstereo.android.database.DBAlbum;
import su.thepeople.carstereo.android.database.DBBand;
import su.thepeople.carstereo.android.database.DBSong;
import su.thepeople.carstereo.lib.platform_interface.ObjectCreator;

/**
 * This class handles object creation via our AndroidDatabase object, which persists to an sqlite database.
 */
public class AndroidObjectCreator implements ObjectCreator {
    private final AndroidDatabase database;

    public AndroidObjectCreator(AndroidDatabase database) {
        this.database = database;
    }

    public long createBand(String bandName) {
        return database.bandDAO().insert(new DBBand(bandName));
    }

    public long createAlbum(@NonNull String name, long bandId, @Nullable Integer year) {
        DBAlbum newAlbum = new DBAlbum(name, bandId, year);
        return database.albumDAO().insert(newAlbum);
    }

    public long createSong(@NonNull String name, @NonNull String fullPath, long bandId, @Nullable Long albumId, @Nullable Integer year) {
        DBSong newSong = new DBSong(name, fullPath, bandId, albumId, year);
        return database.songDAO().insert(newSong);
    }
}

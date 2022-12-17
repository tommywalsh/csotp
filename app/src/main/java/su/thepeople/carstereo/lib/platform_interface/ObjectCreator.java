package su.thepeople.carstereo.lib.platform_interface;

import su.thepeople.carstereo.lib.util.NonNull;
import su.thepeople.carstereo.lib.util.Nullable;

/**
 * Interface to create new Java-accessible objects representing bands/albums/songs in the collection.
 */
public interface ObjectCreator {

    // Returns unique ID for the newly-created Band object with the given name
    long createBand(String bandName);

    // Returns unique ID for the newly-created Album with the given name and band (and optionally, year of release)
    long createAlbum(@NonNull String name, long bandId, @Nullable Integer year);

    // Returns unique ID for the newly-created Album with the given name, band, and disk location (and optionally, album and year of release)
    long createSong(@NonNull String name, @NonNull String fullPath, long bandId, @Nullable Long albumId, @Nullable Integer year);
}

package su.thepeople.carstereo.lib.platform_interface;

import java.util.List;

import su.thepeople.carstereo.lib.data.Album;

/**
 * This interface handles lookup of album(s) based on certain criteria
 */
public interface AlbumFetcher {

    // Returns all albums for the given band
    List<Album> getAllForBand(long bandId);

    // Returns the album with the given ID
    Album lookup(long albumId);

    // Returns any album, chosen at random.
    Album getRandom();
}

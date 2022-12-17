package su.thepeople.carstereo.lib.platform_interface;

import java.util.List;

import su.thepeople.carstereo.lib.data.Band;

/**
 * This interface handles lookup of band(s) based on certain criteria
 */
public interface BandFetcher {
    // Returns a list of all bands in the collection.
    List<Band> getAll();

    // Returns the band associated with the given id.
    Band lookup(long bandId);

    // Returns any band, chosen at random.
    Band getRandom();
}

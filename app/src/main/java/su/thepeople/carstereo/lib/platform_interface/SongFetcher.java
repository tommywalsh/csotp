package su.thepeople.carstereo.lib.platform_interface;

import java.util.List;

import su.thepeople.carstereo.lib.data.Song;

/**
 * This interface handles lookup of song(s) based on certain criteria
 */
public interface SongFetcher {

    // Returns a list of all songs for the given band, in a random order.
    List<Song> getAllForBandShuffled(Long bandId);

    // Returns a list of all songs for the given band, sorted first chronologically, then by album position.
    List<Song> getAllForBandOrdered(Long bandId);

    // Returns a randomly-selected set of songs for the given band. No more than `maxSize` songs should be returned.
    List<Song> getSomeForBand(Long bandId, Integer maxSize);

    // Returns a list of all songs for the given album, in the same order as they appear on the album
    List<Song> getAllForAlbum(Long albumId);

    // Returns a list of randomly-selected songs, by any band, on any (or no) album. No more than `maxSize` songs should be returned.
    List<Song> getRandomBatch(int batchSize);

    // Returns a list of all calendar years for which we have songs.
    List<Integer> getYears();

    // Returns a list of randomly-selected songs, by any band, on any (or no) album, that were released within the given time period.
    List<Song> getRandomBatchForEra(int startYear, int endYear, int batchSize);
}

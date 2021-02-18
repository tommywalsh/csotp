package su.thepeople.carstereo;

import android.util.Log;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Database;
import su.thepeople.carstereo.data.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A simple interface to grab a batch of songs. The caller does not need to care about what rules are being used to
 * generate the batch.
 */
public abstract class SongProvider {

    private Database database;
    private static final String LOG_ID = "Song Provider";

    SongProvider(Database database) {
        this.database = database;
    }

    Database getDatabase() {
        return database;
    }

    public abstract List<Song> getNextBatch();

    /**
     * Specialization for "shuffle mode". Each song is randomly selected from the collection.
     */
    public static class ShuffleProvider extends SongProvider {
        private static final int BATCH_SIZE = 10;

        ShuffleProvider(Database database) {
            super(database);
        }

        public List<Song> getNextBatch() {
            Log.d(LOG_ID, String.format("Getting next batch of %d random songs", BATCH_SIZE));
            return getDatabase().songDAO().getRandomBatch(BATCH_SIZE);
        }
    }

    /**
     * Specialization to randomly select songs from a particular time period.
     */
    public static class EraProvider extends SongProvider {
        private static final int BATCH_SIZE = 10;
        private final int firstYear;
        private final int lastYear;

        EraProvider(Database database, int firstYear, int lastYear) {
            super(database);
            this.firstYear = firstYear;
            this.lastYear = lastYear;
        }

        public List<Song> getNextBatch() {
            List<Song> batch = new ArrayList<>();
            for (int i = 0; i < BATCH_SIZE; i++) {
                Album album = getDatabase().albumDAO().getRandomForEra(firstYear, lastYear);
                if (album != null) {
                    List<Song> songs = getDatabase().songDAO().getRandomFromAlbum(album.uid);
                    if (songs != null && !songs.isEmpty()) {
                        batch.add(songs.get(0));
                    }
                }
            }
            return batch;
        }
    }

    /**
     * Double-Shot Weekend. We pick a band at random, play two random songs, then move to the next random band, etc.
     */
    public static class DoubleShotProvider extends SongProvider {
        long nextBandId;
        int batchSize;

        DoubleShotProvider(Database database, Optional<Band> startingBand) {
            super(database);
            nextBandId = startingBand.map(b -> b.uid).orElse(getRandomBand());
            batchSize = startingBand.map(band -> 1).orElse(2);
        }

        private long getRandomBand() {
            // We have three different strategies for choosing a band. We "average" them by randomizing.
            double randomTrinary = Math.random() * 3;
            if (randomTrinary < 1.0) {
                /*
                 * Choose uniformly from all bands. This biases the song choices towards songs by bands with only a
                 * small number of songs in the collection. For example, say we have 1 song by band A and 100 songs by
                 * band B. We will choose band A with the same frequency we choose band B. Therefore A's single song
                 * is 100 times more likely to be chosen than each of B's songs.
                 */
                return getRandomBandUnweighted();
            } else if (randomTrinary < 2.0) {
                /*
                 * Choose a random song, then choose that band. This biases the song choices towards songs by bands
                 * with a large number of songs in the collection. For example, say band A's albums each have 21 short
                 * songs on them, whereas band B's albums have 7 long songs. Even if we have the same quantity of music,
                 * we'll choose A three times more often than B. This will roughly equalize the time spent listening to
                 * A and B, but not the number of songs.
                 */
                return getRandomBandWeightedBySong();
            } else {
                /*
                 * Finally, choose a random album, then choose that band. This biases the selection towards bands with
                 * lots of albums in the collection. This partially counteracts the worst of the biases in the other
                 * two strategies.
                 */
                return getRandomBandWeightedByAlbum();
            }
        }

        private long getRandomBandUnweighted() {
            return getDatabase().bandDAO().getRandom().uid;
        }

        private long getRandomBandWeightedBySong() {
            List<Song> singleSongList = getDatabase().songDAO().getRandomBatch(1);
            return singleSongList.get(0).bandId;
        }

        private long getRandomBandWeightedByAlbum() {
            Album album = getDatabase().albumDAO().getRandom();
            return album.bandId;
        }

        public List<Song> getNextBatch() {
            Log.d(LOG_ID, String.format("Using band %s for this double shot", nextBandId));
            long thisBandId = nextBandId;
            List<Song> songs = getDatabase().songDAO().getSomeForBand(thisBandId, batchSize);
            nextBandId = getRandomBand();
            batchSize = 2;
            return songs;
        }
    }

    /**
     * Specialization for "band mode". Each song is randomly selected from all of the band's songs.
     */
    public static class BandProvider extends SongProvider {
        private long bandId;

        BandProvider(Database database, long bandId) {
            super(database);
            this.bandId = bandId;
        }

        public List<Song> getNextBatch() {
            Log.d(LOG_ID, String.format("Getting all songs for band %d", bandId));
            return getDatabase().songDAO().getAllForBand(bandId);
        }
    }

    /**
     * Specialization for "album mode". Songs continue playing in album order, starting with the one AFTER the specified
     * song, and then "wrapping around" to the beginning of the album.
     */
    public static class AlbumProvider extends SongProvider {
        private long albumId;
        // This will be set only for the first batch.
        private Optional<Long> previousSongId;

        AlbumProvider(Database database, long albumId) {
            super(database);
            this.albumId = albumId;
            this.previousSongId = Optional.empty();  // empty here means start from the first song.
        }
        AlbumProvider(Database database, long albumId, long previousSongId) {
            super(database);
            this.albumId = albumId;
            this.previousSongId = Optional.of(previousSongId);
        }

        /**
         * Helper method to pop a song from the front of a list.
         */
        private Optional<Song> popSong(List<Song> list) {
            if (list.isEmpty()) {
                return Optional.empty();
            } else {
                Song song = list.get(0);
                list.remove(0);
                return Optional.of(song);
            }
        }

        public List<Song> getNextBatch() {
            Log.d(LOG_ID, String.format("Getting all songs for band %d", albumId));

            // This list will contain the full album from start to finish.
            final List<Song> list = getDatabase().songDAO().getAllForAlbum(albumId);

            /*
             * On the first trip through, we might need to prune the beginning of the list, so that we continue playing
             * the album AFTER the initially-specified song. So, here we loop until we've popped off the song that
             * matches. The remaining list will be the remainder of the album.
             */
            previousSongId.ifPresent(songId -> {
                Log.v(LOG_ID, String.format("Skipping forward so we start after song %d", songId));
                Optional<Song> song = popSong(list);
                while (song.isPresent()) {
                    if (song.get().uid == songId) {
                        break;
                    }
                    Log.v(LOG_ID, String.format("Skipping song %d", song.get().uid));
                    song = popSong(list);
                }
            });

            // Make sure we don't do any pruning on the next batch -- we want to return the whole album.
            previousSongId = Optional.empty();

            // If the specified song was the last one on the album, then we threw away the whole list! Start fresh.
            if (list.isEmpty()) {
                Log.v(LOG_ID, "We skipped the entire album! Start again");
                return getDatabase().songDAO().getAllForAlbum(albumId);
            }
            return list;
        }
    }
}

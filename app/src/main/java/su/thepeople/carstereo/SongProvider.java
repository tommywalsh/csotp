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

    private final Database database;
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
            Log.d(LOG_ID, String.format("Getting next batch of %d random songs between %d and %d", BATCH_SIZE, firstYear, lastYear));
            return getDatabase().songDAO().getRandomBatchForEra(firstYear, lastYear, BATCH_SIZE);
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
     * Block Party Weekend. This shuffles the entire collection, but every so often will play a few
     * songs in a row by the same band.
     */
    public static class BlockPartyProvider extends SongProvider {
        private static final int BLOCK_PARTY_SIZE = 5;

        BlockPartyProvider(Database database) {
            super(database);
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
            List<Song> songs = new ArrayList<>();
            songs.addAll(getBatchBlock());
            songs.addAll(getShuffleBlock());
            return songs;
        }

        private List<Song> getBatchBlock() {
            long bandId = getRandomBand();
            Log.d(LOG_ID, String.format("Using band %s for block party", bandId));
            return getDatabase().songDAO().getSomeForBand(bandId, BLOCK_PARTY_SIZE);
        }

        private List<Song> getShuffleBlock() {
            return getDatabase().songDAO().getRandomBatch(BLOCK_PARTY_SIZE);
        }
    }

    /**
     * Specialization for "band mode". Each song is randomly selected from all of the band's songs.
     */
    public static class BandShuffleProvider extends SongProvider {
        private final long bandId;

        BandShuffleProvider(Database database, long bandId) {
            super(database);
            this.bandId = bandId;
        }

        public List<Song> getNextBatch() {
            Log.d(LOG_ID, String.format("Getting all songs for band %d", bandId));
            return getDatabase().songDAO().getAllForBandShuffled(bandId);
        }
    }

    public static class BandSequentialProvider extends SongProvider {
        private final long bandId;
        List<Song> playlist;

        BandSequentialProvider(Database database, long bandId, Optional<Long> previousSongId, boolean keepSong) {
            super(database);
            this.bandId = bandId;
            initializePlaylist(previousSongId, keepSong);
        }

        private void initializePlaylist(Optional<Long> maybePreviousSongId, boolean keepSong) {
            List<Song> bandSongsInOrder = getDatabase().songDAO().getAllForBandOrdered(bandId);
            playlist = bandSongsInOrder;
            maybePreviousSongId.ifPresent(songId -> playlist = Utils.splitList(bandSongsInOrder, songId, keepSong, s -> s.uid));
        }

        public List<Song> getNextBatch() {
            // This provider should only give a single batch of songs once.
            List<Song> returnValue = new ArrayList<>(playlist);
            playlist.clear();
            return returnValue;
        }

        public static BandSequentialProvider atAlbumStart(Database database, Album album) {
            List<Song> albumSongs = database.songDAO().getAllForAlbum(album.uid);
            if (albumSongs.isEmpty()) {
                return new BandSequentialProvider(database, album.bandId, Optional.empty(), false);
            } else {
                return new BandSequentialProvider(database, album.bandId, Optional.of(albumSongs.get(0).uid), true);
            }
        }
    }

    /**
     * Specialization for "album mode". Songs continue playing in album order, starting with the one AFTER the specified
     * initial song, and then "wrapping around" to the beginning of the album. If no initial song is specified, then we
     * start from the beginning of the album.
     *
     * After each song plays once (regardless of where we started), the provider will refuse to provide any new songs.
     */
    public static class AlbumProvider extends SongProvider {
        private final long albumId;

        List<Song> playlist;

        AlbumProvider(Database database, long albumId, Optional<Long> previousSongId) {
            super(database);
            this.albumId = albumId;
            initializePlaylist(previousSongId);
        }

        private void initializePlaylist(Optional<Long> maybePreviousSongId) {
            List<Song> albumSongsInOrder = getDatabase().songDAO().getAllForAlbum(albumId);
            playlist = albumSongsInOrder;
            maybePreviousSongId.ifPresent(songId -> playlist = Utils.splitList(albumSongsInOrder, songId, false, s -> s.uid));
        }

        public List<Song> getNextBatch() {
            // This provider should only give a single batch of songs once.
            List<Song> returnValue = new ArrayList<>(playlist);
            playlist.clear();
            return returnValue;
        }
    }
}

package su.thepeople.carstereo;

import su.thepeople.carstereo.data.Database;
import su.thepeople.carstereo.data.Song;

import java.util.List;
import java.util.Optional;

/**
 * A simple interface to grab a batch of songs. The caller does not need to care about what rules are being used to
 * generate the batch.
 */
public abstract class SongProvider {

    private Database database;

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
        ShuffleProvider(Database database) {
            super(database);
        }

        public List<Song> getNextBatch() {
            return getDatabase().songDAO().getRandomBatch(10);
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

            // This list will contain the full album from start to finish.
            final List<Song> list = getDatabase().songDAO().getAllForAlbum(albumId);

            /*
             * On the first trip through, we might need to prune the beginning of the list, so that we continue playing
             * the album AFTER the initially-specified song. So, here we loop until we've popped off the song that
             * matches. The remaining list will be the remainder of the album.
             */
            previousSongId.ifPresent(songId -> {
                Optional<Song> song = popSong(list);
                while (song.isPresent()) {
                    if (song.get().uid == songId) {
                        break;
                    }
                    song = popSong(list);
                }
            });

            // Make sure we don't do any pruning on the next batch -- we want to return the whole album.
            previousSongId = Optional.empty();

            // If the specified song was the last one on the album, then we threw away the whole list! Start fresh.
            if (list.isEmpty()) {
                return getDatabase().songDAO().getAllForAlbum(albumId);
            }
            return list;
        }
    }
}

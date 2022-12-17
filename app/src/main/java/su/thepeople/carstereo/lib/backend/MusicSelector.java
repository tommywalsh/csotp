package su.thepeople.carstereo.lib.backend;

import su.thepeople.carstereo.R;
import su.thepeople.carstereo.lib.data.Album;
import su.thepeople.carstereo.lib.platform_interface.PlatformAdapter;
import su.thepeople.carstereo.lib.data.SongInfo;

import java.util.List;
import java.util.Optional;

/**
 * This file implements the logic of the various strategies for selecting music.  There are four
 *
 * - CollectionMode is the default. It plays random tracks chosen from the entire collection.
 * - AlbumMode plays a single album, in order.
 * - BandMode plays songs from a single band.
 * - YearMode plays songs from a single year (or other timespan)
 *
 * Each of these modes can define its own set of submodes to give custom behavior (see below)
 */
public abstract class MusicSelector {
    // These abstract methods are the interface used by the MusicController.
    public abstract int getSubModeIDString();
    public abstract MusicControllerThread.PlayModeEnum getModeType();
    public abstract boolean changeSubMode(SongInfo currentSongInfo); // returns false if no change
    public abstract boolean resyncBackward(SongInfo currentSong); // false if no change
    public abstract boolean resyncForward(SongInfo currentSong); // false if no change

    private SongProvider songProvider;
    private final PlatformAdapter database;

    public SongProvider getSongProvider() {
        return songProvider;
    }

    protected void setSongProvider(SongProvider songProvider) {
        this.songProvider = songProvider;
    }

    protected PlatformAdapter getDatabase() {
        return database;
    }

    protected MusicSelector(SongProvider songProvider, PlatformAdapter database) {
        this.songProvider = songProvider;
        this.database = database;
    }

    public static class CollectionMode extends MusicSelector {
        /**
         * The default mode is to pick songs randomly from the whole collection. But we also have:
         *  "double shot" -> Plays songs in groups of 2 by the same band
         *  "block party" -> Occasionally plays a block of songs by the same band
         */
        enum SubMode {
            FULL_SHUFFLE,
            DOUBLE_SHOT,
            BLOCK_PARTY
        }

        private SubMode subMode;

        public CollectionMode(PlatformAdapter database) {
            super(new SongProvider.ShuffleProvider(database), database);
            subMode = SubMode.FULL_SHUFFLE;
        }

        @Override
        public MusicControllerThread.PlayModeEnum getModeType() { return MusicControllerThread.PlayModeEnum.SHUFFLE; }

        public boolean changeSubMode(SongInfo currentSongInfo) {
            switch(subMode) {
                case FULL_SHUFFLE:
                    setSongProvider(new SongProvider.DoubleShotProvider(getDatabase(), Optional.of(currentSongInfo.band)));
                    subMode = SubMode.DOUBLE_SHOT;
                    break;
                case DOUBLE_SHOT:
                    setSongProvider(new SongProvider.BlockPartyProvider(getDatabase()));
                    subMode = SubMode.BLOCK_PARTY;
                    break;
                default:
                    assert subMode == SubMode.BLOCK_PARTY;
                    setSongProvider(new SongProvider.ShuffleProvider(getDatabase()));
                    subMode = SubMode.FULL_SHUFFLE;
            }
            return true;
        }

        public int getSubModeIDString() {
            switch(subMode) {
                case FULL_SHUFFLE:
                    return R.string.empty;
                case DOUBLE_SHOT:
                    return R.string.double_shot;
                default:
                    assert subMode == SubMode.BLOCK_PARTY;
                    return R.string.block_party;
            }
        }

        @Override
        public boolean resyncBackward(SongInfo currentSong) { return false; }

        @Override
        public boolean resyncForward(SongInfo currentSong) { return false; }
    }

    public static class AlbumMode extends MusicSelector {

        private final long albumId;

        public AlbumMode(PlatformAdapter database, long albumId, Optional<Long> currentSongId) {
            super(new SongProvider.AlbumProvider(database, albumId, currentSongId), database);
            this.albumId = albumId;
        }

        @Override
        public MusicControllerThread.PlayModeEnum getModeType() { return MusicControllerThread.PlayModeEnum.ALBUM; }

        @Override
        public boolean changeSubMode(SongInfo currentSongInfo) { return false; }

        @Override
        public int getSubModeIDString() {
            return R.string.empty;
        }

        @Override
        public boolean resyncBackward(SongInfo currentSong) {
            setSongProvider(new SongProvider.AlbumProvider(getDatabase(), albumId, Optional.empty()));
            return true;
        }

        @Override
        public boolean resyncForward(SongInfo currentSong) { return false; }
    }

    public static class YearMode extends MusicSelector {
        /*
         * Buy default, this class plays songs from a single year.
         * However, another submode tweaks this to play songs from a single decade.
         */
        private boolean decadeMode = false;
        private int year;

        public YearMode(PlatformAdapter database, int year) {
            super(new SongProvider.EraProvider(database, year, year), database);
            this.year = year;
        }

        private static int firstYearOfDecade(int year) {
            return (year / 10) * 10;  // use integer division to discard ones position
        }

        @Override
        public MusicControllerThread.PlayModeEnum getModeType() { return MusicControllerThread.PlayModeEnum.YEAR; }

        @Override
        public boolean changeSubMode(SongInfo currentSongInfo) {
            Integer boxedYear = currentSongInfo.song.getYear();
            if (boxedYear != null) {
                int year = boxedYear;
                if (decadeMode) {
                    setSongProvider(new SongProvider.EraProvider(getDatabase(), year, year));
                } else {
                    int first = firstYearOfDecade(year);
                    setSongProvider(new SongProvider.EraProvider(getDatabase(), first, first + 9));
                }
                decadeMode = !decadeMode;
                return true;
            }
            return false;
        }

        @Override
        public int getSubModeIDString() {
            return decadeMode ? R.string.decade : R.string.empty;
        }

        @Override
        public boolean resyncBackward(SongInfo currentSong) {
            if (decadeMode) {
                year -= 10;
                int first = firstYearOfDecade(year);
                setSongProvider(new SongProvider.EraProvider(getDatabase(), first, first + 9));
            } else {
                year--;
                setSongProvider(new SongProvider.EraProvider(getDatabase(), year, year));
            }
            return true;
        }

        @Override
        public boolean resyncForward(SongInfo currentSong) {
            if (decadeMode) {
                year += 10;
                int first = firstYearOfDecade(year);
                setSongProvider(new SongProvider.EraProvider(getDatabase(), first, first + 9));
            } else {
                year++;
                setSongProvider(new SongProvider.EraProvider(getDatabase(), year, year));
            }
            return true;
        }
    }

    public static class BandMode extends MusicSelector {
        /*
         * By default, we pick random songs from the same band.
         * However, another submode allows for playing all of a band's songs in sequential order.
         */
        private boolean isShuffle = true;

        public BandMode(PlatformAdapter database, long bandId) {
            super(new SongProvider.BandShuffleProvider(database, bandId), database);
        }

        @Override
        public MusicControllerThread.PlayModeEnum getModeType() { return MusicControllerThread.PlayModeEnum.BAND; }

        @Override
        public boolean changeSubMode(SongInfo currentSongInfo) {
            if (isShuffle) {
                setSongProvider(new SongProvider.BandSequentialProvider(getDatabase(), currentSongInfo.band.getUid(), Optional.of(currentSongInfo.song.getUid()), false));
            } else {
                setSongProvider(new SongProvider.BandShuffleProvider(getDatabase(), currentSongInfo.band.getUid()));
            }
            isShuffle = !isShuffle;
            return true;
        }

        public int getSubModeIDString() {
            return isShuffle ? R.string.empty : R.string.sequential;
        }

        @Override
        public boolean resyncBackward(SongInfo currentSong) {
            if (!isShuffle) {
                if (currentSong.album != null) {
                    setSongProvider(SongProvider.BandSequentialProvider.atAlbumStart(getDatabase(), currentSong.album));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean resyncForward(SongInfo currentSong) {
            // TODO: This query is slow and can cause a noticeable lag. Look into caching the list of albums
            // at start time.
            if (!isShuffle) {
                if (currentSong.album != null) {
                    List<? extends Album> albums = getDatabase().getAlbumFetcher().getAllForBand(currentSong.band.getUid());
                    boolean foundAlbum = false;
                    for (Album album : albums) {
                        if (foundAlbum) {
                            setSongProvider(SongProvider.BandSequentialProvider.atAlbumStart(getDatabase(), album));
                            return true;
                        } else if (album.getUid() == currentSong.album.getUid()) {
                            foundAlbum = true;
                        }
                    }
                }
            }
            return false;
        }
    }
}

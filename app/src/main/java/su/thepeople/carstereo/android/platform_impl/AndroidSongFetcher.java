package su.thepeople.carstereo.android.platform_impl;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import su.thepeople.carstereo.android.database.DBSong;
import su.thepeople.carstereo.android.database.DBSongDAO;
import su.thepeople.carstereo.lib.data.Song;
import su.thepeople.carstereo.lib.platform_interface.SongFetcher;

/**
 * Android-specific methods for looking up Songs in our sqlite database.
 */
public class AndroidSongFetcher implements SongFetcher {

    private final DBSongDAO dbDao;

    public AndroidSongFetcher(DBSongDAO dbDao) {
        this.dbDao = dbDao;
    }

    @Nullable
    private static Song fromDB(DBSong song) {
        if (song == null) {
            return null;
        } else {
            return new Song(song.getUid(), song.getName(), song.getFullPath(), song.getBandId(), song.getAlbumId(), song.getYear());
        }
    }

    private static List<Song> fromDBs(Supplier<List<DBSong>> supplier) {
        return supplier.get().stream().map(AndroidSongFetcher::fromDB).collect(Collectors.toList());
    }

    public List<Song> getAllForBandShuffled(Long bandId) {
        return fromDBs(() -> dbDao.getAllForBandShuffled(bandId));
    }

    public List<Song> getAllForBandOrdered(Long bandId) {
        return fromDBs(() -> dbDao.getAllForBandOrdered(bandId));
    }

    public List<Song> getSomeForBand(Long bandId, Integer maxSize) {
        return fromDBs(() -> dbDao.getSomeForBand(bandId, maxSize));
    }

    public List<Song> getAllForAlbum(Long albumId) {
        return fromDBs(() -> dbDao.getAllForAlbum(albumId));
    }

    public List<Song> getRandomBatch(int batchSize) {
        return fromDBs(() -> dbDao.getRandomBatch(batchSize));
    }

    public List<Integer> getYears() {
        return dbDao.getYears();
    }

    public List<Song> getRandomBatchForEra(int startYear, int endYear, int batchSize) {
        return fromDBs(() -> dbDao.getRandomBatchForEra(startYear, endYear, batchSize));
    }
}

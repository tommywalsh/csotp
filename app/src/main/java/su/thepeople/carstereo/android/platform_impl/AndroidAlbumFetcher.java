package su.thepeople.carstereo.android.platform_impl;

import androidx.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

import su.thepeople.carstereo.android.database.DBAlbum;
import su.thepeople.carstereo.android.database.DBAlbumDAO;
import su.thepeople.carstereo.lib.data.Album;
import su.thepeople.carstereo.lib.platform_interface.AlbumFetcher;

/**
 * Android-specific methods for looking up Albums in our sqlite database.
 */
public class AndroidAlbumFetcher implements AlbumFetcher {

    private final DBAlbumDAO dbDao;

    public AndroidAlbumFetcher(DBAlbumDAO dbDao) {
        this.dbDao = dbDao;
    }

    @Nullable
    private static Album fromDB(@Nullable DBAlbum dbBand) {
        if (dbBand == null) {
            return null;
        } else {
            return new Album(dbBand.getUid(), dbBand.getName(), dbBand.getBandId(), dbBand.getYear());
        }
    }

    public List<Album> getAllForBand(long bandId) {
        return dbDao.getAllForBand(bandId).stream().map(AndroidAlbumFetcher::fromDB).collect(Collectors.toList());
    }

    @Nullable
    public Album lookup(long albumId) {
        return fromDB(dbDao.lookup(albumId));
    }

    @Nullable
    public Album getRandom() {
        return fromDB(dbDao.getRandom());
    }

}

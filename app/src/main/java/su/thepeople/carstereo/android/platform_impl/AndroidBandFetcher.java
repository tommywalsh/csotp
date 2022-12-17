package su.thepeople.carstereo.android.platform_impl;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import su.thepeople.carstereo.android.database.DBBand;
import su.thepeople.carstereo.android.database.DBBandDAO;
import su.thepeople.carstereo.lib.data.Band;
import su.thepeople.carstereo.lib.platform_interface.BandFetcher;

/**
 * Android-specific methods for looking up Bands in our sqlite database.
 */
public class AndroidBandFetcher implements BandFetcher {

    private final DBBandDAO dbDao;

    @Nullable
    private static Band fromDB(@Nullable DBBand dbBand) {
        if (dbBand == null) {
            return null;
        } else {
            return new Band(dbBand.uid, dbBand.name);
        }
    }

    public AndroidBandFetcher(DBBandDAO dbDao) {
        this.dbDao = dbDao;
    }

    public List<Band> getAll() {
        return dbDao.getAll().stream().map(AndroidBandFetcher::fromDB).collect(Collectors.toList());
    }

    public Band lookup(long bandId) {
        return fromDB(dbDao.lookup(bandId));
    }

    public Band getRandom() {
        return fromDB(dbDao.getRandom());
    }
}

package su.thepeople.carstereo;

import android.util.Log;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Database;
import su.thepeople.carstereo.data.Song;

import java.io.File;
import java.nio.file.Path;

/**
 * Somewhat hacky class to scan for on-disk music.
 *
 * This class makes all sorts of assumptions about how the music files are laid out. Some of them are reasonable, and
 * some are totally unreasonable.
 *
 */
public class MusicScanner {

    private Database database;

    public MusicScanner(Database database) {
        this.database = database;
    }

    /*
     * This function must not be run on the UI thread!
     */
    private static String LOG_TAG = "Music Scanner";
    private void scanSdcard(File sdcardRoot) {
        Log.d(LOG_TAG, String.format("Scanning %s", sdcardRoot.getAbsolutePath()));
        Path mcotpRootPath = sdcardRoot.toPath().resolve("mcotp");
        File mcotpRoot = mcotpRootPath.toFile();
        if (mcotpRoot.isDirectory()) {
            for (File item : mcotpRoot.listFiles()) {
                if (item.isDirectory() && !item.getName().startsWith("[")) {
                    Log.d(LOG_TAG, String.format("Found band %s", item.getName()));
                    Band newBand = new Band(item.getName());
                    long bandID = database.bandDAO().insert(newBand);
                    scanBandDir(bandID, item);
                }
            }
        }
    }
    private void scanBandDir(long bandID, File bandDir) {
        for (File item: bandDir.listFiles()) {
            if (!item.getName().startsWith("[")) {
                if (item.isDirectory()) {
                    Log.d(LOG_TAG, String.format("Found album %s", item.getName()));
                    Album newAlbum = new Album(item.getName(), bandID);
                    long albumID = database.albumDAO().insert(newAlbum);
                    scanAlbumDir(bandID, albumID, item);
                } else if (item.isFile()) {
                    Log.d(LOG_TAG, String.format("Found loose song %s", item.getName()));
                    Song newSong = new Song(item.getName(), item.getAbsolutePath(), bandID, null);
                    database.songDAO().insert(newSong);
                }
            }
        }
    }

    private void scanAlbumDir(long bandID, long albumID, File albumDir) {
        for (File item: albumDir.listFiles()) {
            if (item.isFile() && !item.getName().startsWith("[")) {
                Log.d(LOG_TAG, String.format("Found album song %s", item.getName()));
                Song newSong = new Song(item.getName(), item.getAbsolutePath(), bandID, albumID);
                database.songDAO().insert(newSong);
            }
        }
    }

    public void scan() {
        // TODO: Find portable way to find MCotP root
        String hardCodedSdcardRoot = "/storage/0000-0000";
        File sdcard = new File(hardCodedSdcardRoot);
        scanSdcard(sdcard);
        Log.d(LOG_TAG, "Completed MCotP scan");
    }
}

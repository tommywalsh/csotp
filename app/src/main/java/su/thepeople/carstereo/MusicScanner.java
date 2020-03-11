package su.thepeople.carstereo;

import android.content.Context;
import android.util.Log;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Database;
import su.thepeople.carstereo.data.Song;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Class to scan for on-disk music.
 *
 * This class assumes that there will be a directory called "mcotp", in which the entire music library is stored. This
 * mcotp directory should be in the root level of an SD card. Failing that, it might work to put the directory as a
 * sibling to whereever Android will put this application's media directories, or as a sibling to one of the media
 * directories' parents.
 */
public class MusicScanner {

    private Database database;
    private Context context;

    public MusicScanner(Context context, Database database) {
        this.database = database;
        this.context = context;
    }


    private boolean containsMcotpDir(File maybeDir) {
        Log.d(LOG_TAG, String.format("Considering %s", maybeDir.toString()));
        if (maybeDir.isDirectory()) {
            File[] dirContents = maybeDir.listFiles();
            if (dirContents != null) {
                for (File item : maybeDir.listFiles()) {
                    if (item.isDirectory() && item.getName().equals("mcotp")) {
                        Log.d(LOG_TAG, "FOUND MCOTP!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Optional<File> findMcotpRoot() {
        for (File mediaDir : context.getExternalMediaDirs()) {
            File candidateDir = mediaDir;
            while (candidateDir != null) {
                if (containsMcotpDir(candidateDir)) {
                    return Optional.of(candidateDir);
                }
                candidateDir = candidateDir.getParentFile();
            }
        }
        return Optional.empty();
    }

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
        Optional<File> mcotpRoot = findMcotpRoot();
        mcotpRoot.ifPresent(root -> scanSdcard(root));
    }
}

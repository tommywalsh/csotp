package su.thepeople.carstereo.backend;

import android.content.Context;
import android.util.Log;

import su.thepeople.carstereo.data.Album;
import su.thepeople.carstereo.data.Band;
import su.thepeople.carstereo.data.Database;
import su.thepeople.carstereo.data.Song;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Class to scan for on-disk music.
 *
 * This class assumes that there will be a directory called "mcotp", in which the entire music library is stored. This
 * mcotp directory should be in the root level of an SD card. Failing that, it might work to put the directory as a
 * sibling to wherever Android will put this application's media directories, or as a sibling to one of the media
 * directories' parents.
 */
public class MusicScanner {

    private static final String LOG_TAG = "Music Scanner";
    private static final Pattern ALBUM_DIR_REGEX = Pattern.compile("^(\\d\\d\\d\\d)[a-z]? - (.*)$");
    private static final Pattern LOOSE_SONG_FILE_REGEX = Pattern.compile("^((\\d\\d\\d\\d) - )?(.*)\\.(\\w{3,4})$");
    private static final Pattern ALBUM_SONG_FILE_REGEX = Pattern.compile("^(\\d*)( - )?(.*)\\.(\\w{3,4})$");

    private final Database database;
    private final Context context;

    public MusicScanner(Context context, Database database) {
        this.database = database;
        this.context = context;
    }

   private Optional<File> getMcotpSubdir(File maybeDir) {
        Log.d(LOG_TAG, String.format("Looking for collection in %s", maybeDir.toString()));
        Optional<File> maybeMcotp = Utils.dirContentsStream(maybeDir)
                .filter(f -> f.isDirectory() && f.getName().equals("mcotp")).findFirst();
        maybeMcotp.ifPresent(mcotp ->
                Log.d(LOG_TAG, String.format("Found music collection at %s", mcotp.getAbsolutePath())));
        return maybeMcotp;
    }

    private Optional<File> findMcotpRoot() {
        return Stream.of(context.getExternalMediaDirs())
                .filter(Objects::nonNull)
                .flatMap(Utils::dirParentStream)
                .map(this::getMcotpSubdir)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private void scanCollection(File mcotpRoot) {
        Log.d(LOG_TAG, String.format("Scanning collection at %s", mcotpRoot.getAbsolutePath()));
        Utils.dirContentsStream(mcotpRoot)
                .filter(File::isDirectory)
                .filter(d -> !d.getName().startsWith("["))
                .forEach(bandDir -> {
                    Log.d(LOG_TAG, String.format("Found band directory %s", bandDir.getName()));
                    Band newBand = new Band(bandDir.getName());
                    long bandID = database.bandDAO().insert(newBand);
                    scanBandDir(bandID, bandDir);
                });
    }

    private Optional<String> getMatch(Matcher matcher, int groupNum) {
        String matchedString = matcher.group(groupNum);
        return Optional.ofNullable(matchedString);
    }

    private void scanBandDir(long bandID, File bandDir) {
        Log.d(LOG_TAG, String.format("Scanning band directory %s", bandDir.getName()));
        Utils.dirContentsStream(bandDir)
                .forEach(f -> {
                    if (f.isDirectory()) {
                        Long albumID = null;  // Using null for dirs that are not real albums
                        String dirName = f.getName();
                        Matcher dirMatcher = ALBUM_DIR_REGEX.matcher(dirName);
                        String albumName = dirMatcher.matches() ? dirMatcher.group(2) : dirName;
                        assert albumName != null;
                        @SuppressWarnings("ConstantConditions") Integer albumYear = dirMatcher.matches() ? Integer.parseInt(dirMatcher.group(1)) : null;
                        if (!albumName.startsWith("[")) {
                            Log.d(LOG_TAG, String.format("Found album %s", albumName));
                            Album newAlbum = new Album(albumName, bandID, albumYear);
                            albumID = database.albumDAO().insert(newAlbum);
                        }
                        scanAlbumDir(bandID, albumID, albumYear, f);
                    } else if (f.isFile()) {
                        String fileName = f.getName();
                        Log.d(LOG_TAG, String.format("Found loose song %s", fileName));
                        Matcher songMatcher = LOOSE_SONG_FILE_REGEX.matcher(fileName);
                        if (!songMatcher.matches()) {
                            Log.w("Loose song does not match format: %s", fileName);
                        }
                        String songName = getMatch(songMatcher, 3).orElse(fileName);
                        Integer songYear = songMatcher.matches() ? getOptionalIntegerFromString(songMatcher.group(2)) : null;
                        Song newSong = new Song(songName, f.getAbsolutePath(), bandID, null, songYear);
                        database.songDAO().insert(newSong);
                    }
                });
    }
    private static Integer getOptionalIntegerFromString(String stringToParse) {
        if (stringToParse == null) {
            return null;
        }
        return Integer.parseInt(stringToParse);
    }

    private void scanAlbumDir(long bandID, Long albumID, Integer albumYear, File albumDir) {
        Utils.dirContentsStream(albumDir)
                .filter(File::isFile)
                .filter(f -> !f.getName().startsWith("["))
                .forEach(songFile -> {
                    String fileName = songFile.getName();
                    Log.d(LOG_TAG, String.format("Found album song %s", fileName));
                    Matcher matcher = ALBUM_SONG_FILE_REGEX.matcher(fileName);
                    if (!matcher.matches()) {
                        Log.w("Album song does not match pattern: %s", fileName);
                    }
                    String songName = getMatch(matcher, 3).orElse(fileName);
                    Song newSong = new Song(songName, songFile.getAbsolutePath(), bandID, albumID, albumYear);
                    database.songDAO().insert(newSong);
                });
    }

    public void scan() {
        Optional<File> maybeRoot = findMcotpRoot();
        maybeRoot.ifPresent(this::scanCollection);
    }
}

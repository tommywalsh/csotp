package su.thepeople.carstereo.lib.data;

import java.io.Serializable;

import su.thepeople.carstereo.R;
import su.thepeople.carstereo.lib.backend.MusicControllerThread;

// POJO to express the current state of the backend music player
public class BackendStatus implements Serializable {
    public MusicControllerThread.PlayModeEnum mode = MusicControllerThread.PlayModeEnum.SHUFFLE;
    public boolean isPlaying = false;
    public int subModeIDString = R.string.empty;
    public SongInfo currentSong = null;
}

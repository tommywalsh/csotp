package su.thepeople.carstereo.data;

import java.io.Serializable;

import su.thepeople.carstereo.R;
import su.thepeople.carstereo.backend.MusicController;
import su.thepeople.carstereo.data.SongInfo;

// POJO to express the current state of the backend music player
public class BackendStatus implements Serializable {
    public MusicController.PlayModeEnum mode = MusicController.PlayModeEnum.SHUFFLE;
    public boolean isPlaying = false;
    public int subModeIDString = R.string.empty;
    public SongInfo currentSong = null;
}

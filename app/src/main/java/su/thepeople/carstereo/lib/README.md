This is a Java library intended to help write music-playing apps and programs.

Although initially developed for an Android car stereo app, the hope is that this lib should work on any platform that supports Java
and has access to a collection of music that follows the Naming Convention of the People.

To use:
1) Implement platform-specific implementations for all of the interfaces in the `platform_interface` package.
These include classes for reading/writing persistent metadata, for doing logging, and for playing audio.

2) Use the `Backend` class to set things up, like so:

```java
// Create a new backend for this platform
Backend theBackend = Backend.initializePlatform(myPlatformAdapter)
        
// This will examine an on-disk music collection and create Java-accessible metadata to represent it.
if (...) {
   theBackend.scanCollection(myMusicStorageDirectories); 
}

// Start up music-playing capabilities, by providing an object to handle requests about what audio files to open/play/pause/etc.
musicControllerThread = theBackend.spawnMusicThread(myNotificationReceiver);
```

3) Set up whatever user interface you think is appropriate. This UI will have two main responsibilities:

- Send requests to the music controller thread (e.g. "the user wants to pause" or "play songs from 1996"):
```java
musicControllerThread.api().forcePause();
musicControllerThread.api().lockSpecificYear(1996);
```
- Act on updates from the music controller thread (e.g. "music is now paused" or "the name of the currently-playing song is Voivod").  This is done
via the `UINotificationAPI` that was implemented as part of step (1).


# The Backend

The Backend is responsible for actually playing music.  There are a number of entities, described
below.

## `MusicController` 

The music controller is in charge of deciding what songs should be played.

This communicates back and forth with the UI thread in three ways:
- It accepts commands from the UI and adjusts its music-playing strategies accordingly.
- It sends the UI any data that it might have requested about available music.
- It sends the UI updates as its status changes.

To do its job, it relies on the `MusicPlayer` and one of the `MusicSelector`s.

## `MusicSelector`

This is a family of classes that produce a sequence of songs to play, according to a variety of
possible strategies.  Each main strategy has its own concrete class (e.g. one of them plays songs 
from a particular band).  Each of these classes can choose to implement its own "sub-modes", which
make tweaks to the general strategy.

To do its job, each `MusicSelector` may use one or more `SongProvider`s.

## `SongProvider`

This is a family of simple classes that know how to extract songs from the database according to 
a set of specific rules.

## `MusicPlayer`

This is a wrapper for the very complicated Android `MediaPlayer`. Its job is to hide that
complexity, and provide a simplified interface to the `MusicController`.

This class's job is really simple: it plays (or pauses) whatever song(s) it is given.



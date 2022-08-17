# Code overview

This software implements an Android-based car stereo.

There are four major parts:
- UI code (and other Android system interactions)
- Backend code, which handles finding, selecting, and playing music
- Interthread support code, for communications between the UI and backend
- Data-related code, mostly for interactions with a database of songs


## Operation

This is a short description. For more details, see the individual readmes in the various package
subdirectories.

There are two threads in the system: one for the UI and one for the backend.

On the UI thread, there is one main entity, called `MainUI`. This reacts to user inputs and system
events, and passes commands over to the backend thread.  This entity also receives messages from 
the backend, and updates the screen accordingly.

On the backend thread, there are three main entities

- The MusicScanner analyses the on-disk music collection, and maintains its own database
- The MusicPlayer is a simplified audio player
- The MusicController is in charge of deciding what songs get played, and when to stop/start playback.

The MusicController accepts commands from the UI thread, and sends status messages back to the UI.



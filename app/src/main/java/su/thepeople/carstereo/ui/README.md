# UI (and associated) code

All of the code in this subdirectory runs on the UI thread. From the point of view of the Android
system, this is the "main thread".

This code is in charge of:
- Receiving, and reacting to, user commands from screen presses
- Receiving, and reacting to, various Android system events
- Sending commands to the backend
- Updating the screen based on status messages from the backend

## Main UI

The `MainUI` class is in charge of:
- Communicated back and forth with the backend
- Setting up all helper classes
- Creating other threads needed by the app
- Presenting and managing the default "front screen" that the app is usually displaying
- Kicking off sub-activities, as necessary

## `InputHandler` helper classes

The UI thread has three main sources of input: the user, the backend, and the Android system
(specifically, the Bluetooth subsystem).  Each of these has its own helper class which receives
the input, optionally does some simple processing, and passes it along to the `MainUI` class.

## Other screens

There are two types of screens that are shown to the user, other than the main "front screen".

`ItemChooser` shows a picklist of items (e.g. bands), and allows the user to select from them.

`SongNavigationUI` brings up various buttons that allow for navigating to different songs.

## Other stuff

There are a handful of other helper classes in here as well. See code files for descriptions.
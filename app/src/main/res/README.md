# CSoTP: Car Stereo of The People

This is an Android app designed to play on-disk music in a car. It is intended to be used on a 
device that is attached to the car dashboard (not held in a hand), and which communicates with a 
built-in amplifier/receiver using Bluetooth.

# About CSoTP

I could find nothing that would do this job acceptably. Not an Android/IOS app, not a purpose-built
car stereo. Nothing. So, I had to make my own. 

## How CSotP works

The Car Stereo of The People is based on these three principles:

* Metadata should be gleaned from on-disk directory structure, not from in-file tags. This gives a
 very limited amount of metadata, but it is consistent and easy to maintain. In-file tags give large 
 amounts of metadata, but it tends to be inconsistent and mostly useless.
 
* There are only a handful of simple "play modes", which should be easy to cycle between with one
 touch. There is no need to provide access to low-level primitives like playlists and repeat modes.
 
* There should be a small number of on-screen controls, all of which should be large. Any on-screen
information needs to be in large print.  The UI should be usable without having to concentrate on
it, and hopefully without even needing to look directly at it. It should be about as easy/safe to 
use as the preset push buttons on old mechanical car radios.

### How to store music

All music needs to be stored on an SD card in a single directory tree. Each song that is part of
an album should be stored in a path like this:
```
/storage/0000-0000/mcotp/Band Name/Album Name/## - Song Title.ext
```

If a song is not part of an album then it should be stored like this:
```
/storage/0000-0000/mcotp/Band Name/Song Title.ext
```

This app does not support having multiple bands or albums for the same song file. If you really want
that, you can simply copy the file into two or more different directories.

### Controlling playback

There are two buttons that control playback. There is a "play/pause" button and a "next" button. 
That's it.

### Controlling song selection

There are three song selection modes, which each use different rules for determining which songs are
heard in which order. They are controlled by two toggle buttons, one of which displays the band
name, the other the album name.

**Entire-Collection Mode** In this mode, songs are randomly selected from the entire music
collection. This is the default mode, which is activated when neither of the two toggle buttons are
depressed.

**Band Mode** In this mode, songs from the same band are randomly selected. This mode is activated
by pressing the band toggle button. This will "lock" on whatever band is currently playing (the
current song will keep playing). You can switch back to entire-collection mode by pressing the
button a second time.

**Album Mode** In this mode, songs from the same album are played, in the same order that they
appear on the album. This mode is activated by pressing the album toggle button. This will continue
playing the current song, and then follow with whatever song comes next on the album (wrapping 
around to the beginning of the album at the end). Again, if you press this button a second time, it
will switch back to entire-collection mode.

There may be future enhancements to allow picking arbitrary bands/albums, but for now, you can
only lock on the currently-playing band/album.


## But, why? (A rant/manifesto)

Why write a custom player? Why not just use something that's already available? Basically, 
because I could find nothing that works acceptably.

### Why not use a purpose-built car stereo?

Because they are very expensive. And, the UIs are absolutely terrible (and not customizable).

### Why use Android?

Since this is intended to play music files from an SD card, and since I needed a custom-written 
interface, I cannot use something like an Arduino or custom hardware... at least not easily.

I did consider a Raspberry Pi, but that had some issues that I don't need to worry about with
something like an Android or IOS device.

With Android or IOS, we have these nice features:
* These devices have batteries, and are electrically isolated from the rest of the vehicle. This
is important because a car's power is very very noisy (well, at least it is in the kinds of cars I
drive). This isolation prevents this noise from being conducted into the audio signal. The car's 
noisy power line is only used to charge the battery.
* Audio can be transmitted digitally via Bluetooth instead of by wire, which eliminates radiated 
interference from affecting the audio signal.
* Screen can be set to auto-dim based on ambient light, so the screen will always be appropriately
bright.
* Touch screen "just works", and doesn't require any calibration.
* UI is designed for touchscreen apps, and no special settings are needed to hide mouse pointers, 
window manager decoration, etc.

I picked Android over IOS because I happen to have an old Android device that was laying around. I
only need to add a simple 12V Bluetooth amplifier, which can be had for very cheap money.



### Why not use an existing Android player?

Here are some aspects I find problematic about most currently-available Android players:

#### Tiny controls

When I want to skip forward one song, I want to do it **right now**. I don't want to have to 
navigate or scroll anywhere to find the controls. I don't want to have to carefully aim my finger 
at a tiny button, and then navigate back if my thumb was 4 pixels off. I want a nice big "next" 
button that I can see out of the corner of my eye and be able to press without concentrating.

Most existing Android players confine their controls to a tiny area of the screen, and devote the
rest of the screen to stuff I don't care about, like grids full of album art, or artist bios, or 
 even ads. It seems that these are geared towards people who, for some reason, want to "interact" 
 with their music on a screen. I don't want to do that. I just want to listen to music.

#### Inconsistent information

Existing Android players use in-file tags to attempt to organize music metadata. 

But, it's a gigantic pain in the neck to try to maintain in-file tags. There are different tag 
formats, different tag types, and hundreds (thousands?) of different strategies that are used. Tag
editing software is cumbersome and not usually effective on all types of music files and tags.

This means that it's difficult to avoid inconsistencies. Here's a small sample of the many problems
seen when using existing Android players that use in-file tags:

* Incorrect separations. According to tags, I have music by three totally separate bands named 
"Echo & the Bunnymen", "Echo and the Bunnymen" and "Echo And The Bunnymen". I also have three
totally different Replacements albums, named "Tim", "Tim (1985)", and "Tim [remastered]". Yuck.

* Metadata with negative value. This takes up screen real estate, and adds complexity in navigation,
 while providing zero benefit. For example, players will try to organize by genre, but...
** I see a genre called "80s", which has exactly one song in it, even though I have hundreds of 
albums from the 1980s.
** Here are some separate genres I have listed: "Country-Rock, Singer/Songwriter, Folk-Rock, AlbumRock",
"Country/Rock", "CountryRock", "Album Rock", "Country-Rock, Singer/Songwriter". Each of these has
some Neil Young songs in them. There are probably more.
** Here are some release date tags that are all treated as totally distinct: "1989", "89", "Jan 15, 1989".

* Some songs are associated with "Greatest Hits" albums, while others are associated with their 
original albums.

* Some songs on the same album are listed with inconsistent track orders (presumably from different
CD/vinyl/cassette track ordering).
 

#### Streaming-oriented UIs

Some players really really really want me to stream music. 

I basically have two choices for how to listen to music:

1) I can use an SD card smaller than my fingernail. On this, I can store like 5000 albums 
worth of music. Then, I can have all of my music with me everywhere I go, for the rest of my life.

2) I can stream music over the internet, which means I am relying on...

* Having decent-speed data service any time I want to listen to music. Driving in 
rural Vermont? No streaming.

* Some crappy tech company having up-to-date licensing agreements in place with all of the (even
crappier) multinational rights-owning conglomerates that hold the rights to some of the songs I 
want to listen to. Someday, Google Play Music will get into a contract dispute with Universal Music
Group, and then I won't be able to listen to Paul's Boutique anymore.

* That crappy tech company actually making the music I want to listen to available and discoverable.
If I want to listen to The Broken Toys, an 80s hardcore band from Lawrence, Mass, can I find that
on Spotify?  Nope.

The first option is obviously vastly superior. I'd almost need to be insane to choose #2. Yet,
every time I open Google Play Music, it brings me to a screen full of streamable stuff instead of
to my on-disk collection. I have to waste time navigating away from that every single time.

#### Complicated ways of doing simple things

Here's a situation that comes up often. I'm listening to songs in a random order, and eventually 
something comes on that I want to listen to more of. Let's say Guadalcanal Diary's "Under The Yoke" 
comes on, and that makes me want to listen to that whole album.

I would typically have to do something like this in most existing players that I've seen:
* Navigate to a "Library" screen
* Navigate to an "Artists" tab
* Manually scroll all the way down to "Guadalcanal Diary", or else open up a search widget.
* Select the band
* Find the album "2x4", select it, and click "play"

This is infuriatingly complex to do, even if I'm just sitting in a chair. It is impossible to do 
safely while driving.

On top of all that inconvenience, it still annoyingly stops playing the current song, and jumps
back to the beginning of the album, instead of just continuing to play. 

#### Leaky abstractions

I do understand that music-playing software has concepts like "queues", "playlists", "shuffle",
"repeat", and "consume".  But, as a user, I rarely care about such things, and I don't usually want
to adjust them all independently -- they should normally be hidden away.

For example, there is a difference between "listening to my music collection" and "listening to one
specific album". In the former, I want shuffle turned on, but not in the latter. But, I don't want 
to have to separately adjust multiple options like this all the time.

I've been on this planet almost fifty years, and I don't think I've ever run into a 
situation where I felt it was appropriate to shuffle an album. And, yet music players treat
"shuffle" as if it were a permanent user preference to be preserved across mutliple contexts.


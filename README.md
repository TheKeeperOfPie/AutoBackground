AutoBackground
==============

Android application for automatically downloading images from websites and applying them as the system wallpaper

Sorry about the fairly messy and undocumented code. My first app, and really my first meaningful program in Java at all.

A Google+ Community for this app is available here: https://plus.google.com/communities/100246478573973585215

Explanation of Settings
==============

Sources
--------------

- Title: name for source entry, used to name folder and downloaded images, editing causes folder/files to be renamed and the duplicate URL set to be cleared
- URL/data: URL to download images at, uses Jsoup to connect (which means currently no JS generated webpage support)
- Number of images: number of images to download from website, or shows the number of images found in a folder when added
- Toggle On/Off: determines whether a source is used for both downloading or to display images in the rotation

Wallpaper
--------------

- Fill Images:
    - On: attempts to fill screen width, cropping height as necessary
    - Off: attempts to fit image height into screen, letterboxing sides as necesssary
- Shuffle Images:
    - On: causes Java Math.random() to select a random image from a compiled list of all usable images in sources
    - Off: rotates through images in order at time of cycle, does not check for adjustment of images by other processes

- Update Interval

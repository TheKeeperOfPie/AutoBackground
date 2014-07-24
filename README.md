AutoBackground
==============

Android application for automatically downloading images from websites and applying them as the system wallpaper

Sorry about the fairly messy and undocumented code. My first app, and really my first meaningful program in Java at all.

A Google+ Community for this app is available here: https://plus.google.com/communities/100246478573973585215

Explanation of Settings
==============

Sources
--------------

- Title: Name for source entry, used to name folder and downloaded images, editing causes folder/files to be renamed and the duplicate URL set to be cleared
- URL/data: URL to download images at, uses Jsoup to connect (which means currently no JS generated webpage support)
- Number of images: Number of images to download from website, or shows the number of images found in a folder when added
- Toggle On/Off: Determines whether a source is used for both downloading or to display images in the rotation

Wallpaper
--------------

- Fill Images:
    - On: Attempts to fill screen width, cropping height as necessary
    - Off: Attempts to fit image height into screen, letterboxing sides as necesssary
- Shuffle Images:
    - On: Causes Java Math.random() to select a random image from a compiled list of all usable images in sources
    - Off: Rotates through images in order at time of cycle, does not check for adjustment of images by other processes

- Update Interval
    - On: Cycles wallpaper to next image every amount of specified time, advanced allows minute by minute control
    - Off: Will not cycle wallpaper, must be cycled manually or using change on return
- Force Interval
    - On: Will cause update timer to immediately change image, used when a screen with the wallpaper is constantly enabled and visible
    - Off: Will cause update timer to update only on a return visibility change, to prevent using CPU cycles and RAM while user is in a different application
- Change on Return
    - On: Causes wallpaper to update when returning from non-visibility, such as another opaque application
    - Off: Will not update on return
- Double Tap Gesture
    - On: Will cycle wallpaper on a double tap gesture on app or on wallpaper
    - Off: Will not cycle on double tap

- Use Fade
    - On: Will cause a fade between images, done by overlapping two OpenGL textures with one texture increasing alpha and the other decreasing, upon finishing will delete old texture, does not animate while fading
    - Off: Will not fade, uses a stark cut into the new image
- Use Animation
    - On: Will use a pan animation bouncing back and forth, default 30 FPS animation
    - Off: Will not animate
- Animation Speed: changes speed of animation by changing number of pixels in image skipped on each frame
- Frame Rate: changes how often the renderer will render a new frame, most devices are locked to 60 FPS
- Animation Safety: pixel buffer applied such that if (screenWidth + buffer < bitmapWidth), will not animate, default 50 pixels to prevent side to side stutter due to rapid direction change

Downloader

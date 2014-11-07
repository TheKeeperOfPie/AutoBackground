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
    - On: Causes Java Random.nextInt() to select a random image from a compiled list of all usable images in sources
    - Off: Rotates through images in order at time of cycle, does not check for adjustment of images by other processes

- Update Interval:
    - On: Cycles wallpaper to next image every amount of specified time, advanced allows minute by minute control
    - Off: Will not cycle wallpaper, must be cycled manually or using change on return
- Force Interval:
    - On: Will cause update timer to immediately change image, used when a screen with the wallpaper is constantly enabled and visible
    - Off: Will cause update timer to update only on a return visibility change, to prevent using CPU cycles and RAM while user is in a different application
- Change on Return:
    - On: Causes wallpaper to update when returning from non-visibility, such as another opaque application
    - Off: Will not update on return
- Double Tap Gesture:
    - On: Will cycle wallpaper on a double tap gesture on app or on wallpaper
    - Off: Will not cycle on double tap

- Parallax Scroll:
    - On: Internally forces wallpaper to scroll in opposite direction
    - Off: Defaults to normal scroll dictated by launcher
- Use Fade:
    - On: Will cause a fade between images, done by overlapping two OpenGL textures with one texture increasing alpha and the other decreasing, upon finishing will delete old texture, does not animate while fading
    - Off: Will not fade, uses a stark cut into the new image
- Use Animation:
    - On: Will use a pan animation bouncing back and forth, default 30 FPS animation
    - Off: Will not animate
- Animation Speed: Changes speed of animation by changing number of pixels in image skipped on each frame
- Frame Rate: Changes how often the renderer will render a new frame, most devices are locked to 60 FPS
- Animation Safety: Pixel buffer applied such that if (screenWidth + buffer < bitmapWidth), will not animate, default 50 pixels to prevent side to side stutter due to rapid direction change

Downloader
--------------

- Image Width: Width in pixels used as the minimum possible width when downloading images, does not affect user added folders nor already cached images
- Image Height : Height in pixels used as the minimum possible width when downloading images, does not affect user added folders nor already cached images
- Download Timer:
    - On: Will download a new set of images every amount of user specified time, default 2 days, should not wake device to download
- Start Time: Time at which download timer will first start and repeat from thereafter
    - Off: Will not download new images, must be downloaded manually
- Use WiFi:
    - On: Will attempt to download if device is connected to WiFi
    - Off: Will not allow download if device is connected to WiFi
- Use Mobile Data:
    - On: Will attempt to download if device is connected to mobile data
    - Off: Will not allow download if device is connected to mobile data
- Download Notification:
    - On: Will show a progress and completed notification with details on download progress and set of images downloaded
    - Off: Will not show notification, download will be done silently in background with only Toast messages
- High Quality:
    - On: Encodes images in ARGB_8888 format
    - Off: Encodes images in RGB_565 format
- Force Images:
    - On: Attempts to append .jpg and .png to a found URL to attempt to access image file stored at that location, shouldn't really ever be used
    - Off: Will only download image files which are actually shown as images
- External Download Path:
    - On: Uses a user specified download path chosen through app directory browser
    - Off: Uses default internal app cache to store images, /data/data/cw.kop.autobackground/cache is default on a Nexus 5
- Keep Images:
    - On: Will keep all old images in cache, simply adding more on each download
    - Off: Will overwrite images on download
- Delete Old:
    - On: Will delete all images of a source once downloading begins
    - Off: Images will simply be overwritten, and extra images will be kept to fill number requirement
- Prevent Duplicates:
    - On: Checks a set of stored URLs against any new URL to be downloaded
    - Off: Downloads a found image regardless if image is already stored (NOTE: URLs differing in even a single character will considered different, as app does not check bitmap pixel data)
- Delete Images: Will purge all downloaded images from website sources and clear their stored duplicates set
- Image Prefix: Changes prefix applied to folders and images after source title and before image index, change will prevent folders with previous prefix to be read (recommend change on clean install and before adding any sources)

Effects
--------------

- Use Effects:
    - On: Enables effects
    - Off: Disables all effects
- Manual Frequency: Percentage chance of applying manual effects to image
- Random Frequency Percentage change of applying random effects to image
- Manual Override:
    - On: Causes manual effects to be applied on top of any random effects
    - Off: Manual effects will not apply when random effect is triggered
- Random Effects:
    - On: Enables chosen random effects
    - Off: Disables random effects
    - Completely Random: Chooses random effect with random but normalized parameters to apply on image, only applies a single effect per image
    - Filter Effects: Chooses random filter to apply to image
    - Dual Tone Random: Chooses 2 random hexcodes, one dark, one bright, to apply to image using duotone effect
    - Dual Tone Warm: Chooses 2 random warm hexcode colors, one dark, one bright, to apply to image using duotone effect
    - Dual Tone Cool: Chooses 2 random cool hexcode colors, one dark, one bright, to apply to image using duotone effect
- Dual Tone Gray:
    - On: Forces dark color in Dual Tone random effects to be a gray color to improve contrast
    - Off: Default Dual Tone behavior
- Toast Effects:
    - On: Displays a Toast message with applied effect and its parameters when effect is applied, can be multiple messages
    - Off: Does not notify of applied effects
- Reset Effects: Causes all manual parameters and settings to be changed to defaults

Notification:
--------------

- Use Notification:
    - On: Enables notification, color options will only be available on Android API level 16+ (4.1+)
    - Off: Disables notification
- Notification Game:
    - On: Creates a 5 wide by 2 high image matching game in expanded notification, changes notification priority to Notification.PRIORITY_MAX
    - Off: Disables image matching game
- Fill Image:
    - On: Causes image to be center cropped to fill notification icon
    - Off: Image will be scaled and fit entirely within notification icon, letterboxing if necessary
- Icon Action: Applies a custom action to the notification icon
- Show Pin Indicator:
    - On: Pinning image will cause pin icon to overlay notification icon
    - Off: Disables any notification that image was pinned
- Previous History Size: Number of files to store in app of image history to be used with Previous action
- Reshow Notification Tutorial - Reshows overlaid tutorial

Notification Options
--------------

- Icon:
    - Application: Uses AutoBackground app icon
    - Image: Uses currently set image from wallpaper
    - Custom: Uses a custom file provided by user, does not check if file is moved
    - None: Shows nothing
- Title/Summary:
    - Application: Shows application name (AutoBackground)
    - Location: Shows URL or storage location of current wallpaper image
    - Custom: Shows user provided text string
    - None: Shows nothing
- Buttons:
    - Copy: Copies image URL found using image title, or image location of file
    - Cycle: Broadcasts cycle intent to wallpaper causing it to cycle, resets update interval change if queued
    - Delete: Permanently deletes the stored image file, does not correct for indexes, leaving blank space in files (not in rotation)
    - Open: Broadcasts intent to open current image file in gallery
    - Pin: Pins current wallpaper image, keeping it pinned for the specified duration
    - Previous: Moves backwards in image history to display previous image, does not save current image into buffer
    - Share: Opens up a default sharing menu for the current image file
    - None: Shows and does nothing

Application
--------------

- Use Toasts:
    - On: Will show various Toast messages to notify user
    - Off: Will not show Toast messages
- Change Theme:
    - Light: White background theme with black text, buttons still have white text
    - Dark: Black background theme with white text
    - Transparent: Transparent background theme with white text, small radius shadow applied to text
- Advanced Settings:
    - On: Shows various advanced settings
    - Off: Does not show advanced settings, still technically enabled, so if changed and then hidden, advanced settings still apply
- Reset Settings: Clears app SharedPreferences to default settings
- Reshow Source List Tutorial: Reshows the source tutorial on revisiting the source list

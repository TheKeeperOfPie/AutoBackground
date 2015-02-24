![](/app/src/main/res/drawable-xxhdpi/app_icon.png)

AutoBackground
==============

Android application for automatically downloading images from websites and applying them as the system wallpaper

Sorry about the fairly messy and undocumented code. My first app, and really my first meaningful program in Java at all.

A Google+ Community for this app is available here: https://plus.google.com/communities/100246478573973585215

Explanation of Settings
==============

Default settings are italized. Advanced settings are marked with a preceding "*". These settings can be revealed by activating Advanced Settings in the Application settings screen.

Sources
--------------

- Title: Name for source entry, used to name folder and downloaded images, editing causes folder/files to be renamed
- URL/data: Data representing where to get images, such as a URL or subreddit. Note that genetic websites are parsed with Jsoup, so it cannot fetch JavaScript generated links
- Number of images: Number of images to download from this source, or displays the number of images found for a local source
- Toggle On/Off: Determines whether a source is used for both downloading or to display images in the rotation
- Preview: Whether or not to show a preview image of the source in the source list
- Time Active: Time throughout a day when a source will be actively used in the displayable images list

Wallpaper
--------------

- *Preserve Context:
    - On: Saves OpenGL EGL context to prevent redrawing the wallpaper when it comes back into view
    - Off: Discards context on pause, clearing up more memory but requiring a decode and redraw when the wallpaper is visible again
- Double Image:
    - On: Shows two images at once, each taking up a constant half of the screen height
    - Off: Shows a single full screen image
- Shuffle Images:
    - On: Selects a random image from a compiled list of all usable images in sources
    - Off: Rotates through images in order at time of cycle, does not check for adjustment of images by other processes

- Update Interval: Time between changing images, or to change on a return to the wallpaper
- Force Interval:
    - On: Forces images to change
    - Off: Waits until visibility change to cycle image
- *Reset On Cycle:
    - On: Resets update interval when the image is cycled manually
    - Off: Update timer will continue as normal when image is cycled
- *Change When Locked:
    - On: Allows wallpaper to be changed on the lockscreen, does not wake device
    - Off: Prevents wallpaper from being change on the lockscreen

- *Transition Speed: Amount of time transition takes between images
- Fade:
    - On: Fades image alphas during transition
    - Off: New image will instantly replace old image
- Horizontal Overshoot: Throws image in horizontally
- *Reverse Horizontal Overshoot:
    - On: Throws image in from right side
    - Off: Throws image in from left side
- *Horizontal Overshoot Intensity: Adjusts amount of horizontal overshoot
- Vertical Overshoot: Throws image in vertically
- *Reverse Vertical Overshoot:
    - On: Throws image in from top
    - Off: Throws image in from bottom
- *Vertical Overshoot Intensity: Adjusts amount of vertical overshoot
- Zoom In: Enlarge new image into view
- Zoom Out: Shrink old image out of view
- Spin In: Rotate new image in
- *Reverse Spin In:
    - On: Rotate in counter-clockwise
    - Off: Rotate in clockwise
- *Spin In Angle: Total angle to rotate in
- Spin Out: Rotate old image out
- *Reverse Spin Out:
    - On: Rotate out clockwise
    - Off: Rotate out counter-clockwise
- *Spin Out Angle: Total angle to rotate out

- Use Horizontal Animation: Side to side animation
- *Horizontal Speed: Horizontal animation speed in number of pixels per animated frame
- Use Vertical Animation: Up and down animation
- *Vertical Speed: Vertical animation speed in number of pixels per animated frame
- *Scale Speed: Adjust speed with zoom factor
- *Frame Rate: Frames per second to render animation
- *Animation Safety: Buffer in pixels required in image to allow animation, used to prevent animation shake

- *Double Tap Gesture: Cycle image when wallpaper is double tapped
- *Use Scrolling: Allow scrolling of wallpaper with launcher screens
- *Parallax Scrolling: Reverse direction wallpaper with launcher scrolling
- *Use Drag: Allow dragging of wallpaper with 2 fingers without moving launcher screens
- *Reverse Drag: Reverse direction image moves when dragged with 2 fingers
- *Use Zoom: Allow wallpaper to be zoomed with a pinch in/out gesture
- *Use Long Press Reset: Reset wallpaper zoom on long press

Downloader
--------------

- Image Width: Width in pixels used as the minimum possible width when downloading images, does not affect user added folders nor already cached images
- Image Height : Height in pixels used as the minimum possible width when downloading images, does not affect user added folders nor already cached images
- *Full Resolution: Download images at source resolution, min width/height still apply
- Use WiFi: Attempt to download on WiFi connection
- Use Mobile Data: Attempt to download on mobile data connection
- Download Timer: Set a download interval when new images will automatically be downloaded
- Start Time: Time at which download timer will first start and repeat from thereafter
- *Reattempt Download: Retry download on a connection change if first attempt fails
- *Download Notification:
    - On: Will show a progress and completed notification with details on download progress and set of images downloaded
    - Off: Will not show notification, download will be done silently in background with only Toast messages
- *Force Images:
    - On: Attempts to append .jpg and .png to a found URL to attempt to access image file stored at that location, shouldn't really ever be used
    - Off: Will only download image files which are actually shown as images
- *Custom Download Path:
    - On: Uses a user specified download path chosen through app directory browser
    - Off: Uses default internal app cache to store images, /data/data/cw.kop.autobackground/cache is default on a Nexus 5\
- *Image History: Store a history of previous links to attempt to prevent duplicates
- *Image History Size: Max number of links to store
- *Cache Thumbnails: Save a separate thumbnail of image for history purposes
- *Thumbnail Size: Max width/height of thumbnails stored
- Keep Images: Keep images in cache without overwriting them
- *Delete Old:
    - On: Will delete all images of a source once downloading begins
    - Off: Images will simply be overwritten, and extra images will be kept to fill number requirement
- *Prevent Duplicates:
    - On: Checks a set of stored URLs against any new URL to be downloaded
    - Off: Downloads a found image regardless if image is already stored (NOTE: URLs differing in even a single character will considered different, as app does not check bitmap pixel data)
- *Delete Images: Will purge all downloaded images from website sources and clear their stored duplicates set
- *Image Prefix: Changes prefix applied to folders and images after source title and before image index, change will prevent folders with previous prefix to be read (recommend change on clean install and before adding any sources)

Accounts
--------------

- Google Account:
    - On: Connect a Google account
    - Off: Disconnects Google account by overwriting access token
- Dropbox Account:
    - On: Connect a Dropbox account
    - Off: Disconnects Dropbox account by overwriting access token

Effects
--------------

- Gaussian Blur Effect: Uses Android's RenderScript ScriptIntrinsicBlur to apply blur to image
- Use OpenGL Effects: Enables following OpenGL texture effects
- Manual Frequency: Percentage chance of applying manual effects to image
- Random Frequency Percentage change of applying random effects to image
- *Manual Override: Causes manual effects to be applied on top of any random effects
- Random Effects:
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

Relevant Effects used in AutoBackground can be viewed here: http://developer.android.com/reference/android/media/effect/EffectFactory.html

Notification:
--------------

- Use Notification:
    - On: Enables notification, color options will only be available on Android API level 16+ (4.1+)
    - Off: Disables notification
- *Notification Game: Creates a 5 wide by 2 high image matching game in expanded notification, changes notification priority to Notification.PRIORITY_MAX
- Icon Action: Applies a custom action to the notification icon
- Show Pin Indicator:
    - On: Pinning image will cause pin icon to overlay notification icon
    - Off: Disables any notification that image was pinned
- Previous History Size: Number of files to store in app of image history to be used with Previous action

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
    - Game: Toggles notification game
    - None: Shows and does nothing

Wear
--------------

- Sync Wear Image: Show the same image on Wear device and main device
- Use Palette Colors: Change watch face colors based on image using Google's Palette API
- Time Type: Type of watch face
- Time Adjust: Adjust time offset of watch face

Application
--------------

- Show Tutorial: Reshows beginning tutorial
- *Use Toasts:
    - On: Will show various Toast messages to notify user
    - Off: Will not show Toast messages
- *Force Multi-Pane: Force a tablet dual pane layout
- Change Theme:
    - Light: White background theme with black text, buttons still have white text
    - Dark: Black background theme with white text
- Advanced Settings: Enables visiblity of advanced settings (advanced settings apply even when hidden)
- Export Sources: Export list of sources to Export folder inside download directory
- Import Sources: Import list of sources from a selected file
- Use Fabric: Turns on app crashing reporting
- Reset Settings: Clears app SharedPreferences to default settings

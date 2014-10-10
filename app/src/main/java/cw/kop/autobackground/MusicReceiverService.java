package cw.kop.autobackground;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import cw.kop.autobackground.downloader.Downloader;

/**
 * Created by TheKeeperOfPie on 10/8/2014.
 */
@SuppressLint("NewApi")
public class MusicReceiverService extends NotificationListenerService implements RemoteController.OnClientUpdateListener{

    private static final String TAG = MusicReceiverService.class.getName();

    private RemoteController audioRemoteController;
    private String previousAlbum = null;
    private String previousArtist = null;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 19) {
            registerAudioController();
        }

        Log.i(TAG, "MusicReceiverService created");

    }


    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void registerAudioController() {

        audioRemoteController = new RemoteController(getApplicationContext(), this);
        audioRemoteController.setArtworkConfiguration(4096, 4096);
        boolean registered = ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).registerRemoteController(audioRemoteController);
        Log.i(TAG, "Audio registered: " + registered);
        Toast.makeText(MusicReceiverService.this, "Audio registered: " + registered, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {

        ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).unregisterRemoteController(audioRemoteController);

        Log.i(TAG, "MusicReceiverService destroyed");

        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }

    @Override
    public void onClientChange(boolean clearing) {

    }

    @Override
    public void onClientPlaybackStateUpdate(int state) {

        if (state == RemoteControlClient.PLAYSTATE_STOPPED) {
            Toast.makeText(MusicReceiverService.this, "Music stopped", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.setAction(LiveWallpaperService.CURRENT_IMAGE);
            sendBroadcast(intent);
        }

        Log.i(TAG, "Playback state: " + state);
        Toast.makeText(MusicReceiverService.this, "Playback state: " + state, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {

    }

    @Override
    public void onClientTransportControlUpdate(int transportControlFlags) {

    }

    @Override
    public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {

        String artist = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, "Error");
        String albumArtist = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, "Error");
        String album = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, "Error");
        String track = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, "Error");

        Log.i(TAG, "Artist: " + artist + " Album: " + album);

//        if (!album.equals(previousAlbum) || !artist.equals(previousArtist)) {

            Bitmap bitmap = metadataEditor.getBitmap(RemoteController.MetadataEditor.BITMAP_KEY_ARTWORK, null);

            Downloader.setMusicBitmap(bitmap);

            Intent intent = new Intent();
            intent.setAction(LiveWallpaperService.UPDATE_CURRENT_WALLPAPER);
            sendBroadcast(intent);
//        }

        previousAlbum = album;
        previousArtist = artist;
    }
}

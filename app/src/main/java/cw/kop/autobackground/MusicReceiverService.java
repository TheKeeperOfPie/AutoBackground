/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cw.kop.autobackground;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import cw.kop.autobackground.files.FileHandler;

/**
 * Created by TheKeeperOfPie on 10/8/2014.
 */

@SuppressLint("NewApi")
public class MusicReceiverService extends NotificationListenerService implements RemoteController.OnClientUpdateListener {

    private static final String TAG = MusicReceiverService.class.getName();

    private RemoteController audioRemoteController;
    private String previousAlbum = null;
    private String previousArtist = null;
    private MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            Toast.makeText(getApplicationContext(),
                    "PlaybackState: " + state,
                    Toast.LENGTH_SHORT).show();
            super.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {

            Bitmap bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (bitmap != null) {
                Toast.makeText(MusicReceiverService.this,
                        "Bitmap ID: " + bitmap.getGenerationId(),
                        Toast.LENGTH_SHORT).show();

                FileHandler.setMusicBitmap(bitmap);

                Intent intent = new Intent();
                intent.setAction(LiveWallpaperService.LOAD_ALBUM_ART);
                sendBroadcast(intent);
            }

            super.onMetadataChanged(metadata);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            registerAudioController();
        }

        Log.i(TAG, "MusicReceiverService created");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void registerAudioController() {

        audioRemoteController = new RemoteController(getApplicationContext(), this);
        audioRemoteController.setArtworkConfiguration(4096, 4096);
        boolean registered = ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).registerRemoteController(
                audioRemoteController);
        Log.i(TAG, "Audio registered: " + registered);
        Toast.makeText(MusicReceiverService.this,
                "Audio registered: " + registered,
                Toast.LENGTH_SHORT).show();

//        MediaSession mediaSession = new MediaSession(getApplicationContext(), "autobackground");
//
//        MediaController mediaController = new MediaController(getApplicationContext(), mediaSession.getSessionToken());
//        mediaController.registerCallback(callback);
    }

    @Override
    public void onDestroy() {

        ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).unregisterRemoteController(
                audioRemoteController);

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

        if (state == RemoteControlClient.PLAYSTATE_STOPPED || state == RemoteControlClient.PLAYSTATE_PAUSED) {
            Toast.makeText(MusicReceiverService.this, "Music stopped", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.setAction(LiveWallpaperService.CURRENT_IMAGE);
            sendBroadcast(intent);

            previousAlbum = null;
            previousArtist = null;
        }

    }

    @Override
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs,
            float speed) {

    }

    @Override
    public void onClientTransportControlUpdate(int transportControlFlags) {

    }

    @Override
    public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {

        String artist = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, null);
        String albumArtist = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                "Error");
        String album = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, "Error");
        String track = metadataEditor.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, "Error");

        if (artist == null) {
            artist = albumArtist;
        }

        Log.i(TAG, "Artist: " + artist + " Album: " + album);
        Toast.makeText(MusicReceiverService.this,
                "Metadata changed, \nArtist: " + artist + " Album: " + album,
                Toast.LENGTH_SHORT).show();

        if (!album.equals(previousAlbum) || !artist.equals(previousArtist)) {

            Bitmap bitmap = metadataEditor.getBitmap(RemoteController.MetadataEditor.BITMAP_KEY_ARTWORK,
                    null);
            if (bitmap != null) {
                Toast.makeText(MusicReceiverService.this,
                        "Bitmap ID: " + bitmap.getGenerationId(),
                        Toast.LENGTH_SHORT).show();

                FileHandler.setMusicBitmap(bitmap);

                Intent intent = new Intent();
                intent.setAction(LiveWallpaperService.LOAD_ALBUM_ART);
                sendBroadcast(intent);
            }
        }

        previousAlbum = album;
        previousArtist = artist;
    }
}

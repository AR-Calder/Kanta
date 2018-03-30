package uk.arcalder.Kanta;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Zynch on 29/03/2018.
 */

public class MusicService extends MediaBrowserServiceCompat {

    private String TAG = MusicService.class.getSimpleName();

    // The action of the incoming Intent indicating that it contains a command to be executed
    public static final String ACTION_CMD = "uk.arcalder.Kanta.ACTION_CMD";

    // The key in the extras of the incoming Intent indicating the command that should be executed
    public static final String CMD_NAME = "CMD_NAME";

    // A value of a CMD_NAME key in the extras of the incoming Intent that indicates that the music playback should be paused
    public static final String CMD_PAUSE = "CMD_PAUSE";

    // TODO check if need
    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;

    private static final String MEDIA_ROOT_ID = "media_root_id";
    private static final String EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MusicProvider musicProvider;        // Provider object used to fetch music metadata
    private PlaybackManager playbackManager;    // Handles interactions between main service and playback operations

    private MediaSessionCompat musicSession;    // Allows interaction with media controllers, volume keys, media buttons, and transport controls.
    private MusicNotificationManager  musicNotificationManager;
    private Bundle sessionExtras;
    private final TimeoutHandler timeoutHandler = new TimeoutHandler(this);
    private final int timeToTimeout_ms = 15000;
    private PlaybackStateCompat.Builder stateBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        /*
        * HEAVILY BASED ON "ANDROID DEVS: BUILDING AN AUDIO APP" GUIDE
        * */

        // Get context
        Context context = getApplicationContext();

        // In onCreate, start a new MediaSession and notify its parent with the session's token
        musicSession = new MediaSessionCompat(this , TAG);
        setSessionToken(musicSession.getSessionToken());

        // MySessionCallback() has methods that handle callbacks from a media controller
        musicSession.setCallback(playbackManager.getMediaSessionCallback());

        // Enable callbacks from MediaButtons and TransportControls
        musicSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        playbackManager.setPlaybackState(null);

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(musicSession.getSessionToken());

        try{
            musicNotificationManager = new MusicNotificationManager(this);
        } catch (RemoteException re) {
            Log.e(TAG, "Unable to create MusicNotificationManager", re);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent){
            String action   = intent.getAction();
            String cmd      = intent.getStringExtra(CMD_NAME);
            if (Objects.equals(ACTION_CMD, action)){
                if (Objects.equals(CMD_PAUSE, cmd)){
                    playbackManager.pause();
                }

            } else {
                // Apparently media button receiver can work just like this...  We shall see!
                // https://developer.android.com/reference/android/support/v4/media/session/MediaButtonReceiver.html
                MediaButtonReceiver.handleIntent(musicSession, intent);
            }
        }
        timeoutHandler.removeCallbacksAndMessages(null);
        timeoutHandler.sendEmptyMessageDelayed(0, timeToTimeout_ms);

        // If killed restart as soon as there is enough memory
        return START_STICKY;
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {

        // Control the level of access for the specified package name.
        if (allowBrowsing(clientPackageName, clientUid)) {
            // Returns a root ID that clients can use with onLoadChildren() to retrieve
            // the content hierarchy.
            return new BrowserRoot(MEDIA_ROOT_ID, null);
        } else {
            // Clients can connect, but this BrowserRoot is an empty hierachy
            // so onLoadChildren returns nothing. This disables the ability to browse for content.
            return new BrowserRoot(EMPTY_MEDIA_ROOT_ID, null);
        }
    }

    private boolean allowBrowsing(String clientPackageName, int clientUid){
        // TODO look at what this is supposed to do because rn: ¯\_(ツ)_/¯
        // Rough understanding is that it checks if a package is allowed to -
        // use this, really this should perform some checks but its just me so...
        return true;
    }


    @Override // apparently need to do this but the docs are shite for what I am supposed to do here so just leaving empty
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaItem>> result){

        //  Browsing not allowed
        if (TextUtils.equals(EMPTY_MEDIA_ROOT_ID, parentMediaId)) {
            result.sendResult(null);
            return;
        }

        // Assume for example that the music catalog is already loaded/cached.

        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        // Check if this is the root menu:
        if (MEDIA_ROOT_ID.equals(parentMediaId)) {
            // Build the MediaItem objects for the top level,
            // and put them in the mediaItems list...
        } else {
            // Examine the passed parentMediaId to see which submenu we're at,
            // and put the children of that menu in the mediaItems list...
        }
        result.sendResult(mediaItems);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) { // User killed us all
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "app swipe-closed");
        stopSelf();
    }

    @Override
    public void onDestroy() { // AKA GRACEFUL EXIT
        Log.d(TAG, "onDestroy");
        // Service is being killed, kill all related things
        playbackManager.stop();
        musicNotificationManager.stopNotification();
        timeoutHandler.removeCallbacksAndMessages(null);
        musicSession.release();
    }

    // TODO onPlaybackStart() etc from playbackManager.PlaybackServiceCallback

    /* "MediaPlayer consumes a considerable amount of device resources (read battery).
     * Since we are set up as a foreground service, the system would almost never kill it automatically.
     * This leaves with two options:
     * 1. The user kills it after he is done playing music.
     * 2. If there is no music playing for some time, we should stop the service to stop consuming resources."
     *
     * -- // http://sapandiwakar.in/building-a-music-player-app-for-android-2/
     */

    private static class TimeoutHandler extends Handler {
        // Refer back to:
        // http://sapandiwakar.in/building-a-music-player-app-for-android-2/

        // Like reference but with garbage collection
        private final SoftReference<MusicService> musicServiceSoftReference;

        private TimeoutHandler(MusicService service){
            musicServiceSoftReference = new SoftReference<MusicService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MusicService service = musicServiceSoftReference.get();
            if (null != service && null != service.playbackManager.getPlaybackState()){
                if (service.playbackManager.getPlaybackState().isPlaying()){
                    Log.d(service.TAG, "Ignoring timeout as MediaPlayer is still in use");

                } else {
                  Log.d(service.TAG, "Stopping service with timeout handler");
                  service.stopSelf();
                }
            }

        }
    }

    private final class MusicSessionCallback extends MediaSessionCompat.Callback{

    }
}

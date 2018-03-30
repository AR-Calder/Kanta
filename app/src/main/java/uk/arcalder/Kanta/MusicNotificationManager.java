package uk.arcalder.Kanta;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.drm.DrmStore;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

/**
 * Created by Zynch on 29/03/2018.
 */

public class MusicNotificationManager extends BroadcastReceiver {

    String TAG = MusicNotificationManager.class.getSimpleName();

    // Channel ID for Oreo notification channels
    private static final String CHANNEL_ID  = "uk.arcalder.kanta.MUSIC_CHANNEL_ID";
    private static final String CHANNEL_DESC= "Allows Kanta (media player) to be controlled through notifications";

    /*
    * Based on google I/O 2016 Samples
    * https://www.youtube.com/watch?v=iIKxyDRjecU
    * */

    // Intent actions
    public static final String ACTION_PLAY  = "uk.arcalder.kanta.play";
    public static final String ACTION_PAUSE = "uk.arcalder.kanta.pause";
    public static final String ACTION_STOP  = "uk.arcalder.kanta.stop";
    public static final String ACTION_PREV  = "uk.arcalder.kanta.prev";
    public static final String ACTION_NEXT  = "uk.arcalder.kanta.next";

    private MusicService musicService;
    private MediaSessionCompat.Token sessionToken;          // https://www.youtube.com/watch?v=iIKxyDRjecU
    private MediaControllerCompat musicController;          // Does the actually music controlling via mediaService
    private PlaybackStateCompat playbackState;              // Playback state for a MediaSession. This includes a state like STATE_PLAYING,
                                                            // - the current playback position, and the current control capabilities.
    private MediaMetadataCompat metadata;                   // Contains metadata about an item, such as the title, artist, etc.
    private NotificationManager notificationManager;        // Does the actual notification managing for our media notifications

    /*  As per https://stackoverflow.com/a/4812421/5496117 :
    *   "If you give the foreign application an Intent, it will execute your Intent with its own permissions.
    *   But if you give the foreign application a PendingIntent, that application will execute your Intent using
    *   your application's permission."
    *
    *   Thus, a notification will need to use a pending intent
    *   */

    // ID & Request code for pending intents
    private static final int NOTIFICATION_ID    = 123;  // not sure if the numbers actually matter
    private static final int REQUEST_CODE       = 321;

    // Pending Intents
    private final PendingIntent playIntent;             // All pretty self-explanatory
    private final PendingIntent pauseIntent;
    private final PendingIntent stopIntent;
    private final PendingIntent prevIntent;
    private final PendingIntent nextIntent;

    private final int colorOfNotification;              // Something that was mentioned in "Best practices in media playback - Google I/O 2016"

    private boolean notificationStarted = false;        // Helps prevents notification getting spammed

    public MusicNotificationManager(MusicService service) throws RemoteException{
        /*
        * HEAVILY BASED ON:
        * https://github.com/googlesamples/android-MediaBrowserService/blob/master/Application/src/main/java/com/example/android/mediasession/service/notifications/MediaNotificationManager.java
        * */

        musicService = service;

        // if color of notification is not set, it will pick theme default. I have not implemented themes properly so that's probably a bad idea.
        colorOfNotification = R.color.colorBackgroundDark2; // TODO check if this works

        // https://developer.android.com/reference/android/app/NotificationManager.html
        notificationManager = (NotificationManager) musicService.getSystemService(Context.NOTIFICATION_SERVICE);

        String packageName = musicService.getPackageName(); // One of the many fields pending intent requires

        // Set PendingIntents with FLAG_CANCEL_CURRENT -if the PendingIntent already exists, the current one should be canceled before generating a new one.
        // https://developer.android.com/reference/android/app/PendingIntent.html
        playIntent = PendingIntent.getBroadcast(musicService, REQUEST_CODE, new Intent(ACTION_PLAY).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        pauseIntent = PendingIntent.getBroadcast(musicService, REQUEST_CODE, new Intent(ACTION_PAUSE).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        stopIntent = PendingIntent.getBroadcast(musicService, REQUEST_CODE, new Intent(ACTION_STOP).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        prevIntent = PendingIntent.getBroadcast(musicService, REQUEST_CODE, new Intent(ACTION_PREV).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);
        nextIntent = PendingIntent.getBroadcast(musicService, REQUEST_CODE, new Intent(ACTION_NEXT).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel existing notifications just in case service has been restarted
        notificationManager.cancelAll();

        Log.d(TAG, "MusicNotificationManager initialized");
    }


    public void startNotification(){
        // Check if notification actually needs started
        if (!notificationStarted){
            metadata = musicController.getMetadata();
            playbackState = musicController.getPlaybackState();

            Notification notification = buildNotification();
            if (null != notification){
                musicController.registerCallback(musicControllerCallback);
                IntentFilter playbackFilter = new IntentFilter();
                playbackFilter.addAction(ACTION_PLAY);
                playbackFilter.addAction(ACTION_PAUSE);
                playbackFilter.addAction(ACTION_STOP);
                playbackFilter.addAction(ACTION_PREV);
                playbackFilter.addAction(ACTION_NEXT);
                musicService.registerReceiver(this, playbackFilter);

                /*  From https://developer.android.com/reference/android/app/Service.html :
                    A started service can use the startForeground(int, Notification) API to put the service in a foreground state,
                    where the system considers it to be something the user is actively aware of and thus not a candidate for killing when low on memory

                    Where int is ID, notification is the one just created here (notification)*/
                musicService.startForeground(NOTIFICATION_ID, notification);

                // Update notification started tracker
                notificationStarted = true;
                Log.d(TAG, "musicController notification started");
            }
        }
    }

    public void stopNotification(){
        // if notification actually needs stopped
        if (notificationStarted){
           musicController.unregisterCallback(musicControllerCallback);
           try {
               notificationManager.cancel(NOTIFICATION_ID);
               musicService.unregisterReceiver(this);
           } catch (Exception somethingToDoWithReceiverNotBeingRegistered){
               // if it wasn't registered then it didn't need unregistered anyway but just in case:
               Log.e(TAG,"Error: could not de-register notification: " ,somethingToDoWithReceiverNotBeingRegistered);
           }
           // actually remove the notification
           musicService.stopForeground(true);

           // update notification state tracker
           notificationStarted = false;

        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "Received intent action: " + action);

        switch(action){
            case ACTION_PAUSE:
                musicController.getTransportControls().pause();
                break;
            case ACTION_PLAY:
                musicController.getTransportControls().play();
                break;
            case ACTION_NEXT:
                musicController.getTransportControls().skipToNext();
                break;
            case ACTION_PREV:
                musicController.getTransportControls().skipToPrevious();
                break;
            default:
                Log.w(TAG, "Unexpected Action: " + action);
        }
    }


    // resets the session token (first run or session destruction)
    private void resetSessionToken() throws RemoteException {
        MediaSessionCompat.Token currentToken = musicService.getSessionToken();
        // if we have the wrong token (not current token) or session token doesn't exist
        if (sessionToken != null && sessionToken != currentToken || sessionToken == null && currentToken != null){
            if (null != musicController) {
                musicController.unregisterCallback(musicControllerCallback);
            }
            // refresh session token
            sessionToken = currentToken;

            // thus must update controller
            if (null != sessionToken) {
                musicController = new MediaControllerCompat(musicService, sessionToken);
                if (notificationStarted) {
                    musicController.registerCallback(musicControllerCallback);
                }
            }
        }
    }

    // Set the notification's tap action
    private PendingIntent createContentIntent(MediaDescriptionCompat mediaDescription){
        Intent openKantaApp = new Intent(musicService, MainActivity.class);
        // IF encountering problems return to the stack overflow post:
        // https://stackoverflow.com/questions/29321261/what-are-the-differences-between-flag-activity-reset-task-if-needed-and-flag-act
        openKantaApp.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (null != mediaDescription){
            // Also from Google I/O 2016
            // https://developer.android.com/reference/android/support/v4/media/MediaDescriptionCompat.html
            openKantaApp.putExtra(MainActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION);
        }

        return PendingIntent.getActivity(musicService, REQUEST_CODE, openKantaApp, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    // TODO this
    private final MediaControllerCompat.Callback musicControllerCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            // Nothing to display or music playback has been stopped (completely stopped not paused)
            if (state.getState() == PlaybackStateCompat.STATE_NONE || state.getState() == PlaybackStateCompat.STATE_STOPPED){
                // Don't need notification anymore
                stopNotification();
            } else {
                Notification notification = buildNotification();
                if (null != notification){
                    // post notification to notification shade
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Log.d(TAG, "Session destroyed");
            try{
                resetSessionToken();
            }catch (RemoteException re){
                Log.e(TAG, "Failed to reset session token: " + re);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat newMetadata) {
           metadata = newMetadata;
           Log.d(TAG, "Metadata changed:" + metadata);
           Notification notification = buildNotification();
           if (null != notification){
               // post notification to notification shade
               notificationManager.notify(NOTIFICATION_ID, notification);
           }

        }
    };


    private Notification buildNotification(){

        Log.d(TAG, "Creating notification");
        if (null == playbackState || null == metadata)
            return null;

        // Super useful, only stores relevant fields (fuck MediaStore)
        MediaDescriptionCompat mediaDescription = metadata.getDescription();

        // Oh look. There's a method for this (Fuck MediaStore)
        Bitmap albumArt = mediaDescription.getIconBitmap();

        // Since my phone is an android oreo device need to implement notification channels.
        if (null == notificationManager.getNotificationChannel(CHANNEL_ID)){
            // should really be channel_id, channel_name but meh
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, musicService.getPackageName(), NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription(CHANNEL_DESC);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            notificationManager.createNotificationChannel(notificationChannel);
            Log.d(TAG, "createNotificationChannel: New channel created");
        } else {
            Log.d(TAG, "createNotificationChannel: Existing channel reused");
        }


        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(musicService, CHANNEL_ID);
        notificationBuilder.setStyle(
                new android.support.v4.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)) // prev, play/pause, next
                // When notification is deleted (when playback is paused and notification can be
                // deleted) fire MediaButtonPendingIntent with ACTION_STOP.
                .setDeleteIntent(stopIntent)
                // seems like a good idea ¯\_(ツ)_/¯
                .setOnlyAlertOnce(true)
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Pending intent that is fired when user clicks on notification.
                .setContentIntent(createContentIntent(mediaDescription))
                .setColor(colorOfNotification)
                .setLargeIcon(albumArt)
                // Title - Usually Song name.
                .setContentTitle(mediaDescription.getTitle())
                // Subtitle - Usually Artist name.
                .setContentText(mediaDescription.getSubtitle());

        // IN LEFT TO RIGHT ORDER:

        // Previous button
        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            notificationBuilder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "media_previous", prevIntent));
        }

        // Play/Pause button
        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING){
            // @android:drawable/ic_media_next
            notificationBuilder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, "media_play", playIntent));
            notificationBuilder.setOngoing(true);
        } else {
            notificationBuilder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "media_pause", pauseIntent));
            notificationBuilder.setOngoing(false);
        }

        // Next button
        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            notificationBuilder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "media_next", nextIntent));
        }

        if (null == playbackState || !notificationStarted){
            Log.d(TAG, "null == playbackState OR !notificationStarted");
            // "Die die die"
            musicService.stopForeground(true);
        }

        return notificationBuilder.build();
    }


}

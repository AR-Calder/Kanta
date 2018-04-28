package uk.arcalder.Kanta;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by Zynch on 14/04/2018.
 */

public class MusicPlayerService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnPreparedListener  {

    // TODO IMPLEMENT MY NOTIFICATION MANAGER, MAKE IT SIMILAR TO https://github.com/googlesamples/android-MediaBrowserService/blob/master/Application/src/main/java/com/example/android/mediasession/service/MusicService.java
    // TODO JUST CTRL-F For NOTIFICATION

    public static final String TAG = MusicPlayerService.class.getSimpleName();

    public static final String CUSTOM_ACTION = "UPDATE";

    // Channel ID for Oreo notification channels
    private static final String CHANNEL_ID  = "uk.arcalder.kanta.musicplayerservice";
    private static final String CHANNEL_DESC= "Allows Kanta (media player) to be controlled through notifications";

    // ID & Request code for pending intents
    private static final int NOTIFICATION_ID    = 412;  // not sure if the numbers actually matter
    private static final int REQUEST_CODE       = 501;

    private boolean notificationStarted = false;

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // We have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // We don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // We don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // We have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    // Track Playback State
    private static PlaybackStateCompat mPlaybackState;
    // Track service state
    private static boolean isServiceStarted = false;
    // Track current song index
    private int songIndex = -1;

    // MediaPlayer, controls & session
    private MediaPlayer mMediaPlayer;
    private MediaControllerCompat mMediaController;
    private MediaSessionCompat mMediaSession;

    // Notification Manager
    private NotificationManager mNotificationManager;

    // Instance of SongList
    private MusicLibrary mMusicLibrary;

    // Audio Focus Tracker
    private int mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
    // Resume on focus gain tracker
    private boolean mPlayOnFocusGain = false;
    // Audio Manager
    private AudioManager mAudioManager;

    // Broadcast manager to update queue and mini player
    LocalBroadcastManager mLocalBroadcastManager;


    // Register receiver for headset unplugged
    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(null != mMediaPlayer && mMediaPlayer.isPlaying() ) {
                mMediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                currentNotification = buildNotification();
                mNotificationManager.notify(NOTIFICATION_ID, currentNotification);
            }
        }
    };

    // Audio Attribute (Req as of 8.0)
    private AudioAttributes mAudioAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setFlags(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();

    // AudioFocusRequests (Req as of 8.0)
    private AudioFocusRequest mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(mAudioAttributes)
            .setOnAudioFocusChangeListener(this) // This might be wrong
            .build();

    private NotificationCompat.Action mPlayAction;
    private NotificationCompat.Action mPauseAction;
    private NotificationCompat.Action mNextAction;
    private NotificationCompat.Action mPrevAction;

    // TODO ------------------------------Media Playback---------------------------------------
    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay");

            // Try to get focus
            if(!getAudioFocus()){
                Log.d(TAG, "onPlay: Couldn't get audio focus");
                return;
            }

            //Should be started but sometimes not :s
            if(!isServiceStarted){
                Log.d(TAG, "onPlay: Starting Service");
                startService(new Intent(getApplicationContext(), MusicPlayerService.class));
            }

            if(mPlaybackState.getState() == PlaybackStateCompat.STATE_PAUSED) {  // If we are resuming something not trying to play an un-started song
                Log.d(TAG, "onPlay: Starting Player");

                //Doesn't update itself
                mMediaSession.setActive(true);
                mMediaPlayer.start();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                initNoisyReceiver();

                currentNotification = buildNotification();
                mNotificationManager.notify(NOTIFICATION_ID, currentNotification);
            }



            super.onPlay();
            // TODO NOTE: onPlay is really onResume/onNowPlaying, don't try to play songs here
        }

        @Override
        public void onPause() {
            super.onPause();

            if( mMediaPlayer.isPlaying() ) {
                mMediaPlayer.pause();
                removeNoisyReceiver();
                //TODO fix?
                //Notifications
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                stopForeground(false);

                currentNotification = buildNotification();
                mNotificationManager.notify(NOTIFICATION_ID, currentNotification);
            }
            // Notify fragments

        }

        @Override
        public void onPlayFromMediaId(String data, Bundle extras) {
            Log.d(TAG, "onPlayFromMediaId");
            super.onPlayFromMediaId(data, extras);

            //Might have been playing something else
            mMediaPlayer.reset();
            initMediaPlayer();

            // Try to get focus
            if(!getAudioFocus()){
                Log.d(TAG, "onPlayFromMediaId: Couldn't get audio focus");
            }

            //Should be started but sometimes not :s
            if(!isServiceStarted){
                Log.d(TAG, "onPlayFromMediaId: Starting Service");
                startService(new Intent(getApplicationContext(), MusicPlayerService.class));
            }

            Log.d(TAG, "onPlayFromMediaId: data is " + data);

            try {
                FileInputStream inputStream = new FileInputStream(new File(data));
                Log.d(TAG, "onPlayFromMediaId: setDataSource");
                Log.d(TAG, "onPlayFromMediaId: getFD = " + inputStream.getFD());
                mMediaPlayer.setDataSource(inputStream.getFD());
                mMediaPlayer.prepareAsync();
                inputStream.close();

            } catch( IOException e ) {
                Log.e(TAG, "Could not play media from data:", e);
                stopSelf();
            }
//                //afd.close();
//                initMediaSessionMetadata();
            // Notify fragments
            mLocalBroadcastManager.sendBroadcast(new Intent("UPDATE"));
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
//            if( COMMAND_EXAMPLE.equalsIgnoreCase(command) ) {
//                //Custom command here
//            }
            mLocalBroadcastManager.sendBroadcast(new Intent("UPDATE"));
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }

        @Override
        public void onStop() {
            Log.d(TAG, "onStop called");
            releaseAudioFocus();
            setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            stopSelf();
            mMediaSession.setActive(false);
            mMediaPlayer.stop();
            isServiceStarted = false;

            // Remove notifications
            stopForeground(true);

            // Notify fragments
            mLocalBroadcastManager.sendBroadcast(new Intent("UPDATE"));
            super.onStop();

        }

        @Override
        public void onSkipToNext() {
            Song nextSong = mMusicLibrary.getNextSong(); // TODO GET NEXT SONG
            if (null != nextSong) {
                Log.d(TAG, "onSkipToNext: nextSong is '" + nextSong.getTitle() + "'");
                //mMusicLibrary.setCurrentSong(nextSong);
                // get current song provides the song at the current position so this works for both
                onPlayFromMediaId(nextSong.getData(), new Bundle());
            } else {
                Log.d(TAG, "onSkipToNext: nextSong was null");
                onStop();
            }

            // Notify fragments
            mLocalBroadcastManager.sendBroadcast(new Intent("UPDATE"));
            super.onSkipToNext();
        }

        @Override
        public void onSkipToPrevious() {
            Song prevSong = mMusicLibrary.getPreviousSong(); // TODO GET PREV SONG
            if (null != prevSong) {
                Log.d(TAG, "onSkipToPrevious: prevSong is '" + prevSong.getTitle() + "'");
                mMusicLibrary.setCurrentSong(prevSong);
                onPlayFromMediaId(mMusicLibrary.getCurrentSong().getData(), new Bundle());
            } else {
                Log.d(TAG, "onSkipToPrevious: prevSong was null");
                onStop();
            }

            // Notify fragments
            mLocalBroadcastManager.sendBroadcast(new Intent("UPDATE"));
            super.onSkipToPrevious();
        }


    };

    @Override
    public void onCreate() {
        super.onCreate();

        // --------------------------External Communications----------------------
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);


        // -------------------------Media Button Receivers------------------------
        mPrevAction =
                new NotificationCompat.Action.Builder(
                        android.R.drawable.ic_media_previous,
                        "previous",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackState.ACTION_SKIP_TO_PREVIOUS)).build();

        mNextAction =
                new NotificationCompat.Action.Builder(
                        android.R.drawable.ic_media_next,
                        "next",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackState.ACTION_SKIP_TO_NEXT)).build();

        mPauseAction =
                new NotificationCompat.Action.Builder(
                        android.R.drawable.ic_media_pause,
                       "pause",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackState.ACTION_PAUSE)).build();

        mPlayAction =
                new NotificationCompat.Action.Builder(
                        android.R.drawable.ic_media_play,
                        "play",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackState.ACTION_PLAY)).build();

        //-------------------------------Main init-------------------------------------------
        mMusicLibrary = MusicLibrary.getInstance();
        initMediaPlayer();
        initMediaSession();
        initNoisyReceiver();
        initMediaController();

        // TODO Notifications
        Log.d(TAG, "onCreate: getPlayBackState");
        mPlaybackState = mMediaController.getPlaybackState();

        int default_playback = 1 << 1;
        Log.d(TAG, "onCreate: set default_playback");
        setMediaPlaybackState(default_playback);

        //-----------------------------Init Notifications------------------------------------
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        // Shite fix for shite issue
        //https://stackoverflow.com/questions/44425584/context-startforegroundservice-did-not-then-call-service-startforeground
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_MIN);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }

        Log.d(TAG, "onCreate MusicService creating MediaSession, MediaPlayer, and MediaNotificationManager");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        releaseAudioFocus();
        removeNoisyReceiver();
        if (mMediaPlayer != null) {
            Log.d(TAG, "onDestroy: mediaplayer.reset()");
            mMediaPlayer.reset();
            Log.d(TAG, "onDestroy: mediaplayer.release()");
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mMediaSession != null) {
            Log.d(TAG, "onDestroy: mediasession.release()");
            mMediaSession.release();
            mMediaSession = null;
        }

        Log.d(TAG, "onDestroy: stop Foreground");
        stopForeground(true);


        //Log.d(TAG, "onDestroy: cancelInitSongs");
        // TODO if for some reason the app is closed before the library is able to load
        // Seemed like a good idea but crashes
        // "Attempt to invoke virtual method 'boolean uk.arcalder.Kanta.SongList$AsyncInitSongs.isCancelled()' on a null object reference"
        // mSongList.cancelInitSongs();
        Log.d(TAG, "onDestroy: super.onDestroy");
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");

        // TODO if for some reason the app is closed before the library is able to load
        // Seemed like a good idea but crashes
        // "Attempt to invoke virtual method 'boolean uk.arcalder.Kanta.SongList$AsyncInitSongs.isCancelled()' on a null object reference"
        // mSongList.cancelInitSongs()

        // Want to kill service if user wishes to close it.
        Log.d(TAG, "onTaskRemoved: stopSelf()");
        stopSelf();
        if (rootIntent != null) {
            Log.d(TAG, "onTaskRemoved: super.onTaskRemoved");
            super.onTaskRemoved(rootIntent);
        }
    }

    private void initMediaController() {
        try {
            mMediaController = new MediaControllerCompat(getApplicationContext(), mMediaSession.getSessionToken());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private static boolean isNoisyReceiverActive = false;

    private void initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mNoisyReceiver, filter);
        isNoisyReceiverActive = true;
    }

    private void removeNoisyReceiver(){
        if (isNoisyReceiverActive){
            Log.d(TAG, "removeNoisyReceiver: removing");
            unregisterReceiver(mNoisyReceiver);
            isNoisyReceiverActive = false;
        } else {
            Log.d(TAG, "removeNoisyReceiver: already removed");
        }

    }

    private void initMediaPlayer() {
        mAudioManager = (mAudioManager == null) ? (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE) : mAudioManager;
        if ( mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setFlags(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mMediaPlayer.setVolume(1.0f, 1.0f);

            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
        }
    }

    private void initMediaSession() {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        // Create a MediaSession
        mMediaSession = new MediaSessionCompat(getApplicationContext(), TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS );

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mMediaSession.setPlaybackState( new PlaybackStateCompat.Builder()
                                                .setActions(
                                                    PlaybackState.ACTION_PLAY |
                                                    PlaybackState.ACTION_SKIP_TO_NEXT |
                                                    PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                                                    PlaybackState.ACTION_PLAY_PAUSE).build());

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSession.setMediaButtonReceiver(pendingIntent);

        // mMediaSessionCallback() has methods that handle callbacks from a media controller
        mMediaSession.setCallback(mMediaSessionCallback);

        // Set the session's token so that client activities can communicate with it.
        this.setSessionToken(mMediaSession.getSessionToken());
    }

    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackState.STATE_PLAYING ) {
            playbackStateBuilder.setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT |
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS);
        } else {
            playbackStateBuilder.setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_SKIP_TO_NEXT |
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS);
        }
        playbackStateBuilder.setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaSession.setPlaybackState(playbackStateBuilder.build());
    }

    private void initMediaSessionMetadata() {
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        //Notification icon in card
        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        //lock screen icon for pre lollipop
        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, "Display Title");
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, "Display Subtitle");
        metadataBuilder.putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, 1);
        metadataBuilder.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, 1);

        mMediaSession.setMetadata(metadataBuilder.build());
    }

    private boolean getAudioFocus() {
        Log.d(TAG, "tryToGetAudioFocus");
        int result = mAudioManager.requestAudioFocus(mAudioFocusRequest);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_FOCUSED;
            // TODO Notify
            return true;
        } else {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
            return false;
        }

    }

    private void releaseAudioFocus() {
        Log.d(TAG, "giveUpAudioFocus");
        if (mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }


    // ------------------------Media Service Listeners -------------------------------


    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "onAudioFocusChange. focusChange: "+ focusChange);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // Audio focus regained

                // Update State
                mCurrentAudioFocusState = AUDIO_FOCUSED;

                if (null == mMediaPlayer) {
                    break;
                }

                // Reset levels
                mMediaPlayer.setVolume(1.0f, 1.0f);

                if (mPlayOnFocusGain && !mMediaPlayer.isPlaying()){
                    // Media player isn't already playing and should be resumed
                    mMediaController.getTransportControls().play();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Audio focus lost but can duck

                if(null != mMediaPlayer ) {
                    mMediaPlayer.setVolume(0.2f, 0.2f);
                }
                // Update State
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Audio focus lost but will gain it back (soon-ish)
                if (mMediaPlayer.isPlaying()){
                    mMediaController.getTransportControls().pause();
                }
                // Update State
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost audio focus, possibly "permanently"
                if (mMediaPlayer.isPlaying()){
                    mMediaController.getTransportControls().pause(); // should really be a on stop but I don't have seek to implemented
                }
                // Update State
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                break;
        }
        //Notifications
        Log.d(TAG,"onAudioFocusChange: buildNotification");
        currentNotification = buildNotification();
        mNotificationManager.notify(NOTIFICATION_ID, currentNotification);
    }



    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion");
        if( mMediaPlayer != null ) {
            // TODO get next song? (and then don't release)
            mMediaSession.getController().getTransportControls().skipToNext();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        isServiceStarted = true;
        // TODO USE APP COMPAT EVERYTHING
        MediaButtonReceiver.handleIntent(mMediaSession, intent);
        mPlaybackState = mMediaController.getPlaybackState();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onPrepared");
        mMediaSession.setActive(true);
        mediaPlayer.start();
        setMediaPlaybackState(PlaybackState.STATE_PLAYING);
        initNoisyReceiver();

        //Notifications
        Intent intent = new Intent(this, MusicPlayerService.class);
        ContextCompat.startForegroundService(this, intent);

        currentNotification = buildNotification();
        this.startForeground(NOTIFICATION_ID, currentNotification);
    }

    // ------------------------------Notifications--------------------------------

    private Notification currentNotification = null;

    private Notification buildNotification(){

        Log.d(TAG, "Creating notification");

        // Get current song
        Song current_song = mMusicLibrary.getCurrentSong();

        mPlaybackState = mMediaController.getPlaybackState();

        // Get Album Art


        Bitmap albumArt = (null != current_song.getArt() && !"".equals(current_song.getArt())) ? BitmapFactory.decodeFile(current_song.getArt()) : BitmapFactory.decodeResource(getResources(), R.drawable.default_album);

        Log.d(TAG, "buildNotification: mPlaybackState.getActions is " + mPlaybackState);

        // Since my phone is an android oreo device need to implement notification channels.
        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null){
            // should really be channel_id, channel_name but meh

            CharSequence name = "Kanta Music";

            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, this.getPackageName(), NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.MAGENTA); // TODO check I have notification LED
            notificationChannel.setDescription(CHANNEL_DESC);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            mNotificationManager.createNotificationChannel(notificationChannel);
            Log.d(TAG, "createNotificationChannel: New channel created");
        } else {
            Log.d(TAG, "createNotificationChannel: Existing channel reused");
        }


        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        notificationBuilder//.setStyle(new Notification.MediaStyle().setMediaSession(mMediaSession.getSessionToken()))
                // Has to be true or it spams ¯\_(ツ)_/¯
                .setOnlyAlertOnce(true)
                // Pending intent that is fired when user clicks on notification.
                .setContentIntent(createContentIntent(current_song))
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setLargeIcon(albumArt)
                .setSmallIcon((mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play)
                // Title - Usually Song name.
                .setContentTitle(current_song.getTitle())
                // Subtitle - Usually Artist name.
                .setContentText(current_song.getArtist())
                .setSubText(current_song.getAlbum())
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                // When notification is deleted (when playback is paused and notification can be
                // deleted) fire MediaButtonPendingIntent with ACTION_STOP.
                        .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackState.ACTION_STOP)); //TODO fix this janky test

                //.addAction(mPrevAction).addAction(mPauseAction).addAction(mNextAction);


        Log.d(TAG, "createNotificationChannel: Set style");
        // IN LEFT TO RIGHT ORDER:

        // Previous button
        notificationBuilder.addAction(mPrevAction);

        Log.d(TAG, "buildNotification: PlaybackState is " + mPlaybackState.getState());

        // Play/Pause button
        if (mPlaybackState.getState() != PlaybackState.STATE_PLAYING){
            // @android:drawable/ic_media_next
            notificationBuilder.addAction(mPlayAction);
            notificationBuilder.setOngoing(true);
        } else {
            notificationBuilder.addAction(mPauseAction);
            notificationBuilder.setOngoing(false);
        }

        // Next button
        notificationBuilder.addAction(mNextAction);


        // Set style after actions so no crash
        notificationBuilder.setStyle(
                new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        if (null == mPlaybackState){
            Log.d(TAG, "null == playbackState OR !notificationStarted");
            // "Die die die"
            this.stopForeground(true);
        }

        Log.d(TAG, "createNotificationChannel: build");

        return notificationBuilder.build();
    }

    // Set the notification's tap action
    private PendingIntent createContentIntent(Song song){
        Intent openUI  = new Intent(this, MainActivity.class);
        // IF encountering problems return to the stack overflow post:
        // https://stackoverflow.com/questions/29321261/what-are-the-differences-between-flag-activity-reset-task-if-needed-and-flag-act
        openUI .setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (null != song){ // TODO Come fix later
            // Also from Google I/O 2016
            // https://developer.android.com/reference/android/support/v4/media/MediaDescriptionCompat.html
            //openUI .putExtra(MainActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION);
        }

        return PendingIntent.getActivity(this, REQUEST_CODE, openUI , PendingIntent.FLAG_CANCEL_CURRENT);
    }


    // -------------------------Shit media service 'needs' but doesn't 'need' ------------------------------------------
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }

        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result, @NonNull Bundle options) {
    }


}
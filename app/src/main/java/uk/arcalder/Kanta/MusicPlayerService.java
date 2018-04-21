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
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by Zynch on 14/04/2018.
 */

public class MusicPlayerService extends MediaBrowserService implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnPreparedListener  {

    // TODO IMPLEMENT MY NOTIFICATION MANAGER, MAKE IT SIMILAR TO https://github.com/googlesamples/android-MediaBrowserService/blob/master/Application/src/main/java/com/example/android/mediasession/service/MusicService.java
    // TODO JUST CTRL-F For NOTIFICATION

    public static final String TAG = MusicPlayerService.class.getSimpleName();

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
    private static PlaybackState mPlaybackState;
    // Track service state
    private static boolean isServiceStarted = false;
    // Track current song index
    private int songIndex = -1;

    // MediaPlayer, controls & session
    private MediaPlayer mMediaPlayer;
    private MediaController mMediaController;
    private MediaSession mMediaSession;

    // Notification Manager
    private NotificationManager mNotificationManager;

    // Instance of SongList
    private SongList mSongList;

    // Audio Focus Tracker
    private int mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
    // Resume on focus gain tracker
    private boolean mPlayOnFocusGain = false;
    // Audio Manager
    private AudioManager mAudioManager;

    // Register receiver for headset unplugged
    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(null != mMediaPlayer && mMediaPlayer.isPlaying() ) {
                mMediaPlayer.pause();
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

    private Notification.Action mPlayAction;
    private Notification.Action mPauseAction;
    private Notification.Action mNextAction;
    private Notification.Action mPrevAction;

    // Media Playback
    private MediaSession.Callback mMediaSessionCallback = new MediaSession.Callback() {

        @Override
        public void onPlay() {
            super.onPlay();
            if( !getAudioFocus() ) {
                Log.d(TAG, "onPlay: Failed to getAudioFocus");
                return;
            }

            //Should be started but sometimes not :s
            if(!isServiceStarted){
                Log.d(TAG, "onPlay: Starting Service");
                startService(new Intent(getApplicationContext(), MusicPlayerService.class));
            }

            // If index not initialized
            songIndex = (songIndex != -1) ? songIndex : 0;
            Song thisSong = mSongList.getSongByIndex(songIndex);


            mMediaSession.setActive(true);
            setMediaPlaybackState(PlaybackState.STATE_PLAYING);

            try {
                FileInputStream inputStream = new FileInputStream(new File(thisSong.getData()));
                Log.d(TAG, "onPlay: setDataSource");
                Log.d(TAG, "onPlay: getFD = " + inputStream.getFD());
                mMediaPlayer.setDataSource(inputStream.getFD());
                mMediaPlayer.prepareAsync();
                inputStream.close();

            } catch( IOException e ) {
                Log.e(TAG, "Could not play media from data:", e);
                stopSelf();
            }

            //TODO fix notification?
            //Notifications
            currentNotification = buildNotification();
            startForeground(NOTIFICATION_ID, currentNotification);

        }

        @Override
        public void onPause() {
            super.onPause();

            if( mMediaPlayer.isPlaying() ) {
                mMediaPlayer.pause();
                unregisterReceiver(mNoisyReceiver);
                setMediaPlaybackState(PlaybackState.STATE_PAUSED);
                //TODO fix?
                //Notifications
                currentNotification = buildNotification();
                startForeground(NOTIFICATION_ID, currentNotification);
            }
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
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
//            if( COMMAND_EXAMPLE.equalsIgnoreCase(command) ) {
//                //Custom command here
//            }
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
        }

        @Override
        public void onStop() {
            super.onStop();
            releaseAudioFocus();
            stopSelf();
            isServiceStarted = false;
        }



    };

    @Override
    public void onCreate() {
        super.onCreate();


        mPrevAction =
                new Notification.Action.Builder(
                        android.R.drawable.ic_media_previous,
                        "previous",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackState.ACTION_SKIP_TO_PREVIOUS)).build();

        mNextAction =
                new Notification.Action.Builder(
                        android.R.drawable.ic_media_next,
                        "next",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackState.ACTION_SKIP_TO_NEXT)).build();

        mPauseAction =
                new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,
                       "pause",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackState.ACTION_PAUSE)).build();

        mPlayAction =
                new Notification.Action.Builder(
                        android.R.drawable.ic_media_play,
                        "play",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackState.ACTION_PLAY)).build();

        mSongList = SongList.getInstance();
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

        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Log.d(TAG, "onCreate MusicService creating MediaSession, MediaPlayer, and MediaNotificationManager");
    }

    private void initMediaController() {
        try {
            mMediaController = new MediaController(getApplicationContext(), mMediaSession.getSessionToken());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mNoisyReceiver, filter);
    }



    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        releaseAudioFocus();
        if (isServiceStarted) {
            unregisterReceiver(mNoisyReceiver);
        }
        mMediaSession.release();
    }

    private void initMediaPlayer() {
        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
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

    private void initMediaSession() {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        // Create a MediaSession
        mMediaSession = new MediaSession(getApplicationContext(), TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS );

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mMediaSession.setPlaybackState( new PlaybackState.Builder()
                                                .setActions(
                                                    PlaybackState.ACTION_PLAY |
                                                    PlaybackState.ACTION_PLAY_PAUSE).build());

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSession.setMediaButtonReceiver(pendingIntent);

        // mMediaSessionCallback() has methods that handle callbacks from a media controller
        mMediaSession.setCallback(mMediaSessionCallback);

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mMediaSession.getSessionToken());
    }

    private void setMediaPlaybackState(int state) {
        PlaybackState.Builder playbackStateBuilder = new PlaybackState.Builder();
        if( state == PlaybackState.STATE_PLAYING ) {
            playbackStateBuilder.setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PAUSE);
        } else {
            playbackStateBuilder.setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY);
        }
        playbackStateBuilder.setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaSession.setPlaybackState(playbackStateBuilder.build());
    }

    private void initMediaSessionMetadata() {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
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
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }

        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull Result<List<MediaBrowser.MediaItem>> result) {

    }


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
                    mMediaPlayer.start();
                    setMediaPlaybackState(PlaybackState.STATE_PLAYING);
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
                    setMediaPlaybackState(PlaybackState.STATE_PAUSED);
                    mMediaPlayer.pause();
                }
                // Update State
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost audio focus, possibly "permanently"
                if (mMediaPlayer.isPlaying()){
                    mMediaPlayer.stop();
                    setMediaPlaybackState(PlaybackState.STATE_STOPPED);
                }
                // Update State
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                break;
        }
        //Notifications
        Log.d(TAG,"onAudioFocusChange: buildNotification");
        currentNotification = buildNotification();
        startForeground(NOTIFICATION_ID, currentNotification);
    }



    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion");
        if( mMediaPlayer != null ) {
            // TODO get next song? (and then don't release)
            mMediaPlayer.release();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        isServiceStarted = true;
        // TODO USE APP COMPAT EVERYTHING
        //MediaButtonReceiver.handleIntent(mMediaSession, intent);
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
        Song current_song = mSongList.getCurrentSong();

        // Get Album Art

        // (null != current_song) ? BitmapFactory.decodeFile(current_song.getArt()) :
        //Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.ic_sync_black_24dp);;

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


        final Notification.Builder notificationBuilder = new Notification.Builder(this, CHANNEL_ID);
        notificationBuilder//.setStyle(new Notification.MediaStyle().setMediaSession(mMediaSession.getSessionToken()))
                // Has to be true or it spams ¯\_(ツ)_/¯
                .setOnlyAlertOnce(true)
                // Pending intent that is fired when user clicks on notification.
                .setContentIntent(createContentIntent(current_song))
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                //.setLargeIcon(albumArt)
                .setSmallIcon(android.R.drawable.stat_notify_missed_call)
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


        // Play/Pause button
        if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING){
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
                new Notification.MediaStyle()
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


}
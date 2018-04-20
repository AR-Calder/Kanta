package uk.arcalder.Kanta;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zynch on 14/04/2018.
 */

public class MusicPlayerService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnPreparedListener  {

    // TODO IMPLEMENT MY NOTIFICATION MANAGER, MAKE IT SIMILAR TO https://github.com/googlesamples/android-MediaBrowserService/blob/master/Application/src/main/java/com/example/android/mediasession/service/MusicService.java
    // TODO JUST CTRL-F For NOTIFICATION

    public static final String TAG = MusicPlayerService.class.getSimpleName();

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
    private android.support.v4.media.session.MediaControllerCompat.TransportControls mTransportControls;
    private MediaSessionCompat mMediaSessionCompat;

    // Instance of SongList
    private SongList mSongList;

    // Notification Manager
    MusicNotificationManager mMusicNotificationManager;

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

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

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

            if (songIndex != -1){
                mSongList.getSongByIndex(songIndex);
            }

            mMediaSessionCompat.setActive(true);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);

            //TODO show notifi? ?
            //mMusicNotificationManager.startNotification();
        }

        @Override
        public void onPause() {
            super.onPause();

            if( mMediaPlayer.isPlaying() ) {
                mMediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                //TODO showPausedNotification();
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

    };

    @Override
    public void onCreate() {
        super.onCreate();
        mSongList = SongList.getInstance();
        initMediaPlayer();
        initMediaSession();
        initNoisyReceiver();

        mMusicNotificationManager = new MusicNotificationManager(this);

        int default_playback = 1 << 1;
        Log.d(TAG, "onCreate: set default_playback");
        setMediaPlaybackState(default_playback);

        Log.d(TAG, "onCreate MusicService creating MediaSession, MediaPlayer, and MediaNotificationManager");
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
        mMediaSessionCompat.release();
        releaseAudioFocus();
        if (isServiceStarted) {
            unregisterReceiver(mNoisyReceiver);
        }
        NotificationManagerCompat.from(this).cancel(1);
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
        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), TAG, mediaButtonReceiver, null);

        mMediaSessionCompat.setCallback(mMediaSessionCallback);
        mMediaSessionCompat.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );

        mTransportControls = mMediaSessionCompat.getController().getTransportControls();

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        setSessionToken(mMediaSessionCompat.getSessionToken());
    }

    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaSessionCompat.setPlaybackState(playbackstateBuilder.build());
    }

    private void initMediaSessionMetadata() {
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        //Notification icon in card
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        //lock screen icon for pre lollipop
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Display Title");
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Display Subtitle");
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1);

        mMediaSessionCompat.setMetadata(metadataBuilder.build());
    }

    private boolean getAudioFocus() {
        Log.d(TAG, "tryToGetAudioFocus");
        int result = mAudioManager.requestAudioFocus(mAudioFocusRequest);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_FOCUSED;
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


    // ------------------------Requirements -------------------------------
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
                if (null != mMediaPlayer){
                    mMediaPlayer.pause();
                }
                // Update State
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost audio focus, probably "permanently"
                mMediaPlayer.stop();
                // Update State
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                break;
        }
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
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onPrepared");
        mMediaSessionCompat.setActive(true);
        mediaPlayer.start();
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        initNoisyReceiver();
    }

}
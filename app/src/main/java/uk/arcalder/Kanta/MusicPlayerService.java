package uk.arcalder.Kanta;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.MediaController;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Stack;

/**
 * Created by arcalder on 24/03/18.
 */

public class MusicPlayerService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener {

    //Tag for debugging
    final static String TAG = "MusicPlayerService";

    // MEMBERS
    private MediaPlayer mediaPlayer;

    // for checking state
    enum MusicServiceState {
        // MediaPlayer is stopped and not prepared to play
        Stopped,

        // MediaPlayer is preparing...
        Preparing,

        // Playback active - media player ready!
        // (but the media player may actually be paused in this state if we don't have audio focus).
        Playing,

        // So that we know we have to resume playback once we get focus back)
        // playback paused (media player ready!)
        Paused
    }

    // for checking if we have focus or not
    AudioManager audioManager;


    // Current service state
    MusicServiceState musicServiceState = MusicServiceState.Preparing;

    // ---------INTENT ACTIONS------------
    public static final String BROADCAST_ACTION             = "uk.kanta.musicplayer.action.MUSIC_PLAYER_SERVICE";
    public static final String BROADCAST_EXTRA_GET_ACTION   = "uk.kanta.musicplayer.extras.MUSIC_PLAYER_SERVICE";

    public static final String BROADCAST_ACTION_PLAY        = "uk.kanta.musicplayer.action.PLAY";
    public static final String BROADCAST_ACTION_PAUSE       = "uk.kanta.musicplayer.action.PAUSE";
    public static final String BROADCAST_ACTION_TOGGLEPLAY  = "uk.kanta.musicplayer.action.TOGGLEPLAY";
    public static final String BROADCAST_ACTION_STOP        = "uk.kanta.musicplayer.action.STOP";
    public static final String BROADCAST_ACTION_NEXT        = "uk.kanta.musicplayer.action.NEXT";
    public static final String BROADCAST_ACTION_PREVIOUS    = "uk.kanta.musicplayer.action.PREVIOUS";


    // --------LOCK SCREEN WIDGET---------
    // Music controls widget for lock-screen
    // TODO COME BACK TO THIS
    MediaController musicController;

    // -----------SONG SET----------------
    // Tracks songs to be played and those previously played

    // Deque of songs previously played
    // once size > history_size oldest item will be removed
    private ArrayDeque<Song> previousSongs;

    // Song that is currently playing
    private Song currentSong = null;

    // Deque of songs to be played
    // Will often be replaced with new song target
    private ArrayDeque<Song> nextSongs;

    // -----------SONG QUEUE--------------
    // a queue of songs that should be played before returning to currentSongs
    private ArrayDeque<Song> songQueue;
    // determines whether on getPrevious should move this to queue or nextSongs
    // boolean fromQueue = currentSong.getIsCurrentSongFromQueue()

    // -----------PLAY MODES--------------
    private enum PlayMode{
        // All of these are pretty self-explanatory
        normalMode,
        repeatMode,
        shuffleMode
    }

    public void onCreate(){
        super.onCreate();

        Context context = getApplicationContext();

        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        initMusicPlayer();


        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(musicPlayerServiceBroadcastReceiver, new IntentFilter(MusicPlayerService.BROADCAST_ACTION));

        IntentFilter headsetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        // TODO registerReceiver()
    }

    public void initMusicPlayer(){
        if (null == mediaPlayer){
            mediaPlayer = new MediaPlayer();
        }

        // let mediaPlayer know what type of content it will be playing
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
        );

        // keep service alive even when screen is off
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        Log.w(TAG, "initMusicPlayer");
    }
// TODO
//    protected void onStop(){
//        super.onStop();
//        if (isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
//            Log.d(TAG, "onStop: don't release MediaPlayer as screen is rotating & playing");
//        } else {
//            mPlayerAdapter.release();
//            Log.d(TAG, "onStop: release MediaPlayer");
//        }
//
//    }

    BroadcastReceiver musicPlayerServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action == null) {
                Log.w(TAG, "musicPlayerServiceBroadcastReceiver: Action was null");
                return;
            }

            switch (action){
                case MusicPlayerService.BROADCAST_ACTION_PLAY:
                    break;
                case MusicPlayerService.BROADCAST_ACTION_PAUSE:
                    break;
                case MusicPlayerService.BROADCAST_ACTION_TOGGLEPLAY:
                    break;
                case MusicPlayerService.BROADCAST_ACTION_STOP:
                    break;
                case MusicPlayerService.BROADCAST_ACTION_NEXT:
                    break;
                case MusicPlayerService.BROADCAST_ACTION_PREVIOUS:
                    break;
            }

            Log.w(TAG, "musicPlayerServiceBroadcastReceiver: Received");
        }
    };

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }
}

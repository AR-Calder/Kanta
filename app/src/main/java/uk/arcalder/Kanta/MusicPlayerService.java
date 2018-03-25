package uk.arcalder.Kanta;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.MediaController;

import java.util.ArrayDeque;

/**
 * Created by arcalder on 24/03/18.
 */

public class MusicPlayerService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener {

    // ---------INTENT ACTIONS------------
    public static final String INTENT_BASE_NAME         = "uk.arcalder.kanta.MusicPlayerService";
    public static final String INTENT_EXTRA_GET_ACTION  = INTENT_BASE_NAME + ".extras.MUSIC_PLAYER_SERVICE";

    public static final String INTENT_ACTION_PLAY       = INTENT_BASE_NAME + ".action.PLAY";
    public static final String INTENT_ACTION_PAUSE      = INTENT_BASE_NAME + ".action.PAUSE";
    public static final String INTENT_ACTION_TOGGLEPLAY = INTENT_BASE_NAME + ".action.TOGGLEPLAY";
    public static final String INTENT_ACTION_STOP       = INTENT_BASE_NAME + ".action.STOP";
    public static final String INTENT_ACTION_NEXT       = INTENT_BASE_NAME + ".action.NEXT";
    public static final String INTENT_ACTION_PREVIOUS   = INTENT_BASE_NAME + ".action.PREVIOUS";


    // Potentially could get off with just sending a bunch of these
    public static final String INTENT_ACTION_QUEUE_SONG   = INTENT_BASE_NAME + ".action.ADD_SONG_TO_QUEUE_END";

    // ---------GENERAL MEMBERS-----------

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

    // Current service state
    MusicServiceState musicServiceState = MusicServiceState.Preparing;

    // for checking if we have focus or not
    AudioManager audioManager;


    public void onCreate(){
        super.onCreate();

        // Get context
        Context context = getApplicationContext();

        // init audio services
        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        initMusicPlayer();

        // register broadcast receiver to listen for commands from kanta components
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(musicPlayerServiceBroadcastReceiver, new IntentFilter(MusicPlayerService.INTENT_BASE_NAME));

        IntentFilter headsetPluggedInFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetPluggedInBroadcastReceiver, headsetPluggedInFilter);

        IntentFilter headsetUnpluggedFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(headsetUnpluggedBroadcastReceiver, headsetUnpluggedFilter);

        Log.w(TAG, "onCreate");
    }

    public void initMusicPlayer() {
        if (null == mediaPlayer) {
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
    private ArrayDeque<Song> playSet;

    // -----------SONG QUEUE--------------
    // a queue of songs that should be played before returning to currentSongs
    private ArrayDeque<Song> songQueue;
    // determines whether on getPrevious should move this to queue or playSet
    // boolean fromQueue = currentSong.getIsCurrentSongFromQueue()

    // -----------PLAY MODES--------------
    private enum PlayMode{
        // All of these are pretty self-explanatory
        normalMode,
        repeatMode,
        shuffleMode
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

    // set the current play set
    public void setPlaySet(ArrayDeque<Song> songs){
        playSet = songs;
    }

    public void addToSongQueue(ArrayDeque<Song> songs){
        songQueue.addAll(songs);
    }

    public void getPreviousSong(){
        if (currentSong.getIsCurrentSongFromQueue()){
            // current song is from queue so add it back to queue
            songQueue.addLast(currentSong);

        } else {
            // current song is from playset so add it back to playset
            playSet.addLast(currentSong);

        }
        currentSong = previousSongs.getLast();
    }

    public void stopMusicPlayerService(){
        if (mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;

            Log.w(TAG, "stopMusicPlayerService");
        }
    }

    BroadcastReceiver headsetPluggedInBroadcastReceiver = new BroadcastReceiver() {
        // Sticky intents seem retarded but whatever
        // https://stackoverflow.com/questions/4092438/intent-action-headset-plug-is-received-when-activity-starts

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(Intent.ACTION_HEADSET_PLUG)){
                Log.w(TAG, "headsetPlugBroadcastReceiver: ACTION_HEADSET_PLUG");

                boolean headsetPluggedIn = (intent.getIntExtra("state", 0) == 1);

                // If music is playing while headset is plugged in, pause it
                if (headsetPluggedIn && musicServiceState == MusicServiceState.Playing && MusicPlayerLogic.settings.get("pause_on_headset_in", false)){
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);

                    // Tell musicplayerservice to pause
                    Intent sendActionIntent = new Intent(MusicPlayerService.INTENT_BASE_NAME);
                    sendActionIntent.putExtra(MusicPlayerService.INTENT_EXTRA_GET_ACTION, MusicPlayerService.INTENT_ACTION_PAUSE);

                    localBroadcastManager.sendBroadcast(sendActionIntent);
                }
            }
        }
    };

    BroadcastReceiver headsetUnpluggedBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(Intent.ACTION_HEADSET_PLUG)){
                Log.w(TAG, "headsetPlugBroadcastReceiver: ACTION_HEADSET_PLUG");

                // If music is playing while headset is unplugged, pause it
                if (musicServiceState == MusicServiceState.Playing && MusicPlayerLogic.settings.get("pause_on_headset_out", true)){
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);

                    // Tell musicplayerservice to pause
                    Intent sendActionIntent = new Intent(MusicPlayerService.INTENT_BASE_NAME);
                    sendActionIntent.putExtra(MusicPlayerService.INTENT_EXTRA_GET_ACTION, MusicPlayerService.INTENT_ACTION_PAUSE);

                    localBroadcastManager.sendBroadcast(sendActionIntent);
                }
            }
        }
    };

    BroadcastReceiver musicPlayerServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action == null) {
                Log.w(TAG, "musicPlayerServiceBroadcastReceiver: Action was null");
                return;
            }

            switch (action){
                case MusicPlayerService.INTENT_ACTION_PLAY:
                    break;
                case MusicPlayerService.INTENT_ACTION_PAUSE:
                    break;
                case MusicPlayerService.INTENT_ACTION_TOGGLEPLAY:
                    break;
                case MusicPlayerService.INTENT_ACTION_STOP:
                    break;
                case MusicPlayerService.INTENT_ACTION_NEXT:
                    break;
                case MusicPlayerService.INTENT_ACTION_PREVIOUS:
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

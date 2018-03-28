package uk.arcalder.Kanta;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.MediaController;

import java.util.ArrayDeque;
import java.util.LinkedList;

/**
 * Created by arcalder on 24/03/18.
 */

public class MusicPlayerService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener {

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

    // (linked) list of songs to be played
    // Will often be replaced with new song target
    private LinkedList<Song> playSet;

    // -----------SONG QUEUE--------------
    // a 'queue' (actually linked list) of songs that should be played before returning to currentSongs
    private LinkedList<Song> songQueue;
    // determines whether on getPrevious should move this to queue or playSet
    // boolean fromQueue = currentSong.getIsCurrentSongFromQueue()

    // -----------PLAY MODES--------------
    private enum PlayMode{
        // All of these are pretty self-explanatory
        normalMode,
        repeatMode,
        shuffleMode
    }
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
        Stopped,
        Preparing,
        Playing,
        Paused

    }

    // Current service state
    MusicServiceState musicServiceState = MusicServiceState.Preparing;


    // for checking if we have focus or not
    AudioManager audioManager;

    public void onCreate(){
        super.onCreate();
        Log.w(TAG, "onCreate()");

        // Get context
        Context context = getApplicationContext();

        // init audio services
        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        initMusicPlayer();

        // register broadcast receiver to listen for commands from kanta components
        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .registerReceiver(musicPlayerServiceBroadcastReceiver, new IntentFilter(MusicPlayerService.INTENT_BASE_NAME));

        // Headset plugged in filter
        IntentFilter headsetPluggedInFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetPluggedInBroadcastReceiver, headsetPluggedInFilter);

        // Headset unplug filter
        IntentFilter headsetUnpluggedFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(headsetUnpluggedBroadcastReceiver, headsetUnpluggedFilter);



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

        // wake events
        mediaPlayer.setOnPreparedListener(this);    // Prepared status is set
        mediaPlayer.setOnCompletionListener(this);  // Current song has finished playing
        mediaPlayer.setOnErrorListener(this);       // MediaPlayer playback error

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

    public void addToSongQueue(ArrayDeque<Song> songs){
        songQueue.addAll(songs);
    }

    private Song getPreviousSong(){
        if(currentSong.getIsCurrentSongFromQueue()){
            // current song is from queue so add it back to queue
            songQueue.addFirst(currentSong);
        } else {
            // current song is from playset so add it back to playset
            playSet.addFirst(currentSong);
        }

        return previousSongs.removeLast();
    }

    public void stopMusicPlayerService(){
        if (mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;

            Log.w(TAG, "stopMusicPlayerService");
        }
    }

    // change current play set
    private void setPlaySet(LinkedList<Song> songs){
        playSet = songs;
    }

    // get next song from queue
    private Song nextSongFromQueue(){
        return songQueue.removeFirst();
    }

    // get next song from play set
    private Song nextSongFromPlaySet(){
        return playSet.removeFirst();
    }

    // get next song
    private Song getNextSong(){
       Song song;
        // Add "current" song to previous songs
        previousSongs.addLast(currentSong);

        // if queue is empty and play set is empty
        if (songQueue.isEmpty() && !playSet.isEmpty()){
            //get next song from play set
            song = nextSongFromPlaySet();

        // elif songQueue is not empty
        } else if(!songQueue.isEmpty()){
            // get next song from song queue
            song =  nextSongFromQueue();

        // No songs in play set or queue
        } else {
            song =  null;
        }

        Log.d(TAG, "getNextSong: "+ (song != null ? song.getTITLE() : "null"));
        return song;
    }

    // add song to song queue
    private void addSongToQueue(Song song){
        Log.d(TAG, "addSongToQueue");
        songQueue.addLast(song);
    }

    // add songs to song queue
    private void addSongsToQueue(LinkedList<Song> songs){
        Log.d(TAG, "addSongsToQueue");
        songQueue.addAll(songs);
    }

    private void moveSongInQueue(int currentIndex, int targetIndex){
        // get move song from current index
        Song song = songQueue.remove(currentIndex);
        Log.d(TAG, "moveSongInQueue: #" +(currentIndex)+ " (song =" + song.getTITLE() +") to #" + targetIndex);
        // place song at target index
        songQueue.add(targetIndex, song);
    }

    public void playNextSong(){
        mediaPlayer.reset();

        Song thisSong = getNextSong();

        Uri songUri = ContentUris.withAppendedId()// TODO unfuck literally everything


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
        musicServiceState = MusicServiceState.Playing;

        // TODO broadcast completed


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

    // TODO my own: Internal flags for the function Below
    private boolean pauseForFocusLoss = false;
    private boolean lowerVolumeForFocusLoss = false;

    @Override // TODO MY OWN
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange){

            // Gained audio focus after unknown non-focus time
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.w(TAG, "AUDIOFOCUS_GAIN");

                if (mediaPlayer == null)
                    initMusicPlayer();

                if (pauseForFocusLoss) {
                    pauseForFocusLoss = false;
                    resumePlayer();
                }

                if (lowerVolumeForFocusLoss) {
                    lowerVolumeForFocusLoss = false;
                    mediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;

            // Completely lost the audio focus
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.w(TAG, "Completely lost the audio focus");
                // no point in waiting
                stopMusicPlayer();
                break;

            // Temp lost audio focus
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.w(TAG, "Temp lost audio focus");

                if (! isPaused()) {
                    pausePlayer();
                    pauseForFocusLoss = true;
                }
                break;

            // Lower volume for notification
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.w(TAG, "Lower volume for notification");
                // set volume to 10% (often lowest on phones)
                mediaPlayer.setVolume(0.1f, 0.1f);
                lowerVolumeForFocusLoss = true;
                break;
        }
    }
}

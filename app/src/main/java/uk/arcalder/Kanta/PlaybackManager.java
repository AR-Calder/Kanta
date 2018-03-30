package uk.arcalder.Kanta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Zynch on 29/03/2018.
 */

class PlaybackManager implements Playback.callback{

    private static final String TAG = PlaybackManager.class.getSimpleName();

    public static final float VOLUME_DUCK               = 0.1f;     // when a notification comes in drop to 10%
    public static final float VOLUME_DEFAULT            = 1.0f;     // default volume %
    private static final int AUDIO_NO_FOCUS_NO_DUCK     = 0;        // Lost focus and can't duck (E.g. a call comes in) - Default state
    private static final int AUDIO_NO_FOCUS_CAN_DUCK    = 1;        // Lost focus but can duck (notification comes in)
    private static final int AUDIO_FOCUSED              = 2;        // We have focus

    private Context context;
    private int managerPlaybackState;
    private final AudioManager audiomanager;
    private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private MediaSession.Callback playbackStateCallback;
    private PlaybackServiceCallback serviceCallback;
    private MusicProvider musicProvider;

    volatile int currentSeekPosition;
    volatile String mediaDescriptionID;

    private MediaPlayer mediaPlayer;

    // intent filter for headphones unplugged
    private final IntentFilter headphonesUnpluggedBroadcastFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private boolean isHeadphoneRecieverRegistered = false;
    private final BroadcastReceiver headphonesUnpluggedBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
                Log.w(TAG, "headsetPlugBroadcastReceiver: ACTION_AUDIO_BECOMING_NOISY");

                // If music is playing while headset is unplugged, pause it
                if (isPlaying()){
                    // Tell musicplayerservice to pause
                    Intent pausePlayerIntent = new Intent(context, MusicService.class);
                    pausePlayerIntent.setAction(MusicService.ACTION_CMD);
                    pausePlayerIntent.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                    context.startService(pausePlayerIntent);
                }
            }
        }
    };

    // Constructor
    public PlaybackManager(Context c, MusicProvider mp) {
        this.context = c;
        this.musicProvider = mp;
        this.audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.managerPlaybackState = PlaybackStateCompat.STATE_NONE;
    }

    // Register headphone detecting receiver
    private void headphoneRecieverRegister() {
        if (!isHeadphoneRecieverRegistered) {
            context.registerReceiver(headphonesUnpluggedBroadcastReceiver, headphonesUnpluggedBroadcastFilter);
            isHeadphoneRecieverRegistered = true;
        }
    }

    // Unregister headphone detecting receiver
    private void headphoneRecieverUnregister() {
        if (isHeadphoneRecieverRegistered) {
            context.unregisterReceiver(headphonesUnpluggedBroadcastReceiver);
            isHeadphoneRecieverRegistered = false;
        }
    }

    @Override
    public void onAudioFocusChange(int focusState) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }



}

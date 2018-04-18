package uk.arcalder.Kanta;

import android.Manifest;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;

    private int mCurrentState;

    private static MediaProvider mMusicProvider = new MediaProvider();

    public ArrayList<Song> songs;


    private MediaBrowserCompat mMediaBrowserCompat;
    private MediaControllerCompat mMediaControllerCompat;

    // Toolbar title
    TextView titleText;
    private String titleOrSearch = "TITLE";

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                Log.d(TAG, "onConnected called");
                mMediaControllerCompat = new MediaControllerCompat(getApplicationContext(), mMediaBrowserCompat.getSessionToken());
                mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);
                MediaControllerCompat.setMediaController(MainActivity.this, mMediaControllerCompat);
                //MediaControllerCompat.getMediaController(MainActivity.this).getTransportControls().playFromMediaId(songs.get(1).getAUDIO_ID(), null);
            } catch( RemoteException e ) {
                Log.wtf(TAG, e);
            }
        }
    };

    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                return;
            }

            switch( state.getState() ) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    mCurrentState = STATE_PLAYING;
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    mCurrentState = STATE_PAUSED;
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate Called");

        // Create MediaBrowserServiceCompat
//        mMediaBrowserCompat = new MediaBrowserCompat(this,
//                new ComponentName(this, MusicPlayerService.class),
//                mMediaBrowserCompatConnectionCallback,
//                null); // optional Bundle

        mMediaBrowserCompat = new MediaBrowserCompat(this, new ComponentName(this, MusicPlayerService.class),
                mMediaBrowserCompatConnectionCallback, getIntent().getExtras());


        Log.d(TAG, "onCreate: connectionCallback supposedly set");

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE); // Once again shite documentation

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.

        } else {
            // Permission has already been granted
            new ScanSongs().execute();
        }

        // Setup bottom navigation view, disable shift mode (which looks crap) and add listener
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigationBarBottom);
        BottomNavigationViewHelper.disableShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(this);

        // Load default fragments
        loadToolbarFragment(new TitlebarFragment(), "HOME");
        loadListFragment(new AlbumListFragment(), "ALBUM");
        loadMiniPlayerFragment(new MiniPlayerFragment());

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart Called");
        mMediaBrowserCompat.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume Called");
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop Called");
        // (see "stay in sync with the MediaSession")
        if (MediaControllerCompat.getMediaController(MainActivity.this) != null) {
            MediaControllerCompat.getMediaController(MainActivity.this).unregisterCallback(mMediaControllerCompatCallback);
        }
        mMediaBrowserCompat.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    new ScanSongs().execute();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), (CharSequence)"PERMISSION DENIED", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    // Mini Player Fragments
    private void loadMiniPlayerFragment(Fragment mp_frag) {
        if (mp_frag != null) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_player, mp_frag)
                    .commit();
        }
    }

    // List fragments
    private boolean loadListFragment(ListFragment list_frag, String TAG) {
        // If Fragment doesn't exist
        if (list_frag != null && null == getSupportFragmentManager().findFragmentByTag(TAG)) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_main, list_frag, TAG)
                    .commit();
            return true;
        } else {
            // Fragment exists
            //Toast.makeText(getApplicationContext(), TAG, Toast.LENGTH_SHORT).show();
        }
        return false;

    }

    // Toolbar / search bar fragments
    private boolean loadToolbarFragment(Fragment toolbar_frag, String TAG) {
        Bundle bundle = new Bundle();
        bundle.putString("TITLE", TAG);
        toolbar_frag.setArguments(bundle);
        // IF Fragment already exists
        if (toolbar_frag != null && null == getSupportFragmentManager().findFragmentByTag(TAG)) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_toolbar, toolbar_frag, TAG)
                    .commit();
            return true;
        } else if (TAG.equals("SEARCH")) {
            // Fragment exists
            //TODO load searchEditText fragment, Set focus to search editText, open keyboard (and do the same onClick of same item)
            Toast.makeText(getApplicationContext(), "load searchEditText fragment, Set focus to search editText, open keyboard", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        Fragment title_fragment = null;
        ListFragment list_fragment = null;

        String TITLE_TAG = null;
        String LIST_TAG = null;

        boolean RESULT = false;

        // TODO replace with actual functionality
        // TODO come up with better way to switch toolbar type
        switch (item.getItemId()) {
            case R.id.navigation_home:
                // Home
                TITLE_TAG = "HOME";
                list_fragment = new SongListFragment();
                title_fragment = new TitlebarFragment();
                break;
            case R.id.navigation_dashboard:
                // Browse
                TITLE_TAG = "BROWSE";
                list_fragment = new AlbumListFragment();
                title_fragment = new TitlebarFragment();
                break;
            case R.id.navigation_search:
                // Search
                TITLE_TAG = "SEARCH";
                list_fragment = new SongListFragment();
                title_fragment = new SearchFragment();
                try {
                    Log.d(TAG, String.valueOf(songs.get(0).getFilePath()));
                    Log.d(TAG, String.valueOf(songs.get(0).getId()));
                    mMediaControllerCompat.getTransportControls();// .playFromMediaId(String.valueOf(songs.get(0).getFilePath()), null);
                } catch (Exception e){
                    Log.wtf(TAG, e);
                }
                break;
            case R.id.navigation_notifications:
                // QUEUE
                TITLE_TAG = "QUEUE";
                list_fragment = new AlbumListFragment();
                title_fragment = new TitlebarFragment();
                break;
            case R.id.navigation_library:
                // LIBRARY
                TITLE_TAG = "LIBRARY";
                list_fragment = new AlbumListFragment();
                title_fragment = new TitlebarFragment();
                break;
        }
        if (null != title_fragment) {
            RESULT = loadToolbarFragment(title_fragment, TITLE_TAG);
        }
        if (null != list_fragment) {
            RESULT = loadListFragment(list_fragment, "ALBUM");
        }

        return RESULT;
    }

    @Override
    public void onClick(View view) {

    }

    class ScanSongs extends AsyncTask<String, Integer, String> {

        /**
         * The action we'll do in the background.
         */
        @Override
        protected String doInBackground(String... params) {

            try {
                // Will scan all songs on the device
                MainActivity.mMusicProvider.scanSongs(getApplicationContext(), "external");
                songs = MainActivity.mMusicProvider.getSongs();
                return "SCAN_OK";
            }
            catch (Exception e) {
                Log.e("Couldn't execute background task", e.toString());
                e.printStackTrace();
                return "SCAN_FAIL";
            }
        }

        /**
         * Called once the background processing is done.
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
        }
    }
}

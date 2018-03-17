package uk.arcalder.Kanta;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    // Toolbar title
    TextView titleText;
    private String titleOrSearch = "TITLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Setup bottom navigation view, disable shift mode (which looks crap) and add listener
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigationBarBottom);
        BottomNavigationViewHelper.disableShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(this);

        // Load default fragments
        loadToolbarFragment(new TitlebarFragment(), "HOME");
        loadListFragment(new AlbumListFragment(), "ALBUM");
        loadMiniPlayerFragment(new MiniPlayerFragment());
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
}

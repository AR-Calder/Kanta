package uk.arcalder.Kanta;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * Created by Zynch on 22/04/2018.
 */

//https://developer.android.com/guide/topics/resources/runtime-changes.html#HandlingTheChange
//Retaining an Object During a Configuration Change
public class VolatileStorageFragment extends Fragment {

    // Object to be retained
    private MusicLibrary mMusicLibrary;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void saveList(){
        mMusicLibrary = MusicLibrary.getInstance();
    }

    public MusicLibrary getList(){
        return mMusicLibrary;
    }
}

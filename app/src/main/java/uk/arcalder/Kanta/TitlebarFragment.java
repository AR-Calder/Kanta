package uk.arcalder.Kanta;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


/**
 * Created by Zynch on 14/03/2018.
 */

public class TitlebarFragment extends Fragment{
    View view;

    private static final String TAG = TitlebarFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        view = inflater.inflate(R.layout.fragment_titlebar, null);

        String title_string = null;
        String home_String = null;

        Bundle args = getArguments();
        try {
            title_string = args.getString("TITLE");
        } catch (Exception e){

        }
        if (null == title_string || "".equals(title_string)) {
            title_string = "HOME";
        }

        TextView title_tv = view.findViewById(R.id.toolbar_title);
        title_tv.setText(title_string);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

}

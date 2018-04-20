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

/**
 * Created by Zynch on 14/03/2018.
 */

public class MiniPlayerFragment extends Fragment implements View.OnClickListener {
    View view;
    public static final String TAG = MiniPlayerFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        view = inflater.inflate(R.layout.fragment_mini_player, null);
        ImageButton settingsButton = (ImageButton) view.findViewById(R.id.btn_expand_player);
        settingsButton.setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_expand_player:
                Intent i = new Intent(this.getActivity(), BigPlayerActivity.class);
                startActivity(i);
                break;
        }
    }
}

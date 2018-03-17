package uk.arcalder.Kanta;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


/**
 * Created by Zynch on 14/03/2018.
 */

public class TitlebarFragment extends Fragment implements View.OnClickListener {
    View view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_titlebar, null);
        ImageButton settingsButton = (ImageButton) view.findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(this);

        String title_string = getArguments().getString("TITLE");
        if (title_string == null) {
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_settings:
                Intent i = new Intent(this.getActivity(), SettingsActivity.class);
                startActivity(i);
                break;
        }
    }
}

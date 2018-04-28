package uk.arcalder.Kanta;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

public class PermissionFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = PermissionFragment.class.getSimpleName();

    Button getPermissionsButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       View view =  inflater.inflate(R.layout.fragment_permissions, null);

       getPermissionsButton = (Button) view.findViewById(R.id.button_get_permissions);
       getPermissionsButton.setOnClickListener(this);


       return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick");
        switch (view.getId()) {
            case R.id.button_get_permissions:
                getPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 123);
                break;

        }
    }

    // --------------------------Handle Permissions "Elegantly"-------------------------------------

    public void getPermission(String Permission, int requestCode){

        Log.i(TAG, "getPermission: Check if have permissions: " + "READ_EXTERNAL_STORAGE");
        // Request permissions
        if (ContextCompat.checkSelfPermission(getActivity(), Permission)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "getPermission: Permission not granted");
            // Permission is not granted so set false for now
            MusicLibrary.getInstance().setHasPermission(false);

            // but request the permission
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Permission},
                    requestCode); // Once again shite documentation

            // The callback method gets the result of the request.
            Log.i(TAG, "getPermission: Requesting Permission");

        } else {
            // Permission has already been granted
            Toast.makeText(getActivity(), "PERMISSION GRANTED", Toast.LENGTH_SHORT).show();
            MusicLibrary.getInstance().setHasPermission(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            // --------------------------HAS PERMISSIONS------------------------------

            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            MusicLibrary.getInstance().setHasPermission(true);
            Toast.makeText(getActivity(), "PERMISSION GRANTED", Toast.LENGTH_SHORT).show();

        } else {

            // --------------------------NO PERMISSIONS------------------------------

            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            MusicLibrary.getInstance().setHasPermission(false);
            Toast.makeText(getActivity(), "PERMISSION DENIED", Toast.LENGTH_SHORT).show();

        }


        // other 'case' lines to check for other
        // permissions this app might request.

    }
}

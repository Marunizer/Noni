package menu.noni.android.noni.model3D.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.xw.repo.BubbleSeekBar;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import menu.noni.android.noni.R;
import menu.noni.android.noni.model3D.util.LocationHelper;

/**
 * Created by marunizer on 2/8/2018.
 *
 * Purpose of this class is to serve as a mini settings menu where user can change search preferences
 *
 * Feature to include: Have the option to not use zipcode, and instead use phone location, will change flow to first decide that, then depending on the option, show text for zip
 *
 */

public class LocationDialogFragment extends DialogFragment {

    static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    FusedLocationProviderClient mFusedLocationClient;
    //Stores the Location of user, involving specific coordinates and such
    Location mLastLocation;

    EditText newZip;
    BubbleSeekBar seekRadius;
    Context context;
    RadioButton locationRadio;
    RadioButton zipcodeRadio;

    public interface NoticeDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) getActivity();
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString()
                    + " must implement NoticeDialogListener");
        }
    }

    /** The system calls this only when creating the layout in a dialog. */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), android.R.style.ThemeOverlay_Material_Dialog_Alert);

        //better but title doesn't appear in right place
        //Theme_Holo_Light_Dialog

        double kmToMiles = .621;

        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        View rootView = inflater.inflate(R.layout.fragment_dialog_location,null);

        seekRadius = rootView.findViewById(R.id.location_seek_bar);
        seekRadius.setProgress((int) (LocationHelper.getRadius()*kmToMiles));

        locationRadio = rootView.findViewById(R.id.radio_location);
        zipcodeRadio = rootView.findViewById(R.id.radio_zipcode);

        newZip = rootView.findViewById(R.id.newAddress);
        newZip.setText(LocationHelper.getZipcode());

        if (LocationHelper.isLocationMethod())
        {
            locationRadio.toggle();
            newZip.setVisibility(View.GONE);
        }
        else
        {
            zipcodeRadio.toggle();
            newZip.setVisibility(View.GONE);
        }

        locationRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newZip.setVisibility(View.INVISIBLE);
                checkLocationSettings();
            }
        });
        zipcodeRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newZip.setVisibility(View.VISIBLE);
            }
        });

        // Set the dialog title
        builder.setTitle("Settings")
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                // Set the action buttons
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        submitButton();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        LocationDialogFragment.this.getDialog().cancel();
                    }
                });

        builder.setView(rootView);

        return builder.create();
    }

    private void checkLocationSettings()
    {
        //Create instance of Client
        if (mFusedLocationClient == null) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                System.out.println("Lets do stuff!");
                getMyLocation();
            }
            else
                getMyLocation();
        }
        else
            getMyLocation();
    }

    public void getMyLocation()
    {
        //If Permissions are not granted, ask for permission
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            return;
        }

        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        mFusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations, this can be null.
                if (location != null)
                    mLastLocation = location;
                //Location could not be accessed
                else //Failed using Location, use zipcode
                    abortUsingLccation();

            }

        }).addOnFailureListener(getActivity(), new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //FAILED TO GET LOCATION, USE ZIPCODE BUTTON INSTEAD
                abortUsingLccation();
            }
        });

    }

    void sendLocation() {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
            LocationHelper.setLocationMethod(true);
            LocationHelper.setLocation(mLastLocation);
            LocationHelper.setLongitude((float)mLastLocation.getLongitude());
            LocationHelper.setLatitude((float)mLastLocation.getLatitude());
            LocationHelper.setAddress(addresses.get(0).getAddressLine(0));
            LocationHelper.setStreetName(addresses.get(0).getThoroughfare());
            LocationHelper.setState(addresses.get(0).getAdminArea());
            LocationHelper.setCity(addresses.get(0).getLocality());
            LocationHelper.setZipcode(addresses.get(0).getPostalCode());
        } catch (IOException e) {
            sendLocation();
            e.printStackTrace();
        }
    }


    public void submitButton()
    {
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                if (zipcodeRadio.isActivated())
                {
                    try{
                        //if we have a new zip code -> change current location AND change shared pref
                        if (!Objects.equals(newZip.getText().toString(), "")) {
                            LocationHelper.setZipcodeAndAll(newZip.getText().toString(), context);

                            SharedPreferences.Editor editor = context.getSharedPreferences("ZIP_PREF", Context.MODE_PRIVATE).edit();
                            editor.putString("zipCode", newZip.getText().toString());
                            editor.apply();
                        }
                        //if we have a new radius
                        if (!Objects.equals(String.valueOf(seekRadius.getProgress()), "")) {
                            LocationHelper.setRadius((seekRadius.getProgress()));
                        }
                        mListener.onDialogPositiveClick(LocationDialogFragment.this);
                        dismiss();
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //Assuming Permission allows the use of location
                else if (locationRadio.isActivated())//use Location GPS
                {
                    sendLocation();
                }
            }
        };
        thread.start();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
            {
                if (grantResults.length > 1)
                {
                    boolean locationPermisssion = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    //Permission is granted, move on
                    if (locationPermisssion)
                        getMyLocation();
                    else
                    {
                        //permission not granted use zip
                        abortUsingLccation();
                    }

                }
                else
                {
                    System.out.println("We shouldn't have come to this line, fix grantresults.length");
                    abortUsingLccation();
                }
            }
        }
    }

    private void abortUsingLccation()
    {
        zipcodeRadio.toggle();
        Toast.makeText(getContext(),"Location Permission are not granted",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }
}

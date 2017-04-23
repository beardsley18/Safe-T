package csce462.safe_t;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.FileInputStream;
import java.util.ArrayList;

import static csce462.safe_t.R.id.time;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0 ;
    private static final int REQUEST_CODE_PERMISSION = 2;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    SupportMapFragment mapFragment;
    GPSTracker gps;
    Button contacts;
    Button startPanic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        contacts = (Button) findViewById(R.id.button_edit_contacts);
        startPanic = (Button) findViewById(R.id.button_send_loc);

        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission)
                    != MockPackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{mPermission},
                        REQUEST_CODE_PERMISSION);

                // If any permission above not allowed by user, this condition will execute every time, else your else part will work
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Called when the user touches the Edit Contacts button */
    public void editContacts(View view) {
        Intent intent = new Intent(this, EditContacts.class);
        startActivity(intent);
    }

    /** Called when the user touches the Send Location button */
    public void panic(View view){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {

            }
            else {
                //Log.d(TAG, "request permissions");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
        else{
            send_GPS();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        //Log.d(TAG, "function two");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    send_GPS();
                }
                else {
                    //Log.d(TAG, "message failed");
                    Toast.makeText(getApplicationContext(), "SMS failed, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    private void send_GPS(){
        //input emergency contact numbers
        String inStr = "";
        try
        {
            FileInputStream fis = openFileInput(EditContacts.FILENAME);
            int numBytes = fis.available();
            byte[] input = new byte[numBytes];
            fis.read(input);
            inStr = new String(input);
            fis.close();
        }
        catch (Exception e){
            //Log.d(TAG, "Please don't crash input.");
            e.printStackTrace();
        }
        if(inStr.isEmpty()){
            Toast.makeText(getApplicationContext(), "No contacts set", Toast.LENGTH_LONG).show();
        }
        else {
            //parse numbers
            ArrayList<String> contactNumbers = new ArrayList<String>();
            int counter = 1;
            for (String field : inStr.split("\n")) {
                if (counter == 2) {
                    contactNumbers.add(field);
                }
                ++counter;
                if (counter == 4) {
                    counter = 1;
                }
            }

            gps = new GPSTracker(MainActivity.this);
            // check if GPS enabled
            if (gps.canGetLocation()) {
                double latitude = gps.getLatitude();
                double longitude = gps.getLongitude();
                SmsManager smsManager = SmsManager.getDefault();
                for (String phoneNumber : contactNumbers) {
                    smsManager.sendTextMessage(phoneNumber, null, "SOS\nMy location is - \nLat: " + latitude + "\nLong: " + longitude, null, null);
                }
                Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
            } else {
                // can't get location
                // GPS or Network is not enabled
                // Ask user to enable GPS/network in settings
                Toast.makeText(getApplicationContext(), "No GPS location", Toast.LENGTH_LONG).show();
                gps.showSettingsAlert();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng tamu = new LatLng(30.6189336, -96.3386431);
        googleMap.addMarker(new MarkerOptions().position(tamu).title("Texas A&M"));
        //googleMap.moveCamera(CameraUpdateFactory.newLatLng(tamu));
    }
}

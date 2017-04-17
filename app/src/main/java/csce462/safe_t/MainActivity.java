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
import android.widget.Toast;

import static csce462.safe_t.R.id.time;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0 ;
    private static final int REQUEST_CODE_PERMISSION = 2;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        gps = new GPSTracker(MainActivity.this);
        // check if GPS enabled
        if(gps.canGetLocation()) {
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            //Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("8324742045", null, "SOS\nMy location is - \nLat: " + latitude + "\nLong: " + longitude, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
        }
        else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            Toast.makeText(getApplicationContext(), "No GPS location", Toast.LENGTH_LONG).show();
            gps.showSettingsAlert();
        }
    }
}

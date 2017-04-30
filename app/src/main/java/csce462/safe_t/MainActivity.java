package csce462.safe_t;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0 ;
    private static final int REQUEST_CODE_PERMISSION = 2;
    String gpsPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    String smsPermission = Manifest.permission.SEND_SMS;
    String bluetoothPermission = Manifest.permission.BLUETOOTH;
    SupportMapFragment mapFragment;
    GPSTracker gps;
    Button contacts;
    Button startPanic;
    ArrayList<EmergencyContact> contactsArrayList;

    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;
    BluetoothChatService mChatService;

    Toolbar myToolbar;
    TextView pairedListText;
    ListView pairedList;
    LinearLayout containerLayout;
    PopupWindow popUpWindow;
    LinearLayout.LayoutParams layoutParams;
    LinearLayout mainLayout;
    ArrayList<String> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        //ActionBar ab = getSupportActionBar();

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        gps = new GPSTracker(MainActivity.this);

        contacts = (Button) findViewById(R.id.button_edit_contacts);
        startPanic = (Button) findViewById(R.id.button_send_loc);

        try {
            if (ActivityCompat.checkSelfPermission(this, gpsPermission)
                    != MockPackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{gpsPermission},
                        REQUEST_CODE_PERMISSION);
            }
            /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                //if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {

                //}
                //else {
                    //Log.d(TAG, "request permissions");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
                //}
            }*/
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        String inStr = "";
        try
        {
            FileInputStream fis = openFileInput(EditContacts.FILENAME);
            int numBytes = fis.available();
            byte[] input = new byte[numBytes];
            fis.read(input);
            inStr = new String(input);
            //Log.d(TAG, inStr);
            fis.close();
        }
        catch (Exception e){
            //Log.d(TAG, "Please don't crash input.");
            e.printStackTrace();
        }

        //parse list into EmergencyContacts
        contactsArrayList = new ArrayList<EmergencyContact>();
        int counter = 1;
        String name = "";
        String phone = "";
        String email = "";
        for(String field: inStr.split("\n")) {
            switch (counter) {
                case 1:
                    name = field;
                    ++counter;
                    break;
                case 2:
                    phone = field;
                    ++counter;
                    break;
                case 3:
                    email = field;
                    EmergencyContact parseContact = new EmergencyContact(name, phone, email);
                    contactsArrayList.add(parseContact);
                    counter = 1;
                    break;
            }
        }

        containerLayout = new LinearLayout(this);
        mainLayout = new LinearLayout(this);
        popUpWindow = new PopupWindow(this);
        pairedListText = new TextView(this);
        pairedList = new ListView(this);
        pairedList.setOnItemClickListener(mDeviceClickListener);
        mChatService = new BluetoothChatService(getApplicationContext(), mHandler);

        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pairedListText.setText("Paired Devices");
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        containerLayout.addView(pairedListText, layoutParams);
        containerLayout.addView(pairedList, layoutParams);
        containerLayout.setBackgroundColor(Color.WHITE);
        popUpWindow.setContentView(containerLayout);

        list = new ArrayList<String>();
        BA = BluetoothAdapter.getDefaultAdapter();
    }

    //When returning from EditContacts, resend the email list in case of changes
    @Override
    protected void onRestart(){
        super.onRestart();
        sendContactEmails();
    }

    //Add Bluetooth button to action bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return true;
    }

    //When Bluetooth button clicked, start Bluetooth connection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bluetooth:
                if (!BA.isEnabled()) {
                    Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnOn, 0);
                }
                else{
                    continueBluetooth();
                }
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    //Runs when item in Paired Devices popup list is selected
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            BluetoothDevice device = BA.getRemoteDevice(address);
            //Log.d(TAG, "device address: " + address);
            popUpWindow.dismiss();
            mChatService.connect(device, true);
        }
    };

    //Called when "Turn on Bluetooth?" popup finishes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 0) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                continueBluetooth();
            }
        }
    }

    //Finish Bluetooth connection
    private void continueBluetooth(){
        pairedDevices = BA.getBondedDevices();
        final ArrayAdapter adapter = new  ArrayAdapter(this,android.R.layout.simple_list_item_1);
        for (BluetoothDevice device : pairedDevices) {
            adapter.add(device.getName() + "\n" + device.getAddress());
        }

        pairedList.setAdapter(adapter);
        popUpWindow.showAtLocation(mainLayout, Gravity.TOP, 10, 10);
    }

    //The Handler that gets information back from the BluetoothChatService
    private final android.os.Handler mHandler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothChatService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
                            sendContactEmails();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_LONG).show();
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            Toast.makeText(getApplicationContext(), "Unable to connect", Toast.LENGTH_LONG).show();
                            break;
                    }
                    break;
                case BluetoothChatService.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case BluetoothChatService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //Log.d(TAG, "Received " + readMessage);
                    if(readMessage.equals("PANIC")){panic(findViewById(android.R.id.content));}
                    break;
            }
        }
    };

    /** Assuming Bluetooth connection is working, send email list to device */
    public void sendContactEmails(){
        if(!contactsArrayList.isEmpty()) {
            String toSend = "";
            for (EmergencyContact contact : contactsArrayList) {
                toSend += contact.getEmail() + ',';
                //String email = contact.getEmail();
                //mChatService.write(email.getBytes());
                //Log.d(TAG, "Sent " + email);
            }
            //remove final comma
            toSend = toSend.substring(0, toSend.length()-1);
            //mChatService.write("done".getBytes());
            //send emails
            Log.d(TAG, "Sending " + toSend);
            mChatService.write(toSend.getBytes());
        }
    }

    /** Called when the user touches the Edit Contacts button */
    public void editContacts(View view) {
        Intent intent = new Intent(this, EditContacts.class);
        startActivity(intent);
    }

    /** Called when the user touches the Send Location button */
    public void panic(View view){
        /*if(contactsArrayList.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No contacts set", Toast.LENGTH_LONG).show();
        }
        else{
            gps = new GPSTracker(MainActivity.this);
            // check if GPS enabled
            if (gps.canGetLocation()) {
                double latitude = gps.getLatitude();
                double longitude = gps.getLongitude();
                SmsManager smsManager = SmsManager.getDefault();
                //for (String phoneNumber : contactNumbers) {
                //if(smsManager.)
                for(EmergencyContact contact: contactsArrayList){
                    String phoneNumber = contact.getPhone();
                    smsManager.sendTextMessage(phoneNumber, null, "SOS\nMy location is - \nLat: " + latitude + "\nLong: " + longitude, null, null);
                }
                Toast.makeText(getApplicationContext(), "SMS sent", Toast.LENGTH_LONG).show();
            } else {
                // can't get location
                // GPS or Network is not enabled
                // Ask user to enable GPS/network in settings
                Toast.makeText(getApplicationContext(), "No GPS location", Toast.LENGTH_LONG).show();
                gps.showSettingsAlert();
            }
        }*/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            //if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {

            //}
            //else {
            //Log.d(TAG, "request permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
            //}
        }
        else{
            sendGPS();
        }
    }

    /** Called when SMS Permissions popup finishes */
    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        //Log.d(TAG, "function two");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendGPS();
                }
                else {
                    //Log.d(TAG, "message failed");
                    Toast.makeText(getApplicationContext(), "SMS failed", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    /** Now that checks are handled, actually send GPS location over SMS to emergency contacts */
    private void sendGPS(){
        if(contactsArrayList.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No contacts set", Toast.LENGTH_LONG).show();
        }
        else{
            // check if GPS enabled
            if (gps.canGetLocation()) {
                double latitude = gps.getLatitude();
                double longitude = gps.getLongitude();
                SmsManager smsManager = SmsManager.getDefault();
                //for (String phoneNumber : contactNumbers) {
                for(EmergencyContact contact: contactsArrayList){
                    String phoneNumber = contact.getPhone();
                    smsManager.sendTextMessage(phoneNumber, null, "SOS\nMy location is - \nLat: " + latitude + "\nLong: " + longitude, null, null);
                }
                Toast.makeText(getApplicationContext(), "SMS sent", Toast.LENGTH_LONG).show();
            } else {
                // can't get location
                // GPS or Network is not enabled
                // Ask user to enable GPS/network in settings
                Toast.makeText(getApplicationContext(), "No GPS location", Toast.LENGTH_LONG).show();
                gps.showSettingsAlert();
            }
        }
    }

    /** Called when Google Map is created, use to edit map */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //LatLng tamu = new LatLng(30.6189336, -96.3386431);
        //googleMap.addMarker(new MarkerOptions().position(tamu).title("Texas A&M"));
        //googleMap.moveCamera(CameraUpdateFactory.newLatLng(tamu));
        if (gps.canGetLocation()) {
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            LatLng current = new LatLng(latitude, longitude);
            googleMap.addMarker(new MarkerOptions().position(current).title("Current Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(current));
        }
        LatLng hrbb = new LatLng(30.6189336, -96.3386431);
        LatLng sbisa = new LatLng(30.617211,-96.3459747);
        LatLng rudder = new LatLng(30.6128287,-96.3424501);

        LatLng annex = new LatLng(30.6164067,-96.3406688);
        LatLng msc = new LatLng(30.612222,-96.3417949);
        LatLng northgate = new LatLng(30.6180923,-96.3460079);
        googleMap.addCircle(new CircleOptions().center(hrbb).fillColor(Color.BLUE).radius(25).strokeColor(Color.BLUE).strokeWidth(1));
        googleMap.addCircle(new CircleOptions().center(sbisa).fillColor(Color.BLUE).radius(25).strokeColor(Color.BLUE).strokeWidth(1));
        googleMap.addCircle(new CircleOptions().center(rudder).fillColor(Color.BLUE).radius(25).strokeColor(Color.BLUE).strokeWidth(1));
        googleMap.addCircle(new CircleOptions().center(annex).fillColor(Color.RED).radius(25).strokeColor(Color.RED).strokeWidth(1));
        googleMap.addCircle(new CircleOptions().center(msc).fillColor(Color.RED).radius(25).strokeColor(Color.RED).strokeWidth(1));
        googleMap.addCircle(new CircleOptions().center(northgate).fillColor(Color.RED).radius(25).strokeColor(Color.RED).strokeWidth(1));
    }
}

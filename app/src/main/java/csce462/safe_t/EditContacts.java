package csce462.safe_t;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class EditContacts extends AppCompatActivity {

    private static final String TAG = "EditContactsActivity";
    public static final String FILENAME = "contacts";
    private ArrayList<EmergencyContact> contactsArrayList;
    ListView contactsList;
    Button addContacts;
    Button mainMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contacts);

        contactsList = (ListView) findViewById(R.id.list_view_contacts);
        addContacts = (Button) findViewById(R.id.button_add_contacts);
        mainMenu = (Button) findViewById(R.id.button_main_menu);
    }

    @Override
    protected void onStart(){
        super.onStart();

        String inStr = "";
        try
        {
            FileInputStream fis = openFileInput(FILENAME);
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
        for(String field: inStr.split("\n")){
            switch (counter){
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

        ArrayAdapter<EmergencyContact> adapter = new ArrayAdapter<EmergencyContact>(this, android.R.layout.simple_list_item_1, contactsArrayList);
        contactsList.setAdapter(adapter);
        contactsList.setOnItemClickListener(mMessageClickedHandler);
    }

    private AdapterView.OnItemClickListener mMessageClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            contactsArrayList.remove(position);
            Toast.makeText(getApplicationContext(), "Contact deleted", Toast.LENGTH_LONG).show();
            try
            {
                FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
                for(EmergencyContact contact: contactsArrayList){
                    fos.write(contact.toString().getBytes());
                    fos.write("\n".getBytes());
                }
                fos.close();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
            catch (Exception e){
                //Log.d(TAG, "Please don't crash output.");
                e.printStackTrace();
            }
        }
    };

    public void addNewContact(View view){
        Intent intent = new Intent(this, AddNewContact.class);
        startActivity(intent);
    }

    public void mainMenu(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}

package csce462.safe_t;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.FileOutputStream;

public class AddNewContact extends AppCompatActivity {

    private static final String TAG = "AddNewContactActivity";
    TextView addNewEmergencyContact;
    EditText submitName;
    EditText submitPhone;
    EditText submitEmail;
    Button submitForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_contact);
        addNewEmergencyContact = (TextView) findViewById(R.id.text_view_add_new_contact);
        submitName = (EditText) findViewById(R.id.edit_text_name);
        submitPhone = (EditText) findViewById(R.id.edit_text_phone);
        submitEmail = (EditText) findViewById(R.id.edit_text_email);
        submitForm = (Button) findViewById(R.id.button_submit_new_contact);
    }

    public void submitContact(View view){
        String name = submitName.getText().toString();
        String phone = submitPhone.getText().toString();
        String email = submitEmail.getText().toString();
        String contact = name+'\n'+phone+'\n'+email+'\n';
        try
        {
            FileOutputStream fos = openFileOutput(EditContacts.FILENAME, Context.MODE_APPEND);
            fos.write(contact.getBytes());
            fos.close();
        }
        catch (Exception e){
            //Log.d(TAG, "Please don't crash output.");
            e.printStackTrace();
        }

        Intent intent = new Intent(this, EditContacts.class);
        startActivity(intent);
    }
}

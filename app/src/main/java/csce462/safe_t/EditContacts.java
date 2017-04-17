package csce462.safe_t;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class EditContacts extends AppCompatActivity {

    private static final String TAG = "EditContactsActivity";
    public String filename = "contacts";
    TextView sampleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contacts);
        try
        {
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write("Can you see this?".getBytes());
            fos.close();
        }
        catch (Exception e){
            Log.d(TAG, "Please don't crash output.");
            e.printStackTrace();
        }
        try
        {
            FileInputStream fis = openFileInput(filename);
            int numBytes = fis.available();
            byte[] input = new byte[numBytes];
            fis.read(input);
            String inStr = new String(input);
            sampleText = (TextView)findViewById(R.id.sampleTextView);
            sampleText.setText(inStr);
            Log.d(TAG, inStr);
            fis.close();
        }
        catch (Exception e){
            Log.d(TAG, "Please don't crash input.");
            e.printStackTrace();
        }
    }

    public void addNewContact(View view){

    }


}

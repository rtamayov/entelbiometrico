package eikon.biometrico.entel.pe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUException;

import eikon.biometrico.entel.pe.R;

public class MainActivity extends Activity {

    private Button btnScan;
    private Button btnRead;
    private TextView tvReceivedMsg;

    private static final String TAG = "MainCompat";
    private int EIKON_STEP = 1;


    private String eikon_serial_number = "";
    private String eikon_device_name = "";

    private static final int FIRST_CHECK = 1;
    private static final int SECOND_SCAN = 2;

    private ReaderCollection readers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnScan = (Button) findViewById(R.id.btnScan);
        btnRead = (Button) findViewById(R.id.btnRead);

        tvReceivedMsg = (TextView) findViewById(R.id.tvReceivedMsg);



        btnScan.setEnabled(true);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*if(ContextCompat.checkSelfPermission(Main.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    initializeScan();
                }else{
                    requerirPermisos();
                }*/



                // initiliaze dp sdk
                try {
                    Context applContext = getApplicationContext();
                    readers = Globals.getInstance().getReaders(applContext);
                } catch (UareUException e) {
                    onBackPressed();
                }

                int nSize = readers.size();
                if (nSize > 1)
                {
                    //retornar error
                }
                else
                {
                    /*
                    Intent i = new Intent();
                    i.putExtra("serial_number", (nSize == 0 ? "" : readers.get(0).GetDescription().serial_number));
                    i.putExtra("device_name", (nSize == 0 ? "" : readers.get(0).GetDescription().name));
                    setResult(Activity.RESULT_OK, i);
                    finish();
                    */

                    eikon_serial_number = readers.get(0).GetDescription().serial_number;
                    eikon_device_name = readers.get(0).GetDescription().name;

                    showToast("serial_number: " +  readers.get(0).GetDescription().serial_number + " - device_name: " +  readers.get(0).GetDescription().name );
                    btnRead.setEnabled(true);
                }

            }
        });


        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                initializeEikon();

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            //log error proceso
        }
    }


    private void initializeEikon(){
        Log.i(TAG,"initailizeEikon:init step: "+ EIKON_STEP);
        if(EIKON_STEP == 0){
            Intent intent = new Intent(MainActivity.this, EikonActivity.class);
            intent.putExtra("EIKON_STEP",EIKON_STEP);
            //startActivityForResult(intent, FIRST_CHECK);
        } else if (EIKON_STEP == 1 && eikon_serial_number.length() > 0){
            Intent intent = new Intent(MainActivity.this, EikonActivity.class);
            intent.putExtra("EIKON_STEP",EIKON_STEP);
            intent.putExtra("eikon_serial_number",eikon_serial_number);
            startActivityForResult(intent, SECOND_SCAN);
        } else {
            //log error proceso
        }
    }



    private void showToast(String message){
        Toast.makeText(this.getApplicationContext(), message,
                Toast.LENGTH_SHORT).show();
    }
}

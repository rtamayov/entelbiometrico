package com.outsystemsenterprise.enteluat.PEMayorista;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zy.lib.morpho.ui.BioCapture;
import com.zy.lib.morpho.ui.IBioCapture;
import com.zy.lib.morpho.ui.ZyRequest;
import com.zy.lib.morpho.ui.ZyResponse;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main extends Activity {

    private static final String TAG = "MainCompat";
    private static final String DIRECTORY_NAME = "/entelWSQ/";
    private static final String OUTPUT_FILE_EXTENSION = ".txt";
    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";
    private static final int FIRST_CHECK = 1;
    private static final int SECOND_SCAN = 2;
    private int scan_step = 0;
    private int EIKON_STEP = 0;
    private int STORAGE_PERMISSION_CODE = 1;
    private String eikon_serial_number = "";
    private String eikon_device_name = "";
    private String scan_request_code = "";
    private Reader eikon_reader;
    private Button btnScan;
    private TextView tvReceivedMsg;
    private String directorioWSQ_PATH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnScan = (Button) findViewById(R.id.btnScan);

        tvReceivedMsg = (TextView) findViewById(R.id.tvReceivedMsg);

        btnScan.setEnabled(false);

        if(Build.VERSION.SDK_INT > 22){
            requerirPermisos();
        }else{
            btnScan.setEnabled(true);
        }

        tvReceivedMsg.setText(validarExtra());

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(Main.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    initializeScan();
                }else{
                    requerirPermisos();
                }
            }
        });
    }

    private void initializeScan() {
        switch (scan_step){
            case 0:
                initializeMorpho();
                break;
            case 1:
                initializeEikon();
        }
    }

    private String getTimestampString() {
        Log.i(TAG,"getTimeStampString:init");
        Long timestamp = System.currentTimeMillis() / 1000;
        return timestamp.toString();
    }

    private void crearDirectorios() {
        Log.i(TAG,"crearDirectorio:init");
        File directorio = null;
        if(Environment.getExternalStorageState() == null){
            //se crea directorio en almacenamiento local
            directorio = new File(Environment.getDataDirectory() + DIRECTORY_NAME + "//");
        } else if (Environment.getExternalStorageState() != null){
            //se crea directorio en almacenamiento externo si esta disponible
            directorio = new File(Environment.getExternalStorageDirectory() + DIRECTORY_NAME + "//");
        }
        if(!directorio.exists()){
            Log.i(TAG,"crearDirectorio:exists: "+ directorio.exists());
            boolean checkDir = directorio.mkdir();
            Log.i(TAG,"crearDirectorio:dircreated?: "+ checkDir);
        }
        directorioWSQ_PATH = directorio.getAbsolutePath();
        Log.i(TAG,"crearDirectorio:end ret: " + directorioWSQ_PATH);
    }

    private String validarExtra(){
        Bundle bundle =  getIntent().getExtras();
        if(bundle != null){
            if(bundle.getString("ScanRequest") != null && !bundle.getString("ScanRequest").isEmpty()){
                scan_request_code = bundle.getString("ScanRequest");
            }else{
                scan_request_code = getTimestampString();
            }
        }else{
            scan_request_code = getTimestampString();
        }
        return scan_request_code;
    }

    private void requerirPermisos(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            new AlertDialog.Builder(this)
                    .setTitle("Se necesitan permisos")
                    .setMessage("Se requiere guardar la huella escaneada en el almacenamiento externo.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(Main.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(Main.this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == STORAGE_PERMISSION_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                btnScan.setEnabled(true);
                crearDirectorios();
            } else {
                showToast("No se otorgaron Permisos");
            }
        }
    }

    private void initializeMorpho() {
        IBioCapture iBioCapture = new BioCapture(this, new IBioCapture.ICallback() {
            @Override
            public void onStart() {}

            @Override
            public void onComplete() {}

            @Override
            public void onSuccess(ZyResponse zyResponse)  {
                showToast("Huella Obtenida\nCreando Archivo");
                guardarWSQ(formatToBase64(zyResponse.wsq));
            }

            @Override
            public void onError(ZyResponse obj) {
                if(obj.deError.contains("19005")){
                    scan_step = 1;
                    initializeEikon();
                }
                showToast(obj.deError);
            }
        });
        ZyRequest zyRequest = new ZyRequest();
        iBioCapture.capturar(zyRequest);
    }

    private void guardarWSQ(String response){
        Log.i(TAG,"guardarWSQ:init");
        try{
            //File file = new File(directorioWSQ_PATH, tvReceivedMsg.getText() + OUTPUT_FILE_EXTENSION);
            File file = new File( Environment.getExternalStorageDirectory() +"/Android/data/com.outsystemscloud.roberttamayo.AbrirNativa/files/A/B/C/D/E/MyDocs/", tvReceivedMsg.getText() + OUTPUT_FILE_EXTENSION);


            Log.i(TAG,"guardarWSQ:file2save: " + file.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(response.getBytes());
            fos.close();
        } catch (IOException e){
            e.printStackTrace();
            Log.e(TAG,"guardarWSQ:IOExcp res: "+e);
        } catch (Exception e){
            e.printStackTrace();
            Log.e(TAG,"guardarWSQ:AllExcp res: "+e);
        }
        showToast("Archivo Creado");
        Log.i(TAG,"guardarWSQ:end");

    }

    private String formatToBase64(byte[] wsq){
        if (wsq == null) {
            return null;
        }
        int idx;
        byte[] data = new byte[(wsq.length + 2)];
        System.arraycopy(wsq, 0, data, 0, wsq.length);
        byte[] dest = new byte[((data.length / 3) * 4)];
        int sidx = 0;
        int didx = 0;
        while (sidx < wsq.length) {
            dest[didx] = (byte) ((data[sidx] >>> 2) & 63);
            dest[didx + 1] = (byte) (((data[sidx + 1] >>> 4) & 15) | ((data[sidx] << 4) & 63));
            dest[didx + 2] = (byte) (((data[sidx + 2] >>> 6) & 3) | ((data[sidx + 1] << 2) & 63));
            dest[didx + 3] = (byte) (data[sidx + 2] & 63);
            sidx += 3;
            didx += 4;
        }
        for (idx = 0; idx < dest.length; idx++) {
            if (dest[idx] < (byte) 26) {
                dest[idx] = (byte) (dest[idx] + 65);
            } else if (dest[idx] < (byte) 52) {
                dest[idx] = (byte) ((dest[idx] + 97) - 26);
            } else if (dest[idx] < (byte) 62) {
                dest[idx] = (byte) ((dest[idx] + 48) - 52);
            } else if (dest[idx] < (byte) 63) {
                dest[idx] = (byte) 43;
            } else {
                dest[idx] = (byte) 47;
            }
        }
        for (idx = dest.length - 1; idx > (wsq.length * 4) / 3; idx--) {
            dest[idx] = (byte) 61;
        }
        return new String(dest);
    }

    private void showToast(String message){
        Toast.makeText(this.getApplicationContext(), message,
                Toast.LENGTH_SHORT).show();
    }

    private void initializeEikon(){
        Log.i(TAG,"initailizeEikon:init step: "+ EIKON_STEP);
        if(EIKON_STEP == 0){
            Intent intent = new Intent(Main.this, EikonActivity.class);
            intent.putExtra("EIKON_STEP",EIKON_STEP);
            startActivityForResult(intent, FIRST_CHECK);
        } else if (EIKON_STEP == 1 && eikon_serial_number.length() > 0){
            Intent intent = new Intent(Main.this, EikonActivity.class);
            intent.putExtra("EIKON_STEP",EIKON_STEP);
            intent.putExtra("eikon_serial_number",eikon_serial_number);
            startActivityForResult(intent, SECOND_SCAN);
        } else {
            //log error proceso
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null){
            //log error proceso
        }
        switch(requestCode){
            case FIRST_CHECK:
                eikon_serial_number = (String) data.getExtras().get("eikon_serial_number");
                eikon_device_name = (String) data.getExtras().get("eikon_device_name");

                if((eikon_device_name != null && !eikon_device_name.isEmpty()) &&
                        (eikon_serial_number != null && !eikon_serial_number.isEmpty())){
                    try{
                        eikon_reader = Globals.getInstance().getReader(eikon_serial_number, this.getApplicationContext());
                        if(eikon_reader.GetDescription().technology == Reader.Technology.HW_TECHNOLOGY_CAPACITIVE){
                            PendingIntent mPermissionIntent;
                            mPermissionIntent = PendingIntent.getBroadcast(this.getApplicationContext(),0,new Intent(ACTION_USB_PERMISSION),0);
                            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                            this.getApplicationContext().registerReceiver(mUsbReceiver,filter);

                            if(DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(this.getApplicationContext(),mPermissionIntent,eikon_device_name)){
                                CheckDevice();
                                EIKON_STEP = 1;
                                btnScan.setText("Start Scan");
                            }
                        }
                    } catch (UareUException e){

                    } catch (DPFPDDUsbException e){

                    }
                }

                break;
            case SECOND_SCAN:
                Log.i(TAG, "ON RESULT OF SCAN");

                if(resultCode == Activity.RESULT_OK){
                    Log.i(TAG, "RESULT OF SCAN STATUS - OK");

                    String imageBase64 = data.getStringExtra("imageBase64");
                    String wsqBase64 = data.getStringExtra("wsqBase64");

                    Log.i(TAG,"imageBase64: "+imageBase64);
                    Log.i(TAG,"wsqBase64: "+wsqBase64);



                    try {
                        JSONObject json = new JSONObject();
                        json.put("imageBase64", imageBase64);
                        json.put("wsqBase64", wsqBase64);

                        String message = json.toString();

                        Log.i(TAG,"message: "+message);
                        guardarWSQ(wsqBase64);
                        scan_step = 0;
                        EIKON_STEP = 0;
                        btnScan.setText("Scan");
                    }
                    catch (Exception e) {
                        e.printStackTrace();

                        Log.i(TAG, "RESULT OF SCAN STATUS - FAILED: "+ e);
                    }


                }else{
                    Log.i(TAG, "RESULT OF SCAN STATUS - FAILED from capture method");

                }

                break;
        }
    }

    protected void CheckDevice() {
        try {
            eikon_reader.Open(Reader.Priority.EXCLUSIVE);

            if(eikon_reader.GetCapabilities().can_capture){ Log.i(TAG, "Can capture "); }
            else{ Log.i(TAG, "Cannot capture ");}

            if(eikon_reader.GetCapabilities().can_stream){ Log.i(TAG, "Can stream "); }
            else{ Log.i(TAG, "Cannot stream "); }

            eikon_reader.Close();
            Globals.getInstance().enableCamera();
        } catch (UareUException e1) {
            //Error de procedimiento
        }

    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                            CheckDevice();
                        }
                    }
                    else {
                        //setButtonsEnabled(false);
                    }
                }
            }
        }
    };
}

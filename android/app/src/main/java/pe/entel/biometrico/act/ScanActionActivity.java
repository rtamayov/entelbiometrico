package pe.entel.biometrico.act;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.zy.lib.morpho.ui.BioCapture;
import com.zy.lib.morpho.ui.IBioCapture;
import com.zy.lib.morpho.ui.ZyRequest;
import com.zy.lib.morpho.ui.ZyResponse;

import java.io.File;
import java.io.FileOutputStream;

import biometrico.entel.pe.R;
import pe.entel.biometrico.util.Globals;
import pe.entel.biometrico.util.Utils;

public class ScanActionActivity extends Activity {

    private String instructions;

    private static String TAG = "ScanActionActivity";

    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";

    private String m_deviceName = "";
    private int eikon_step = 0;


    Reader m_reader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_scan);



        //verifyStoragePermissions(this);



        Intent intent = getIntent();
        instructions = intent.getStringExtra("file");

        instructions = instructions.substring(2, instructions.length()-2);

        Log.v("XXX", instructions);


        initializeMorpho();

    }



    private void initializeMorpho() {
        IBioCapture iBioCapture = new BioCapture(this, new IBioCapture.ICallback() {
            @Override
            public void onStart() {}

            @Override
            public void onComplete() {}

            @Override
            public void onSuccess(ZyResponse zyResponse)  {

                SaveWSQ(formatToBase64(zyResponse.wsq));

                Toast.makeText(getApplicationContext(), "Archivo Guardado",
                        Toast.LENGTH_SHORT).show();

                finish();

            }

            @Override
            public void onError(ZyResponse obj) {

                if(obj.deError.contains("19005")){
                    Toast.makeText(getApplicationContext(), "Using digital", Toast.LENGTH_SHORT).show();
                    initializeEikon();
                }else{

                    Toast.makeText(getApplicationContext(), obj.deError,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }


            }
        });
        ZyRequest zyRequest = new ZyRequest();
        iBioCapture.capturar(zyRequest);

    }


    private void initializeEikon() {

        if(eikon_step == 0){
            Intent i = new Intent(ScanActionActivity.this, GetReaderActivity.class);
            i.putExtra("device_name", m_deviceName);
            i.putExtra("parent_activity", "ScanActionActivity");
            startActivityForResult(i, eikon_step);
        } else if (eikon_step == 1){
            Intent i = new Intent(ScanActionActivity.this, CaptureFingerprintActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, eikon_step);
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null)
        {
            Toast.makeText(getApplicationContext(), "No data on activity result",
                    Toast.LENGTH_SHORT).show();
            //displayReaderNotFound();
            return;
        }

        Globals.ClearLastBitmap();
        m_deviceName = (String) data.getExtras().get("device_name");



        switch (requestCode)
        {
            case 0:

                if((m_deviceName != null) && !m_deviceName.isEmpty())
                {
                    //m_selectedDevice.setText("Device: " + m_deviceName);

                    try {
                        Context applContext = getApplicationContext();
                        m_reader = Globals.getInstance().getReader(m_deviceName, applContext);

                        {
                            PendingIntent mPermissionIntent;
                            mPermissionIntent = PendingIntent.getBroadcast(applContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                            applContext.registerReceiver(mUsbReceiver, filter);

                            if(DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(applContext, mPermissionIntent, m_deviceName))
                            {
                                //CheckDevice();
                                eikon_step = 1;
                                initializeEikon();
                            }
                        }
                    } catch (UareUException e1)
                    {
                        Toast.makeText(getApplicationContext(), e1.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                    catch (DPFPDDUsbException e)
                    {
                        Toast.makeText(getApplicationContext(), e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                } else
                {
                    Toast.makeText(getApplicationContext(), "device name is empty",
                            Toast.LENGTH_SHORT).show();
                }

                break;
            case 1:
                Log.i(TAG, "ON RESULT OF SCAN");

                if(resultCode == Activity.RESULT_OK){
                    Log.i(TAG, "RESULT OF SCAN STATUS - OK");

                    String wsqBase64 = data.getStringExtra("wsqBase64");

                    Log.i(TAG,"wsqBase64: "+wsqBase64);
                    if(wsqBase64 == null){

                        //CaptureFingerprinyActivity put WsqBase64 in extras, but ScanActionActivity got it null
                        Log.i(TAG, "WsqBase64 String is null");
                        Toast.makeText(getApplicationContext(), "Ocurrió un error con la imagen, intente nuevamente",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }else{

                        try {
                            SaveWSQ(wsqBase64);
                            eikon_step = 0;

                            Toast.makeText(getApplicationContext(), "Archivo Guardado",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            Log.i(TAG, "Error saving file: "+ e);
                            Toast.makeText(getApplicationContext(), "Ocurrió un error guardando el archivo",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }


                }else{
                    Log.i(TAG, "RESULT OF SCAN STATUS - FAILED from capture method");
                    Toast.makeText(getApplicationContext(), "La imagen está vacía",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
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

    void SaveWSQ(String wsq){

        try {

            String nombre = instructions
                    + ".txt";

            String strFolder = Environment.getExternalStorageDirectory() + Utils.rutaArchivo();




            File folder = new File(strFolder);
            boolean success = true;
            if (!folder.exists()) {
                //Toast.makeText(MainActivity.this, "Directory Does Not Exist, Create It", Toast.LENGTH_SHORT).show();
                success = folder.mkdir();
            }


            File file = new File(strFolder,
                    nombre);

            file.createNewFile();
            FileOutputStream stream = new FileOutputStream(file);
            try {

                stream.write(wsq.getBytes());
            } finally {
                stream.close();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action))
            {
                synchronized (this)
                {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        if(device != null)
                        {
                            //call method to set up device communication
                            //CheckDevice();
                        }
                    }
                    else
                    {
                        //setButtonsEnabled(false);
                    }
                }
            }
        }
    };
}

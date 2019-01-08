package pe.entel.biometrico.act;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.zy.lib.morpho.ui.BioCapture;
import com.zy.lib.morpho.ui.IBioCapture;
import com.zy.lib.morpho.ui.ZyRequest;
import com.zy.lib.morpho.ui.ZyResponse;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import biometrico.entel.pe.R;
import pe.entel.biometrico.util.Globals;

public class LauncherActivity extends Activity {

    private Button m_morpho;
    private Button m_eikon;
    private static String TAG = "LauncherActivity";
    private static String OUTPUT_FILE_EXTENSION = ".txt";
    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";
    private String file_name = "test";
    private String eikon_serial_number = "";
    private String m_deviceName = "";
    private int eikon_step = 0;
    Reader m_reader;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_launcher);

        m_morpho = (Button) findViewById(R.id.launch_morpho);
        m_eikon = (Button) findViewById(R.id.launch_eikon);

        m_morpho.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                initializeMorpho();
            }
        });

        m_eikon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializeEikon();
            }
        });

    }

    private void initializeEikon(){
        if(eikon_step == 0){
            Intent i = new Intent(LauncherActivity.this, GetReaderActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, eikon_step);
        } else if (eikon_step == 1){
            Intent i = new Intent(LauncherActivity.this, CaptureFingerprintActivity.class);
            i.putExtra("device_name", m_deviceName);
            startActivityForResult(i, eikon_step);
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
                saveWSQ(formatWsqToBase64(zyResponse.wsq));
            }

            @Override
            public void onError(ZyResponse obj) {

                Toast.makeText(getApplicationContext(), obj.deError,
                        Toast.LENGTH_SHORT).show();
                if(obj.deError.contains("19005")){
                    //scan_step = 1;
                    //initializeEikon();
                }
            }
        });
        ZyRequest zyRequest = new ZyRequest();
        iBioCapture.capturar(zyRequest);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (data == null)
        {
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
                                m_eikon.setText("Eikon 1");
                                initializeEikon();
                            }
                        }
                    } catch (UareUException e1)
                    {
                        //displayReaderNotFound();
                    }
                    catch (DPFPDDUsbException e)
                    {
                        //displayReaderNotFound();
                    }

                } else
                {
                    //displayReaderNotFound();
                }

                break;
            case 1:
                Log.i(TAG, "ON RESULT OF SCAN");

                if(resultCode == Activity.RESULT_OK){
                    Log.i(TAG, "RESULT OF SCAN STATUS - OK");

                    String wsqBase64 = data.getStringExtra("wsqBase64");

                    Log.i(TAG,"wsqBase64: "+wsqBase64);

                    try {
                        saveWSQ(wsqBase64);
                        eikon_step = 0;
                        m_eikon.setText("Eikon");
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

    private void saveWSQ(String response){
        Log.i(TAG,"saveWSQ:init");
        try{
            File file = new File( Environment.getExternalStorageDirectory() +"/Android/data/com.outsystemscloud.roberttamayo.AbrirNativa/files/A/B/C/D/E/MyDocs/", file_name + OUTPUT_FILE_EXTENSION);
            Log.i(TAG,"saveWSQ:file2save: " + file.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(response.getBytes());
            fos.close();
        } catch (IOException e){
            e.printStackTrace();
            Log.e(TAG,"saveWSQ:IOExcp res: "+e);
        } catch (Exception e){
            e.printStackTrace();
            Log.e(TAG,"saveWSQ:AllExcp res: "+e);
        }
        Toast.makeText(this.getApplicationContext(), "File Saved",
                Toast.LENGTH_SHORT).show();
        Log.i(TAG,"saveWSQ:end");
    }

    public static String formatWsqToBase64(byte[] wsq){
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
package pe.entel.biowsq;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.Reader.Priority;
import com.digitalpersona.uareu.UareUException;

import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbHost;
import com.digitalpersona.uareu.dpfpddusbhost.DPFPDDUsbException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.os.Bundle;

import android.content.Context;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

public class ScanActionActivity extends Activity {

    private String instructions;

    private final String LOG_TAG = "ScanActionActivity";

    private final int GET_READER_CODE = 1;
    private final int CAPTURE_FINGERPRINT_CODE = 2;


    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";

    private String m_deviceName = "";

    Reader m_reader;


    @Override
    public void onStop()
    {
        super.onStop();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //enable tracing
        System.setProperty("DPTRACE_ON", "1");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_action);

        Utils.verifyStoragePermissions(this);

        //File Instructions
        Intent intent = getIntent();
        instructions = intent.getStringExtra("file");
        instructions = instructions.substring(2, instructions.length()-2);
        Log.v(LOG_TAG, "Route: " +instructions);

        launchGetReader();
    }


    //Activity Launchers
    protected void launchGetReader()
    {
        Intent i = new Intent(ScanActionActivity.this, GetReaderActivity.class);
        i.putExtra("device_name", m_deviceName);
        startActivityForResult(i, GET_READER_CODE);
    }

    protected void launchCaptureFingerprint()
    {
        Intent i = new Intent(ScanActionActivity.this,CaptureFingerprintActivity.class);
        i.putExtra("device_name", m_deviceName);
        i.putExtra("instructions", instructions);
        startActivityForResult(i, CAPTURE_FINGERPRINT_CODE);
    }
    //Activity Launchers



    protected void CheckDevice()
    {
        try
        {
            m_reader.Open(Priority.EXCLUSIVE);
            Reader.Capabilities cap = m_reader.GetCapabilities();
            //can capture
            if(cap.can_capture){
                Log.i("LOG_TAG","Device Can Capture!");
            }
            m_reader.Close();
        }
        catch (UareUException e1)
        {
            displayReaderNotFound();
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (data == null)
        {
            displayReaderNotFound();
            return;
        }

        Globals.ClearLastBitmap();
        m_deviceName = (String) data.getExtras().get("device_name");

        switch (requestCode)
        {
            case GET_READER_CODE:
                Log.i(LOG_TAG,"ON GET READER RESULT");
                if((m_deviceName != null) && !m_deviceName.isEmpty())
                {
                    Log.i("LOG_TAG", "Device: " + m_deviceName);

                    try {
                        Context applContext = getApplicationContext();
                        m_reader = Globals.getInstance().getReader(m_deviceName, applContext);
                        {
                            PendingIntent mPermissionIntent;
                            mPermissionIntent = PendingIntent.getBroadcast(applContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                            applContext.registerReceiver(mUsbReceiver, filter);

                            if(DPFPDDUsbHost.DPFPDDUsbCheckAndRequestPermissions(applContext, mPermissionIntent, m_deviceName)){
                                try{
                                    m_reader.Open(Priority.EXCLUSIVE);
                                    Reader.Capabilities cap = m_reader.GetCapabilities();
                                    if(cap.can_capture){
                                        Log.i("LOG_TAG","Device Can Capture!");
                                        //CHAIN
                                        launchCaptureFingerprint();
                                    }
                                    m_reader.Close();
                                }
                                catch (UareUException e1){
                                    displayReaderNotFound();
                                }
                            }
                        }
                    } catch (UareUException e1){
                        displayReaderNotFound();
                    } catch (DPFPDDUsbException e){
                        displayReaderNotFound();
                    }
                } else{
                    displayReaderNotFound();
                }
                break;
            case CAPTURE_FINGERPRINT_CODE:
                Log.i(LOG_TAG,"ON CAPTURE RESULT");

                if(resultCode == Activity.RESULT_OK){
                    Log.i(LOG_TAG, "CAPTURE RESULT OK");
                    finish();
                }else{
                    Log.i(LOG_TAG, "CAPTURE RESULT CANCELED");
                    finish();
                }
                break;
        }
    }



    private void displayReaderNotFound()
    {
        Toast.makeText(ScanActionActivity.this, "No se detectó el huellero", Toast.LENGTH_SHORT).show();
        finish();
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
                            CheckDevice();
                        }
                    }
                }
            }
        }
    };
}

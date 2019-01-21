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
import android.widget.TextView;
import android.os.Bundle;

import android.content.Context;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;


public class MainActivity extends Activity {

    private final String LOG_TAG = "MainActivity";

    private final int GET_READER_CODE = 1;
    private final int CAPTURE_FINGERPRINT_CODE = 2;


    private static final String ACTION_USB_PERMISSION = "com.digitalpersona.uareu.dpfpddusbhost.USB_PERMISSION";

    private Button m_getReader;
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
        setContentView(R.layout.activity_main);

        m_getReader = (Button) findViewById(R.id.get_reader);

        // register handler for UI elements
        m_getReader.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                launchGetReader();
            }
        });
    }


    //Activity Launchers
    protected void launchGetReader()
    {
        Intent i = new Intent(MainActivity.this, GetReaderActivity.class);
        i.putExtra("device_name", m_deviceName);
        startActivityForResult(i, GET_READER_CODE);
    }

    protected void launchCaptureFingerprint()
    {
        Intent i = new Intent(MainActivity.this,CaptureFingerprintActivity.class);
        i.putExtra("device_name", m_deviceName);
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
                    }
                    catch (DPFPDDUsbException e){
                        displayReaderNotFound();
                    }
                } else{
                    displayReaderNotFound();
                }
                break;
            case CAPTURE_FINGERPRINT_CODE:
                Log.i(LOG_TAG,"ON CAPTURE RESULT");
                break;
        }
    }



    private void displayReaderNotFound()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Reader Not Found");
        alertDialogBuilder.setMessage("Plug in a reader and try again.").setCancelable(false).setPositiveButton("Ok",
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog,int id) {}
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
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

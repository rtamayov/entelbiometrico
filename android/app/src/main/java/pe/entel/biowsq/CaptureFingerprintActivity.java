package pe.entel.biowsq;

import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.Reader.Priority;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.content.Context;

public class CaptureFingerprintActivity extends Activity {

    private final String LOG_TAG = "CaptureFingerprintAct";

    private Button m_back;
    private String m_deviceName = "";

    private Reader m_reader = null;
    private int m_DPI = 0;
    private Bitmap m_bitmap = null;
    private ImageView m_imgView;
    private boolean m_reset = false;
    private Reader.CaptureResult cap_result = null;


    private void initializeActivity()
    {
        m_deviceName = getIntent().getExtras().getString("device_name");

        m_imgView = (ImageView) findViewById(R.id.bitmap_image);
        m_bitmap = Globals.GetLastBitmap();
        if (m_bitmap == null) m_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.black);
        m_imgView.setImageBitmap(m_bitmap);

        m_back = (Button) findViewById(R.id.back);

        m_back.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                onBackPressed ();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_stream);
        initializeActivity();
        // initiliaze dp sdk
        try
        {
            Context applContext = getApplicationContext();
            m_reader = Globals.getInstance().getReader(m_deviceName, applContext);
            m_reader.Open(Priority.EXCLUSIVE);
            m_DPI = Globals.GetFirstDPI(m_reader);
        }
        catch (Exception e)
        {
            Log.w("UareUSampleJava", "error during init of reader");
            m_deviceName = "";
            onBackPressed();
            return;
        }

        // loop capture on a separate thread to avoid freezing the UI
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    m_reset = false;
                    while (!m_reset)
                    {
                        cap_result = m_reader.Capture(Fid.Format.ANSI_381_2004, Globals.DefaultImageProcessing, m_DPI, -1);
                        // an error occurred
                        if (cap_result == null || cap_result.image == null) continue;
                        // save bitmap image locally
                        m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());
                        runOnUiThread(new Runnable()
                        {
                            @Override public void run()
                            {
                                UpdateGUI();
                            }
                        });
                    }
                }
                catch (Exception e)
                {
                    if(!m_reset)
                    {
                        Log.w("UareUSampleJava", "error during capture: " + e.toString());
                        m_deviceName = "";
                        onBackPressed();
                    }
                }
            }
        }).start();
    }

    public void UpdateGUI()
    {
        m_imgView.setImageBitmap(m_bitmap);
        m_imgView.invalidate();


        Log.i(LOG_TAG,"Got NEW Fingerprint Image!");

    }

    @Override
    public void onBackPressed()
    {
        try
        {
            m_reset = true;
            try {m_reader.CancelCapture(); } catch (Exception e) {}
            m_reader.Close();

        }
        catch (Exception e)
        {
            Log.w("UareUSampleJava", "error during reader shutdown");
        }

        Intent i = new Intent();
        i.putExtra("device_name", m_deviceName);
        setResult(Activity.RESULT_OK, i);
        finish();
    }

    // called when orientation has changed to manually destroy and recreate activity
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_capture_stream);
        initializeActivity();
    }
}

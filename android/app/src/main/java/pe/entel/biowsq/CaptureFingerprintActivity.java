package pe.entel.biowsq;

import com.digitalpersona.uareu.Compression;
import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.Reader.Priority;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.dpfj.CompressionImpl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

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

    private String instructions;


    private void initializeActivity()
    {
        m_deviceName = getIntent().getExtras().getString("device_name");
        instructions = getIntent().getExtras().getString("instructions");

        m_imgView = (ImageView) findViewById(R.id.bitmap_image);
        m_bitmap = Globals.GetLastBitmap();
        if (m_bitmap == null) m_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.black);
        m_imgView.setImageBitmap(m_bitmap);

        m_back = (Button) findViewById(R.id.back);

        m_back.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                terminarButton();
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


    }

    // called when orientation has changed to manually destroy and recreate activity
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_capture_stream);
        initializeActivity();
    }



    public void storeFileAndReturn(){

        //WSQ IN B64
        //grab capture result and convert it into wsq as bytearray
        Fid ISOFid = cap_result.image;
        byte[] wsqRawCompress = processImage(ISOFid.getViews()[0].getData(),ISOFid.getViews()[0].getWidth(), ISOFid.getViews()[0].getHeight());
        String wsqBase64 = Utils.formatWsqToBase64(wsqRawCompress);

        if(wsqBase64 != null){

            Intent i = new Intent();
            i.putExtra("m_deviceName",m_deviceName);
            Log.i(LOG_TAG,"wsqBase64: "+wsqBase64);
            //i.putExtra("wsqBase64", wsqBase64);

            try {
                m_reader.Close();
            } catch (UareUException e) {
                e.printStackTrace();
            }


            /****STORE FILE****/

            try {
                if(instructions == "" || instructions.isEmpty() || instructions == null ){
                    Toast.makeText(getApplicationContext(), "No hay ruta para guardar archivo",
                            Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_CANCELED, i);
                    finish();
                }else{
                    SaveWSQ(wsqBase64);
                    Log.i(LOG_TAG,"Saving Image");
                    Toast.makeText(getApplicationContext(), "Archivo Guardado",
                            Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK, i);
                    finish();
                }


            }
            catch (Exception e) {
                e.printStackTrace();
                Log.i(LOG_TAG,"Ocurrió un error guardando el archivo ");
                Toast.makeText(getApplicationContext(), "Ocurrió un error guardando el archivo",
                        Toast.LENGTH_SHORT).show();

                setResult(Activity.RESULT_CANCELED, i);
                finish();
            }

            /****STORE FILE****/



        }else{

            Intent i = new Intent();
            i.putExtra("m_deviceName",m_deviceName);
            Log.i(LOG_TAG,"Ocurrió un error al procesar la huella ");
            setResult(Activity.RESULT_CANCELED, i);
            finish();

        }

    }




    public void terminarButton(){

        try{

            if(cap_result != null){
                if(cap_result.image == null){
                    Log.i(LOG_TAG, "Image is null");
                    onBackPressed ();
                }else{
                    Log.i(LOG_TAG, "Storing File");
                    storeFileAndReturn();
                }
            }else{
                Log.i(LOG_TAG, "Capture result is null");
                Toast.makeText(getApplicationContext(),
                        "No se ha capturado ninguna huella, intenta nuevamente", Toast.LENGTH_SHORT).show();
            }

        }catch (Exception e){

            Toast.makeText(getApplicationContext(),
                    "Try Again", Toast.LENGTH_SHORT).show();

        }

    }






    //SAVE FILE

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

            Toast.makeText(CaptureFingerprintActivity.this, "Error creating file!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();

        }
    }

    public byte[] processImage(byte[] img, int width, int height) {

        Bitmap bmWSQ = null;
        bmWSQ = getBitmapAlpha8FromRaw(img, width, height);

        byte[] arrayT = null;

        Bitmap redimWSQ = overlay(bmWSQ);
        int numOfbytes = redimWSQ.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(numOfbytes);
        redimWSQ.copyPixelsToBuffer(buffer);
        arrayT = buffer.array();

        int v1 = 1;
        for (int i = 0; i < arrayT.length; i++) {
            if (i < 40448) { // 79
                arrayT[i] = (byte) 255;
            } else if (i >= 40448 && i <= 221696) {

                if (v1 < 132) {
                    arrayT[i] = (byte) 255;
                } else if (v1 > 382) {
                    arrayT[i] = (byte) 255;
                }
                if (v1 == 512) {
                    v1 = 0;
                }
                v1++;
            } else if (i > 221696) { // 433
                arrayT[i] = (byte) 255;
            }

        }

        CompressionImpl comp = new CompressionImpl();
        try {
            comp.Start();
            comp.SetWsqBitrate(500, 0);

            byte[] rawCompress = comp.CompressRaw(arrayT, redimWSQ.getWidth(), redimWSQ.getHeight(), 500, 8,
                    Compression.CompressionAlgorithm.COMPRESSION_WSQ_NIST);

            comp.Finish();
            Log.i("Util", "getting WSQ...");
            return rawCompress;

        } catch (UareUException e) {
            Log.e("Util", "UareUException..." + e);
            return null;
        } catch (Exception e) {
            Log.e("Util", "Exception..." + e);
            return null;
        }



    }

    private Bitmap overlay(Bitmap bmp) {
        Bitmap bmOverlay = Bitmap.createBitmap(512, 512, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp, 512 / 2 - bmp.getWidth() / 2, 512 / 2 - bmp.getHeight() / 2, null);
        canvas.save();
        return bmOverlay;
    }

    private Bitmap getBitmapAlpha8FromRaw(byte[] Src, int width, int height) {
        byte [] Bits = new byte[Src.length];
        int i = 0;
        for(i=0;i<Src.length;i++)
        {
            Bits[i] = Src[i];
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));

        return bitmap;
    }

}

package pe.entel.biometrico.act;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.zy.lib.morpho.ui.BioCapture;
import com.zy.lib.morpho.ui.IBioCapture;
import com.zy.lib.morpho.ui.ZyRequest;
import com.zy.lib.morpho.ui.ZyResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import biometrico.entel.pe.R;

public class LauncherActivity extends Activity {

    private Button m_morpho;
    private final String TAG = "LauncherActivity";
    private final String OUTPUT_FILE_EXTENSION = ".txt";
    private String file_name = "test";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_launcher);

        m_morpho = (Button) findViewById(R.id.launch_morpho);

        m_morpho.setOnClickListener(new View.OnClickListener()
        {

            public void onClick(View v)
            {
                initializeMorpho();
            }
        });

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

    private void saveWSQ(String response){
        Log.i(TAG,"saveWSQ:init");
        try{
            //File file = new File(directorioWSQ_PATH, tvReceivedMsg.getText() + OUTPUT_FILE_EXTENSION);
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
}

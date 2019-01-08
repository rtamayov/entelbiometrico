package pe.entel.biometrico;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.zy.lib.morpho.ui.BioCapture;
import com.zy.lib.morpho.ui.IBioCapture;
import com.zy.lib.morpho.ui.ZyRequest;
import com.zy.lib.morpho.ui.ZyResponse;

import biometrico.entel.pe.R;

public class LauncherActivity extends Activity {

    private Button m_morpho;



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

            }

            @Override
            public void onError(ZyResponse obj) {

                Toast.makeText(getApplicationContext(), obj.deError,
                        Toast.LENGTH_SHORT).show();

            }
        });
        ZyRequest zyRequest = new ZyRequest();
        iBioCapture.capturar(zyRequest);

        Toast.makeText(this.getApplicationContext(), "Initialized",
                Toast.LENGTH_SHORT).show();
    }
}

package pe.entel.biometrico;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

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

    }

}

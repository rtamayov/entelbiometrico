package com.outsystemsenterprise.enteluat.PEMayorista;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.digitalpersona.uareu.Compression;
import com.digitalpersona.uareu.Engine;
import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Fmd;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.UareUGlobal;
import com.digitalpersona.uareu.dpfj.CompressionImpl;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class EikonActivity extends AppCompatActivity {

    private ReaderCollection readers;
    private int eikon_step;
    private String eikon_serial_number = "";
    private String eikon_device_name = "";
    private String m_enginError = "";
    private Engine m_engine = null;
    private Reader m_reader = null;
    private Bitmap m_bitmap = null;
    private Fmd m_fmd1 = null;
    private boolean flag_HasCheckedImage = false;

    private ImageView m_imgView;
    private TextView m_title;
    private boolean m_reset = true;
    private CountDownTimer m_timer = null;
    private TextView m_text_conclusion;
    private Reader.CaptureResult cap_result = null;

    private static final String LOG_TAG = "CAPTURE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eikon);

        //eikon_serial_number = getIntent().getExtras().getString("eikon_serial_number");
        //eikon_device_name = getIntent().getExtras().getString("eikon_device_name");

        eikon_step = getIntent().getExtras().getInt("EIKON_STEP");

        if (eikon_step == 0) {
            try {
                readers = Globals.getInstance().getReaders(this.getApplicationContext());
            } catch (UareUException e) {
                //retornar error
            }

            int nSize = readers.size();
            if (nSize > 1) {
                //retornar error
            } else {
                Intent intent = new Intent();
                intent.putExtra("eikon_serial_number", (nSize == 0 ? "" : readers.get(0).GetDescription().serial_number));
                intent.putExtra("eikon_device_name", (nSize == 0 ? "" : readers.get(0).GetDescription().name));
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        } else if (eikon_step == 1) {
            int image_view_id = getApplication().getResources().getIdentifier("bitmap_image", "id", getApplication().getPackageName());
            //m_imgView = (ImageView) findViewById(image_view_id);
            m_imgView = (ImageView) findViewById(R.id.bitmap_image);


            if(m_imgView == null) { Log.i(LOG_TAG,"IMGVIEW null"); }
            else { Log.i(LOG_TAG,"IMGVIEW not null"); }

            m_bitmap = Globals.GetLastBitmap();
            //if (m_bitmap == null) m_bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.black);

            if (m_bitmap != null) m_imgView.setImageBitmap(m_bitmap);

            eikon_serial_number = getIntent().getExtras().getString("eikon_serial_number");
            m_text_conclusion = (TextView) findViewById(getApplication().getResources().getIdentifier("tvTitle", "id", getApplication().getPackageName()));
            //m_text_conclusion = (TextView) findViewById(R.id.tvTitle);


            if (eikon_serial_number != null) {
                capture();
            } else {
                Intent i = new Intent();
                i.putExtra("message", "No serial number provided");
                setResult(Activity.RESULT_CANCELED, i);
                finish();
            }
        }
    }


    private void capture(){
        // initiliaze dp sdk
            try {
                Context applContext = getApplicationContext();
                m_reader = Globals.getInstance().getReader(eikon_serial_number, applContext);
                m_reader.Open(Reader.Priority.EXCLUSIVE);
                m_engine = UareUGlobal.GetEngine();
            } catch (Exception e)
            {
                Log.i(LOG_TAG,"error: "+e);
                Log.i(LOG_TAG, "error during init of reader");
                eikon_serial_number = "";
                eikon_device_name = "";

                Intent i = new Intent();
                i.putExtra("message", "Error on capturing image");
                setResult(Activity.RESULT_CANCELED, i);
                finish();
                return;
            }

            // updates UI continuously -- UI THREAD --
            m_timer = new CountDownTimer(250, 250) {
                public void onTick(long millisUntilFinished) { }
                public void onFinish() {
                    m_imgView.setImageBitmap(m_bitmap);
                    m_imgView.invalidate();

                    //image size has been checked and has no image error
                    if(m_enginError.isEmpty() && flag_HasCheckedImage)
                    {
                        if (cap_result != null)
                        {
                            if (cap_result.quality != null){

                                switch(cap_result.quality){
                                    case FAKE_FINGER:
                                        m_text_conclusion.setText("Fake finger");
                                        m_bitmap = null;
                                        break;
                                    case NO_FINGER:
                                        m_text_conclusion.setText("No finger");
                                        m_bitmap = null;
                                        break;
                                    case CANCELED:
                                        m_text_conclusion.setText("Capture cancelled");
                                        break;
                                    case TIMED_OUT:
                                        m_text_conclusion.setText("Capture timed out");
                                        break;
                                    case FINGER_TOO_LEFT:
                                        m_text_conclusion.setText("Finger too left");
                                        break;
                                    case FINGER_TOO_RIGHT:
                                        m_text_conclusion.setText("Finger too right");
                                        break;
                                    case FINGER_TOO_HIGH:
                                        m_text_conclusion.setText("Finger too high");
                                        break;
                                    case FINGER_TOO_LOW:
                                        m_text_conclusion.setText("Finger too low");
                                        break;
                                    case FINGER_OFF_CENTER:
                                        m_text_conclusion.setText("Finger off center");
                                        break;
                                    case SCAN_SKEWED:
                                        m_text_conclusion.setText("Scan skewed");
                                        break;
                                    case SCAN_TOO_SHORT:
                                        m_text_conclusion.setText("Scan too short");
                                        break;
                                    case SCAN_TOO_LONG:
                                        m_text_conclusion.setText("Scan too long");
                                        break;
                                    case SCAN_TOO_SLOW:
                                        m_text_conclusion.setText("Scan too slow");
                                        break;
                                    case SCAN_TOO_FAST:
                                        m_text_conclusion.setText("Scan too fast");
                                        break;
                                    case SCAN_WRONG_DIRECTION:
                                        m_text_conclusion.setText("Wrong direction");
                                        break;
                                    case READER_DIRTY:
                                        m_text_conclusion.setText("Reader dirty");
                                        break;
                                    case GOOD:
                                        m_text_conclusion.setText("Good Fingerprint");
                                        m_reset = false;
                                        compressImage();
                                        break;
                                    default:
                                        if (cap_result.image == null){
                                            m_text_conclusion.setText("An error occurred");
                                        }
                                }
                            }
                        }

                    }
                    //there's an image error (i.e. size) or image has not been checked
                    else {
                        if(!m_enginError.isEmpty())
                            m_text_conclusion.setText("Engine: " + m_enginError);
                    }

                    if (m_reset)
                        m_timer.start();
                }
            }.start();

            // loop capture on a separate thread to avoid freezing the UI
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    m_reset = true;
                    while (m_reset)
                    {
                        try
                        {
                            cap_result = m_reader.Capture(Fid.Format.ANSI_381_2004, Reader.ImageProcessing.IMG_PROC_DEFAULT, 500, -1);
                            // an error occurred
                            if (cap_result == null || cap_result.image == null) continue;
                            // save bitmap image locally
                            m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());

                        } catch (Exception e)
                        {
                            Log.w(LOG_TAG, "error during capture: " + e.toString());
                            eikon_device_name = "";
                            eikon_serial_number = "";
                            onBackPressed();
                        }

                        //Try to convert create FMD to know if image is correct size
                        try {
                            m_enginError = "";

                            // save bitmap image locally
                            m_bitmap = Globals.GetBitmapFromRaw(cap_result.image.getViews()[0].getImageData(), cap_result.image.getViews()[0].getWidth(), cap_result.image.getViews()[0].getHeight());
                            m_fmd1 = m_engine.CreateFmd(cap_result.image, Fmd.Format.ANSI_378_2004);

                        } catch (Exception e)
                        {
                            m_enginError = e.toString();
                            Log.w(LOG_TAG, "Engine error: " + e.toString());
                        }finally
                        {
                            //Make sure that check of image size has been checked
                            flag_HasCheckedImage = true;
                        }

                    }







                }
            }).start();

        }

        private void compressImage() {
            //IMAGE IN B64
            //Grab bitmap stored in imageview and convert it as a bytearray
            Bitmap bm = ((BitmapDrawable) m_imgView.getDrawable()).getBitmap();
            if(bm == null){
                Log.i(LOG_TAG,"bm null");
            }
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, bStream);
            byte[] imageByteArray = bStream.toByteArray();
            String imageBase64 = encode(imageByteArray);


            //WSQ IN B64
            //grab capture result and convert it into wsq as bytearray
            Fid ISOFid = cap_result.image;
            byte[] wsqRawCompress = processImage(ISOFid.getViews()[0].getData(),ISOFid.getViews()[0].getWidth(), ISOFid.getViews()[0].getHeight());
            String wsqBase64 = encode(wsqRawCompress);

            if(imageBase64 != null && wsqBase64 != null){

                Intent i = new Intent();
                Log.i(LOG_TAG,"imageBase64: "+imageBase64);
                Log.i(LOG_TAG,"wsqBase64: "+wsqBase64);


                i.putExtra("imageBase64", imageBase64);
                i.putExtra("wsqBase64", wsqBase64);

                setResult(Activity.RESULT_OK, i);
                finish();

            }else{

                Intent i = new Intent();
                i.putExtra("message", "Image conversion failed");
                setResult(Activity.RESULT_CANCELED, i);
                finish();

            }
        }





        @Override
        public void onBackPressed() {
            try {
                m_reset = false;
                try { m_reader.CancelCapture(); } catch (Exception e) {}
                m_reader.Close();

                // re-enable camera
                Globals.getInstance().enableCamera();
            } catch (Exception e)
            {
                Log.w(LOG_TAG, "error during reader shutdown");
            }

            Intent i = new Intent();
            i.putExtra("message", "Button back");
            setResult(Activity.RESULT_CANCELED, i);
            finish();
        }






















        private String encode(byte[] d) {
            if (d == null) {
                return null;
            }
            int idx;
            byte[] data = new byte[(d.length + 2)];
            System.arraycopy(d, 0, data, 0, d.length);
            byte[] dest = new byte[((data.length / 3) * 4)];
            int sidx = 0;
            int didx = 0;
            while (sidx < d.length) {
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
            for (idx = dest.length - 1; idx > (d.length * 4) / 3; idx--) {
                dest[idx] = (byte) 61;
            }
            return new String(dest);
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



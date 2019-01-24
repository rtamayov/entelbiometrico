package pe.entel.biowsq;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

import pe.entel.biowsq.BuildConfig;

public class Utils {

    private static final String LOG_TAG = "Utils";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static String formatWsqToBase64(byte[] wsq) {
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

    public static String rutaArchivo() {
        String respuesta = "/Android/data/com.outsystemsenterprise.entel.PEMayorista/files/entelWSQ/";

        if (BuildConfig.FLAVOR.equals("dev")) {
            respuesta = "/Android/data/com.outsystemsenterprise.enteldev.PEMayorista/files/entelWSQ/";
        } else if (BuildConfig.FLAVOR.equals("tst")) {
            respuesta = "/Android/data/com.outsystemsenterprise.enteltst.PEMayorista/files/entelWSQ/";
        }else if (BuildConfig.FLAVOR.equals("pp")) {
            respuesta = "/Android/data/com.outsystemsenterprise.entelpp.PEMayorista/files/entelWSQ/";
        }

        return respuesta;
    }

    private static String rutaArchivoError() {
        String respuesta = "/Android/data/com.outsystemsenterprise.entel.PEMayorista/files/entelError/";

        if (BuildConfig.FLAVOR.equals("dev")) {
            respuesta = "/Android/data/com.outsystemsenterprise.enteldev.PEMayorista/files/entelError/";
        } else if (BuildConfig.FLAVOR.equals("tst")) {
            respuesta = "/Android/data/com.outsystemsenterprise.enteltst.PEMayorista/files/entelError/";
        }else if (BuildConfig.FLAVOR.equals("pp")) {
            respuesta = "/Android/data/com.outsystemsenterprise.entelpp.PEMayorista/files/entelError/";
        }

        return respuesta;
    }

    public static String cualBuild() {
        String respuesta = "NIGUNO";

        if (BuildConfig.FLAVOR.equals("dev")) {
            respuesta = "DESARROLLO";
        } else if (BuildConfig.FLAVOR.equals("tst")) {
            respuesta = "TEST";
        }else if (BuildConfig.FLAVOR.equals("pp")) {
            respuesta = "PRE PRODUCCION";
        }else if(BuildConfig.FLAVOR.equals("prod")) {
            respuesta = "PRODUCCION";
        }

        return respuesta;
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public static void saveErrorInStorage(String ErrorMessage){


        try {

            String nombre = "error.txt";

            String strFolder = Environment.getExternalStorageDirectory() + Utils.rutaArchivoError();



            File folder = new File(strFolder);
            boolean success = true;
            if (!folder.exists()) {
                //Toast.makeText(MainActivity.this, "Directory Does Not Exist, Create It", Toast.LENGTH_SHORT).show();
                success = folder.mkdir();
            }


            File file = new File(strFolder, nombre);

            file.createNewFile();
            FileOutputStream stream = new FileOutputStream(file);
            try {

                stream.write(ErrorMessage.getBytes());
            } finally {
                stream.close();
            }


        } catch (Exception e) {

            e.printStackTrace();
            Log.e(LOG_TAG,e.getMessage());

        }


    }


    public static String fnVersion(Context poContext) {
        String lsVersion = "";
        PackageInfo loPackageInfo = null;
        try {
            loPackageInfo = poContext.getPackageManager().getPackageInfo(
                    poContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.v(LOG_TAG, "Verificar el manifest: " + e.getMessage());
            return "ERROR VERSION";
        }

        lsVersion = loPackageInfo.versionName;

        return lsVersion;
    }
}

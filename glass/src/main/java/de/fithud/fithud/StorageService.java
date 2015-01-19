package de.fithud.fithud;

import android.app.Service;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class StorageService extends Service {
    private static final String TAG = StorageService.class.getSimpleName();
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();
        if (isExternalStorageWritable()) {
            File dir = getDataStorageDir();
            File file = new File(dir, "hans2.txt");
            Log.i(TAG, file.toString());
            try {
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                Writer w = new BufferedWriter(osw);
                w.write("hello hans!");
                w.flush();
                w.close();

                /*
                FileInputStream fis = new FileInputStream(file);
                StringBuffer fileContent = new StringBuffer("");
                byte[] buffer = new byte[1024];
                int n;
                while ((n = fis.read(buffer)) != -1) {
                    fileContent.append(new String(buffer, 0, n));
                }
                Log.i(TAG, fileContent.toString());
                fis.close();
                */

            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.i(TAG, Uri.fromFile(file).toString());
            scanFile(file);
        }
    }
    private void scanFile(File file) {

        MediaScannerConnection.scanFile(StorageService.this,
                new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        Log.i(TAG, "Finished scanning " + path);
                    }
                });
    }
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    public File getDataStorageDir() {
        // Get the directory for the user's public pictures directory.

        File dir = new File(Environment.getExternalStoragePublicDirectory(
                      Environment.DIRECTORY_DOCUMENTS), "fithud");
        //File dir = new File(Environment.getExternalStorageDirectory() + "/DCIM");
        if (!dir.isDirectory()) {
            Log.e(TAG, "creating Directory...");

            if (!dir.mkdirs()) {
                Log.e(TAG, "error creating Directory!");
            }
        }


        return dir;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

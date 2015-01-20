package de.fithud.fithudlib;

import android.app.Service;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.googlecode.jcsv.annotations.MapToColumn;
import com.googlecode.jcsv.annotations.internal.ValueProcessorProvider;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.AnnotationEntryParser;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;
import com.googlecode.jcsv.writer.CSVEntryConverter;
import com.googlecode.jcsv.writer.CSVWriter;
import com.googlecode.jcsv.writer.internal.CSVWriterBuilder;

public class StorageService extends Service implements MessengerClient {
    private static final String TAG = StorageService.class.getSimpleName();
    MessengerConnection conn = new MessengerConnection(this);
    private ArrayList<csvSensorEntry> sensorData = new ArrayList<>();
    private ArrayList<csvSensorEntry> avchievementsData = new ArrayList<>();

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                sensorData.add(new csvSensorEntry("Heartrate", msg.getData().getFloat("value")));
                break;
            case FHSensorManager.Messages.CADENCE_MESSAGE:
                sensorData.add(new csvSensorEntry("Cadence", msg.getData().getFloat("value")));
                break;
            case FHSensorManager.Messages.SPEED_MESSAGE:
                sensorData.add(new csvSensorEntry("Speed", msg.getData().getFloat("value")));
                break;
        }
    }
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();
        //conn.connect(FHSensorManager.class);
        sensorData.add(new csvSensorEntry("Heartrate", 90.0));
        sensorData.add(new csvSensorEntry("Heartrate", 88.0));
        sensorData.add(new csvSensorEntry("Heartrate", 87.0));
    }

    private class csvSensorEntry {
        @MapToColumn(column=0)
        public String sensorType;
        @MapToColumn(column=1)
        public double value;
        @MapToColumn(column=2)
        public String date;
        public csvSensorEntry(String sensorType, double value) {
            this.sensorType = sensorType;
            this.value = value;
            this.date = new Date().toString();
        }
    };

    public class SensorEntryConverter implements CSVEntryConverter<csvSensorEntry> {
        @Override
        public String[] convertEntry(csvSensorEntry e) {
            String[] columns = new String[3];

            columns[0] = e.sensorType;
            columns[1] = Double.toString(e.value);
            columns[2] = e.date;

            return columns;
        }
    }
    private class csvAchievementEntry {
        public String achievement;
        public float value;
        public Date date;
        public csvAchievementEntry(String achievement, float value) {
            this.achievement = achievement;
            this.value = value;
            this.date = new Date();
        }
    };

    private void writeSensorCsv() {
        try {
            File dir = getDataStorageDir();
            File file = new File(dir, "fithud_sensor_data.csv");
            Log.i(TAG, file.toString());

            Writer out = new FileWriter(file);
            CSVWriter<csvSensorEntry> csvWriter =
                    new CSVWriterBuilder<csvSensorEntry>(out).entryConverter(new SensorEntryConverter()).build();
            csvWriter.writeAll(sensorData);
            csvWriter.flush();
            csvWriter.close();
            // TODO
            //scanFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "writing csv success!");

    }
    private void readSensorCsv() {
        try {
            File dir = getDataStorageDir();
            File file = new File(dir, "fithud_sensor_data.csv");
            Log.i(TAG, file.toString());
            Reader csvFile = new InputStreamReader(new FileInputStream(file));

            ValueProcessorProvider vpp = new ValueProcessorProvider();
            CSVReader<csvSensorEntry> sensorReader = new CSVReaderBuilder<csvSensorEntry>(csvFile).entryParser(
                    new AnnotationEntryParser<csvSensorEntry>(csvSensorEntry.class, vpp)).build();
            List<csvSensorEntry> persons = sensorReader.readAll();
        } catch (IOException e) {
            e.printStackTrace();
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

        //File dir = new File(Environment.getExternalStoragePublicDirectory(
        //              Environment.DIRECTORY_DOCUMENTS), "fithud");
        //
        File dir = new File(Environment.getExternalStorageDirectory() + "/DCIM");
        if (!dir.isDirectory()) {
            Log.e(TAG, "creating Directory...");

            if (!dir.mkdirs()) {
                Log.e(TAG, "error creating Directory!");
            }
        }
        return dir;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        if (isExternalStorageWritable()) {
            writeSensorCsv();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

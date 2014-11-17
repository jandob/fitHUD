package de.fithud.fithud;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

/**
 * Created by jandob on 11/17/14.
 */
public class FithudService extends Service {
    public class FithudBinder extends Binder {

        public float getHeartRate() {
            float rate = fithudSensorManager.getHeartrate();
            return rate;
        }
    }
    private FithudSensorManager fithudSensorManager;

    @Override
    public void onCreate() {
        super.onCreate();

        SensorManager sensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        FithudSensorManager fithudSensorManager = new FithudSensorManager();

    }

    private final FithudBinder binder = new FithudBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO start a live card

        //TODO check return value
        return START_STICKY;
    }
    @Override
    public void onDestroy() {

        super.onDestroy();
    }
}

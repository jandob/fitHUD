package de.fithud.fithudlib;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


/**
 * Created by jandob on 11/17/14.
 */
public class MainService extends Service {
    private static final String LIVE_CARD_TAG = "fithud";
    private static final String TAG = MainService.class.getSimpleName();

    public class FithudBinder extends Binder {
        public float getHeartRate() {
            return 0;
        }
    }
    private FHSensorManager fithudSensorManager;

    @Override
    public void onCreate() {
        super.onCreate();

    }

    private final FithudBinder binder = new FithudBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 0;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

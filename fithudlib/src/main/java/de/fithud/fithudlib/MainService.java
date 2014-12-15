package de.fithud.fithudlib;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;


/**
 * Created by jandob on 11/17/14.
 */
public class MainService extends Service implements UpdateListener {
    private static final String TAG = MainService.class.getSimpleName();
    private boolean isRunning = false;
    public boolean isRunning() {return isRunning;}

    @Override
    public void onUpdate(String name, Float value) {
        sendUpdate(name, value);
    }

    public final ArrayList<UpdateListener> mListeners = new ArrayList<UpdateListener>();
    public void registerListener(UpdateListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(UpdateListener listener) {
        mListeners.remove(listener);
    }

    private void sendUpdate(String name, Float value) {
        Log.i(TAG, "sending update");

        for (int i=mListeners.size()-1; i>=0; i--) {
            mListeners.get(i).onUpdate(name, value);
        }
    }

    // generates ticks for debugging
    private long mTick = 0;
    private final Handler mHandler = new Handler();
    private final Runnable mTickRunnable = new Runnable() {

        public void run() {
            mTick++;
            //sendUpdate(mTick);
            mHandler.postDelayed(mTickRunnable, 1000);
        }
    };
    // end generates ticks for debugging

    // Service Binder
    public class FithudBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
        public float getHeartRate() {
            return 0;
        }
    }
    
    private FHSensorManager fhSensorManager;

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
        //debug, to be removed
        mHandler.removeCallbacks(mTickRunnable);
        mHandler.post(mTickRunnable);
        // can be called several times (on every startService() call)
        // but does not need to be synchronized since its called by android
        Log.i(TAG, "onStartCommand()");
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        fhSensorManager = new FHSensorManager(this, getBaseContext());
        fhSensorManager.registerListener(this);
        // Service is restarted if it gets terminated. Intent data passed to the onStartCommand
        // method is null. Used for services which manages their own state and do not depend on
        // the Intent data.
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
    }
}

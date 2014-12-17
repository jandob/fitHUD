package de.fithud.fithudlib;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

/**
 * Created by jandob on 12/16/14.
 */
public class TestService extends MessengerService {
    private static final String TAG = TestService.class.getSimpleName();

    public final class Messages extends MessengerService.Messages {
        public static final int SENSOR_MESSAGE = 1;
        public static final int CLIENT_MESSAGE = 2;
        public static final int CLIENT_RESPONSE_MESSAGE = 3;
    }

    @Override
    void handleMessage(Message msg) {
        switch(msg.what) {
            case Messages.CLIENT_MESSAGE:
                Message response = Message.obtain(null, Messages.CLIENT_RESPONSE_MESSAGE);
                Bundle bundle = new Bundle();
                bundle.putString("answer", "hans");
                response.setData(bundle);
                try {
                    msg.replyTo.send(response);
                } catch (RemoteException e) {}
                break;
        }
    }
    void sendTestMsg(float val) {
        for (int i=mClients.size()-1; i>=0; i--) {
            Message msg = Message.obtain(null, Messages.SENSOR_MESSAGE);
            Bundle bundle = new Bundle();
            bundle.putFloat("HeartRate", val);
            msg.setData(bundle);
            try {
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
    // generates ticks for debugging
    private long mTick = 0;
    private final Handler mHandler = new Handler();
    private final Runnable mTickRunnable = new Runnable() {

        public void run() {
            mTick++;
            sendTestMsg((float)mTick);
            mHandler.postDelayed(mTickRunnable, 1000);
        }
    };

    //private FHSensorManager fhSensorManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
        //debug, to be removed
        mHandler.removeCallbacks(mTickRunnable);
        mHandler.post(mTickRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // can be called several times (on every startService() call)
        // but does not need to be synchronized since its called by android
        Log.i(TAG, "onStartCommand()");
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        //fhSensorManager = new FHSensorManager(this, getBaseContext());
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

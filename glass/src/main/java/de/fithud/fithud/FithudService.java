package de.fithud.fithud;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

/**
 * Created by jandob on 11/17/14.
 */
public class FithudService extends Service {
    private static final String LIVE_CARD_TAG = "fithud";
    private static final String TAG = FithudService.class.getSimpleName();

    public class FithudBinder extends Binder {
        public float getHeartRate() {
            return 0;
        }
    }
    private FithudSensorManager fithudSensorManager;
    private LiveCard mLiveCard;
    private FithudRenderer mRenderer;
    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //TODO
            //startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
        SensorManager sensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        fithudSensorManager = new FithudSensorManager(getBaseContext(),
                sensorManager, locationManager, btManager, btAdapter);

    }

    private final FithudBinder binder = new FithudBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO start a live card
        if (mLiveCard == null) {
            Log.d(TAG, "creating live card");
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mRenderer = new FithudRenderer(this, fithudSensorManager);
            mLiveCard.setDirectRenderingEnabled(true);
            mLiveCard.getSurfaceHolder().addCallback(mRenderer);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, FithudMenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            mLiveCard.attach(this);

            mLiveCard.publish((intent == null) ? PublishMode.SILENT : PublishMode.REVEAL);
            Log.d(TAG, "live card published");
        } else {
            Log.d(TAG, "live card  already published; just showing it!");
            mLiveCard.navigate();
        }

        //TODO check return value
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        if (mLiveCard != null) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }
}

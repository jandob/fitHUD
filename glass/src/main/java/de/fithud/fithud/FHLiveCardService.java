package de.fithud.fithud;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import de.fithud.fithudlib.FHSensorManager;

/**
 * Created by jandob on 11/17/14.
 */
public class FHLiveCardService extends Service {
    private static final String LIVE_CARD_TAG = "fithud";
    private static final String TAG = FHLiveCardService.class.getSimpleName();

    public class FithudBinder extends Binder {
        public float getHeartRate() {
            return 0;
        }
    }
    private LiveCard mLiveCard;
    private FHLiveCardRenderer mRenderer;
    // T: Sensor Manager for test purposes.
    //private FHSensorManager fhSensorManager;

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
        //start a live card
        if (mLiveCard == null) {
            Log.d(TAG, "creating live card");
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mRenderer = new FHLiveCardRenderer(this);
            // T: Sensor Manager for test purposes.
            //fhSensorManager = new FHSensorManager(this, getBaseContext());

            mLiveCard.setDirectRenderingEnabled(true);
            mLiveCard.getSurfaceHolder().addCallback(mRenderer);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, FHMenuActivity.class);
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
        return Service.START_NOT_STICKY;
    }
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        if (mLiveCard != null) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        //fhSensorManager.closeConnections();
        super.onDestroy();
    }
}

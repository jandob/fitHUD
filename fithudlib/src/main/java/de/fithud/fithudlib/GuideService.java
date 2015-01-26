
package de.fithud.fithudlib;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */

public class GuideService extends MessengerService implements MessengerClient {
    //CardBuilder mCard;
    MessengerConnection conn;


    private final String TAG = GuideService.class.getSimpleName();

    private static final int VALUE_LOW = 0;
    private static final int VALUE_OK = 1;
    private static final int VALUE_HIGH = 2;

    private static final int cardio = 0;
    private static final int fatburn = 1;
    private static final int interval = 2;
    private static final int recreation = 3;

    private static int hr_min = 0;
    private static int hr_max = 0;
    private static int speed_low_min = 80;
    private static int speed_low_max = 90;
    private static int speed_high_min = 90;
    private static int speed_high_max = 100;
    private static int cadence_min = 0;
    private static int cadence_max = 0;
    private static int height_min = 0;
    private static int height_max = 0;
    private static long intervalTime = 5000;      // Duration of an interval in mSec

    private static boolean interval_state = false;
    private static long startTime = 0;

    private static int DISABLED = 4;
    private static boolean guide_active = false;
    private static int training_mode = DISABLED;
    private static int challenge_mode = DISABLED;
    private static boolean speech_active = false;

    public final class GuideMessages extends MessengerService.Messages {
        public static final int CARDIO_TRAINING = 21;
        public static final int FATBURN_TRAINING = 22;
        public static final int INTERVAL_TRAINING = 23;
        public static final int HEIGHT_CHALLENGE = 24;
        public static final int CADENCE_CHALLENGE = 25;
        public static final int CALORIES_CHALLENGE = 26;
        public static final int GUIDE_COMMAND = 30;
        public static final int TRAINING_MODE_COMMAND = 31;
        public static final int CHALLENGE_MODE_COMMAND = 32;
        public static final int SPEECH_COMMAND = 33;
    }


    @Override
    public void handleMessage(Message msg) {


        Log.i(TAG, "handling Msg");

        switch (msg.what) {
            case GuideService.GuideMessages.GUIDE_COMMAND:
                updateTrainingMode(training_mode);
                guide_active = msg.getData().getBoolean("guideActive");
                Log.i(TAG,"Guide mode changed." + msg.getData().getBoolean(""));
                break;
/*
            case GuideService.GuideMessages.TRAINING_MODE_COMMAND:
                training_mode = command[1];
                challenge_mode = DISABLED;
                updateTrainingMode(training_mode);
                Log.i(TAG,"Training mode changed." + command[1]);
                String guide_text = "Test guide message";
                sendMsgString(GuideMessages.FATBURN_TRAINING, guide_text);
                break;

            case GuideService.GuideMessages.CHALLENGE_MODE_COMMAND:
                challenge_mode = command[1];
                training_mode = DISABLED;
                updateChallengeMode(challenge_mode);
                Log.i(TAG,"Challenge mode changed." + command[1]);
                break;

            case GuideService.GuideMessages.SPEECH_COMMAND:
                Log.i(TAG,"Speech mode changed." + command[1]);
                if(command[1] == 1) speech_active = true; else speech_active = false;

            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                int heartRate[] = msg.getData().getIntArray("value");

                Log.i(TAG, "Guide got hr message.");

                if(guide_active == 1 && training_mode < 2) {
                    int answerCheck = GuideClass.heartRateCheck(heartRate[0]);
                    if (answerCheck == 0) {
                        Log.i(TAG,"HR" + heartRate[0]);
                        Log.i(TAG,"heartRate is too low, faster you little piggy");
                    } else {
                        Log.i(TAG,"HR" + heartRate[0]);
                        Log.i(TAG, "heartRateCHeck sais: " + answerCheck);
                    }
                }

            case FHSensorManager.Messages.SENSOR_STATUS_MESSAGE:
                Log.i(TAG, "Guide got sensor status message.");

                break;
            case FHSensorManager.Messages.CADENCE_MESSAGE:
                int cadence[] = msg.getData().getIntArray("value");
                break;
            case FHSensorManager.Messages.SPEED_MESSAGE:
                Log.v(TAG, "speed received");
                int speed[] = msg.getData().getIntArray("value");
                break;
            case FHSensorManager.Messages.GUIDE_MESSAGE:*/
        }
    }

    public static void updateTrainingMode(int training_mode){
        // TODO: Set min/max heart rate for cardio training
        if(training_mode == cardio){
            hr_min = 80;
            hr_max = 90;
        }
        // TODO: Set min/max heart rate for fatburn training
        if(training_mode == fatburn){
            hr_min = 90;
            hr_max = 100;
        }

        if(training_mode == interval){
            startTime = System.currentTimeMillis();
        }
    }

    public static void updateChallengeMode(int challenge_mode) {

    }

    public static int heartRateCheck(int heartRate) {
        if(heartRate < hr_min){
            return VALUE_LOW;
        } else if (heartRate < hr_max) {
            return VALUE_OK;
        } else {
            return VALUE_HIGH;
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "on start");
        MessengerConnection conn = new MessengerConnection(this);
        conn.connect(FHSensorManager.class);

    }

    void sendMsgString(int messageType, String content){
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {Message msg = Message.obtain(null, messageType);
                Bundle bundle = new Bundle();
                // bundle.putFloat("value", val);
                // bundle.putFloat("value", val);
                bundle.putString("text", content);
                msg.setData(bundle);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }


    public void sendDataToSensormanager(int[] data) {
        Message msg = Message.obtain(null, 4);
        Bundle bundle = new Bundle();
        // bundle.putFloat("value", val);
        bundle.putIntArray("command", data);
        msg.setData(bundle);
        try {
            conn.send(msg);
        }
        catch (RemoteException e){

        }
    }
}

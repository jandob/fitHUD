/*
package de.fithud.fithudlib;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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

*/
/**
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 *//*

public class GuideService extends Service implements MessengerClient {
    //CardBuilder mCard;
    MessengerConnection conn = new MessengerConnection(this);
    private final String TAG = "GuideService";
    public static float last_speed = 0;
    public static int last_revolutions = 0;
    private static final double wheel_type = 4.4686;

    public boolean speedometer_connected = false;
    public boolean heartrate_connected = false;
    public boolean cadence_connected = false;

    @Override
    public void handleMessage(Message msg) {
        //Log.i(TAG, "handling Msg");
        switch (msg.what) {
            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                int heartRate[] = msg.getData().getIntArray("value");
                Log.i(TAG, "Heartrate " + heartRate[0]);
                break;
            case FHSensorManager.Messages.CADENCE_MESSAGE:
                int[] cadence = msg.getData().getIntArray("value");
                Log.i(TAG, "Cadence_rev: " + cadence[0] + " Cadence_time: " + cadence[1]);
                break;
            case FHSensorManager.Messages.SPEED_MESSAGE:
                int speed_dataset[] = msg.getData().getIntArray("value");
                float time_difference = 0;
                int revolutions_difference = speed_dataset[0] - last_revolutions;
                if (speed_dataset[1] < last_speed) {
                    time_difference = (float) speed_dataset[1] + 65536 - last_speed;
                } else {
                    time_difference = (float) speed_dataset[1] - last_speed;
                }
                last_speed = (float) speed_dataset[1];
                last_revolutions = speed_dataset[0];

                time_difference = time_difference / 1024;
                double speed = 0;
                if (time_difference > 0) {
                    speed = ((revolutions_difference*wheel_type) / time_difference) * 3.6;
                } else {
                    speed = 0;
                }

                Log.i(TAG, "Speed_rev: " + speed_dataset[0] + " speed_time: " + speed_dataset[1]);
                Log.i(TAG, "Speed: " + speed);

                speed_sensor = (int)speed;
                //addSpeedData(speed_sensor);
                break;
            case FHSensorManager.Messages.SENSOR_STATUS_MESSAGE:
                int[] sensor_status = msg.getData().getIntArray("value");
                if(sensor_status[0] == 1){ heartrate_connected = true;}
                else {heartrate_connected =false;}
                if(sensor_status[1] == 1){ speedometer_connected = true;}
                else {heartrate_connected =false;}
                if(sensor_status[2] == 1){ cadence_connected = true;}
                else {heartrate_connected =false;}
                break;
        }
    }

    //private GestureDetector mGestureDetector;
    // Timer variables

    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    // Plotting Variables
   private TextView speedText;
    private TextView heartText;
    private TextView heightText;
    private TextView terrainRoadText;
    private TextView terrainOffroadText;
    private TextView terrainAsphaltText;

    private static final int HISTORY_SIZE = 50;
    private static double sin_counter = 0.0;
    private static boolean plot_speed = false;
    private static boolean plot_heart = false;
    private static boolean plot_height = false;
    private static boolean plot_terrain = false;

    private static boolean init_speed = false;
    private static boolean init_heart = false;
    private static boolean init_height = false;
    private static boolean init_terrain = false;
    private ImageView imageview = null;

    private int speed_sensor = 0;

    @Override
    protected void onCreate(Bundle bundle) {
        //doBindService(FHSensorManager.class);
        conn.connect(FHSensorManager.class);

        Log.i("GuideService", "on start");
        //initSeries();

        super.onCreate(bundle);
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
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

    public void sendWakeup() {
        int[] command = new int[2];
        command[0] = FHSensorManager.Commands.WAKEUP_COMMAND;
        command[1] = 0;
        sendDataToSensormanager(command);
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 500ms
        timer.schedule(timerTask, 5000, 1000); //
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run the plots
                handler.post(new Runnable() {
                    public void run() {
                    }
                });
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        //startTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stoptimertask();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        conn.disconnect();
    }
}
*/

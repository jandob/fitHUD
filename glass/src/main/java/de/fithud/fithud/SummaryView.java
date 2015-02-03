package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
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
import android.content.Intent;
import android.media.AudioManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.GuideService;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;
import de.fithud.fithudlib.MessengerServiceActivity;

import static java.lang.Math.round;


/**
 * Created by JohanV on 04.01.2015.
 */
public class SummaryView extends Activity implements MessengerClient {

    MessengerConnection sensorConn = new MessengerConnection(this);
    MessengerConnection guideConn = new MessengerConnection(this);
    private AudioManager mAudioManager;


    private  TextView liveCardBikeText = null;
    private  TextView liveCardHeartText = null;
    private  TextView liveCardHeightText = null;
    private  TextView liveCardStatus = null;
    private TextView summaryGuideText = null;
    private TextView distanceText = null;
    private TextView timeText = null;
    private TextView caloriesText = null;

    private String TAG = SummaryView.class.getSimpleName();
    int heartRate = 0;
    float speed = (float) 0.0;
    int cadence = 0;
    float distance = (float) 0.0;
    int calories = 10;


    @Override
    public void handleMessage(Message msg) {

        switch (msg.what) {
            case FHSensorManager.Messages.SENSOR_STATUS_MESSAGE:
                Log.i(TAG,"Got msg");
                break;
            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                heartRate = (int) msg.getData().getFloat("value");
                liveCardHeartText.setText(heartRate + " bpm");
                break;
            case FHSensorManager.Messages.CADENCE_MESSAGE:
                cadence = (int) msg.getData().getFloat("value");
                liveCardHeightText.setText(cadence + " U/min");
                break;
            case FHSensorManager.Messages.SPEED_MESSAGE:
                speed = msg.getData().getFloat("value");
                liveCardBikeText.setText(round(speed*100)/100 + " km/h");
                break;
            case FHSensorManager.Messages.DISTANCE_MESSAGE:
                distance = msg.getData().getFloat("value");
                distanceText.setText("distance: " + distance);

            case GuideService.GuideMessages.GUIDE_TEXT:
                Log.d(TAG, "Summary got guide text: " + msg.getData().getString("text"));
                summaryGuideText.setText(msg.getData().getString("text"));
                break;

            case GuideService.GuideMessages.GUIDE_TIME:
                Log.d(TAG, "Workout time: " + msg.getData().getString("text"));
                timeText.setText(msg.getData().getString("text"));
                break;

            case FHSensorManager.Messages.CALORIES_MESSAGE:
                Log.d(TAG, "Calories: " + msg.getData().getFloat("value"));
                calories = (int) msg.getData().getFloat("value");
                caloriesText.setText(calories + " kJ");
                break;
        }
    }


    @Override
    protected void onDestroy() {
        //sendBoolToGuide(GuideService.GuideMessages.SPEECH_COMMAND, false);
        GuideService.summaryBoundToGuide = false;
        sensorConn.disconnect();
        guideConn.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle bundle) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        TAG = SummaryView.class.getSimpleName();

        sensorConn.connect(FHSensorManager.class);
        guideConn.connect(GuideService.class);

        super.onCreate(bundle);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setContentView(R.layout.fithud);
        liveCardBikeText = (TextView) findViewById(R.id.liveCardBikeText);
        liveCardHeartText = (TextView) findViewById(R.id.liveCardHeartText);
        liveCardHeightText = (TextView) findViewById(R.id.liveCardHeightText);
        liveCardStatus = (TextView) findViewById(R.id.liveCardStatus);
        summaryGuideText = (TextView) findViewById(R.id.summaryGuideText);
        distanceText = (TextView) findViewById(R.id.distanceText);
        timeText = (TextView) findViewById(R.id.timeText);
        caloriesText = (TextView) findViewById(R.id.caloriesText);

        int speed = 5;
        int heartrate = heartRate;
        int height = 5;
        liveCardBikeText.setText(speed + " km/h");
        liveCardHeartText.setText(heartrate + " bpm");
        liveCardHeightText.setText(height + " m");
        liveCardStatus.setBackgroundColor(Color.RED);
        GuideService.summaryBoundToGuide = true;
        Log.d(TAG, "Summary on create");
    }

    @Override
    protected void onResume() {
        GuideService.summaryBoundToGuide = true;
        //sendBoolToGuide(GuideService.GuideMessages.SPEECH_COMMAND, true);
        super.onResume();
    }

    @Override
    protected void onPause() {
        //sendBoolToGuide(GuideService.GuideMessages.SPEECH_COMMAND, false);
        GuideService.summaryBoundToGuide = false;
        super.onPause();
    }

    public void sendBoolToGuide(int messageType, boolean guideActive) {
        Message msg = Message.obtain(null, messageType);
        Bundle bundle = new Bundle();
        bundle.putBoolean("summaryBoundToGuide", guideActive);
        //bundle.putIntArray("command", data);
        msg.setData(bundle);
        try {
            guideConn.send(msg);
        }
        catch (RemoteException e){

        }
    }
}
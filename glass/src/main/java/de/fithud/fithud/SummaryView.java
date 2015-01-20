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
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;
import de.fithud.fithudlib.MessengerServiceActivity;


/**
 * Created by JohanV on 04.01.2015.
 */
public class SummaryView extends Activity implements MessengerClient {

    MessengerConnection conn = new MessengerConnection(this);
    private AudioManager mAudioManager;

    private  TextView liveCardBikeText = null;
    private  TextView liveCardHeartText = null;
    private  TextView liveCardHeightText = null;
    private  TextView liveCardStatus = null;
    private String TAG;
    int heartRate[] = new int[] {1};
    int speed[] = new int[] {1};
    int cadence[] = new int[] {1};

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case FHSensorManager.Messages.SENSOR_STATUS_MESSAGE:
            Log.i(TAG,"Got msg");
                break;
            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                heartRate = msg.getData().getIntArray("value");
                liveCardHeartText.setText(heartRate[0] + " bpm");
                break;
            case FHSensorManager.Messages.CADENCE_MESSAGE:
                cadence = msg.getData().getIntArray("value");
                liveCardHeightText.setText(cadence[0] + " U/min");
                break;
            case FHSensorManager.Messages.SPEED_MESSAGE:
                speed = msg.getData().getIntArray("value");
                liveCardBikeText.setText(speed[0] + " km/h");
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        conn.disconnect();
    }

    @Override
    protected void onCreate(Bundle bundle) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        TAG = SummaryView.class.getSimpleName();
        conn.connect(FHSensorManager.class);
        super.onCreate(bundle);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setContentView(R.layout.fithud);
        liveCardBikeText = (TextView) findViewById(R.id.liveCardBikeText);
        liveCardHeartText = (TextView) findViewById(R.id.liveCardHeartText);
        liveCardHeightText = (TextView) findViewById(R.id.liveCardHeightText);
        liveCardStatus = (TextView) findViewById(R.id.liveCardStatus);

        int speed = 5;
        int heartrate = heartRate[0];
        int height = 5;
        liveCardBikeText.setText(speed + " km/h");
        liveCardHeartText.setText(heartrate + " bpm");
        liveCardHeightText.setText(height + " m");
        liveCardStatus.setBackgroundColor(Color.RED);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
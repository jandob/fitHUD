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

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case FHSensorManager.Messages.SENSOR_STATUS_MESSAGE:
            Log.i(TAG,"Got msg");
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
        int heartrate = 5;
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
package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
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
public class SensorMenu extends Activity implements MessengerClient {

    MessengerConnection conn = new MessengerConnection(this);
    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;

    private boolean speedometer_connected = false;
    private boolean heartrate_conected = false;
    private boolean cadence_connected = false;

    private CheckBox speedCheckbox = null;
    private CheckBox heartrateCheckbox = null;
    private CheckBox cadenceCheckbox = null;

    private String TAG;

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "Message");
        switch (msg.what) {
            case FHSensorManager.Messages.SENSOR_STATUS_MESSAGE:
                int[] sensor_status = msg.getData().getIntArray("value");
                if (sensor_status[0] == 1) {
                    heartrate_conected = true;
                } else {
                    heartrate_conected = false;
                }
                if (sensor_status[1] == 1) {
                    speedometer_connected = true;
                } else {
                    speedometer_connected = false;
                }
                if (sensor_status[2] == 1) {
                    cadence_connected = true;
                } else {
                    cadence_connected = false;
                }
                speedCheckbox.setChecked(speedometer_connected);
                heartrateCheckbox.setChecked(heartrate_conected);
                cadenceCheckbox.setChecked(cadence_connected);
                mAdapter.notifyDataSetChanged();
                Log.i(TAG, "Status: " + sensor_status[0] + " " + sensor_status[1] + " " + sensor_status[2]);

                break;
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

    // This function is called with clicking on the second card in GuideSettings
    public void searchSensors() {
        int[] test = new int[2];
        test[0] = 1;
        test[1] = 0;
        sendDataToSensormanager(test);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //doUnbindService();
        conn.disconnect();

    }

    @Override
    protected void onCreate(Bundle bundle) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        TAG = SensorMenu.class.getSimpleName();
        //doBindService(FHSensorManager.class);
        conn.connect(FHSensorManager.class);
        super.onCreate(bundle);
        createCards();

        mCardScrollView = new CardScrollView(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // Handle the TAP event.
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mAudioManager.playSoundEffect(Sounds.TAP);
                switch (mCardScrollView.getSelectedItemPosition()) {
                    case 0:

                        break;

                    case 1:
                        searchSensors();
                        break;
                }
            }
        });

        mAdapter = new CardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);

        speedCheckbox = (CheckBox) findViewById(R.id.speedometerCheckBox);
        heartrateCheckbox = (CheckBox) findViewById(R.id.heartrateCheckBox);
        cadenceCheckbox = (CheckBox) findViewById(R.id.cadenceCheckBox);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScrollView.activate();
    }

    @Override
    protected void onPause() {
        mCardScrollView.deactivate();
        super.onPause();
    }

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();

        mCards.add(new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                .setEmbeddedLayout(R.layout.sensors)
                .setFootnote("Sensor status"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Search for Sensors"));
    }

    private class CardScrollAdapter extends com.google.android.glass.widget.CardScrollAdapter {

        @Override
        public int getPosition(Object item) {
            return mCards.indexOf(item);
        }

        @Override
        public int getCount() {
            return mCards.size();
        }

        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }

        @Override
        public int getItemViewType(int position) {
            return mCards.get(position).getItemViewType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).getView(convertView, parent);
        }
    }
}
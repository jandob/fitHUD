package de.fithud.fithud;

import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;
import de.fithud.fithudlib.StorageService;

public class MainImmersion extends Activity implements MessengerClient {
    MessengerConnection conn = new MessengerConnection(this);
    private final String TAG = "MainImmersion";
    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private ExampleCardScrollAdapter mAdapter;
    public static float last_speed = 0;
    public static int last_revolutions = 0;
    private static final double wheel_type = 4.4686;

    public boolean speedometer_connected = false;
    public boolean heartrate_conected = false;
    public boolean cadence_connected = false;

    View sensorview;
    private int speed_sensor = 0;

    @Override
    public void handleMessage(Message msg) {
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
                if(sensor_status[0] == 1){ heartrate_conected = true;}
                else {heartrate_conected =false;}
                if(sensor_status[1] == 1){ speedometer_connected = true;}
                else {heartrate_conected =false;}
                if(sensor_status[2] == 1){ cadence_connected = true;}
                else {heartrate_conected =false;}
                break;
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.makeWorkout:
                    //mCardScrollView.setSelection(0);
                    startActivity(new Intent(MainImmersion.this, WorkoutMenu.class));
                    break;
                case R.id.showAchivements:
                    //mCardScrollView.setSelection(1);
                    startActivity(new Intent(MainImmersion.this, Achievements.class));
                    break;
                case R.id.showPlots:
                    startActivity(new Intent(MainImmersion.this, ShowPlots.class));
                    break;

                case R.id.whereIsMyBike:
                    sendWakeup();
                    break;

                case R.id.showSummary:
                    startActivity(new Intent(MainImmersion.this, SummaryView.class));
                    //startService(new Intent(this, FHLiveCardService.class));
                    break;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onCreate(Bundle bundle) {

        startService(new Intent(this, StorageService.class));
        conn.connect(FHSensorManager.class);

        Log.i("MainImmersion", "on start");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        super.onCreate(bundle);
        createCards();

        mCardScrollView = new CardScrollView(this);

        // Handle the TAP event.
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //openOptionsMenu();
                switch (mCardScrollView.getSelectedItemPosition()) {
                    case 0:
                        //Toast.makeText(getApplicationContext(), "Searching....", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainImmersion.this, SensorMenu.class));
                        break;

                    case 1:
                        startActivity(new Intent(MainImmersion.this, WorkoutMenu.class));
                        break;

                    case 2:
                        startActivity(new Intent(MainImmersion.this, Achievements.class));
                        break;

                    case 3:
                        startActivity(new Intent(MainImmersion.this, ShowPlots.class));
                        break;
                }
            }
        });

        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);
        mCardScrollView.setSelection(1);
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
        CardBuilder sensorcard = new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Sensors Settings")
                .setFootnote("Status and Search");
        sensorview = sensorcard.getView();
        mCards.add(sensorcard);

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Workout!")
                .setFootnote("Starting a workout or set guide"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Achivements")
                .setFootnote("Your unlocked Achivements!"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Plots")
                .setFootnote("Live plots"));
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, StorageService.class));
        conn.disconnect();
        super.onDestroy();

    }

    private class ExampleCardScrollAdapter extends CardScrollAdapter {

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

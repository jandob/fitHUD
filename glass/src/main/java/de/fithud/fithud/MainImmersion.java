package de.fithud.fithud;

import com.androidplot.Plot;
import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.ui.widget.Widget;
import com.androidplot.util.PlotStatistics;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;
import de.fithud.fithudlib.MessengerServiceActivity;

public class MainImmersion extends Activity implements MessengerClient {
    MessengerConnection conn = new MessengerConnection(this);
    private final String TAG = "MainImmersion";
    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private ExampleCardScrollAdapter mAdapter;

    public boolean speedometer_connected = false;
    public boolean heartrate_conected = false;
    public boolean cadence_connected = false;

    View sensorview;

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
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
        stopService(new Intent(this, StorageService.class));
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
        super.onDestroy();
        conn.disconnect();
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

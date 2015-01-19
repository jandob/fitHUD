package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.media.AudioManager;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.GuideClass;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;

/**
 * Created by JohanV on 04.01.2015.
 */
public class Achievements extends Activity implements MessengerClient{

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;
    private final String TAG = "Achievements";
    MessengerConnection conn = new MessengerConnection(this);
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MMM yyyy HH:mm");

    // Achievement values
    private static int speedRecord = 0;
    private static int distanceRecord = 0;
    private static int heightRecord = 0;
    private static int caloriesRecord = 0;

    private static String speedRecordDate;
    private static String heightRecordDate;
    private static String distanceRecordDate;
    private static String caloriesRecordDate;

    private static boolean recordChanged = false;


    @Override
    protected void onCreate(Bundle bundle) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        conn.connect(FHSensorManager.class);

        super.onCreate(bundle);

        // Load achievement history !!!

        // Set record history
        speedRecord = 15;
        distanceRecord = 200;
        heightRecord = 250;
        caloriesRecord = 50;

        // Set date/time of records
        speedRecordDate = sdf.format(new Date(0));
        distanceRecordDate = sdf.format(new Date(0));
        heightRecordDate = sdf.format(new Date(0));
        caloriesRecordDate = sdf.format(new Date(0));
        Log.d(TAG, "Date1:" + speedRecordDate);
        Log.d(TAG, "Date1:" + distanceRecordDate);
        Log.d(TAG, "Date1:" + heightRecordDate);
        Log.d(TAG, "Date1:" + caloriesRecordDate);

        createCards();

        mCardScrollView = new CardScrollView(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mAdapter = new CardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);
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

        // Create here cards based of completed achivements
        mCards = new ArrayList<CardBuilder>();

        String speedString = Integer.toString(speedRecord);

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)        // Add embedded layout
                .setText("Speed record: " + speedString + " km/h")
                .setFootnote("Not too bad")
                .setTimestamp(speedRecordDate)          // Not working
                .addImage(R.drawable.achievement_speed));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Distance record")
                .setFootnote("you biked 50km!!")
                .setTimestamp("Now")
                .addImage(R.drawable.achievement_distance));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Height record")
                .setFootnote("High as the sky!!")
                .setTimestamp("Yesterday")
                .addImage(R.drawable.achievement_height));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Calories record")
                .setFootnote("Calorie killer!!")
                .setTimestamp("Now"));
    }

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handling Msg");

        switch (msg.what) {
            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                int heartRate[] = msg.getData().getIntArray("value");
                checkHeartRate(heartRate[0]);
                Log.i(TAG, "Heartrate " + heartRate[0]);
                break;
            case FHSensorManager.Messages.CADENCE_MESSAGE:
                break;
            case FHSensorManager.Messages.HEIGTH_MESSAGE:
                break;
            case FHSensorManager.Messages.SPEED_MESSAGE:
                checkSpeed(msg.getData().getIntArray("value")[0]);
                Log.i(TAG, "Current Speed");
                break;
        }

    }

    private void checkHeartRate(int current_heartRate){


    }

    private void checkSpeed(int current_speed){

        if(current_speed > speedRecord){                // Set new record values
            speedRecord = current_speed;
            speedRecordDate = sdf.format(new Date());
            Log.i(TAG, "New speed record:" + speedRecord);
            Log.i(TAG, "Date changed: " + speedRecordDate);

            String speedString = Integer.toString(current_speed);
            mCards.get(0).setText(speedString);         // Update card - ???

            recordChanged = true;

            //textView.setText(currentDateTimeString);
        }
    }

    @Override
    protected void onDestroy() {
        //mCardScrollView.destroyDrawingCache();
        if(recordChanged){
            // Save new achievements
        }

        super.onPause();
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


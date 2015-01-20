package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
    private static int totDistanceRecord = 0;
    private static int distanceRecord = 0;
    private static int heightRecord = 0;
    private static int caloriesRecord = 0;
    private static int cadenceRecord = 0;

    private static String speedRecordDate;
    private static String heightRecordDate;
    private static String distanceRecordDate;
    private static String totDistanceStartDate;
    private static String caloriesRecordDate;
    private static String cadenceRecordDate;

    private static int[] speedAchievementLevel = new int[] {25, 50, 60, 70, 80};
    private static int[] distanceAchievementLevel = new int[] {1, 10, 25, 50, 100};
    private static int[] heightAchievementLevel = new int[] {100, 200, 300, 400, 500};

    private static int speedLevelIndex = 0;
    private static int distanceLevelIndex = 0;
    private static int heightLevelIndex = 0;

    private static boolean recordChanged = false;
    private static boolean speechOutputEnabled = false;


    @Override
    protected void onCreate(Bundle bundle) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        conn.connect(FHSensorManager.class);

        super.onCreate(bundle);

        // Load achievement history !!!

        // Set record history
        speedRecord = 15;
        distanceRecord = 50;
        heightRecord = 250;
        cadenceRecord = 120;
        caloriesRecord = 50;
        totDistanceRecord = 50;
        checkSpeed(30);

        // Set date/time of records
        speedRecordDate = sdf.format(new Date(0));
        distanceRecordDate = sdf.format(new Date(0));
        totDistanceStartDate = sdf.format(new Date(0));
        heightRecordDate = sdf.format(new Date(0));
        cadenceRecordDate = sdf.format(new Date(0));
        caloriesRecordDate = sdf.format(new Date(0));

        createCards();

        mCardScrollView = new CardScrollView(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mCardScrollView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                mAudioManager.playSoundEffect(Sounds.SUCCESS);
                switch (mCardScrollView.getSelectedItemPosition()) {
                    case 3:                     //Guide
                        resetTotDistance();
                        break;
                }
                return false;
            }
        });


        mAdapter = new CardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);
    }

    private void resetTotDistance() {
        totDistanceRecord = 0;
        mCards.get(0).setText("Total Distance: " + totDistanceRecord + " km");
        mAdapter.notifyDataSetChanged();

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

        String speedString = Integer.toString(speedRecord);

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)        // Add embedded layout
                .setText("Speed record: " + speedString + " km/h")
                .setFootnote("Not too bad")
                .setTimestamp(speedRecordDate)
                .addImage(R.drawable.achievement_speed));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Distance record: " + distanceRecord + " km")
                .setFootnote("you biked 50km!!")
                .setTimestamp(distanceRecordDate)
                .addImage(R.drawable.achievement_distance));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Total distance: " + totDistanceRecord + " km")
                .setFootnote("you biked 50km!!")
                .setTimestamp(totDistanceStartDate)
                .addImage(R.drawable.achievement_distance));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Height record: " + heightRecord + "m")
                .setFootnote("High as the sky!!")
                .setTimestamp(heightRecordDate)
                .addImage(R.drawable.achievement_height));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Cadence record: " + cadenceRecord + "m")
                .setFootnote("You can do better")
                .setTimestamp(cadenceRecordDate)
                .addImage(R.drawable.achievement_speed));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Calories record: " + caloriesRecord)
                .setFootnote("Calorie killer!!")
                .setTimestamp(caloriesRecordDate));
    }

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handling Msg");

        switch (msg.what) {
            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                //int heartRate[] = msg.getData().getIntArray("value");
                //checkHeartRate(heartRate[0]);
                //Log.i(TAG, "Heartrate " + heartRate[0]);
                break;
            case FHSensorManager.Messages.CADENCE_MESSAGE:
                checkCadence(msg.getData().getIntArray("value")[0]);
                Log.i(TAG, "Current cadence");
                break;
            case FHSensorManager.Messages.HEIGTH_MESSAGE:
                checkHeight(msg.getData().getIntArray("value")[0]);
                Log.i(TAG, "Current height");
                break;
            case FHSensorManager.Messages.SPEED_MESSAGE:
                //checkSpeed(msg.getData().getIntArray("value")[0]);
                Log.i(TAG, "Current speed");
                break;
            /*case FHSensorManager.Messages.SPEED_MESSAGE:
                checkCadence(msg.getData().getIntArray("value")[0]);
                Log.i(TAG, "Calories");
                break;*/
        }

    }

    private void checkCadence(int current_cadence) {

        if(current_cadence > cadenceRecord){                // Set new record values
            cadenceRecord = current_cadence;
            cadenceRecordDate = sdf.format(new Date());     // Get date of record
            Log.i(TAG, "New cadence record:" + cadenceRecord);
            Log.i(TAG, "Date changed: " + cadenceRecordDate);

            String cadenceString = Integer.toString(current_cadence);
            mCards.get(0).setText(cadenceString);
            mAdapter.notifyDataSetChanged();
            recordChanged = true;
        }
    }

    private void checkHeight(int current_height) {

        if(current_height > heightRecord){                          // Set new record values
            heightRecord = current_height;
            heightRecordDate = sdf.format(new Date());              // Get date of record
            Log.i(TAG, "New height record:" + heightRecord);
            Log.i(TAG, "Date changed: " + heightRecordDate);

            String heightString = Integer.toString(current_height);
            mCards.get(0).setText(heightString);
            mAdapter.notifyDataSetChanged();
            recordChanged = true;
        }

    }


/*
    private void checkCalories(int current_heartRate){


    }*/

    private void checkSpeed(int current_speed){

        if(current_speed > speedRecord){                            // Set new record values
            speedRecord = current_speed;
            speedRecordDate = sdf.format(new Date());               // Get date of record
            Log.i(TAG, "New speed record:" + speedRecord);
            Log.i(TAG, "Date changed: " + speedRecordDate);

            recordChanged = true;
        }

        if (speedLevelIndex + 1 <= speedAchievementLevel.length) {
            int speedDiff = current_speed - speedAchievementLevel[speedLevelIndex + 1];
            Log.d(TAG,"Speed diff: " + speedDiff);
        }

        if((speedRecord >= speedAchievementLevel[speedLevelIndex]) && speedLevelIndex <= speedAchievementLevel.length){
            Log.d(TAG,"Speed level: " + speedAchievementLevel[speedLevelIndex]);

            if (speedLevelIndex + 1 <= speedAchievementLevel.length) {
                speedLevelIndex++;
            }

            if (speechOutputEnabled) {
                // TODO: Speak output
                //
            }
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


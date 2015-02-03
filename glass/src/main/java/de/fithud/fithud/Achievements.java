package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.speech.tts.TextToSpeech;
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
import java.util.Locale;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.GuideClass;
import de.fithud.fithudlib.GuideService;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;

/**
 * Created by JohanV on 04.01.2015.
 */
public class Achievements extends Activity implements MessengerClient {

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;
    private final String TAG = "Achievements";
    MessengerConnection conn = new MessengerConnection(this);
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MMM yyyy HH:mm");
    private TextToSpeech tts;

    // Achievement values
    private static int speedRecordReachedSpeed = 0; // Erreichte Geschwindigkeit
    private static int speedRecordNextSpeed = 0;    // Nächst erreichbare Geschwindigkeit
    private static int speedRecordLevel = 0;        // Erreichtes level
    private static int speedRecordLevels = 0;       // Erreichbare levels

    private static int distanceRecordReachedDistance = 0;
    private static int distanceRecordNextDistance = 0;
    private static int distanceRecordLevel = 0;
    private static int distanceRecordLevels = 0;

    private static int heightRecordReachedHeight = 0;
    private static int heightRecordNextHeight = 0;
    private static int heightRecordLevel = 0;
    private static int heightRecordLevels = 0;

    private static int cadenceRecordReachedBPM = 0;
    private static int cadenceRecordNextBPM = 0;
    private static int cadenceRecordLevel = 0;
    private static int cadenceRecordLevels = 0;

    private static int caloriesRecordReachedBurnedCals = 0;
    private static int caloriesRecordNextBurnedCals = 0;
    private static int caloriesRecordLevel = 0;
    private static int caloriesRecordLevels = 0;

    private static int totDistanceRecord = 0;
    private static int distanceRecord = 0;
    private static int nextDistanceRecord = 0;
    private static int heightRecord = 0;
    private static int nextHeightRecord = 0;
    private static int caloriesRecord = 0;
    private static int nextCaloriesRecord = 0;
    private static int cadenceRecord = 0;
    private static int nextCadenceRecord = 0;

    private static String speedRecordDate;
    private static String heightRecordDate;
    private static String distanceRecordDate;
    private static String totDistanceStartDate;
    private static String caloriesRecordDate;
    private static String cadenceRecordDate;

    private static int speedLevelIndex = 0;
    private static int heightLevelIndex = 0;
    private static int cadenceLevelIndex = 0;

    @Override
    protected void onCreate(Bundle bundle) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        conn.connect(GuideService.class);

        super.onCreate(bundle);

        // Load achievement history !!!

        // Set record history
        speedRecordReachedSpeed = 25;
        distanceRecord = 50;
        heightRecord = 0;
        cadenceRecord = 120;
        caloriesRecord = 50;
        totDistanceRecord = 50;


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

    private void changeSpeedCard(int speedRecordReachedSpeed, int speedRecordNextSpeed, int speedRecordLevel, int speedRecordLevels) {
        mCards.get(0).setText("Speed Record: " + speedRecordReachedSpeed + " km/h");
        mCards.get(0).setTimestamp("Level " + speedRecordLevel + " / " + speedRecordLevels + " reached.");
        mCards.get(0).setFootnote("CHALLENGE: " + speedRecordNextSpeed + " km/h");
        //mAdapter.notifyDataSetChanged();
    }

    private void changeDistanceCard(int distanceRecordReachedDistance, int distanceRecordNextDistance, int distanceRecordLevel, int distanceRecordLevels) {
        mCards.get(1).setText("Distance Record: " + distanceRecordReachedDistance + " km");
        mCards.get(1).setTimestamp("Level " + distanceRecordLevel + " / " + distanceRecordLevels + " reached.");
        mCards.get(1).setFootnote("CHALLENGE: " + distanceRecordNextDistance + " km");
        //mAdapter.notifyDataSetChanged();
    }

    private void changeHeightCard(int heightRecordReachedHeight, int heightRecordNextHeight, int heightRecordLevel, int heightRecordLevels) {
        mCards.get(2).setText("Height Record: " + heightRecordReachedHeight + " m");
        mCards.get(2).setTimestamp("Level " + heightRecordLevel + " / " + heightRecordLevels + " reached.");
        mCards.get(2).setFootnote("CHALLENGE: " + heightRecordNextHeight + " m");
        //mAdapter.notifyDataSetChanged();
    }

    private void changeCadenceCard(int cadenceRecordReachedBPM, int cadenceRecordNextBPM, int cadenceRecordLevel, int cadenceRecordLevels) {
        mCards.get(3).setText("Cadence Record: " + cadenceRecordReachedBPM + " bpm");
        mCards.get(3).setTimestamp("Level " + cadenceRecordLevel + " / " + cadenceRecordLevels + " reached.");
        mCards.get(3).setFootnote("CHALLENGE: " + cadenceRecordNextBPM + " bpm");
        //mAdapter.notifyDataSetChanged();
    }

    private void changeCaloriesCard(int caloriesRecordReachedBurnedCals, int caloriesRecordNextBurnedCals, int caloriesRecordLevel, int caloriesRecordLevels) {
        mCards.get(4).setText("Calories Record: " + caloriesRecordReachedBurnedCals + " kCal");
        mCards.get(4).setTimestamp("Level " + caloriesRecordLevel + " / " + caloriesRecordLevels + " reached.");
        mCards.get(4).setFootnote("CHALLENGE: " + caloriesRecordNextBurnedCals + " kCal");
        //mAdapter.notifyDataSetChanged();
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

        String speedString = Integer.toString(speedRecordReachedSpeed);

        // 0: Achievement card for speed
        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)        // Add embedded layout
                .setText("Speed record: " + speedString + " km/h")
                .setFootnote("Not too bad")
                .setTimestamp(speedRecordDate)
                .addImage(R.drawable.achievement_speed));

        // 1: Achievement card for distance
        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Distance record: " + distanceRecord + " km")
                .setFootnote("you biked 50km!!")
                .setTimestamp(distanceRecordDate)
                .addImage(R.drawable.achievement_distance));

        // 2: Achievement card for height
        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Height record: " + heightRecord + "m")
                .setFootnote("High as the sky!!")
                .setTimestamp(heightRecordDate)
                .addImage(R.drawable.achievement_height));

        // 3: Achievement card for cadence
        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Cadence record: " + cadenceRecord + "m")
                .setFootnote("You can do better")
                .setTimestamp(cadenceRecordDate)
                .addImage(R.drawable.achievement_heartrate));

        // 4: Achievement card for calories
        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Calories record: " + caloriesRecord)
                .setFootnote("Calorie killer!!")
                .setTimestamp(caloriesRecordDate)
                .addImage(R.drawable.achievement_calories));

        // ToDo: Port to Handle messages...
        // Speed data from msg service
        speedRecordReachedSpeed = 20;
        speedRecordNextSpeed = 30;
        speedRecordLevel = 1;
        speedRecordLevels = 7;
        changeSpeedCard(speedRecordReachedSpeed, speedRecordNextSpeed, speedRecordLevel, speedRecordLevels);

        // Distance data from msg service
        distanceRecordReachedDistance = 1;
        distanceRecordNextDistance = 2;
        distanceRecordLevel = 1;
        distanceRecordLevels = 5;
        changeDistanceCard(distanceRecordReachedDistance, distanceRecordNextDistance, distanceRecordLevel, distanceRecordLevels);

        // Height data from msg service
        heightRecordReachedHeight = 100;
        heightRecordNextHeight = 500;
        heightRecordLevel = 1;
        heightRecordLevels = 4;
        changeHeightCard(heightRecordReachedHeight, heightRecordNextHeight, heightRecordLevel, heightRecordLevels);

        // Cadence data from msg service
        cadenceRecordReachedBPM = 70;
        cadenceRecordNextBPM = 80;
        cadenceRecordLevel = 1;
        cadenceRecordLevels = 4;
        changeCadenceCard(cadenceRecordReachedBPM, cadenceRecordNextBPM, cadenceRecordLevel, cadenceRecordLevels);

        // Calories data from msg service
        caloriesRecordReachedBurnedCals = 500;
        caloriesRecordNextBurnedCals= 100;
        caloriesRecordLevel = 1;
        caloriesRecordLevels = 3;
        changeCaloriesCard(caloriesRecordReachedBurnedCals, caloriesRecordNextBurnedCals, caloriesRecordLevel, caloriesRecordLevels);
    }

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handling Msg");
/*
        switch (msg.what) {
            case FHSensorManager.Messages.CADENCE_MESSAGE:
                //checkCadence(msg.getData().getIntArray("value")[0]);
                //Log.i(TAG, "Current cadence");
                break;
            case FHSensorManager.Messages.HEIGTH_MESSAGE:
                //checkHeight(msg.getData().getIntArray("value")[0]);
                //Log.i(TAG, "Current height");
                break;
            case FHSensorManager.Messages.SPEED_MESSAGE:
                //checkSpeed(msg.getData().getIntArray("value")[0]);
                //Log.i(TAG, "Current speed");
                break;
        }
*/

        switch (msg.what) {
            case GuideService.GuideMessages.ACHIEVEMENT_SPEED:
                String test = msg.getData().getString("text");
                Log.i("achMsg", "X"+test);
                break;
            case GuideService.GuideMessages.ACHIEVEMENT_HEIGHT:
                String test2 = msg.getData().getString("text");
                Log.i("achMsg", "Y"+test2);
                break;
        }

    }

    @Override
    protected void onDestroy() {
        //mCardScrollView.destroyDrawingCache();
        conn.disconnect();
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


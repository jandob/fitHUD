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
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;

/**
 * Created by JohanV on 04.01.2015.
 */
public class Achievements extends Activity implements MessengerClient, TextToSpeech.OnInitListener {

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;
    private final String TAG = "Achievements";
    MessengerConnection conn = new MessengerConnection(this);
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MMM yyyy HH:mm");
    private TextToSpeech tts;

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

    private static int[] speedAchievementLevel = new int[] {0, 25, 50, 60, 70, 80};
    private static int[] heightAchievementLevel = new int[] {0, 100, 200, 300, 400, 500};
    private static int[] cadenceAchievementLevel = new int[] {0, 60, 80, 120, 150};

    private static int speedLevelIndex = 0;
    private static int heightLevelIndex = 0;
    private static int cadenceLevelIndex = 0;

    private static int speedDiff = 0;
    private static int heightDiff = 0;
    private static int cadenceDiff = 0;

    private static boolean recordChanged = false;
    private static boolean speechOutputEnabled = true;


    @Override
    protected void onCreate(Bundle bundle) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        conn.connect(FHSensorManager.class);

        super.onCreate(bundle);

        tts = new TextToSpeech(this,this);

        // Load achievement history !!!

        // Set record history
        speedRecord = 15;
        distanceRecord = 50;
        heightRecord = 0;
        cadenceRecord = 120;
        caloriesRecord = 50;
        totDistanceRecord = 50;

        checkHeight(150);

        checkHeight(250);

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
                checkSpeed(msg.getData().getIntArray("value")[0]);
                Log.i(TAG, "Current speed");
                break;
            /*case FHSensorManager.Messages.SPEED_MESSAGE:
                checkCadence(msg.getData().getIntArray("value")[0]);
                Log.i(TAG, "Calories");
                break;*/
        }

    }

    private void checkCadence(int current_cadence) {

        if(current_cadence > cadenceRecord){                            // Set new record values
            cadenceRecord = current_cadence;
            cadenceRecordDate = sdf.format(new Date());               // Get date of record
            Log.i(TAG, "New cadence record:" + cadenceRecord);
            Log.i(TAG, "Date changed: " + cadenceRecordDate);

            recordChanged = true;

            if (cadenceLevelIndex + 1 <= cadenceAchievementLevel.length) {

                if(cadenceRecord >= cadenceAchievementLevel[cadenceLevelIndex+1]){
                    cadenceLevelIndex++;
                    Log.d(TAG,"cadence level: " + cadenceAchievementLevel[cadenceLevelIndex]);

                    if (speechOutputEnabled) {
                        // TODO: tts.speak("New cadence achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                cadenceDiff = cadenceAchievementLevel[cadenceLevelIndex + 1] - cadenceRecord;
                Log.d(TAG,"cadence diff: " + cadenceDiff);
            }
        }
    }

    //TODO: Height or max elevation?
    private void checkHeight(int current_height) {

        if(current_height > heightRecord){                            // Set new record values
            heightRecord = current_height;
            heightRecordDate = sdf.format(new Date());               // Get date of record
            Log.i(TAG, "New height record:" + heightRecord);
            Log.i(TAG, "Date changed: " + heightRecordDate);

            recordChanged = true;

            if (heightLevelIndex + 1 <= heightAchievementLevel.length) {

                if(heightRecord >= heightAchievementLevel[heightLevelIndex+1]){
                    heightLevelIndex++;
                    Log.d(TAG,"Height level: " + heightAchievementLevel[heightLevelIndex]);

                    if (speechOutputEnabled) {
                        // TODO: tts.speak("New height achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                heightDiff = heightAchievementLevel[heightLevelIndex + 1] - heightRecord;
                Log.d(TAG,"Height diff: " + heightDiff);
            }
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
            if (speedLevelIndex + 1 <= speedAchievementLevel.length) {

                if(speedRecord >= speedAchievementLevel[speedLevelIndex+1]){
                    speedLevelIndex++;
                    Log.d(TAG,"Speed level: " + speedAchievementLevel[speedLevelIndex]);

                    if (speechOutputEnabled) {
                        // TODO: tts.speak("New speed achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                speedDiff = speedAchievementLevel[speedLevelIndex + 1] - speedRecord;
                Log.d(TAG,"Speed diff: " + speedDiff);

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

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, "TTS:This Language is not supported");
            } else {
                //speech_text = "Speech activated";
                //tts.speak(speech_text, TextToSpeech.QUEUE_FLUSH, null);
            }
        } else {
            Log.d(TAG, "TTS:Initilization Failed!");
        }
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


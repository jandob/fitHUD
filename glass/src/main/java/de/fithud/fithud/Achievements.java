package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.media.AudioManager;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.fithud.fithudlib.FHSensorManager;
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
    private static final String TAG = Achievements.class.getSimpleName();
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
    private static int heightRecord = 0;
    private static int caloriesRecord = 0;
    private static int cadenceRecord = 0;


    private static String speedRecordDate;
    private static String heightRecordDate;
    private static String distanceRecordDate;
    private static String totDistanceStartDate;
    private static String caloriesRecordDate;
    private static String cadenceRecordDate;

    private boolean actCreated = false;


    @Override
    protected void onCreate(Bundle bundle) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        conn.connect(FHSensorManager.class);

        super.onCreate(bundle);

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
                        break;
                }
                return false;
            }
        });


        mAdapter = new CardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);

        actCreated = true;
    }

    private void changeSpeedCard() {
        mCards.get(0).setText("Speed Record: " + GuideService.speedRecord + " km/h");
        mCards.get(0).setTimestamp("Level " + GuideService.speedLevelIndex + " / " + GuideService.speedAchievementLevels.length + " reached.");
        if (GuideService.speedLevelIndex+2 <= GuideService.speedAchievementLevels.length) {
            mCards.get(0).setFootnote("Next level: " + GuideService.speedAchievementLevels[GuideService.speedLevelIndex+1] + " km/h");
        } else {
            mCards.get(0).setFootnote("All levels completed");
        }
    }

    private void changeDistanceCard() {
        mCards.get(1).setText("Distance Record: " + GuideService.distanceRecord + " km");
        mCards.get(1).setTimestamp("Level " + GuideService.distanceLevelIndex + " / " + GuideService.distanceAchievementLevels.length + " reached.");
        if (GuideService.distanceLevelIndex+2 <= GuideService.distanceAchievementLevels.length){
            mCards.get(1).setFootnote("Next level: " + GuideService.distanceAchievementLevels[GuideService.distanceLevelIndex+1] + " km");
        } else {
            mCards.get(1).setFootnote("All levels completed");
        }

    }

    private void changeHeightCard() {
        mCards.get(2).setText("Height Record: " + GuideService.heightRecord + " m");
        mCards.get(2).setTimestamp("Level " + GuideService.speedLevelIndex + " / " + GuideService.heightAchievementLevels.length + " reached.");
        if (GuideService.heightLevelIndex+2 <= GuideService.heightAchievementLevels.length){
            mCards.get(2).setFootnote("Next level: " + GuideService.heightAchievementLevels[GuideService.heightLevelIndex+1] + " m");
        } else {
            mCards.get(2).setFootnote("All levels completed");
        }
    }

    private void changeCadenceCard() {
        mCards.get(3).setText("Cadence Record: " + GuideService.cadenceRecord + " bpm");
        mCards.get(3).setTimestamp("Level " + GuideService.cadenceLevelIndex + " / " + GuideService.cadenceAchievementLevels.length + " reached.");
        if(GuideService.cadenceLevelIndex+2 <= GuideService.cadenceAchievementLevels.length) {
            mCards.get(3).setFootnote("Next level: " + GuideService.cadenceAchievementLevels[GuideService.cadenceLevelIndex+1] + " bpm");
        } else {
            mCards.get(3).setFootnote("All levels completed");
        }
    }

    private void changeCaloriesCard() {
        mCards.get(4).setText("Calories Record: " + GuideService.caloriesRecord + " kCal");
        mCards.get(4).setTimestamp("Level " + GuideService.caloriesLevelIndex + " / " + GuideService.caloriesAchievementLevels.length + " reached.");
        if(GuideService.caloriesLevelIndex+2 <= GuideService.caloriesAchievementLevels.length) {
            mCards.get(4).setFootnote("Next level: " + GuideService.caloriesAchievementLevels[GuideService.caloriesLevelIndex+1] + " kCal");
        } else {
            mCards.get(4).setFootnote("All levels completed");
        }
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

        changeSpeedCard();
        changeHeightCard();
        changeCadenceCard();
        changeCaloriesCard();
        changeDistanceCard();
    }

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handling Msg");

        if (actCreated) {
            switch (msg.what) {
                case FHSensorManager.Messages.CADENCE_MESSAGE:
                    changeCadenceCard();
                    mAdapter.notifyDataSetChanged();
                    break;
                case FHSensorManager.Messages.HEIGTH_MESSAGE:
                    changeHeightCard();
                    mAdapter.notifyDataSetChanged();
                    break;
                case FHSensorManager.Messages.SPEED_MESSAGE:
                    changeSpeedCard();
                    mAdapter.notifyDataSetChanged();
                    break;

                case FHSensorManager.Messages.CALORIES_MESSAGE:
                    changeCaloriesCard();
                    mAdapter.notifyDataSetChanged();
                    break;

                case FHSensorManager.Messages.DISTANCE_MESSAGE:
                    changeDistanceCard();
                    mAdapter.notifyDataSetChanged();
                    break;
            }
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


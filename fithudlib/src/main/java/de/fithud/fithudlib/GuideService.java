package de.fithud.fithudlib;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;


import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


/**
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */

public class GuideService extends MessengerService implements TextToSpeech.OnInitListener, MessengerClient {
    //CardBuilder mCard;
    MessengerConnection conn;
    private TextToSpeech tts;

    private static final String TAG = GuideService.class.getSimpleName();

    private static final int cardio = 0;
    private static final int fatburn = 1;
    private static final int interval = 2;

    private static int hr_min = 0;
    private static int hr_max = 0;
    private static int speed_low_min = 80;
    private static int speed_low_max = 90;
    private static int speed_high_min = 90;
    private static int speed_high_max = 100;
    private static long intervalTime = 5000;      // Duration of an interval in mSec

    private static boolean interval_state = false;
    private static long startTime = 0;

    private static int DISABLED = 4;
    private static boolean guide_active = false;
    public static int training_mode = DISABLED;
    public static int challenge_mode = DISABLED;
    private static boolean speech_active = false;
    private static boolean workout_started = false;
    public static boolean summaryBoundToGuide = false;
    //private static int speechCounter = 0;
    //private static int speechPeriod = 10;

    private static final int heightAim = 100;       // Height difference in meters
    private static final int cadenceAim = 120;      // Cadence in rotations per minute
    private static final int caloriesAim = 100;

    private static String GuideText = "";
    private static String GuideTextPrev = "";
    private static int progressIndex = 0;

    // Achievements variables
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MMM yyyy HH:mm");

    private static int speedRecord = 0;
    private static int heightRecord = 0;
    private static int cadenceRecord = 0;

    private static String speedRecordDate;
    private static String heightRecordDate;
    private static String cadenceRecordDate;

    private static String workoutRunningTime;
    private long workoutStartTime;
    private long workoutCurrentTime;

    private static int[] speedAchievementLevels = new int[] {0, 20, 30, 40, 50, 60, 70, 80};
    private static int[] distanceAchievementLevels = new int[] {0, 1, 2, 5, 10, 50};
    private static int[] heightAchievementLevels = new int[] {0, 100, 500, 1000, 1500};
    private static int[] cadenceAchievementLevels = new int[] {0, 70, 80, 120, 130};
    private static int[] caloriesAchievementLevel = new int[] {0, 50, 100, 150};

    private static int speedLevelIndex = 0;
    private static int heightLevelIndex = 0;
    private static int cadenceLevelIndex = 0;

    private static int speedDiff = 0;
    private static int heightDiff = 0;
    private static int cadenceDiff = 0;

    private static boolean recordChanged = false;
    private static boolean speechOutputEnabled = true;

    // end Achievements variables


    public final class GuideMessages extends MessengerService.Messages {
        public static final int GUIDE_COMMAND = 30;
        public static final int TRAINING_MODE_COMMAND = 31;
        public static final int CHALLENGE_MODE_COMMAND = 32;
        public static final int SPEECH_COMMAND = 33;
        public static final int GUIDE_TEXT = 34;
        public static final int WORKOUT_COMMAND = 35;
        public static final int GUIDE_TIME = 36;
        public static final int ACHIEVEMENT_SPEED = 37;
        public static final int ACHIEVEMENT_HEIGHT = 38;
    }


    @Override
    public void handleMessage(Message msg) {

        float value;

        switch (msg.what) {
            case GuideService.GuideMessages.GUIDE_COMMAND:
                //updateTrainingMode(training_mode);
                guide_active = msg.getData().getBoolean("guideActive");
                Log.i(TAG,"Guide active: " + msg.getData().getBoolean("guideActive"));
                if (speech_active){
                    if (guide_active){
                        tts.speak("Guide is now activated", TextToSpeech.QUEUE_FLUSH, null);
                    } else {
                        tts.speak("Guide is not activated anymore", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                break;

            case GuideService.GuideMessages.WORKOUT_COMMAND:
                workout_started = msg.getData().getBoolean("workoutActive");
                Log.v(TAG, "WorkoutActive changed");

                if (workout_started) {
                    startTimer();
                } else {
                    stopTimer();
                }

                if(speech_active){
                    if (workout_started){
                        tts.speak("Workout started.", TextToSpeech.QUEUE_FLUSH, null);
                    } else {
                        tts.speak("Workout finished.", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                break;

            case GuideService.GuideMessages.TRAINING_MODE_COMMAND:
                training_mode = (msg.getData().getInt("trainingMode"));
                updateTrainingMode(training_mode);
                challenge_mode = DISABLED;
                progressIndex = 0;
                Log.i(TAG,"Training mode changed." + training_mode);
                if (speech_active){
                    switch (training_mode) {
                        case 0:
                            tts.speak("Cardio training selected", TextToSpeech.QUEUE_FLUSH, null);
                            break;
                        case 1:
                            tts.speak("Fat burn training selected", TextToSpeech.QUEUE_FLUSH, null);
                            break;
                        case 2:
                            tts.speak("Interval training selected", TextToSpeech.QUEUE_FLUSH, null);
                            break;
                    }
                }
                break;

            case GuideService.GuideMessages.CHALLENGE_MODE_COMMAND:
                challenge_mode = (msg.getData().getInt("challengeMode"));
                training_mode = DISABLED;
                progressIndex = 0;
                Log.i(TAG,"Challenge mode changed." + challenge_mode);
                if (speech_active){
                    switch (challenge_mode) {
                        case 0:
                            tts.speak("Height challenge selected", TextToSpeech.QUEUE_FLUSH, null);
                            break;
                        case 1:
                            tts.speak("Cadence challenge selected", TextToSpeech.QUEUE_FLUSH, null);
                            break;
                        case 2:
                            tts.speak("Calories challenge selected", TextToSpeech.QUEUE_FLUSH, null);
                            break;
                    }
                }

                break;

            case GuideService.GuideMessages.SPEECH_COMMAND:
                speech_active = msg.getData().getBoolean("speechActive");
                Log.i(TAG,"Speech active: " + speech_active);
                if (speech_active) {
                    tts.speak("Speech enabled", TextToSpeech.QUEUE_FLUSH, null);
                }
                break;

            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "HR received" + value);

                if(workout_started && guide_active && training_mode < 2) {  // Cardio & Fatburn training
                    heartRateCheck((int)value);
                }
                break;

            case FHSensorManager.Messages.CALORIES_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "Calories received" + value);

                if(workout_started && guide_active && challenge_mode == 2) {    // Calories challenge
                    caloriesCheck((int)value);
                }
                break;

            case FHSensorManager.Messages.CADENCE_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "cadence received" + value);

                if(workout_started && guide_active && (challenge_mode == 1)) {  // Cadence challenge
                    cadenceCheck(value);
                }
                break;

            case FHSensorManager.Messages.SPEED_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "speed received" + value);

                if(workout_started) {
                    if (guide_active && (training_mode == 2)) {          // Interval training mode
                        speedCheck(value);
                    }
                    achievementSpeedCheck((int) value);                 // Check speed achievements
                }

                break;

            case FHSensorManager.Messages.HEIGTH_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "height received" + value);

                if(workout_started) {

                    if (guide_active && (challenge_mode == 0)) {         // Height challenge
                        heightCheck((int) value);
                    }

                    achievementHeightCheck((int) value);
                }
                break;



        }
    }

    public void updateTrainingMode(int training_mode){
        // TODO: Set min/max heart rate for cardio training
        if(training_mode == cardio){
            hr_min = 80;
            hr_max = 90;
        }
        // TODO: Set min/max heart rate for fatburn training
        if(training_mode == fatburn){
            hr_min = 90;
            hr_max = 100;
        }

        if(training_mode == interval){
            startTime = System.currentTimeMillis();
        }
    }

    private void heartRateCheck(int heartRate) {
        GuideTextPrev = GuideText;
        if(heartRate < hr_min){
            GuideText = "Your heart rate is too low!";
        } else if (heartRate < hr_max) {
            GuideText = "Perfect pace, keep going!";
        } else {
            GuideText = "Slow down, your heart rate is too high!";
        }

        sendMsgString(GuideMessages.GUIDE_TEXT, GuideText);
        if(speech_active && summaryBoundToGuide && GuideTextPrev != GuideText) {
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH,null);
        }
        /*if(speech_active && speechCounter == speechPeriod){
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH,null);
            speechCounter = 0;
        }
        speechCounter++;*/
    };

    private void speedCheck(float current_speed) {
        GuideTextPrev = GuideText;
        long currentTime = System.currentTimeMillis();
        if((currentTime - startTime) >= intervalTime) {
            interval_state = !interval_state;
            startTime = System.currentTimeMillis();
        }
        if (interval_state){
            if(current_speed < speed_high_min) {
                GuideText = "Go faster, you are too slow";
            } else if (current_speed < speed_high_max) {
                GuideText = "Perfect pace!";
            } else {
                GuideText = "Slow down, you are too slow";
            }
        } else {
            if(current_speed < speed_low_min) {
                GuideText = "Go faster, you are too slow";
            } else if (current_speed < speed_low_max) {
                GuideText = "Perfect pace!";
            } else {
                GuideText = "Slow down, you are too slow";
            }
        }

        sendMsgString(GuideMessages.GUIDE_TEXT, GuideText);
        if(speech_active && summaryBoundToGuide && GuideTextPrev != GuideText) {
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH,null);
        }
        /*if(speech_active && speechCounter == speechPeriod){
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH,null);
            speechCounter = 0;
        }
        speechCounter++;*/
    };


    // Need to reset all flags if new challenge started.
    private void cadenceCheck(float current_cadence) {
        GuideTextPrev = GuideText;
        if(current_cadence > 0 && progressIndex == 0){
            GuideText = "Let's get started.";
            progressIndex++;
        } else if(current_cadence >= (0.5 * cadenceAim) && progressIndex == 1) {
            GuideText = "You are half way there.";
            progressIndex++;
        } else if(current_cadence >= (0.8 * cadenceAim) && progressIndex == 2) {
            GuideText = "Your are almost there";
            progressIndex++;
        } else if(current_cadence >= (0.9 * cadenceAim) && progressIndex == 3) {
            GuideText = "Only a few meters left";
            progressIndex++;
        } else if(current_cadence >= cadenceAim && progressIndex == 4){
            GuideText = "Congratulations! You have completed your challenge";
            progressIndex++;
        } else {
            GuideText = "Keep going.";
        }
        Log.v(TAG, "Guide: " + GuideText);
        sendMsgString(GuideMessages.GUIDE_TEXT, GuideText);
        if(speech_active && summaryBoundToGuide && GuideTextPrev != GuideText) {
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH,null);
        }
        /*if(speech_active && speechCounter == speechPeriod){
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH, null);
            speechCounter = 0;
        }
        speechCounter++;*/
    };

    public void heightCheck(int current_height) {
        GuideTextPrev = GuideText;
        if(current_height > 0 && progressIndex == 0){
            GuideText = "Let's get started.";
            progressIndex++;
        } else if(current_height >= (0.5 * heightAim) && progressIndex == 1) {
            GuideText = "You are half way there.";
            progressIndex++;
        } else if(current_height >= (0.8 * heightAim) && progressIndex == 2) {
            GuideText = "Your are almost there";
            progressIndex++;
        } else if(current_height >= (0.9 * heightAim) && progressIndex == 3) {
            GuideText = "Only a few meters left";
            progressIndex++;
        } else if(current_height >= heightAim && progressIndex == 4){
            GuideText = "Congratulations! You have completed your challenge successfully";
            progressIndex++;
        }

        sendMsgString(GuideMessages.GUIDE_TEXT, GuideText);
        if(speech_active && summaryBoundToGuide &&GuideTextPrev != GuideText) {
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH,null);
        }
        /*if(speech_active && speechCounter == speechPeriod){
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH, null);
            speechCounter = 0;
        }
        speechCounter++;*/
    };

    public void caloriesCheck(int current_calories) {
        GuideTextPrev = GuideText;
        if(current_calories > 0 && progressIndex == 0){
            GuideText = "Let's get started.";
            progressIndex++;
        } else if(current_calories >= (0.5 * caloriesAim) && progressIndex == 1) {
            GuideText = "You are half way there.";
            progressIndex++;
        } else if(current_calories >= (0.8 * caloriesAim) && progressIndex == 2) {
            GuideText = "Your are almost there";
            progressIndex++;
        } else if(current_calories >= (0.9 * caloriesAim) && progressIndex == 3) {
            GuideText = "Only a few more calories";
            progressIndex++;
        } else if(current_calories >= caloriesAim && progressIndex == 4){
            GuideText = "Congratulations! You have completed your challenge successfully";
            progressIndex++;
        }

        sendMsgString(GuideMessages.GUIDE_TEXT, GuideText);
        if(speech_active && summaryBoundToGuide && GuideTextPrev != GuideText) {
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH,null);
        }
        /*if(speech_active && speechCounter == speechPeriod){
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH, null);
            speechCounter = 0;
        }
        speechCounter++;*/
    };

    @Override
    public void onCreate() {
        super.onCreate();
        conn = new MessengerConnection(this);
        conn.connect(FHSensorManager.class);
        tts = new TextToSpeech(this,this);

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, "TTS:This Language is not supported");
            }
        } else {
            Log.d(TAG, "TTS:Initilization Failed!");
        }
    }

    @Override
    public void onDestroy() {
        speech_active = false;
        summaryBoundToGuide = false;
        conn.disconnect();

        // Close the Text to Speech Library
        if(tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTS Destroyed");
        }

        super.onDestroy();
    }

    void sendMsgString(int messageType, String content){
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {Message msg = Message.obtain(null, messageType);
                Bundle bundle = new Bundle();
                // bundle.putFloat("value", val);
                // bundle.putFloat("value", val);
                bundle.putString("text", content);
                msg.setData(bundle);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    private void achievementSpeedCheck(int current_speed){

        if(current_speed > speedRecord){                            // Set new record values
            speedRecord = current_speed;
            speedRecordDate = sdf.format(new Date());               // Get date of record
            Log.i(TAG, "New speed record:" + speedRecord);
            Log.i(TAG, "Date changed: " + speedRecordDate);

            recordChanged = true;
            if (speedLevelIndex + 2 <= speedAchievementLevels.length) {

                if(speedRecord >= speedAchievementLevels[speedLevelIndex+1]){
                    speedLevelIndex++;
                    Log.d(TAG,"Speed level: " + speedAchievementLevels[speedLevelIndex]);

                    if (speechOutputEnabled) {
                        tts.speak("New speed achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }

                    // ToDo receive and act accordingly in Achievements.java
                    // ZU SENDEN: speedLevelIndex, speedAchievementLevels[speedLevelIndex], speedAchievementLevels[speedLevelIndex + 1], speedAchievementLevels.length
                    sendMsgString(GuideMessages.ACHIEVEMENT_SPEED, "value");

                    Toast.makeText(this, "Speed record: " + speedAchievementLevels[speedLevelIndex] + "km/h", Toast.LENGTH_LONG).show();
                }
                //speedDiff = speedAchievementLevels[speedLevelIndex + 1] - speedRecord;
                //Log.d(TAG,"Speed diff: " + speedDiff);

            }
        }
    }

    //TODO: Height or max elevation?
    private void achievementHeightCheck(int current_height) {

        if(current_height > heightRecord){                            // Set new record values
            heightRecord = current_height;
            heightRecordDate = sdf.format(new Date());               // Get date of record
            Log.i(TAG, "New height record:" + heightRecord);
            Log.i(TAG, "Date changed: " + heightRecordDate);

            recordChanged = true;

            if (heightLevelIndex + 1 <= heightAchievementLevels.length) {

                if(heightRecord >= heightAchievementLevels[heightLevelIndex+1]){
                    heightLevelIndex++;
                    Log.d(TAG,"Height level: " + heightAchievementLevels[heightLevelIndex]);

                    if (speechOutputEnabled) {
                        tts.speak("New height achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }
                    Toast.makeText(this, "Height record: " + heightAchievementLevels[heightLevelIndex+1] + "m", Toast.LENGTH_LONG).show();
                }
                heightDiff = heightAchievementLevels[heightLevelIndex + 1] - heightRecord;
                Log.d(TAG,"Height diff: " + heightDiff);
            }
        }
    }

    private void checkCadence(int current_cadence) {

        if(current_cadence > cadenceRecord){                          // Set new record values
            cadenceRecord = current_cadence;
            cadenceRecordDate = sdf.format(new Date());               // Get date of record
            Log.i(TAG, "New cadence record:" + cadenceRecord);
            Log.i(TAG, "Date changed: " + cadenceRecordDate);

            recordChanged = true;

            if (cadenceLevelIndex + 1 <= cadenceAchievementLevels.length) {

                if(cadenceRecord >= cadenceAchievementLevels[cadenceLevelIndex+1]){
                    cadenceLevelIndex++;
                    Log.d(TAG,"cadence level: " + cadenceAchievementLevels[cadenceLevelIndex]);

                    if (speechOutputEnabled) {
                        tts.speak("New cadence achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }
                cadenceDiff = cadenceAchievementLevels[cadenceLevelIndex + 1] - cadenceRecord;
                Log.d(TAG,"cadence diff: " + cadenceDiff);
            }
        }
    }


    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    private void stopTimer() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    private void startTimer() {

        timer = new Timer();
        workoutStartTime = System.currentTimeMillis();

        initializeTimerTask();
        // TimerTask starts immediately, repeats every second
        timer.schedule(timerTask, 100, 1000);

    }


    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run the plots
                handler.post(new Runnable() {
                    public void run() {

                        workoutCurrentTime = System.currentTimeMillis();
                        long time_diff = workoutCurrentTime - workoutStartTime;

                        //hh:mm:ss
                        workoutRunningTime = String.format("%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(time_diff),
                        TimeUnit.MILLISECONDS.toMinutes(time_diff) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time_diff)),
                        TimeUnit.MILLISECONDS.toSeconds(time_diff) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time_diff)));

                        sendMsgString(GuideMessages.GUIDE_TIME, workoutRunningTime);

                        Log.v(TAG, workoutRunningTime);

                    }
                });
            }
        };
    }

}
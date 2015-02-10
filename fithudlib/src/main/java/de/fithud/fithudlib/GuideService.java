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
    private static int speed_low_min = 10;
    private static int speed_low_max = 15;
    private static int speed_high_min = 15;
    private static int speed_high_max = 20;
    private static long intervalTime = 10000;      // Duration of an interval in mSec

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

    public static int speedRecord = 0;
    public static int heightRecord = 0;
    public static int cadenceRecord = 0;
    public static int distanceRecord = 0;
    public static int caloriesRecord = 0;

    public static String speedRecordDate;
    public static String heightRecordDate;
    public static String cadenceRecordDate;

    private static String workoutRunningTime;
    private long workoutStartTime;
    private long workoutCurrentTime;

    public static int[] speedAchievementLevels = new int[]{0, 20, 25, 30, 50, 60, 70, 80};
    public static int[] distanceAchievementLevels = new int[]{0, 1, 2, 5, 10, 20};
    public static int[] heightAchievementLevels = new int[]{0, 100, 500, 1000, 1500};
    public static int[] cadenceAchievementLevels = new int[]{0, 70, 80, 120, 130};
    public static int[] caloriesAchievementLevels = new int[]{0, 300, 600, 1000};

    // Memory for plotting values (They are readout in onCreate of "showPlots")
    private static final int History_Size = 50;
    public static List<Float> cadenceHistory = new ArrayList<Float>();
    public static List<Float> heightHistory = new ArrayList<Float>();
    public static List<Float> respirationHistory = new ArrayList<Float>();
    public static List<Float> speedHistory = new ArrayList<Float>();
    public static List<Float> heartHistory = new ArrayList<Float>();
    private static  int heightCounter = 0;
    // This defines which values are added to the height history
    // Example: HEIGHT_DIVIDER = 20 --> 3samples/min because sampling rate is 1Hz
    // If History == 50 --> History holds 50/3 min of data = 16.6 min
    public static final int HEIGHT_DIVIDER = 10;

    public static int speedLevelIndex = 0;
    public static int heightLevelIndex = 0;
    public static int cadenceLevelIndex = 0;
    public static int distanceLevelIndex = 0;
    public static int caloriesLevelIndex = 0;

    public static int speedDiff = 0;
    public static int heightDiff = 0;
    public static int cadenceDiff = 0;

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
        public static final int ACHIEVEMENT_REACHED = 37;
        public static final int ACHIEVEMENT_HEIGHT = 38;
    }


    @Override
    public void handleMessage(Message msg) {

        float value;

        switch (msg.what) {
            case GuideService.GuideMessages.GUIDE_COMMAND:
                //updateTrainingMode(training_mode);
                guide_active = msg.getData().getBoolean("guideActive");
                Log.i(TAG, "Guide active: " + msg.getData().getBoolean("guideActive"));
                if (speech_active) {
                    if (guide_active) {
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

                if (speech_active) {
                    if (workout_started) {
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
                Log.i(TAG, "Training mode changed." + training_mode);
                if (speech_active) {
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
                Log.i(TAG, "Challenge mode changed." + challenge_mode);
                if (speech_active) {
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
                Log.i(TAG, "Speech active: " + speech_active);
                if (speech_active) {
                    tts.speak("Speech enabled", TextToSpeech.QUEUE_FLUSH, null);
                }
                break;

            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "HR received" + value);

                if (workout_started && guide_active && training_mode < 2) {  // Cardio & Fatburn training
                    heartRateCheck((int) value);
                }

                // Adding to plotting history
                if (heartHistory.size() > History_Size) {
                    heartHistory.remove(0);
                }
                heartHistory.add(value);
                break;

            case FHSensorManager.Messages.CALORIES_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "Calories received" + value);

                if (workout_started) {
                    if(guide_active && challenge_mode == 2){         // Calories challenge
                        caloriesCheck((int) value);
                    }
                    achievementCaloriesCheck((int)value);
                }

                break;

            case FHSensorManager.Messages.CADENCE_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "cadence received" + value);

                if (workout_started) {
                    if(guide_active && (challenge_mode == 1)){      // Cadence challenge
                        cadenceCheck(value);
                    }
                    achievementCadenceCheck((int)value);
                }

                // Adding to plotting history
                if (cadenceHistory.size() > History_Size) {
                    cadenceHistory.remove(0);
                }
                cadenceHistory.add(value);
                break;

            case FHSensorManager.Messages.SPEED_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "speed received" + value);

                if (workout_started) {

                    // Interval training mode
                    if (guide_active && (training_mode == 2)) {
                        speedCheck(value);
                    }
                    achievementSpeedCheck((int) value);
                }

                // Adding to plotting history
                if (speedHistory.size() > History_Size) {
                    speedHistory.remove(0);
                }
                speedHistory.add(value);
                break;

            case FHSensorManager.Messages.HEIGTH_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "height received" + value);

                if (workout_started) {

                    // Height challenge
                    if (guide_active && (challenge_mode == 0)) {
                        heightCheck((int) value);
                    }
                    achievementHeightCheck((int) value);
                }

                heightCounter = heightCounter + 1;
                if (heightCounter == HEIGHT_DIVIDER) {
                    // Adding to plotting history
                    if (heightHistory.size() > History_Size) {
                        heightHistory.remove(0);
                    }
                    heightHistory.add(value);
                    heightCounter = 0;
                }
                break;
            case FHSensorManager.Messages.DISTANCE_MESSAGE:
                value = msg.getData().getFloat("value");
                Log.v(TAG, "Distance received" + value);

                if (workout_started){
                    achievementDistanceCheck((int)value);
                }
                break;

        }
    }

    public void updateTrainingMode(int training_mode) {
        // TODO: Set min/max heart rate for cardio training
        if (training_mode == cardio) {
            hr_min = 120;
            hr_max = 130;
        }
        // TODO: Set min/max heart rate for fatburn training
        if (training_mode == fatburn) {
            hr_min = 90;
            hr_max = 100;
        }

        if (training_mode == interval) {
            startTime = System.currentTimeMillis();
        }
    }

    private void heartRateCheck(int heartRate) {
        GuideTextPrev = GuideText;
        if (heartRate < hr_min) {
            GuideText = "HR low!";
        } else if (heartRate < hr_max) {
            GuideText = "HR ok!";
        } else {
            GuideText = "HR high!";
        }

        if (summaryBoundToGuide && GuideTextPrev != GuideText) {
            sendMsgString(GuideMessages.GUIDE_TEXT, GuideText);
            if (speech_active) {
                tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }


    private void speedCheck(float current_speed) {
        GuideTextPrev = GuideText;
        long currentTime = System.currentTimeMillis();
        if ((currentTime - startTime) >= intervalTime) {
            interval_state = !interval_state;
            startTime = System.currentTimeMillis();
        }
        if (interval_state) {
            if (current_speed < speed_high_min) {
                GuideText = "Go faster!";
            } else if (current_speed < speed_high_max) {
                GuideText = "Perfect pace!";
            } else {
                GuideText = "Slow down!";
            }
        } else {
            if (current_speed < speed_low_min) {
                GuideText = "Go faster!";
            } else if (current_speed < speed_low_max) {
                GuideText = "Perfect pace!";
            } else {
                GuideText = "Slow down!";
            }
        }
        Log.d(TAG, "Current Guide Text: " + GuideText);
        if (summaryBoundToGuide && GuideTextPrev != GuideText) {
            sendMsgString(GuideMessages.GUIDE_TEXT, GuideText);
            if (speech_active) {
                tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    ;


    private void cadenceCheck(float current_cadence) {
        GuideTextPrev = GuideText;
        if (current_cadence > 0 && progressIndex == 0) {
            GuideText = "Let's get started.";
            progressIndex++;
        } else if (current_cadence >= (0.5 * cadenceAim) && progressIndex == 1) {
            GuideText = "You are half way there.";
            progressIndex++;
        } else if (current_cadence >= (0.8 * cadenceAim) && progressIndex == 2) {
            GuideText = "Your are almost there";
            progressIndex++;
        } else if (current_cadence >= cadenceAim && progressIndex == 3) {
            GuideText = "Congratulations! Challenge completed";
            progressIndex++;
        } else {
            GuideText = "Keep going.";
        }
        Log.v(TAG, "Guide: " + GuideText);
        if (summaryBoundToGuide && GuideTextPrev != GuideText) {
            sendMsgString(GuideMessages.GUIDE_TEXT, GuideText);
            if (speech_active) {
                tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }



    public void heightCheck(int current_height) {
        GuideTextPrev = GuideText;
        if (current_height > 0 && progressIndex == 0) {
            GuideText = "Let's get started.";
            progressIndex++;
        } else if (current_height >= (0.5 * heightAim) && progressIndex == 1) {
            GuideText = "You are half way there.";
            progressIndex++;
        } else if (current_height >= (0.8 * heightAim) && progressIndex == 2) {
            GuideText = "Your are almost there";
            progressIndex++;
        } else if (current_height >= (0.9 * heightAim) && progressIndex == 3) {
            GuideText = "Only a few meters left";
            progressIndex++;
        } else if (current_height >= heightAim && progressIndex == 4) {
            GuideText = "Congratulations! Challenge completed";
            progressIndex++;
        }

        if (summaryBoundToGuide && GuideTextPrev != GuideText) {
            sendMsgString(GuideMessages.GUIDE_TEXT, GuideText);
            if (speech_active) {
                tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    ;

    public void caloriesCheck(int current_calories) {
        GuideTextPrev = GuideText;
        if (current_calories > 0 && progressIndex == 0) {
            GuideText = "Let's get started.";
            progressIndex++;
        } else if (current_calories >= (0.5 * caloriesAim) && progressIndex == 1) {
            GuideText = "You are half way there.";
            progressIndex++;
        } else if (current_calories >= (0.8 * caloriesAim) && progressIndex == 2) {
            GuideText = "Your are almost there";
            progressIndex++;
        } else if (current_calories >= (0.9 * caloriesAim) && progressIndex == 3) {
            GuideText = "Only a few more calories";
            progressIndex++;
        } else if (current_calories >= caloriesAim && progressIndex == 4) {
            GuideText = "Congratulations! Challenge completed";
            progressIndex++;
        }

        if (summaryBoundToGuide && GuideTextPrev != GuideText) {
            sendMsgString(GuideMessages.GUIDE_TEXT, GuideText);
            if (speech_active) {
                tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
        /*if(speech_active && speechCounter == speechPeriod){
            tts.speak(GuideText, TextToSpeech.QUEUE_FLUSH, null);
            speechCounter = 0;
        }
        speechCounter++;*/
    }

    ;

    @Override
    public void onCreate() {
        super.onCreate();
        conn = new MessengerConnection(this);
        conn.connect(FHSensorManager.class);
        tts = new TextToSpeech(this, this);

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
        if (tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTS Destroyed");
        }

        super.onDestroy();
    }

    void sendMsgString(int messageType, String content) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                Message msg = Message.obtain(null, messageType);
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

    private void achievementSpeedCheck(int current_speed) {

        if (current_speed > speedRecord) {                            // Set new record values
            speedRecord = current_speed;
            speedRecordDate = sdf.format(new Date());               // Get date of record
            Log.i(TAG, "New speed record:" + speedRecord);
            Log.i(TAG, "Date changed: " + speedRecordDate);

            if (speedLevelIndex + 2 <= speedAchievementLevels.length) {

                if (speedRecord >= speedAchievementLevels[speedLevelIndex + 1]) {
                    speedLevelIndex++;
                    Log.d(TAG, "Speed level: " + speedAchievementLevels[speedLevelIndex]);

                    if (speechOutputEnabled) {
                        tts.speak("New speed achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }

                    // ToDo receive and act accordingly in Achievements.java
                    // ZU SENDEN: speedLevelIndex, speedAchievementLevels[speedLevelIndex], speedAchievementLevels[speedLevelIndex + 1], speedAchievementLevels.length
                    sendMsgString(GuideMessages.ACHIEVEMENT_REACHED, "text");

                    Toast.makeText(this, "Speed record: " + speedAchievementLevels[speedLevelIndex] + "km/h", Toast.LENGTH_LONG).show();
                    showThumb();
                }
            }
        }
    }

    private void achievementCaloriesCheck(int current_calories) {

        if (current_calories > caloriesRecord) {                            // Set new record values
            caloriesRecord = current_calories;
            Log.i(TAG, "New calories record:" + caloriesRecord);

            if (caloriesLevelIndex + 2 <= caloriesAchievementLevels.length) {

                if (caloriesRecord >= caloriesAchievementLevels[caloriesLevelIndex + 1]) {
                    caloriesLevelIndex++;

                    if (speechOutputEnabled) {
                        tts.speak("New calories achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }

                    sendMsgString(GuideMessages.ACHIEVEMENT_REACHED, "text");
                    Toast.makeText(this, "Calories record: " + caloriesAchievementLevels[caloriesLevelIndex] + "kCal", Toast.LENGTH_LONG).show();
                    showThumb();
                }
            }
        }
    }

    private void achievementDistanceCheck(int current_distance) {

        if (current_distance > distanceRecord) {                    // Set new record values
            distanceRecord = current_distance;
            Log.i(TAG, "New distance record:" + speedRecord);

            if (distanceLevelIndex + 2 <= distanceAchievementLevels.length) {

                if (distanceRecord >= distanceAchievementLevels[distanceLevelIndex + 1]) {
                    distanceLevelIndex++;

                    if (speechOutputEnabled) {
                        tts.speak("New distance achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }

                    sendMsgString(GuideMessages.ACHIEVEMENT_REACHED, "text");
                    Toast.makeText(this, "Distance record: " + distanceAchievementLevels[distanceLevelIndex] + " km", Toast.LENGTH_LONG).show();
                    showThumb();
                }
            }
        }
    }

    private void achievementHeightCheck(int current_height) {

        if (current_height > heightRecord) {                            // Set new record values
            heightRecord = current_height;
            Log.i(TAG, "New height record:" + heightRecord);

            if (heightLevelIndex + 2 <= heightAchievementLevels.length) {

                if (heightRecord >= heightAchievementLevels[heightLevelIndex + 1]) {
                    heightLevelIndex++;

                    if (speechOutputEnabled) {
                        tts.speak("New height achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }
                    Toast.makeText(this, "Height record: " + heightAchievementLevels[heightLevelIndex + 1] + "m", Toast.LENGTH_LONG).show();
                    showThumb();
                }
            }
        }
    }

    private void achievementCadenceCheck(int current_cadence) {

        if (current_cadence > cadenceRecord) {                          // Set new record values
            cadenceRecord = current_cadence;
            cadenceRecordDate = sdf.format(new Date());                 // Get date of record
            Log.i(TAG, "New cadence record:" + cadenceRecord);
            Log.i(TAG, "Date changed: " + cadenceRecordDate);

            if (cadenceLevelIndex + 2 <= cadenceAchievementLevels.length) {

                if (cadenceRecord >= cadenceAchievementLevels[cadenceLevelIndex + 1]) {
                    cadenceLevelIndex++;
                    Log.d(TAG, "cadence level: " + cadenceAchievementLevels[cadenceLevelIndex]);

                    if (speechOutputEnabled) {
                        tts.speak("New cadence achievement unlocked", TextToSpeech.QUEUE_FLUSH, null);
                    }
                    Toast.makeText(this, "Cadence record: " + cadenceAchievementLevels[cadenceLevelIndex + 1] + "rpm", Toast.LENGTH_LONG).show();
                    showThumb();
                }
            }
        }
    }


    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    public void sendDataToSensormanager(int[] data) {
        Message msg = Message.obtain(null, 4);
        Bundle bundle = new Bundle();
        bundle.putIntArray("command", data);
        msg.setData(bundle);
        try {
            conn.send(msg);
        } catch (RemoteException e) {
        }
    }

    public void showThumb() {
        int[] test = new int[2];
        test[0] = FHSensorManager.Commands.SHOW_THUMB;
        test[1] = 0;
        sendDataToSensormanager(test);
    }

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
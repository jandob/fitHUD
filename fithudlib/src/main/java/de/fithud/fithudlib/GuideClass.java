package de.fithud.fithudlib;

import android.os.Handler;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */

public class GuideClass {
    private static final int VALUE_LOW = 0;
    private static final int VALUE_OK = 1;
    private static final int VALUE_HIGH = 2;
    private final String TAG = "GuideService";

    private static final int cardio = 0;
    private static final int fatburn = 1;
    private static final int interval = 2;
    private static final int recreation = 3;

    private static int hr_min = 0;
    private static int hr_max = 0;
    private static int speed_low_min = 80;
    private static int speed_low_max = 90;
    private static int speed_high_min = 90;
    private static int speed_high_max = 100;
    private static int cadence_min = 0;
    private static int cadence_max = 0;
    private static int height_min = 0;
    private static int height_max = 0;
    private static long intervalTime = 5000;      // Duration of an interval in mSec

    private static boolean interval_state = false;
    private static long startTime = 0;



    public static void updateTrainingMode(int training_mode){
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

    public static void updateChallengeMode(int challenge_mode) {

    }


    public static int heartRateCheck(int heartRate) {
        if(heartRate < hr_min){
            return VALUE_LOW;
        } else if (heartRate < hr_max) {
            return VALUE_OK;
        } else {
            return VALUE_HIGH;
        }
    };

    public static int speedCheck(float current_speed) {

        long currentTime = System.currentTimeMillis();
        if((currentTime - startTime) >= intervalTime) {
            interval_state = !interval_state;
            startTime = System.currentTimeMillis();
        }
        if (interval_state){
            if(current_speed < speed_high_min) {
                return VALUE_LOW;
            } else if (current_speed < speed_high_max) {
                return VALUE_OK;
            } else {
                return VALUE_HIGH;
            }
        } else {
            if(current_speed < speed_low_min) {
                return VALUE_LOW;
            } else if (current_speed < speed_low_max) {
                return VALUE_OK;
            } else {
                return VALUE_HIGH;
            }
        }
    };


    private static final int heightAim = 100;       // Height difference in meters
    private static final int cadenceAim = 120;      // Cadence in rotations per minute
    private static final int caloriesAim = 100;

    private static String challengeText;
    private static int progressIndex = 0;
    private static boolean challengeStarted = true;

    // Need to reset all flags if new challenge started.
    public static int cadenceCheck(float current_cadence) {

        if(current_cadence > 0 && progressIndex == 0){
            challengeText = "Let's get started.";
            progressIndex++;
        } else if(current_cadence >= (0.5 * cadenceAim) && progressIndex == 1) {
            challengeText = "You are half way there.";
            progressIndex++;
        } else if(current_cadence >= (0.8 * cadenceAim) && progressIndex == 2) {
            challengeText = "Your are almost there";
            progressIndex++;
        } else if(current_cadence >= (0.9 * cadenceAim) && progressIndex == 3) {
            challengeText = "Only a few meters left";
            progressIndex++;
        } else if(current_cadence >= cadenceAim && progressIndex == 4){
            challengeText = "Congratulations! You have your challenge succesfully completed";
            progressIndex++;
        }

        return 0;
    };

    public static int heightCheck(int current_height) {

        if(current_height > 0 && progressIndex == 0){
            challengeText = "Let's get started.";
            progressIndex++;
        } else if(current_height >= (0.5 * heightAim) && progressIndex == 1) {
            challengeText = "You are half way there.";
            progressIndex++;
        } else if(current_height >= (0.8 * heightAim) && progressIndex == 2) {
            challengeText = "Your are almost there";
            progressIndex++;
        } else if(current_height >= (0.9 * heightAim) && progressIndex == 3) {
            challengeText = "Only a few meters left";
            progressIndex++;
        } else if(current_height >= heightAim && progressIndex == 4){
            challengeText = "Congratulations! You have your challenge succesfully completed";
            progressIndex++;
        }

        return 0;
    };

    public static int caloriesCheck(int current_calories) {

        if(current_calories > 0 && progressIndex == 0){
            challengeText = "Let's get started.";
            progressIndex++;
        } else if(current_calories >= (0.5 * caloriesAim) && progressIndex == 1) {
            challengeText = "You are half way there.";
            progressIndex++;
        } else if(current_calories >= (0.8 * caloriesAim) && progressIndex == 2) {
            challengeText = "Your are almost there";
            progressIndex++;
        } else if(current_calories >= (0.9 * caloriesAim) && progressIndex == 3) {
            challengeText = "Only a few more calories";
            progressIndex++;
        } else if(current_calories >= caloriesAim && progressIndex == 4){
            challengeText = "Congratulations! You have your challenge succesfully completed";
            progressIndex++;
        }
        return 0;
    };


}

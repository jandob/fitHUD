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
        if(training_mode == cardio){
            hr_min = 80;
            hr_max = 90;
        }
        if(training_mode == fatburn){
            hr_min = 90;
            hr_max = 100;
        }
        if(training_mode == interval){
            startTime = System.currentTimeMillis();
        }
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

    public static int speedCheck(int current_speed) {

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


/*
    public static int cadenceCheck(int current_cadence) {

    }

    public static int heightCheck(int current_height) {

    }*/


}

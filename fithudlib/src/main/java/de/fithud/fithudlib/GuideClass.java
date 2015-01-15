package de.fithud.fithudlib;

/**
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */

public class GuideClass {
    private static final int Heart_Rate_Too_Low = 0;
    private static final int Heart_Rate_OK = 1;
    private static final int Heart_Rate_Too_High = 2;
    private final String TAG = "GuideService";
    public static int heartRateCheck(int heartRate) {
        if(heartRate < 90){
            return Heart_Rate_Too_Low;
        } else if (heartRate < 100) {
            return Heart_Rate_OK;
        } else {
            return Heart_Rate_Too_High;
        }
    };
}

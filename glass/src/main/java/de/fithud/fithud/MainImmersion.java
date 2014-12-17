package de.fithud.fithud;

import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;

import java.util.Calendar;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.MessengerServiceActivity;
import de.fithud.fithudlib.TestService;
import de.fithud.fithudlib.UpdateListener;

/**
 * An {@link android.app.Activity} showing a tuggable "Hello World!" card.
 * <p/>
 * The main content view is composed of a one-card {@link com.google.android.glass.widget.CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class MainImmersion extends MessengerServiceActivity {
    /**
     * {@link com.google.android.glass.widget.CardScrollView} to use as the main content view.
     */
    private CardScrollView mCardScroller;
    CardBuilder mCard;
    private final String TAG = "MainImmersion";
    private View mView;

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handling Msg");
        switch(msg.what) {
            case TestService.Messages.SENSOR_MESSAGE:
                Log.i(TAG, "handling Msg: case");
                float val = msg.getData().getFloat("HeartRate");
                mCard.setText(Float.toString(val));
                setContentView(mCard.getView());
                mCardScroller.getAdapter().notifyDataSetChanged();


                Log.i(TAG, "handling Msg:text set to " + val);
                break;
        }
    }

    private GestureDetector mGestureDetector;

    //private Intent intent = new Intent(this, FHLiveCardService.class);

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu){
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    public void findDevelopers(String platform){
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.find_android:
                    findDevelopers("Show Speed");
                    Log.i("hans", "hans");
                    break;
                case R.id.find_javascript:
                    findDevelopers("Show heart rate");
                    break;
                case R.id.find_ios:
                    findDevelopers("Show whatever");
                    startService(new Intent(this, FHLiveCardService.class));
                    break;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        doBindService(FHSensorManager.class);
        Log.i("MainImmersion", "on start");
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        super.onCreate(bundle);

        mView = buildView();

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return mView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mView;
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });
        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Plays disallowed sound to indicate that TAP actions are not supported.
                //AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                //am.playSoundEffect(Sounds.DISALLOWED);
                openOptionsMenu();
            }
        });

        setContentView(mCardScroller);
        // T: start Live Card directly for testing
        //startService(LiveCardServiceIntent);
    }

    // T: destroy liveCardService, when Activity is destroyed.
   // @Override
    //protected void onDestroy(){
     //   stopService(LiveCardServiceIntent);
    //}

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link com.google.android.glass.widget.CardBuilder} class.
     */
    private View buildView() {
        mCard = new CardBuilder(this, CardBuilder.Layout.TEXT);
        mCard.setText(Float.toString(9));
        return mCard.getView();
    }

    @Override
    protected void onDestroy() {
        doUnbindService();
    }
}

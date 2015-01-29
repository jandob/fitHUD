package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.content.Intent;
import android.media.AudioManager;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

import de.fithud.fithudlib.GuideService;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;

/**
 * Created by JohanV on 04.01.2015.
 */
public class WorkoutMenu extends Activity implements MessengerClient{

    private static final String TAG = WorkoutMenu.class.getSimpleName();

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;
    MessengerConnection guideConn = new MessengerConnection(this);

    private static boolean workoutActive = false;

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.workoutmenu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.startStop:
                    mCardScrollView.setSelection(0);
                    break;
                case R.id.guideSettings:
                    //mCardScrollView.setSelection(1);
                    startActivity(new Intent(WorkoutMenu.this, GuideSettings.class));
                    break;
                case R.id.showSummary:
                    startActivity(new Intent(WorkoutMenu.this, SummaryView.class));
                    //startService(new Intent(this, FHLiveCardService.class));
                    break;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public void startStopWorkout(){
        // Start workout here ! Therefore communicate with livecard
        if(!workoutActive) {
            workoutActive = true;
            mCards.get(0).setText("Workout running");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Activating workout...");

        }
        else
        {
            mCards.get(0).setText("Workout inactive");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Deactivating workout...");
            workoutActive = false;
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {

        guideConn.connect(GuideService.class);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        super.onCreate(bundle);
        createCards();

        mCardScrollView = new CardScrollView(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // Handle the TAP event.
        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mAudioManager.playSoundEffect(Sounds.TAP);
                switch (mCardScrollView.getSelectedItemPosition()) {
                    case 0:
                        startActivity(new Intent(WorkoutMenu.this, GuideSettings.class));
                        break;

                    case 1:
                        startStopWorkout();
                        sendBoolToGuide(GuideService.GuideMessages.WORKOUT_COMMAND, workoutActive);
                        break;

                }
            }
        });

        mCardScrollView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                mAudioManager.playSoundEffect(Sounds.DISMISSED);
                switch (mCardScrollView.getSelectedItemPosition()) {
                    case 0:                     //Guide
                        startActivity(new Intent(WorkoutMenu.this, SummaryView.class));
                        break;
                }
                return true;
            }
        });

        mAdapter = new CardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);
        mCardScrollView.setSelection(1);
    }

    @Override
    protected void onResume() {
        if(workoutActive) {
            mCards.get(1).setText("Workout active");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Workout is active...");
        }
        else{
            mCards.get(1).setText("Workout inactive");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Workout is inactive...");
        }

        super.onResume();
        mCardScrollView.activate();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        mCardScrollView.deactivate();
        super.onPause();
    }

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Guide Settings")
                .setFootnote("Start or set the guide"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Start/Stop!")
                .setFootnote("Start or stop the workout"));
    }

    @Override
    public void handleMessage(Message msg) {

    }

    public void sendBoolToGuide(int messageType, boolean guideActive) {
        Message msg = Message.obtain(null, messageType);
        Bundle bundle = new Bundle();
        bundle.putBoolean("workoutActive", guideActive);
        msg.setData(bundle);
        Log.d(TAG, "Data has been sent to guide.");
        try {
            guideConn.send(msg);
        }
        catch (RemoteException e){

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


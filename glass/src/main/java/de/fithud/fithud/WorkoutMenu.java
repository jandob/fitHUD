package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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

import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;
import de.fithud.fithudlib.StorageService;

/**
 * Created by JohanV on 04.01.2015.
 */
public class WorkoutMenu extends Activity {

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;
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
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public void startStopWorkout(){
        // Start workout here ! Therefore communicate with livecard
        if(!workoutActive) {
            workoutActive = true;
            mCards.get(0).setText("Workout active");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Activating workout...");
            startService(new Intent(WorkoutMenu.this, StorageService.class));
        }
        else
        {
            mCards.get(0).setText("Workout inactive");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Deactivating workout...");
            workoutActive = false;
            stopService(new Intent(WorkoutMenu.this, StorageService.class));
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
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
                        startStopWorkout();
                        break;

                    case 1:
                        startActivity(new Intent(WorkoutMenu.this, GuideSettings.class));
                        break;
                }
            }
        });

        mAdapter = new CardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);
    }

    @Override
    protected void onResume() {
        if(workoutActive) {
            mCards.get(0).setText("Workout active");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Workout is active...");
        }
        else{
            mCards.get(0).setText("Workout inactive");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Workzt is inactive...");
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
                .setText("Start/Stop!")
                .setFootnote("Start or stop the workout"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Guide Settings")
                .setFootnote("Start or set the guide"));
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


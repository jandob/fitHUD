package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
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
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;
import java.util.Locale;
import android.speech.tts.TextToSpeech;

import org.apache.http.auth.MalformedChallengeException;

import java.util.ArrayList;
import java.util.List;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.GuideService;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;


/**
 * Created by JohanV on 04.01.2015.
 */
public class GuideSettings extends Activity implements MessengerClient {

    MessengerConnection sensorConn = new MessengerConnection(this);
    MessengerConnection guideConn = new MessengerConnection(this);

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;

    private final String TAG = GuideSettings.class.getSimpleName();

    private static boolean speechEnabled = false;
    private static int speech_enabled = 0;
    private static boolean guide_active = false;


    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.guidesettingsmenu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId == Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.startstopguide:
                    mCardScrollView.setSelection(0);
                    break;
                case R.id.fatcardio:
                    mCardScrollView.setSelection(1);
                    break;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public void sendBoolToGuide(int messageType, boolean guideActive) {
        Message msg = Message.obtain(null, messageType);
        Bundle bundle = new Bundle();
        bundle.putBoolean("guideActive", guideActive);
        //bundle.putIntArray("command", data);
        msg.setData(bundle);
        Log.d(TAG, "Data has been sent to guide.");
        try {
            guideConn.send(msg);
        }
        catch (RemoteException e){

        }
    }


    // This function is called with clicking on the first card in GuideSettings
    public void startStopGuide(){

        if(!guide_active) {

            mCards.get(0).setText("Guide is activated");
            mAdapter.notifyDataSetChanged();
            Log.v(TAG,"Guide activated");
            guide_active = true;
        }
        else {
            mCards.get(0).setText("Guide is deactivated");
            mAdapter.notifyDataSetChanged();
            Log.v(TAG, "Guide deactivated");
            guide_active = false;
        }

        sendBoolToGuide(GuideService.GuideMessages.GUIDE_COMMAND, guide_active);
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
                    case 0:                     //Guide
                        startStopGuide();
                        break;
                    case 1:                     // Start training mode
                        startActivity(new Intent(GuideSettings.this, TrainingMode.class));
                        break;
                    case 2:                     // Start challenge
                        startActivity(new Intent(GuideSettings.this, ChallengeMode.class));
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
    protected void onDestroy() {
        sensorConn.disconnect();
        //guideConn.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if(guide_active) {
            mCards.get(0).setText("Guide is activated");
            mAdapter.notifyDataSetChanged();
            Log.d(TAG, "Guide is activated...");
        }
        else {
            mCards.get(0).setText("Guide is deactivated");
            mAdapter.notifyDataSetChanged();
            Log.d(TAG, "Guide is deactivated...");
        }

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

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Start Guide!")
                .setFootnote("Tap to start or stop the guide"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Training mode")
                .setFootnote("Tap to choose your training mode"));

        //TODO: Implement challenges.
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Challenge yourself!")
                .setFootnote("Tap to select your challenge"));

    }

    @Override
    public void handleMessage(Message msg) {

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


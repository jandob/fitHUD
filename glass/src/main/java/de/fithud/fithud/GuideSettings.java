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
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;


/**
 * Created by JohanV on 04.01.2015.
 */
public class GuideSettings extends Activity implements TextToSpeech.OnInitListener, MessengerClient {

    MessengerConnection conn = new MessengerConnection(this);
    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;

    private TextToSpeech tts;
    private String speech_text = "test";

    private final String TAG = "GuideSettings";
    private static boolean speechEnabled = false;

    // Setting variables
    private static int speech_enabled = 0;
    private static int guide_active = 0;
    private static int training_mode = 0;



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

    public void sendDataToSensormanager(int[] data) {
        Message msg = Message.obtain(null, 4);
        Bundle bundle = new Bundle();
        bundle.putIntArray("command", data);
        msg.setData(bundle);

        try {
            conn.send(msg);
        }
        catch (RemoteException e){

        }
    }

    // This function is called with clicking on the first card in GuideSettings
    public void startStopGuide(){
        // Communicate with live card here !!
        if(guide_active == 0) {

            mCards.get(0).setText("Guide is activated");
            mAdapter.notifyDataSetChanged();
            Log.d(TAG,"Guide activating...");
            speech_text = "Guide is now activated";
            guide_active = 1;
        }
        else {
            mCards.get(0).setText("Guide is deactivated");
            mAdapter.notifyDataSetChanged();
            Log.d(TAG, "Guide deactivating...");
            speech_text = "Guide is now deactivated";
            guide_active = 0;
        }
        int[] command = new int[2];
        command[0] = FHSensorManager.Commands.GUIDE_COMMAND;
        command[1] = guide_active;
        sendDataToSensormanager(command);

        if(speech_enabled == 1) {
            tts.speak(speech_text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }


/*
    public void speechSupportSwitch() {

        if(speech_enabled == 0) {
            Log.d(TAG,"Speech support turned on...");
            mCards.get(2).setText("Speech enabled");
            mAdapter.notifyDataSetChanged();
            speech_text = "Speech output now enabled";
            tts.speak(speech_text, TextToSpeech.QUEUE_FLUSH, null);
            speech_enabled = 1;
        }
        else {
            Log.d(TAG,"Speech support turned off...");
            mCards.get(2).setText("Speech disabled");
            mAdapter.notifyDataSetChanged();
            speech_enabled = 0;
        }
        int[] command = new int[2];
        command[0] = FHSensorManager.Commands.SPEECH_COMMAND;
        command[1] = speech_enabled;
        sendDataToSensormanager(command);
    }*/


    @Override
    protected void onCreate(Bundle bundle) {
        conn.connect(FHSensorManager.class);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        super.onCreate(bundle);
        createCards();

        tts = new TextToSpeech(this,this);

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
                        //startActivity(new Intent(GuideSettings.this, ));
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
        if(guide_active == 1) {
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
                .setFootnote("Start or stop the guide"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Training mode")
                .setFootnote("Tap to choose your training mode"));

        //TODO: Implement challenges.
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Challenge yourself!")
                .setFootnote("Tap to select your challenge"));

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
    public void handleMessage(Message msg) {

        int [] command = msg.getData().getIntArray("command");
        Log.i(TAG, "handling Msg");
/*
        switch (command[0]) {
            case FHSensorManager.Commands.SPEECH_COMMAND:
                if (command[1] == 1) speechEnabled = true; speechEnabled = false;
                break;
        }
        */
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


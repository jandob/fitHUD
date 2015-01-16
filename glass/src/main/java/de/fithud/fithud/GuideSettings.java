package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;
import java.util.Locale;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by JohanV on 04.01.2015.
 */
public class GuideSettings extends Activity implements TextToSpeech.OnInitListener{

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;
    private TextToSpeech tts;
    private String speech_text = "test";

    private static boolean guideOnOff = false;
    private static boolean fatBurn = false;
    private static boolean speechOn = false;

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

    // This function is called with clicking on the first card in GuideSettings
    public void startStopGuide(){
        // Communicate with live card here !!
        if(!guideOnOff) {

            mCards.get(0).setText("Guide is activated");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD","Guide activating...");
            speech_text = "Guide is now activated";
            guideOnOff = true;
        }
        else {
            mCards.get(0).setText("Guide is deactivated");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD","Guide deactivating...");
            speech_text = "Guide is now deactivated";
            guideOnOff = false;
        }
        tts.speak(speech_text, TextToSpeech.QUEUE_FLUSH, null);
    }

    // This function is called with clicking on the second card in GuideSettings
    public void fatCardioSwitch(){
        // Change here the style of the guide!!
        if(!fatBurn) {
            Log.d("FitHUD","Selecting fat-burn...");
            mCards.get(1).setText("Fat-Burn");
            mAdapter.notifyDataSetChanged();
            speech_text = "Fat burn training selected";
            fatBurn = true;
        }
        else{
            Log.d("FitHUD","Selecting cardio...");
            mCards.get(1).setText("Cardio");
            mAdapter.notifyDataSetChanged();
            speech_text = "Cardio training selected";
            fatBurn = false;
        }
        tts.speak(speech_text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void speechSupportSwitch() {

        if(!speechOn) {
            Log.d("FitHUD","Speech support turned on...");
            mCards.get(2).setText("Speech enabled");
            mAdapter.notifyDataSetChanged();
            speech_text = "Speech output now enabled";
            tts.speak(speech_text, TextToSpeech.QUEUE_FLUSH, null);
            speechOn = true;
        }
        else {
            Log.d("FitHUD","Speech support turned off...");
            mCards.get(2).setText("Speech disabled");
            mAdapter.notifyDataSetChanged();
            speechOn = false;
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
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
                    case 0:
                        startStopGuide();
                        break;

                    case 1:
                        fatCardioSwitch();
                        break;
                    case 2:
                        speechSupportSwitch();
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
        if(guideOnOff) {
            mCards.get(0).setText("Guide is activated");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Guide is activated...");
        }
        else {
            mCards.get(0).setText("Guide is deactivated");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Guide is deactivated...");
        }

        if(fatBurn){
            mCards.get(1).setText("Fat-Burn");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Fatburn is activated...");
        }
        else{
            mCards.get(1).setText("Cardio");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD", "Cardio is activated...");
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
                .setText("Start/Stop Guide!")
                .setFootnote("Start or stop the guide"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Fat-Burn")
                .setFootnote("Guide setting for biking"));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Speech Settings.")
                .setFootnote("Activate speech support"));
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d("TTS", "This Language is not supported");
            } else {
                //speech_text = "Speech activated";
                //tts.speak(speech_text, TextToSpeech.QUEUE_FLUSH, null);
            }
        } else {
            Log.d("TTS", "Initilization Failed!");
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


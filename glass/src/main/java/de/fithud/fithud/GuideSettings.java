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


/**
 * Created by JohanV on 04.01.2015.
 */
public class GuideSettings extends Activity {

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;

    private static boolean guideOnOff = false;
    private static boolean fatBurn = false;

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
            guideOnOff = true;
        }
        else {
            mCards.get(0).setText("Guide is deactivated");
            mAdapter.notifyDataSetChanged();
            Log.d("FitHUD","Guide deactivating...");
            guideOnOff = false;
        }
    }

    // This function is called with clicking on the second card in GuideSettings
    public void fatCardioSwitch(){
        // Change here the style of the guide!!
        if(!fatBurn) {
            Log.d("FitHUD","Selecting fat-burn...");
            mCards.get(1).setText("Fat-Burn");
            mAdapter.notifyDataSetChanged();
            fatBurn = true;
        }
        else{
            Log.d("FitHUD","Selecting cardio...");
            mCards.get(1).setText("Cardio");
            mAdapter.notifyDataSetChanged();
            fatBurn = false;
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
                        startStopGuide();
                        break;

                    case 1:
                        fatCardioSwitch();
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


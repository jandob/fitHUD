package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;

/**
 * Created by Nikolas on 2015-01-17.
 */
public class ChallengeMode extends Activity implements MessengerClient{

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;
    private GestureDetector mGestureDetector;

    MessengerConnection conn = new MessengerConnection(this);
    private String TAG = "ChallengeMode";

    private static boolean speechEnabled = false;
    private static final int HEIGHT = 0;
    private static final int CADENCE = 1;
    private static final int CALORIES = 2;
    private static final int DISABLED = 4;

    private static int challengeMode = DISABLED;



    @Override
    protected void onCreate(Bundle bundle) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        conn.connect(FHSensorManager.class);
        super.onCreate(bundle);
        createCards();

        mCardScrollView = new CardScrollView(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mCardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //mCardScrollView.activate();
                mAudioManager.playSoundEffect(Sounds.TAP);
                switch (mCardScrollView.getSelectedItemPosition()) {
                    case 0:
                        challengeModeSwitch(HEIGHT);
                        break;
                    case 1:
                        challengeModeSwitch(CADENCE);
                        break;
                    case 2:
                        challengeModeSwitch(CALORIES);
                        break;
                }
            }
        });

        mAdapter = new CardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);
    }


    public void challengeModeSwitch(int challenge_mode){

        if(challenge_mode == HEIGHT) {
            mCards.get(0).setIcon(R.drawable.check_black);
            mCards.get(1).setIcon(R.drawable.empty);
            mCards.get(2).setIcon(R.drawable.empty);
            mAdapter.notifyDataSetChanged();
        }
        else if (challenge_mode == CADENCE){
            mCards.get(1).setIcon(R.drawable.check_black);
            mCards.get(0).setIcon(R.drawable.empty);
            mCards.get(2).setIcon(R.drawable.empty);
            mAdapter.notifyDataSetChanged();
        } else {
            mCards.get(2).setIcon(R.drawable.check_black);
            mCards.get(0).setIcon(R.drawable.empty);
            mCards.get(1).setIcon(R.drawable.empty);
            mAdapter.notifyDataSetChanged();
        }
        challengeMode = challenge_mode;
        int[] command = new int[2];
        command[0] = FHSensorManager.Commands.CHALLENGE_MODE_COMMAND;
        command[1] = challenge_mode;
        sendDataToSensormanager(command);

        if(speechEnabled) {
            //TODO: tts.speak(speech_text, TextToSpeech.QUEUE_FLUSH, null);
            Log.d(TAG, "Speech output");
        }
    }

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Height challenge!")
                .setIcon(R.drawable.empty)
                .setFootnote("Tap to set your challenge - swipe to see alternatives"));


        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Cadence challenge!")
                .setIcon(R.drawable.empty)
                .setFootnote("Tap to set your challenge - swipe to see alternatives"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Calories challenge!")
                .setIcon(R.drawable.empty)
                .setFootnote("Tap to set your challenge - swipe to see alternatives"));
    }

     @Override
    protected void onResume() {
        super.onResume();

        if(challengeMode == HEIGHT) {
            mCards.get(0).setIcon(R.drawable.check_black);
            mCards.get(1).setIcon(R.drawable.empty);
            mCards.get(2).setIcon(R.drawable.empty);
            mAdapter.notifyDataSetChanged();
        }
        else if (challengeMode == CADENCE){
            mCards.get(1).setIcon(R.drawable.check_black);
            mCards.get(0).setIcon(R.drawable.empty);
            mCards.get(2).setIcon(R.drawable.empty);
            mAdapter.notifyDataSetChanged();
        } else if (challengeMode == CALORIES){
            mCards.get(2).setIcon(R.drawable.check_black);
            mCards.get(0).setIcon(R.drawable.empty);
            mCards.get(1).setIcon(R.drawable.empty);
            mAdapter.notifyDataSetChanged();
        } else {
            mCards.get(2).setIcon(R.drawable.empty);
            mCards.get(0).setIcon(R.drawable.empty);
            mCards.get(1).setIcon(R.drawable.empty);
            mAdapter.notifyDataSetChanged();
        }

        mCardScrollView.activate();
    }

    @Override
    protected void onPause() {
        mCardScrollView.deactivate();
        super.onPause();
    }

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handling Msg");
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

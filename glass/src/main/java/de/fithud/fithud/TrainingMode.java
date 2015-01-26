package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
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
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.GuideService;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;

/**
 * Created by Nikolas on 2015-01-17.
 */
public class TrainingMode extends Activity implements MessengerClient{

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;
    private GestureDetector mGestureDetector;

    MessengerConnection conn = new MessengerConnection(this);
    private String TAG = "TrainingMode";
    private boolean cardScrollAdapterOn = true;

    private static boolean speechEnabled = false;
    private static final int CARDIO = 0;
    private static final int FATBURN = 1;
    private static final int INTERVAL = 2;
    private static final int DISABLED = 4;

    private static int trainingMode = DISABLED;



    @Override
    protected void onCreate(Bundle bundle) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        conn.connect(GuideService.class);
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
                        trainingModeSwitch(CARDIO);
                        Log.v(TAG, "training mode: " + trainingMode);
                        break;
                    case 1:
                        trainingModeSwitch(FATBURN);
                        Log.v(TAG, "training mode: " + trainingMode);
                        break;
                    case 2:
                        trainingModeSwitch(INTERVAL);
                        Log.v(TAG, "training mode: " + trainingMode);
                        break;
                }
            }
        });

        mGestureDetector = createGestureDetector(this);

        mAdapter = new CardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);
    }

    @Override
    protected void onDestroy() {
        conn.disconnect();
        super.onDestroy();
    }

    public void trainingModeSwitch(int training_mode){

        if(training_mode == CARDIO) {
            mCards.get(0).setIcon(R.drawable.check_black);
            mCards.get(1).setIcon(R.drawable.empty);
            mCards.get(2).setIcon(R.drawable.empty);
            mAdapter.notifyDataSetChanged();
        }
        else if (training_mode == FATBURN){
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
        trainingMode = training_mode;
        sendModeToGuide(GuideService.GuideMessages.TRAINING_MODE_COMMAND, trainingMode);

        if(speechEnabled) {
            //TODO: tts.speak(speech_text, TextToSpeech.QUEUE_FLUSH, null);
            Log.d(TAG, "Speech output");
        }
    }

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Cardio")
                .setIcon(R.drawable.empty)
                .setFootnote("Tap to select this mode - swipe to see alternatives"));


        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Fatburn")
                .setIcon(R.drawable.empty)
                .setFootnote("Tap to select this mode - swipe to see alternatives"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Interval")
                .setIcon(R.drawable.empty)
                .setFootnote("Tap to select this mode - swipe to see alternatives"));
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return super.onGenericMotionEvent(event);
    }

    private GestureDetector createGestureDetector(Context context)
    {
        GestureDetector gestureDetector = new GestureDetector(context);
        //Create a base listener for generic gestures
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                Log.i(TAG, "gesture = " + gesture);
                return false;
            }
        });

        gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
            @Override
            public void onFingerCountChanged(int previousCount, int currentCount) {
                // do something on finger count changes
                Log.i(TAG, "onFingerCountChanged()");

            }
        });
        gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                // do something on scrolling
                Log.i(TAG, "onScroll()");

                return false;
            }
        });
        return gestureDetector;
    }



    @Override
    protected void onResume() {
        super.onResume();

        if(trainingMode == CARDIO) {
            mCards.get(0).setIcon(R.drawable.check_black);
            mCards.get(1).setIcon(R.drawable.empty);
            mCards.get(2).setIcon(R.drawable.empty);
            mAdapter.notifyDataSetChanged();
        }
        else if (trainingMode == FATBURN){
            mCards.get(1).setIcon(R.drawable.check_black);
            mCards.get(0).setIcon(R.drawable.empty);
            mCards.get(2).setIcon(R.drawable.empty);
            mAdapter.notifyDataSetChanged();
        } else if (trainingMode == INTERVAL){
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
/*
        Log.i(TAG, "handling Msg");

        switch (msg.what) {
            case FHSensorManager.Commands.SPEECH_COMMAND:
                int[] speech_change = msg.getData().getIntArray("value");
                if ( speech_change[0] == 1) speechEnabled = true; speechEnabled = false;
                Log.i(TAG, "Speech settings: " + speech_change[0]);
                break;
            case FHSensorManager.Commands.CHALLENGE_MODE_COMMAND:
                int[] challenge_mode = msg.getData().getIntArray("value");
                if (challenge_mode[0] < 4) {
                    mCards.get(0).setIcon(R.drawable.empty);
                    mCards.get(1).setIcon(R.drawable.empty);
                    mCards.get(2).setIcon(R.drawable.empty);
                    mAdapter.notifyDataSetChanged();
                }
        }*/
    }

    public void sendModeToGuide(int messageType, int mode) {
        Message msg = Message.obtain(null, messageType);
        Bundle bundle = new Bundle();
        bundle.putInt("trainingMode", mode);
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

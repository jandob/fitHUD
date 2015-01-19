package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Message;
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
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;

/**
 * Created by Nikolas on 2015-01-17.
 */
public class TrainingMode extends Activity{

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;
    private GestureDetector mGestureDetector;
    private String TAG = "TrainingMode";
    private boolean cardScrollAdapterOn = true;


    @Override
    protected void onCreate(Bundle bundle) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

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
                        if (cardScrollAdapterOn){
                            //mGestureDetector.setScrollListener()
                            //mCardScrollView.deactivate();
                            // Activate scroll
                            //mCardScrollView.activate();
                            //cardScrollAdapterOn = false;
                        } else {
                            //mCardScrollView.activate();
                            //cardScrollAdapterOn = true;
                        }
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

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Cardio")
                .setFootnote("Start or stop the guide"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Fatburn")
                .setFootnote("Start or stop the guide"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Interval")
                .setFootnote("Start or stop the guide"));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Recreation")
                .setFootnote("Start or stop the guide"));
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
        /*
        if (guide_active == 1) {
            mCards.get(0).setText("Guide is activated");
            mAdapter.notifyDataSetChanged();
            Log.d(TAG, "Guide is activated...");
        } else {
            mCards.get(0).setText("Guide is deactivated");
            mAdapter.notifyDataSetChanged();
            Log.d(TAG, "Guide is deactivated...");
        }*/
    }

    @Override
    protected void onPause() {
        mCardScrollView.deactivate();
        super.onPause();
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

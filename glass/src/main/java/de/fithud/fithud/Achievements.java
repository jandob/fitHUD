package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.media.AudioManager;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

import de.fithud.fithudlib.FHSensorManager;
import de.fithud.fithudlib.GuideClass;
import de.fithud.fithudlib.MessengerClient;
import de.fithud.fithudlib.MessengerConnection;

/**
 * Created by JohanV on 04.01.2015.
 */
public class Achievements extends Activity implements MessengerClient{

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;
    private final String TAG = "Achievements";
    MessengerConnection conn = new MessengerConnection(this);

    // Achievement values
    private static int speed_record = 0;
    private static int distance_record = 0;
    private static int height_record = 0;


    @Override
    protected void onCreate(Bundle bundle) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        conn.connect(FHSensorManager.class);

        super.onCreate(bundle);
        createCards();

        mCardScrollView = new CardScrollView(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mAdapter = new CardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScrollView.activate();
    }

    @Override
    protected void onPause() {
        mCardScrollView.deactivate();
        super.onPause();
    }

    private void createCards() {

        // Create here cards based of completed achivements

        mCards = new ArrayList<CardBuilder>();

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Speed-Breaker!!")
                .setFootnote("Your are a freaking speed machine")
                .setTimestamp("just now")
                .addImage(R.drawable.achievement_speed));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Distance record")
                .setFootnote("you biked 50km!!")
                .setTimestamp("just now")
                .addImage(R.drawable.achievement_distance));

        mCards.add(new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setText("Height record")
                .setFootnote("High as the sky!!")
                .setTimestamp("literally")
                .addImage(R.drawable.achievement_height));
    }

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handling Msg");

        switch (msg.what) {
            case FHSensorManager.Messages.HEARTRATE_MESSAGE:
                int heartRate[] = msg.getData().getIntArray("value");
                checkHeartRate(heartRate[0]);
                Log.i(TAG, "Heartrate " + heartRate[0]);
                break;
            case FHSensorManager.Messages.CADENCE_MESSAGE:
                break;
            case FHSensorManager.Messages.HEIGTH_MESSAGE:
                break;
            case FHSensorManager.Messages.SPEED_MESSAGE:
                break;
        }

    }

    private void checkHeartRate(int heartRate){

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


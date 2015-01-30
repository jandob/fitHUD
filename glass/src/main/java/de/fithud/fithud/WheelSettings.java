package de.fithud.fithud;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
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
public class WheelSettings extends Activity implements MessengerClient{

    private CardScrollView mCardScrollView;
    private List<CardBuilder> mCards;
    private CardScrollAdapter mAdapter;
    private AudioManager mAudioManager;

    MessengerConnection conn = new MessengerConnection(this);

    private String TAG = WheelSettings.class.getSimpleName();
    private boolean wheelSpeedActive = false;
    private boolean wheelLightActive = false;


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
                        sendWakeup();
                        break;
                    case 1:
                        wheelSpeedSwitch();
                        break;
                    case 2:
                        wheelLightSwitch();
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
        //conn.disconnect();
        super.onDestroy();
    }

    public void wheelSpeedSwitch(){

        if(!wheelSpeedActive) {
            mCards.get(1).setText("Wheel speed on");
            mCards.get(1).setFootnote("Tap to turn off speed on wheel");
            mAdapter.notifyDataSetChanged();
            wheelSpeedActive = true;
        } else {
            mCards.get(1).setText("Wheel speed off");
            mCards.get(1).setFootnote("Tap to show your speed on wheel");
            mAdapter.notifyDataSetChanged();
            wheelSpeedActive = false;
        }
    }

    public void wheelLightSwitch(){
        if(!wheelLightActive) {
            mCards.get(2).setText("Wheel light on");
            mCards.get(2).setFootnote("Tap to turn off your wheel light");
            mAdapter.notifyDataSetChanged();
            wheelLightActive = true;
        }
        else {
            mCards.get(2).setText("Wheel light off");
            mCards.get(2).setFootnote("Tap to turn on your wheel light");
            mAdapter.notifyDataSetChanged();
            wheelLightActive = false;
        }

    }

    private void createCards() {
        mCards = new ArrayList<CardBuilder>();

        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Find your bike")
                .setFootnote("Tap to wake up your bike!"));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Wheel speed off")
                .setFootnote("Tap to show your speed on wheel"));
        mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Wheel light off")
                .setFootnote("Tap to turn on your wheel light"));
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

    public void sendWakeup() {
        mCards.get(0).setFootnote("Searching...");
        mAdapter.notifyDataSetChanged();
        int[] command = new int[2];
        command[0] = FHSensorManager.Commands.WAKEUP_COMMAND;
        command[1] = 0;
        sendDataToSensormanager(command);
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

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what) {
            case FHSensorManager.Messages.SEARCH_READY:
                mCards.get(0).setFootnote("Tap to wake up your bike!");
                mAdapter.notifyDataSetChanged();
                break;


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

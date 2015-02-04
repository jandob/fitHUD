package de.fithud.fithud;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import de.fithud.fithudlib.MessengerClient;


public class MainActivity extends Activity implements MessengerClient {

    Button btnStart, btnStop, btnBind, btnUnbind;
    TextView textStatus, textData;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    public void handleMessage(Message msg) {
        switch(msg.what) {
            case TestService.Messages.SENSOR_MESSAGE:
                float val = msg.getData().getFloat("HeartRate");
                textData.setText(Float.toString(val));
                break;
            case TestService.Messages.CLIENT_RESPONSE_MESSAGE:

                String answer = msg.getData().getString("answer");
                Log.i(TAG, "answer:" + msg.getData().toString());
                textStatus.setText(answer);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnBind = (Button) findViewById(R.id.btnBind);
        btnUnbind = (Button) findViewById(R.id.btnUnbind);
        textStatus = (TextView) findViewById(R.id.textStatus);
        textData = (TextView) findViewById(R.id.textData);

        btnStart.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v){
                }
            }
        );
        btnStop.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v){
                        stopService(new Intent(MainActivity.this, TestService.class));
                    }
                }
        );
        btnBind.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v){
                    }
                }
        );
        btnUnbind.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v){
                    }
                }
        );
        Log.i(TAG, "onCreate()");
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

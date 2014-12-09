package de.fithud.fithud;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import de.fithud.fithudlib.MainService;


public class MainActivity extends Activity implements MainService.UpdateListener {
    Button btnStart, btnStop, btnBind, btnUnbind;
    TextView textStatus, textData;
    private static final String TAG = MainActivity.class.getSimpleName();
    private MainService mService;

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
                    startService(new Intent(MainActivity.this, MainService.class));
                }
            }
        );
        btnStop.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v){
                        stopService(new Intent(MainActivity.this, MainService.class));
                    }
                }
        );
        btnBind.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v){
                        // Bind to Service
                        Intent intent = new Intent(MainActivity.this, MainService.class);
                        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                    }
                }
        );
        btnUnbind.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v){
                        if (mService != null) {
                            mService.unregisterListener(MainActivity.this);
                        }
                        // unbind service
                        unbindService(mConnection);
                        textStatus.setText("service disconnected");
                    }
                }
        );
        Log.i(TAG, "onCreate()");
        //startService(new Intent(this, MainService.class));
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            // We've bound to Service, cast the IBinder and get Service instance
            MainService.FithudBinder fhBinder = (MainService.FithudBinder) binder;
            mService = fhBinder.getService();
            mService.registerListener(MainActivity.this);
            textStatus.setText("service connected ");
            Log.i(TAG, "onServiceConnected()");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            textStatus.setText("service crashed?");
        }
    };

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

    @Override // updateListener interface from MainService
    public void onUpdate(long value) {
        textData.setText(Long.toString(value));
    }
}

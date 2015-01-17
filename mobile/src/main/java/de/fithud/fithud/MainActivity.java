package de.fithud.fithud;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;


public class MainActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    Button btnStart, btnStop, btnBind, btnUnbind;
    TextView textStatus, textData;
    private static final String TAG = MainActivity.class.getSimpleName();

    // google drive stuff
    private GoogleApiClient mGoogleApiClient;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Logger.getLogger(HttpTransport.class.getName()).setLevel(Level.CONFIG);
        setupButtons();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        Log.i(TAG, "onCreate()");
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
        Log.i(TAG, "onConnected()");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.i(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        Log.i(TAG, "onConnectionFailed");
        Log.i(TAG, result.toString());
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }
    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity)getActivity()).onDialogDismissed();
        }
    }

    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback = new
        ResultCallback<DriveApi.DriveContentsResult>() {
            @Override
            public void onResult(DriveApi.DriveContentsResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.i(TAG, "Error while trying to create new file contents");
                    return;
                }
                final DriveContents driveContents = result.getDriveContents();

                // Perform I/O off the UI thread.
                new Thread() {
                    @Override
                    public void run() {
                        // write content to DriveContents
                        OutputStream outputStream = driveContents.getOutputStream();
                        Writer writer = new OutputStreamWriter(outputStream);
                        try {
                            writer.write("Hello World!");
                            writer.close();
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle("New file")
                                .setMimeType("text/plain")
                                .setStarred(true).build();

                        // create a file on root folder
                        Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                .createFile(mGoogleApiClient, changeSet, driveContents)
                                .setResultCallback(fileCallback);
                    }
                }.start();
            }
        };
    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
        ResultCallback<DriveFolder.DriveFileResult>() {
            @Override
            public void onResult(DriveFolder.DriveFileResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.i(TAG, "Error while trying to create the file");
                    return;
                }
                Log.i(TAG, "Created a file with content: " + result.getDriveFile().getDriveId());
            }
        };
    private void setupButtons() {
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

                        Log.i(TAG, "hans");
                        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                                .setResultCallback(driveContentsCallback);
                    }
                }
        );
        btnStop.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v){

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {  // more about this later
            mGoogleApiClient.connect();
        }
        Log.i(TAG, "onStart()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
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

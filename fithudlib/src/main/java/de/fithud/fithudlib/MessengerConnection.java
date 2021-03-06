package de.fithud.fithudlib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;


/**
 * Use this class to connect to a MessengerService.
 * e.g.:
 * MessengerConnection conn = MessengerConnection(this)
 * conn.connect()
 *
 * Note: You need to implement the MessengerClient interface.
 */

public class MessengerConnection {
    private Context mClient; // TODO how to define mClient implements MessengerClient(see constructor)
    private static final String TAG = MessengerConnection.class.getSimpleName();
    /** Messenger for communicating with service. */
    public  Messenger mService;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;

    public <T extends Context & MessengerClient> MessengerConnection(T client) {
        mClient = client;
    }

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            ((MessengerClient)mClient).handleMessage(msg);
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "onServiceConnected()");
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        MessengerService.Messages.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };

    public void connect(Class serviceClass) {
        // Establish a connection with the service.
        mClient.bindService(new Intent(mClient,
                serviceClass), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Log.d(TAG, "connecting to service: " + serviceClass + "from: " + mClient);
    }

    public void disconnect() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            MessengerService.Messages.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            mClient.unbindService(mConnection);
            mIsBound = false;
        }
    }

    public void send(Message msg) throws RemoteException {
        mService.send(msg);
    }
}


package de.fithud.fithudlib;

/**
 * Created by jandob on 12/16/14.
 */
import android.app.Activity;
import android.app.Service;
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


public abstract class MessengerServiceActivity extends Activity {
    /** Messenger for communicating with service. */
    public  Messenger mService;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;
    /**
     * To be overriden.
     */
    abstract public void handleMessage(Message msg);

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            MessengerServiceActivity.this.handleMessage(msg);
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    public final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    public ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i("Messenger Service abstract", "onServiceConnected()");
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

    public void doBindService(Class serviceClass) {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        //bindService(new Intent(MessengerServiceActivity.this,
        bindService(new Intent(this,
                        serviceClass), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    public void doUnbindService() {
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
            unbindService(mConnection);
            mIsBound = false;
        }
    }

}


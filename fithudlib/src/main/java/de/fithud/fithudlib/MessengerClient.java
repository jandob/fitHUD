package de.fithud.fithudlib;

import android.os.Message;

/**
 *  Used for MessengerClient.
 *  e.g.:
 *
 *  public void handleMessage(Message msg) {
 *      switch (msg.what) {
 *          case exampleMessengerService.Messages.HEARTRATE_MESSAGE:
 *              // do something
 *              break;
 *      }
 *  }
 */
public interface MessengerClient {
    public void handleMessage(Message msg);
}


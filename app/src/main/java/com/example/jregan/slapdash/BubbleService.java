package com.example.jregan.slapdash;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class BubbleService extends Service {
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE = 3;
    static final int MSG_SET_STRING_VALUE = 4;
    private static final String LOGTAG = "MyBubbleService";
    private static boolean isRunning = false;
    // Target we publish for clients to send messages to IncomingHandler.
    final Messenger mMessenger = new Messenger(new IncomingMessHandler());
    // Keeps track of all current registered clients.
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    private NotificationManager nm;
    private Timer timer = new Timer();
    private int counter = 0;
    private int incrementBy = 1;

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOGTAG, "Service Started.");
        showNotification();
        timer.scheduleAtFixedRate(new MyTask(), 0, 100L);
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOGTAG, "Received start id " + startId + ": " + intent);
        // run until explicitly stopped.
        return START_STICKY;
    }

    /**
     * Show notification that can launch out activity if the user selects the notification.
     */
    private void showNotification() {
        int unknownPiReqCode = 0;
        Intent notifIntent = new Intent(this, SlappyScrollingActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, unknownPiReqCode, notifIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        CharSequence text = getText(R.string.service_started);
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification.Builder(this)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setTicker(getText(R.string.n_ticker))
                .setContentTitle("hi i am the notification title")
                .setContentText(text)
                        //              .setSmallIcon(R.drawable.new_mail)
                        //              .setLargeIcon(aBitmap)
                .build();
        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        nm.notify(R.string.service_started, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancel the persistent notification.
        if (timer != null) {
            timer.cancel();
        }
        counter = 0;
        nm.cancel(R.string.service_started);
        Log.i(LOGTAG, "Service Stopped.");
        isRunning = false;
    }

    private void sendMessageToUI(int intvaluetosend) {
        Iterator<Messenger> messengerIterator = mClients.iterator();
        while (messengerIterator.hasNext()) {
            Messenger messenger = messengerIterator.next();
            try {
                // Send data as an Integer
                messenger.send(Message.obtain(null, MSG_SET_INT_VALUE, intvaluetosend, 0));

                // Send data as a String
                Bundle bundle = new Bundle();
                bundle.putString("str1", "ab" + intvaluetosend + "cd");
                Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
                msg.setData(bundle);
                messenger.send(msg);
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list.
                mClients.remove(messenger);
            }
        }
    }

    private class MyTask extends TimerTask {
        @Override
        public void run() {
            Log.i(LOGTAG, "Timer doing work; counter = " + counter);
            try {
                counter += incrementBy;
                sendMessageToUI(counter);
            } catch (Throwable t) {
                Log.e(LOGTAG, "Timer Tick Failed.", t);
            }
        }
    }

    // Handler of incoming messages from clients.
    class IncomingMessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            Log.d(LOGTAG, "handleMessage: " + msg.what);
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    Log.d(LOGTAG, "registering a new client");
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.d(LOGTAG, "unregistering a client");
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SET_INT_VALUE:
                    Log.d(LOGTAG, "changing the value of the increment");
                    incrementBy = msg.arg1;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}

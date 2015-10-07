package com.example.jregan.slapdash;

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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A singleton service.
 */
public class BubbleService extends Service {

    private static final String LOGTAG = "MyBubbleService";

    /**
     * Message types.  Switch on the 'what' field for one these
     * values, and interpret the rest accordingly.
     */
    static class Msg {
        static class Observer {
            static final int REGISTER = 1;
            static final int UNREGISTER = 2;
        }
        static class Set {
            static final int INT_VALUE = 3;
            static final int STR_VALUE = 4;
        }
    }

    // Accepts incoming message from whatever binds to this service.
    private final Messenger msgReceiver = new Messenger(new IncomingMessHandler());

    // Observers (activities or other services) to notify when something interesting happens.
    List<Messenger> msgSenders = new ArrayList<>();

    // Means to tell the human using the device that something interesting happened.
    private NotificationManager nm;

    private int counter = 0;
    private int incrementBy = 1;

    // Used to launch tasks in a non-UX thread.
    private class PeriodicTasker {
        private static final long DELAY_BEFORE_FIRST_TASK = 0L;
        private static final long WAIT_BETWEEN_TASKS = 1 * 1000L;
        private Timer timer = null;

        void start() {
            timer = new Timer();
            timer.scheduleAtFixedRate(
                    new MyTask(),
                    DELAY_BEFORE_FIRST_TASK,
                    WAIT_BETWEEN_TASKS);
        }

        void stop() {
            if (timer != null) {
                // Currently running task will continue to completion.
                timer.cancel();
            }
        }
    }

    private final PeriodicTasker tasker = new PeriodicTasker();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOGTAG, "Service Started.");
        showNotification();
        tasker.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOGTAG, "Received start id " + startId + ": " + intent);
        // run until explicitly stopped.
        return START_STICKY;
    }

    /**
     * Show notification that can launch activity if user selects it.
     */
    private void showNotification() {
        int unknownPiReqCode = 0;
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, unknownPiReqCode,
                new Intent(this, SlappyScrollingActivity.class),
                PendingIntent.FLAG_CANCEL_CURRENT);

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        CharSequence text = getText(R.string.service_started);
        Notification notification = new Notification.Builder(this)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setTicker(getText(R.string.n_ticker))
                .setContentTitle(getText(R.string.n_title))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_stat_action_trending_up)
                        // .setLargeIcon(maybe get one of these)
                .build();
        // Send the notification.
        // We use a layout id because it is a unique number.
        // We use it later to cancel.
        nm.notify(R.string.service_started, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return msgReceiver.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tasker.stop();
        counter = 0;
        nm.cancel(R.string.service_started);
        Log.i(LOGTAG, "Service Stopped.");
    }

    private class MyTask extends TimerTask {
        @Override
        public void run() {
            Log.i(LOGTAG, "Timer doing work; counter = " + counter);
            try {
                // Simulates a slow task.
                Thread.sleep(2000);
                counter += incrementBy;
                updateObservers(counter);
            } catch (Throwable t) {
                Log.e(LOGTAG, "Timer Tick Failed.", t);
            }
        }
    }

    private void updateObservers(int valueToSend) {
        List<Messenger> dead = new ArrayList<>();
        for (Messenger observer: msgSenders) {
            try {
                // Send data as an Integer
                observer.send(Message.obtain(null, Msg.Set.INT_VALUE, valueToSend, 0));

                // Send data as a String
                Bundle bundle = new Bundle();
                bundle.putString("str1", "ab" + valueToSend + "cd");
                Message msg = Message.obtain(null, Msg.Set.STR_VALUE);
                msg.setData(bundle);
                observer.send(msg);
            } catch (RemoteException e) {
                dead.add(observer);
            }
        }
        for (Messenger observer: dead) {
            msgSenders.remove(observer);
        }
    }

    // Handler of incoming messages from clients.
    private class IncomingMessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOGTAG, "handleMessage: " + msg.what);
            switch (msg.what) {
                case Msg.Observer.REGISTER:
                    Log.d(LOGTAG, "registering a new client");
                    msgSenders.add(msg.replyTo);
                    break;
                case Msg.Observer.UNREGISTER:
                    Log.d(LOGTAG, "unregistering a client");
                    msgSenders.remove(msg.replyTo);
                    break;
                case Msg.Set.INT_VALUE:
                    Log.d(LOGTAG, "changing the value of the increment");
                    incrementBy = msg.arg1;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}

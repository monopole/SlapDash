package com.example.jregan.slapdash;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SlappyScrollingActivity extends AppCompatActivity
        implements View.OnClickListener, ServiceConnection {

    private static final String LOGTAG = "HooHahScrollingActivity";
    private static boolean DO_SILLY_THING = 3 - 2 == 1;
    private final Messenger mMessenger = new Messenger(new ClientMessageHandler());
    boolean mIsBound = false;
    private Button btnStart, btnStop, btnBind, btnUnbind, btnUpby1, btnUpby10;
    private TextView textStatus, textIntValue, textStrValue;
    private Messenger mServiceMessenger = null;

    private void prepButtons() {
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnBind = (Button) findViewById(R.id.btnBind);
        btnUnbind = (Button) findViewById(R.id.btnUnbind);
        textStatus = (TextView) findViewById(R.id.textStatus);
        textIntValue = (TextView) findViewById(R.id.textIntValue);
        textStrValue = (TextView) findViewById(R.id.textStrValue);
        btnUpby1 = (Button) findViewById(R.id.btnUpby1);
        btnUpby10 = (Button) findViewById(R.id.btnUpby10);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnBind.setOnClickListener(this);
        btnUnbind.setOnClickListener(this);
        btnUpby1.setOnClickListener(this);
        btnUpby10.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOGTAG, "in onCreate, mIsBound = " + mIsBound);
        setContentView(R.layout.activity_slappy_scrolling);
        if (DO_SILLY_THING) {
            prepButtons();
        } else {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });
        }
    }

    /**
     * This happens if the activity is destroyed.
     * <p/>
     * E.g. on phone rotation, this is called, and shortly thereafter onCreate is called.
     * If the service was running when onDestroy was called, the service will keep running,
     * and onCreate will notice that it's running and bind to it, even if the service
     * wasn't bound before destroy.  It's possible to store an isBound bit away to avoid
     * such automatic binding.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOGTAG, "got DESTROY");
        try {
            doUnbindService();
        } catch (Throwable t) {
            Log.e(LOGTAG, "Failed to unbind from the service", t);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(LOGTAG, "in onSaveInstanceState");
        outState.putBoolean("isBound", mIsBound);
        outState.putString("textStatus", textStatus.getText().toString());
        outState.putString("textIntValue", textIntValue.getText().toString());
        outState.putString("textStrValue", textStrValue.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(LOGTAG, "in onRestoreInstanceState");
        boolean wasBound = false;
        if (savedInstanceState != null) {
            Log.i(LOGTAG, "pulling in data");
            wasBound = savedInstanceState.getBoolean("isBound", false);
            textStatus.setText(savedInstanceState.getString("textStatus"));
            textIntValue.setText(savedInstanceState.getString("textIntValue"));
            textStrValue.setText(savedInstanceState.getString("textStrValue"));
        }
        if (wasBound) {
            if (BubbleService.isRunning()) {
                doBindService();
            }
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * Binds this Activity (which implements ServiceConnection), to the given service.
     * If this succeeds, onServiceConnected will execute, creating a Messenger to
     * use for talking to the service.
     */
    private void doBindService() {
        Log.i(LOGTAG, "Doing the service binding in thread " + Thread.currentThread().getId());
        bindService(new Intent(this, BubbleService.class), this, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        textStatus.setText("Binding.");
        Log.i(LOGTAG, "Binding completed.");
    }

    /**
     * This method will execute in the same thread shortly after (and as a consequence of)
     * doBindService.  The registration allows messages to be sent back, to be handled in
     * this activity by ClientMessageHandler.
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mServiceMessenger = new Messenger(service);
        textStatus.setText("Attached.");
        Log.i(LOGTAG, "got onServiceConnected in thread " + Thread.currentThread().getId());
        try {
            // Register this activity with the service, allowing the service to
            // send messages back
            Message msg = Message.obtain(null, BubbleService.MSG_REGISTER_CLIENT);
            msg.replyTo = mMessenger;
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(LOGTAG, "Service trouble; unable to register with it.");
        }
    }

    /**
     * Undoes the work of doBindService.
     */
    private void doUnbindService() {
        Log.i(LOGTAG, "Doing the service UN-binding");
        if (!mIsBound) {
            Log.i(LOGTAG, "Nothing to do for unbind, since mIsBound is false.");
            return;
        }
        if (mServiceMessenger == null) {
            Log.i(LOGTAG, "Don't have a server messenger, no need to unregister myself");
        } else {
            // Tell the service not to send any more messages.
            try {
                Message msg = Message.obtain(null, BubbleService.MSG_UNREGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                // Maybe the service crashed?
            }
        }
        unbindService(this);
        mIsBound = false;
        textStatus.setText("Unbinding.");
    }

    /**
     * Called when the connection with the service has been unexpectedly disconnected.
     * Not called as a consequence of unBindService.
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(LOGTAG, "got onServiceDisConnected");
        mServiceMessenger = null;
        textStatus.setText("Disconnected.");
    }

    /**
     * Handle button clicks.
     */
    @Override
    public void onClick(View v) {
        if (v.equals(btnStart)) {
            Log.i(LOGTAG, "Calling startService.");
            startService(new Intent(SlappyScrollingActivity.this, BubbleService.class));
        } else if (v.equals(btnStop)) {
            Log.i(LOGTAG, "Calling unbind and stopservice.");
            doUnbindService();
            stopService(new Intent(SlappyScrollingActivity.this, BubbleService.class));
        } else if (v.equals(btnBind)) {
            Log.i(LOGTAG, "Calling bindservice.");
            doBindService();
        } else if (v.equals(btnUnbind)) {
            Log.i(LOGTAG, "Calling unbindservice.");
            doUnbindService();
        } else if (v.equals(btnUpby1)) {
            Log.i(LOGTAG, "Calling send message with 1 in it.");
            sendMessageToService(1);
        } else if (v.equals(btnUpby10)) {
            Log.i(LOGTAG, "Calling send message with 1000 in it.");
            sendMessageToService(1000);
        }
    }

    /**
     * Send message to the service, obviously.
     *
     * @param valueToSend The data to send
     */
    private void sendMessageToService(int valueToSend) {
        Log.i(LOGTAG, "Trying to send message to service.");
        if (!mIsBound) {
            Log.e(LOGTAG, "Cannot send message since not mIsBound.");
            return;
        }
        if (mServiceMessenger == null) {
            Log.e(LOGTAG, "Cannot send message since mServiceMessenger is null.");
            return;
        }
        try {
            Message msg = Message.obtain(null, BubbleService.MSG_SET_INT_VALUE, valueToSend, 0);
            msg.replyTo = mMessenger;
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(LOGTAG, "Trouble sending data to service");
        }
    }

    /**
     * Handle incoming messages from MyService
     */
    private class ClientMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOGTAG, "In activity - have an incoming message.");
            switch (msg.what) {
                case BubbleService.MSG_SET_INT_VALUE:
                    Log.d(LOGTAG, "It is an INT message: " + msg.arg1);
                    textIntValue.setText("Int Message: " + msg.arg1);
                    break;
                case BubbleService.MSG_SET_STRING_VALUE:
                    String str1 = msg.getData().getString("str1");
                    Log.d(LOGTAG, "It is an STRING message: " + str1);
                    textStrValue.setText("Str Message: " + str1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    // Original method from ScrollingAction.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(LOGTAG, "got onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_slappy_scrolling, menu);
        return true;
    }

    // Original method from ScrollingAction.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(LOGTAG, "got onOptionsItemSelected");
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

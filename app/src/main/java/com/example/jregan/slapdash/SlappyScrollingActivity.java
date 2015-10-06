package com.example.jregan.slapdash;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

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
    private ServiceConnection mConnection = this;

    // SILLY
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
        setContentView(R.layout.activity_slappy_scrolling);
        if (DO_SILLY_THING) {
            prepButtons();
            if (BubbleService.isRunning()) {
                doBindService();
            }
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


    // SILLY
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("textStatus", textStatus.getText().toString());
        outState.putString("textIntValue", textIntValue.getText().toString());
        outState.putString("textStrValue", textStrValue.getText().toString());
    }


    // SILLY
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            textStatus.setText(savedInstanceState.getString("textStatus"));
            textIntValue.setText(savedInstanceState.getString("textIntValue"));
            textStrValue.setText(savedInstanceState.getString("textStrValue"));
        }
        super.onRestoreInstanceState(savedInstanceState);
    }


    /**
     * SILLY Send data to the service
     *
     * @param intvaluetosend The data to send
     */
    private void sendMessageToService(int intvaluetosend) {
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
            Message msg = Message.obtain(null, BubbleService.MSG_SET_INT_VALUE, intvaluetosend, 0);
            msg.replyTo = mMessenger;
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(LOGTAG, "Trouble sending integer to service!!!!!!!!!!!!!!!!");
        }
    }

    /**
     * SILLY Bind this Activity to BubbleService
     */
    private void doBindService() {
        Log.i(LOGTAG, "Doing the service binding");
        bindService(new Intent(this, BubbleService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        textStatus.setText("Binding.");
    }

    /**
     * SILLY Un-bind this Activity to BubbleService
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
            // Tell the service we're going away.
            try {
                Message msg = Message.obtain(null, BubbleService.MSG_UNREGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
        }
        // Detach our existing connection.
        unbindService(mConnection);
        mIsBound = false;
        textStatus.setText("Unbinding.");
    }

    /**
     * SILLY Handle button clicks
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
     * SILLY This is called once when the service gets started.and knows about us. Could
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mServiceMessenger = new Messenger(service);
        textStatus.setText("Attached.");
        lotsOfLogSpacing();
        Log.i(LOGTAG, "got onServiceConnected");
        try {
            Message msg = Message.obtain(null, BubbleService.MSG_REGISTER_CLIENT);
            msg.replyTo = mMessenger;
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(LOGTAG, "Service trouble; unable to register with it.");
        }
    }

    private void lotsOfLogSpacing() {
        for (int i = 1; i < 20; i++) {
            Log.i(LOGTAG, "*");
        }
    }

    /**
     * SILLY Called when the connection with the service has been unexpectedly disconnected -
     * process crashed.
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        lotsOfLogSpacing();
        Log.i(LOGTAG, "got onServiceDisConnected");
        mServiceMessenger = null;
        textStatus.setText("Disconnected.");
    }

    /**
     * SILLY
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
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(LOGTAG, "got onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_slappy_scrolling, menu);
        return true;
    }

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

    /**
     * SILLY Handle incoming messages from MyService
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
}

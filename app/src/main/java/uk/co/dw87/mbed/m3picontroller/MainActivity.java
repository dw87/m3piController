package uk.co.dw87.mbed.m3picontroller;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {
    public static final String PREFS_FILE = "m3piPrefsFile";

    public String mConnectionString;
    public Boolean mBroadcastChecked;

    private RobotThread mRobotThread;
    private RobotView mRobotView;

    //Use this BroadcastReceiver to check for changes in the WiFi state
    //e.g. If it is enabled/disabled or gets/loses connection while the app is running
    BroadcastReceiver wifiConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                return;
            }
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, 0));

            if (!noConnectivity) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    // Handle connected case
                    wifiOn();
                }
            } else {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    // Handle disconnected case
                    wifiOff();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        //Keep the device awake while using the app
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mRobotView = (RobotView)findViewById(R.id.robotArea);

        mRobotView.setRobotXView((TextView)findViewById(R.id.robotX_value));
        mRobotView.setRobotYView((TextView) findViewById(R.id.robotY_value));
        mRobotView.setSpriteXView((TextView) findViewById(R.id.spriteX_value));
        mRobotView.setSpriteYView((TextView) findViewById(R.id.spriteY_value));
        mRobotView.setTouchXView((TextView) findViewById(R.id.touchX_value));
        mRobotView.setTouchYView((TextView) findViewById(R.id.touchY_value));
        mRobotView.setStatusView((TextView) findViewById(R.id.status_label));

        final Button connectButton = (Button) findViewById(R.id.btnConnect);
        final Button startStopButton = (Button) findViewById(R.id.btnStartStop);
        final Button aboutButton = (Button) findViewById(R.id.btnAbout);

        connectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRobotThread.getMode() == RobotThread.STATE_READY){
                    if(mRobotThread.doConnect(mConnectionString, mBroadcastChecked)){
                        connectButton.setText(R.string.disconnect_button);
                        startStopButton.setEnabled(true);
                    }
                }
                else if (mRobotThread.getMode() == RobotThread.STATE_CONNECTED ||
                        mRobotThread.getMode() == RobotThread.STATE_PAUSED){
                    mRobotThread.doDisconnect();
                    connectButton.setText(R.string.connect_button);
                    startStopButton.setEnabled(false);
                }
            }
        });

        startStopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mRobotThread.getMode() == RobotThread.STATE_CONNECTED) || (mRobotThread.getMode() == RobotThread.STATE_PAUSED)){
                    mRobotThread.doStart();
                    startStopButton.setText(R.string.stop_button);
                    connectButton.setEnabled(false);
                }
                else if (mRobotThread.getMode() == RobotThread.STATE_RUNNING){
                    mRobotThread.doPause();
                    startStopButton.setText(R.string.start_button);
                    connectButton.setEnabled(true);
                }
            }
        });

        aboutButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openAboutDialog();
            }
        });

        this.startControl(mRobotView, null, savedInstanceState);

        if (mWifi.isConnected()) {
            mRobotThread.setState(RobotThread.STATE_READY);
            connectButton.setEnabled(true);
        }

        // Restore saved preferences
        SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
        mConnectionString = settings.getString("robotString", "0.0.0.0");
        mBroadcastChecked = settings.getBoolean("broadcastChecked", false);

        //notify("onCreate");
    }

    private void startControl(RobotView gView, RobotThread gThread, Bundle savedInstanceState) {
        //Set up a new game, we don't care about previous states
        mRobotThread = new RobotThread(mRobotView);
        mRobotView.setThread(mRobotThread);
        mRobotThread.setState(RobotThread.STATE_NOWIFI);
    }

    private void wifiOn(){
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifi.isConnected()) {
            if(mRobotThread.getMode() == RobotThread.STATE_NOWIFI) {
                mRobotThread.onWiFiConnect();
            }
            final Button connectButton = (Button) findViewById(R.id.btnConnect);
            connectButton.setEnabled(true);
        }
    }

    private void wifiOff() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (!mWifi.isConnected()) {
            if(mRobotThread.getMode() != RobotThread.STATE_NOWIFI) {
                mRobotThread.onWiFiDisconnect();
            }
            final Button connectButton = (Button) findViewById(R.id.btnConnect);
            final Button startStopButton = (Button) findViewById(R.id.btnStartStop);
            connectButton.setText(R.string.connect_button);
            startStopButton.setText(R.string.start_button);
            connectButton.setEnabled(false);
            startStopButton.setEnabled(false);
        }
    }

    /*
	 * Activity state functions
	 */

    @Override
    protected void onPause() {
        super.onPause();

        final Button connectButton = (Button) findViewById(R.id.btnConnect);
        final Button startStopButton = (Button) findViewById(R.id.btnStartStop);

        unregisterReceiver(wifiConnectionReceiver);

        if(mRobotThread.getMode() == RobotThread.STATE_RUNNING) {
            mRobotThread.setState(RobotThread.STATE_PAUSED);
            connectButton.setEnabled(true);
            startStopButton.setText(R.string.start_button);
        }

        //notify("onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        final Button connectButton = (Button) findViewById(R.id.btnConnect);
        final Button startStopButton = (Button) findViewById(R.id.btnStartStop);

        if (!mWifi.isConnected()) {
            mRobotThread.setState(RobotThread.STATE_NOWIFI);
            connectButton.setEnabled(false);
            startStopButton.setEnabled(false);
            connectButton.setText(R.string.connect_button);
            startStopButton.setText(R.string.start_button);
        }

        registerReceiver(wifiConnectionReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));

        //if(mRobotThread.getMode() == RobotThread.STATE_PAUSED) {
        //    mRobotThread.setState(RobotThread.STATE_RUNNING);
        //}
        //notify("onResume");
    }

    @Override
    protected void onStart(){
        super.onStart();
        //notify("onStart");
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        //notify("onRestart");
    }

    @Override
    protected void onStop(){
        super.onStop();
        //notify("onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRobotView.cleanup();
        mRobotThread = null;
        mRobotView = null;
        //notify("onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){
            case R.id.action_settings:
                if (mRobotThread.getMode() == RobotThread.STATE_READY) {
                    openSettingsDialog();
                }
                return true;
            case R.id.action_about:
                openAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void openSettingsDialog(){
        View promptView = getLayoutInflater().inflate(R.layout.robot_menu, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set robot_menu.xml to be the layout file of the alertDialog builder
        alertDialogBuilder.setView(promptView);

        final EditText input = (EditText) promptView.findViewById(R.id.robotInput);
        input.setText(mConnectionString);

        final CheckBox broadcast = (CheckBox) promptView.findViewById(R.id.broadcastInput);
        broadcast.setChecked(mBroadcastChecked);

        input.setEnabled(!mBroadcastChecked);

         alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // get user input and set it to result
                        mConnectionString = input.getText().toString();
                        mBroadcastChecked = broadcast.isChecked();
                        SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("robotString", mConnectionString);
                        editor.putBoolean("broadcastChecked", mBroadcastChecked);

                        // Commit the edits!
                        editor.commit();
                    }
                })
                .setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,	int id) {
                            dialog.cancel();
                        }
                    });

        // Create an alert dialog
        final AlertDialog alertD = alertDialogBuilder.create();

        alertD.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                //Match IP Address
                //if(input.getText().toString().matches("(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))")){
                if(input.getText().toString().matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")){
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
                //Or match Hostname
                //else if (input.getText().toString().matches("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$")){
                //    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                //}
                else {
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                //Match IP Address
                //if(s.toString().matches("(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))")){
                if (s.toString().matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")) {
                    alertD.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
                ////Or match Hostname
                //else if (s.toString().matches("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$")){
                //    alertD.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                //}
                else {
                    alertD.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        broadcast.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    input.setEnabled(false);
                }
                else {
                    input.setEnabled(true);
                }
            }
        });

        alertD.show();
    }

    public void openAboutDialog(){
        AlertDialog.Builder dlgAbout  = new AlertDialog.Builder(this);

        dlgAbout.setTitle(R.string.app_name);
        dlgAbout.setMessage(R.string.app_about);
        dlgAbout.setCancelable(true);
        dlgAbout.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //dismiss the dialog
                dialog.cancel();
            }
        });
        dlgAbout.create().show();
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // Do nothing if nothing is selected
    }

    private void notify(String methodName) {
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ball)
                .setContentTitle(methodName)
                .setContentText(Long.toString(System.currentTimeMillis()))
                .setAutoCancel(true).build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }
}
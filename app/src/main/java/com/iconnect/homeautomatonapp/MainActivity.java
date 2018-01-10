package com.iconnect.homeautomatonapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mAdapter;
    private BluetoothDevice mBtDevice;
    private ConnectThread connectThread;
    private ConnectBT connectBT;
    private BroadcastReceiver mReceiver;
    IntentFilter intentFilter;
    private TextView tv_connection, tv_msg_sent, tv_msg_received;
    private CheckBox cb_FansOn, cb_FansOff, cb_LightsOn, cb_LightsOff, cb_TvOn, cb_Tvoff, cb_Terminal1On,
    cb_Terminal1Off, cb_Terminal2On, cb_Terminal2Off;
    private ImageView lightsImage, fanImage;
    private ImageButton btStatusBtn;
    private String bluetoothMsg;
    private String address = "", name = "";
    boolean isConnected = false;
    boolean isEnabled = false;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LISTVIEW = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btStatusBtn = (ImageButton)findViewById(R.id.bt_status_btn);
        tv_msg_sent = (TextView)findViewById(R.id.tv_msg_sent);
        tv_msg_received = (TextView)findViewById(R.id.tv_msg_received);
        tv_connection = (TextView)findViewById(R.id.tv_connection);
        tv_connection.setText("Disconnected");
        tv_connection.setTextColor(Color.RED);
        cb_LightsOn = (CheckBox)findViewById(R.id.cb_Lights_On);
        cb_LightsOff = (CheckBox)findViewById(R.id.cb_Lights_Off);
        cb_FansOn = (CheckBox) findViewById(R.id.cb_Fans_On);
        cb_FansOff = (CheckBox) findViewById(R.id.cb_Fans_Off);
        lightsImage = (ImageView)findViewById(R.id.lights_imageView);
        fanImage = (ImageView)findViewById(R.id.fans_imageView);

        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        mReceiver = new BluetoothStateReceiver();

        setupBluetooth();

        cb_LightsOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (isConnected && isEnabled) {
                    if (compoundButton.isChecked()) {
                        cb_LightsOff.setChecked(false);
                        bluetoothMsg = "Lights On";
                        connectThread.writeMsg("Lights On");
                        lightsImage.setImageResource(R.drawable.lights_on);
                    }
                } else {
                    compoundButton.setChecked(false);
                    makeText("Bluetooth is not connected to remote device");
                    setupBluetooth();
                }
            }
        });

        cb_LightsOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (isConnected && isEnabled) {
                    if (compoundButton.isChecked()) {
                        cb_LightsOn.setChecked(false);
                        bluetoothMsg = "Lights Off";
                        connectThread.writeMsg("Lights Off");
                        lightsImage.setImageResource(R.drawable.lights_off);
                    }
                } else {
                    compoundButton.setChecked(false);
                    makeText("Bluetooth is not connected to remote device");
                    setupBluetooth();
                }
            }
        });

        btStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupBluetooth();
            }
        });

        tv_connection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isConnected)
                    setupBluetooth();
            }
        });
    }

    private void setupBluetooth() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            makeText("Device does not support bluetooth");
            finish();
        } else {
            if (!mAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Intent intent = new Intent(MainActivity.this, PairedDevicesActivity.class);
                startActivityForResult(intent, REQUEST_LISTVIEW);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ENABLE_BT) {
                makeText("Bluetooth Enabled");
                Intent intent = new Intent(MainActivity.this, PairedDevicesActivity.class);
                startActivityForResult(intent, REQUEST_LISTVIEW);
            } else if (requestCode == REQUEST_LISTVIEW) {
                address = data.getStringExtra("address");
                name = data.getStringExtra("name");
                makeText("Connected to " + name + " bluetooth device");
                mBtDevice = mAdapter.getRemoteDevice(address);
                connectBT = new ConnectBT(mBtDevice);
                connectBT.start();
            }
        } else {
            makeText("Unable to connect to bluetooth. Pls try again later");
        }
    }


    private class ConnectBT extends Thread {
        private final BluetoothSocket socket;

        ConnectBT(BluetoothDevice device) {
            BluetoothSocket temp = null;
            try {
                temp = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
                System.out.println("Socket created successfully");
            } catch (IOException e) {
                System.out.println("Unable to connect");
            }
            socket = temp;
        }

        public void run() {
            mAdapter.cancelDiscovery();
            try {
                System.out.println("Trying to connect socket");
                socket.connect();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                try {
                    socket.close();
                } catch (IOException e1) {
                    System.out.println(e.getMessage());
                }
                return;
            }

            System.out.println("Socket connected successfully");
            connectThread = new ConnectThread(socket);
            connectThread.start();
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                makeText(e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread{
        BluetoothSocket socket;
        private InputStream inputStream = null;
        private OutputStream outStream = null;

        ConnectThread(BluetoothSocket socket) {
            InputStream tempIn = null;
            OutputStream tempOut = null;
            this.socket = socket;
            try {
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tempIn;
            outStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while(true) {
                try {
                    bytes = inputStream.read(buffer);
                    final String readMessage = new String(buffer, 0, bytes);
                    bluetoothMsg = readMessage;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (readMessage.equalsIgnoreCase("Lights On")) {
                                cb_LightsOn.setChecked(true);
                                tv_msg_received.setText(readMessage);
                            } else if (readMessage.equalsIgnoreCase("Lights Off")){
                                cb_LightsOff.setChecked(true);
                                tv_msg_received.setText(readMessage);
                            } else if (readMessage.equalsIgnoreCase("Fans On")){
                                cb_LightsOff.setChecked(true);
                                tv_msg_received.setText(readMessage);
                            } else if (readMessage.equalsIgnoreCase("Fans Off")){
                                cb_LightsOff.setChecked(true);
                                tv_msg_received.setText(readMessage);
                            }
                        }
                    });
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    break;
                }
            }
        }

        public void writeMsg(final String msg) {
            byte[] bytes = msg.getBytes();
            try {
                if (socket != null) {
                    outStream.write(bytes);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_msg_sent.setText(msg);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_msg_sent.setText("Bluetooth devices are not paired");
                        }
                    });
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private class BluetoothStateReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            {
                String action = intent.getAction();
                if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    tv_connection.setText("Connected");
                    tv_connection.setTextColor(Color.GREEN);
                    isConnected = true;
                } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
                    tv_connection.setText("Disconnected");
                    tv_connection.setTextColor(Color.RED);
                    isConnected = false;
                } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_ON:
                            btStatusBtn.setImageResource(R.drawable.bluetooth);
                            tv_connection.setClickable(false);
                            btStatusBtn.setEnabled(false);
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            btStatusBtn.setImageResource(R.drawable.lights_off);
                            btStatusBtn.setEnabled(true);
                            tv_connection.setClickable(true);
                    }
                }
            }
        }
    }

    private void makeText(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onStart();
        if (mAdapter.isEnabled()) {
            isEnabled = true;
            btStatusBtn.setImageResource(R.drawable.bluetooth);
            btStatusBtn.setEnabled(false);
        } else {
            isEnabled = false;
            btStatusBtn.setImageResource(R.drawable.lights_off);
            btStatusBtn.setEnabled(true);
        }

        /* if (mBtDevice != null) {
            if (name != null && mAdapter.getRemoteDevice(name) != null) {
                tv_connection.setText("Connected");
                tv_connection.setTextColor(Color.GREEN);
                isConnected = true;
            } else {
                tv_connection.setText("Disconnected");
                tv_connection.setTextColor(Color.RED);
                isConnected = false;
            }
        } */
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
}

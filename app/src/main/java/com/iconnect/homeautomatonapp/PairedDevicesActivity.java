package com.iconnect.homeautomatonapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class PairedDevicesActivity extends AppCompatActivity {
    private BluetoothAdapter mAdapter;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paired_devices);

        listView = (ListView)findViewById(R.id.listView);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevices = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                bluetoothDevices.add(device);
            }
        } else {
            Toast.makeText(this, "No paired device", Toast.LENGTH_SHORT).show();
            Intent intent = getIntent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }

        ArrayAdapter<BluetoothDevice> arrayAdapter = new ArrayAdapter<BluetoothDevice>(
                this,
                android.R.layout.simple_list_item_1,
                bluetoothDevices
        );

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Bundle bundle = new Bundle();
                bundle.putString("name", bluetoothDevices.get(i).getName());
                bundle.putString("address", bluetoothDevices.get(i).getAddress());
                Intent intent = getIntent();
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}

package com.bifel.lamp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_DATA_RECEIVE = "action_data_receive";
    public static final String EXTRA_TEXT = "data";

    private TimePickerDialog alarmDialog;
    private AlertDialog alertDialog;
    private BTAdapter btAdapter;
    private ToastSender toast;
    private BroadcastReceiver mReceiver;
    private ImageView imgLamp;
    private ArrayAdapter<CharSequence> listAdapter;
//    private List<CharSequence> list = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice);
        toast = new ToastSender(getMainLooper(), this);
        btAdapter = new BTAdapter(BluetoothAdapter.getDefaultAdapter(), this);
        imgLamp = findViewById(R.id.imgLamp);
        mReceiver = createReceiver();
        alarmDialog = createTimePickerDialog();
        alertDialog = createAlertDialog();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(ACTION_DATA_RECEIVE);
        filter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btAdapter.destroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 0) toast.send("Please enable bluetooth access");
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        btAdapter.cancelDiscovery();
    }

    public void onLampPress(View view) {
        if (btAdapter.isOutputStreamActive()) {
            btAdapter.sendData("S00000");
        } else {
            toast.send("Can't send data");
        }
//        btAdapter.intentEcho("on0000");
    }

    public void onAlarmPress(View view) {
        alarmDialog.show();
    }

    public void onRefreshPress(View view) {
        listAdapter.clear();
        btAdapter.startDiscovery();
        listAdapter.addAll(btAdapter.getAlreadyPairedBluetoothDevices());
        listAdapter.notifyDataSetChanged();

        alertDialog.show();

    }

    @SuppressLint("DefaultLocale")
    private void setAlarm(int hour, int minutes) {
        System.out.println("Time picker dialog: hour - " + hour + " minutes - " + minutes);
        btAdapter.sendData(String.format("%06d%n", hour * 60 + minutes));

    }

    private void setLampImagine(boolean isOn) {
        if (isOn) {
            imgLamp.setImageResource(R.drawable.img_lamp_on);
        } else {
            imgLamp.setImageResource(R.drawable.img_lamp_off);
        }
    }

    private void parseData(String data) {
        System.out.println(data);
        switch (data) {
            case "on0000":
                setLampImagine(true);
                break;

            case "off000":
                setLampImagine(false);
                break;
        }

    }

    private TimePickerDialog createTimePickerDialog() {
        return new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @SuppressLint("DefaultLocale")
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minutes) {
                setAlarm(hour, minutes);
            }
        }, 8, 0, true);
    }

    private BroadcastReceiver createReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                System.out.println("OnReceive  action = " + action);
                if (action == null) return;
                switch (action) {
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                        list.add(device.getName());
                        listAdapter.add(device.getName());
                        listAdapter.notifyDataSetChanged();
                        btAdapter.addNewDevice(device);
                        toast.send("Found device " + device.getName());
                        break;
                    case ACTION_DATA_RECEIVE:
                        Bundle extras = intent.getExtras();
                        if (extras != null) parseData(extras.getString(EXTRA_TEXT));
                        else System.out.println("Extras == null");
                        break;
                    case BluetoothAdapter.ACTION_REQUEST_ENABLE:
                        startActivityForResult(intent, 0);
                        break;
                }
            }
        };
    }

    private AlertDialog createAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enabled devices")
                .setAdapter(listAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        btAdapter.connectToDevice(listAdapter.getItem(which));
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(R.color.gray);
        return alertDialog;

    }
}
package com.bifel.lamp;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TimePicker;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_DATA_RECEIVE = "action_data_receive";
    public static final String ACTION_STOP_DISCOVERY = "action_stop_discovery";
    public static final String ACTION_START_DISCOVERY = "action_start_discovery";
    public static final String ACTION_CLOSE_LIST_DIALOG = "action_close_list_dialog";
    public static final String EXTRA_TEXT = "extra_text";

    private TimePickerDialog alarmDialog;
    private ListDialog listDialog;
    private BTAdapter btAdapter;
    private ToastSender toast;
    private BroadcastReceiver mReceiver;
    private ImageView imgLamp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toast = new ToastSender(this, getMainLooper());
        btAdapter = new BTAdapter(this, BluetoothAdapter.getDefaultAdapter());
        imgLamp = findViewById(R.id.imgLamp);
        mReceiver = createReceiver();
        alarmDialog = createTimePickerDialog();

        listDialog = new ListDialog(this);
        listDialog.setOnClick(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                btAdapter.connectToDevice(listDialog.getItem(position));
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        filter.addAction(ACTION_DATA_RECEIVE);
        filter.addAction(ACTION_STOP_DISCOVERY);
        filter.addAction(ACTION_START_DISCOVERY);
        filter.addAction(ACTION_CLOSE_LIST_DIALOG);
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
        btAdapter.startDiscovery();
        listDialog.show();
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
            case "S1":
                setLampImagine(true);
                break;

            case "S0":
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
                        listDialog.clear();
                        listDialog.setDiscoveryMonitor(true);
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        listDialog.setDiscoveryMonitor(false);
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        System.out.println("Found device " + device.getName());
                        listDialog.add(device.getName());
                        btAdapter.addNewDevice(device);
                        break;
                    case BluetoothAdapter.ACTION_REQUEST_ENABLE:
                        startActivityForResult(intent, 0);
                        break;
                    case ACTION_DATA_RECEIVE:
                        Bundle extras = intent.getExtras();
                        if (extras != null) parseData(extras.getString(EXTRA_TEXT));
                        else System.out.println("Extras == null");
                        break;
                    case ACTION_STOP_DISCOVERY:
                        btAdapter.cancelDiscovery();
                        break;
                    case ACTION_START_DISCOVERY:
                        btAdapter.startDiscovery();
                        break;
                    case ACTION_CLOSE_LIST_DIALOG:
                        listDialog.dismiss();
                        break;
                }
            }
        };
    }
}
package com.bifel.lamp.activity;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TimePicker;

import com.bifel.lamp.BTAdapter;
import com.bifel.lamp.ListDialog;
import com.bifel.lamp.R;
import com.bifel.lamp.ToastSender;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_DATA_RECEIVE = "action_data_receive";
    public static final String ACTION_STOP_DISCOVERY = "action_stop_discovery";
    public static final String ACTION_START_DISCOVERY = "action_start_discovery";
    public static final String ACTION_CLOSE_LIST_DIALOG = "action_close_list_dialog";
    public static final String ACTION_CONNECTED = "action_connected";
    public static final String EXTRA_TEXT = "extra_text";

    private TimePickerDialog alarmDialog;
    private ListDialog listDialog;
    private BTAdapter btAdapter;
    private ToastSender toast;
    private BroadcastReceiver mReceiver;
    private ImageView imgLamp;
    private SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toast = new ToastSender(this, getMainLooper());
        btAdapter = new BTAdapter(this, BluetoothAdapter.getDefaultAdapter());
        imgLamp = findViewById(R.id.imgLamp);
        mReceiver = createReceiver();
        alarmDialog = createTimePickerDialog();

        sp = PreferenceManager.getDefaultSharedPreferences(this);

        listDialog = new ListDialog(this);
        listDialog.setOnClick(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                btAdapter.connectToDevice(listDialog.getItem(position).toString());
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
        filter.addAction(ACTION_CONNECTED);
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
            btAdapter.send("S");
        } else {
            toast.send("Can't send data");
        }
    }

    public void onAlarmPress(View view) {
        alarmDialog.show();
    }

    public void onRefreshPress(View view) {
        btAdapter.startDiscovery();
        listDialog.show();
    }

    @Override
    protected void onResume() {
        btAdapter.send("D" + sp.getString("flashIntensity", "0"));
        super.onResume();
    }

    public void onSettingsPress(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @SuppressLint("DefaultLocale")
    private void setAlarm(int hour, int minutes) {
        long currentTime = System.currentTimeMillis();

        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeInMillis(currentTime);
        int currentMinutes = calendar.get(Calendar.MINUTE);
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        System.out.println("Current hour " + currentHour + "| Current minute - " + currentMinutes);

        long currentLocalMinutes = (currentHour * 60) + currentMinutes;
        long futureLocalMinutes = (hour * 60) + minutes;

        long dif = futureLocalMinutes - currentLocalMinutes;
        dif += dif < 0 ? 1440 : 0; // 1440 is a minutes in day
        long delayToAlarmInMinutes = TimeUnit.MINUTES.convert(dif, TimeUnit.MINUTES);

        btAdapter.send("A" + delayToAlarmInMinutes * 60_000);

    }

    private void setLampImagine(boolean isOn) {
        if (isOn) {
            imgLamp.setImageResource(R.drawable.img_lamp_on);
        } else {
            imgLamp.setImageResource(R.drawable.img_lamp_off);
        }
    }

    private void parseData(String data) {
        switch (data.substring(0, 1)) {
            case "S":
                if ("S1".equals(data)) {
                    setLampImagine(true);
                } else {
                    setLampImagine(false);
                }
                break;
            case "A":
                int timeInMillis = Integer.valueOf(data.substring(1, data.length()));
                System.out.println(timeInMillis);
                toast.send("Alarm will flash in " + timeInMillis / 3_600_000 + ":" + timeInMillis % 3_600_000 / 60_000);
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
                        if (device != null) {
                            System.out.println("Found device " + device.getName());
                            listDialog.add(device.getName());
                            btAdapter.addNewDevice(device);
                        }
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
                    case ACTION_CONNECTED:
                        btAdapter.send("O");
                        break;
                }
            }
        };
    }
}
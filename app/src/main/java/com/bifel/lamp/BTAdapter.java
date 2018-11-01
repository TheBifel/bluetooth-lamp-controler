package com.bifel.lamp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class BTAdapter {

    private final static Intent ENABLE_BLUETOOTH_INTENT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    private final static String ERROR_MESSAGE = "Bluetooth device not found";
    private final static String STANDARD_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private Thread readLoopThread;
    private Thread connectToBTThread;
    private BluetoothAdapter mBluetoothAdapter;
    private ToastSender toast;
    private OutputStream mmOutputStream;
    private Context context;
    private Map<String, BluetoothDevice> devices = new HashMap<>();
    private Map<String, BluetoothDevice> notPairedDevices = new HashMap<>();

    public BTAdapter(Context context, BluetoothAdapter mBluetoothAdapter) {
        this.mBluetoothAdapter = mBluetoothAdapter;
        toast = new ToastSender(context, context.getMainLooper());
        this.context = context;

        if (!mBluetoothAdapter.isEnabled()) {
            context.startActivity(ENABLE_BLUETOOTH_INTENT); // request turn on bluetooth
        }
    }

    public void destroy() {
        if (readLoopThread != null) {
            readLoopThread.interrupt();
        }
    }

    public void cancelDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
    }

    public boolean isOutputStreamActive() {
        return mmOutputStream != null;
    }

    public void connectToDevice(final CharSequence name) {
        cancelDiscovery();
        if (connectToBTThread != null && connectToBTThread.isAlive()) {
            toast.send("Still trying connect");
            return;
        }
        connectToBTThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (notPairedDevices.containsKey(name.toString())) {
                    pairDevice(name);
                    devices.put(name.toString(), Objects.requireNonNull(notPairedDevices.get(name.toString())));
                }
                final BluetoothDevice mmDevice = devices.get(name.toString());
                try {
                    BluetoothSocket mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(STANDARD_UUID)); //Standard SerialPortService ID
                    mmSocket.connect();
                    mmOutputStream = mmSocket.getOutputStream();
                    if (readLoopThread != null) {
                        readLoopThread.interrupt();
                        readLoopThread = null;
                    }
                    beginListenForData(mmSocket.getInputStream());
                    sendIntentToMainActivity(MainActivity.ACTION_CLOSE_LIST_DIALOG);
                    toast.send("BT Name: " + mmDevice.getName() + "\nBT Address: " + mmDevice.getAddress());
                } catch (IOException e) {
                    toast.send("Cant connect to " + mmDevice.getName());
                }
            }
        });
        connectToBTThread.start();
    }

    public void sendData(String message) {
        if (mmOutputStream != null) {
            try {
                mmOutputStream.write(message.getBytes());
            } catch (IOException e) {
                toast.send("Cant write to output stream");
                e.printStackTrace();
            }
        } else {
            toast.send(ERROR_MESSAGE);
        }
    }

    public void getAlreadyPairedBluetoothDevices() {
        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            if (device != null) {
                devices.put(device.getName(), device);
            }
        }
    }

    public void addNewDevice(BluetoothDevice device) {
        if (devices.containsKey(device.getName())) {
            notPairedDevices.put(device.getName(), device);
        }
    }

    public void startDiscovery() {
        devices.clear();
        notPairedDevices.clear();
        getAlreadyPairedBluetoothDevices();
        mBluetoothAdapter.startDiscovery();
    }

    private void pairDevice(CharSequence name) {
        final BluetoothDevice device = devices.get(name.toString());
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            toast.send("Cant pair to this device");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void sendIntentToMainActivity(String action, String data) {
        Intent intent = new Intent(action);
        intent.putExtra(MainActivity.EXTRA_TEXT, data);
        context.sendBroadcast(intent);
    }

    @SuppressWarnings("SameParameterValue")
    private void sendIntentToMainActivity(String action) {
        Intent intent = new Intent(action);
        context.sendBroadcast(intent);
    }

    private void beginListenForData(final InputStream mmInputStream) {

        readLoopThread = new Thread(new Runnable() {
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(mmInputStream));
                while (readLoopThread.isAlive()) {
                    try {
                        sendIntentToMainActivity(MainActivity.ACTION_DATA_RECEIVE, reader.readLine());
                    } catch (IOException ignored) {}
                }
            }
        });
        readLoopThread.start();
    }
}

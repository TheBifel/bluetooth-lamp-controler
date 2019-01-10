package com.bifel.lamp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import com.bifel.lamp.activity.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BTAdapter {

    private final static Intent ENABLE_BLUETOOTH_INTENT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    private final static String ERROR_MESSAGE = "Bluetooth device not found";
    private final static String STANDARD_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private Thread readLoopThread;
    private Thread connectToBTThread;
    private BluetoothAdapter mBluetoothAdapter;
    private ToastSender toast;
    private OutputStream outputStream;
    private Context context;
    private Map<String, BluetoothDevice> devices = new HashMap<>();

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
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void cancelDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
    }

    public boolean isOutputStreamActive() {
        return outputStream != null;
    }

    public void connectToDevice(final String name) {
        cancelDiscovery();
        if (connectToBTThread != null && connectToBTThread.isAlive()) {
            toast.send("Still trying connect");
            return;
        }
        connectToBTThread = new Thread(new Runnable() {
            @Override
            public void run() {

                final BluetoothDevice device = devices.get(name);
                if (device == null){
                    toast.send("Try connect again");
                    return;
                }
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    pairDevice(device);
                }

                try {
                    BluetoothSocket mmSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(STANDARD_UUID));
                    mmSocket.connect();
                    outputStream = mmSocket.getOutputStream();
                    if (readLoopThread != null) {
                        readLoopThread.interrupt();
                        readLoopThread = null;
                    }
                    beginListenForData(mmSocket.getInputStream());
                    sendIntentToMainActivity(MainActivity.ACTION_CLOSE_LIST_DIALOG);
                    sendIntentToMainActivity(MainActivity.ACTION_CONNECTED);
                    toast.send("BT Name: " + device.getName() + "\nBT Address: " + device.getAddress());
                } catch (IOException e) {
                    toast.send("Cant connect to " + device.getName());
                }
            }
        });
        connectToBTThread.start();
    }

    public void send(String message) {
        if (outputStream != null) {
            try {
                message = message + '\n';
                outputStream.write(message.getBytes());
                System.out.println(message);
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
            devices.put(device.getName(), device);
    }

    public void startDiscovery() {
        devices.clear();
        getAlreadyPairedBluetoothDevices();
        mBluetoothAdapter.startDiscovery();
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            device.createBond();
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
                while (readLoopThread != null && readLoopThread.isAlive()) {
                    try {
                        sendIntentToMainActivity(MainActivity.ACTION_DATA_RECEIVE, reader.readLine());
                    } catch (IOException ignored) {}
                }
            }
        });
        readLoopThread.start();
    }
}

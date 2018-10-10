package com.bifel.lamp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BTAdapter {

    private final static Intent ENABLE_BLUETOOTH_INTENT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    private final static String ERROR_MESSAGE = "Bluetooth device not found";
    private final static String STANDARD_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private Thread readLoopThread;
    private BluetoothAdapter mBluetoothAdapter;
    private ToastSender toast;
    private OutputStream mmOutputStream;
    private Context context;
    private Map<String, BluetoothDevice> devices = new HashMap<>();
    private Map<String, BluetoothDevice> notPairedDevices = new HashMap<>();

    public BTAdapter(BluetoothAdapter mBluetoothAdapter, Context context) {
        this.mBluetoothAdapter = mBluetoothAdapter;
        toast = new ToastSender(context.getMainLooper(), context);
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
        Thread findBTThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final BluetoothDevice mmDevice = devices.get(name.toString());
                try {
                    BluetoothSocket mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(STANDARD_UUID)); //Standard SerialPortService ID
                    mmSocket.connect();
                    mmOutputStream = mmSocket.getOutputStream();
                    beginListenForData(mmSocket.getInputStream());
                    toast.send("BT Name: " + mmDevice.getName() + "\nBT Address: " + mmDevice.getAddress());
                } catch (IOException e) {
                    toast.send("Cant connect to " + mmDevice.getName());
                }
            }
        });
        findBTThread.start();
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

    public List<CharSequence> getAlreadyPairedBluetoothDevices() {
        List<CharSequence> pairedDevices = new ArrayList<>();

        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            if (device != null) {
                pairedDevices.add(device.getName());
                devices.put(device.getName(), device);
            }
        }

        return pairedDevices;
    }

    public void addNewDevice(BluetoothDevice device) {
        notPairedDevices.put(device.getName(), device);
    }

    public void startDiscovery() {
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

    private void sendIntentToMainActivity(String data) {
        Intent intent = new Intent(MainActivity.ACTION_DATA_RECEIVE);
        intent.putExtra(MainActivity.EXTRA_TEXT, data);
        context.sendBroadcast(intent);
    }

    private void beginListenForData(final InputStream mmInputStream) {
        readLoopThread = new Thread(new Runnable() {
            private String data;

            public void run() {
                while (!readLoopThread.isInterrupted()) {
                    try {
                        byte[] packetBytes = new byte[6];
                        //noinspection ResultOfMethodCallIgnored
                        mmInputStream.read(packetBytes);
                        data = new String(packetBytes, "US-ASCII");
                        System.out.println("Read data - " + data);

                        sendIntentToMainActivity(data);

                    } catch (IOException ignored) {
                    }
                }
            }
        });
        readLoopThread.start();
    }

    public void intentEcho(String text) {
        sendIntentToMainActivity(text);
    }


}

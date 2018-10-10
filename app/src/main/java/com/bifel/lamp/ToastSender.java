package com.bifel.lamp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ToastSender {
    private Handler toastHandler;
    private Context context;

    public ToastSender(Looper mainLooper, Context context) {
        this.toastHandler = new Handler(mainLooper);
        this.context = context;
    }

    public void send(final String massage) {
        toastHandler.post(new Runnable() {
            public void run() {
                Toast.makeText(context, massage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

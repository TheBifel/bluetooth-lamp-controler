package com.bifel.lamp;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.bifel.lamp.activity.MainActivity;

import java.util.Collection;

public class ListDialog extends Dialog {

    private ListView list;
    private View progressBar;
    private ImageView btnRefresh;
    private Context context;
    private ArrayAdapter<CharSequence> adapter;


    public ListDialog(Context context) {
        super(context);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.bt_devices_dialog);

        list = findViewById(R.id.list);
        adapter = new ArrayAdapter<>(context, R.layout.bt_list_item);
        progressBar = findViewById(R.id.progressBar);
        btnRefresh = findViewById(R.id.imgRefresh);

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIntentToMainActivity(MainActivity.ACTION_START_DISCOVERY);
            }
        });

        list.setAdapter(adapter);
        this.context = context;
    }

    @Override
    protected void onStop() {
        super.onStop();
        sendIntentToMainActivity(MainActivity.ACTION_STOP_DISCOVERY);
    }

    public void setOnClick(AdapterView.OnItemClickListener listener) {
        list.setOnItemClickListener(listener);
    }

    public CharSequence getItem(int position) {
        return adapter.getItem(position);
    }

    public void setDiscoveryMonitor(boolean isVisible) {
        progressBar.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        btnRefresh.setVisibility(isVisible ? View.INVISIBLE : View.VISIBLE);
    }

    public void add(CharSequence item) {
        adapter.add(item);
        adapter.notifyDataSetChanged();
    }

    @SuppressWarnings("unused")
    public void addAll(Collection<CharSequence> item) {
        adapter.addAll(item);
        adapter.notifyDataSetChanged();
    }

    public void clear() {
        adapter.clear();
    }

    private void sendIntentToMainActivity(String action) {
        Intent intent = new Intent(action);
        context.sendBroadcast(intent);
    }
}

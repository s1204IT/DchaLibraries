package com.android.bluetooth.map;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ExpandableListView;
import com.android.bluetooth.R;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class BluetoothMapEmailSettings extends Activity {
    private static final boolean D = true;
    private static final String TAG = "BluetoothMapEmailSettings";
    private static final boolean V = false;
    LinkedHashMap<BluetoothMapEmailSettingsItem, ArrayList<BluetoothMapEmailSettingsItem>> mGroups;
    BluetoothMapEmailSettingsLoader mLoader = new BluetoothMapEmailSettingsLoader(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_map_email_settings);
        this.mGroups = this.mLoader.parsePackages(true);
        ExpandableListView listView = (ExpandableListView) findViewById(R.id.bluetooth_map_email_settings_list_view);
        BluetoothMapEmailSettingsAdapter adapter = new BluetoothMapEmailSettingsAdapter(this, listView, this.mGroups, this.mLoader.getAccountsEnabledCount());
        listView.setAdapter(adapter);
    }
}

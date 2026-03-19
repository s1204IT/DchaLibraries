package com.android.bluetooth.map;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ExpandableListView;
import com.android.bluetooth.R;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class BluetoothMapSettings extends Activity {
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final String TAG = "BluetoothMapSettings";
    private static final boolean V = false;
    LinkedHashMap<BluetoothMapAccountItem, ArrayList<BluetoothMapAccountItem>> mGroups;
    BluetoothMapAccountLoader mLoader = new BluetoothMapAccountLoader(this);

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.bluetooth_map_settings);
        this.mGroups = this.mLoader.parsePackages(true);
        ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.bluetooth_map_settings_list_view);
        expandableListView.setAdapter(new BluetoothMapSettingsAdapter(this, expandableListView, this.mGroups, this.mLoader.getAccountsEnabledCount()));
    }
}

package com.android.companiondevicemanager;

import android.app.Activity;
import android.companion.BluetoothDeviceFilterUtils;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.companiondevicemanager.DeviceDiscoveryService;
import com.android.internal.util.Preconditions;

public class DeviceChooserActivity extends Activity {
    private View mCancelButton;
    ListView mDeviceListView;
    View mLoadingIndicator = null;
    private View mPairButton;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getService().mDevicesFound.isEmpty()) {
            Log.e("DeviceChooserActivity", "About to show UI, but no devices to show");
        }
        getWindow().addPrivateFlags(524288);
        if (getService().mRequest.isSingleDevice()) {
            setContentView(R.layout.device_confirmation);
            DeviceDiscoveryService.DeviceFilterPair deviceFilterPair = getService().mDevicesFound.get(0);
            setTitle(Html.fromHtml(getString(R.string.confirmation_title, Html.escapeHtml(getCallingAppName()), Html.escapeHtml(deviceFilterPair.getDisplayName())), 0));
            this.mPairButton = findViewById(R.id.button_pair);
            this.mPairButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    DeviceChooserActivity deviceChooserActivity = this.f$0;
                    deviceChooserActivity.onDeviceConfirmed(deviceChooserActivity.getService().mSelectedDevice);
                }
            });
            getService().mSelectedDevice = deviceFilterPair;
            onSelectionUpdate();
        } else {
            setContentView(R.layout.device_chooser);
            this.mPairButton = findViewById(R.id.button_pair);
            this.mPairButton.setVisibility(8);
            setTitle(Html.fromHtml(getString(R.string.chooser_title, Html.escapeHtml(getCallingAppName())), 0));
            this.mDeviceListView = (ListView) findViewById(R.id.device_list);
            DeviceDiscoveryService.DevicesAdapter devicesAdapter = getService().mDevicesAdapter;
            this.mDeviceListView.setAdapter((ListAdapter) devicesAdapter);
            devicesAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    DeviceChooserActivity.this.onSelectionUpdate();
                }
            });
            ListView listView = this.mDeviceListView;
            ProgressBar progressBar = getProgressBar();
            this.mLoadingIndicator = progressBar;
            listView.addFooterView(progressBar, null, false);
        }
        getService().mActivity = this;
        this.mCancelButton = findViewById(R.id.button_cancel);
        this.mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.cancel();
            }
        });
    }

    private void cancel() {
        getService().onCancel();
        setResult(0);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing() && !isChangingConfigurations()) {
            cancel();
        }
    }

    private CharSequence getCallingAppName() {
        try {
            PackageManager packageManager = getPackageManager();
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo((String) Preconditions.checkStringNotEmpty(getCallingPackage(), "This activity must be called for result"), 0));
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        TextView textView = (TextView) findViewById(R.id.title);
        int padding = getPadding(getResources());
        textView.setPadding(padding, padding, padding, padding);
        textView.setText(charSequence);
    }

    private ProgressBar getProgressBar() {
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setForegroundGravity(1);
        int padding = getPadding(getResources());
        progressBar.setPadding(padding, padding, padding, padding);
        return progressBar;
    }

    static int getPadding(Resources resources) {
        return resources.getDimensionPixelSize(R.dimen.padding);
    }

    private void onSelectionUpdate() {
        DeviceDiscoveryService.DeviceFilterPair deviceFilterPair = getService().mSelectedDevice;
        if (this.mPairButton.getVisibility() != 0 && deviceFilterPair != null) {
            onDeviceConfirmed(deviceFilterPair);
        } else {
            this.mPairButton.setEnabled(deviceFilterPair != null);
        }
    }

    private DeviceDiscoveryService getService() {
        return DeviceDiscoveryService.sInstance;
    }

    protected void onDeviceConfirmed(DeviceDiscoveryService.DeviceFilterPair deviceFilterPair) {
        getService().onDeviceSelected(getCallingPackage(), BluetoothDeviceFilterUtils.getDeviceMacAddress(deviceFilterPair.device));
        setResult(-1, new Intent().putExtra("android.companion.extra.DEVICE", deviceFilterPair.device));
        finish();
    }
}

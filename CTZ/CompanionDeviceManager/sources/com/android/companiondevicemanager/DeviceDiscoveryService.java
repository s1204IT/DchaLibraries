package com.android.companiondevicemanager;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.BluetoothDeviceFilterUtils;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.DeviceFilter;
import android.companion.ICompanionDeviceDiscoveryService;
import android.companion.ICompanionDeviceDiscoveryServiceCallback;
import android.companion.IFindDeviceCallback;
import android.companion.WifiDeviceFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.android.companiondevicemanager.DeviceDiscoveryService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class DeviceDiscoveryService extends Service {
    static DeviceDiscoveryService sInstance;
    private List<BluetoothLeDeviceFilter> mBLEFilters;
    private ScanCallback mBLEScanCallback;
    private List<ScanFilter> mBLEScanFilters;
    private BluetoothLeScanner mBLEScanner;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothBroadcastReceiver mBluetoothBroadcastReceiver;
    private List<BluetoothDeviceFilter> mBluetoothFilters;
    DevicesAdapter mDevicesAdapter;
    List<DeviceFilterPair> mDevicesFound;
    private List<DeviceFilter<?>> mFilters;
    IFindDeviceCallback mFindCallback;
    AssociationRequest mRequest;
    DeviceFilterPair mSelectedDevice;
    ICompanionDeviceDiscoveryServiceCallback mServiceCallback;
    private WifiBroadcastReceiver mWifiBroadcastReceiver;
    private List<WifiDeviceFilter> mWifiFilters;
    private WifiManager mWifiManager;
    private ScanSettings mDefaultScanSettings = new ScanSettings.Builder().build();
    boolean mIsScanning = false;
    DeviceChooserActivity mActivity = null;
    private final ICompanionDeviceDiscoveryService mBinder = new ICompanionDeviceDiscoveryService.Stub() {
        public void startDiscovery(AssociationRequest associationRequest, String str, IFindDeviceCallback iFindDeviceCallback, ICompanionDeviceDiscoveryServiceCallback iCompanionDeviceDiscoveryServiceCallback) {
            DeviceDiscoveryService.this.mFindCallback = iFindDeviceCallback;
            DeviceDiscoveryService.this.mServiceCallback = iCompanionDeviceDiscoveryServiceCallback;
            DeviceDiscoveryService.this.startDiscovery(associationRequest);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder.asBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBluetoothAdapter = ((BluetoothManager) getSystemService(BluetoothManager.class)).getAdapter();
        this.mBLEScanner = this.mBluetoothAdapter.getBluetoothLeScanner();
        this.mWifiManager = (WifiManager) getSystemService(WifiManager.class);
        this.mDevicesFound = new ArrayList();
        this.mDevicesAdapter = new DevicesAdapter();
        sInstance = this;
    }

    private void startDiscovery(AssociationRequest associationRequest) {
        String address;
        if (!associationRequest.equals(this.mRequest)) {
            this.mRequest = associationRequest;
            this.mFilters = associationRequest.getDeviceFilters();
            this.mWifiFilters = CollectionUtils.filter(this.mFilters, WifiDeviceFilter.class);
            this.mBluetoothFilters = CollectionUtils.filter(this.mFilters, BluetoothDeviceFilter.class);
            this.mBLEFilters = CollectionUtils.filter(this.mFilters, BluetoothLeDeviceFilter.class);
            this.mBLEScanFilters = CollectionUtils.map(this.mBLEFilters, new Function() {
                @Override
                public final Object apply(Object obj) {
                    return ((BluetoothLeDeviceFilter) obj).getScanFilter();
                }
            });
            reset();
        }
        if (!ArrayUtils.isEmpty(this.mDevicesFound)) {
            onReadyToShowUI();
        }
        if (this.mRequest.isSingleDevice()) {
            int size = CollectionUtils.size(this.mBluetoothFilters);
            for (int i = 0; i < size; i++) {
                BluetoothDeviceFilter bluetoothDeviceFilter = this.mBluetoothFilters.get(i);
                if (!TextUtils.isEmpty(bluetoothDeviceFilter.getAddress())) {
                    address = bluetoothDeviceFilter.getAddress();
                    break;
                }
            }
            address = null;
        } else {
            address = null;
        }
        if (address != null) {
            Iterator it = CollectionUtils.emptyIfNull(this.mBluetoothAdapter.getBondedDevices()).iterator();
            while (it.hasNext()) {
                onDeviceFound(DeviceFilterPair.findMatch((BluetoothDevice) it.next(), this.mBluetoothFilters));
            }
        }
        if (shouldScan(this.mBluetoothFilters)) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.bluetooth.device.action.FOUND");
            intentFilter.addAction("android.bluetooth.device.action.DISAPPEARED");
            this.mBluetoothBroadcastReceiver = new BluetoothBroadcastReceiver();
            registerReceiver(this.mBluetoothBroadcastReceiver, intentFilter);
            this.mBluetoothAdapter.startDiscovery();
        }
        if (shouldScan(this.mBLEFilters) && this.mBLEScanner != null) {
            this.mBLEScanCallback = new BLEScanCallback();
            this.mBLEScanner.startScan(this.mBLEScanFilters, this.mDefaultScanSettings, this.mBLEScanCallback);
        }
        if (shouldScan(this.mWifiFilters)) {
            this.mWifiBroadcastReceiver = new WifiBroadcastReceiver();
            registerReceiver(this.mWifiBroadcastReceiver, new IntentFilter("android.net.wifi.SCAN_RESULTS"));
            this.mWifiManager.startScan();
        }
        this.mIsScanning = true;
        Handler.getMain().sendMessageDelayed(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((DeviceDiscoveryService) obj).stopScan();
            }
        }, this), 20000L);
    }

    private boolean shouldScan(List<? extends DeviceFilter> list) {
        return !ArrayUtils.isEmpty(list) || ArrayUtils.isEmpty(this.mFilters);
    }

    private void reset() {
        stopScan();
        this.mDevicesFound.clear();
        this.mSelectedDevice = null;
        notifyDataSetChanged();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopScan();
        return super.onUnbind(intent);
    }

    private void stopScan() {
        if (this.mIsScanning) {
            this.mIsScanning = false;
            DeviceChooserActivity deviceChooserActivity = this.mActivity;
            if (deviceChooserActivity != null) {
                if (deviceChooserActivity.mDeviceListView != null) {
                    deviceChooserActivity.mDeviceListView.removeFooterView(deviceChooserActivity.mLoadingIndicator);
                }
                this.mActivity = null;
            }
            this.mBluetoothAdapter.cancelDiscovery();
            if (this.mBluetoothBroadcastReceiver != null) {
                unregisterReceiver(this.mBluetoothBroadcastReceiver);
                this.mBluetoothBroadcastReceiver = null;
            }
            if (this.mBLEScanner != null) {
                this.mBLEScanner.stopScan(this.mBLEScanCallback);
            }
            if (this.mWifiBroadcastReceiver != null) {
                unregisterReceiver(this.mWifiBroadcastReceiver);
                this.mWifiBroadcastReceiver = null;
            }
        }
    }

    private void onDeviceFound(DeviceFilterPair deviceFilterPair) {
        if (deviceFilterPair == null || this.mDevicesFound.contains(deviceFilterPair)) {
            return;
        }
        if (this.mDevicesFound.isEmpty()) {
            onReadyToShowUI();
        }
        this.mDevicesFound.add(deviceFilterPair);
        notifyDataSetChanged();
    }

    private void notifyDataSetChanged() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((DeviceDiscoveryService.DevicesAdapter) obj).notifyDataSetChanged();
            }
        }, this.mDevicesAdapter));
    }

    private void onReadyToShowUI() {
        try {
            this.mFindCallback.onSuccess(PendingIntent.getActivity(this, 0, new Intent(this, (Class<?>) DeviceChooserActivity.class), 1409286144));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void onDeviceLost(DeviceFilterPair deviceFilterPair) {
        this.mDevicesFound.remove(deviceFilterPair);
        notifyDataSetChanged();
    }

    void onDeviceSelected(String str, String str2) {
        try {
            this.mServiceCallback.onDeviceSelected(str, getUserId(), str2);
        } catch (RemoteException e) {
            Log.e("DeviceDiscoveryService", "Failed to record association: " + str + " <-> " + str2);
        }
    }

    void onCancel() {
        try {
            this.mServiceCallback.onDeviceSelectionCancel();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    class DevicesAdapter extends ArrayAdapter<DeviceFilterPair> {
        private Drawable BLUETOOTH_ICON;
        private Drawable WIFI_ICON;

        private Drawable icon(int i) {
            Drawable drawable = DeviceDiscoveryService.this.getResources().getDrawable(i, null);
            drawable.setTint(-12303292);
            return drawable;
        }

        public DevicesAdapter() {
            super(DeviceDiscoveryService.this, 0, DeviceDiscoveryService.this.mDevicesFound);
            this.BLUETOOTH_ICON = icon(android.R.drawable.stat_sys_data_bluetooth);
            this.WIFI_ICON = icon(android.R.drawable.ic_media_route_connecting_dark_30_mtrl);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            boolean z = view instanceof TextView;
            ?? NewView = view;
            if (!z) {
                NewView = newView();
            }
            bind(NewView, (DeviceFilterPair) getItem(i));
            return NewView;
        }

        private void bind(TextView textView, final DeviceFilterPair deviceFilterPair) {
            int i;
            Drawable drawable;
            textView.setText(deviceFilterPair.getDisplayName());
            if (deviceFilterPair.equals(DeviceDiscoveryService.this.mSelectedDevice)) {
                i = -7829368;
            } else {
                i = 0;
            }
            textView.setBackgroundColor(i);
            if (deviceFilterPair.device instanceof ScanResult) {
                drawable = this.WIFI_ICON;
            } else {
                drawable = this.BLUETOOTH_ICON;
            }
            textView.setCompoundDrawablesWithIntrinsicBounds(drawable, (Drawable) null, (Drawable) null, (Drawable) null);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    DeviceDiscoveryService.DevicesAdapter.lambda$bind$0(this.f$0, deviceFilterPair, view);
                }
            });
        }

        public static void lambda$bind$0(DevicesAdapter devicesAdapter, DeviceFilterPair deviceFilterPair, View view) {
            DeviceDiscoveryService.this.mSelectedDevice = deviceFilterPair;
            devicesAdapter.notifyDataSetChanged();
        }

        private TextView newView() {
            TextView textView = new TextView(DeviceDiscoveryService.this);
            textView.setTextColor(-16777216);
            int padding = DeviceChooserActivity.getPadding(DeviceDiscoveryService.this.getResources());
            textView.setPadding(padding, padding, padding, padding);
            textView.setCompoundDrawablePadding(padding);
            return textView;
        }
    }

    static class DeviceFilterPair<T extends Parcelable> {
        public final T device;
        public final DeviceFilter<T> filter;

        private DeviceFilterPair(T t, DeviceFilter<T> deviceFilter) {
            this.device = t;
            this.filter = deviceFilter;
        }

        public static <T extends Parcelable> DeviceFilterPair<T> findMatch(final T t, List<? extends DeviceFilter<T>> list) {
            if (ArrayUtils.isEmpty(list)) {
                return new DeviceFilterPair<>(t, null);
            }
            DeviceFilter deviceFilter = (DeviceFilter) CollectionUtils.find(list, new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ((DeviceFilter) obj).matches(t);
                }
            });
            if (deviceFilter != null) {
                return new DeviceFilterPair<>(t, deviceFilter);
            }
            return null;
        }

        public String getDisplayName() {
            if (this.filter == null) {
                Preconditions.checkNotNull(this.device);
                if (this.device instanceof BluetoothDevice) {
                    return BluetoothDeviceFilterUtils.getDeviceDisplayNameInternal((BluetoothDevice) this.device);
                }
                if (this.device instanceof ScanResult) {
                    return BluetoothDeviceFilterUtils.getDeviceDisplayNameInternal((ScanResult) this.device);
                }
                if (this.device instanceof android.bluetooth.le.ScanResult) {
                    return BluetoothDeviceFilterUtils.getDeviceDisplayNameInternal(((android.bluetooth.le.ScanResult) this.device).getDevice());
                }
                throw new IllegalArgumentException("Unknown device type: " + this.device.getClass());
            }
            return this.filter.getDeviceDisplayName(this.device);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return Objects.equals(BluetoothDeviceFilterUtils.getDeviceMacAddress(this.device), BluetoothDeviceFilterUtils.getDeviceMacAddress(((DeviceFilterPair) obj).device));
        }

        public int hashCode() {
            return Objects.hash(BluetoothDeviceFilterUtils.getDeviceMacAddress(this.device));
        }

        public String toString() {
            return "DeviceFilterPair{device=" + this.device + ", filter=" + this.filter + '}';
        }
    }

    private class BLEScanCallback extends ScanCallback {
        public BLEScanCallback() {
        }

        @Override
        public void onScanResult(int i, android.bluetooth.le.ScanResult scanResult) {
            DeviceFilterPair deviceFilterPairFindMatch = DeviceFilterPair.findMatch(scanResult, DeviceDiscoveryService.this.mBLEFilters);
            if (deviceFilterPairFindMatch == null) {
                return;
            }
            if (i == 4) {
                DeviceDiscoveryService.this.onDeviceLost(deviceFilterPairFindMatch);
            } else {
                DeviceDiscoveryService.this.onDeviceFound(deviceFilterPairFindMatch);
            }
        }
    }

    private class BluetoothBroadcastReceiver extends BroadcastReceiver {
        private BluetoothBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            DeviceFilterPair deviceFilterPairFindMatch = DeviceFilterPair.findMatch((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE"), DeviceDiscoveryService.this.mBluetoothFilters);
            if (deviceFilterPairFindMatch == null) {
                return;
            }
            if (intent.getAction().equals("android.bluetooth.device.action.FOUND")) {
                DeviceDiscoveryService.this.onDeviceFound(deviceFilterPairFindMatch);
            } else {
                DeviceDiscoveryService.this.onDeviceLost(deviceFilterPairFindMatch);
            }
        }
    }

    private class WifiBroadcastReceiver extends BroadcastReceiver {
        private WifiBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.net.wifi.SCAN_RESULTS")) {
                List<ScanResult> scanResults = DeviceDiscoveryService.this.mWifiManager.getScanResults();
                for (int i = 0; i < scanResults.size(); i++) {
                    DeviceDiscoveryService.this.onDeviceFound(DeviceFilterPair.findMatch(scanResults.get(i), DeviceDiscoveryService.this.mWifiFilters));
                }
            }
        }
    }
}

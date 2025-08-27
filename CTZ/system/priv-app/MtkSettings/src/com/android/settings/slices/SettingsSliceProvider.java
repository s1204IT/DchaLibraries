package com.android.settings.slices;

import android.app.slice.SliceManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Log;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import com.android.settings.bluetooth.BluetoothSliceBuilder;
import com.android.settings.location.LocationSliceBuilder;
import com.android.settings.notification.ZenModeSliceBuilder;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.WifiSliceBuilder;
import com.android.settings.wifi.calling.WifiCallingSliceHelper;
import com.android.settingslib.SliceBroadcastRelay;
import com.android.settingslib.utils.ThreadUtils;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/* loaded from: classes.dex */
public class SettingsSliceProvider extends SliceProvider {
    private final KeyValueListParser mParser;
    final Set<Uri> mRegisteredUris;
    Map<Uri, SliceData> mSliceDataCache;
    Map<Uri, SliceData> mSliceWeakDataCache;
    SlicesDatabaseAccessor mSlicesDatabaseAccessor;

    public SettingsSliceProvider() {
        super("android.permission.READ_SEARCH_INDEXABLES");
        this.mRegisteredUris = new ArraySet();
        this.mParser = new KeyValueListParser(',');
    }

    @Override // androidx.slice.SliceProvider
    public boolean onCreateSliceProvider() {
        this.mSlicesDatabaseAccessor = new SlicesDatabaseAccessor(getContext());
        this.mSliceDataCache = new ConcurrentHashMap();
        this.mSliceWeakDataCache = new WeakHashMap();
        return true;
    }

    @Override // androidx.slice.SliceProvider
    public Uri onMapIntentToUri(Intent intent) {
        try {
            return ((SliceManager) getContext().getSystemService(SliceManager.class)).mapIntentToUri(SliceDeepLinkSpringBoard.parse(intent.getData(), getContext().getPackageName()));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override // androidx.slice.SliceProvider
    public void onSlicePinned(Uri uri) {
        if (WifiSliceBuilder.WIFI_URI.equals(uri)) {
            registerIntentToUri(WifiSliceBuilder.INTENT_FILTER, uri);
            return;
        }
        if (ZenModeSliceBuilder.ZEN_MODE_URI.equals(uri)) {
            registerIntentToUri(ZenModeSliceBuilder.INTENT_FILTER, uri);
        } else if (BluetoothSliceBuilder.BLUETOOTH_URI.equals(uri)) {
            registerIntentToUri(BluetoothSliceBuilder.INTENT_FILTER, uri);
        } else {
            loadSliceInBackground(uri);
        }
    }

    @Override // androidx.slice.SliceProvider
    public void onSliceUnpinned(Uri uri) {
        if (this.mRegisteredUris.contains(uri)) {
            Log.d("SettingsSliceProvider", "Unregistering uri broadcast relay: " + uri);
            SliceBroadcastRelay.unregisterReceivers(getContext(), uri);
            this.mRegisteredUris.remove(uri);
        }
        this.mSliceDataCache.remove(uri);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [222=9] */
    @Override // androidx.slice.SliceProvider
    public Slice onBindSlice(Uri uri) {
        StrictMode.ThreadPolicy threadPolicy = StrictMode.getThreadPolicy();
        try {
            if (!ThreadUtils.isMainThread()) {
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
            }
            if (getBlockedKeys().contains(uri.getLastPathSegment())) {
                Log.e("SettingsSliceProvider", "Requested blocked slice with Uri: " + uri);
                return null;
            }
            if (WifiCallingSliceHelper.WIFI_CALLING_URI.equals(uri)) {
                return FeatureFactory.getFactory(getContext()).getSlicesFeatureProvider().getNewWifiCallingSliceHelper(getContext()).createWifiCallingSlice(uri);
            }
            if (WifiSliceBuilder.WIFI_URI.equals(uri)) {
                return WifiSliceBuilder.getSlice(getContext());
            }
            if (ZenModeSliceBuilder.ZEN_MODE_URI.equals(uri)) {
                return ZenModeSliceBuilder.getSlice(getContext());
            }
            if (BluetoothSliceBuilder.BLUETOOTH_URI.equals(uri)) {
                return BluetoothSliceBuilder.getSlice(getContext());
            }
            if (LocationSliceBuilder.LOCATION_URI.equals(uri)) {
                return LocationSliceBuilder.getSlice(getContext());
            }
            SliceData sliceData = this.mSliceWeakDataCache.get(uri);
            if (sliceData == null) {
                loadSliceInBackground(uri);
                return getSliceStub(uri);
            }
            if (!this.mSliceDataCache.containsKey(uri)) {
                this.mSliceWeakDataCache.remove(uri);
            }
            return SliceBuilderUtils.buildSlice(getContext(), sliceData);
        } finally {
            StrictMode.setThreadPolicy(threadPolicy);
        }
    }

    @Override // androidx.slice.SliceProvider
    public Collection<Uri> onGetSliceDescendants(Uri uri) {
        ArrayList arrayList = new ArrayList();
        if (SliceBuilderUtils.getPathData(uri) != null) {
            arrayList.add(uri);
            return arrayList;
        }
        String authority = uri.getAuthority();
        String path = uri.getPath();
        boolean zIsEmpty = path.isEmpty();
        if (zIsEmpty && TextUtils.isEmpty(authority)) {
            List<String> sliceKeys = this.mSlicesDatabaseAccessor.getSliceKeys(true);
            List<String> sliceKeys2 = this.mSlicesDatabaseAccessor.getSliceKeys(false);
            arrayList.addAll(buildUrisFromKeys(sliceKeys, "android.settings.slices"));
            arrayList.addAll(buildUrisFromKeys(sliceKeys2, "com.android.settings.slices"));
            arrayList.addAll(getSpecialCaseUris(true));
            arrayList.addAll(getSpecialCaseUris(false));
            return arrayList;
        }
        if (!zIsEmpty && !TextUtils.equals(path, "/action") && !TextUtils.equals(path, "/intent")) {
            return arrayList;
        }
        boolean zEquals = TextUtils.equals(authority, "android.settings.slices");
        arrayList.addAll(buildUrisFromKeys(this.mSlicesDatabaseAccessor.getSliceKeys(zEquals), authority));
        arrayList.addAll(getSpecialCaseUris(zEquals));
        return arrayList;
    }

    private List<Uri> buildUrisFromKeys(List<String> list, String str) {
        ArrayList arrayList = new ArrayList();
        Uri.Builder builderAppendPath = new Uri.Builder().scheme("content").authority(str).appendPath("action");
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            builderAppendPath.path("action/" + it.next());
            arrayList.add(builderAppendPath.build());
        }
        return arrayList;
    }

    void loadSlice(Uri uri) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        try {
            SliceData sliceDataFromUri = this.mSlicesDatabaseAccessor.getSliceDataFromUri(uri);
            IntentFilter intentFilter = SliceBuilderUtils.getPreferenceController(getContext(), sliceDataFromUri).getIntentFilter();
            if (intentFilter != null) {
                registerIntentToUri(intentFilter, uri);
            }
            if (((SliceManager) getContext().getSystemService(SliceManager.class)).getPinnedSlices().contains(uri)) {
                this.mSliceDataCache.put(uri, sliceDataFromUri);
            }
            this.mSliceWeakDataCache.put(uri, sliceDataFromUri);
            getContext().getContentResolver().notifyChange(uri, null);
            Log.d("SettingsSliceProvider", "Built slice (" + uri + ") in: " + (System.currentTimeMillis() - jCurrentTimeMillis));
        } catch (IllegalStateException e) {
            Log.e("SettingsSliceProvider", "Could not get slice data for uri: " + uri, e);
        }
    }

    void loadSliceInBackground(final Uri uri) {
        ThreadUtils.postOnBackgroundThread(new Runnable() { // from class: com.android.settings.slices.-$$Lambda$SettingsSliceProvider$3mq4GNawZ0Wc-zLrSLnj1f92or0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.loadSlice(uri);
            }
        });
    }

    private Slice getSliceStub(Uri uri) {
        return new Slice.Builder(uri).build();
    }

    private List<Uri> getSpecialCaseUris(boolean z) {
        if (z) {
            return getSpecialCasePlatformUris();
        }
        return getSpecialCaseOemUris();
    }

    private List<Uri> getSpecialCasePlatformUris() {
        return Arrays.asList(WifiSliceBuilder.WIFI_URI, BluetoothSliceBuilder.BLUETOOTH_URI, LocationSliceBuilder.LOCATION_URI);
    }

    private List<Uri> getSpecialCaseOemUris() {
        return Arrays.asList(ZenModeSliceBuilder.ZEN_MODE_URI);
    }

    void registerIntentToUri(IntentFilter intentFilter, Uri uri) {
        Log.d("SettingsSliceProvider", "Registering Uri for broadcast relay: " + uri);
        this.mRegisteredUris.add(uri);
        SliceBroadcastRelay.registerReceiver(getContext(), uri, SliceRelayReceiver.class, intentFilter);
    }

    Set<String> getBlockedKeys() {
        String string = Settings.Global.getString(getContext().getContentResolver(), "blocked_slices");
        ArraySet arraySet = new ArraySet();
        try {
            this.mParser.setString(string);
            Collections.addAll(arraySet, parseStringArray(string));
            return arraySet;
        } catch (IllegalArgumentException e) {
            Log.e("SettingsSliceProvider", "Bad Settings Slices Whitelist flags", e);
            return arraySet;
        }
    }

    private String[] parseStringArray(String str) {
        if (str != null) {
            String[] strArrSplit = str.split(":");
            if (strArrSplit.length > 0) {
                return strArrSplit;
            }
        }
        return new String[0];
    }
}

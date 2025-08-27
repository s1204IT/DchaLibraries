package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.connecteddevice.usb.UsbConnectionBroadcastReceiver;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class UsbDetailsFragment extends DashboardFragment {
    private List<UsbDetailsController> mControllers;
    private UsbBackend mUsbBackend;
    private UsbConnectionBroadcastReceiver.UsbConnectionListener mUsbConnectionListener = new UsbConnectionBroadcastReceiver.UsbConnectionListener() { // from class: com.android.settings.connecteddevice.usb.-$$Lambda$UsbDetailsFragment$0qs6NXPaSCNUBBPVeTrwViGe6pk
        @Override // com.android.settings.connecteddevice.usb.UsbConnectionBroadcastReceiver.UsbConnectionListener
        public final void onUsbConnectionChanged(boolean z, long j, int i, int i2) {
            UsbDetailsFragment.lambda$new$0(this.f$0, z, j, i, i2);
        }
    };
    UsbConnectionBroadcastReceiver mUsbReceiver;
    private static final String TAG = UsbDetailsFragment.class.getSimpleName();
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.connecteddevice.usb.UsbDetailsFragment.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.usb_details_fragment;
            return Lists.newArrayList(new SearchIndexableResource[]{searchIndexableResource});
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            return super.getNonIndexableKeys(context);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return new ArrayList(UsbDetailsFragment.createControllerList(context, new UsbBackend(context), null));
        }
    };

    public static /* synthetic */ void lambda$new$0(UsbDetailsFragment usbDetailsFragment, boolean z, long j, int i, int i2) {
        Iterator<UsbDetailsController> it = usbDetailsFragment.mControllers.iterator();
        while (it.hasNext()) {
            it.next().refresh(z, j, i, i2);
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1291;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return TAG;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.usb_details_fragment;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, android.support.v14.preference.PreferenceFragment
    public void onCreatePreferences(Bundle bundle, String str) {
        super.onCreatePreferences(bundle, str);
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        this.mUsbBackend = new UsbBackend(context);
        this.mControllers = createControllerList(context, this.mUsbBackend, this);
        this.mUsbReceiver = new UsbConnectionBroadcastReceiver(context, this.mUsbConnectionListener, this.mUsbBackend);
        getLifecycle().addObserver(this.mUsbReceiver);
        return new ArrayList(this.mControllers);
    }

    private static List<UsbDetailsController> createControllerList(Context context, UsbBackend usbBackend, UsbDetailsFragment usbDetailsFragment) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new UsbDetailsHeaderController(context, usbDetailsFragment, usbBackend));
        arrayList.add(new UsbDetailsDataRoleController(context, usbDetailsFragment, usbBackend));
        arrayList.add(new UsbDetailsFunctionsController(context, usbDetailsFragment, usbBackend));
        arrayList.add(new UsbDetailsPowerRoleController(context, usbDetailsFragment, usbBackend));
        return arrayList;
    }
}

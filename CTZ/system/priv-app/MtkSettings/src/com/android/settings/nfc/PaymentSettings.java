package com.android.settings.nfc;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.nfc.PaymentBackend;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class PaymentSettings extends SettingsPreferenceFragment implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.nfc.PaymentSettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            Resources resources = context.getResources();
            SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
            searchIndexableRaw.key = "payment";
            searchIndexableRaw.title = resources.getString(R.string.nfc_payment_settings_title);
            searchIndexableRaw.screenTitle = resources.getString(R.string.nfc_payment_settings_title);
            searchIndexableRaw.keywords = resources.getString(R.string.keywords_payment_settings);
            arrayList.add(searchIndexableRaw);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            if (context.getPackageManager().hasSystemFeature("android.hardware.nfc")) {
                return nonIndexableKeys;
            }
            nonIndexableKeys.add("payment");
            return nonIndexableKeys;
        }
    };
    private PaymentBackend mPaymentBackend;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 70;
    }

    @Override // com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.nfc_payment_settings;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mPaymentBackend = new PaymentBackend(getActivity());
        setHasOptionsMenu(true);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        List<PaymentBackend.PaymentAppInfo> paymentAppInfos = this.mPaymentBackend.getPaymentAppInfos();
        if (paymentAppInfos != null && paymentAppInfos.size() > 0) {
            NfcPaymentPreference nfcPaymentPreference = new NfcPaymentPreference(getPrefContext(), this.mPaymentBackend);
            nfcPaymentPreference.setKey("payment");
            preferenceScreen.addPreference(nfcPaymentPreference);
            preferenceScreen.addPreference(new NfcForegroundPreference(getPrefContext(), this.mPaymentBackend));
        }
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        ViewGroup viewGroup = (ViewGroup) getListView().getParent();
        View viewInflate = getActivity().getLayoutInflater().inflate(R.layout.nfc_payment_empty, viewGroup, false);
        viewGroup.addView(viewInflate);
        setEmptyView(viewInflate);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mPaymentBackend.onResume();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mPaymentBackend.onPause();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        MenuItem menuItemAdd = menu.add(R.string.nfc_payment_how_it_works);
        menuItemAdd.setIntent(new Intent(getActivity(), (Class<?>) HowItWorks.class));
        menuItemAdd.setShowAsActionFlags(0);
    }
}

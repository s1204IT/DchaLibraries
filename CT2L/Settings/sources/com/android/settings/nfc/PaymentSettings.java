package com.android.settings.nfc;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.internal.content.PackageMonitor;
import com.android.settings.HelpUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.nfc.PaymentBackend;
import java.util.List;

public class PaymentSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, View.OnClickListener {
    private LayoutInflater mInflater;
    private PaymentBackend mPaymentBackend;
    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();
    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            PaymentSettings.this.refresh();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPaymentBackend = new PaymentBackend(getActivity());
        this.mInflater = (LayoutInflater) getSystemService("layout_inflater");
        setHasOptionsMenu(true);
    }

    public void refresh() {
        PreferenceManager manager = getPreferenceManager();
        PreferenceScreen screen = manager.createPreferenceScreen(getActivity());
        List<PaymentBackend.PaymentAppInfo> appInfos = this.mPaymentBackend.getPaymentAppInfos();
        if (appInfos != null && appInfos.size() > 0) {
            for (PaymentBackend.PaymentAppInfo appInfo : appInfos) {
                PaymentAppPreference preference = new PaymentAppPreference(getActivity(), appInfo, this);
                preference.setTitle(appInfo.caption);
                if (appInfo.banner != null) {
                    screen.addPreference(preference);
                } else {
                    Log.e("PaymentSettings", "Couldn't load banner drawable of service " + appInfo.componentName);
                }
            }
        }
        TextView emptyText = (TextView) getView().findViewById(R.id.nfc_payment_empty_text);
        TextView learnMore = (TextView) getView().findViewById(R.id.nfc_payment_learn_more);
        ImageView emptyImage = (ImageView) getView().findViewById(R.id.nfc_payment_tap_image);
        if (screen.getPreferenceCount() == 0) {
            emptyText.setVisibility(0);
            learnMore.setVisibility(0);
            emptyImage.setVisibility(0);
            getListView().setVisibility(8);
        } else {
            SwitchPreference foreground = new SwitchPreference(getActivity());
            boolean foregroundMode = this.mPaymentBackend.isForegroundMode();
            foreground.setPersistent(false);
            foreground.setTitle(getString(R.string.nfc_payment_favor_foreground));
            foreground.setChecked(foregroundMode);
            foreground.setOnPreferenceChangeListener(this);
            screen.addPreference(foreground);
            emptyText.setVisibility(8);
            learnMore.setVisibility(8);
            emptyImage.setVisibility(8);
            getListView().setVisibility(0);
        }
        setPreferenceScreen(screen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = this.mInflater.inflate(R.layout.nfc_payment, container, false);
        TextView learnMore = (TextView) v.findViewById(R.id.nfc_payment_learn_more);
        learnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v2) {
                String helpUrl = PaymentSettings.this.getResources().getString(R.string.help_url_nfc_payment);
                if (!TextUtils.isEmpty(helpUrl)) {
                    Uri fullUri = HelpUtils.uriWithAddedParameters(PaymentSettings.this.getActivity(), Uri.parse(helpUrl));
                    Intent intent = new Intent("android.intent.action.VIEW", fullUri);
                    intent.setFlags(276824064);
                    PaymentSettings.this.startActivity(intent);
                    return;
                }
                Log.e("PaymentSettings", "Help url not set.");
            }
        });
        return v;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() instanceof PaymentBackend.PaymentAppInfo) {
            PaymentBackend.PaymentAppInfo appInfo = (PaymentBackend.PaymentAppInfo) v.getTag();
            if (appInfo.componentName != null) {
                this.mPaymentBackend.setDefaultPaymentApp(appInfo.componentName);
            }
            refresh();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        refresh();
    }

    @Override
    public void onPause() {
        this.mSettingsPackageMonitor.unregister();
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        String searchUri = Settings.Secure.getString(getContentResolver(), "payment_service_search_uri");
        if (!TextUtils.isEmpty(searchUri)) {
            MenuItem menuItem = menu.add(R.string.nfc_payment_menu_item_add_service);
            menuItem.setShowAsActionFlags(1);
            menuItem.setIntent(new Intent("android.intent.action.VIEW", Uri.parse(searchUri)));
        }
    }

    private class SettingsPackageMonitor extends PackageMonitor {
        private SettingsPackageMonitor() {
        }

        public void onPackageAdded(String packageName, int uid) {
            PaymentSettings.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageAppeared(String packageName, int reason) {
            PaymentSettings.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageDisappeared(String packageName, int reason) {
            PaymentSettings.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageRemoved(String packageName, int uid) {
            PaymentSettings.this.mHandler.obtainMessage().sendToTarget();
        }
    }

    public static class PaymentAppPreference extends Preference {
        private final PaymentBackend.PaymentAppInfo appInfo;
        private final View.OnClickListener listener;

        public PaymentAppPreference(Context context, PaymentBackend.PaymentAppInfo appInfo, View.OnClickListener listener) {
            super(context);
            setLayoutResource(R.layout.nfc_payment_option);
            this.appInfo = appInfo;
            this.listener = listener;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            RadioButton radioButton = (RadioButton) view.findViewById(android.R.id.button1);
            radioButton.setChecked(this.appInfo.isDefault);
            radioButton.setOnClickListener(this.listener);
            radioButton.setTag(this.appInfo);
            ImageView banner = (ImageView) view.findViewById(R.id.banner);
            banner.setImageDrawable(this.appInfo.banner);
            banner.setOnClickListener(this.listener);
            banner.setTag(this.appInfo);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!(preference instanceof SwitchPreference)) {
            return false;
        }
        this.mPaymentBackend.setForegroundMode(((Boolean) newValue).booleanValue());
        return true;
    }
}

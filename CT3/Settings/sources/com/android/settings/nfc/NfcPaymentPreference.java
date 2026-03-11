package com.android.settings.nfc;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import com.android.settings.CustomDialogPreference;
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend;
import com.mediatek.settings.FeatureOption;
import java.util.List;

public class NfcPaymentPreference extends CustomDialogPreference implements PaymentBackend.Callback, View.OnClickListener {
    private final NfcPaymentAdapter mAdapter;
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final PaymentBackend mPaymentBackend;
    private ImageView mSettingsButtonView;

    public NfcPaymentPreference(Context context, PaymentBackend backend) {
        super(context, null);
        this.mPaymentBackend = backend;
        this.mContext = context;
        backend.registerCallback(this);
        this.mAdapter = new NfcPaymentAdapter();
        setDialogTitle(context.getString(R.string.nfc_payment_pay_with));
        this.mLayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        setWidgetLayoutResource(R.layout.preference_widget_settings);
        refresh();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        this.mSettingsButtonView = (ImageView) view.findViewById(R.id.settings_button);
        this.mSettingsButtonView.setOnClickListener(this);
        updateSettingsVisibility();
    }

    public void refresh() {
        List<PaymentBackend.PaymentAppInfo> appInfos = this.mPaymentBackend.getPaymentAppInfos();
        PaymentBackend.PaymentAppInfo defaultApp = this.mPaymentBackend.getDefaultApp();
        if (appInfos != null) {
            PaymentBackend.PaymentAppInfo[] apps = (PaymentBackend.PaymentAppInfo[]) appInfos.toArray(new PaymentBackend.PaymentAppInfo[appInfos.size()]);
            this.mAdapter.updateApps(apps, defaultApp);
        }
        setTitle(R.string.nfc_payment_default);
        if (defaultApp != null) {
            setSummary(defaultApp.label);
        } else {
            setSummary(this.mContext.getString(R.string.nfc_payment_default_not_set));
        }
        updateSettingsVisibility();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        builder.setSingleChoiceItems(this.mAdapter, 0, listener);
    }

    @Override
    public void onPaymentAppsChanged() {
        refresh();
    }

    @Override
    public void onClick(View view) {
        PaymentBackend.PaymentAppInfo defaultAppInfo = this.mPaymentBackend.getDefaultApp();
        if (defaultAppInfo == null || defaultAppInfo.settingsComponent == null) {
            return;
        }
        Intent settingsIntent = new Intent("android.intent.action.MAIN");
        settingsIntent.setComponent(defaultAppInfo.settingsComponent);
        settingsIntent.addFlags(268435456);
        try {
            this.mContext.startActivity(settingsIntent);
        } catch (ActivityNotFoundException e) {
            Log.e("NfcPaymentPreference", "Settings activity not found.");
        }
    }

    void updateSettingsVisibility() {
        if (this.mSettingsButtonView == null) {
            return;
        }
        PaymentBackend.PaymentAppInfo defaultApp = this.mPaymentBackend.getDefaultApp();
        if (defaultApp == null || defaultApp.settingsComponent == null) {
            this.mSettingsButtonView.setVisibility(8);
        } else {
            this.mSettingsButtonView.setVisibility(0);
        }
    }

    class NfcPaymentAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener, View.OnClickListener, View.OnLongClickListener {
        private PaymentBackend.PaymentAppInfo[] appInfos;

        public NfcPaymentAdapter() {
        }

        public void updateApps(PaymentBackend.PaymentAppInfo[] appInfos, PaymentBackend.PaymentAppInfo currentDefault) {
            this.appInfos = appInfos;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return this.appInfos.length;
        }

        @Override
        public PaymentBackend.PaymentAppInfo getItem(int i) {
            return this.appInfos[i];
        }

        @Override
        public long getItemId(int i) {
            return this.appInfos[i].componentName.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            PaymentBackend.PaymentAppInfo appInfo = this.appInfos[position];
            if (convertView == null) {
                convertView = NfcPaymentPreference.this.mLayoutInflater.inflate(R.layout.nfc_payment_option, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) convertView.findViewById(R.id.banner);
                holder.radioButton = (RadioButton) convertView.findViewById(R.id.button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.imageView.setImageDrawable(appInfo.banner);
            holder.imageView.setTag(appInfo);
            holder.imageView.setContentDescription(appInfo.label);
            holder.imageView.setOnClickListener(this);
            holder.radioButton.setOnCheckedChangeListener(null);
            holder.radioButton.setChecked(appInfo.isDefault);
            holder.radioButton.setContentDescription(appInfo.label);
            holder.radioButton.setOnCheckedChangeListener(this);
            holder.radioButton.setTag(appInfo);
            if (FeatureOption.MTK_NFC_GSMA_SUPPORT) {
                holder.imageView.setOnLongClickListener(this);
                holder.radioButton.setOnLongClickListener(this);
            }
            return convertView;
        }

        public class ViewHolder {
            public ImageView imageView;
            public RadioButton radioButton;

            public ViewHolder() {
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            PaymentBackend.PaymentAppInfo appInfo = (PaymentBackend.PaymentAppInfo) compoundButton.getTag();
            makeDefault(appInfo);
        }

        @Override
        public void onClick(View view) {
            PaymentBackend.PaymentAppInfo appInfo = (PaymentBackend.PaymentAppInfo) view.getTag();
            makeDefault(appInfo);
        }

        void makeDefault(PaymentBackend.PaymentAppInfo appInfo) {
            if (!appInfo.isDefault) {
                NfcPaymentPreference.this.mPaymentBackend.setDefaultPaymentApp(appInfo.componentName);
            }
            NfcPaymentPreference.this.getDialog().dismiss();
        }

        @Override
        public boolean onLongClick(View view) {
            PaymentBackend.PaymentAppInfo appInfo = (PaymentBackend.PaymentAppInfo) view.getTag();
            Log.d("NfcPaymentPreference", "onLongClick " + appInfo.componentName.toString());
            Intent intent = new Intent("com.gsma.services.nfc.SELECT_DEFAULT_SERVICE");
            List<ResolveInfo> apps = NfcPaymentPreference.this.mContext.getPackageManager().queryIntentActivities(intent, 0);
            if (apps != null && apps.size() != 0) {
                for (ResolveInfo app : apps) {
                    String packageName = app.activityInfo.packageName;
                    Log.d("NfcPaymentPreference", "app packageName: " + packageName);
                    if (appInfo.componentName.getPackageName().equals(packageName)) {
                        try {
                            Intent startIntent = new Intent();
                            startIntent.setAction("com.gsma.services.nfc.SELECT_DEFAULT_SERVICE");
                            startIntent.setPackage(packageName);
                            NfcPaymentPreference.this.getContext().startActivity(startIntent);
                            return true;
                        } catch (ActivityNotFoundException e) {
                            Log.d("NfcPaymentPreference", "ActivityNotFoundException");
                            return true;
                        }
                    }
                }
                return true;
            }
            return true;
        }
    }
}

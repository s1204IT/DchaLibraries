package com.android.settings.nfc;

import android.app.AlertDialog;
import android.app.Dialog;
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
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend;
import com.android.settingslib.CustomDialogPreference;
import com.mediatek.settings.FeatureOption;
import java.util.List;

public class NfcPaymentPreference extends CustomDialogPreference implements View.OnClickListener, PaymentBackend.Callback {
    private final NfcPaymentAdapter mAdapter;
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final PaymentBackend mPaymentBackend;
    private ImageView mSettingsButtonView;

    public NfcPaymentPreference(Context context, PaymentBackend paymentBackend) {
        super(context, null);
        this.mPaymentBackend = paymentBackend;
        this.mContext = context;
        paymentBackend.registerCallback(this);
        this.mAdapter = new NfcPaymentAdapter();
        setDialogTitle(context.getString(R.string.nfc_payment_pay_with));
        this.mLayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        setWidgetLayoutResource(R.layout.preference_widget_gear);
        refresh();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mSettingsButtonView = (ImageView) preferenceViewHolder.findViewById(R.id.settings_button);
        this.mSettingsButtonView.setOnClickListener(this);
        updateSettingsVisibility();
    }

    public void refresh() {
        List<PaymentBackend.PaymentAppInfo> paymentAppInfos = this.mPaymentBackend.getPaymentAppInfos();
        PaymentBackend.PaymentAppInfo defaultApp = this.mPaymentBackend.getDefaultApp();
        if (paymentAppInfos != null) {
            this.mAdapter.updateApps((PaymentBackend.PaymentAppInfo[]) paymentAppInfos.toArray(new PaymentBackend.PaymentAppInfo[paymentAppInfos.size()]), defaultApp);
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
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
        super.onPrepareDialogBuilder(builder, onClickListener);
        builder.setSingleChoiceItems(this.mAdapter, 0, onClickListener);
    }

    @Override
    public void onPaymentAppsChanged() {
        refresh();
    }

    @Override
    public void onClick(View view) {
        PaymentBackend.PaymentAppInfo defaultApp = this.mPaymentBackend.getDefaultApp();
        if (defaultApp != null && defaultApp.settingsComponent != null) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(defaultApp.settingsComponent);
            intent.addFlags(268435456);
            try {
                this.mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e("NfcPaymentPreference", "Settings activity not found.");
            }
        }
    }

    void updateSettingsVisibility() {
        if (this.mSettingsButtonView != null) {
            PaymentBackend.PaymentAppInfo defaultApp = this.mPaymentBackend.getDefaultApp();
            if (defaultApp == null || defaultApp.settingsComponent == null) {
                this.mSettingsButtonView.setVisibility(8);
            } else {
                this.mSettingsButtonView.setVisibility(0);
            }
        }
    }

    class NfcPaymentAdapter extends BaseAdapter implements View.OnClickListener, View.OnLongClickListener, CompoundButton.OnCheckedChangeListener {
        private PaymentBackend.PaymentAppInfo[] appInfos;

        public NfcPaymentAdapter() {
        }

        public void updateApps(PaymentBackend.PaymentAppInfo[] paymentAppInfoArr, PaymentBackend.PaymentAppInfo paymentAppInfo) {
            this.appInfos = paymentAppInfoArr;
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
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            PaymentBackend.PaymentAppInfo paymentAppInfo = this.appInfos[i];
            if (view == null) {
                view = NfcPaymentPreference.this.mLayoutInflater.inflate(R.layout.nfc_payment_option, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.imageView = (ImageView) view.findViewById(R.id.banner);
                viewHolder.radioButton = (RadioButton) view.findViewById(R.id.button);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.imageView.setImageDrawable(paymentAppInfo.banner);
            viewHolder.imageView.setTag(paymentAppInfo);
            viewHolder.imageView.setContentDescription(paymentAppInfo.label);
            viewHolder.imageView.setOnClickListener(this);
            if (FeatureOption.MTK_ST_NFC_GSMA_SUPPORT) {
                viewHolder.imageView.setOnLongClickListener(this);
            }
            viewHolder.radioButton.setOnCheckedChangeListener(null);
            viewHolder.radioButton.setChecked(paymentAppInfo.isDefault);
            viewHolder.radioButton.setContentDescription(paymentAppInfo.label);
            viewHolder.radioButton.setOnCheckedChangeListener(this);
            viewHolder.radioButton.setTag(paymentAppInfo);
            return view;
        }

        public class ViewHolder {
            public ImageView imageView;
            public RadioButton radioButton;

            public ViewHolder() {
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            makeDefault((PaymentBackend.PaymentAppInfo) compoundButton.getTag());
        }

        @Override
        public void onClick(View view) {
            makeDefault((PaymentBackend.PaymentAppInfo) view.getTag());
        }

        void makeDefault(PaymentBackend.PaymentAppInfo paymentAppInfo) {
            if (!paymentAppInfo.isDefault) {
                NfcPaymentPreference.this.mPaymentBackend.setDefaultPaymentApp(paymentAppInfo.componentName);
            }
            Dialog dialog = NfcPaymentPreference.this.getDialog();
            if (dialog != null) {
                dialog.dismiss();
            }
        }

        @Override
        public boolean onLongClick(View view) {
            PaymentBackend.PaymentAppInfo paymentAppInfo = (PaymentBackend.PaymentAppInfo) view.getTag();
            if (paymentAppInfo.componentName != null) {
                Log.d("NfcPaymentPreference", "onLongClick " + paymentAppInfo.componentName.toString());
                Intent intent = new Intent("com.gsma.services.nfc.SELECT_DEFAULT_SERVICE");
                intent.addFlags(32);
                List<ResolveInfo> listQueryIntentActivities = NfcPaymentPreference.this.mContext.getPackageManager().queryIntentActivities(intent, 65536);
                if (listQueryIntentActivities != null && listQueryIntentActivities.size() != 0) {
                    for (ResolveInfo resolveInfo : listQueryIntentActivities) {
                        String str = resolveInfo.activityInfo.packageName;
                        if (paymentAppInfo.componentName.getPackageName().equals(str)) {
                            intent.setClassName(str, resolveInfo.activityInfo.name);
                            try {
                                NfcPaymentPreference.this.mContext.startActivity(intent);
                                return true;
                            } catch (ActivityNotFoundException e) {
                                Log.e("NfcPaymentPreference", "Activity not found.");
                                return true;
                            }
                        }
                    }
                    return true;
                }
                return true;
            }
            return true;
        }
    }
}

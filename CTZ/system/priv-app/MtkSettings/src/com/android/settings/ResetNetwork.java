package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.enterprise.ActionDisabledByAdminDialogHelper;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.RestrictedLockUtils;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class ResetNetwork extends InstrumentedFragment {
    private View mContentView;
    private CheckBox mEsimCheckbox;
    private View mEsimContainer;
    private Button mInitiateButton;
    private final View.OnClickListener mInitiateListener = new View.OnClickListener() { // from class: com.android.settings.ResetNetwork.1
        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (!ResetNetwork.this.runKeyguardConfirmation(55)) {
                ResetNetwork.this.showFinalConfirmation();
            }
        }
    };
    private Spinner mSubscriptionSpinner;
    private List<SubscriptionInfo> mSubscriptions;

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().setTitle(R.string.reset_network_title);
    }

    private boolean runKeyguardConfirmation(int i) {
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(i, getActivity().getResources().getText(R.string.reset_network_title));
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i != 55) {
            return;
        }
        if (i2 == -1) {
            showFinalConfirmation();
        } else {
            establishInitialState();
        }
    }

    private void showFinalConfirmation() {
        Bundle bundle = new Bundle();
        if (this.mSubscriptions != null && this.mSubscriptions.size() > 0) {
            bundle.putInt("subscription", this.mSubscriptions.get(this.mSubscriptionSpinner.getSelectedItemPosition()).getSubscriptionId());
        }
        bundle.putBoolean("erase_esim", this.mEsimCheckbox.isChecked());
        new SubSettingLauncher(getContext()).setDestination(ResetNetworkConfirm.class.getName()).setArguments(bundle).setTitle(R.string.reset_network_confirm_title).setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    private void establishInitialState() {
        this.mSubscriptionSpinner = (Spinner) this.mContentView.findViewById(R.id.reset_network_subscription);
        this.mEsimContainer = this.mContentView.findViewById(R.id.erase_esim_container);
        this.mEsimCheckbox = (CheckBox) this.mContentView.findViewById(R.id.erase_esim);
        this.mSubscriptions = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoList();
        if (this.mSubscriptions != null && this.mSubscriptions.size() > 0) {
            int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (!SubscriptionManager.isUsableSubIdValue(defaultDataSubscriptionId)) {
                defaultDataSubscriptionId = SubscriptionManager.getDefaultVoiceSubscriptionId();
            }
            if (!SubscriptionManager.isUsableSubIdValue(defaultDataSubscriptionId)) {
                defaultDataSubscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId();
            }
            if (!SubscriptionManager.isUsableSubIdValue(defaultDataSubscriptionId)) {
                defaultDataSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
            }
            this.mSubscriptions.size();
            ArrayList arrayList = new ArrayList();
            int size = 0;
            for (SubscriptionInfo subscriptionInfo : this.mSubscriptions) {
                if (subscriptionInfo.getSubscriptionId() == defaultDataSubscriptionId) {
                    size = arrayList.size();
                }
                String string = subscriptionInfo.getDisplayName().toString();
                if (TextUtils.isEmpty(string)) {
                    string = subscriptionInfo.getNumber();
                }
                if (TextUtils.isEmpty(string)) {
                    string = subscriptionInfo.getCarrierName().toString();
                }
                if (TextUtils.isEmpty(string)) {
                    string = String.format("MCC:%s MNC:%s Slot:%s Id:%s", Integer.valueOf(subscriptionInfo.getMcc()), Integer.valueOf(subscriptionInfo.getMnc()), Integer.valueOf(subscriptionInfo.getSimSlotIndex()), Integer.valueOf(subscriptionInfo.getSubscriptionId()));
                }
                arrayList.add(string);
            }
            ArrayAdapter arrayAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, arrayList);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.mSubscriptionSpinner.setAdapter((SpinnerAdapter) arrayAdapter);
            this.mSubscriptionSpinner.setSelection(size);
            if (this.mSubscriptions.size() > 1) {
                this.mSubscriptionSpinner.setVisibility(0);
            } else {
                this.mSubscriptionSpinner.setVisibility(4);
            }
        } else {
            this.mSubscriptionSpinner.setVisibility(4);
        }
        this.mInitiateButton = (Button) this.mContentView.findViewById(R.id.initiate_reset_network);
        this.mInitiateButton.setOnClickListener(this.mInitiateListener);
        if (showEuiccSettings(getContext())) {
            this.mEsimContainer.setVisibility(0);
            ((TextView) this.mContentView.findViewById(R.id.erase_esim_title)).setText(R.string.reset_esim_title);
            this.mEsimContainer.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.ResetNetwork.2
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    ResetNetwork.this.mEsimCheckbox.toggle();
                }
            });
            return;
        }
        this.mEsimCheckbox.setChecked(false);
    }

    private boolean showEuiccSettings(Context context) {
        if (!((EuiccManager) context.getSystemService("euicc")).isEnabled()) {
            return false;
        }
        ContentResolver contentResolver = context.getContentResolver();
        return (Settings.Global.getInt(contentResolver, "euicc_provisioned", 0) == 0 && Settings.Global.getInt(contentResolver, "development_settings_enabled", 0) == 0) ? false : true;
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        UserManager userManager = UserManager.get(getActivity());
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_network_reset", UserHandle.myUserId());
        if (!userManager.isAdminUser() || RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_network_reset", UserHandle.myUserId())) {
            return layoutInflater.inflate(R.layout.network_reset_disallowed_screen, (ViewGroup) null);
        }
        if (enforcedAdminCheckIfRestrictionEnforced != null) {
            new ActionDisabledByAdminDialogHelper(getActivity()).prepareDialogBuilder("no_network_reset", enforcedAdminCheckIfRestrictionEnforced).setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.android.settings.-$$Lambda$ResetNetwork$sNSFVrhYYO7NxbKY35cdb4I6sYI
                @Override // android.content.DialogInterface.OnDismissListener
                public final void onDismiss(DialogInterface dialogInterface) {
                    this.f$0.getActivity().finish();
                }
            }).show();
            return new View(getContext());
        }
        this.mContentView = layoutInflater.inflate(R.layout.reset_network, (ViewGroup) null);
        establishInitialState();
        return this.mContentView;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 83;
    }
}

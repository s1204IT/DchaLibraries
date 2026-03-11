package com.android.settings;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.settingslib.RestrictedLockUtils;
import java.util.ArrayList;
import java.util.List;

public class ResetNetwork extends OptionsMenuFragment {
    private View mContentView;
    private Button mInitiateButton;
    private final View.OnClickListener mInitiateListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (ResetNetwork.this.runKeyguardConfirmation(55)) {
                return;
            }
            ResetNetwork.this.showFinalConfirmation();
        }
    };
    private Spinner mSubscriptionSpinner;
    private List<SubscriptionInfo> mSubscriptions;

    public boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(request, res.getText(R.string.reset_network_title));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 55) {
            return;
        }
        if (resultCode == -1) {
            showFinalConfirmation();
        } else {
            establishInitialState();
        }
    }

    public void showFinalConfirmation() {
        Bundle args = new Bundle();
        if (this.mSubscriptions != null && this.mSubscriptions.size() > 0) {
            int selectedIndex = this.mSubscriptionSpinner.getSelectedItemPosition();
            SubscriptionInfo subscription = this.mSubscriptions.get(selectedIndex);
            args.putInt("subscription", subscription.getSubscriptionId());
        }
        ((SettingsActivity) getActivity()).startPreferencePanel(ResetNetworkConfirm.class.getName(), args, R.string.reset_network_confirm_title, null, null, 0);
    }

    private void establishInitialState() {
        this.mSubscriptionSpinner = (Spinner) this.mContentView.findViewById(R.id.reset_network_subscription);
        this.mSubscriptions = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoList();
        if (this.mSubscriptions != null && this.mSubscriptions.size() > 0) {
            int defaultSubscription = SubscriptionManager.getDefaultDataSubscriptionId();
            if (!SubscriptionManager.isUsableSubIdValue(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultVoiceSubscriptionId();
            }
            if (!SubscriptionManager.isUsableSubIdValue(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultSmsSubscriptionId();
            }
            if (!SubscriptionManager.isUsableSubIdValue(defaultSubscription)) {
                defaultSubscription = SubscriptionManager.getDefaultSubscriptionId();
            }
            int selectedIndex = 0;
            this.mSubscriptions.size();
            List<String> subscriptionNames = new ArrayList<>();
            for (SubscriptionInfo record : this.mSubscriptions) {
                if (record.getSubscriptionId() == defaultSubscription) {
                    selectedIndex = subscriptionNames.size();
                }
                String name = record.getDisplayName().toString();
                if (TextUtils.isEmpty(name)) {
                    name = record.getNumber();
                }
                if (TextUtils.isEmpty(name)) {
                    name = record.getCarrierName().toString();
                }
                if (TextUtils.isEmpty(name)) {
                    name = String.format("MCC:%s MNC:%s Slot:%s Id:%s", Integer.valueOf(record.getMcc()), Integer.valueOf(record.getMnc()), Integer.valueOf(record.getSimSlotIndex()), Integer.valueOf(record.getSubscriptionId()));
                }
                subscriptionNames.add(name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, subscriptionNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.mSubscriptionSpinner.setAdapter((SpinnerAdapter) adapter);
            this.mSubscriptionSpinner.setSelection(selectedIndex);
            this.mSubscriptionSpinner.setVisibility(0);
        } else {
            this.mSubscriptionSpinner.setVisibility(4);
        }
        this.mInitiateButton = (Button) this.mContentView.findViewById(R.id.initiate_reset_network);
        this.mInitiateButton.setOnClickListener(this.mInitiateListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        UserManager um = UserManager.get(getActivity());
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_network_reset", UserHandle.myUserId());
        if (!um.isAdminUser() || RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_network_reset", UserHandle.myUserId())) {
            return inflater.inflate(R.layout.network_reset_disallowed_screen, (ViewGroup) null);
        }
        if (admin != null) {
            View view = inflater.inflate(R.layout.admin_support_details_empty_view, (ViewGroup) null);
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), view, admin, false);
            view.setVisibility(0);
            return view;
        }
        this.mContentView = inflater.inflate(R.layout.reset_network, (ViewGroup) null);
        establishInitialState();
        return this.mContentView;
    }

    @Override
    protected int getMetricsCategory() {
        return 83;
    }
}

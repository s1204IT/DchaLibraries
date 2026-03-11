package com.android.systemui.statusbar.policy;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import java.util.List;

public class EmergencyCryptkeeperText extends TextView {
    private final KeyguardUpdateMonitorCallback mCallback;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final BroadcastReceiver mReceiver;

    public EmergencyCryptkeeperText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onPhoneStateChanged(int i) {
                EmergencyCryptkeeperText.this.update();
            }

            @Override
            public void onRefreshCarrierInfo() {
                EmergencyCryptkeeperText.this.update();
            }
        };
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                    EmergencyCryptkeeperText.this.update();
                }
            }
        };
        setVisibility(8);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mKeyguardUpdateMonitor.registerCallback(this.mCallback);
        getContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mKeyguardUpdateMonitor != null) {
            this.mKeyguardUpdateMonitor.removeCallback(this.mCallback);
        }
        getContext().unregisterReceiver(this.mReceiver);
    }

    public void update() {
        boolean zIsNetworkSupported = ConnectivityManager.from(this.mContext).isNetworkSupported(0);
        boolean z = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
        if (zIsNetworkSupported && !z) {
            List<SubscriptionInfo> subscriptionInfo = this.mKeyguardUpdateMonitor.getSubscriptionInfo(false);
            int size = subscriptionInfo.size();
            boolean z2 = true;
            CharSequence text = null;
            for (int i = 0; i < size; i++) {
                IccCardConstants.State simState = this.mKeyguardUpdateMonitor.getSimState(subscriptionInfo.get(i).getSubscriptionId());
                CharSequence carrierName = subscriptionInfo.get(i).getCarrierName();
                if (simState.iccCardExist() && !TextUtils.isEmpty(carrierName)) {
                    z2 = false;
                    text = carrierName;
                }
            }
            if (z2) {
                if (size != 0) {
                    text = subscriptionInfo.get(0).getCarrierName();
                } else {
                    text = getContext().getText(R.string.capability_desc_canRequestTouchExploration);
                    Intent intentRegisterReceiver = getContext().registerReceiver(null, new IntentFilter("android.provider.Telephony.SPN_STRINGS_UPDATED"));
                    if (intentRegisterReceiver != null) {
                        text = intentRegisterReceiver.getStringExtra("plmn");
                    }
                }
            }
            setText(text);
            setVisibility(TextUtils.isEmpty(text) ? 8 : 0);
            return;
        }
        setText((CharSequence) null);
        setVisibility(8);
    }
}

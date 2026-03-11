package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class UsbModeChooserActivity extends Activity {
    public static final int[] DEFAULT_MODES = {2, 4, 6, 8, 10};
    private UsbBackend mBackend;
    private AlertDialog mDialog;
    private BroadcastReceiver mDisconnectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"android.hardware.usb.action.USB_STATE".equals(action)) {
                return;
            }
            boolean connected = intent.getBooleanExtra("connected", false);
            boolean hostConnected = intent.getBooleanExtra("host_connected", false);
            if (connected || hostConnected) {
                return;
            }
            UsbModeChooserActivity.this.mDialog.dismiss();
        }
    };
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    private LayoutInflater mLayoutInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mLayoutInflater = LayoutInflater.from(this);
        this.mDialog = new AlertDialog.Builder(this).setTitle(R.string.usb_use).setView(R.layout.usb_dialog_container).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                UsbModeChooserActivity.this.finish();
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UsbModeChooserActivity.this.finish();
            }
        }).create();
        this.mDialog.show();
        LinearLayout container = (LinearLayout) this.mDialog.findViewById(R.id.container);
        this.mEnforcedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(this, "no_usb_file_transfer", UserHandle.myUserId());
        this.mBackend = new UsbBackend(this);
        int current = this.mBackend.getCurrentMode();
        for (int i = 0; i < DEFAULT_MODES.length; i++) {
            if (this.mBackend.isModeSupported(DEFAULT_MODES[i]) && !this.mBackend.isModeDisallowedBySystem(DEFAULT_MODES[i])) {
                inflateOption(DEFAULT_MODES[i], current == DEFAULT_MODES[i], container, this.mBackend.isModeDisallowed(DEFAULT_MODES[i]));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_STATE");
        registerReceiver(this.mDisconnectedReceiver, filter);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(this.mDisconnectedReceiver);
        super.onStop();
    }

    private void inflateOption(final int mode, boolean selected, LinearLayout linearLayout, final boolean disallowedByAdmin) {
        View viewInflate = this.mLayoutInflater.inflate(R.layout.restricted_radio_with_summary, (ViewGroup) linearLayout, false);
        TextView titleView = (TextView) viewInflate.findViewById(android.R.id.title);
        titleView.setText(getTitle(mode));
        TextView summaryView = (TextView) viewInflate.findViewById(android.R.id.summary);
        summaryView.setText(getSummary(mode));
        if (disallowedByAdmin) {
            if (this.mEnforcedAdmin != null) {
                setDisabledByAdmin(viewInflate, titleView, summaryView);
            } else {
                return;
            }
        }
        viewInflate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (disallowedByAdmin && UsbModeChooserActivity.this.mEnforcedAdmin != null) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(UsbModeChooserActivity.this, UsbModeChooserActivity.this.mEnforcedAdmin);
                    return;
                }
                if (!ActivityManager.isUserAMonkey()) {
                    UsbModeChooserActivity.this.mBackend.setMode(mode);
                }
                UsbModeChooserActivity.this.mDialog.dismiss();
                UsbModeChooserActivity.this.finish();
            }
        });
        ((Checkable) viewInflate).setChecked(selected);
        linearLayout.addView(viewInflate);
    }

    private void setDisabledByAdmin(View rootView, TextView titleView, TextView summaryView) {
        if (this.mEnforcedAdmin == null) {
            return;
        }
        titleView.setEnabled(false);
        summaryView.setEnabled(false);
        rootView.findViewById(R.id.restricted_icon).setVisibility(0);
        Drawable[] compoundDrawables = titleView.getCompoundDrawablesRelative();
        compoundDrawables[0].mutate().setColorFilter(getColor(R.color.disabled_text_color), PorterDuff.Mode.MULTIPLY);
    }

    private static int getSummary(int mode) {
        switch (mode) {
            case DefaultWfcSettingsExt.RESUME:
                return R.string.usb_use_charging_only_desc;
            case DefaultWfcSettingsExt.PAUSE:
                return R.string.usb_use_power_only_desc;
            case DefaultWfcSettingsExt.CREATE:
                return R.string.usb_use_file_transfers_desc;
            case DefaultWfcSettingsExt.DESTROY:
            case 5:
            case 7:
            case 9:
            default:
                return 0;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                return R.string.usb_use_photo_transfers_desc;
            case 6:
                return R.string.usb_use_MIDI_desc;
            case 8:
                return R.string.usb_ums_summary;
            case 10:
                return R.string.usb_bicr_summary;
        }
    }

    private static int getTitle(int mode) {
        switch (mode) {
            case DefaultWfcSettingsExt.RESUME:
                return R.string.usb_use_charging_only;
            case DefaultWfcSettingsExt.PAUSE:
                return R.string.usb_use_power_only;
            case DefaultWfcSettingsExt.CREATE:
                return R.string.usb_use_file_transfers;
            case DefaultWfcSettingsExt.DESTROY:
            case 5:
            case 7:
            case 9:
            default:
                return 0;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                return R.string.usb_use_photo_transfers;
            case 6:
                return R.string.usb_use_MIDI;
            case 8:
                return R.string.usb_use_mass_storage;
            case 10:
                return R.string.usb_use_built_in_cd_rom;
        }
    }
}

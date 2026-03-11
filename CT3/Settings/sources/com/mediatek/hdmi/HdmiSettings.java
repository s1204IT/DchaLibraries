package com.mediatek.hdmi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioSystem;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.hdmi.IMtkHdmiManager;
import java.util.ArrayList;
import java.util.List;

public class HdmiSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private Activity mActivity;
    private ListPreference mAudioOutputPref;
    private Context mContext;
    private AlertDialog mHDMIExcludeDialog;
    private IMtkHdmiManager mHdmiManager;
    private boolean mRet;
    private SwitchPreference mToggleHdmiPref;
    private ListPreference mVideoResolutionPref;
    private ListPreference mVideoScalePref;
    private BroadcastReceiver mActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("HDMISettings", "receive: " + action);
            if (!"android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED".equals(action) || Settings.Global.getInt(HdmiSettings.this.mActivity.getContentResolver(), "wifi_display_on", 0) != 0 || !HdmiSettings.this.mRet) {
                return;
            }
            Log.d("HDMISettings", "wifi display disconnected");
            try {
                HdmiSettings.this.mHdmiManager.enableHdmi(true);
                HdmiSettings.this.mRet = false;
            } catch (RemoteException e) {
            }
        }
    };
    private ContentObserver mHdmiSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d("@M_HDMISettings", "mHdmiSettingsObserver onChanged: " + selfChange);
            HdmiSettings.this.updatePref();
        }
    };

    private void popupDialog(AlertDialog dialog) {
        dialog.getWindow().setType(2003);
        dialog.getWindow().getAttributes().privateFlags |= 16;
        dialog.show();
    }

    private void showDialog() {
        Resources mResource = Resources.getSystem();
        String messageString = this.mActivity.getResources().getString(R.string.hdmi_wfd_off_hdmi_on);
        this.mHDMIExcludeDialog = new AlertDialog.Builder(this.mContext).setMessage(messageString).setPositiveButton(mResource.getString(android.R.string.face_error_vendor_unknown), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("HDMISettings", "HDMI on, turn off WifiDisplay");
                Settings.Global.putInt(HdmiSettings.this.mContext.getContentResolver(), "wifi_display_on", 0);
                HdmiSettings.this.mRet = true;
            }
        }).setNegativeButton(mResource.getString(android.R.string.face_acquired_insufficient), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("HDMISettings", "HDMI on, user DON'T turn off WifiDisplay -> turn off HDMI");
                HdmiSettings.this.mRet = false;
                HdmiSettings.this.updatePref();
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                Log.d("HDMISettings", "onCancel(): user DON'T turn off WifiDisplay -> turn off HDMI");
                HdmiSettings.this.mRet = false;
                HdmiSettings.this.updatePref();
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
                Log.d("HDMISettings", "onDismiss()");
            }
        }).create();
        popupDialog(this.mHDMIExcludeDialog);
    }

    @Override
    protected int getMetricsCategory() {
        return 100005;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("@M_HDMISettings", "HdmiSettings.onCreate()");
        addPreferencesFromResource(R.xml.hdmi_settings);
        this.mActivity = getActivity();
        this.mContext = getActivity();
        this.mToggleHdmiPref = (SwitchPreference) findPreference("hdmi_toggler");
        this.mToggleHdmiPref.setOnPreferenceChangeListener(this);
        this.mVideoResolutionPref = (ListPreference) findPreference("video_resolution");
        this.mVideoResolutionPref.setOnPreferenceChangeListener(this);
        this.mVideoScalePref = (ListPreference) findPreference("video_scale");
        this.mVideoScalePref.setOnPreferenceChangeListener(this);
        this.mVideoScalePref.getEntries();
        CharSequence[] values = this.mVideoScalePref.getEntryValues();
        List<CharSequence> scaleEntries = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            if (Integer.parseInt(values[i].toString()) != 0) {
                scaleEntries.add(this.mActivity.getResources().getString(R.string.hdmi_scale_scale_down, values[i]));
            } else {
                scaleEntries.add(this.mActivity.getResources().getString(R.string.hdmi_scale_no_scale));
            }
        }
        this.mVideoScalePref.setEntries((CharSequence[]) scaleEntries.toArray(new CharSequence[scaleEntries.size()]));
        this.mAudioOutputPref = (ListPreference) findPreference("audio_output");
        this.mAudioOutputPref.setOnPreferenceChangeListener(this);
        this.mHdmiManager = IMtkHdmiManager.Stub.asInterface(ServiceManager.getService("mtkhdmi"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
        this.mContext.registerReceiverAsUser(this.mActionReceiver, UserHandle.ALL, filter, null, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (this.mHdmiManager == null) {
            finish();
            return;
        }
        try {
            String title = getString(R.string.hdmi_settings);
            String hdmi = getString(R.string.hdmi_replace_hdmi);
            if (this.mHdmiManager.getDisplayType() == 2) {
                String mhl = getString(R.string.hdmi_replace_mhl);
                this.mActivity.setTitle(title.replaceAll(hdmi, mhl));
                this.mToggleHdmiPref.setTitle(this.mToggleHdmiPref.getTitle().toString().replaceAll(hdmi, mhl));
            } else if (this.mHdmiManager.getDisplayType() == 3) {
                String slimport = getString(R.string.slimport_replace_hdmi);
                this.mActivity.setTitle(title.replaceAll(hdmi, slimport));
                this.mToggleHdmiPref.setTitle(this.mToggleHdmiPref.getTitle().toString().replaceAll(hdmi, slimport));
            } else {
                this.mActivity.setTitle(title);
            }
            if (!this.mHdmiManager.hasCapability(1)) {
                Log.d("@M_HDMISettings", "remove mVideoScalePref");
                getPreferenceScreen().removePreference(this.mVideoScalePref);
            }
            if (this.mHdmiManager.getAudioParameter(120, 3) > 2) {
                return;
            }
            Log.d("@M_HDMISettings", "remove mAudioOutputPref");
            getPreferenceScreen().removePreference(this.mAudioOutputPref);
        } catch (RemoteException e) {
            Log.d("@M_HDMISettings", "HdmiManager RemoteException");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePref();
        this.mActivity.getContentResolver().registerContentObserver(Settings.System.getUriFor("hdmi_enable_status"), false, this.mHdmiSettingsObserver);
        this.mActivity.getContentResolver().registerContentObserver(Settings.System.getUriFor("hdmi_cable_plugged"), false, this.mHdmiSettingsObserver);
    }

    @Override
    public void onPause() {
        this.mActivity.getContentResolver().unregisterContentObserver(this.mHdmiSettingsObserver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void updatePref() {
        Log.i("@M_HDMISettings", "updatePref");
        updatePrefStatus();
        updateSelectedResolution();
        updateSelectedScale();
        updateSelectedAudioOutput();
    }

    private void updatePrefStatus() {
        Log.i("@M_HDMISettings", "updatePrefStatus");
        boolean shouldEnable = false;
        try {
            shouldEnable = this.mHdmiManager.isSignalOutputting();
        } catch (RemoteException e) {
            Log.w("@M_HDMISettings", "hdmi manager RemoteException: " + e.getMessage());
        }
        this.mVideoResolutionPref.setEnabled(shouldEnable);
        this.mVideoScalePref.setEnabled(shouldEnable);
        boolean hdmiEnabled = Settings.System.getInt(this.mActivity.getContentResolver(), "hdmi_enable_status", 1) == 1;
        this.mToggleHdmiPref.setChecked(hdmiEnabled);
    }

    private void updateSelectedResolution() {
        Log.i("@M_HDMISettings", "updateSelectedResolution");
        int videoResolution = Settings.System.getInt(this.mActivity.getContentResolver(), "hdmi_video_resolution", 100);
        if (videoResolution > 100) {
            videoResolution = 100;
        }
        int[] supportedResolutions = {100};
        try {
            supportedResolutions = this.mHdmiManager.getSupportedResolutions();
        } catch (RemoteException e) {
            Log.w("@M_HDMISettings", "hdmi manager RemoteException: " + e.getMessage());
        }
        CharSequence[] stringArray = this.mActivity.getResources().getStringArray(R.array.hdmi_video_resolution_entries);
        List<CharSequence> realResolutionEntries = new ArrayList<>();
        List<CharSequence> realResolutionValues = new ArrayList<>();
        realResolutionEntries.add(this.mActivity.getResources().getString(R.string.hdmi_auto));
        realResolutionValues.add(Integer.toString(100));
        for (int resolution : supportedResolutions) {
            try {
                realResolutionEntries.add(stringArray[resolution]);
                realResolutionValues.add(Integer.toString(resolution));
            } catch (ArrayIndexOutOfBoundsException e2) {
                Log.d("@M_HDMISettings", e2.getMessage());
            }
        }
        this.mVideoResolutionPref.setEntries((CharSequence[]) realResolutionEntries.toArray(new CharSequence[realResolutionEntries.size()]));
        this.mVideoResolutionPref.setEntryValues((CharSequence[]) realResolutionValues.toArray(new CharSequence[realResolutionValues.size()]));
        this.mVideoResolutionPref.setValue(Integer.toString(videoResolution));
    }

    private void updateSelectedScale() {
        Log.i("@M_HDMISettings", "updateSelectedScale");
        int videoScale = Settings.System.getInt(this.mActivity.getContentResolver(), "hdmi_video_scale", 0);
        this.mVideoScalePref.setValue(Integer.toString(videoScale));
    }

    private void updateSelectedAudioOutput() {
        Log.i("@M_HDMISettings", "updateSelectedAudioOutput");
        try {
            this.mAudioOutputPref.setEnabled(this.mHdmiManager.isSignalOutputting());
        } catch (RemoteException e) {
            Log.w("@M_HDMISettings", "hdmi manager RemoteException: " + e.getMessage());
        }
        int audioOutputMode = Settings.System.getIntForUser(this.mActivity.getContentResolver(), "hdmi_audio_output_mode", 0, -2);
        this.mAudioOutputPref.setValue(Integer.toString(audioOutputMode));
        Log.i("@M_HDMISettings", "updateSelectedAudioOutput audioOutputMode: " + audioOutputMode);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        Log.d("@M_HDMISettings", key + " preference changed");
        try {
            if ("hdmi_toggler".equals(key)) {
                boolean checked = ((Boolean) newValue).booleanValue();
                Log.d("@M_HDMISettings", key + " enableHdmi start");
                if (Settings.Global.getInt(this.mActivity.getContentResolver(), "wifi_display_on", 0) != 0) {
                    showDialog();
                } else {
                    this.mHdmiManager.enableHdmi(checked);
                }
            } else if ("video_resolution".equals(key)) {
                this.mHdmiManager.setVideoResolution(Integer.parseInt((String) newValue));
            } else if ("video_scale".equals(key)) {
                int scaleValue = Integer.parseInt((String) newValue);
                if (scaleValue >= 0 && scaleValue <= 10) {
                    this.mHdmiManager.setVideoScale(scaleValue);
                } else {
                    Log.d("@M_HDMISettings", "scaleValue error: " + scaleValue);
                }
            } else if ("audio_output".equals(key)) {
                int which = Integer.parseInt((String) newValue);
                int maxChannel = 2;
                if (which == 1) {
                    maxChannel = this.mHdmiManager.getAudioParameter(120, 3);
                }
                AudioSystem.setParameters("HDMI_channel=" + maxChannel);
                Settings.System.putIntForUser(this.mActivity.getContentResolver(), "hdmi_audio_output_mode", which, -2);
                Log.d("@M_HDMISettings", "AudioSystem.setParameters HDMI_channel = " + maxChannel + ",which: " + which);
            }
        } catch (RemoteException e) {
            Log.w("@M_HDMISettings", "hdmi manager RemoteException: " + e.getMessage());
        }
        return true;
    }
}

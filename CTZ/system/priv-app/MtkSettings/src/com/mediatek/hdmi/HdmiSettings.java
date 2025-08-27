package com.mediatek.hdmi;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioSystem;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.wifi.AccessPoint;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class HdmiSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String HDMI_ENABLE;
    private static final String HDMI_VIDEO_AUTO;
    private static final String HDMI_VIDEO_RESOLUTION;
    private Activity mActivity;
    private ListPreference mAudioOutputPref;
    private Object mHdmiManager;
    private HdmiObserver mHdmiObserver;
    private SwitchPreference mToggleHdmiPref;
    private ListPreference mVideoResolutionPref;
    private ListPreference mVideoScalePref;
    private boolean isPlugIn = false;
    private final Handler mHandler = new Handler() { // from class: com.mediatek.hdmi.HdmiSettings.1
        @Override // android.os.Handler
        public void handleMessage(Message message) throws NoSuchMethodException, Resources.NotFoundException, SecurityException, ClassNotFoundException {
            switch (message.what) {
                case AccessPoint.Speed.MODERATE /* 10 */:
                    if (HdmiSettings.this.mVideoResolutionPref != null) {
                        HdmiSettings.this.mVideoResolutionPref.setEnabled(true);
                        HdmiSettings.this.updatePref();
                        break;
                    }
                    break;
                case 11:
                    if (HdmiSettings.this.mVideoResolutionPref != null) {
                        HdmiSettings.this.mVideoResolutionPref.setEnabled(false);
                        HdmiSettings.this.updatePref();
                        break;
                    }
                    break;
                case 12:
                    if (HdmiSettings.this.mToggleHdmiPref != null) {
                        HdmiSettings.this.mToggleHdmiPref.setEnabled(true);
                        break;
                    }
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    };
    private ContentObserver mHdmiSettingsObserver = new ContentObserver(new Handler()) { // from class: com.mediatek.hdmi.HdmiSettings.2
        @Override // android.database.ContentObserver
        public void onChange(boolean z) throws NoSuchMethodException, Resources.NotFoundException, SecurityException, ClassNotFoundException {
            Log.d("@M_HDMISettings", "mHdmiSettingsObserver onChanged: " + z);
            HdmiSettings.this.updatePref();
        }
    };

    static {
        HDMI_VIDEO_RESOLUTION = HdimReflectionHelper.HDMI_HIDL_SUPPORT ? "persist.vendor.sys.hdmi_hidl.resolution" : "persist.vendor.sys.hdmi.resolution";
        HDMI_ENABLE = HdimReflectionHelper.HDMI_HIDL_SUPPORT ? "persist.vendor.sys.hdmi_hidl.enable" : "persist.vendor.sys.hdmi.enable";
        HDMI_VIDEO_AUTO = HdimReflectionHelper.HDMI_HIDL_SUPPORT ? "persist.vendor.sys.hdmi_hidl.auto" : "persist.vendor.sys.hdmi.auto";
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 46;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i("@M_HDMISettings", "HdmiSettings.onCreate()");
        if (HdimReflectionHelper.HDMI_TB_SUPPORT) {
            addPreferencesFromResource(R.xml.hdmi_box_settings);
        } else {
            addPreferencesFromResource(R.xml.hdmi_settings);
        }
        this.mActivity = getActivity();
        this.mToggleHdmiPref = (SwitchPreference) findPreference("hdmi_toggler");
        this.mToggleHdmiPref.setOnPreferenceChangeListener(this);
        this.mVideoResolutionPref = (ListPreference) findPreference("video_resolution");
        this.mVideoResolutionPref.setOnPreferenceChangeListener(this);
        this.mVideoScalePref = (ListPreference) findPreference("video_scale");
        this.mVideoScalePref.setOnPreferenceChangeListener(this);
        this.mVideoScalePref.getEntries();
        CharSequence[] entryValues = this.mVideoScalePref.getEntryValues();
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < entryValues.length; i++) {
            if (Integer.parseInt(entryValues[i].toString()) != 0) {
                arrayList.add(this.mActivity.getResources().getString(R.string.hdmi_scale_scale_down, entryValues[i]));
            } else {
                arrayList.add(this.mActivity.getResources().getString(R.string.hdmi_scale_no_scale));
            }
        }
        this.mVideoScalePref.setEntries((CharSequence[]) arrayList.toArray(new CharSequence[arrayList.size()]));
        this.mAudioOutputPref = (ListPreference) findPreference("audio_output");
        this.mAudioOutputPref.setOnPreferenceChangeListener(this);
        this.mHdmiManager = HdimReflectionHelper.getHdmiService();
        if (HdimReflectionHelper.HDMI_TB_SUPPORT) {
            if (this.mHdmiObserver == null) {
                this.mHdmiObserver = new HdmiObserver(this.mActivity.getApplicationContext());
            }
            this.mHdmiObserver.startObserve();
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) throws ClassNotFoundException {
        super.onActivityCreated(bundle);
        if (this.mHdmiManager == null) {
            finish();
            return;
        }
        String string = getString(R.string.hdmi_settings);
        String string2 = getString(R.string.hdmi_replace_hdmi);
        int hdmiDisplayTypeConstant = HdimReflectionHelper.getHdmiDisplayTypeConstant("DISPLAY_TYPE_MHL");
        int hdmiDisplayTypeConstant2 = HdimReflectionHelper.getHdmiDisplayTypeConstant("DISPLAY_TYPE_SLIMPORT");
        int hdmiDisplayType = HdimReflectionHelper.getHdmiDisplayType(this.mHdmiManager);
        if (hdmiDisplayType == hdmiDisplayTypeConstant) {
            String string3 = getString(R.string.hdmi_replace_mhl);
            this.mActivity.setTitle(string.replaceAll(string2, string3));
            this.mToggleHdmiPref.setTitle(this.mToggleHdmiPref.getTitle().toString().replaceAll(string2, string3));
        } else if (hdmiDisplayType == hdmiDisplayTypeConstant2) {
            String string4 = getString(R.string.slimport_replace_hdmi);
            this.mActivity.setTitle(string.replaceAll(string2, string4));
            this.mToggleHdmiPref.setTitle(this.mToggleHdmiPref.getTitle().toString().replaceAll(string2, string4));
        } else {
            this.mActivity.setTitle(string);
        }
        if (!HdimReflectionHelper.hasCapability(this.mHdmiManager)) {
            Log.d("@M_HDMISettings", "remove mVideoScalePref");
            getPreferenceScreen().removePreference(this.mVideoScalePref);
        }
        if (HdimReflectionHelper.getAudioParameter(this.mHdmiManager) <= 2) {
            Log.d("@M_HDMISettings", "remove mAudioOutputPref");
            getPreferenceScreen().removePreference(this.mAudioOutputPref);
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() throws NoSuchMethodException, Resources.NotFoundException, SecurityException, ClassNotFoundException {
        super.onResume();
        updatePref();
        this.mActivity.getContentResolver().registerContentObserver(Settings.System.getUriFor("hdmi_enable_status"), false, this.mHdmiSettingsObserver);
        this.mActivity.getContentResolver().registerContentObserver(Settings.System.getUriFor("hdmi_cable_plugged"), false, this.mHdmiSettingsObserver);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        this.mActivity.getContentResolver().unregisterContentObserver(this.mHdmiSettingsObserver);
        super.onPause();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        if (this.mHdmiObserver != null) {
            this.mHdmiObserver.stopObserve();
        }
        super.onDestroy();
    }

    private void updatePref() throws NoSuchMethodException, Resources.NotFoundException, SecurityException, ClassNotFoundException {
        Log.i("@M_HDMISettings", "updatePref");
        updatePrefStatus();
        updateSelectedResolution();
        updateSelectedScale();
        updateSelectedAudioOutput();
    }

    private void updatePrefStatus() throws NoSuchMethodException, SecurityException {
        Log.i("@M_HDMISettings", "updatePrefStatus");
        boolean zIsSignalOutputting = HdimReflectionHelper.isSignalOutputting(this.mHdmiManager);
        Log.i("@M_HDMISettings", "updatePrefStatus, shouldEnable = " + zIsSignalOutputting);
        if (HdimReflectionHelper.HDMI_TB_SUPPORT) {
            this.mVideoResolutionPref.setEnabled(this.isPlugIn);
        } else {
            this.mVideoResolutionPref.setEnabled(zIsSignalOutputting);
        }
        this.mVideoScalePref.setEnabled(zIsSignalOutputting);
        boolean z = Settings.System.getInt(this.mActivity.getContentResolver(), "hdmi_enable_status", 1) == 1;
        if (HdimReflectionHelper.HDMI_TB_SUPPORT) {
            z = SystemProperties.getInt(HDMI_ENABLE, 0) == 1;
        }
        this.mToggleHdmiPref.setChecked(z);
    }

    private void updateSelectedResolution() throws NoSuchMethodException, Resources.NotFoundException, ClassNotFoundException, SecurityException {
        int i;
        Log.i("@M_HDMISettings", "updateSelectedResolution");
        int hdmiDisplayTypeConstant = HdimReflectionHelper.getHdmiDisplayTypeConstant("AUTO");
        int i2 = Settings.System.getInt(this.mActivity.getContentResolver(), "hdmi_video_resolution", hdmiDisplayTypeConstant);
        if (HdimReflectionHelper.HDMI_TB_SUPPORT) {
            i2 = SystemProperties.getInt(HDMI_VIDEO_RESOLUTION, 13);
            i = SystemProperties.getInt(HDMI_VIDEO_AUTO, 13);
        } else {
            i = 0;
        }
        if (i2 > hdmiDisplayTypeConstant || i == 1) {
            i2 = hdmiDisplayTypeConstant;
        }
        new int[1][0] = hdmiDisplayTypeConstant;
        int[] supportedResolutions = HdimReflectionHelper.getSupportedResolutions(this.mHdmiManager);
        String[] stringArray = this.mActivity.getResources().getStringArray(HdimReflectionHelper.HDMI_TB_SUPPORT ? R.array.hdmi_box_video_resolution_entries : R.array.hdmi_video_resolution_entries);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        arrayList.add(this.mActivity.getResources().getString(R.string.hdmi_auto));
        arrayList2.add(Integer.toString(hdmiDisplayTypeConstant));
        for (int i3 : supportedResolutions) {
            try {
                arrayList.add(stringArray[i3]);
                arrayList2.add(Integer.toString(i3));
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.d("@M_HDMISettings", e.getMessage());
            }
        }
        this.mVideoResolutionPref.setEntries((CharSequence[]) arrayList.toArray(new CharSequence[arrayList.size()]));
        this.mVideoResolutionPref.setEntryValues((CharSequence[]) arrayList2.toArray(new CharSequence[arrayList2.size()]));
        this.mVideoResolutionPref.setValue(Integer.toString(i2));
    }

    private void updateSelectedScale() {
        Log.i("@M_HDMISettings", "updateSelectedScale");
        this.mVideoScalePref.setValue(Integer.toString(Settings.System.getInt(this.mActivity.getContentResolver(), "hdmi_video_scale", 0)));
    }

    private void updateSelectedAudioOutput() {
        Log.i("@M_HDMISettings", "updateSelectedAudioOutput");
        this.mAudioOutputPref.setEnabled(HdimReflectionHelper.isSignalOutputting(this.mHdmiManager));
        int intForUser = Settings.System.getIntForUser(this.mActivity.getContentResolver(), "hdmi_audio_output_mode", 0, -2);
        this.mAudioOutputPref.setValue(Integer.toString(intForUser));
        Log.i("@M_HDMISettings", "updateSelectedAudioOutput audioOutputMode: " + intForUser);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) throws IllegalAccessException, NoSuchMethodException, SecurityException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException {
        String key = preference.getKey();
        Log.d("@M_HDMISettings", key + " preference changed");
        if ("hdmi_toggler".equals(key)) {
            HdimReflectionHelper.enableHdmi(this.mHdmiManager, ((Boolean) obj).booleanValue());
            this.mToggleHdmiPref.setEnabled(false);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(12), 500L);
        } else if ("video_resolution".equals(key)) {
            if (HdimReflectionHelper.HDMI_TB_SUPPORT) {
                if (HdimReflectionHelper.HDMI_HIDL_SUPPORT) {
                    if (Integer.parseInt((String) obj) == HdimReflectionHelper.getHdmiDisplayTypeConstant("AUTO")) {
                        HdimReflectionHelper.setAutoMode(this.mHdmiManager, true);
                    } else {
                        HdimReflectionHelper.setAutoMode(this.mHdmiManager, false);
                    }
                } else if (Integer.parseInt((String) obj) == HdimReflectionHelper.getHdmiDisplayTypeConstant("AUTO")) {
                    SystemProperties.set(HDMI_VIDEO_AUTO, "1");
                } else {
                    SystemProperties.set(HDMI_VIDEO_AUTO, "0");
                }
            }
            HdimReflectionHelper.setVideoResolution(this.mHdmiManager, Integer.parseInt((String) obj));
        } else if ("video_scale".equals(key)) {
            int i = Integer.parseInt((String) obj);
            if (i >= 0 && i <= 10) {
                HdimReflectionHelper.setVideoScale(this.mHdmiManager, i);
            } else {
                Log.d("@M_HDMISettings", "scaleValue error: " + i);
            }
        } else if ("audio_output".equals(key)) {
            int i2 = Integer.parseInt((String) obj);
            int hdmiDisplayTypeConstant = HdimReflectionHelper.getHdmiDisplayTypeConstant("AUDIO_OUTPUT_STEREO");
            if (i2 == 1) {
                hdmiDisplayTypeConstant = HdimReflectionHelper.getAudioParameter(this.mHdmiManager);
            }
            AudioSystem.setParameters("HDMI_channel=" + hdmiDisplayTypeConstant);
            Settings.System.putIntForUser(this.mActivity.getContentResolver(), "hdmi_audio_output_mode", i2, -2);
            Log.d("@M_HDMISettings", "AudioSystem.setParameters HDMI_channel = " + hdmiDisplayTypeConstant + ",which: " + i2);
        }
        return true;
    }

    private class HdmiObserver extends UEventObserver {
        private final Context mContext;

        public HdmiObserver(Context context) {
            this.mContext = context;
            init();
        }

        public void startObserve() {
            startObserving("DEVPATH=/devices/virtual/switch/hdmi");
        }

        public void stopObserve() {
            stopObserving();
        }

        public void onUEvent(UEventObserver.UEvent uEvent) throws NumberFormatException {
            int i;
            try {
                i = Integer.parseInt(uEvent.get("SWITCH_STATE"));
            } catch (NumberFormatException e) {
                Log.w("HdmiReceiver.HdmiObserver", "HdmiObserver: Could not parse switch state from event " + uEvent);
                i = 0;
            }
            update(i);
        }

        private synchronized void init() {
            try {
                update(Integer.parseInt(getContentFromFile("/sys/class/switch/hdmi/state")));
            } catch (NumberFormatException e) {
                Log.w("HdmiReceiver.HdmiObserver", "HDMI state fail");
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [397=7, 399=5, 400=5, 401=5] */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:28:0x006b */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:70:0x0007 */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r1v19, types: [java.lang.String] */
        private String getContentFromFile(String str) throws Throwable {
            String strTrim;
            String str2;
            StringBuilder sb;
            FileReader fileReader;
            ?? r1;
            char[] cArr = new char[1024];
            FileReader fileReader2 = null;
            FileReader fileReader3 = null;
            FileReader fileReader4 = null;
            FileReader fileReader5 = null;
            try {
                try {
                    fileReader = new FileReader(str);
                } catch (FileNotFoundException e) {
                    strTrim = null;
                } catch (IOException e2) {
                    strTrim = null;
                } catch (IndexOutOfBoundsException e3) {
                    e = e3;
                    strTrim = null;
                }
                try {
                    try {
                        strTrim = String.valueOf(cArr, 0, fileReader.read(cArr, 0, cArr.length)).trim();
                    } catch (FileNotFoundException e4) {
                        strTrim = null;
                    } catch (IOException e5) {
                        strTrim = null;
                    } catch (IndexOutOfBoundsException e6) {
                        e = e6;
                        strTrim = null;
                    }
                    try {
                        r1 = "HdmiReceiver.HdmiObserver";
                        Log.d("HdmiReceiver.HdmiObserver", str + " content is " + strTrim);
                    } catch (FileNotFoundException e7) {
                        fileReader3 = fileReader;
                        Log.w("HdmiReceiver.HdmiObserver", "can't find file " + str);
                        fileReader2 = fileReader3;
                        if (fileReader3 != null) {
                            try {
                                fileReader3.close();
                                fileReader2 = fileReader3;
                            } catch (IOException e8) {
                                e = e8;
                                str2 = "HdmiReceiver.HdmiObserver";
                                sb = new StringBuilder();
                                sb.append("close reader fail: ");
                                sb.append(e.getMessage());
                                Log.w(str2, sb.toString());
                                return strTrim;
                            }
                        }
                        return strTrim;
                    } catch (IOException e9) {
                        fileReader4 = fileReader;
                        Log.w("HdmiReceiver.HdmiObserver", "IO exception when read file " + str);
                        fileReader2 = fileReader4;
                        if (fileReader4 != null) {
                            try {
                                fileReader4.close();
                                fileReader2 = fileReader4;
                            } catch (IOException e10) {
                                e = e10;
                                str2 = "HdmiReceiver.HdmiObserver";
                                sb = new StringBuilder();
                                sb.append("close reader fail: ");
                                sb.append(e.getMessage());
                                Log.w(str2, sb.toString());
                                return strTrim;
                            }
                        }
                        return strTrim;
                    } catch (IndexOutOfBoundsException e11) {
                        e = e11;
                        fileReader5 = fileReader;
                        Log.w("HdmiReceiver.HdmiObserver", "index exception: " + e.getMessage());
                        fileReader2 = fileReader5;
                        if (fileReader5 != null) {
                            try {
                                fileReader5.close();
                                fileReader2 = fileReader5;
                            } catch (IOException e12) {
                                e = e12;
                                str2 = "HdmiReceiver.HdmiObserver";
                                sb = new StringBuilder();
                                sb.append("close reader fail: ");
                                sb.append(e.getMessage());
                                Log.w(str2, sb.toString());
                                return strTrim;
                            }
                        }
                        return strTrim;
                    }
                    try {
                        fileReader.close();
                        fileReader2 = r1;
                    } catch (IOException e13) {
                        e = e13;
                        str2 = "HdmiReceiver.HdmiObserver";
                        sb = new StringBuilder();
                        sb.append("close reader fail: ");
                        sb.append(e.getMessage());
                        Log.w(str2, sb.toString());
                        return strTrim;
                    }
                    return strTrim;
                } catch (Throwable th) {
                    th = th;
                    fileReader2 = fileReader;
                    if (fileReader2 != null) {
                        try {
                            fileReader2.close();
                        } catch (IOException e14) {
                            Log.w("HdmiReceiver.HdmiObserver", "close reader fail: " + e14.getMessage());
                        }
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }

        private synchronized void update(int i) {
            if (HdmiSettings.this.mVideoResolutionPref != null) {
                if (i == 0) {
                    HdmiSettings.this.isPlugIn = false;
                    HdmiSettings.this.mHandler.sendEmptyMessage(11);
                } else {
                    HdmiSettings.this.isPlugIn = true;
                    HdmiSettings.this.mHandler.sendEmptyMessage(10);
                }
            }
        }
    }
}

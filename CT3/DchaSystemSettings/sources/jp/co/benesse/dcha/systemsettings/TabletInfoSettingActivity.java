package jp.co.benesse.dcha.systemsettings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.view.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.util.Logger;

public class TabletInfoSettingActivity extends PreferenceActivity {
    private static final Uri URI_TEST_ENVIRONMENT_INFO = Uri.parse("content://jp.co.benesse.dcha.databox.db.KvsProvider/kvs/test.environment.info");
    private IDchaService mDchaService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.d("TabletInfoSettingActivity", "onServiceConnected 0001");
            TabletInfoSettingActivity.this.mDchaService = IDchaService.Stub.asInterface(service);
            TabletInfoSettingActivity.this.hideNavigationBar(false);
            Logger.d("TabletInfoSettingActivity", "onServiceConnected 0002");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.d("TabletInfoSettingActivity", "onServiceDisconnected 0001");
            TabletInfoSettingActivity.this.mDchaService = null;
            Logger.d("TabletInfoSettingActivity", "onServiceDisconnected 0002");
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d("TabletInfoSettingActivity", "onReceive 0001");
            try {
                Logger.d("TabletInfoSettingActivity", "onReceive 0002");
                TabletInfoSettingActivity.this.mDchaService.removeTask("jp.co.benesse.dcha.allgrade.usersetting");
                Logger.d("TabletInfoSettingActivity", "onReceive 0003");
            } catch (RemoteException e) {
                Logger.e("TabletInfoSettingActivity", "RemoteException", e);
                Logger.d("TabletInfoSettingActivity", "onReceive 0004");
            }
            Logger.d("TabletInfoSettingActivity", "onReceive 0005");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("TabletInfoSettingActivity", "onCreate 0001");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.device_info_settings);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("jp.co.benesse.dcha.allgrade.b001.ACTION_ACTIVATE");
        registerReceiver(this.mReceiver, intentFilter);
        setStringSummary("firmware_version", Build.VERSION.RELEASE);
        findPreference("firmware_version").setEnabled(true);
        String patch = Build.VERSION.SECURITY_PATCH;
        if (!"".equals(patch)) {
            setStringSummary("security_patch", patch);
        } else {
            getPreferenceScreen().removePreference(findPreference("security_patch"));
        }
        setStringSummary("device_model", Build.MODEL);
        setStringSummary("build_number", Build.DISPLAY);
        findPreference("build_number").setEnabled(true);
        findPreference("kernel_version").setSummary(getFormattedKernelVersion());
        String environment = getKvsValue(this, URI_TEST_ENVIRONMENT_INFO, "environment", null);
        String version = getKvsValue(this, URI_TEST_ENVIRONMENT_INFO, "version", null);
        if (!TextUtils.isEmpty(environment) && !TextUtils.isEmpty(version)) {
            findPreference("test_environment").setSummary(getString(R.string.test_environment_format, new Object[]{environment, version}));
        } else {
            getPreferenceScreen().removePreference(findPreference("test_environment"));
        }
        Intent intent = new Intent("jp.co.benesse.dcha.dchaservice.DchaService");
        intent.setPackage("jp.co.benesse.dcha.dchaservice");
        bindService(intent, this.mServiceConnection, 1);
        PreferenceGroup parentPreference = (PreferenceGroup) findPreference("container");
        updatePreferenceToSpecificActivityOrRemove(this, parentPreference, "terms", 1);
        updatePreferenceToSpecificActivityOrRemove(this, parentPreference, "license", 1);
        updatePreferenceToSpecificActivityOrRemove(this, parentPreference, "copyright", 1);
        Logger.d("TabletInfoSettingActivity", "onCreate 0002");
    }

    @Override
    protected void onResume() {
        Logger.d("TabletInfoSettingActivity", "onResume 0001");
        super.onResume();
        hideNavigationBar(false);
        Logger.d("TabletInfoSettingActivity", "onResume 0002");
    }

    @Override
    protected void onPause() {
        Logger.d("TabletInfoSettingActivity", "onPause 0001");
        super.onPause();
        Logger.d("TabletInfoSettingActivity", "onPause 0002");
    }

    @Override
    protected void onDestroy() {
        Logger.d("TabletInfoSettingActivity", "onDestroy 0001");
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
        this.mReceiver = null;
        if (this.mServiceConnection != null) {
            Logger.d("TabletInfoSettingActivity", "onDestroy 0002");
            unbindService(this.mServiceConnection);
            this.mServiceConnection = null;
            this.mDchaService = null;
        }
        Logger.d("TabletInfoSettingActivity", "onDestroy 0003");
    }

    private void setStringSummary(String preference, String value) {
        Logger.d("TabletInfoSettingActivity", "setStringSummary 0001");
        try {
            Logger.d("TabletInfoSettingActivity", "setStringSummary 0002");
            findPreference(preference).setSummary(value);
            Logger.d("TabletInfoSettingActivity", "setStringSummary 0003");
        } catch (RuntimeException e) {
            Logger.d("TabletInfoSettingActivity", "setStringSummary 0004");
            Logger.e("TabletInfoSettingActivity", "RuntimeException", e);
            findPreference(preference).setSummary(getResources().getString(R.string.device_info_default));
        }
        Logger.d("TabletInfoSettingActivity", "setStringSummary 0005");
    }

    private String getFormattedKernelVersion() {
        Logger.d("TabletInfoSettingActivity", "getFormattedKernelVersion 0001");
        try {
            Logger.d("TabletInfoSettingActivity", "getFormattedKernelVersion 0002");
            return formatKernelVersion(readLine("/proc/version"));
        } catch (IOException e) {
            Logger.d("TabletInfoSettingActivity", "getFormattedKernelVersion 0003");
            Logger.e("TabletInfoSettingActivity", "IO Exception when getting kernel version for Device Info screen", e);
            return "Unavailable";
        }
    }

    private String readLine(String filename) throws IOException {
        Logger.d("TabletInfoSettingActivity", "readLine 0001");
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            Logger.d("TabletInfoSettingActivity", "readLine 0002");
            String line = reader.readLine();
            Logger.d("TabletInfoSettingActivity", "readLine 0003");
            reader.close();
            return line;
        } catch (Throwable th) {
            Logger.d("TabletInfoSettingActivity", "readLine 0003");
            reader.close();
            throw th;
        }
    }

    private String formatKernelVersion(String rawKernelVersion) {
        Logger.d("TabletInfoSettingActivity", "formatKernelVersion 0001");
        Matcher m = Pattern.compile("Linux version (\\S+) \\((\\S+?)\\) (?:\\(gcc.+? \\)) (#\\d+) (?:.*?)?((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)").matcher(rawKernelVersion);
        if (!m.matches()) {
            Logger.d("TabletInfoSettingActivity", "formatKernelVersion 0002");
            Logger.e("TabletInfoSettingActivity", "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        }
        if (m.groupCount() >= 4) {
            Logger.d("TabletInfoSettingActivity", "formatKernelVersion 0004");
            return m.group(1) + "\n" + m.group(2) + " " + m.group(3) + "\n" + m.group(4);
        }
        Logger.d("TabletInfoSettingActivity", "formatKernelVersion 0003");
        Logger.e("TabletInfoSettingActivity", "Regex match on /proc/version only returned " + m.groupCount() + " groups");
        return "Unavailable";
    }

    private boolean updatePreferenceToSpecificActivityOrRemove(Context context, PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {
        Logger.d("TabletInfoSettingActivity", "updatePreferenceToSpecificActivityOrRemove 0001");
        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            Logger.d("TabletInfoSettingActivity", "updatePreferenceToSpecificActivityOrRemove 0002");
            return false;
        }
        Intent intent = preference.getIntent();
        if (intent != null) {
            Logger.d("TabletInfoSettingActivity", "updatePreferenceToSpecificActivityOrRemove 0003");
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                Logger.d("TabletInfoSettingActivity", "updatePreferenceToSpecificActivityOrRemove 0004");
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                    Logger.d("TabletInfoSettingActivity", "updatePreferenceToSpecificActivityOrRemove 0005");
                    preference.setIntent(new Intent().setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
                    if ((flags & 1) != 0) {
                        Logger.d("TabletInfoSettingActivity", "updatePreferenceToSpecificActivityOrRemove 0006");
                        preference.setTitle(resolveInfo.loadLabel(pm));
                    }
                    Logger.d("TabletInfoSettingActivity", "updatePreferenceToSpecificActivityOrRemove 0007");
                    return true;
                }
            }
        }
        parentPreferenceGroup.removePreference(preference);
        Logger.d("TabletInfoSettingActivity", "updatePreferenceToSpecificActivityOrRemove 0008");
        return false;
    }

    protected String getKvsValue(Context context, Uri uri, String key, String defaultValue) {
        String[] projection = {"value"};
        String[] selectionArgs = {key};
        String value = defaultValue;
        Cursor cursor = null;
        try {
            try {
                ContentResolver cr = context.getContentResolver();
                cursor = cr.query(uri, projection, "key=?", selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    value = cursor.getString(cursor.getColumnIndex("value"));
                }
            } catch (Exception e) {
                Logger.d("TabletInfoSettingActivity", "getKvsValue", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
            return value;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Logger.d("TabletInfoSettingActivity", "dispatchKeyEvent 0001");
        if (event.getAction() == 0 && event.getKeyCode() == 4) {
            Logger.d("TabletInfoSettingActivity", "dispatchKeyEvent 0002");
            moveSettingActivity();
            Logger.d("TabletInfoSettingActivity", "dispatchKeyEvent 0003");
            return true;
        }
        Logger.d("TabletInfoSettingActivity", "dispatchKeyEvent 0004");
        return false;
    }

    protected void moveSettingActivity() {
        Logger.d("TabletInfoSettingActivity", "moveSettingActivity 0001");
        Intent intent = new Intent();
        intent.setClassName("jp.co.benesse.dcha.allgrade.usersetting", "jp.co.benesse.dcha.allgrade.usersetting.activity.SettingMenuActivity");
        startActivity(intent);
        finish();
        Logger.d("TabletInfoSettingActivity", "moveSettingActivity 0002");
    }

    public void hideNavigationBar(boolean hide) {
        Logger.d("TabletInfoSettingActivity", "hideNavigationBar 0001");
        try {
            Logger.d("TabletInfoSettingActivity", "hideNavigationBar 0002");
            if (this.mDchaService != null) {
                Logger.d("TabletInfoSettingActivity", "hideNavigationBar 0003");
                this.mDchaService.hideNavigationBar(hide);
            }
        } catch (RemoteException e) {
            Logger.d("TabletInfoSettingActivity", "hideNavigationBar 0004");
            Logger.e("TabletInfoSettingActivity", "RemoteException", e);
        }
        Logger.d("TabletInfoSettingActivity", "hideNavigationBar 0005");
    }
}

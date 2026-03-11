package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Telephony;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.telephony.PhoneConstants;
import java.util.ArrayList;

public class ApnSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final Uri DEFAULTAPN_URI = Uri.parse("content://telephony/carriers/restore");
    private static final Uri PREFERAPN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static boolean mRestoreDefaultApnMode;
    private IntentFilter mMobileStateFilter;
    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.ANY_DATA_STATE")) {
                PhoneConstants.DataState state = ApnSettings.getMobileDataState(intent);
                switch (AnonymousClass3.$SwitchMap$com$android$internal$telephony$PhoneConstants$DataState[state.ordinal()]) {
                    case 1:
                        if (!ApnSettings.mRestoreDefaultApnMode) {
                            ApnSettings.this.fillList();
                        } else {
                            ApnSettings.this.showDialog(1001);
                        }
                        break;
                }
            }
        }
    };
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private RestoreApnUiHandler mRestoreApnUiHandler;
    private HandlerThread mRestoreDefaultApnThread;
    private String mSelectedKey;
    private SubscriptionInfo mSubscriptionInfo;
    private UserManager mUm;
    private boolean mUnavailable;

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$android$internal$telephony$PhoneConstants$DataState = new int[PhoneConstants.DataState.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$PhoneConstants$DataState[PhoneConstants.DataState.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    public static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra("state");
        return str != null ? Enum.valueOf(PhoneConstants.DataState.class, str) : PhoneConstants.DataState.DISCONNECTED;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Activity activity = getActivity();
        int subId = activity.getIntent().getIntExtra("sub_id", -1);
        this.mUm = (UserManager) getSystemService("user");
        this.mMobileStateFilter = new IntentFilter("android.intent.action.ANY_DATA_STATE");
        if (!this.mUm.hasUserRestriction("no_config_mobile_networks")) {
            setHasOptionsMenu(true);
        }
        this.mSubscriptionInfo = Utils.findRecordBySubId(activity, subId);
        Toast.makeText(getActivity(), R.string.invalid_function, 1).show();
        activity.finish();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TextView empty = (TextView) getView().findViewById(android.R.id.empty);
        if (empty != null) {
            empty.setText(R.string.apn_settings_not_available);
            getListView().setEmptyView(empty);
        }
        if (this.mUm.hasUserRestriction("no_config_mobile_networks")) {
            this.mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
        } else {
            addPreferencesFromResource(R.xml.apn_settings);
            getListView().setItemsCanFocus(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!this.mUnavailable) {
            getActivity().registerReceiver(this.mMobileStateReceiver, this.mMobileStateFilter);
            if (!mRestoreDefaultApnMode) {
                fillList();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!this.mUnavailable) {
            getActivity().unregisterReceiver(this.mMobileStateReceiver);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mRestoreDefaultApnThread != null) {
            this.mRestoreDefaultApnThread.quit();
        }
    }

    private static boolean canHandleDefault(String type) {
        if (type == null || type.equals("")) {
            return false;
        }
        String[] types = type.split(",");
        for (String t : types) {
            if (t.equals("default") || t.equals("*")) {
                return true;
            }
        }
        return false;
    }

    public void fillList() {
        TelephonyManager tm = (TelephonyManager) getSystemService("phone");
        String mccmnc = this.mSubscriptionInfo == null ? "" : tm.getSimOperator(this.mSubscriptionInfo.getSubscriptionId());
        Log.d("ApnSettings", "mccmnc = " + mccmnc);
        String where = "numeric=\"" + mccmnc + "\" AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL))";
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{"_id", "name", "apn", "type"}, where, null, "name ASC");
        if (cursor != null) {
            PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
            apnList.removeAll();
            ArrayList<Preference> mmsApnList = new ArrayList<>();
            this.mSelectedKey = getSelectedApnKey();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(1);
                String apn = cursor.getString(2);
                String key = cursor.getString(0);
                String type = cursor.getString(3);
                ApnPreference pref = new ApnPreference(getActivity());
                pref.setKey(key);
                pref.setTitle(name);
                pref.setSummary(apn);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);
                boolean selectable = canHandleDefault(type);
                pref.setSelectable(selectable);
                if (selectable) {
                    if (this.mSelectedKey != null && this.mSelectedKey.equals(key)) {
                        pref.setChecked();
                    }
                    apnList.addPreference(pref);
                } else {
                    mmsApnList.add(pref);
                }
                cursor.moveToNext();
            }
            cursor.close();
            for (Preference preference : mmsApnList) {
                apnList.addPreference(preference);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!this.mUnavailable) {
            menu.add(0, 1, 0, getResources().getString(R.string.menu_new)).setIcon(android.R.drawable.ic_menu_add).setShowAsAction(1);
            menu.add(0, 2, 0, getResources().getString(R.string.menu_restore)).setIcon(android.R.drawable.ic_menu_upload);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                addNewApn();
                return true;
            case 2:
                new AlertDialog.Builder(getActivity()).setMessage(getResources().getString(R.string.menu_restore)).setTitle(R.string.menu_restore).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ApnSettings.this.restoreDefaultApn();
                    }
                }).setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addNewApn() {
        Intent intent = new Intent("android.intent.action.INSERT", Telephony.Carriers.CONTENT_URI);
        int subId = this.mSubscriptionInfo != null ? this.mSubscriptionInfo.getSubscriptionId() : -1;
        intent.putExtra("sub_id", subId);
        startActivity(intent);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int pos = Integer.parseInt(preference.getKey());
        Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
        startActivity(new Intent("android.intent.action.EDIT", url));
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("ApnSettings", "onPreferenceChange(): Preference - " + preference + ", newValue - " + newValue + ", newValue type - " + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
            return true;
        }
        return true;
    }

    private void setSelectedApnKey(String key) {
        this.mSelectedKey = key;
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put("apn_id", this.mSelectedKey);
        resolver.update(PREFERAPN_URI, values, null, null);
    }

    private String getSelectedApnKey() {
        String key = null;
        Cursor cursor = getContentResolver().query(PREFERAPN_URI, new String[]{"_id"}, null, null, "name ASC");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(0);
        }
        cursor.close();
        return key;
    }

    public boolean restoreDefaultApn() {
        showDialog(1001);
        mRestoreDefaultApnMode = true;
        if (this.mRestoreApnUiHandler == null) {
            this.mRestoreApnUiHandler = new RestoreApnUiHandler();
        }
        if (this.mRestoreApnProcessHandler == null || this.mRestoreDefaultApnThread == null) {
            this.mRestoreDefaultApnThread = new HandlerThread("Restore default APN Handler: Process Thread");
            this.mRestoreDefaultApnThread.start();
            this.mRestoreApnProcessHandler = new RestoreApnProcessHandler(this.mRestoreDefaultApnThread.getLooper(), this.mRestoreApnUiHandler);
        }
        this.mRestoreApnProcessHandler.sendEmptyMessage(1);
        return true;
    }

    private class RestoreApnUiHandler extends Handler {
        private RestoreApnUiHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    Activity activity = ApnSettings.this.getActivity();
                    if (activity == null) {
                        boolean unused = ApnSettings.mRestoreDefaultApnMode = false;
                    } else {
                        ApnSettings.this.fillList();
                        ApnSettings.this.getPreferenceScreen().setEnabled(true);
                        boolean unused2 = ApnSettings.mRestoreDefaultApnMode = false;
                        ApnSettings.this.removeDialog(1001);
                        Toast.makeText(activity, ApnSettings.this.getResources().getString(R.string.restore_default_apn_completed), 1).show();
                    }
                    break;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ContentResolver resolver = ApnSettings.this.getContentResolver();
                    resolver.delete(ApnSettings.DEFAULTAPN_URI, null, null);
                    this.mRestoreApnUiHandler.sendEmptyMessage(2);
                    break;
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id != 1001) {
            return null;
        }
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getResources().getString(R.string.restore_default_apn));
        dialog.setCancelable(false);
        return dialog;
    }
}

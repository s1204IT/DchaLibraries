package com.android.settings.network;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaApnSetting;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.ext.IApnSettingsExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.TelephonyUtils;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class ApnSettings extends RestrictedSettingsFragment implements Preference.OnPreferenceChangeListener {
    private static final Uri DEFAULTAPN_URI = Uri.parse("content://telephony/carriers/restore");
    private static final Uri PREFERAPN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static boolean mRestoreDefaultApnMode;
    private boolean mAllowAddingApns;
    private IApnSettingsExt mApnExt;
    private boolean mHideImsApn;
    private IntentFilter mMobileStateFilter;
    private final BroadcastReceiver mMobileStateReceiver;
    private String mMvnoMatchData;
    private String mMvnoType;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private RestoreApnUiHandler mRestoreApnUiHandler;
    private HandlerThread mRestoreDefaultApnThread;
    private boolean mRestoreOngoing;
    private String mSelectedKey;
    private SimHotSwapHandler mSimHotSwapHandler;
    private SubscriptionInfo mSubscriptionInfo;
    private UiccController mUiccController;
    private boolean mUnavailable;
    private UserManager mUserManager;

    public ApnSettings() {
        super("no_config_mobile_networks");
        this.mRestoreOngoing = false;
        this.mMobileStateReceiver = new BroadcastReceiver() { // from class: com.android.settings.network.ApnSettings.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals("android.intent.action.ANY_DATA_STATE")) {
                    if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                        ApnSettings.this.updateScreenEnableState(context);
                        return;
                    }
                    return;
                }
                PhoneConstants.DataState mobileDataState = ApnSettings.getMobileDataState(intent);
                Log.d("ApnSettings", "onReceive ACTION_ANY_DATA_CONNECTION_STATE_CHANGED,state = " + mobileDataState);
                if (AnonymousClass4.$SwitchMap$com$android$internal$telephony$PhoneConstants$DataState[mobileDataState.ordinal()] == 1 && !ApnSettings.mRestoreDefaultApnMode) {
                    ApnSettings.this.fillList();
                }
                ApnSettings.this.updateScreenForDataStateChange(context, intent);
            }
        };
    }

    /* renamed from: com.android.settings.network.ApnSettings$4, reason: invalid class name */
    static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$PhoneConstants$DataState = new int[PhoneConstants.DataState.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$PhoneConstants$DataState[PhoneConstants.DataState.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String stringExtra = intent.getStringExtra("state");
        if (stringExtra != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, stringExtra);
        }
        return PhoneConstants.DataState.DISCONNECTED;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 12;
    }

    @Override // com.android.settings.RestrictedSettingsFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        int intExtra = activity.getIntent().getIntExtra("sub_id", -1);
        this.mMobileStateFilter = new IntentFilter("android.intent.action.ANY_DATA_STATE");
        this.mMobileStateFilter.addAction("android.intent.action.AIRPLANE_MODE");
        setIfOnlyAvailableForAdmins(true);
        this.mSubscriptionInfo = SubscriptionManager.from(activity).getActiveSubscriptionInfo(intExtra);
        this.mUiccController = UiccController.getInstance();
        if (this.mSubscriptionInfo == null) {
            Log.d("ApnSettings", "onCreate()... Invalid subId: " + intExtra);
            getActivity().finish();
        }
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() { // from class: com.android.settings.network.ApnSettings.2
            @Override // com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener
            public void onSimHotSwap() {
                Log.d("ApnSettings", "onSimHotSwap, finish activity");
                if (ApnSettings.this.getActivity() != null) {
                    ApnSettings.this.getActivity().finish();
                }
            }
        });
        this.mApnExt = UtilsExt.getApnSettingsExt(activity);
        this.mApnExt.initTetherField(this);
        PersistableBundle config = ((CarrierConfigManager) getSystemService("carrier_config")).getConfig();
        this.mHideImsApn = config.getBoolean("hide_ims_apn_bool");
        this.mAllowAddingApns = config.getBoolean("allow_adding_apns_bool");
        if (this.mAllowAddingApns && ApnEditor.hasAllApns(config.getStringArray("read_only_apn_types_string_array"))) {
            Log.d("ApnSettings", "not allowing adding APN because all APN types are read only");
            this.mAllowAddingApns = false;
        }
        this.mUserManager = UserManager.get(activity);
    }

    @Override // com.android.settings.RestrictedSettingsFragment, com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getEmptyTextView().setText(R.string.apn_settings_not_available);
        this.mUnavailable = isUiRestricted();
        setHasOptionsMenu(!this.mUnavailable);
        if (this.mUnavailable) {
            addPreferencesFromResource(R.xml.placeholder_prefs);
        } else {
            addPreferencesFromResource(R.xml.apn_settings);
        }
    }

    @Override // com.android.settings.RestrictedSettingsFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        if (this.mUnavailable) {
            return;
        }
        getActivity().registerReceiver(this.mMobileStateReceiver, this.mMobileStateFilter);
        if (!mRestoreDefaultApnMode) {
            fillList();
            removeDialog(1001);
        }
        this.mApnExt.updateTetherState();
        this.mApnExt.onApnSettingsEvent(0);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        if (this.mUnavailable) {
            return;
        }
        getActivity().unregisterReceiver(this.mMobileStateReceiver);
        this.mApnExt.onApnSettingsEvent(1);
    }

    @Override // com.android.settings.RestrictedSettingsFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        if (this.mRestoreDefaultApnThread != null) {
            this.mRestoreDefaultApnThread.quit();
        }
        if (this.mSimHotSwapHandler != null) {
            this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        }
        this.mApnExt.onDestroy();
    }

    @Override // com.android.settings.RestrictedSettingsFragment
    public RestrictedLockUtils.EnforcedAdmin getRestrictionEnforcedAdmin() {
        UserHandle userHandleOf = UserHandle.of(this.mUserManager.getUserHandle());
        if (this.mUserManager.hasUserRestriction("no_config_mobile_networks", userHandleOf) && !this.mUserManager.hasBaseUserRestriction("no_config_mobile_networks", userHandleOf)) {
            return RestrictedLockUtils.EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        return null;
    }

    public void fillList() {
        int subscriptionId;
        ArrayList<ApnPreference> arrayList;
        ArrayList<ApnPreference> arrayList2;
        ArrayList<ApnPreference> arrayList3;
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        if (this.mSubscriptionInfo != null) {
            subscriptionId = this.mSubscriptionInfo.getSubscriptionId();
        } else {
            subscriptionId = -1;
        }
        int i = subscriptionId;
        String simOperator = this.mSubscriptionInfo == null ? "" : telephonyManager.getSimOperator(i);
        Log.d("ApnSettings", "before plugin, mccmnc = " + simOperator);
        String operatorNumericFromImpi = this.mApnExt.getOperatorNumericFromImpi(simOperator, SubscriptionManager.getPhoneId(this.mSubscriptionInfo.getSubscriptionId()));
        Log.d("ApnSettings", "mccmnc = " + operatorNumericFromImpi);
        String strCustomizeQuerySelectionforCdma = "numeric=\"" + operatorNumericFromImpi + "\"";
        if (this.mSubscriptionInfo != null && CdmaUtils.isSupportCdma(i)) {
            strCustomizeQuerySelectionforCdma = CdmaApnSetting.customizeQuerySelectionforCdma(strCustomizeQuerySelectionforCdma, operatorNumericFromImpi, i);
        }
        String str = strCustomizeQuerySelectionforCdma + " AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL)) AND user_visible!=0";
        if (!FeatureOption.MTK_VOLTE_SUPPORT || this.mHideImsApn) {
            str = str + " AND NOT (type='ims' OR type='ia,ims')";
        }
        String fillListQuery = this.mApnExt.getFillListQuery(str, operatorNumericFromImpi);
        Log.d("ApnSettings", "fillList where: " + fillListQuery);
        String apnSortOrder = this.mApnExt.getApnSortOrder("name ASC");
        Log.d("ApnSettings", "fillList sort: " + apnSortOrder);
        Cursor cursorQuery = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[]{"_id", "name", "apn", "type", "mvno_type", "mvno_match_data", "sourcetype"}, fillListQuery.toString(), null, apnSortOrder);
        if (cursorQuery != null) {
            Log.d("ApnSettings", "fillList, cursor count: " + cursorQuery.getCount());
            int i2 = 1;
            IccRecords iccRecords = (this.mUiccController == null || this.mSubscriptionInfo == null) ? null : this.mUiccController.getIccRecords(SubscriptionManager.getPhoneId(i), 1);
            PreferenceGroup preferenceGroup = (PreferenceGroup) findPreference("apn_list");
            preferenceGroup.removeAll();
            ArrayList<ApnPreference> arrayList4 = new ArrayList<>();
            ArrayList<ApnPreference> arrayList5 = new ArrayList<>();
            ArrayList<ApnPreference> arrayList6 = new ArrayList<>();
            ArrayList<ApnPreference> arrayList7 = new ArrayList<>();
            this.mSelectedKey = getSelectedApnKey();
            cursorQuery.moveToFirst();
            while (!cursorQuery.isAfterLast()) {
                String string = cursorQuery.getString(i2);
                String string2 = cursorQuery.getString(2);
                String string3 = cursorQuery.getString(0);
                String string4 = cursorQuery.getString(3);
                String string5 = cursorQuery.getString(4);
                String string6 = cursorQuery.getString(5);
                int i3 = cursorQuery.getInt(6);
                if (shouldSkipApn(string4)) {
                    cursorQuery.moveToNext();
                } else {
                    String strUpdateApnName = this.mApnExt.updateApnName(string, i3);
                    ArrayList<ApnPreference> arrayList8 = arrayList7;
                    ApnPreference apnPreference = new ApnPreference(getPrefContext());
                    apnPreference.setKey(string3);
                    apnPreference.setTitle(strUpdateApnName);
                    apnPreference.setSummary(string2);
                    apnPreference.setPersistent(false);
                    apnPreference.setOnPreferenceChangeListener(this);
                    apnPreference.setSubId(i);
                    apnPreference.setApnEditable(this.mApnExt.isAllowEditPresetApn(string4, string2, operatorNumericFromImpi, i3));
                    apnPreference.setSubId((this.mSubscriptionInfo == null ? null : Integer.valueOf(this.mSubscriptionInfo.getSubscriptionId())).intValue());
                    boolean z = (string4 == null || !(string4.equals("mms") || string4.equals("ia") || string4.equals("ims") || string4.equals("emergency"))) && this.mApnExt.isSelectable(string4);
                    apnPreference.setSelectable(z);
                    Log.d("ApnSettings", "mSelectedKey = " + this.mSelectedKey + " key = " + string3 + " name = " + strUpdateApnName + " selectable=" + z);
                    if (z) {
                        arrayList3 = arrayList8;
                        arrayList = arrayList6;
                        arrayList2 = arrayList5;
                        addApnToList(apnPreference, arrayList4, arrayList5, iccRecords, string5, string6);
                        this.mApnExt.customizeUnselectableApn(string2, string5, string6, arrayList4, arrayList2, (this.mSubscriptionInfo == null ? null : Integer.valueOf(this.mSubscriptionInfo.getSubscriptionId())).intValue());
                    } else {
                        arrayList = arrayList6;
                        arrayList2 = arrayList5;
                        arrayList3 = arrayList8;
                        addApnToList(apnPreference, arrayList, arrayList3, iccRecords, string5, string6);
                        this.mApnExt.customizeUnselectableApn(string2, string5, string6, arrayList, arrayList3, (this.mSubscriptionInfo == null ? null : Integer.valueOf(this.mSubscriptionInfo.getSubscriptionId())).intValue());
                    }
                    cursorQuery.moveToNext();
                    arrayList7 = arrayList3;
                    arrayList6 = arrayList;
                    arrayList5 = arrayList2;
                }
                i2 = 1;
            }
            ArrayList<ApnPreference> arrayList9 = arrayList7;
            ArrayList<ApnPreference> arrayList10 = arrayList6;
            ArrayList<ApnPreference> arrayList11 = arrayList5;
            cursorQuery.close();
            if (arrayList11.isEmpty()) {
                arrayList9 = arrayList10;
            } else {
                arrayList4 = arrayList11;
            }
            Iterator<ApnPreference> it = arrayList4.iterator();
            while (it.hasNext()) {
                preferenceGroup.addPreference(it.next());
            }
            Iterator<ApnPreference> it2 = arrayList9.iterator();
            while (it2.hasNext()) {
                preferenceGroup.addPreference(it2.next());
            }
            setPreferApnChecked(arrayList4);
            updateScreenEnableState(getActivity());
        }
    }

    private void addApnToList(ApnPreference apnPreference, ArrayList<ApnPreference> arrayList, ArrayList<ApnPreference> arrayList2, IccRecords iccRecords, String str, String str2) {
        Log.d("ApnSettings", "mvnoType = " + str + ", mvnoMatchData = " + str2);
        if (iccRecords != null && !TextUtils.isEmpty(str) && !TextUtils.isEmpty(str2)) {
            if (ApnSetting.mvnoMatches(iccRecords, str, str2)) {
                arrayList2.add(apnPreference);
                this.mMvnoType = str;
                this.mMvnoMatchData = str2;
                Log.d("ApnSettings", "mvnoMatches...");
                return;
            }
            return;
        }
        arrayList.add(apnPreference);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if (!this.mUnavailable) {
            if (this.mAllowAddingApns && !this.mRestoreOngoing) {
                menu.add(0, 1, 0, getResources().getString(R.string.menu_new)).setIcon(R.drawable.ic_menu_add_white).setShowAsAction(1);
            }
            menu.add(0, 2, 0, getResources().getString(R.string.menu_restore)).setIcon(android.R.drawable.ic_menu_upload);
        }
        this.mApnExt.updateMenu(menu, 1, 2, TelephonyManager.getDefault().getSimOperator(this.mSubscriptionInfo != null ? this.mSubscriptionInfo.getSubscriptionId() : -1));
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 1:
                addNewApn();
                return true;
            case 2:
                restoreDefaultApn();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void addNewApn() {
        if (!this.mRestoreOngoing) {
            Log.d("ApnSettings", "addNewApn...");
            Intent intent = new Intent("android.intent.action.INSERT", Telephony.Carriers.CONTENT_URI);
            intent.putExtra("sub_id", this.mSubscriptionInfo != null ? this.mSubscriptionInfo.getSubscriptionId() : -1);
            if (!TextUtils.isEmpty(this.mMvnoType) && !TextUtils.isEmpty(this.mMvnoMatchData)) {
                intent.putExtra("mvno_type", this.mMvnoType);
                intent.putExtra("mvno_match_data", this.mMvnoMatchData);
            }
            this.mApnExt.addApnTypeExtra(intent);
            startActivity(intent);
        }
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) throws NumberFormatException {
        Intent intent = new Intent("android.intent.action.EDIT", ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, Integer.parseInt(preference.getKey())));
        Log.d("ApnSettings", "put subid = " + this.mSubscriptionInfo.getSubscriptionId());
        intent.putExtra("sub_id", this.mSubscriptionInfo.getSubscriptionId());
        startActivity(intent);
        return true;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Log.d("ApnSettings", "onPreferenceChange(): Preference - " + preference + ", newValue - " + obj + ", newValue type - " + obj.getClass());
        if (obj instanceof String) {
            setSelectedApnKey((String) obj);
            return true;
        }
        return true;
    }

    private void setSelectedApnKey(String str) {
        this.mSelectedKey = str;
        ContentResolver contentResolver = getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put("apn_id", this.mSelectedKey);
        contentResolver.update(getUriForCurrSubId(PREFERAPN_URI), contentValues, null, null);
    }

    private String getSelectedApnKey() {
        String string;
        Cursor cursorQuery = getContentResolver().query(getUriForCurrSubId(PREFERAPN_URI), new String[]{"_id"}, null, null, "name ASC");
        if (cursorQuery.getCount() > 0) {
            cursorQuery.moveToFirst();
            string = cursorQuery.getString(0);
        } else {
            string = null;
        }
        cursorQuery.close();
        Log.d("ApnSettings", "getSelectedApnKey(), key = " + string);
        return string;
    }

    private boolean restoreDefaultApn() {
        Log.d("ApnSettings", "restoreDefaultApn...");
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

    private Uri getUriForCurrSubId(Uri uri) {
        int subscriptionId = this.mSubscriptionInfo != null ? this.mSubscriptionInfo.getSubscriptionId() : -1;
        if (SubscriptionManager.isValidSubscriptionId(subscriptionId)) {
            return Uri.withAppendedPath(uri, "subId/" + String.valueOf(subscriptionId));
        }
        return uri;
    }

    private class RestoreApnUiHandler extends Handler {
        private RestoreApnUiHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (message.what == 2) {
                Log.d("ApnSettings", "restore APN complete~~");
                Activity activity = ApnSettings.this.getActivity();
                if (activity == null) {
                    boolean unused = ApnSettings.mRestoreDefaultApnMode = false;
                    return;
                }
                ApnSettings.this.fillList();
                ApnSettings.this.getPreferenceScreen().setEnabled(!(Settings.System.getInt(activity.getContentResolver(), "airplane_mode_on", -1) == 1));
                boolean unused2 = ApnSettings.mRestoreDefaultApnMode = false;
                ApnSettings.this.removeDialog(1001);
                Toast.makeText(activity, ApnSettings.this.getResources().getString(R.string.restore_default_apn_completed), 1).show();
                ApnSettings.this.mRestoreOngoing = false;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler handler) {
            super(looper);
            this.mRestoreApnUiHandler = handler;
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (message.what == 1) {
                Log.d("ApnSettings", "restore APN start~~");
                ApnSettings.this.getContentResolver().delete(ApnSettings.this.getUriForCurrSubId(ApnSettings.DEFAULTAPN_URI), null, null);
                this.mRestoreApnUiHandler.sendEmptyMessage(2);
                ApnSettings.this.mRestoreOngoing = true;
            }
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public Dialog onCreateDialog(int i) {
        if (i == 1001) {
            ProgressDialog progressDialog = new ProgressDialog(getActivity()) { // from class: com.android.settings.network.ApnSettings.3
                @Override // android.app.Dialog
                public boolean onTouchEvent(MotionEvent motionEvent) {
                    return true;
                }
            };
            progressDialog.setMessage(getResources().getString(R.string.restore_default_apn));
            progressDialog.setCancelable(false);
            return progressDialog;
        }
        return null;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public int getDialogMetricsCategory(int i) {
        if (i == 1001) {
            return 579;
        }
        return 0;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [701=4] */
    private void updateScreenForDataStateChange(Context context, Intent intent) {
        String stringExtra = intent.getStringExtra("apnType");
        Log.d("ApnSettings", "Receiver,send MMS status, get type = " + stringExtra);
        if ("mms".equals(stringExtra)) {
            boolean z = false;
            boolean z2 = Settings.System.getInt(context.getContentResolver(), "airplane_mode_on", -1) == 1;
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            if (!z2 && !isMmsInTransaction(context) && this.mApnExt.getScreenEnableState(this.mSubscriptionInfo.getSubscriptionId(), getActivity())) {
                z = true;
            }
            preferenceScreen.setEnabled(z);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [714=4] */
    private void updateScreenEnableState(Context context) {
        int subscriptionId = this.mSubscriptionInfo.getSubscriptionId();
        boolean z = false;
        boolean z2 = 5 == TelephonyManager.getDefault().getSimState(SubscriptionManager.getSlotIndex(subscriptionId));
        boolean z3 = Settings.System.getInt(context.getContentResolver(), "airplane_mode_on", -1) == 1;
        boolean z4 = !z3 && z2;
        Log.d("ApnSettings", "updateScreenEnableState(), subId = " + subscriptionId + " ,airplaneModeEnabled = " + z3 + " ,simReady = " + z2);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (z4 && this.mApnExt.getScreenEnableState(subscriptionId, getActivity())) {
            z = true;
        }
        preferenceScreen.setEnabled(z);
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private boolean isMmsInTransaction(Context context) {
        NetworkInfo networkInfo;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivityManager == null || (networkInfo = connectivityManager.getNetworkInfo(2)) == null) {
            return false;
        }
        NetworkInfo.State state = networkInfo.getState();
        Log.d("ApnSettings", "mms state = " + state);
        return state == NetworkInfo.State.CONNECTING || state == NetworkInfo.State.CONNECTED;
    }

    public boolean shouldSkipApn(String str) {
        return "cmmail".equals(str);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPrepareOptionsMenu(Menu menu) {
        int size = menu.size();
        boolean zIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity());
        Log.d("ApnSettings", "onPrepareOptionsMenu isAirplaneModeOn = " + zIsAirplaneModeOn);
        for (int i = 0; i < size; i++) {
            menu.getItem(i).setEnabled(!zIsAirplaneModeOn);
        }
        super.onPrepareOptionsMenu(menu);
    }

    private void setPreferApnChecked(ArrayList<ApnPreference> arrayList) {
        if (arrayList == null || arrayList.isEmpty()) {
            return;
        }
        String key = null;
        if (this.mSelectedKey != null) {
            Iterator<ApnPreference> it = arrayList.iterator();
            while (it.hasNext()) {
                ApnPreference next = it.next();
                if (this.mSelectedKey.equals(next.getKey())) {
                    next.setChecked();
                    key = this.mSelectedKey;
                }
            }
        }
        if (this.mApnExt.shouldSelectFirstApn() && key == null && arrayList.get(0) != null) {
            arrayList.get(0).setChecked();
            key = arrayList.get(0).getKey();
        }
        if (key != null && key != this.mSelectedKey) {
            setSelectedApnKey(key);
            this.mSelectedKey = key;
        }
        Log.d("ApnSettings", "setPreferApnChecked, APN = " + this.mSelectedKey);
    }

    public void onIntentUpdate(Intent intent) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        int intExtra = intent.getIntExtra("sub_id", -1);
        this.mSubscriptionInfo = SubscriptionManager.from(activity).getActiveSubscriptionInfo(intExtra);
        Log.d("ApnSettings", "onIntentUpdate sub_id = " + intExtra);
    }
}

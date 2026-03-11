package com.mediatek.wifi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.settings.ApnPreference;
import com.android.settings.R;
import com.android.settings.wifi.WifiSettings;
import com.mediatek.internal.telephony.CellConnMgr;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiGprsSelector extends WifiSettings implements Preference.OnPreferenceChangeListener {
    private PreferenceCategory mApnList;
    private CellConnMgr mCellConnMgr;
    private CheckBoxPreference mDataEnabler;
    private Preference mDataEnablerGemini;
    private int mInitValue;
    private IntentFilter mMobileStateFilter;
    private Uri mRestoreCarrierUri;
    private String mSelectedKey;
    private Map<Integer, SubscriptionInfo> mSimMap;
    private int mSubId;
    private TelephonyManager mTelephonyManager;
    private Uri mUri;
    private WifiManager mWifiManager;
    private static final String[] PROJECTION_ARRAY = {"_id", "name", "apn", "type", "sourcetype"};
    private static final Uri PREFERAPN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private boolean mIsCallStateIdle = true;
    private boolean mAirplaneModeEnabled = false;
    private boolean mIsSIMExist = true;
    private List<Integer> mSimMapKeyList = null;
    private boolean mScreenEnable = true;
    private boolean mIsGprsSwitching = false;
    private int mSelectedDataSubId = -1;
    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED".equals(action)) {
                long subId = intent.getLongExtra("subscription", -1L);
                Log.d("@M_WifiGprsSelector", "changed default data subId: " + subId);
                WifiGprsSelector.this.mTimeHandler.removeMessages(2001);
                if (WifiGprsSelector.this.isResumed()) {
                    WifiGprsSelector.this.removeDialog(1001);
                }
                WifiGprsSelector.this.mIsGprsSwitching = false;
                WifiGprsSelector.this.updateDataEnabler();
                return;
            }
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                WifiGprsSelector.this.mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                Log.d("@M_WifiGprsSelector", "AIRPLANE_MODE state changed: " + WifiGprsSelector.this.mAirplaneModeEnabled + ";");
                WifiGprsSelector.this.mApnList.setEnabled(WifiGprsSelector.this.mAirplaneModeEnabled ? false : true);
                WifiGprsSelector.this.updateDataEnabler();
                return;
            }
            if (action.equals("com.android.mms.transaction.START")) {
                Log.d("@M_WifiGprsSelector", "ssr: TRANSACTION_START in ApnSettings;");
                WifiGprsSelector.this.mScreenEnable = false;
                WifiGprsSelector.this.mApnList.setEnabled(WifiGprsSelector.this.mAirplaneModeEnabled ? false : WifiGprsSelector.this.mScreenEnable);
            } else if (action.equals("com.android.mms.transaction.STOP")) {
                Log.d("@M_WifiGprsSelector", "ssr: TRANSACTION_STOP in ApnSettings;");
                WifiGprsSelector.this.mScreenEnable = true;
                WifiGprsSelector.this.mApnList.setEnabled(WifiGprsSelector.this.mAirplaneModeEnabled ? false : WifiGprsSelector.this.mScreenEnable);
            } else {
                if (!"android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE".equals(action)) {
                    return;
                }
                Log.d("@M_WifiGprsSelector", "receive ACTION_SIM_INFO_UPDATE");
                List<SubscriptionInfo> simList = SubscriptionManager.from(WifiGprsSelector.this.getActivity()).getActiveSubscriptionInfoList();
                if (simList == null) {
                    return;
                }
                WifiGprsSelector.this.mSubId = WifiGprsSelector.this.getSubId();
                WifiGprsSelector.this.updateDataEnabler();
            }
        }
    };
    ContentObserver mGprsConnectObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.i("@M_WifiGprsSelector", "Gprs connection changed");
            WifiGprsSelector.this.mSubId = WifiGprsSelector.this.getSubId();
            WifiGprsSelector.this.updateDataEnabler();
        }
    };
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            super.onServiceStateChanged(serviceState);
            WifiGprsSelector.this.mIsCallStateIdle = WifiGprsSelector.this.mTelephonyManager.getCallState() == 0;
        }
    };
    private Runnable mServiceComplete = new Runnable() {
        @Override
        public void run() {
        }
    };
    Handler mTimeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2000:
                    Log.d("@M_WifiGprsSelector", "detach time out......");
                    if (WifiGprsSelector.this.isResumed()) {
                        WifiGprsSelector.this.removeDialog(1001);
                    }
                    WifiGprsSelector.this.mIsGprsSwitching = false;
                    WifiGprsSelector.this.updateDataEnabler();
                    break;
                case 2001:
                    Log.d("@M_WifiGprsSelector", "attach time out......");
                    if (WifiGprsSelector.this.isResumed()) {
                        WifiGprsSelector.this.removeDialog(1001);
                    }
                    WifiGprsSelector.this.mIsGprsSwitching = false;
                    WifiGprsSelector.this.updateDataEnabler();
                    break;
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("@M_WifiGprsSelector", "onActivityCreated()");
        addPreferencesFromResource(R.xml.wifi_access_points_and_gprs);
        this.mApnList = (PreferenceCategory) findPreference("apn_list");
        PreferenceCategory dataEnableCategory = (PreferenceCategory) findPreference("data_enabler_category");
        if (isGeminiSupport()) {
            this.mDataEnablerGemini = findPreference("data_enabler_gemini");
            dataEnableCategory.removePreference(findPreference("data_enabler"));
        } else {
            this.mDataEnabler = (CheckBoxPreference) findPreference("data_enabler");
            this.mDataEnabler.setOnPreferenceChangeListener(this);
            dataEnableCategory.removePreference(findPreference("data_enabler_gemini"));
        }
        initPhoneState();
        this.mMobileStateFilter = new IntentFilter("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        this.mMobileStateFilter.addAction("android.intent.action.AIRPLANE_MODE");
        this.mMobileStateFilter.addAction("com.android.mms.transaction.START");
        this.mMobileStateFilter.addAction("com.android.mms.transaction.STOP");
        this.mMobileStateFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mMobileStateFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        this.mMobileStateFilter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
        getActivity().setTitle(R.string.wifi_gprs_selector_title);
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        init();
        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        Log.d("@M_WifiGprsSelector", "onResume");
        super.onResume();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
        getActivity().registerReceiver(this.mMobileStateReceiver, this.mMobileStateFilter);
        this.mAirplaneModeEnabled = Settings.Global.getInt(getActivity().getContentResolver(), "airplane_mode_on", 0) != 0;
        this.mWifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService("wifi");
        this.mScreenEnable = isMMSNotTransaction();
        fillList(this.mSubId);
        updateDataEnabler();
        if (isGeminiSupport()) {
            this.mCellConnMgr = new CellConnMgr(getActivity());
            getContentResolver().registerContentObserver(Settings.System.getUriFor("gprs_connection_sim_setting"), false, this.mGprsConnectObserver);
        }
        if (!this.mIsGprsSwitching) {
            return;
        }
        showDialog(1001);
    }

    private boolean isMMSNotTransaction() {
        NetworkInfo networkInfo;
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        if (cm == null || (networkInfo = cm.getNetworkInfo(2)) == null) {
            return true;
        }
        NetworkInfo.State state = networkInfo.getState();
        Log.d("@M_WifiGprsSelector", "mms state = " + state);
        return (state == NetworkInfo.State.CONNECTING || state == NetworkInfo.State.CONNECTED) ? false : true;
    }

    private boolean init() {
        Log.d("@M_WifiGprsSelector", "init()");
        this.mIsSIMExist = this.mTelephonyManager.hasIccCard();
        return true;
    }

    @Override
    public void onPause() {
        Log.d("@M_WifiGprsSelector", "onPause");
        super.onPause();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        getActivity().unregisterReceiver(this.mMobileStateReceiver);
        if (isGeminiSupport()) {
            getContentResolver().unregisterContentObserver(this.mGprsConnectObserver);
        }
        if (!this.mIsGprsSwitching) {
            return;
        }
        removeDialog(1001);
    }

    @Override
    public void onDestroy() {
        this.mTimeHandler.removeMessages(2001);
        this.mTimeHandler.removeMessages(2000);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    private void initPhoneState() {
        Log.d("@M_WifiGprsSelector", "initPhoneState()");
        Intent it = getActivity().getIntent();
        this.mSubId = it.getIntExtra("simId", -1);
        this.mSimMap = new HashMap();
        initSimMap();
        if (this.mSubId == -1) {
            this.mSubId = getSubId();
        }
        Log.d("@M_WifiGprsSelector", "GEMINI_SIM_ID_KEY = " + this.mSubId + ";");
    }

    private void fillList(int subId) {
        ApnPreference apnPref;
        boolean selectable;
        this.mApnList.removeAll();
        if (subId < 0) {
            return;
        }
        Log.d("@M_WifiGprsSelector", "fillList(), subId=" + subId + ";");
        String where = ("numeric=\"" + getQueryWhere(subId) + "\"") + " AND NOT (type='ia' AND (apn='' OR apn IS NULL))";
        if (!SystemProperties.get("persist.mtk_volte_support").equals("1")) {
            where = where + " AND NOT type='ims'";
        }
        Log.d("@M_WifiGprsSelector", "where = " + where + ";");
        Cursor cursor = getActivity().managedQuery(this.mUri, PROJECTION_ARRAY, where, "name ASC");
        ArrayList<Preference> mmsApnList = new ArrayList<>();
        boolean keySetChecked = false;
        this.mSelectedKey = getSelectedApnKey();
        Log.d("@M_WifiGprsSelector", "mSelectedKey = " + this.mSelectedKey + ";");
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String name = cursor.getString(1);
            String apn = cursor.getString(2);
            String key = cursor.getString(0);
            String type = cursor.getString(3);
            cursor.getInt(4);
            ApnPreference pref = new ApnPreference(getPrefContext());
            pref.setSubId(subId);
            pref.setKey(key);
            pref.setTitle(name);
            pref.setSummary(apn);
            pref.setPersistent(false);
            pref.setOnPreferenceChangeListener(this);
            if (type == null) {
                selectable = true;
            } else if (type.equals("mms") || type.equals("cmmail") || type.equals("ims")) {
                selectable = false;
            } else {
                selectable = !type.equals("emergency");
            }
            pref.setSelectable(selectable);
            if (selectable) {
                if (this.mSelectedKey != null && this.mSelectedKey.equals(key)) {
                    setSelectedApnKey(key);
                    pref.setChecked();
                    keySetChecked = true;
                    Log.d("@M_WifiGprsSelector", "apn key: " + key + " set.;");
                }
                Log.d("@M_WifiGprsSelector", "key:  " + key + " added!;");
                this.mApnList.addPreference(pref);
                if (isGeminiSupport()) {
                    pref.setDependency("data_enabler_gemini");
                } else {
                    pref.setDependency("data_enabler");
                }
            } else {
                mmsApnList.add(pref);
            }
            cursor.moveToNext();
        }
        int mSelectableApnCount = this.mApnList.getPreferenceCount();
        if (!keySetChecked && mSelectableApnCount > 0 && (apnPref = (ApnPreference) this.mApnList.getPreference(0)) != null) {
            setSelectedApnKey(apnPref.getKey());
            apnPref.setChecked();
            Log.d("@M_WifiGprsSelector", "Key does not match.Set key: " + apnPref.getKey() + ".");
        }
        this.mIsCallStateIdle = this.mTelephonyManager.getCallState() == 0;
        int slotId = SubscriptionManager.getSlotId(subId);
        boolean simReady = 5 == this.mTelephonyManager.getSimState(slotId);
        PreferenceCategory preferenceCategory = this.mApnList;
        if (!this.mScreenEnable || !this.mIsCallStateIdle || this.mAirplaneModeEnabled) {
            simReady = false;
        }
        preferenceCategory.setEnabled(simReady);
    }

    private String getQueryWhere(int subId) {
        String numeric = TelephonyManager.getDefault().getSimOperator(subId);
        this.mUri = Telephony.Carriers.CONTENT_URI.buildUpon().appendPath("subId").appendPath(Integer.toString(subId)).build();
        this.mRestoreCarrierUri = PREFERAPN_URI.buildUpon().appendPath("subId").appendPath(Integer.toString(subId)).build();
        Log.d("@M_WifiGprsSelector", "where = " + numeric + ";");
        Log.d("@M_WifiGprsSelector", "mUri = " + this.mUri + ";");
        return numeric;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("@M_WifiGprsSelector", "onPreferenceChange(): Preference - " + preference + ", newValue - " + newValue + ", newValue type - " + newValue.getClass());
        String key = preference == null ? "" : preference.getKey();
        if ("data_enabler".equals(key)) {
            boolean checked = ((Boolean) newValue).booleanValue();
            Log.d("@M_WifiGprsSelector", "Data connection enabled?" + checked);
            dealWithConnChange(checked);
            return true;
        }
        if (!(newValue instanceof String)) {
            return true;
        }
        setSelectedApnKey((String) newValue);
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if (!"data_enabler_gemini".equals(key)) {
            return super.onPreferenceTreeClick(preference);
        }
        final List<SimItem> simItemList = new ArrayList<>();
        for (Integer simid : this.mSimMapKeyList) {
            SubscriptionInfo subinfo = this.mSimMap.get(simid);
            if (subinfo != null) {
                SimItem simitem = new SimItem(subinfo, this);
                simitem.mState = this.mTelephonyManager.getSimState(subinfo.getSimSlotIndex());
                simItemList.add(simitem);
            }
        }
        int simListSize = simItemList.size();
        Log.d("@M_WifiGprsSelector", "simListSize = " + simListSize);
        int offItem = simListSize - 1;
        int index = -1;
        int dataConnectId = SubscriptionManager.getDefaultDataSubscriptionId();
        Log.d("@M_WifiGprsSelector", "getSimSlot,dataConnectId = " + dataConnectId);
        for (int i = 0; i < offItem; i++) {
            if (simItemList.get(i).mSubId == dataConnectId) {
                index = i;
            }
        }
        if (index != -1) {
            offItem = index;
        }
        this.mInitValue = offItem;
        Log.d("@M_WifiGprsSelector", "mInitValue = " + this.mInitValue);
        SelectionListAdapter mAdapter = new SelectionListAdapter(simItemList);
        AlertDialog dialog = new AlertDialog.Builder(getPrefContext()).setSingleChoiceItems(mAdapter, this.mInitValue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                Log.d("@M_WifiGprsSelector", "which = " + which);
                SimItem simItem = (SimItem) simItemList.get(which);
                WifiGprsSelector.this.mSubId = simItem.mSubId;
                Log.d("@M_WifiGprsSelector", "mSubId = " + WifiGprsSelector.this.mSubId);
                Log.d("@M_WifiGprsSelector", "mIsSim=" + simItem.mIsSim + ",mState=" + simItem.mState + ",SIM_INDICATOR_LOCKED=1");
                if (simItem.mIsSim) {
                    int state = WifiGprsSelector.this.mCellConnMgr.getCurrentState(simItem.mSubId, 4);
                    if (WifiGprsSelector.this.mCellConnMgr != null && state == 4) {
                        Log.d("@M_WifiGprsSelector", "mCellConnMgr.handleCellConn");
                        WifiGprsSelector.this.showDialog(1002);
                    } else {
                        WifiGprsSelector.this.switchGprsDefautlSIM(simItem.mSubId);
                    }
                } else {
                    WifiGprsSelector.this.switchGprsDefautlSIM(0);
                }
                dialog2.dismiss();
            }
        }).setTitle(R.string.data_conn_category_title).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
            }
        }).create();
        dialog.show();
        return true;
    }

    public int getSubId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    private void setSelectedApnKey(String key) {
        this.mSelectedKey = key;
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put("apn_id", this.mSelectedKey);
        resolver.update(this.mRestoreCarrierUri, values, null, null);
    }

    private String getSelectedApnKey() {
        Cursor cursor = getActivity().managedQuery(this.mRestoreCarrierUri, new String[]{"_id"}, null, "name ASC");
        if (cursor.getCount() <= 0) {
            return null;
        }
        cursor.moveToFirst();
        String key = cursor.getString(0);
        return key;
    }

    public void updateDataEnabler() {
        boolean z = false;
        if (isGeminiSupport()) {
            Log.d("@M_WifiGprsSelector", "updateDataEnabler, mSubId=" + this.mSubId);
            fillList(this.mSubId);
            Preference preference = this.mDataEnablerGemini;
            if (this.mIsSIMExist && !this.mAirplaneModeEnabled) {
                z = true;
            }
            preference.setEnabled(z);
            return;
        }
        boolean enabled = this.mTelephonyManager.getDataEnabled();
        Log.d("@M_WifiGprsSelector", "updateDataEnabler(), current state=" + enabled);
        this.mDataEnabler.setChecked(enabled);
        Log.d("@M_WifiGprsSelector", "single card mDataEnabler, true");
        CheckBoxPreference checkBoxPreference = this.mDataEnabler;
        if (this.mIsSIMExist && !this.mAirplaneModeEnabled) {
            z = true;
        }
        checkBoxPreference.setEnabled(z);
    }

    private void dealWithConnChange(boolean enabled) {
        if (isGeminiSupport()) {
            Log.d("@M_WifiGprsSelector", "only sigle SIM load can controling data connection");
            return;
        }
        Log.d("@M_WifiGprsSelector", "dealWithConnChange(),new request state is enabled?" + enabled + ";");
        this.mTelephonyManager.setDataEnabled(enabled);
        showDialog(1001);
        this.mIsGprsSwitching = true;
        if (enabled) {
            this.mTimeHandler.sendEmptyMessageDelayed(2001, 30000L);
        } else {
            this.mTimeHandler.sendEmptyMessageDelayed(2000, 10000L);
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        if (id == 1001) {
            dialog.setMessage(getResources().getString(R.string.data_enabler_waiting_message));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            return dialog;
        }
        if (id == 1002) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            new ArrayList();
            ArrayList<String> simStatusStrings = this.mCellConnMgr.getStringUsingState(this.mSelectedDataSubId, 4);
            builder.setTitle(simStatusStrings.get(0));
            builder.setMessage(simStatusStrings.get(1));
            builder.setPositiveButton(simStatusStrings.get(2), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                    WifiGprsSelector.this.mCellConnMgr.handleRequest(WifiGprsSelector.this.mSelectedDataSubId, 4);
                }
            });
            builder.setNegativeButton(simStatusStrings.get(3), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                }
            });
            return builder.create();
        }
        return super.onCreateDialog(id);
    }

    private void initSimMap() {
        List<SubscriptionInfo> simList = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoList();
        if (simList == null) {
            return;
        }
        this.mSimMap.clear();
        Log.i("@M_WifiGprsSelector", "sim number is " + simList.size());
        for (SubscriptionInfo subinfo : simList) {
            this.mSimMap.put(Integer.valueOf(subinfo.getSubscriptionId()), subinfo);
        }
        this.mSimMapKeyList = new ArrayList(this.mSimMap.keySet());
    }

    public void switchGprsDefautlSIM(int subId) {
        if (subId < 0 || !SubscriptionManager.isValidSubscriptionId(subId) || subId == SubscriptionManager.getDefaultDataSubscriptionId()) {
            return;
        }
        SubscriptionManager.from(getActivity()).setDefaultDataSubId(subId);
        showDialog(1001);
        this.mIsGprsSwitching = true;
        if (subId > 0) {
            this.mTimeHandler.sendEmptyMessageDelayed(2001, 30000L);
            Log.d("@M_WifiGprsSelector", "set ATTACH_TIME_OUT");
        } else {
            this.mTimeHandler.sendEmptyMessageDelayed(2000, 10000L);
            Log.d("@M_WifiGprsSelector", "set DETACH_TIME_OUT");
        }
    }

    public int getStatusResource(int state) {
        switch (state) {
            case DefaultWfcSettingsExt.PAUSE:
                return 134349075;
            case DefaultWfcSettingsExt.CREATE:
                return 134349056;
            case DefaultWfcSettingsExt.DESTROY:
                return 134349049;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                return 134349082;
            case 5:
            default:
                return -1;
            case 6:
                return 134349080;
            case 7:
                return 134349036;
            case 8:
                return 134349081;
        }
    }

    private boolean isGeminiSupport() {
        TelephonyManager.MultiSimVariants config = TelephonyManager.getDefault().getMultiSimConfiguration();
        if (config == TelephonyManager.MultiSimVariants.DSDS || config == TelephonyManager.MultiSimVariants.DSDA) {
            return true;
        }
        return false;
    }

    class SelectionListAdapter extends BaseAdapter {
        List<SimItem> mSimItemList;

        public SelectionListAdapter(List<SimItem> simItemList) {
            this.mSimItemList = simItemList;
        }

        @Override
        public int getCount() {
            return this.mSimItemList.size();
        }

        @Override
        public Object getItem(int position) {
            return this.mSimItemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater mFlater = LayoutInflater.from(WifiGprsSelector.this.getPrefContext());
                convertView = mFlater.inflate(R.layout.preference_sim_default_select, (ViewGroup) null);
                holder = new ViewHolder();
                setViewHolderId(holder, convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            SimItem simItem = (SimItem) getItem(position);
            setNameAndNum(holder.mTextName, holder.mTextNum, simItem);
            setImageSim(holder.mImageSim, simItem);
            setImageStatus(holder.mImageStatus, simItem);
            setTextNumFormat(holder.mTextNumFormat, simItem);
            holder.mCkRadioOn.setChecked(WifiGprsSelector.this.mInitValue == position);
            if (simItem.mState == 1) {
                convertView.setEnabled(false);
                holder.mTextName.setEnabled(false);
                holder.mTextNum.setEnabled(false);
                holder.mCkRadioOn.setEnabled(false);
            } else {
                convertView.setEnabled(true);
                holder.mTextName.setEnabled(true);
                holder.mTextNum.setEnabled(true);
                holder.mCkRadioOn.setEnabled(true);
            }
            return convertView;
        }

        private void setTextNumFormat(TextView textNumFormat, SimItem simItem) {
            if (!simItem.mIsSim || simItem.mNumber == null) {
                return;
            }
            switch (simItem.mDispalyNumberFormat) {
                case DefaultWfcSettingsExt.RESUME:
                    textNumFormat.setVisibility(8);
                    break;
                case DefaultWfcSettingsExt.PAUSE:
                    textNumFormat.setVisibility(0);
                    if (simItem.mNumber.length() >= 4) {
                        textNumFormat.setText(simItem.mNumber.substring(0, 4));
                    } else {
                        textNumFormat.setText(simItem.mNumber);
                    }
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    textNumFormat.setVisibility(0);
                    if (simItem.mNumber.length() >= 4) {
                        textNumFormat.setText(simItem.mNumber.substring(simItem.mNumber.length() - 4));
                    } else {
                        textNumFormat.setText(simItem.mNumber);
                    }
                    break;
            }
        }

        private void setImageStatus(ImageView imageStatus, SimItem simItem) {
            if (!simItem.mIsSim) {
                return;
            }
            int res = WifiGprsSelector.this.getStatusResource(simItem.mState);
            if (res == -1) {
                imageStatus.setVisibility(8);
            } else {
                imageStatus.setVisibility(0);
                imageStatus.setImageResource(res);
            }
        }

        private void setImageSim(RelativeLayout imageSim, SimItem simItem) {
            if (simItem.mIsSim) {
                Bitmap resColor = simItem.mSimIconBitmap;
                if (resColor == null) {
                    return;
                }
                Drawable drawable = new BitmapDrawable(resColor);
                imageSim.setVisibility(0);
                imageSim.setBackground(drawable);
                return;
            }
            if (simItem.mColor == 8) {
                imageSim.setVisibility(0);
                imageSim.setBackgroundResource(134349026);
            } else {
                imageSim.setVisibility(8);
            }
        }

        private void setViewHolderId(ViewHolder holder, View convertView) {
            holder.mTextName = (TextView) convertView.findViewById(R.id.simNameSel);
            holder.mTextNum = (TextView) convertView.findViewById(R.id.simNumSel);
            holder.mImageStatus = (ImageView) convertView.findViewById(R.id.simStatusSel);
            holder.mTextNumFormat = (TextView) convertView.findViewById(R.id.simNumFormatSel);
            holder.mCkRadioOn = (RadioButton) convertView.findViewById(R.id.Enable_select);
            holder.mImageSim = (RelativeLayout) convertView.findViewById(R.id.simIconSel);
        }

        private void setNameAndNum(TextView textName, TextView textNum, SimItem simItem) {
            if (simItem.mName == null) {
                textName.setVisibility(8);
            } else {
                textName.setVisibility(0);
                textName.setText(simItem.mName);
            }
            if (simItem.mIsSim && simItem.mNumber != null && simItem.mNumber.length() != 0) {
                textNum.setVisibility(0);
                textNum.setText(simItem.mNumber);
            } else {
                textNum.setVisibility(8);
            }
        }

        class ViewHolder {
            RadioButton mCkRadioOn;
            RelativeLayout mImageSim;
            ImageView mImageStatus;
            TextView mTextName;
            TextView mTextNum;
            TextView mTextNumFormat;

            ViewHolder() {
            }
        }
    }

    static class SimItem {
        public int mColor;
        public boolean mIsSim;
        public String mName;
        public String mNumber;
        public Bitmap mSimIconBitmap;
        public int mSlot;
        public int mSubId;
        private WifiGprsSelector mWifiGprsSeletor;
        public int mDispalyNumberFormat = 0;
        public int mState = 5;

        public SimItem(SubscriptionInfo subinfo, WifiGprsSelector wifiGprsSelector) {
            this.mIsSim = true;
            this.mName = null;
            this.mNumber = null;
            this.mColor = -1;
            this.mSlot = -1;
            this.mSubId = -1;
            this.mSimIconBitmap = null;
            this.mWifiGprsSeletor = null;
            this.mIsSim = true;
            this.mName = subinfo.getDisplayName().toString();
            this.mNumber = subinfo.getNumber();
            this.mColor = subinfo.getIconTint();
            this.mSlot = subinfo.getSimSlotIndex();
            this.mSubId = subinfo.getSubscriptionId();
            this.mWifiGprsSeletor = wifiGprsSelector;
            this.mSimIconBitmap = subinfo.createIconBitmap(wifiGprsSelector.getPrefContext());
        }
    }
}

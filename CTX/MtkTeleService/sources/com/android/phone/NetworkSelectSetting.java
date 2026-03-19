package com.android.phone;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.telephony.CellInfo;
import android.telephony.NetworkRegistrationState;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.INetworkQueryServiceCallback;
import com.android.phone.NetworkQueryService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NetworkSelectSetting extends PreferenceFragment {
    private static final boolean DBG = true;
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 4;
    private static final int EVENT_NETWORK_SCAN_ERROR = 3;
    private static final int EVENT_NETWORK_SCAN_RESULTS = 2;
    private static final int EVENT_NETWORK_SELECTION_DONE = 1;
    private static final String PREF_KEY_CONNECTED_NETWORK_OPERATOR = "connected_network_operator_preference";
    private static final String PREF_KEY_NETWORK_OPERATORS = "network_operators_preference";
    private static final String TAG = "NetworkSelectSetting";
    private List<CellInfo> mCellInfoList;
    private PreferenceCategory mConnectedNetworkOperatorsPreference;
    private List<String> mForbiddenPlmns;
    private ViewGroup mFrameLayout;
    private NetworkOperators mNetworkOperators;
    private PreferenceCategory mNetworkOperatorsPreferences;
    private View mProgressHeader;
    private NetworkOperatorPreference mSelectedNetworkOperatorPreference;
    boolean mShouldUnbind;
    private Preference mStatusMessagePreference;
    private TelephonyManager mTelephonyManager;
    private int mPhoneId = -1;
    private final Runnable mUpdateNetworkOperatorsRunnable = new Runnable() {
        @Override
        public final void run() {
            this.f$0.updateNetworkOperatorsPreferenceCategory();
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    NetworkSelectSetting.this.logd("network selection done: hide the progress header");
                    NetworkSelectSetting.this.setProgressBarVisible(false);
                    if (((AsyncResult) message.obj).exception != null) {
                        NetworkSelectSetting.this.logd("manual network selection: failed! ");
                        NetworkSelectSetting.this.updateNetworkSelection();
                        NetworkSelectSetting.this.mSelectedNetworkOperatorPreference.setSummary(R.string.network_could_not_connect);
                    } else {
                        NetworkSelectSetting.this.logd("manual network selection: succeeded! ");
                        NetworkSelectSetting.this.mSelectedNetworkOperatorPreference.setSummary(R.string.network_connected);
                    }
                    break;
                case 2:
                    NetworkSelectSetting.this.mCellInfoList = new ArrayList(NetworkSelectSetting.this.aggregateCellInfoList((List) message.obj));
                    NetworkSelectSetting.this.logd("after aggregate: " + NetworkSelectSetting.this.mCellInfoList.toString());
                    if (NetworkSelectSetting.this.mCellInfoList == null || NetworkSelectSetting.this.mCellInfoList.size() == 0) {
                        NetworkSelectSetting.this.addMessagePreference(R.string.empty_networks_list);
                    } else {
                        NetworkSelectSetting.this.updateNetworkOperators();
                    }
                    break;
                case 3:
                    int i = message.arg1;
                    NetworkSelectSetting.this.logd("error while querying available networks " + i);
                    NetworkSelectSetting.this.stopNetworkQuery();
                    NetworkSelectSetting.this.addMessagePreference(R.string.network_query_error);
                    break;
                case 4:
                    NetworkSelectSetting.this.stopNetworkQuery();
                    NetworkSelectSetting.this.logd("scan complete");
                    if (NetworkSelectSetting.this.mCellInfoList == null) {
                        NetworkSelectSetting.this.addMessagePreference(R.string.empty_networks_list);
                    }
                    break;
            }
        }
    };
    private final INetworkQueryServiceCallback mCallback = new INetworkQueryServiceCallback.Stub() {
        @Override
        public void onResults(List<CellInfo> list) {
            NetworkSelectSetting.this.logd("get scan results.");
            NetworkSelectSetting.this.mHandler.obtainMessage(2, list).sendToTarget();
        }

        @Override
        public void onComplete() {
            NetworkSelectSetting.this.logd("network scan completed.");
            NetworkSelectSetting.this.mHandler.obtainMessage(4).sendToTarget();
        }

        @Override
        public void onError(int i) {
            NetworkSelectSetting.this.logd("get onError callback with error code: " + i);
            NetworkSelectSetting.this.mHandler.obtainMessage(3, i, 0).sendToTarget();
        }
    };
    private INetworkQueryService mNetworkQueryService = null;
    private final ServiceConnection mNetworkQueryServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            NetworkSelectSetting.this.logd("connection created, binding local service.");
            NetworkSelectSetting.this.mNetworkQueryService = ((NetworkQueryService.LocalBinder) iBinder).getService();
            NetworkSelectSetting.this.loadNetworksList();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            NetworkSelectSetting.this.logd("connection disconnected, cleaning local binding.");
            NetworkSelectSetting.this.mNetworkQueryService = null;
        }
    };

    public static NetworkSelectSetting newInstance(int i) {
        Bundle bundle = new Bundle();
        bundle.putInt(NetworkSelectSettingActivity.KEY_PHONE_ID, i);
        NetworkSelectSetting networkSelectSetting = new NetworkSelectSetting();
        networkSelectSetting.setArguments(bundle);
        return networkSelectSetting;
    }

    @Override
    public void onCreate(Bundle bundle) {
        logd("onCreate");
        super.onCreate(bundle);
        this.mPhoneId = getArguments().getInt(NetworkSelectSettingActivity.KEY_PHONE_ID);
        addPreferencesFromResource(R.xml.choose_network);
        this.mConnectedNetworkOperatorsPreference = (PreferenceCategory) findPreference(PREF_KEY_CONNECTED_NETWORK_OPERATOR);
        this.mNetworkOperatorsPreferences = (PreferenceCategory) findPreference(PREF_KEY_NETWORK_OPERATORS);
        this.mStatusMessagePreference = new Preference(getContext());
        this.mSelectedNetworkOperatorPreference = null;
        this.mTelephonyManager = (TelephonyManager) getContext().getSystemService("phone");
        this.mNetworkOperators = new NetworkOperators(getContext());
        setRetainInstance(DBG);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        logd("onViewCreated");
        super.onViewCreated(view, bundle);
        if (getListView() != null) {
            getListView().setDivider(null);
        }
        Activity activity = getActivity();
        if (activity != null) {
            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(DBG);
            }
            this.mFrameLayout = (ViewGroup) activity.findViewById(R.id.choose_network_content);
            View viewInflate = activity.getLayoutInflater().inflate(R.layout.choose_network_progress_header, this.mFrameLayout, false);
            this.mFrameLayout.addView(viewInflate);
            this.mFrameLayout.setVisibility(0);
            this.mProgressHeader = viewInflate.findViewById(R.id.progress_bar_animation);
            setProgressBarVisible(false);
        }
        forceConfigConnectedNetworkOperatorsPreferenceCategory();
    }

    @Override
    public void onStart() {
        logd("onStart");
        super.onStart();
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voidArr) {
                return Arrays.asList(NetworkSelectSetting.this.mTelephonyManager.getForbiddenPlmns());
            }

            @Override
            protected void onPostExecute(List<String> list) {
                NetworkSelectSetting.this.mForbiddenPlmns = list;
                NetworkSelectSetting.this.bindNetworkQueryService();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        logd("User clicked the screen");
        stopNetworkQuery();
        setProgressBarVisible(false);
        if (preference instanceof NetworkOperatorPreference) {
            if (this.mSelectedNetworkOperatorPreference != null) {
                this.mSelectedNetworkOperatorPreference.setSummary("");
            }
            this.mSelectedNetworkOperatorPreference = (NetworkOperatorPreference) preference;
            CellInfo cellInfo = this.mSelectedNetworkOperatorPreference.getCellInfo();
            logd("User click a NetworkOperatorPreference: " + cellInfo.toString());
            MetricsLogger.action(getContext(), 1210);
            Message messageObtainMessage = this.mHandler.obtainMessage(1);
            Phone phone = PhoneFactory.getPhone(this.mPhoneId);
            if (phone != null) {
                logd("Connect to the network: " + CellInfoUtil.getNetworkTitle(cellInfo));
                this.mSelectedNetworkOperatorPreference.setSummary(R.string.network_connecting);
                if (this.mConnectedNetworkOperatorsPreference.getPreferenceCount() > 0) {
                    NetworkOperatorPreference networkOperatorPreference = (NetworkOperatorPreference) this.mConnectedNetworkOperatorsPreference.getPreference(0);
                    if (!CellInfoUtil.getNetworkTitle(cellInfo).equals(CellInfoUtil.getNetworkTitle(networkOperatorPreference.getCellInfo()))) {
                        networkOperatorPreference.setSummary(R.string.network_disconnected);
                    }
                }
                OperatorInfo operatorInfoFromCellInfo = CellInfoUtil.getOperatorInfoFromCellInfo(cellInfo);
                logd("manually selected network operator: " + operatorInfoFromCellInfo.toString());
                phone.selectNetworkManually(operatorInfoFromCellInfo, DBG, messageObtainMessage);
                setProgressBarVisible(DBG);
                return DBG;
            }
            loge("Error selecting network. phone is null.");
            this.mSelectedNetworkOperatorPreference = null;
            return false;
        }
        preferenceScreen.setEnabled(false);
        return false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(getActivity() instanceof NetworkSelectSettingActivity)) {
            throw new IllegalStateException("Parent activity is not NetworkSelectSettingActivity");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        logd("onStop");
        getView().removeCallbacks(this.mUpdateNetworkOperatorsRunnable);
        stopNetworkQuery();
        unbindNetworkQueryService();
    }

    private void loadNetworksList() {
        logd("load networks list...");
        setProgressBarVisible(DBG);
        try {
            if (this.mNetworkQueryService != null) {
                logd("start network query");
                this.mNetworkQueryService.startNetworkQuery(this.mCallback, this.mPhoneId, DBG);
            } else {
                logd("unable to start network query, mNetworkQueryService is null");
                addMessagePreference(R.string.network_query_error);
            }
        } catch (RemoteException e) {
            loge("loadNetworksList: exception from startNetworkQuery " + e);
            addMessagePreference(R.string.network_query_error);
        }
    }

    private void updateNetworkOperators() {
        logd("updateNetworkOperators");
        if (getActivity() != null) {
            View view = getView();
            Handler handler = view.getHandler();
            if (handler != null && handler.hasCallbacks(this.mUpdateNetworkOperatorsRunnable)) {
                return;
            }
            view.post(this.mUpdateNetworkOperatorsRunnable);
        }
    }

    private void updateNetworkOperatorsPreferenceCategory() {
        this.mNetworkOperatorsPreferences.removeAll();
        configConnectedNetworkOperatorsPreferenceCategory();
        for (int i = 0; i < this.mCellInfoList.size(); i++) {
            if (!this.mCellInfoList.get(i).isRegistered()) {
                NetworkOperatorPreference networkOperatorPreference = new NetworkOperatorPreference(this.mCellInfoList.get(i), getContext(), this.mForbiddenPlmns);
                networkOperatorPreference.setKey(CellInfoUtil.getNetworkTitle(this.mCellInfoList.get(i)));
                networkOperatorPreference.setOrder(i);
                this.mNetworkOperatorsPreferences.addPreference(networkOperatorPreference);
            }
        }
    }

    private void forceConfigConnectedNetworkOperatorsPreferenceCategory() {
        logd("Force config ConnectedNetworkOperatorsPreferenceCategory");
        int dataState = this.mTelephonyManager.getDataState();
        TelephonyManager telephonyManager = this.mTelephonyManager;
        if (dataState == 2) {
            List networkRegistrationStates = this.mTelephonyManager.getServiceStateForSubscriber(this.mPhoneId).getNetworkRegistrationStates(1);
            if (networkRegistrationStates == null || networkRegistrationStates.size() == 0) {
                loge("getNetworkRegistrationStates return null");
                removeConnectedNetworkOperatorPreference();
                return;
            }
            CellInfo cellInfoWrapCellInfoWithCellIdentity = CellInfoUtil.wrapCellInfoWithCellIdentity(((NetworkRegistrationState) networkRegistrationStates.get(0)).getCellIdentity());
            if (cellInfoWrapCellInfoWithCellIdentity != null) {
                logd("Currently registered cell: " + cellInfoWrapCellInfoWithCellIdentity.toString());
                NetworkOperatorPreference networkOperatorPreference = new NetworkOperatorPreference(cellInfoWrapCellInfoWithCellIdentity, getContext(), this.mForbiddenPlmns);
                networkOperatorPreference.setTitle(this.mTelephonyManager.getNetworkOperatorName());
                networkOperatorPreference.setSummary(R.string.network_connected);
                networkOperatorPreference.setIcon(4);
                this.mConnectedNetworkOperatorsPreference.addPreference(networkOperatorPreference);
                return;
            }
            loge("Invalid CellIfno: " + cellInfoWrapCellInfoWithCellIdentity.toString());
            removeConnectedNetworkOperatorPreference();
            return;
        }
        logd("No currently registered cell");
        removeConnectedNetworkOperatorPreference();
    }

    private void configConnectedNetworkOperatorsPreferenceCategory() {
        logd("config ConnectedNetworkOperatorsPreferenceCategory");
        if (this.mCellInfoList.size() == 0) {
            logd("empty cellinfo list");
            removeConnectedNetworkOperatorPreference();
        }
        CellInfo cellInfo = null;
        Iterator<CellInfo> it = this.mCellInfoList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            CellInfo next = it.next();
            if (next.isRegistered()) {
                cellInfo = next;
                break;
            }
        }
        if (cellInfo == null) {
            logd("no registered network");
            removeConnectedNetworkOperatorPreference();
            return;
        }
        if (this.mConnectedNetworkOperatorsPreference.getPreferenceCount() == 0) {
            logd("ConnectedNetworkSelectList is empty, add one");
            addConnectedNetworkOperatorPreference(cellInfo);
            return;
        }
        if (!CellInfoUtil.getNetworkTitle(cellInfo).equals(CellInfoUtil.getNetworkTitle(((NetworkOperatorPreference) this.mConnectedNetworkOperatorsPreference.getPreference(0)).getCellInfo()))) {
            logd("reconfig the category: connected network changed");
            addConnectedNetworkOperatorPreference(cellInfo);
        } else {
            logd("same network operator is connected, only refresh the connected network");
            ((NetworkOperatorPreference) this.mConnectedNetworkOperatorsPreference.getPreference(0)).refresh();
        }
    }

    private void addConnectedNetworkOperatorPreference(CellInfo cellInfo) {
        logd("addConnectedNetworkOperatorPreference");
        removeConnectedNetworkOperatorPreference();
        NetworkOperatorPreference networkOperatorPreference = new NetworkOperatorPreference(cellInfo, getContext(), this.mForbiddenPlmns);
        networkOperatorPreference.setSummary(R.string.network_connected);
        this.mConnectedNetworkOperatorsPreference.addPreference(networkOperatorPreference);
        getPreferenceScreen().addPreference(this.mConnectedNetworkOperatorsPreference);
    }

    private void removeConnectedNetworkOperatorPreference() {
        this.mConnectedNetworkOperatorsPreference.removeAll();
        getPreferenceScreen().removePreference(this.mConnectedNetworkOperatorsPreference);
    }

    protected void setProgressBarVisible(boolean z) {
        if (this.mProgressHeader != null) {
            this.mProgressHeader.setVisibility(z ? 0 : 8);
        }
    }

    private void addMessagePreference(int i) {
        logd("remove callback");
        getView().removeCallbacks(this.mUpdateNetworkOperatorsRunnable);
        setProgressBarVisible(false);
        logd("addMessagePreference");
        this.mStatusMessagePreference.setTitle(i);
        removeConnectedNetworkOperatorPreference();
        this.mNetworkOperatorsPreferences.removeAll();
        this.mNetworkOperatorsPreferences.addPreference(this.mStatusMessagePreference);
    }

    private List<CellInfo> aggregateCellInfoList(List<CellInfo> list) {
        logd("before aggregate: " + list.toString());
        HashMap map = new HashMap();
        for (CellInfo cellInfo : list) {
            String networkTitle = CellInfoUtil.getNetworkTitle(cellInfo);
            if (cellInfo.isRegistered() || !map.containsKey(networkTitle)) {
                map.put(networkTitle, cellInfo);
            } else if (!((CellInfo) map.get(networkTitle)).isRegistered() && CellInfoUtil.getLevel((CellInfo) map.get(networkTitle)) <= CellInfoUtil.getLevel(cellInfo)) {
                map.put(networkTitle, cellInfo);
            }
        }
        return new ArrayList(map.values());
    }

    private void bindNetworkQueryService() {
        logd("bindNetworkQueryService");
        getContext().bindService(new Intent(getContext(), (Class<?>) NetworkQueryService.class).setAction("com.android.phone.intent.action.LOCAL_BINDER"), this.mNetworkQueryServiceConnection, 1);
        this.mShouldUnbind = DBG;
    }

    private void unbindNetworkQueryService() {
        logd("unbindNetworkQueryService");
        if (this.mShouldUnbind) {
            logd("mShouldUnbind is true");
            getContext().unbindService(this.mNetworkQueryServiceConnection);
            this.mShouldUnbind = false;
        }
    }

    private void updateNetworkSelection() {
        ServiceState serviceStateForSubscriber;
        logd("Update notification about no service of user selected operator");
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        Phone phone = PhoneFactory.getPhone(this.mPhoneId);
        if (phone != null && (serviceStateForSubscriber = this.mTelephonyManager.getServiceStateForSubscriber(phone.getSubId())) != null) {
            phoneGlobals.notificationMgr.updateNetworkSelection(serviceStateForSubscriber.getState(), phone.getSubId());
        }
    }

    private void stopNetworkQuery() {
        try {
            if (this.mNetworkQueryService != null) {
                logd("Stop network query");
                this.mNetworkQueryService.stopNetworkQuery();
                this.mNetworkQueryService.unregisterCallback(this.mCallback);
            }
        } catch (RemoteException e) {
            loge("Exception from stopNetworkQuery " + e);
        }
    }

    private void logd(String str) {
        Log.d(TAG, str);
    }

    private void loge(String str) {
        Log.e(TAG, str);
    }
}

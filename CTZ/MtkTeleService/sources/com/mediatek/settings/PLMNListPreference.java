package com.mediatek.settings;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.phone.TimeConsumingPreferenceActivity;
import com.mediatek.internal.telephony.NetworkInfoWithAcT;
import com.mediatek.phone.PhoneFeatureConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class PLMNListPreference extends TimeConsumingPreferenceActivity implements PhoneGlobals.SubInfoUpdateListener {
    private IntentFilter mIntentFilter;
    private NetworkInfoWithAcT mOldInfo;
    private PreferenceScreen mPLMNListContainer;
    private PhoneStateListener mPhoneStateListener;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private TelephonyManager mTelephonyManager;
    private ArrayList<NetworkInfoWithAcT> mPLMNList = new ArrayList<>();
    private int mNumbers = 0;
    private int mSubId = -1;
    private Phone mPhone = null;
    private SIMCapability mCapability = new SIMCapability(0, 0, 0, 0);
    private MyHandler mHandler = new MyHandler();
    ArrayList<String> mListPriority = new ArrayList<>();
    ArrayList<String> mListService = new ArrayList<>();
    private boolean mAirplaneModeEnabled = false;
    private boolean mListItemClicked = false;
    private boolean mFirstResume = false;
    private int mState = -1;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.AIRPLANE_MODE")) {
                PLMNListPreference.this.finish();
            }
        }
    };

    static int access$410(PLMNListPreference pLMNListPreference) {
        int i = pLMNListPreference.mNumbers;
        pLMNListPreference.mNumbers = i - 1;
        return i;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.mtk_plmn_list);
        this.mPLMNListContainer = (PreferenceScreen) findPreference("button_plmn_list_key");
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        this.mSubId = this.mPhone != null ? this.mPhone.getSubId() : -1;
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.plmn_list_setting_title);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        registerReceiver(this.mReceiver, this.mIntentFilter);
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        if (!PhoneUtils.isValidSubId(this.mSubId)) {
            Log.i("Settings/PLMNListPreference", "mSubId is invalid,activity finish!!!");
            finish();
        } else {
            this.mFirstResume = true;
            registerCallBacks();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mIsForeground = true;
        this.mListItemClicked = false;
        setScreenEnabled();
        if (this.mFirstResume) {
            this.mFirstResume = false;
            initUi();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt(SubscriptionInfoHelper.SUB_ID_EXTRA, this.mSubId);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.d("Settings/PLMNListPreference", "onPreferenceTreeClick()... preference: " + preference + ", mListItemClicked: " + this.mListItemClicked);
        if (this.mListItemClicked) {
            return true;
        }
        this.mListItemClicked = true;
        setScreenEnabled();
        Intent intent = new Intent(this, (Class<?>) NetworkEditor.class);
        Log.d("Settings/PLMNListPreference", "get order: " + preference.getOrder());
        try {
            NetworkInfoWithAcT networkInfoWithAcT = this.mPLMNList.get(preference.getOrder());
            if (networkInfoWithAcT == null) {
                return false;
            }
            this.mOldInfo = networkInfoWithAcT;
            extractInfoFromNetworkInfo(intent, networkInfoWithAcT);
            startActivityForResult(intent, 200);
            return true;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        unRegisterCallBacks();
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        super.onDestroy();
    }

    private void initUi() {
        getSIMCapability();
        initPlmnList(this, false);
        this.mAirplaneModeEnabled = Settings.System.getInt(getContentResolver(), "airplane_mode_on", -1) == 1;
        Log.d("Settings/PLMNListPreference", "onResume()... mListItemClicked: " + this.mListItemClicked);
        this.mListItemClicked = false;
        setScreenEnabled();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 0, R.string.plmn_list_setting_add_plmn).setShowAsAction(1);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean z;
        Log.d("Settings/PLMNListPreference", "onPrepareOptionsMenu: mSubId " + this.mSubId);
        if (PhoneUtils.isValidSubId(this.mSubId)) {
            if ((TelephonyManager.getDefault().getCallState(this.mSubId) == 0) && TelephonyUtils.isRadioOn(this.mSubId, this)) {
                z = true;
            }
        } else {
            z = false;
        }
        if (menu != null) {
            menu.setGroupEnabled(0, z && !this.mAirplaneModeEnabled);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 1) {
            Intent intent = new Intent(this, (Class<?>) NetworkEditor.class);
            intent.putExtra("plmn_name", "");
            intent.putExtra("plmn_code", "");
            intent.putExtra("plmn_priority", 0);
            intent.putExtra("plmn_service", 0);
            intent.putExtra("plmn_add", true);
            intent.putExtra("plmn_sub", this.mSubId);
            startActivityForResult(intent, 100);
        } else if (itemId == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void initPlmnList(TimeConsumingPreferenceActivity timeConsumingPreferenceActivity, boolean z) {
        Log.d("Settings/PLMNListPreference", "init with skipReading = " + z);
        if (!z) {
            PhoneFactory.getPhone(this.mPhone.getPhoneId()).getPol(this.mHandler.obtainMessage(0, 0, 0));
            if (timeConsumingPreferenceActivity != null) {
                setDialogTitle(getString(R.string.plmn_list_setting_title));
                timeConsumingPreferenceActivity.onStarted(this.mPLMNListContainer, true);
            }
        }
    }

    @Override
    public void onFinished(Preference preference, boolean z) {
        super.onFinished(preference, z);
        setScreenEnabled();
    }

    private void getSIMCapability() {
        PhoneFactory.getPhone(this.mPhone.getPhoneId()).getPolCapability(this.mHandler.obtainMessage(2, 0, 2));
    }

    private void refreshPreference(ArrayList<NetworkInfoWithAcT> arrayList) {
        if (this.mPLMNListContainer.getPreferenceCount() != 0) {
            this.mPLMNListContainer.removeAll();
        }
        if (this.mPLMNList != null) {
            this.mPLMNList.clear();
        }
        if (arrayList == null || arrayList.size() == 0) {
            Log.d("Settings/PLMNListPreference", "refreshPreference : NULL PLMN list!");
            return;
        }
        Collections.sort(arrayList, new NetworkCompare());
        int i = 0;
        Iterator<NetworkInfoWithAcT> it = arrayList.iterator();
        while (it.hasNext()) {
            if (it.next().getOperatorNumeric().equals("46020")) {
                it.remove();
            }
        }
        for (NetworkInfoWithAcT networkInfoWithAcT : arrayList) {
            if (addPLMNPreference(networkInfoWithAcT, i)) {
                i++;
            }
            Log.d("Settings/PLMNListPreference", "Plmnlist: " + networkInfoWithAcT);
        }
        if (this.mPLMNList != null) {
            adjustPriority(this.mPLMNList);
        }
    }

    class NetworkCompare implements Comparator<NetworkInfoWithAcT> {
        NetworkCompare() {
        }

        @Override
        public int compare(NetworkInfoWithAcT networkInfoWithAcT, NetworkInfoWithAcT networkInfoWithAcT2) {
            return networkInfoWithAcT.getPriority() - networkInfoWithAcT2.getPriority();
        }
    }

    private boolean addPLMNPreference(NetworkInfoWithAcT networkInfoWithAcT, int i) {
        int accessTechnology = networkInfoWithAcT.getAccessTechnology();
        Log.d("Settings/PLMNListPreference", "act: " + accessTechnology);
        if (!PhoneFeatureConstants.FeatureOption.isMtkLteSupport() && (accessTechnology & 8) != 0) {
            return false;
        }
        String operatorAlphaName = networkInfoWithAcT.getOperatorAlphaName();
        String nWString = getNWString(accessTechnology);
        Preference preference = new Preference(this);
        preference.setTitle(operatorAlphaName + "(" + nWString + ")");
        preference.setOrder(i);
        this.mPLMNListContainer.addPreference(preference);
        this.mPLMNList.add(networkInfoWithAcT);
        return true;
    }

    private void extractInfoFromNetworkInfo(Intent intent, NetworkInfoWithAcT networkInfoWithAcT) {
        intent.putExtra("plmn_code", networkInfoWithAcT.getOperatorNumeric());
        intent.putExtra("plmn_name", networkInfoWithAcT.getOperatorAlphaName());
        intent.putExtra("plmn_priority", networkInfoWithAcT.getPriority());
        intent.putExtra("plmn_service", networkInfoWithAcT.getAccessTechnology());
        intent.putExtra("plmn_add", false);
        intent.putExtra("plmn_sub", this.mSubId);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (intent != null && TelephonyUtils.isSimStateReady(this.mPhone.getPhoneId()) && this.mPLMNList != null) {
            NetworkInfoWithAcT networkInfoWithAcTCreateNetworkInfo = createNetworkInfo(intent);
            if (i2 == 200) {
                handlePLMNListDelete(this.mOldInfo);
                return;
            }
            if (i2 == 100) {
                if (i == 100) {
                    handlePLMNListAdd(networkInfoWithAcTCreateNetworkInfo);
                } else if (i == 200) {
                    handlePLMNListEdit(networkInfoWithAcTCreateNetworkInfo);
                }
            }
        }
    }

    private NetworkInfoWithAcT createNetworkInfo(Intent intent) {
        String stringExtra = intent.getStringExtra("plmn_code");
        return new NetworkInfoWithAcT(intent.getStringExtra("plmn_name"), stringExtra, intent.getIntExtra("plmn_service", 0), intent.getIntExtra("plmn_priority", 0));
    }

    private void handleSetPLMN(ArrayList<NetworkInfoWithAcT> arrayList) {
        this.mNumbers = arrayList.size();
        this.mIsForeground = true;
        onStarted(this.mPLMNListContainer, false);
        for (int i = 0; i < arrayList.size(); i++) {
            NetworkInfoWithAcT networkInfoWithAcT = arrayList.get(i);
            Log.d("Settings/PLMNListPreference", "handleSetPLMN: set network: " + networkInfoWithAcT);
            PhoneFactory.getPhone(this.mPhone.getPhoneId()).setPolEntry(networkInfoWithAcT, this.mHandler.obtainMessage(1, 0, 1));
        }
    }

    private void handlePLMNListAdd(NetworkInfoWithAcT networkInfoWithAcT) {
        Log.d("Settings/PLMNListPreference", "handlePLMNListAdd: add new network: " + networkInfoWithAcT);
        dumpNetworkInfo(this.mPLMNList);
        this.mPLMNList.add(0, networkInfoWithAcT);
        adjustPriority(this.mPLMNList);
        dumpNetworkInfo(this.mPLMNList);
        handleSetPLMN(this.mPLMNList);
    }

    private void dumpNetworkInfo(List<NetworkInfoWithAcT> list) {
        if (list == null) {
            Log.d("Settings/PLMNListPreference", "dumpNetworkInfo : list is null");
            return;
        }
        Log.d("Settings/PLMNListPreference", "dumpNetworkInfo : **********start*******");
        for (int i = 0; i < list.size(); i++) {
            Log.d("Settings/PLMNListPreference", "dumpNetworkInfo : " + list.get(i));
        }
        Log.d("Settings/PLMNListPreference", "dumpNetworkInfo : ***********stop*******");
    }

    private void handlePLMNListEdit(NetworkInfoWithAcT networkInfoWithAcT) {
        Log.d("Settings/PLMNListPreference", "handlePLMNListEdit: change : " + networkInfoWithAcT);
        dumpNetworkInfo(this.mPLMNList);
        NetworkInfoWithAcT networkInfoWithAcT2 = this.mPLMNList.get(networkInfoWithAcT.getPriority());
        networkInfoWithAcT2.setOperatorAlphaName(networkInfoWithAcT.getOperatorAlphaName());
        networkInfoWithAcT2.setOperatorNumeric(networkInfoWithAcT.getOperatorNumeric());
        networkInfoWithAcT2.setAccessTechnology(networkInfoWithAcT.getAccessTechnology());
        dumpNetworkInfo(this.mPLMNList);
        handleSetPLMN(this.mPLMNList);
    }

    private void adjustPriority(ArrayList<NetworkInfoWithAcT> arrayList) {
        Iterator<NetworkInfoWithAcT> it = arrayList.iterator();
        int i = 0;
        while (it.hasNext()) {
            it.next().setPriority(i);
            i++;
        }
    }

    private void handlePLMNListDelete(NetworkInfoWithAcT networkInfoWithAcT) {
        Log.d("Settings/PLMNListPreference", "handlePLMNListDelete : " + networkInfoWithAcT);
        dumpNetworkInfo(this.mPLMNList);
        int size = this.mPLMNList.size();
        this.mPLMNList.remove(networkInfoWithAcT.getPriority());
        ArrayList<NetworkInfoWithAcT> arrayList = new ArrayList<>();
        for (int i = 0; i < this.mPLMNList.size(); i++) {
            arrayList.add(this.mPLMNList.get(i));
        }
        for (int size2 = arrayList.size(); size2 < size; size2++) {
            arrayList.add(new NetworkInfoWithAcT("", "", 1, size2));
        }
        adjustPriority(arrayList);
        dumpNetworkInfo(arrayList);
        handleSetPLMN(arrayList);
    }

    private class MyHandler extends Handler {
        private MyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleGetPLMNResponse(message);
                    break;
                case 1:
                    handleSetPLMNResponse(message);
                    break;
                case 2:
                    handleGetPLMNCapibilityResponse(message);
                    break;
            }
        }

        public void handleGetPLMNResponse(Message message) {
            Log.d("Settings/PLMNListPreference", "handleGetPLMNResponse: done");
            if (message.arg2 == 0) {
                PLMNListPreference.this.onFinished(PLMNListPreference.this.mPLMNListContainer, true);
            } else {
                PLMNListPreference.this.onFinished(PLMNListPreference.this.mPLMNListContainer, false);
            }
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception == null) {
                PLMNListPreference.this.refreshPreference((ArrayList) asyncResult.result);
                return;
            }
            Log.d("Settings/PLMNListPreference", "handleGetPLMNResponse with exception = " + asyncResult.exception);
            if (PLMNListPreference.this.mPLMNList == null) {
                PLMNListPreference.this.mPLMNList = new ArrayList();
            }
        }

        public void handleSetPLMNResponse(Message message) {
            Log.d("Settings/PLMNListPreference", "handleSetPLMNResponse: done");
            PLMNListPreference.access$410(PLMNListPreference.this);
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                Log.d("Settings/PLMNListPreference", "handleSetPLMNResponse with exception = " + asyncResult.exception);
            } else {
                Log.d("Settings/PLMNListPreference", "handleSetPLMNResponse: with OK result!");
            }
            if (PLMNListPreference.this.mNumbers == 0) {
                Log.d("Settings/PLMNListPreference", "handleSetPLMNResponse: MESSAGE_GET_PLMN_LIST");
                PhoneFactory.getPhone(PLMNListPreference.this.mPhone.getPhoneId()).getPol(PLMNListPreference.this.mHandler.obtainMessage(0, 0, 1));
            }
        }

        public void handleGetPLMNCapibilityResponse(Message message) {
            Log.d("Settings/PLMNListPreference", "handleGetPLMNCapibilityResponse: done");
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception == null) {
                PLMNListPreference.this.mCapability.setCapability((int[]) asyncResult.result);
                return;
            }
            Log.d("Settings/PLMNListPreference", "handleGetPLMNCapibilityResponse with exception = " + asyncResult.exception);
        }
    }

    private class SIMCapability {
        int mFirstFormat;
        int mFirstIndex;
        int mLastFormat;
        int mLastIndex;

        public SIMCapability(int i, int i2, int i3, int i4) {
            this.mFirstIndex = i;
            this.mLastIndex = i2;
            this.mFirstFormat = i3;
            this.mLastFormat = i4;
        }

        public void setCapability(int[] iArr) {
            if (iArr.length < 4) {
                return;
            }
            this.mFirstIndex = iArr[0];
            this.mLastIndex = iArr[1];
            this.mFirstFormat = iArr[2];
            this.mLastFormat = iArr[3];
            Log.d("Settings/PLMNListPreference", "SIM PLMN List capability length: " + this.mLastIndex);
        }
    }

    private String getNWString(int i) {
        return getResources().getStringArray(R.array.plmn_prefer_network_type_choices)[NetworkEditor.covertRilNW2Ap(this, i, this.mSubId)];
    }

    private void setScreenEnabled() {
        boolean z = false;
        boolean z2 = (TelephonyManager.getDefault().getCallState(this.mSubId) == 0) && !this.mAirplaneModeEnabled && TelephonyUtils.isRadioOn(this.mSubId, this);
        getPreferenceScreen().setEnabled(z2);
        Log.d("Settings/PLMNListPreference", "setScreenEnabled()... + mListItemClicked: " + this.mListItemClicked);
        PreferenceScreen preferenceScreen = this.mPLMNListContainer;
        if (!this.mListItemClicked && z2) {
            z = true;
        }
        preferenceScreen.setEnabled(z);
        invalidateOptionsMenu();
    }

    private void registerCallBacks() {
        this.mPhoneStateListener = new PhoneStateListener(Integer.valueOf(this.mSubId)) {
            @Override
            public void onCallStateChanged(int i, String str) {
                super.onCallStateChanged(i, str);
                Log.d("Settings/PLMNListPreference", "onCallStateChanged ans state is " + i);
                PLMNListPreference.this.setScreenEnabled();
            }

            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                if (PLMNListPreference.this.mState != serviceState.getState()) {
                    Log.d("Settings/PLMNListPreference", "onServiceStateChanged state is " + serviceState.getState());
                    PLMNListPreference.this.mState = serviceState.getState();
                    PLMNListPreference.this.setScreenEnabled();
                }
            }
        };
        this.mTelephonyManager.listen(this.mPhoneStateListener, 33);
    }

    private void unRegisterCallBacks() {
        if (PhoneUtils.isValidSubId(this.mSubId)) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
        unregisterReceiver(this.mReceiver);
    }

    @Override
    public void handleSubInfoUpdate() {
        Log.d("Settings/PLMNListPreference", "handleSubInfoUpdate...");
        finish();
    }
}

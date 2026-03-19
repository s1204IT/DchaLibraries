package com.android.phone;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;
import com.android.ims.ImsManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.phone.INetworkQueryServiceCallback;
import com.android.phone.NetworkSelectListPreference;
import com.android.phone.PhoneGlobals;
import com.mediatek.phone.ext.ExtensionManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class NetworkSelectListPreference extends ListPreference implements DialogInterface.OnCancelListener, Preference.OnPreferenceChangeListener, PhoneGlobals.SubInfoUpdateListener {
    private static final boolean DBG = SystemProperties.get("ro.build.type").equals("eng");
    private static final int DIALOG_ALERT_PLMN_SEARCH = 1000;
    private static final int DIALOG_ALL_FORBIDDEN = 400;
    private static final int DIALOG_NETWORK_LIST_LOAD = 200;
    private static final int DIALOG_NETWORK_SELECTION = 100;
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 4;
    private static final int EVENT_NETWORK_SCAN_ERROR = 3;
    private static final int EVENT_NETWORK_SCAN_RESULTS = 2;
    private static final int EVENT_NETWORK_SELECTION_DONE = 1;
    private static final String LOG_TAG = "networkSelect";
    private final INetworkQueryServiceCallback mCallback;
    private CellInfo mCellInfo;
    private List<CellInfo> mCellInfoList;
    private List<String> mForbiddenPlmns;
    private final Handler mHandler;
    private boolean mNeedScanAgain;
    private NetworkOperators mNetworkOperators;
    INetworkQueryService mNetworkQueryService;
    private int mPhoneId;
    private PhoneStateListener mPhoneStateListener;
    private AlertDialog mPlmnAlertDialog;
    private ProgressDialog mProgressDialog;
    private boolean mShowAlert;
    private int mSubId;
    private TelephonyManager mTelephonyManager;

    public NetworkSelectListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPhoneId = -1;
        this.mHandler = new AnonymousClass2();
        this.mNetworkQueryService = null;
        this.mCallback = new INetworkQueryServiceCallback.Stub() {
            @Override
            public void onResults(List<CellInfo> list) {
                if (NetworkSelectListPreference.DBG) {
                    NetworkSelectListPreference.this.logd("get scan results: " + list.toString());
                }
                NetworkSelectListPreference.this.mHandler.obtainMessage(2, list).sendToTarget();
            }

            @Override
            public void onComplete() {
                if (NetworkSelectListPreference.DBG) {
                    NetworkSelectListPreference.this.logd("network scan completed.");
                }
                NetworkSelectListPreference.this.mHandler.obtainMessage(4).sendToTarget();
            }

            @Override
            public void onError(int i) {
                if (NetworkSelectListPreference.DBG) {
                    NetworkSelectListPreference.this.logd("get onError callback with error code: " + i);
                }
                NetworkSelectListPreference.this.mHandler.obtainMessage(3, i, 0).sendToTarget();
            }
        };
        this.mPlmnAlertDialog = null;
        this.mShowAlert = false;
    }

    public NetworkSelectListPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mPhoneId = -1;
        this.mHandler = new AnonymousClass2();
        this.mNetworkQueryService = null;
        this.mCallback = new INetworkQueryServiceCallback.Stub() {
            @Override
            public void onResults(List<CellInfo> list) {
                if (NetworkSelectListPreference.DBG) {
                    NetworkSelectListPreference.this.logd("get scan results: " + list.toString());
                }
                NetworkSelectListPreference.this.mHandler.obtainMessage(2, list).sendToTarget();
            }

            @Override
            public void onComplete() {
                if (NetworkSelectListPreference.DBG) {
                    NetworkSelectListPreference.this.logd("network scan completed.");
                }
                NetworkSelectListPreference.this.mHandler.obtainMessage(4).sendToTarget();
            }

            @Override
            public void onError(int i3) {
                if (NetworkSelectListPreference.DBG) {
                    NetworkSelectListPreference.this.logd("get onError callback with error code: " + i3);
                }
                NetworkSelectListPreference.this.mHandler.obtainMessage(3, i3, 0).sendToTarget();
            }
        };
        this.mPlmnAlertDialog = null;
        this.mShowAlert = false;
    }

    @Override
    protected void onClick() {
        final TelephonyManager telephonyManager = new TelephonyManager(getContext(), this.mSubId);
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voidArr) {
                String[] forbiddenPlmns = telephonyManager.getForbiddenPlmns();
                if (forbiddenPlmns != null) {
                    return Arrays.asList(forbiddenPlmns);
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<String> list) {
                NetworkSelectListPreference.this.mForbiddenPlmns = list;
                NetworkSelectListPreference.this.logd("mForbiddenPlmns = " + NetworkSelectListPreference.this.mForbiddenPlmns);
                NetworkSelectListPreference.this.loadNetworksList(true);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    class AnonymousClass2 extends Handler {
        AnonymousClass2() {
        }

        @Override
        public void handleMessage(Message message) {
            boolean z = false;
            switch (message.what) {
                case 1:
                    if (NetworkSelectListPreference.DBG) {
                        NetworkSelectListPreference.this.logd("hideProgressPanel");
                    }
                    try {
                        NetworkSelectListPreference.this.dismissProgressBar();
                        break;
                    } catch (IllegalArgumentException e) {
                    }
                    NetworkSelectListPreference.this.setEnabled(true);
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null) {
                        if (NetworkSelectListPreference.DBG) {
                            NetworkSelectListPreference.this.logd("manual network selection: failed!");
                        }
                        NetworkSelectListPreference.this.mNetworkOperators.displayNetworkSelectionFailed(asyncResult.exception);
                    } else {
                        if (NetworkSelectListPreference.DBG) {
                            NetworkSelectListPreference.this.logd("manual network selection: succeeded! " + NetworkSelectListPreference.this.getNetworkTitle(NetworkSelectListPreference.this.mCellInfo));
                        }
                        NetworkSelectListPreference.this.mNetworkOperators.displayNetworkSelectionSucceeded(message.arg1);
                    }
                    NetworkSelectListPreference.this.mNetworkOperators.getNetworkSelectionMode();
                    break;
                case 2:
                    List list = (List) message.obj;
                    list.removeIf(new Predicate() {
                        @Override
                        public final boolean test(Object obj) {
                            return NetworkSelectListPreference.AnonymousClass2.lambda$handleMessage$0((CellInfo) obj);
                        }
                    });
                    if (list.size() > 0) {
                        Iterator it = list.iterator();
                        while (true) {
                            if (it.hasNext()) {
                                if (!NetworkSelectListPreference.this.isInvalidCellInfo((CellInfo) it.next())) {
                                }
                            } else {
                                z = true;
                            }
                        }
                        if (z) {
                            NetworkSelectListPreference.this.mNeedScanAgain = true;
                            if (NetworkSelectListPreference.DBG) {
                                NetworkSelectListPreference.this.logd("Invalid cell info. Stop current network scan and start a new one via old API");
                            }
                            try {
                                if (NetworkSelectListPreference.this.mNetworkQueryService != null) {
                                    NetworkSelectListPreference.this.mNetworkQueryService.stopNetworkQuery();
                                }
                            } catch (RemoteException e2) {
                                NetworkSelectListPreference.this.loge("exception from stopNetworkQuery " + e2);
                                return;
                            }
                        } else {
                            NetworkSelectListPreference.this.mCellInfoList = new ArrayList(list);
                            if (NetworkSelectListPreference.DBG) {
                                NetworkSelectListPreference.this.logd("CALLBACK_SCAN_RESULTS" + NetworkSelectListPreference.this.mCellInfoList.toString());
                            }
                        }
                    }
                    break;
                case 3:
                    int i = message.arg1;
                    if (NetworkSelectListPreference.DBG) {
                        NetworkSelectListPreference.this.logd("error while querying available networks " + i);
                    }
                    if (i == 4) {
                        if (NetworkSelectListPreference.DBG) {
                            NetworkSelectListPreference.this.logd("Modem does not support: try to scan network again via Phone");
                        }
                        boolean z2 = NetworkSelectListPreference.this.mNeedScanAgain;
                        NetworkSelectListPreference.this.mNeedScanAgain = true;
                        NetworkSelectListPreference.this.loadNetworksList(false);
                        NetworkSelectListPreference.this.mNeedScanAgain = z2;
                    } else {
                        try {
                            if (NetworkSelectListPreference.this.mNetworkQueryService != null) {
                                NetworkSelectListPreference.this.mNetworkQueryService.unregisterCallback(NetworkSelectListPreference.this.mCallback);
                            }
                        } catch (RemoteException e3) {
                            NetworkSelectListPreference.this.loge("onError: exception from unregisterCallback " + e3);
                        }
                        NetworkSelectListPreference.this.displayNetworkQueryFailed(i);
                    }
                    break;
                case 4:
                    if (NetworkSelectListPreference.this.mNeedScanAgain) {
                        NetworkSelectListPreference.this.logd("CellInfo is invalid to display. Start a new scan via Phone. ");
                        NetworkSelectListPreference.this.loadNetworksList(false);
                        NetworkSelectListPreference.this.mNeedScanAgain = false;
                    } else {
                        try {
                            if (NetworkSelectListPreference.this.mNetworkQueryService != null) {
                                NetworkSelectListPreference.this.mNetworkQueryService.unregisterCallback(NetworkSelectListPreference.this.mCallback);
                            }
                        } catch (RemoteException e4) {
                            NetworkSelectListPreference.this.loge("onComplete: exception from unregisterCallback " + e4);
                        }
                        if (NetworkSelectListPreference.DBG) {
                            NetworkSelectListPreference.this.logd("scan complete, load the cellInfosList");
                        }
                        NetworkSelectListPreference.this.networksListLoaded();
                    }
                    break;
            }
        }

        static boolean lambda$handleMessage$0(CellInfo cellInfo) {
            return cellInfo == null;
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        if (DBG) {
            logd("user manually close the dialog");
        }
        try {
            if (this.mNetworkQueryService != null) {
                this.mNetworkQueryService.stopNetworkQuery();
                this.mNetworkQueryService.unregisterCallback(this.mCallback);
            }
            this.mNetworkOperators.getNetworkSelectionMode();
        } catch (RemoteException e) {
            loge("onCancel: exception from stopNetworkQuery " + e);
        }
    }

    @Override
    protected void onDialogClosed(boolean z) {
        super.onDialogClosed(z);
        if (!z) {
            this.mNetworkOperators.getNetworkSelectionMode();
        }
    }

    public void setNetworkQueryService(INetworkQueryService iNetworkQueryService) {
        this.mNetworkQueryService = iNetworkQueryService;
    }

    protected void initialize(int i, INetworkQueryService iNetworkQueryService, NetworkOperators networkOperators, ProgressDialog progressDialog) {
        this.mSubId = i;
        this.mNetworkQueryService = iNetworkQueryService;
        this.mNetworkOperators = networkOperators;
        this.mProgressDialog = progressDialog;
        this.mNeedScanAgain = false;
        if (SubscriptionManager.isValidSubscriptionId(this.mSubId)) {
            this.mPhoneId = SubscriptionManager.getPhoneId(this.mSubId);
        }
        this.mTelephonyManager = (TelephonyManager) getContext().getSystemService("phone");
        setSummary(new TelephonyManager(getContext(), this.mSubId).getNetworkOperatorName());
        setOnPreferenceChangeListener(this);
        this.mShowAlert = getContext().getResources().getBoolean(R.bool.config_show_plmn_search_alert_dialog);
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        registerPhoneState();
    }

    @Override
    protected void onPrepareForRemoval() {
        destroy();
        super.onPrepareForRemoval();
    }

    private void destroy() {
        try {
            dismissProgressBar();
        } catch (IllegalArgumentException e) {
            loge("onDestroy: exception from dismissProgressBar " + e);
        }
        try {
            if (this.mNetworkQueryService != null) {
                this.mNetworkQueryService.unregisterCallback(this.mCallback);
            }
        } catch (RemoteException e2) {
            loge("onDestroy: exception from unregisterCallback " + e2);
        }
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        if (this.mPhoneStateListener != null) {
            unRegisterPhoneState();
        }
    }

    private void displayEmptyNetworkList() {
        PhoneGlobals.getInstance().notificationMgr.postTransientNotification(2, getContext().getResources().getString(R.string.empty_networks_list));
    }

    private void displayNetworkSelectionInProgress() {
        showProgressDialog(100);
    }

    private void displayNetworkQueryFailed(int i) {
        String string = getContext().getResources().getString(R.string.network_query_error);
        try {
            dismissProgressBar();
        } catch (IllegalArgumentException e) {
        }
        PhoneGlobals.getInstance().notificationMgr.postTransientNotification(2, string);
    }

    private void loadNetworksList(boolean z) {
        if (DBG) {
            logd("load networks list, mNeedScanAgain = " + this.mNeedScanAgain);
        }
        if (!this.mNeedScanAgain) {
            showProgressDialog(DIALOG_NETWORK_LIST_LOAD);
        }
        if (this.mShowAlert) {
            showAlertDialog(1000);
        }
        try {
            if (this.mNetworkQueryService != null) {
                this.mNetworkQueryService.startNetworkQuery(this.mCallback, this.mPhoneId, z);
            } else {
                displayNetworkQueryFailed(1);
            }
        } catch (RemoteException e) {
            loge("loadNetworksList: exception from startNetworkQuery " + e);
            displayNetworkQueryFailed(1);
        }
    }

    private void networksListLoaded() {
        if (DBG) {
            logd("networks list loaded");
        }
        dismissAlertDialog(1000);
        if (DBG) {
            logd("hideProgressPanel");
        }
        try {
            dismissProgressBar();
        } catch (IllegalArgumentException e) {
            loge("Fail to dismiss network load list dialog " + e);
        }
        this.mNetworkOperators.getNetworkSelectionMode();
        if (this.mCellInfoList != null) {
            Iterator<CellInfo> it = this.mCellInfoList.iterator();
            while (it.hasNext()) {
                if (getOperatorInfoFromCellInfo(it.next()).getOperatorNumeric().equals("46020")) {
                    it.remove();
                }
            }
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            int i = 0;
            ExtensionManager.getNetworkSettingExt().customizeNetworkSelectKey(this);
            for (CellInfo cellInfo : this.mCellInfoList) {
                String networkTitle = getNetworkTitle(cellInfo);
                if (!arrayList.contains(networkTitle)) {
                    if (CellInfoUtil.isForbidden(cellInfo, this.mForbiddenPlmns)) {
                        networkTitle = networkTitle + " " + getContext().getResources().getString(R.string.forbidden_network);
                    }
                    arrayList.add(networkTitle);
                    arrayList2.add(String.valueOf(i));
                    i++;
                }
            }
            setEntries((CharSequence[]) arrayList.toArray(new CharSequence[arrayList.size()]));
            setEntryValues((CharSequence[]) arrayList2.toArray(new CharSequence[arrayList2.size()]));
            try {
                super.onClick();
                return;
            } catch (WindowManager.BadTokenException e2) {
                logd("showDialog exception: " + e2);
                return;
            }
        }
        displayEmptyNetworkList();
    }

    private void dismissProgressBar() {
        if (this.mProgressDialog != null && this.mProgressDialog.isShowing()) {
            this.mProgressDialog.dismiss();
        }
    }

    private void showProgressDialog(int i) {
        if (this.mProgressDialog == null) {
            this.mProgressDialog = new ProgressDialog(getContext());
        } else {
            dismissProgressBar();
        }
        if (i == 100) {
            this.mProgressDialog.setMessage(getContext().getResources().getString(R.string.register_on_network, getNetworkTitle(this.mCellInfo)));
            this.mProgressDialog.setCanceledOnTouchOutside(false);
            this.mProgressDialog.setCancelable(false);
            this.mProgressDialog.setIndeterminate(true);
        } else if (i == DIALOG_NETWORK_LIST_LOAD) {
            this.mProgressDialog.setMessage(getContext().getResources().getString(R.string.load_networks_progress));
            this.mProgressDialog.setCanceledOnTouchOutside(false);
            this.mProgressDialog.setCancelable(true);
            this.mProgressDialog.setIndeterminate(false);
            this.mProgressDialog.setOnCancelListener(this);
        }
        this.mProgressDialog.show();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        int i = Integer.parseInt((String) obj);
        this.mCellInfo = this.mCellInfoList.get(i);
        if (DBG) {
            logd("index = " + i + ", selected network: " + this.mCellInfo.toString());
        }
        MetricsLogger.action(getContext(), 1210);
        Message messageObtainMessage = this.mHandler.obtainMessage(1);
        Phone phone = PhoneFactory.getPhone(this.mPhoneId);
        if (phone != null) {
            OperatorInfo operatorInfoFromCellInfo = getOperatorInfoFromCellInfo(this.mCellInfo);
            if (ExtensionManager.getNetworkSettingExt().onPreferenceTreeClick(operatorInfoFromCellInfo, this.mPhoneId)) {
                return true;
            }
            if (DBG) {
                logd("manually selected network: " + operatorInfoFromCellInfo.toString());
            }
            phone.selectNetworkManually(operatorInfoFromCellInfo, true, messageObtainMessage);
            displayNetworkSelectionInProgress();
        } else {
            loge("Error selecting network. phone is null.");
        }
        return true;
    }

    private String getNetworkTitle(CellInfo cellInfo) {
        String strUnicodeWrap;
        OperatorInfo operatorInfoFromCellInfo = getOperatorInfoFromCellInfo(cellInfo);
        if (!TextUtils.isEmpty(operatorInfoFromCellInfo.getOperatorAlphaLong())) {
            strUnicodeWrap = operatorInfoFromCellInfo.getOperatorAlphaLong();
        } else if (!TextUtils.isEmpty(operatorInfoFromCellInfo.getOperatorAlphaShort())) {
            strUnicodeWrap = operatorInfoFromCellInfo.getOperatorAlphaShort();
        } else {
            strUnicodeWrap = BidiFormatter.getInstance().unicodeWrap(operatorInfoFromCellInfo.getOperatorNumeric(), TextDirectionHeuristics.LTR);
        }
        return ExtensionManager.getNetworkSettingExt().customizeNetworkName(operatorInfoFromCellInfo, this.mSubId, strUnicodeWrap);
    }

    private String getOperatorNumeric(CellInfo cellInfo) {
        return getOperatorInfoFromCellInfo(cellInfo).getOperatorNumeric();
    }

    private OperatorInfo getOperatorInfoFromCellInfo(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoLte) {
            CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
            return new OperatorInfo((String) cellInfoLte.getCellIdentity().getOperatorAlphaLong(), (String) cellInfoLte.getCellIdentity().getOperatorAlphaShort(), cellInfoLte.getCellIdentity().getMobileNetworkOperator());
        }
        if (cellInfo instanceof CellInfoWcdma) {
            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo;
            return new OperatorInfo((String) cellInfoWcdma.getCellIdentity().getOperatorAlphaLong(), (String) cellInfoWcdma.getCellIdentity().getOperatorAlphaShort(), cellInfoWcdma.getCellIdentity().getMobileNetworkOperator());
        }
        if (cellInfo instanceof CellInfoGsm) {
            CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
            return new OperatorInfo((String) cellInfoGsm.getCellIdentity().getOperatorAlphaLong(), (String) cellInfoGsm.getCellIdentity().getOperatorAlphaShort(), cellInfoGsm.getCellIdentity().getMobileNetworkOperator());
        }
        if (cellInfo instanceof CellInfoCdma) {
            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
            return new OperatorInfo((String) cellInfoCdma.getCellIdentity().getOperatorAlphaLong(), (String) cellInfoCdma.getCellIdentity().getOperatorAlphaShort(), "");
        }
        return new OperatorInfo("", "", "");
    }

    private boolean isInvalidCellInfo(CellInfo cellInfo) {
        CharSequence operatorAlphaLong;
        CharSequence operatorAlphaShort;
        boolean z;
        if (DBG) {
            logd("Check isInvalidCellInfo: " + cellInfo.toString());
        }
        if (cellInfo instanceof CellInfoLte) {
            CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
            operatorAlphaLong = cellInfoLte.getCellIdentity().getOperatorAlphaLong();
            operatorAlphaShort = cellInfoLte.getCellIdentity().getOperatorAlphaShort();
            z = !cellInfoLte.getCellSignalStrength().equals(new CellSignalStrengthLte());
        } else if (cellInfo instanceof CellInfoWcdma) {
            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo;
            operatorAlphaLong = cellInfoWcdma.getCellIdentity().getOperatorAlphaLong();
            operatorAlphaShort = cellInfoWcdma.getCellIdentity().getOperatorAlphaShort();
            z = !cellInfoWcdma.getCellSignalStrength().equals(new CellSignalStrengthWcdma());
        } else if (cellInfo instanceof CellInfoGsm) {
            CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
            operatorAlphaLong = cellInfoGsm.getCellIdentity().getOperatorAlphaLong();
            operatorAlphaShort = cellInfoGsm.getCellIdentity().getOperatorAlphaShort();
            z = !cellInfoGsm.getCellSignalStrength().equals(new CellSignalStrengthGsm());
        } else {
            if (!(cellInfo instanceof CellInfoCdma)) {
                return true;
            }
            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
            operatorAlphaLong = cellInfoCdma.getCellIdentity().getOperatorAlphaLong();
            operatorAlphaShort = cellInfoCdma.getCellIdentity().getOperatorAlphaShort();
            z = !cellInfoCdma.getCellSignalStrength().equals(new CellSignalStrengthCdma());
        }
        return TextUtils.isEmpty(operatorAlphaLong) && TextUtils.isEmpty(operatorAlphaShort) && z;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        if (isPersistent()) {
            return parcelableOnSaveInstanceState;
        }
        SavedState savedState = new SavedState(parcelableOnSaveInstanceState);
        savedState.mDialogListEntries = getEntries();
        savedState.mDialogListEntryValues = getEntryValues();
        savedState.mCellInfoList = this.mCellInfoList;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable == null || !parcelable.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        if (getEntries() == null && savedState.mDialogListEntries != null) {
            setEntries(savedState.mDialogListEntries);
        }
        if (getEntryValues() == null && savedState.mDialogListEntryValues != null) {
            setEntryValues(savedState.mDialogListEntryValues);
        }
        if (this.mCellInfoList == null && savedState.mCellInfoList != null) {
            this.mCellInfoList = savedState.mCellInfoList;
        }
        super.onRestoreInstanceState(savedState.getSuperState());
    }

    private static class SavedState extends Preference.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        List<CellInfo> mCellInfoList;
        CharSequence[] mDialogListEntries;
        CharSequence[] mDialogListEntryValues;

        SavedState(Parcel parcel) {
            super(parcel);
            ClassLoader classLoader = Object.class.getClassLoader();
            this.mDialogListEntries = parcel.readCharSequenceArray();
            this.mDialogListEntryValues = parcel.readCharSequenceArray();
            if (this.mCellInfoList != null) {
                this.mCellInfoList = parcel.readParcelableList(this.mCellInfoList, classLoader);
            }
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeCharSequenceArray(this.mDialogListEntries);
            parcel.writeCharSequenceArray(this.mDialogListEntryValues);
            parcel.writeParcelableList(this.mCellInfoList, i);
        }

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }
    }

    private void logd(String str) {
        Log.d(LOG_TAG, "[NetworksList] " + str);
    }

    private void loge(String str) {
        Log.e(LOG_TAG, "[NetworksList] " + str);
    }

    private void showAlertDialog(int i) {
        if (i == 400) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(android.R.string.dialog_alert_title);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(R.string.network_setting_all_forbidden_dialog);
            builder.setPositiveButton(android.R.string.yes, (DialogInterface.OnClickListener) null);
            this.mPlmnAlertDialog = builder.create();
            this.mPlmnAlertDialog.show();
            return;
        }
        if (i == 1000) {
            if (DBG) {
                logd("show PLMN search alert...");
            }
            AlertDialog.Builder builder2 = new AlertDialog.Builder(getContext());
            builder2.setMessage(R.string.msg_plmn_search_alert);
            builder2.setPositiveButton(android.R.string.yes, (DialogInterface.OnClickListener) null);
            builder2.create().show();
        }
    }

    @Override
    public void handleSubInfoUpdate() {
        if (DBG) {
            logd("handleSubInfoUpdate...");
        }
    }

    private void cancelNetworkSearch() {
        if (DBG) {
            logd("cancel network search");
        }
        if (this.mNetworkQueryService != null) {
            try {
                if (!isWfcCallOngoing()) {
                    this.mNetworkQueryService.stopNetworkQuery();
                    this.mNetworkQueryService.unregisterCallback(this.mCallback);
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e2) {
                if (DBG) {
                    logd("Fail to dismiss network load list dialog");
                }
            }
            dismissAlertDialog(1000);
        }
        try {
            dismissProgressBar();
        } catch (IllegalArgumentException e3) {
            loge("Fail to dismiss progress dialog " + e3);
        }
    }

    private void registerPhoneState() {
        this.mPhoneStateListener = new PhoneStateListener(Integer.valueOf(this.mSubId)) {
            @Override
            public void onCallStateChanged(int i, String str) {
                super.onCallStateChanged(i, str);
                NetworkSelectListPreference.this.logd("onCallStateChanged ans state is " + i);
                if (!(i == 0)) {
                    NetworkSelectListPreference.this.cancelNetworkSearch();
                }
            }
        };
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        }
    }

    private void unRegisterPhoneState() {
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
    }

    private void dismissAlertDialog(int i) {
        if (this.mShowAlert && i == 1000) {
            if (DBG) {
                logd("dismiss PLMN search alert...");
            }
            if (this.mPlmnAlertDialog != null) {
                this.mPlmnAlertDialog.dismiss();
                this.mPlmnAlertDialog = null;
            }
        }
    }

    private boolean isWfcCallOngoing() {
        boolean z = false;
        if (ImsManager.isWfcEnabledByPlatform(getContext())) {
            Phone phone = PhoneFactory.getPhone(this.mPhoneId);
            if (phone != null) {
                ImsPhone imsPhone = phone.getImsPhone();
                if (DBG) {
                    logd("isWfcCallOngoing imsPhone" + imsPhone);
                }
                if (((TelephonyManager) phone.getContext().getSystemService("phone")).isWifiCallingAvailable() && imsPhone != null && (!imsPhone.getBackgroundCall().isIdle() || !imsPhone.getForegroundCall().isIdle() || !imsPhone.getRingingCall().isIdle())) {
                    z = true;
                }
            } else {
                logd("Error phone is null.");
            }
        }
        logd("isWfcCallOngoing" + z);
        return z;
    }
}

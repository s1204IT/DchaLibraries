package com.mediatek.settings;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.mediatek.internal.telephony.FemtoCellInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class FemtoPointList extends PreferenceActivity implements DialogInterface.OnCancelListener, PhoneGlobals.SubInfoUpdateListener {
    private boolean mAirplaneModeEnabled;
    private ArrayList<FemtoCellInfo> mFemtoList;
    private PreferenceScreen mFemtoPointListContainer;
    private HashMap<Preference, FemtoCellInfo> mFemtoPointMap;
    private MyHandler mHandler;
    private IntentFilter mIntentFilter;
    private boolean mIsDoingAction;
    private Phone mPhone;
    private String mSelectFemotCellTips;
    private int mSubId;
    private Toast mToast;
    protected boolean mIsForeground = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.AIRPLANE_MODE")) {
                FemtoPointList.this.mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                FemtoPointList.this.log("ACTION_AIRPLANE_MODE_CHANGED || mAirplaneModeEnabled:" + FemtoPointList.this.mAirplaneModeEnabled);
                FemtoPointList.this.setScreenEnabled(true);
            }
        }
    };
    private final int[] FEMTO_CELL_ICON_TYPE = {R.drawable.mtk_csgs_other_type, R.drawable.mtk_csgs_allowed_type, R.drawable.mtk_csgs_operator_allowed_type, R.drawable.mtk_csgs_operator_rejected_type};

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.mtk_carrier_select_list);
        this.mHandler = new MyHandler();
        this.mFemtoPointListContainer = (PreferenceScreen) findPreference("list_networks_key");
        this.mFemtoPointMap = new HashMap<>();
        setTitle(R.string.femtocell_point_list_title);
        setActionBarEnable();
        this.mSubId = getIntent().getIntExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, -1);
        this.mPhone = PhoneUtils.getPhoneUsingSubId(this.mSubId);
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mReceiver, this.mIntentFilter);
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        log("onPreferenceTreeClick() select FemtoCell :" + ((Object) preference.getTitle()));
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            log("onPreferenceTreeClick(...) in geminiPhone status");
        } else {
            this.mSelectFemotCellTips = getApplicationContext().getString(R.string.register_femtocell_point_wait_tip, preference.getTitle());
            displayFemtoCellSeletionProgressDialog();
            PhoneFactory.getPhone(this.mPhone.getPhoneId()).selectFemtoCell(this.mFemtoPointMap.get(preference), this.mHandler.obtainMessage(2));
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mIsForeground = true;
        scanFemtoCellPoint();
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause mIsDoingAction = " + this.mIsDoingAction);
        this.mIsForeground = false;
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            log("onPause GeminiSupport");
        } else if (this.mIsDoingAction) {
            this.mIsDoingAction = false;
            PhoneFactory.getPhone(this.mPhone.getPhoneId()).abortFemtoCellList(this.mHandler.obtainMessage(3));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        log("[onDestroy]Call onDestroy. unbindService");
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
    }

    private class MyHandler extends Handler {
        private MyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            FemtoPointList.this.log("Handle message msg.what = " + message.what);
            switch (message.what) {
                case 1:
                    handleGetFemtoCellListResponse(message);
                    break;
                case 2:
                    handleSelectFemtoCellResponse(message);
                    break;
                case 3:
                    handleAbortFemtoCellListResponse(message);
                    break;
                default:
                    FemtoPointList.this.log("Handle message default");
                    break;
            }
        }

        private void handleGetFemtoCellListResponse(Message message) {
            FemtoPointList.this.log("Handle getFemtoCellList done.");
            FemtoPointList.this.mIsDoingAction = false;
            FemtoPointList.this.hideFemtoPointListLoadProgressDialog();
            FemtoPointList.this.setScreenEnabled(true);
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception == null) {
                FemtoPointList.this.refreshPreference((ArrayList) asyncResult.result);
                return;
            }
            FemtoPointList.this.log("handleGetPLMNResponse with exception = " + asyncResult.exception);
            if (!(asyncResult.exception instanceof CommandException) || asyncResult.exception.getCommandError() == CommandException.Error.ABORTED) {
                FemtoPointList.this.log("handleGetFemtoCellListResponse else case...");
                return;
            }
            FemtoPointList.this.showScanFailTips();
            if (FemtoPointList.this.mFemtoList == null) {
                FemtoPointList.this.mFemtoList = new ArrayList();
            }
        }

        private void handleSelectFemtoCellResponse(Message message) {
            FemtoPointList.this.log("Handle selectFemtoCell done.");
            FemtoPointList.this.mIsDoingAction = false;
            FemtoPointList.this.hideFemtoCellSeletionProgressDialog();
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                FemtoPointList.this.showSelectFailTips();
                FemtoPointList.this.log("handleSelectFemtoCellResponse with exception = " + asyncResult.exception);
                return;
            }
            FemtoPointList.this.log("handleSelectFemtoCellResponse with OK result!");
            FemtoPointList.this.finish();
        }

        private void handleAbortFemtoCellListResponse(Message message) {
            FemtoPointList.this.log("handleAbortFemtoCellListResponse ");
        }
    }

    private void scanFemtoCellPoint() {
        log("scanFemtoCellPoint ...");
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            log("scanFemtoCellPoint() in geminiPhone status");
            return;
        }
        displayFemtoPointListLoadProgressDialog();
        PhoneFactory.getPhone(this.mPhone.getPhoneId()).getFemtoCellList(this.mHandler.obtainMessage(1));
        this.mIsDoingAction = true;
    }

    private void refreshPreference(ArrayList<FemtoCellInfo> arrayList) {
        clearScreenAndContainers();
        this.mFemtoList = arrayList;
        if (arrayList == null || arrayList.size() == 0) {
            log("refreshPreference : NULL FemtoCell list!");
            if (arrayList == null) {
                this.mFemtoList = new ArrayList<>();
                return;
            }
            return;
        }
        log("Add FemtoCell Number : " + arrayList.size());
        Iterator<FemtoCellInfo> it = arrayList.iterator();
        while (it.hasNext()) {
            addFemtoCellPreference(it.next());
        }
    }

    private void clearScreenAndContainers() {
        if (this.mFemtoPointListContainer.getPreferenceCount() != 0) {
            this.mFemtoPointListContainer.removeAll();
        }
        if (this.mFemtoPointMap != null) {
            this.mFemtoPointMap.clear();
        }
        if (this.mFemtoList != null) {
            this.mFemtoList.clear();
        }
    }

    private void addFemtoCellPreference(FemtoCellInfo femtoCellInfo) {
        Preference preference = new Preference(this);
        fillPreferenceWithFemtoCellInfo(preference, femtoCellInfo);
        this.mFemtoPointListContainer.addPreference(preference);
        this.mFemtoPointMap.put(preference, femtoCellInfo);
    }

    private void fillPreferenceWithFemtoCellInfo(Preference preference, FemtoCellInfo femtoCellInfo) {
        if (preference == null) {
            log("fillPreference Pref == null");
            return;
        }
        preference.setIcon(this.FEMTO_CELL_ICON_TYPE[femtoCellInfo.getCsgIconType()]);
        preference.setTitle(femtoCellInfo.getOperatorAlphaLong() + " " + getHnbOrCsgId(femtoCellInfo));
    }

    private String getHnbOrCsgId(FemtoCellInfo femtoCellInfo) {
        String homeNodeBName = femtoCellInfo.getHomeNodeBName();
        if (homeNodeBName.equals("")) {
            String strValueOf = String.valueOf(femtoCellInfo.getCsgId());
            log("getHnbOrCsgId : result == null =: " + strValueOf);
            return strValueOf;
        }
        return homeNodeBName;
    }

    private void showScanFailTips() {
        showTips(R.string.network_query_error);
    }

    private void showSelectFailTips() {
        showTips(R.string.register_femtocell_point_result_fail_tip);
    }

    private void showTips(int i) {
        if (this.mToast != null) {
            this.mToast.cancel();
        }
        this.mToast = Toast.makeText(this, getResources().getString(i), 0);
        this.mToast.show();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int i) {
        ProgressDialog progressDialogCreateFemtoPointListLoadProgressDialog;
        if (i == 100) {
            progressDialogCreateFemtoPointListLoadProgressDialog = createFemtoPointListLoadProgressDialog();
        } else if (i == 200) {
            progressDialogCreateFemtoPointListLoadProgressDialog = createFemtoPointSelectProgressDialog();
        } else {
            progressDialogCreateFemtoPointListLoadProgressDialog = null;
        }
        log("[onCreateDialog] create dialog id is " + i);
        return progressDialogCreateFemtoPointListLoadProgressDialog;
    }

    private ProgressDialog createFemtoPointSelectProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(this.mSelectFemotCellTips);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        return progressDialog;
    }

    private ProgressDialog createFemtoPointListLoadProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.in_search_femtocell_networks_wait_tip));
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(this);
        progressDialog.setCanceledOnTouchOutside(false);
        return progressDialog;
    }

    private void displayFemtoPointListLoadProgressDialog() {
        if (this.mIsForeground) {
            showDialog(100);
        }
    }

    private void hideFemtoPointListLoadProgressDialog() {
        removeDialog(100);
    }

    private void displayFemtoCellSeletionProgressDialog() {
        if (this.mIsForeground) {
            showDialog(200);
        }
    }

    private void hideFemtoCellSeletionProgressDialog() {
        removeDialog(200);
    }

    @Override
    protected void onPrepareDialog(int i, Dialog dialog) {
        if (i == 200 || i == 100) {
            setScreenEnabled(false);
        }
    }

    private void setScreenEnabled(boolean z) {
        log("flag : " + z + " isRadioPoweroff : " + isRadioPoweroff() + " mAirplaneModeEnabled : " + this.mAirplaneModeEnabled);
        getPreferenceScreen().setEnabled((!z || isRadioPoweroff() || this.mAirplaneModeEnabled) ? false : true);
    }

    private void setActionBarEnable() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private boolean isRadioPoweroff() {
        return this.mPhone.getServiceState().getState() == 3;
    }

    private void log(String str) {
        Log.d("phone/FemtoPointList", "[FemtoCellsList] " + str);
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}

package com.mediatek.settings.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.List;

/* loaded from: classes.dex */
public class SmartCallFwdFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener {
    private AlertDialog mAlertDialog;
    boolean mBound;
    private Context mContext;
    private ProgressDialog mProgressDialog;
    private EditTextPreference mSim1Pref;
    private EditTextPreference mSim2Pref;
    private ListPreference mSmartCallFwdModePref;
    private SwitchBar mSwitchBar;
    private TelephonyManager mTelephonyManager;
    private MtkTelephonyManagerEx mTelephonyManagerEx;
    private final int MSG_TYPE_DATA = 0;
    private final int MSG_TYPE_REGISTER = 1;
    private final int MSG_GET_CF_REQ = 2;
    private final int MSG_SET_CF_REQ = 3;
    private final int MSG_GET_CF_RES = 4;
    private final int MSG_SET_CF_RES = 5;
    private final int MSG_TYPE_DEREGISTER = 6;
    private final int CF_REASON_NOT_REACHABLE = 3;
    private final int SIM1 = 0;
    private final int SIM2 = 1;
    private final int TOTAL_SIM = 2;
    private final int READ = 0;
    private final int WRITE = 1;
    private final int SIM1_TO_SIM2 = 1;
    private final int SIM2_TO_SIM1 = 2;
    private final int DUAL_SIM = 3;
    private boolean mSwitchState = false;
    private boolean mValidListener = false;
    private SharedPreferences mSharedPreferences = null;
    private Messenger mMessenger = new Messenger(new IncomingHandler());
    private CFInfo[] mCfInfoArr = {new CFInfo(), new CFInfo()};
    private String[] mSummary = {"SIM1 TO SIM2", "SIM2 TO SIM1", "DUAL SIM"};
    private String[] mActionString = {"Disabling", "Enabling"};
    private String[] simPrefValue = new String[2];
    private boolean mReadProgress = false;
    private boolean mWriteProgress = false;
    private boolean mFlag = false;
    private String mSim1num = null;
    private String mSim2num = null;
    private int mCurrSelectedMode = 3;
    private int mPrevSelectedMode = 3;
    private boolean mPrevSwitchState = false;
    private boolean mNewSim1Inserted = false;
    private boolean mNewSim2Inserted = false;
    Messenger mService = null;
    private ServiceConnection mConnection = new ServiceConnection() { // from class: com.mediatek.settings.sim.SmartCallFwdFragment.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) throws RemoteException {
            SmartCallFwdFragment.this.mService = new Messenger(iBinder);
            SmartCallFwdFragment.this.mBound = true;
            Message messageObtain = Message.obtain((Handler) null, 1);
            messageObtain.replyTo = SmartCallFwdFragment.this.mMessenger;
            try {
                SmartCallFwdFragment.this.mService.send(messageObtain);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            SmartCallFwdFragment.this.mService = null;
            SmartCallFwdFragment.this.mBound = false;
        }
    };

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.smart_call_fwd_settings);
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        Log.d("SmartCallFwdFragment", "onResume:");
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onDestroy() throws RemoteException {
        super.onDestroy();
        if (getActivity().isFinishing()) {
            stopService();
        }
    }

    @Override // android.app.Fragment
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d("SmartCallFwdFragment", "onAttach:");
        this.mContext = activity.getApplicationContext();
        this.mTelephonyManagerEx = MtkTelephonyManagerEx.getDefault();
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        startService();
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Log.d("SmartCallFwdFragment", "onActivityCreated:");
        this.mSharedPreferences = this.mContext.getSharedPreferences("sim_id", 0);
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        if (settingsActivity == null) {
            return;
        }
        this.mSwitchBar = settingsActivity.getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
        this.mSwitchBar.setEnabled(false);
        this.mSim1Pref = (EditTextPreference) findPreference("smart_sim1_pref");
        this.mSim2Pref = (EditTextPreference) findPreference("smart_sim2_pref");
        this.mSim1num = getLine1Number(0);
        this.mSim2num = getLine1Number(1);
        Log.d("SmartCallFwdFragment", "mSim1num: " + this.mSim1num + ", mSim2num: " + this.mSim2num);
        getPreviousPrefValue();
        if (this.mSim1num != null && this.mSim1num.length() > 0) {
            Log.d("SmartCallFwdFragment", "mSim1num.length():" + this.mSim1num.length());
            this.mSim1Pref.setText(this.mSim1num);
        } else {
            if (this.simPrefValue[0] != null) {
                this.mSim1Pref.setText(this.simPrefValue[0]);
            }
            this.mSim1num = null;
        }
        if (this.mSim2num == null || this.mSim2num.length() <= 0) {
            if (this.simPrefValue[1] != null) {
                this.mSim2Pref.setText(this.simPrefValue[1]);
            }
            this.mSim2num = null;
        } else {
            this.mSim2Pref.setText(this.mSim2num);
            Log.d("SmartCallFwdFragment", "mSim2num.length():" + this.mSim2num.length());
        }
        detectSimChange();
        this.mSmartCallFwdModePref = (ListPreference) findPreference("smart_call_fwd_modes");
        this.mSim1Pref.setOnPreferenceChangeListener(this);
        this.mSim2Pref.setOnPreferenceChangeListener(this);
        this.mSmartCallFwdModePref.setOnPreferenceChangeListener(this);
        this.mSmartCallFwdModePref.setValue(Integer.toString(3));
        this.mSmartCallFwdModePref.setSummary(this.mSummary[2]);
        showProgressDialog(getResources().getString(R.string.progress_dlg_reading));
    }

    private void showProgressDialog(String str) {
        Log.d("SmartCallFwdFragment", "showProgressDialog");
        this.mProgressDialog = new ProgressDialog(getActivity());
        this.mProgressDialog.setIndeterminate(false);
        this.mProgressDialog.setProgressStyle(0);
        this.mProgressDialog.setCancelable(false);
        this.mProgressDialog.setCanceledOnTouchOutside(false);
        this.mProgressDialog.setTitle(R.string.progress_dlg_title);
        this.mProgressDialog.setMessage(str);
        this.mProgressDialog.show();
    }

    private void updatePreference(int i) {
        Log.d("SmartCallFwdFragment", "updatePreference");
        this.mSwitchState = i != 0;
        if (i > 0) {
            this.mSmartCallFwdModePref.setValue(Integer.toString(i));
            this.mSmartCallFwdModePref.setSummary(this.mSummary[i - 1]);
        } else {
            this.mSmartCallFwdModePref.setValue(Integer.toString(3));
            this.mSmartCallFwdModePref.setSummary(this.mSummary[2]);
        }
        Log.d("SmartCallFwdFragment", "mSwitchState:" + this.mSwitchState);
        this.mSwitchBar.setEnabled(true);
        if (this.mSwitchState != this.mSwitchBar.isChecked()) {
            this.mFlag = true;
        }
        this.mSwitchBar.setChecked(this.mSwitchState);
        this.mSmartCallFwdModePref.setEnabled(true);
        if (this.mSim1num == null) {
            this.mSim1Pref.setEnabled(!this.mSwitchState);
        }
        if (this.mSim2num == null) {
            this.mSim2Pref.setEnabled(true ^ this.mSwitchState);
        }
        if (this.mNewSim1Inserted || this.mSim1Pref.getText() == null || (this.mSim1Pref.getText() != null && this.mSim1Pref.getText().length() == 0)) {
            this.mSim1Pref.setSummary("unknown");
            this.mSim1Pref.setText("");
        } else {
            this.mSim1Pref.setSummary(this.mSim1Pref.getText());
        }
        if (this.mNewSim2Inserted || this.mSim2Pref.getText() == null || (this.mSim2Pref.getText() != null && this.mSim2Pref.getText().length() == 0)) {
            this.mSim2Pref.setSummary("unknown");
            this.mSim2Pref.setText("");
        } else {
            this.mSim2Pref.setSummary(this.mSim2Pref.getText());
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:12:0x009e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void detectSimChange() {
        int simCount = this.mTelephonyManager.getSimCount();
        Log.d("SmartCallFwdFragment", "detectSimChange : numSlots = " + simCount);
        for (int i = 0; i < simCount; i++) {
            SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = SubscriptionManager.from(this.mContext).getActiveSubscriptionInfoForSimSlotIndex(i);
            Log.d("SmartCallFwdFragment", "sir = " + activeSubscriptionInfoForSimSlotIndex + "for slot" + i);
            StringBuilder sb = new StringBuilder();
            sb.append("sim_slot_");
            sb.append(i);
            String string = sb.toString();
            String lastSimImsi = getLastSimImsi(string);
            boolean z = true;
            if (activeSubscriptionInfoForSimSlotIndex != null) {
                String subscriberId = this.mTelephonyManager.getSubscriberId(activeSubscriptionInfoForSimSlotIndex.getSubscriptionId());
                Log.d("SmartCallFwdFragment", "lastSimImsi = " + lastSimImsi + " currentSimImsi = " + subscriberId);
                if (lastSimImsi.length() == 0 || !lastSimImsi.equals(subscriberId)) {
                    setLastSimImsi(string, subscriberId);
                } else {
                    z = false;
                }
            }
            if (i == 0) {
                this.mNewSim1Inserted = z;
                Log.d("SmartCallFwdFragment", "detectSimChange : mNewSim1Inserted = " + this.mNewSim1Inserted);
            } else {
                this.mNewSim2Inserted = z;
                Log.d("SmartCallFwdFragment", "detectSimChange : mNewSim2Inserted = " + this.mNewSim2Inserted);
            }
        }
    }

    private String getLastSimImsi(String str) {
        String string = "";
        try {
            string = this.mSharedPreferences.getString(str, "");
        } catch (ClassCastException e) {
            e.printStackTrace();
            this.mSharedPreferences.edit().remove(str).commit();
        }
        Log.d("SmartCallFwdFragment", "getLastSubId strSlotId = " + str + ", imsi = " + string);
        return string;
    }

    private void setLastSimImsi(String str, String str2) {
        Log.d("SmartCallFwdFragment", "setLastSubId: strSlotId = " + str + ", value = " + str2);
        SharedPreferences.Editor editorEdit = this.mSharedPreferences.edit();
        editorEdit.putString(str, str2);
        editorEdit.commit();
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
    }

    @Override // android.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) throws NumberFormatException, RemoteException {
        String str;
        boolean z;
        getActivity();
        Log.d("SmartCallFwdFragment", "onPreferenceChange:preference =" + preference + ", newValue: " + obj);
        String str2 = (String) obj;
        if (str2.length() == 0) {
            str = "unknown";
        } else {
            str = str2;
        }
        if (preference == this.mSim1Pref) {
            this.mSim1Pref.setSummary(str);
            if ("unknown".equals(str)) {
                setSimPrefValue("smart_sim1_pref", "");
            } else {
                setSimPrefValue("smart_sim1_pref", str);
            }
        } else if (preference == this.mSim2Pref) {
            this.mSim2Pref.setSummary(str);
            if ("unknown".equals(str)) {
                setSimPrefValue("smart_sim2_pref", "");
            } else {
                setSimPrefValue("smart_sim2_pref", str);
            }
        } else if (preference == this.mSmartCallFwdModePref) {
            int i = Integer.parseInt(str2);
            Log.d("SmartCallFwdFragment", "selected mode:" + i);
            this.mPrevSelectedMode = this.mCurrSelectedMode;
            this.mCurrSelectedMode = i;
            if (!this.mSwitchBar.isChecked()) {
                Log.d("SmartCallFwdFragment", "switch is off: don't update");
                z = false;
                this.mSmartCallFwdModePref.setValue(Integer.toString(i));
                this.mSmartCallFwdModePref.setSummary(this.mSummary[this.mCurrSelectedMode - 1]);
            } else {
                z = true;
            }
            setSmartCallFwdMode(i, z);
        }
        return true;
    }

    private void setSmartCallFwdMode(int i, boolean z) throws RemoteException {
        Log.d("SmartCallFwdFragment", "setSmartCallFwdMode:" + i);
        if (this.mPrevSelectedMode == this.mCurrSelectedMode) {
            showToast("Already Set");
            return;
        }
        if (i == 1) {
            this.mCfInfoArr[0].action = 1;
            this.mCfInfoArr[1].action = 0;
        } else if (i == 2) {
            this.mCfInfoArr[0].action = 0;
            this.mCfInfoArr[1].action = 1;
        } else {
            this.mCfInfoArr[0].action = 1;
            this.mCfInfoArr[1].action = 1;
        }
        if (z) {
            setCallForwardStatus(0, this.mCfInfoArr[0].action);
            showProgressDialog(getResources().getString(R.string.progress_dlg_writing));
        }
    }

    @Override // com.android.settings.widget.SwitchBar.OnSwitchChangeListener
    public void onSwitchChanged(Switch r3, boolean z) throws RemoteException {
        Log.d("SmartCallFwdFragment", "OnSwitchChanged: " + z);
        if (!isPhoneNumberSet()) {
            showToast("Set phone numbers for both SIMs first");
            if (z) {
                this.mSwitchBar.setChecked(false);
                return;
            }
            return;
        }
        setSmartCallFwdProperty(z);
        this.mPrevSwitchState = !z;
        if (this.mFlag) {
            this.mFlag = false;
            Log.d("SmartCallFwdFragment", "Not triggered from user");
            return;
        }
        this.mSwitchBar.setChecked(z);
        if (this.mSim1num == null) {
            this.mSim1Pref.setEnabled(!z);
        }
        if (this.mSim2num == null) {
            this.mSim2Pref.setEnabled(!z);
        }
        this.mSmartCallFwdModePref.setEnabled(true);
        if (!z) {
            disableSmartCallForward();
        } else {
            enableSmartCallForward();
        }
    }

    private void setSmartCallFwdProperty(boolean z) {
        int i;
        if (z) {
            i = this.mCurrSelectedMode;
        } else {
            i = 0;
        }
        Log.d("SmartCallFwdFragment", "SetSystem property to " + i);
        Settings.System.putInt(this.mContext.getContentResolver(), "smartcallmode", i);
    }

    private boolean isPhoneNumberSet() {
        if ("unknown".equals(this.mSim1Pref.getText()) || "unknown".equals(this.mSim2Pref.getText()) || this.mSim1Pref.getText() == null || this.mSim2Pref.getText() == null || ((this.mSim1Pref.getText() != null && this.mSim1Pref.getText().length() == 0) || (this.mSim2Pref.getText() != null && this.mSim2Pref.getText().length() == 0))) {
            Log.d("SmartCallFwdFragment", "Phone number/ numbers not present");
            return false;
        }
        Log.d("SmartCallFwdFragment", "Phone number/ numbers present");
        return true;
    }

    private void disableSmartCallForward() throws RemoteException {
        Log.d("SmartCallFwdFragment", "disableSmartCallForward for mCurrSelectedMode: " + this.mCurrSelectedMode);
        int i = 1;
        if (this.mCurrSelectedMode == 1) {
            this.mCfInfoArr[0].action = 0;
        } else {
            if (this.mCurrSelectedMode == 2) {
                this.mCfInfoArr[1].action = 0;
                setCallForwardStatus(i, this.mCfInfoArr[i].action);
                showProgressDialog(getResources().getString(R.string.progress_dlg_writing));
            }
            this.mCfInfoArr[0].action = 0;
            this.mCfInfoArr[1].action = 0;
        }
        i = 0;
        setCallForwardStatus(i, this.mCfInfoArr[i].action);
        showProgressDialog(getResources().getString(R.string.progress_dlg_writing));
    }

    private void enableSmartCallForward() throws RemoteException {
        Log.d("SmartCallFwdFragment", "enableSmartCallForward for mCurrSelectedMode: " + this.mCurrSelectedMode);
        int i = 0;
        if (this.mCurrSelectedMode == 1) {
            this.mCfInfoArr[0].action = 1;
        } else if (this.mCurrSelectedMode == 2) {
            this.mCfInfoArr[1].action = 1;
            i = 1;
        } else {
            this.mCfInfoArr[0].action = 1;
            this.mCfInfoArr[1].action = 1;
        }
        setCallForwardStatus(i, this.mCfInfoArr[i].action);
        showProgressDialog(getResources().getString(R.string.progress_dlg_writing));
    }

    private void getCallForwardStatus(int i) throws RemoteException {
        if (this.mService == null) {
            Log.d("SmartCallFwdFragment", "service not started yet");
            return;
        }
        Log.d("SmartCallFwdFragment", "getCallForwardStatus: " + i);
        this.mReadProgress = true;
        Message messageObtain = Message.obtain((Handler) null, 2);
        Bundle bundle = new Bundle();
        bundle.putInt("simId", i);
        bundle.putInt("act", 0);
        messageObtain.setData(bundle);
        try {
            this.mService.send(messageObtain);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void setCallForwardStatus(int i, int i2) throws RemoteException {
        if (this.mService == null) {
            Log.d("SmartCallFwdFragment", "service not started yet");
            return;
        }
        String text = (i == 0 ? this.mSim2Pref : this.mSim1Pref).getText();
        Log.d("SmartCallFwdFragment", "setCallForward to " + text);
        this.mWriteProgress = true;
        Message messageObtain = Message.obtain((Handler) null, 3);
        Bundle bundle = new Bundle();
        bundle.putInt("simId", i);
        bundle.putInt("act", 1);
        bundle.putString("phnum", text);
        bundle.putInt("action", i2);
        messageObtain.setData(bundle);
        try {
            this.mService.send(messageObtain);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    class IncomingHandler extends Handler {
        IncomingHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) throws RemoteException {
            new Bundle();
            Bundle data = message.getData();
            switch (message.what) {
                case 0:
                    break;
                case 1:
                    Log.d("SmartCallFwdFragment", "Service has started");
                    SmartCallFwdFragment.this.getCallForwardStatus(0);
                    break;
                case 2:
                case 3:
                default:
                    super.handleMessage(message);
                    break;
                case 4:
                    handleGetCfResp(data);
                    break;
                case 5:
                    handleSetCfResp(data);
                    break;
            }
        }

        private void handleSetCfResp(Bundle bundle) throws RemoteException {
            Log.d("SmartCallFwdFragment", "handleSetCfResp: " + bundle.getInt("simId"));
            Log.d("SmartCallFwdFragment", "status: " + bundle.getInt("status"));
            Log.d("SmartCallFwdFragment", "reason: " + bundle.getInt("reason"));
            Log.d("SmartCallFwdFragment", "phnum: " + bundle.getString("phnum"));
            Log.d("SmartCallFwdFragment", "error: " + bundle.getInt("err"));
            Log.d("SmartCallFwdFragment", "action: " + bundle.getInt("action"));
            Log.d("SmartCallFwdFragment", "callwait: " + bundle.getInt("callwait"));
            Log.d("SmartCallFwdFragment", "mCurrSelectedMode: " + SmartCallFwdFragment.this.mCurrSelectedMode);
            int i = bundle.getInt("err");
            int i2 = bundle.getInt("simId");
            SmartCallFwdFragment.this.mCfInfoArr[i2].status = bundle.getInt("status");
            SmartCallFwdFragment.this.mCfInfoArr[i2].phnum = bundle.getString("phnum");
            SmartCallFwdFragment.this.mCfInfoArr[i2].callwait = bundle.getInt("callwait");
            SmartCallFwdFragment.this.mCfInfoArr[i2].error = i;
            if (i == -1) {
                Log.d("SmartCallFwdFragment", "Network error" + (i2 + 1));
            } else if (i > 0) {
                Log.d("SmartCallFwdFragment", "set cf failed on sim" + (i2 + 1));
            } else {
                Log.d("SmartCallFwdFragment", "set cf success on sim" + (i2 + 1));
            }
            if (i2 == 0) {
                SmartCallFwdFragment.this.setCallForwardStatus(1, SmartCallFwdFragment.this.mCfInfoArr[1].action);
            } else {
                updateSetCfStatus();
            }
        }

        private void updateSetCfStatus() {
            String str;
            String string;
            String str2;
            String string2;
            String str3 = "[SIM1]:\n";
            int i = 0;
            if (SmartCallFwdFragment.this.mCfInfoArr[0].error != -1) {
                if (SmartCallFwdFragment.this.mCfInfoArr[0].error <= 0) {
                    if (SmartCallFwdFragment.this.mCfInfoArr[0].action == 0) {
                        str = str3 + "CallForward: disabled.\n";
                    } else {
                        str = str3 + "CallForward: enabled.\nIf SIM1 unreachable,incoming calls will be forwarded to " + SmartCallFwdFragment.this.mSim2Pref.getText();
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(str);
                    sb.append("\nCallWaiting: ");
                    sb.append(SmartCallFwdFragment.this.mCfInfoArr[0].callwait == 1 ? "enabled" : "disabled");
                    sb.append(" on SIM2");
                    string = sb.toString();
                    if (SmartCallFwdFragment.this.mCurrSelectedMode != 2) {
                        i = 1;
                    }
                } else {
                    string = str3 + "\nCallForward: " + SmartCallFwdFragment.this.mActionString[SmartCallFwdFragment.this.mCfInfoArr[0].action] + " failed.\n";
                }
            } else {
                string = str3 + SmartCallFwdFragment.this.getResources().getString(R.string.network_error);
            }
            String str4 = string + "\n\n[SIM2]:\n";
            if (SmartCallFwdFragment.this.mCfInfoArr[1].error != -1) {
                if (SmartCallFwdFragment.this.mCfInfoArr[1].error <= 0) {
                    if (SmartCallFwdFragment.this.mCfInfoArr[1].action == 0) {
                        str2 = str4 + "CallForward: disabled.\n";
                    } else {
                        str2 = str4 + "CallForward: enabled.\nIf SIM2 unreachable,incoming calls will be forwarded to " + SmartCallFwdFragment.this.mSim1Pref.getText();
                    }
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(str2);
                    sb2.append("\nCallWaiting: ");
                    sb2.append(SmartCallFwdFragment.this.mCfInfoArr[1].callwait == 1 ? "enabled" : "disabled");
                    sb2.append(" on SIM1");
                    string2 = sb2.toString();
                    if (SmartCallFwdFragment.this.mCurrSelectedMode != 1) {
                        i |= 2;
                    }
                } else {
                    string2 = str4 + "CallForward: " + SmartCallFwdFragment.this.mActionString[SmartCallFwdFragment.this.mCfInfoArr[1].action] + " failed.\n";
                }
            } else {
                string2 = str4 + SmartCallFwdFragment.this.getResources().getString(R.string.network_error);
            }
            Log.d("SmartCallFwdFragment", "successMode:" + i);
            Log.d("SmartCallFwdFragment", "mPrevSelectedMode:" + SmartCallFwdFragment.this.mPrevSelectedMode);
            if (i > 0) {
                SmartCallFwdFragment.this.mSmartCallFwdModePref.setValue(Integer.toString(i));
                SmartCallFwdFragment.this.mSmartCallFwdModePref.setSummary(SmartCallFwdFragment.this.mSummary[i - 1]);
            } else {
                if (!SmartCallFwdFragment.this.mPrevSwitchState) {
                    SmartCallFwdFragment.this.mFlag = true;
                    if (SmartCallFwdFragment.this.mSim1num == null) {
                        SmartCallFwdFragment.this.mSim1Pref.setEnabled(true);
                    }
                    if (SmartCallFwdFragment.this.mSim2num == null) {
                        SmartCallFwdFragment.this.mSim2Pref.setEnabled(true);
                    }
                    SmartCallFwdFragment.this.mSwitchBar.setChecked(SmartCallFwdFragment.this.mPrevSwitchState);
                }
                SmartCallFwdFragment.this.mCurrSelectedMode = SmartCallFwdFragment.this.mPrevSelectedMode;
                SmartCallFwdFragment.this.mSmartCallFwdModePref.setValue(Integer.toString(SmartCallFwdFragment.this.mCurrSelectedMode));
                SmartCallFwdFragment.this.mSmartCallFwdModePref.setSummary(SmartCallFwdFragment.this.mSummary[SmartCallFwdFragment.this.mCurrSelectedMode - 1]);
            }
            if (SmartCallFwdFragment.this.mProgressDialog != null) {
                Log.d("SmartCallFwdFragment", "Updating complete:");
                SmartCallFwdFragment.this.mProgressDialog.dismiss();
                SmartCallFwdFragment.this.mProgressDialog = null;
            }
            Log.d("SmartCallFwdFragment", "statusMsg: " + string2);
            SmartCallFwdFragment.this.showAlertDialog(SmartCallFwdFragment.this.getResources().getString(R.string.progress_dlg_title), string2);
        }

        private void handleGetCfResp(Bundle bundle) throws RemoteException {
            Log.d("SmartCallFwdFragment", "handleGetCfResp: " + bundle.getInt("simId"));
            Log.d("SmartCallFwdFragment", "status" + bundle.getInt("status"));
            Log.d("SmartCallFwdFragment", "reason" + bundle.getInt("reason"));
            Log.d("SmartCallFwdFragment", "phnum" + bundle.getString("phnum"));
            int i = bundle.getInt("simId");
            if (SmartCallFwdFragment.this.mCfInfoArr[i] != null) {
                SmartCallFwdFragment.this.mCfInfoArr[i].status = bundle.getInt("status");
                SmartCallFwdFragment.this.mCfInfoArr[i].reason = bundle.getInt("reason");
                SmartCallFwdFragment.this.mCfInfoArr[i].phnum = bundle.getString("phnum");
                SmartCallFwdFragment.this.mCfInfoArr[i].callwait = bundle.getInt("callwait");
                SmartCallFwdFragment.this.mCfInfoArr[i].error = bundle.getInt("err");
            }
            if (i == 0) {
                SmartCallFwdFragment.this.getCallForwardStatus(1);
            } else if (i == 1) {
                SmartCallFwdFragment.this.updateGetCfStatus();
            }
        }
    }

    private void updateGetCfStatus() {
        String string;
        int i;
        String text = this.mSim1Pref.getText();
        String text2 = this.mSim2Pref.getText();
        Log.d("SmartCallFwdFragment", "updateGetCfStatus");
        Log.d("SmartCallFwdFragment", "sim1Num: " + text + ", sim2Num: " + text2);
        StringBuilder sb = new StringBuilder();
        sb.append("mCfInfoArr[SIM1] phnum:");
        sb.append(this.mCfInfoArr[0].phnum);
        Log.d("SmartCallFwdFragment", sb.toString());
        Log.d("SmartCallFwdFragment", "mCfInfoArr[SIM2] phnum:" + this.mCfInfoArr[1].phnum);
        Log.d("SmartCallFwdFragment", "mCfInfoArr[SIM1] callwait:" + this.mCfInfoArr[0].callwait);
        Log.d("SmartCallFwdFragment", "mCfInfoArr[SIM2] callwait:" + this.mCfInfoArr[1].callwait);
        if (this.mCfInfoArr[0].error == -1 || this.mCfInfoArr[1].error == -1) {
            this.mSwitchBar.setEnabled(true);
            if (this.mProgressDialog != null) {
                this.mProgressDialog.dismiss();
                this.mProgressDialog = null;
            }
            string = "" + getResources().getString(R.string.network_error);
            showToast(string);
            i = 0;
        } else {
            int i2 = (this.mCfInfoArr[0].status == 1 && this.mCfInfoArr[0].callwait == 1 && PhoneNumberUtils.compareLoosely(text2, this.mCfInfoArr[0].phnum)) ? 1 : 0;
            if (this.mCfInfoArr[1].status == 1 && this.mCfInfoArr[1].callwait == 1 && PhoneNumberUtils.compareLoosely(text, this.mCfInfoArr[1].phnum)) {
                i = i2 | 2;
            } else {
                i = i2;
            }
            if (i == 1) {
                this.mCfInfoArr[1].action = 0;
                this.mCurrSelectedMode = 1;
            } else if (i == 2) {
                this.mCfInfoArr[0].action = 0;
                this.mCurrSelectedMode = 2;
            } else {
                this.mCurrSelectedMode = 3;
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("");
            sb2.append("[SIM1]:\n CallForwarding: ");
            sb2.append((i & 1) == 1 ? "enabled" : "disabled");
            String string2 = sb2.toString();
            StringBuilder sb3 = new StringBuilder();
            sb3.append(string2);
            sb3.append("\n CallWaiting: ");
            sb3.append(this.mCfInfoArr[0].callwait == 1 ? "enabled" : "disabled");
            String string3 = sb3.toString();
            StringBuilder sb4 = new StringBuilder();
            sb4.append(string3);
            sb4.append("\n\n[SIM2]:\n CallForwarding: ");
            sb4.append((i & 2) == 2 ? "enabled" : "disabled");
            String string4 = sb4.toString();
            StringBuilder sb5 = new StringBuilder();
            sb5.append(string4);
            sb5.append("\n CallWaiting: ");
            sb5.append(this.mCfInfoArr[1].callwait == 1 ? "enabled" : "disabled");
            string = sb5.toString();
        }
        updatePreference(i);
        Log.d("SmartCallFwdFragment", "cfStatus:" + i);
        Log.d("SmartCallFwdFragment", "statusMsg: " + string);
        if (this.mProgressDialog != null) {
            Log.d("SmartCallFwdFragment", "Reading complete:");
            this.mProgressDialog.dismiss();
            this.mProgressDialog = null;
        }
    }

    private void startService() {
        Intent intentCreateExplicitFromImplicitIntent = createExplicitFromImplicitIntent(this.mContext, new Intent("mediatek.settings.SMART_CALL_FWD_SERVICE"));
        if (intentCreateExplicitFromImplicitIntent != null) {
            this.mContext.bindService(intentCreateExplicitFromImplicitIntent, this.mConnection, 1);
        } else {
            Log.d("SmartCallFwdFragment", "null explicit intent");
        }
    }

    private void stopService() throws RemoteException {
        if (this.mBound) {
            try {
                this.mService.send(Message.obtain((Handler) null, 6));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            this.mMessenger = null;
            getActivity().getApplicationContext().unbindService(this.mConnection);
        }
    }

    public static Intent createExplicitFromImplicitIntent(Context context, Intent intent) {
        Log.d("SmartCallFwdFragment", "createExplicitFromImplicitIntent");
        List<ResolveInfo> listQueryIntentServices = context.getPackageManager().queryIntentServices(intent, 0);
        if (listQueryIntentServices == null || listQueryIntentServices.size() != 1) {
            return null;
        }
        ResolveInfo resolveInfo = listQueryIntentServices.get(0);
        ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
        Intent intent2 = new Intent(intent);
        intent2.setComponent(componentName);
        return intent2;
    }

    class CFInfo {
        public int simId = 0;
        public int status = 0;
        public int reason = 3;
        public String phnum = "unknown";
        public int error = -1;
        public int action = 1;
        public int callwait = 0;

        public CFInfo() {
        }
    }

    private void showAlertDialog(String str, String str2) {
        Log.d("SmartCallFwdFragment", "showAlertDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(str);
        builder.setMessage(str2);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { // from class: com.mediatek.settings.sim.SmartCallFwdFragment.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (SmartCallFwdFragment.this.mProgressDialog != null) {
                    Log.d("SmartCallFwdFragment", "Clear unwanted progress dialog");
                    SmartCallFwdFragment.this.mProgressDialog.dismiss();
                    SmartCallFwdFragment.this.mProgressDialog = null;
                }
            }
        });
        builder.setCancelable(false);
        this.mAlertDialog = builder.show();
    }

    private void showToast(String str) {
        Toast.makeText(this.mContext, str, 1).show();
    }

    private String getSimPrefValue(String str) {
        String string = "";
        try {
            string = this.mSharedPreferences.getString(str, "");
        } catch (ClassCastException e) {
            e.printStackTrace();
            this.mSharedPreferences.edit().remove(str).commit();
        }
        Log.d("SmartCallFwdFragment", "getSimPrefValue simPref = " + str + ", value = " + string);
        return string;
    }

    private void setSimPrefValue(String str, String str2) {
        Log.d("SmartCallFwdFragment", "setSimPrefValue: simPref = " + str + ", value = " + str2);
        SharedPreferences.Editor editorEdit = this.mSharedPreferences.edit();
        editorEdit.putString(str, str2);
        editorEdit.commit();
    }

    private void getPreviousPrefValue() {
        Log.d("SmartCallFwdFragment", "getPreviousPrefValues");
        this.simPrefValue[0] = getSimPrefValue("smart_sim1_pref");
        this.simPrefValue[1] = getSimPrefValue("smart_sim2_pref");
    }

    public String getLine1Number(int i) {
        int subIdUsingPhoneId = MtkSubscriptionManager.getSubIdUsingPhoneId(i);
        Log.d("SmartCallFwdFragment", "getLine1Number with simId " + i + " ,subId " + subIdUsingPhoneId);
        return this.mTelephonyManager.getLine1Number(subIdUsingPhoneId);
    }
}

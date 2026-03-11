package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.cdma.OmhEventHandler;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IRCSSettings;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimDialogActivity extends Activity {
    private Dialog mDialog;
    private ISettingsMiscExt mMiscExt;
    private IRCSSettings mRCSExt;
    private SimHotSwapHandler mSimHotSwapHandler;
    private ISimManagementExt mSimManagementExt;
    private static String TAG = "SimDialogActivity";
    public static String PREFERRED_SIM = "preferred_sim";
    public static String DIALOG_TYPE_KEY = "dialog_type";
    private int mDataSub = -1;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(SimDialogActivity.TAG, "onReceive, action = " + action);
            SimDialogActivity.this.dismissSimDialog();
            SimDialogActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        setSimStateCheck();
        this.mSimManagementExt = UtilsExt.getSimManagmentExtPlugin(getApplicationContext());
        this.mMiscExt = UtilsExt.getMiscPlugin(getApplicationContext());
        this.mRCSExt = UtilsExt.getRcsSettingsPlugin(getApplicationContext());
        int dialogType = extras.getInt(DIALOG_TYPE_KEY, -1);
        switch (dialogType) {
            case DefaultWfcSettingsExt.RESUME:
            case DefaultWfcSettingsExt.PAUSE:
            case DefaultWfcSettingsExt.CREATE:
                if (isFinishing()) {
                    Log.e(TAG, "Activity Finishing!");
                }
                this.mDialog = createDialog(this, dialogType);
                this.mDialog.show();
                return;
            case DefaultWfcSettingsExt.DESTROY:
                List<SubscriptionInfo> subs = SubscriptionManager.from(this).getActiveSubscriptionInfoList();
                if (subs == null || subs.size() != 1) {
                    Log.w(TAG, "Subscription count is not 1, skip preferred SIM dialog");
                    finish();
                    return;
                } else {
                    displayPreferredDialog(extras.getInt(PREFERRED_SIM));
                    return;
                }
            default:
                throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
        }
    }

    private void displayPreferredDialog(int slotId) {
        Resources res = getResources();
        final Context context = getApplicationContext();
        final SubscriptionInfo sir = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (sir != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            int subId = SubscriptionManager.getSubIdUsingPhoneId(slotId);
            String title = this.mMiscExt.customizeSimDisplayString(res.getString(R.string.sim_preferred_title), subId);
            String message = this.mMiscExt.customizeSimDisplayString(res.getString(R.string.sim_preferred_message, sir.getDisplayName()), subId);
            alertDialogBuilder.setTitle(title);
            alertDialogBuilder.setMessage(message);
            alertDialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    int subId2 = sir.getSubscriptionId();
                    PhoneAccountHandle phoneAccountHandle = SimDialogActivity.this.subscriptionIdToPhoneAccountHandle(subId2);
                    SimDialogActivity.this.setDefaultDataSubId(context, subId2);
                    SimDialogActivity.setDefaultSmsSubId(context, subId2);
                    SimDialogActivity.this.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
                    SimDialogActivity.this.dismissSimDialog();
                    SimDialogActivity.this.finish();
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    SimDialogActivity.this.dismissSimDialog();
                    SimDialogActivity.this.finish();
                }
            });
            alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    SimDialogActivity.this.finish();
                }
            });
            this.mDialog = alertDialogBuilder.create();
            this.mDialog.show();
            return;
        }
        finish();
    }

    public void setDefaultDataSubId(Context context, int subId) {
        Log.d(TAG, "setDefaultDataSubId, sub = " + subId);
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        this.mSimManagementExt.setDataState(subId);
        subscriptionManager.setDefaultDataSubId(subId);
        this.mSimManagementExt.setDataStateEnable(subId);
        this.mDataSub = subId;
    }

    public static void setDefaultSmsSubId(Context context, int subId) {
        Log.d(TAG, "setDefaultSmsSubId, sub = " + subId);
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        subscriptionManager.setDefaultSmsSubId(subId);
    }

    public void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccount) {
        Log.d(TAG, "setUserSelectedOutgoingPhoneAccount phoneAccount = " + phoneAccount);
        TelecomManager telecomManager = TelecomManager.from(this);
        telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccount);
    }

    public PhoneAccountHandle subscriptionIdToPhoneAccountHandle(int subId) {
        TelecomManager telecomManager = TelecomManager.from(this);
        TelephonyManager telephonyManager = TelephonyManager.from(this);
        Iterator<PhoneAccountHandle> phoneAccounts = telecomManager.getCallCapablePhoneAccounts().listIterator();
        while (phoneAccounts.hasNext()) {
            PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            if (subId == telephonyManager.getSubIdForPhoneAccount(phoneAccount)) {
                return phoneAccountHandle;
            }
        }
        return null;
    }

    public Dialog createDialog(final Context context, final int id) {
        ArrayList<String> list = new ArrayList<>();
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        final List<SubscriptionInfo> subInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        int selectableSubInfoLength = subInfoList == null ? 0 : subInfoList.size();
        DialogInterface.OnClickListener selectionListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (SimDialogActivity.this.mSimManagementExt.simDialogOnClick(id, i, context)) {
                    Log.d(SimDialogActivity.TAG, "finish() ,onclick handled by simDialogOnClick in Plugin");
                    SimDialogActivity.this.dismissSimDialog();
                    SimDialogActivity.this.finish();
                    return;
                }
                switch (id) {
                    case DefaultWfcSettingsExt.RESUME:
                        SubscriptionInfo subscriptionInfo = (SubscriptionInfo) subInfoList.get(i);
                        int iIntValue = (subscriptionInfo != null ? Integer.valueOf(subscriptionInfo.getSubscriptionId()) : null).intValue();
                        if (CdmaUtils.isCdmaCardCompetionForData(context)) {
                            int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
                            Log.d(SimDialogActivity.TAG, "currnt default Id is: " + defaultDataSubscriptionId + " ,target Id: " + iIntValue);
                            if (defaultDataSubscriptionId != iIntValue) {
                                if (TelecomManager.from(context).isInCall()) {
                                    Toast.makeText(context, R.string.default_data_switch_err_msg1, 0).show();
                                } else {
                                    CdmaUtils.startAlertCdmaDialog(context, iIntValue, id);
                                }
                            }
                        } else if (CdmaUtils.isNonOmhSimInOmhDevice(iIntValue)) {
                            OmhEventHandler.getInstance(context).obtainMessage(101, 1001, iIntValue).sendToTarget();
                        } else {
                            SimDialogActivity.this.setDefaultDataSubId(context, iIntValue);
                        }
                        break;
                    case DefaultWfcSettingsExt.PAUSE:
                        List<PhoneAccountHandle> callCapablePhoneAccounts = TelecomManager.from(context).getCallCapablePhoneAccounts();
                        Log.d(SimDialogActivity.TAG, "phoneAccountsList = " + callCapablePhoneAccounts.toString());
                        if (SystemProperties.get("ro.cmcc_light_cust_support").equals("1")) {
                            Log.d(SimDialogActivity.TAG, "CMCC OM project value = " + i);
                            TelephonyManager.MultiSimVariants multiSimConfiguration = TelephonyManager.from(context).getMultiSimConfiguration();
                            if (i == 0 && SubscriptionManager.from(context).getActiveSubscriptionInfoCount() == 1 && (multiSimConfiguration == TelephonyManager.MultiSimVariants.DSDS || multiSimConfiguration == TelephonyManager.MultiSimVariants.DSDA)) {
                                i = 1;
                            }
                        }
                        if (i > callCapablePhoneAccounts.size()) {
                            Log.w(SimDialogActivity.TAG, "phone account changed, do noting! value = " + i + ", phone account size = " + callCapablePhoneAccounts.size());
                        } else {
                            Log.d(SimDialogActivity.TAG, "value = " + i);
                            SimDialogActivity.this.setUserSelectedOutgoingPhoneAccount(i >= 1 ? callCapablePhoneAccounts.get(i - 1) : null);
                        }
                        break;
                    case DefaultWfcSettingsExt.CREATE:
                        SimDialogActivity.setDefaultSmsSubId(context, SimDialogActivity.this.getPickSmsDefaultSub(subInfoList, i));
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
                }
                SimDialogActivity.this.dismissSimDialog();
                SimDialogActivity.this.finish();
            }
        };
        DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                if (keyCode == 4) {
                    SimDialogActivity.this.finish();
                    return true;
                }
                return true;
            }
        };
        ArrayList<SubscriptionInfo> callsSubInfoList = new ArrayList<>();
        ArrayList<SubscriptionInfo> smsSubInfoList = new ArrayList<>();
        if (id == 1) {
            TelecomManager telecomManager = TelecomManager.from(context);
            TelephonyManager telephonyManager = TelephonyManager.from(context);
            Iterator<PhoneAccountHandle> phoneAccounts = telecomManager.getCallCapablePhoneAccounts().listIterator();
            this.mSimManagementExt.updateList(list, callsSubInfoList, telecomManager.getCallCapablePhoneAccounts().size());
            int accountSize = telecomManager.getCallCapablePhoneAccounts().size();
            Log.d(TAG, "phoneAccounts size = " + accountSize);
            if (accountSize > 1) {
                list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
                callsSubInfoList.add(null);
            }
            while (phoneAccounts.hasNext()) {
                PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccounts.next());
                if (phoneAccount == null) {
                    Log.d(TAG, "phoneAccount is null");
                } else {
                    list.add((String) phoneAccount.getLabel());
                    int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
                    Log.d(TAG, "phoneAccount label = " + phoneAccount.getLabel() + ", subId = " + subId);
                    if (subId != -1) {
                        SubscriptionInfo sir = SubscriptionManager.from(context).getActiveSubscriptionInfo(subId);
                        callsSubInfoList.add(sir);
                    } else {
                        callsSubInfoList.add(null);
                    }
                }
            }
            Log.d(TAG, "callsSubInfoList = " + callsSubInfoList + ", list = " + list);
            this.mSimManagementExt.customizeListArray(list);
            this.mSimManagementExt.customizeSubscriptionInfoArray(callsSubInfoList);
        } else if (id == 2) {
            setupSmsSubInfoList(list, subInfoList, selectableSubInfoLength, smsSubInfoList);
        } else {
            for (int i = 0; i < selectableSubInfoLength; i++) {
                SubscriptionInfo sir2 = subInfoList.get(i);
                CharSequence displayName = sir2.getDisplayName();
                if (displayName == null) {
                    displayName = "";
                }
                list.add(displayName.toString());
            }
        }
        String[] arr = (String[]) list.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ListAdapter adapter = new SelectAccountListAdapter(getAdapterData(id, subInfoList, callsSubInfoList, smsSubInfoList), builder.getContext(), R.layout.select_account_list_item, arr, id);
        switch (id) {
            case DefaultWfcSettingsExt.RESUME:
                builder.setTitle(R.string.select_sim_for_data);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                builder.setTitle(R.string.select_sim_for_calls);
                break;
            case DefaultWfcSettingsExt.CREATE:
                builder.setTitle(R.string.sim_card_select_title);
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
        }
        changeDialogTitle(builder, id);
        Dialog dialog = builder.setAdapter(adapter, selectionListener).create();
        dialog.setOnKeyListener(keyListener);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                SimDialogActivity.this.finish();
            }
        });
        return dialog;
    }

    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private final float OPACITY;
        private Context mContext;
        private int mDialogId;
        private int mResId;
        private List<SubscriptionInfo> mSubInfoList;

        public SelectAccountListAdapter(List<SubscriptionInfo> subInfoList, Context context, int resource, String[] arr, int dialogId) {
            super(context, resource, arr);
            this.OPACITY = 0.54f;
            this.mContext = context;
            this.mResId = resource;
            this.mDialogId = dialogId;
            this.mSubInfoList = subInfoList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView;
            ViewHolder holder;
            ViewHolder viewHolder = null;
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            if (convertView == null) {
                rowView = inflater.inflate(this.mResId, (ViewGroup) null);
                holder = new ViewHolder(this, viewHolder);
                holder.title = (TextView) rowView.findViewById(R.id.title);
                holder.summary = (TextView) rowView.findViewById(R.id.summary);
                holder.icon = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) convertView.getTag();
            }
            SubscriptionInfo sir = this.mSubInfoList.get(position);
            if (sir == null) {
                holder.title.setText(getItem(position));
                holder.summary.setText("");
                if (this.mDialogId == 1) {
                    setPhoneAccountIcon(holder, position);
                } else {
                    holder.icon.setImageDrawable(SimDialogActivity.this.getResources().getDrawable(R.drawable.ic_live_help));
                }
                SimDialogActivity.this.mSimManagementExt.setSmsAutoItemIcon(holder.icon, this.mDialogId, position);
                SimDialogActivity.this.mSimManagementExt.setCurrNetworkIcon(holder.icon, this.mDialogId, position);
                holder.icon.setAlpha(0.54f);
            } else {
                holder.title.setText(sir.getDisplayName());
                holder.summary.setText(sir.getNumber());
                holder.icon.setImageBitmap(sir.createIconBitmap(this.mContext));
                holder.icon.setAlpha(1.0f);
            }
            return rowView;
        }

        private class ViewHolder {
            ImageView icon;
            TextView summary;
            TextView title;

            ViewHolder(SelectAccountListAdapter this$1, ViewHolder viewHolder) {
                this();
            }

            private ViewHolder() {
            }
        }

        private void setPhoneAccountIcon(ViewHolder holder, int location) {
            Log.d(SimDialogActivity.TAG, "setSipAccountBitmap()... location: " + location);
            String askFirst = SimDialogActivity.this.getResources().getString(R.string.sim_calls_ask_first_prefs_title);
            String lableString = getItem(location);
            TelecomManager telecomManager = TelecomManager.from(this.mContext);
            List<PhoneAccountHandle> phoneAccountHandles = telecomManager.getCallCapablePhoneAccounts();
            if (!askFirst.equals(lableString)) {
                if (phoneAccountHandles.size() > 1) {
                    location--;
                }
                PhoneAccount phoneAccount = null;
                if (location >= 0 && location < phoneAccountHandles.size()) {
                    phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandles.get(location));
                }
                Log.d(SimDialogActivity.TAG, "setSipAccountBitmap()... position: " + location + " account: " + phoneAccount);
                if (phoneAccount == null) {
                    return;
                }
                holder.icon.setImageDrawable(phoneAccount.getIcon().loadDrawable(this.mContext));
                return;
            }
            holder.icon.setImageDrawable(SimDialogActivity.this.getResources().getDrawable(R.drawable.ic_live_help));
        }
    }

    private void setSimStateCheck() {
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d(SimDialogActivity.TAG, "onSimHotSwap, finish Activity");
                SimDialogActivity.this.dismissSimDialog();
                SimDialogActivity.this.finish();
            }
        });
        IntentFilter itentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mReceiver, itentFilter);
    }

    private void unsetSimStateCheck() {
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        unregisterReceiver(this.mReceiver);
    }

    @Override
    protected void onPause() {
        OmhEventHandler.getInstance(this).sendEmptyMessage(102);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unsetSimStateCheck();
        dismissSimDialog();
        if (this.mDataSub != -1) {
            Toast.makeText(this, this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.data_switch_started), this.mDataSub), 1).show();
        }
        super.onDestroy();
    }

    public int getPickSmsDefaultSub(List<SubscriptionInfo> subInfoList, int value) {
        int subId = -1;
        if (value < 1) {
            int length = subInfoList == null ? 0 : subInfoList.size();
            subId = length == 1 ? subInfoList.get(value).getSubscriptionId() : -2;
        } else if (value >= 1 && value < subInfoList.size() + 1) {
            subId = subInfoList.get(value - 1).getSubscriptionId();
        }
        int subId2 = this.mRCSExt.getDefaultSmsClickContentExt(subInfoList, value, subId);
        Log.d(TAG, "getPickSmsDefaultSub, value: " + value + ", subId: " + subId2);
        return subId2;
    }

    private void setupSmsSubInfoList(ArrayList<String> list, List<SubscriptionInfo> subInfoList, int selectableSubInfoLength, ArrayList<SubscriptionInfo> smsSubInfoList) {
        this.mSimManagementExt.updateList(list, smsSubInfoList, selectableSubInfoLength);
        if (selectableSubInfoLength > 1 && this.mRCSExt.isNeedAskFirstItemForSms()) {
            list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
            smsSubInfoList.add(null);
        }
        for (int i = 0; i < selectableSubInfoLength; i++) {
            SubscriptionInfo sir = subInfoList.get(i);
            smsSubInfoList.add(sir);
            CharSequence displayName = sir.getDisplayName();
            if (displayName == null) {
                displayName = "";
            }
            list.add(displayName.toString());
        }
        this.mSimManagementExt.customizeListArray(list);
        this.mSimManagementExt.customizeSubscriptionInfoArray(smsSubInfoList);
        this.mSimManagementExt.initAutoItemForSms(list, smsSubInfoList);
    }

    private List<SubscriptionInfo> getAdapterData(int id, List<SubscriptionInfo> subInfoList, ArrayList<SubscriptionInfo> callsSubInfoList, ArrayList<SubscriptionInfo> smsSubInfoList) {
        switch (id) {
            case DefaultWfcSettingsExt.RESUME:
                return subInfoList;
            case DefaultWfcSettingsExt.PAUSE:
                return callsSubInfoList;
            case DefaultWfcSettingsExt.CREATE:
                return smsSubInfoList;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
        }
    }

    private void changeDialogTitle(AlertDialog.Builder builder, int id) {
        switch (id) {
            case DefaultWfcSettingsExt.RESUME:
                builder.setTitle(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.select_sim_for_data), -1));
                return;
            case DefaultWfcSettingsExt.PAUSE:
                builder.setTitle(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.select_sim_for_calls), -1));
                return;
            case DefaultWfcSettingsExt.CREATE:
                builder.setTitle(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.sim_card_select_title), -1));
                return;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
        }
    }

    public void dismissSimDialog() {
        if (this.mDialog == null || !this.mDialog.isShowing()) {
            return;
        }
        this.mDialog.dismiss();
        this.mDialog = null;
    }
}

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
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class SimDialogActivity extends Activity {
    private static ISimManagementExt mSimManagementExt;
    private Dialog mDialog;
    private SimHotSwapHandler mSimHotSwapHandler;
    private static String TAG = "SimDialogActivity";
    public static String PREFERRED_SIM = "preferred_sim";
    public static String DIALOG_TYPE_KEY = "dialog_type";
    private SimHotSwapHandler.OnSimHotSwapListener mSimHotSwapListener = new SimHotSwapHandler.OnSimHotSwapListener() {
        @Override
        public void onSimHotSwap() {
            Log.d(SimDialogActivity.TAG, "onSimHotSwap, finish Activity");
            SimDialogActivity.this.dismissSimDialog();
            SimDialogActivity.this.finish();
        }
    };
    private int mNewDataSubId = -1;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(SimDialogActivity.TAG, "onReceive, action=" + action);
            SimDialogActivity.this.dismissSimDialog();
            SimDialogActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setSimStateCheck();
        mSimManagementExt = UtilsExt.getSimManagementExt(getApplicationContext());
        int intExtra = getIntent().getIntExtra(DIALOG_TYPE_KEY, -1);
        Log.d(TAG, "onCreate, dialogType=" + intExtra);
        showSimDialog(intExtra);
    }

    private void showSimDialog(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
                if (isFinishing()) {
                    Log.e(TAG, "Activity is finishing.");
                }
                this.mDialog = createDialog(this, i);
                this.mDialog.show();
                Log.d(TAG, "show selection dialog=" + this.mDialog);
                return;
            case 3:
                List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(this).getActiveSubscriptionInfoList();
                if (activeSubscriptionInfoList == null || activeSubscriptionInfoList.size() != 1) {
                    Log.w(TAG, "Subscription count is not 1, skip preferred SIM dialog");
                    finish();
                    return;
                } else {
                    displayPreferredDialog(getIntent().getIntExtra(PREFERRED_SIM, 0));
                    return;
                }
            default:
                throw new IllegalArgumentException("Invalid dialog type " + i + " sent.");
        }
    }

    private void displayPreferredDialog(int i) {
        Resources resources = getResources();
        final Context applicationContext = getApplicationContext();
        final SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = SubscriptionManager.from(applicationContext).getActiveSubscriptionInfoForSimSlotIndex(i);
        if (activeSubscriptionInfoForSimSlotIndex != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.sim_preferred_title);
            builder.setMessage(resources.getString(R.string.sim_preferred_message, activeSubscriptionInfoForSimSlotIndex.getDisplayName()));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    int subscriptionId = activeSubscriptionInfoForSimSlotIndex.getSubscriptionId();
                    PhoneAccountHandle phoneAccountHandleSubscriptionIdToPhoneAccountHandle = SimDialogActivity.this.subscriptionIdToPhoneAccountHandle(subscriptionId);
                    SimDialogActivity.this.setDefaultDataSubId(applicationContext, subscriptionId);
                    SimDialogActivity.setDefaultSmsSubId(applicationContext, subscriptionId);
                    SimDialogActivity.this.setUserSelectedOutgoingPhoneAccount(phoneAccountHandleSubscriptionIdToPhoneAccountHandle);
                    SimDialogActivity.this.dismissSimDialog();
                    SimDialogActivity.this.finish();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    SimDialogActivity.this.dismissSimDialog();
                    SimDialogActivity.this.finish();
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    SimDialogActivity.this.finish();
                }
            });
            this.mDialog = builder.create();
            this.mDialog.show();
            Log.d(TAG, "show preferred dialog=" + this.mDialog);
            return;
        }
        finish();
    }

    private void setDefaultDataSubId(Context context, int i) {
        Log.d(TAG, "setDefaultDataSubId, sub=" + i);
        SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(context);
        mSimManagementExt.setDataState(i);
        subscriptionManagerFrom.setDefaultDataSubId(i);
        mSimManagementExt.setDataStateEnable(i);
        this.mNewDataSubId = i;
    }

    private static void setDefaultSmsSubId(Context context, int i) {
        Log.d(TAG, "setDefaultSmsSubId, sub=" + i);
        SubscriptionManager.from(context).setDefaultSmsSubId(i);
    }

    private void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        Log.d(TAG, "setUserSelectedOutgoingPhoneAccount, phoneAccount=" + phoneAccountHandle);
        TelecomManager.from(this).setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(int i) {
        TelecomManager telecomManagerFrom = TelecomManager.from(this);
        TelephonyManager telephonyManagerFrom = TelephonyManager.from(this);
        ListIterator<PhoneAccountHandle> listIterator = telecomManagerFrom.getCallCapablePhoneAccounts().listIterator();
        Log.d(TAG, "Match phone account, subId=" + i + ", phone account list exist=" + listIterator.hasNext());
        while (listIterator.hasNext()) {
            PhoneAccountHandle next = listIterator.next();
            PhoneAccount phoneAccount = telecomManagerFrom.getPhoneAccount(next);
            int subIdForPhoneAccount = telephonyManagerFrom.getSubIdForPhoneAccount(phoneAccount);
            Log.d(TAG, "Match phone account, phoneAccountSubId=" + subIdForPhoneAccount + ", phoneAccount=" + phoneAccount);
            if (i == subIdForPhoneAccount) {
                return next;
            }
        }
        return null;
    }

    public Dialog createDialog(final Context context, final int i) {
        int size;
        ArrayList<String> arrayList = new ArrayList<>();
        final List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            size = activeSubscriptionInfoList.size();
        } else {
            size = 0;
        }
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                int subscriptionId;
                if (SimDialogActivity.mSimManagementExt.simDialogOnClick(i, i2, context)) {
                    Log.d(SimDialogActivity.TAG, "Handle the click event in plugin.");
                    SimDialogActivity.this.dismissSimDialog();
                    SimDialogActivity.this.finish();
                    return;
                }
                switch (i) {
                    case 0:
                        SubscriptionInfo subscriptionInfo = (SubscriptionInfo) activeSubscriptionInfoList.get(i2);
                        if (subscriptionInfo == null) {
                            subscriptionId = -1;
                        } else {
                            subscriptionId = subscriptionInfo.getSubscriptionId();
                        }
                        if (!CdmaUtils.isCdmaCardCompetionForData(context)) {
                            SimDialogActivity.this.setDefaultDataSubId(context, subscriptionId);
                        } else {
                            int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
                            Log.d(SimDialogActivity.TAG, "currnt default subId=" + defaultDataSubscriptionId + ", targetId=" + subscriptionId);
                            if (defaultDataSubscriptionId != subscriptionId) {
                                if (!TelecomManager.from(context).isInCall()) {
                                    SimDialogActivity.this.setDefaultDataSubId(context, subscriptionId);
                                } else {
                                    Toast.makeText(context, R.string.default_data_switch_err_msg1, 0).show();
                                }
                            }
                        }
                        break;
                    case 1:
                        List<PhoneAccountHandle> callCapablePhoneAccounts = TelecomManager.from(context).getCallCapablePhoneAccounts();
                        Log.d(SimDialogActivity.TAG, "value=" + i2 + ", phoneAccountsList=" + callCapablePhoneAccounts);
                        if (i2 <= callCapablePhoneAccounts.size()) {
                            SimDialogActivity.this.setUserSelectedOutgoingPhoneAccount(i2 < 1 ? null : callCapablePhoneAccounts.get(i2 - 1));
                        } else {
                            Log.w(SimDialogActivity.TAG, "phone account changed, do noting. value=" + i2 + ", phone account size=" + callCapablePhoneAccounts.size());
                        }
                        break;
                    case 2:
                        SimDialogActivity.setDefaultSmsSubId(context, SimDialogActivity.this.getPickSmsDefaultSub(activeSubscriptionInfoList, i2));
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid dialog type " + i + " in SIM dialog.");
                }
                SimDialogActivity.this.dismissSimDialog();
                SimDialogActivity.this.finish();
            }
        };
        DialogInterface.OnKeyListener onKeyListener = new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i2, KeyEvent keyEvent) {
                if (i2 == 4) {
                    SimDialogActivity.this.finish();
                    return true;
                }
                return true;
            }
        };
        ArrayList<SubscriptionInfo> arrayList2 = new ArrayList<>();
        ArrayList<SubscriptionInfo> arrayList3 = new ArrayList<>();
        if (i != 1) {
            if (i == 2) {
                setupSmsSubInfoList(arrayList, activeSubscriptionInfoList, size, arrayList3);
            } else {
                for (int i2 = 0; i2 < size; i2++) {
                    CharSequence displayName = activeSubscriptionInfoList.get(i2).getDisplayName();
                    if (displayName == null) {
                        displayName = "";
                    }
                    arrayList.add(displayName.toString());
                }
            }
        } else {
            TelecomManager telecomManagerFrom = TelecomManager.from(context);
            TelephonyManager telephonyManagerFrom = TelephonyManager.from(context);
            ListIterator<PhoneAccountHandle> listIterator = telecomManagerFrom.getCallCapablePhoneAccounts().listIterator();
            int size2 = telecomManagerFrom.getCallCapablePhoneAccounts().size();
            mSimManagementExt.updateList(arrayList, arrayList2, size2);
            Log.d(TAG, "phone account size=" + size2);
            if (size2 > 1) {
                arrayList.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
                arrayList2.add(null);
            }
            while (listIterator.hasNext()) {
                PhoneAccount phoneAccount = telecomManagerFrom.getPhoneAccount(listIterator.next());
                if (phoneAccount == null) {
                    Log.d(TAG, "phoneAccount is null");
                } else {
                    arrayList.add((String) phoneAccount.getLabel());
                    int subIdForPhoneAccount = telephonyManagerFrom.getSubIdForPhoneAccount(phoneAccount);
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    TelecomManager telecomManager = telecomManagerFrom;
                    sb.append("phoneAccount label=");
                    sb.append((Object) phoneAccount.getLabel());
                    sb.append(", subId=");
                    sb.append(subIdForPhoneAccount);
                    Log.d(str, sb.toString());
                    if (subIdForPhoneAccount != -1) {
                        arrayList2.add(SubscriptionManager.from(context).getActiveSubscriptionInfo(subIdForPhoneAccount));
                    } else {
                        arrayList2.add(null);
                    }
                    telecomManagerFrom = telecomManager;
                }
            }
            Log.d(TAG, "callsSubInfoList=" + arrayList2 + ", list=" + arrayList);
        }
        String[] strArr = (String[]) arrayList.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        SelectAccountListAdapter selectAccountListAdapter = new SelectAccountListAdapter(getAdapterData(i, activeSubscriptionInfoList, arrayList2, arrayList3), builder.getContext(), R.layout.select_account_list_item, strArr, i);
        switch (i) {
            case 0:
                builder.setTitle(R.string.select_sim_for_data);
                break;
            case 1:
                builder.setTitle(R.string.select_sim_for_calls);
                break;
            case 2:
                builder.setTitle(R.string.sim_card_select_title);
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + i + " in SIM dialog.");
        }
        AlertDialog alertDialogCreate = builder.setAdapter(selectAccountListAdapter, onClickListener).create();
        alertDialogCreate.setOnKeyListener(onKeyListener);
        alertDialogCreate.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                SimDialogActivity.this.finish();
            }
        });
        return alertDialogCreate;
    }

    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private final float OPACITY;
        private Context mContext;
        private int mDialogId;
        private int mResId;
        private List<SubscriptionInfo> mSubInfoList;

        public SelectAccountListAdapter(List<SubscriptionInfo> list, Context context, int i, String[] strArr, int i2) {
            super(context, i, strArr);
            this.OPACITY = 0.54f;
            this.mContext = context;
            this.mResId = i;
            this.mDialogId = i2;
            this.mSubInfoList = list;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            LayoutInflater layoutInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            if (view == null) {
                view = layoutInflater.inflate(this.mResId, (ViewGroup) null);
                viewHolder = new ViewHolder();
                viewHolder.title = (TextView) view.findViewById(R.id.title);
                viewHolder.summary = (TextView) view.findViewById(R.id.summary);
                viewHolder.icon = (ImageView) view.findViewById(R.id.icon);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            SubscriptionInfo subscriptionInfo = this.mSubInfoList.get(i);
            if (subscriptionInfo == null) {
                viewHolder.title.setText(getItem(i));
                viewHolder.summary.setText("");
                if (this.mDialogId == 1) {
                    setPhoneAccountIcon(viewHolder, i);
                } else {
                    viewHolder.icon.setImageDrawable(SimDialogActivity.this.getResources().getDrawable(R.drawable.ic_live_help));
                }
                SimDialogActivity.mSimManagementExt.setSmsAutoItemIcon(viewHolder.icon, this.mDialogId, i);
                SimDialogActivity.mSimManagementExt.setCurrNetworkIcon(viewHolder.icon, this.mDialogId, i);
                viewHolder.icon.setAlpha(0.54f);
            } else {
                viewHolder.title.setText(subscriptionInfo.getDisplayName());
                viewHolder.summary.setText(subscriptionInfo.getNumber());
                viewHolder.icon.setImageBitmap(subscriptionInfo.createIconBitmap(this.mContext));
                viewHolder.icon.setAlpha(1.0f);
            }
            return view;
        }

        private class ViewHolder {
            ImageView icon;
            TextView summary;
            TextView title;

            private ViewHolder() {
            }
        }

        private void setPhoneAccountIcon(ViewHolder viewHolder, int i) {
            Log.d(SimDialogActivity.TAG, "setPhoneAccountIcon, location=" + i);
            String string = SimDialogActivity.this.getResources().getString(R.string.sim_calls_ask_first_prefs_title);
            String item = getItem(i);
            TelecomManager telecomManagerFrom = TelecomManager.from(this.mContext);
            List<PhoneAccountHandle> callCapablePhoneAccounts = telecomManagerFrom.getCallCapablePhoneAccounts();
            if (!string.equals(item)) {
                if (callCapablePhoneAccounts.size() > 1) {
                    i--;
                }
                PhoneAccount phoneAccount = null;
                if (i >= 0 && i < callCapablePhoneAccounts.size()) {
                    phoneAccount = telecomManagerFrom.getPhoneAccount(callCapablePhoneAccounts.get(i));
                }
                Log.d(SimDialogActivity.TAG, "setPhoneAccountIcon(), location=" + i + ", account=" + phoneAccount);
                if (phoneAccount != null && phoneAccount.getIcon() != null) {
                    viewHolder.icon.setImageDrawable(phoneAccount.getIcon().loadDrawable(this.mContext));
                    return;
                }
                return;
            }
            viewHolder.icon.setImageDrawable(SimDialogActivity.this.getResources().getDrawable(R.drawable.ic_live_help));
        }
    }

    private void setSimStateCheck() {
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(this.mSimHotSwapListener);
        registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
    }

    private void unsetSimStateCheck() {
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        this.mSimHotSwapHandler = null;
        unregisterReceiver(this.mReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        int intExtra = intent.getIntExtra(DIALOG_TYPE_KEY, -1);
        Log.d(TAG, "onNewIntent, dialogType=" + intExtra);
        setIntent(intent);
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.setOnCancelListener(null);
            this.mDialog.setOnDismissListener(null);
            this.mDialog.dismiss();
            this.mDialog = null;
            this.mSimHotSwapHandler.unregisterOnSimHotSwap();
            this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
            this.mSimHotSwapHandler.registerOnSimHotSwap(this.mSimHotSwapListener);
            showSimDialog(intExtra);
            Log.d(TAG, "onNewIntent, recreate the SIM dialog.");
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        unsetSimStateCheck();
        dismissSimDialog();
        if (this.mNewDataSubId != -1) {
            Toast.makeText(this, R.string.data_switch_started, 1).show();
        }
        super.onDestroy();
    }

    private int getPickSmsDefaultSub(List<SubscriptionInfo> list, int i) {
        int subscriptionId;
        if (i >= 1) {
            if (i >= 1 && i < list.size() + 1) {
                subscriptionId = list.get(i - 1).getSubscriptionId();
            } else {
                subscriptionId = -1;
            }
        } else {
            if ((list == null ? 0 : list.size()) == 1) {
                subscriptionId = list.get(i).getSubscriptionId();
            } else {
                subscriptionId = -2;
            }
        }
        int defaultSmsClickContentExt = mSimManagementExt.getDefaultSmsClickContentExt(list, i, subscriptionId);
        Log.d(TAG, "getPickSmsDefaultSub, value=" + i + ", subId=" + defaultSmsClickContentExt);
        return defaultSmsClickContentExt;
    }

    private void setupSmsSubInfoList(ArrayList<String> arrayList, List<SubscriptionInfo> list, int i, ArrayList<SubscriptionInfo> arrayList2) {
        mSimManagementExt.updateList(arrayList, arrayList2, i);
        if (i > 1 && mSimManagementExt.isNeedAskFirstItemForSms()) {
            arrayList.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
            arrayList2.add(null);
        }
        for (int i2 = 0; i2 < i; i2++) {
            SubscriptionInfo subscriptionInfo = list.get(i2);
            arrayList2.add(subscriptionInfo);
            CharSequence displayName = subscriptionInfo.getDisplayName();
            if (displayName == null) {
                displayName = "";
            }
            arrayList.add(displayName.toString());
        }
        mSimManagementExt.initAutoItemForSms(arrayList, arrayList2);
    }

    private List<SubscriptionInfo> getAdapterData(int i, List<SubscriptionInfo> list, ArrayList<SubscriptionInfo> arrayList, ArrayList<SubscriptionInfo> arrayList2) {
        switch (i) {
            case 0:
                return list;
            case 1:
                return arrayList;
            case 2:
                return arrayList2;
            default:
                Log.e(TAG, "Invalid dialog type=" + i);
                return null;
        }
    }

    private void dismissSimDialog() {
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
    }
}

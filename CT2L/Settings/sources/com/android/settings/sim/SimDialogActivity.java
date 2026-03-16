package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
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
import com.android.settings.Utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimDialogActivity extends Activity {
    private static String TAG = "SimDialogActivity";
    public static String PREFERRED_SIM = "preferred_sim";
    public static String DIALOG_TYPE_KEY = "dialog_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        int dialogType = extras.getInt(DIALOG_TYPE_KEY, -1);
        switch (dialogType) {
            case 0:
            case 1:
            case 2:
            case 10:
                createDialog(this, dialogType).show();
                return;
            case 3:
                displayPreferredDialog(extras.getInt(PREFERRED_SIM));
                return;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            default:
                throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
        }
    }

    private void displayPreferredDialog(int slotId) {
        Resources res = getResources();
        final Context context = getApplicationContext();
        final SubscriptionInfo sir = Utils.findRecordBySlotId(context, slotId);
        if (sir != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.sim_preferred_title);
            alertDialogBuilder.setMessage(res.getString(R.string.sim_preferred_message, sir.getDisplayName()));
            alertDialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    int subId = sir.getSubscriptionId();
                    PhoneAccountHandle phoneAccountHandle = SimDialogActivity.this.subscriptionIdToPhoneAccountHandle(subId);
                    SimDialogActivity.setDefaultDataSubId(context, subId);
                    SimDialogActivity.setDefaultSmsSubId(context, subId);
                    SimDialogActivity.this.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
                    SimDialogActivity.this.finish();
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    SimDialogActivity.this.finish();
                }
            });
            alertDialogBuilder.create().show();
            return;
        }
        finish();
    }

    private static void setDefaultDataSubId(Context context, int subId) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        subscriptionManager.setDefaultDataSubId(subId);
        Toast.makeText(context, R.string.data_switch_started, 1).show();
    }

    private static void setDefaultSmsSubId(Context context, int subId) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        subscriptionManager.setDefaultSmsSubId(subId);
    }

    private void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccount) {
        TelecomManager telecomManager = TelecomManager.from(this);
        telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccount);
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(int subId) {
        TelecomManager telecomManager = TelecomManager.from(this);
        Iterator<PhoneAccountHandle> phoneAccounts = telecomManager.getCallCapablePhoneAccounts().listIterator();
        while (phoneAccounts.hasNext()) {
            PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            String phoneAccountId = phoneAccountHandle.getId();
            if (phoneAccount.hasCapabilities(4) && TextUtils.isDigitsOnly(phoneAccountId) && Integer.parseInt(phoneAccountId) == subId) {
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
            public void onClick(DialogInterface dialog, int value) {
                switch (id) {
                    case 0:
                    case 10:
                        SubscriptionInfo sir = (SubscriptionInfo) subInfoList.get(value);
                        SimDialogActivity.setDefaultDataSubId(context, sir.getSubscriptionId());
                        break;
                    case 1:
                        TelecomManager telecomManager = TelecomManager.from(context);
                        List<PhoneAccountHandle> phoneAccountsList = telecomManager.getCallCapablePhoneAccounts();
                        SimDialogActivity.this.setUserSelectedOutgoingPhoneAccount(value < 1 ? null : phoneAccountsList.get(value - 1));
                        break;
                    case 2:
                        SubscriptionInfo sir2 = (SubscriptionInfo) subInfoList.get(value);
                        SimDialogActivity.setDefaultSmsSubId(context, sir2.getSubscriptionId());
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
                }
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
        if (id == 1) {
            TelecomManager telecomManager = TelecomManager.from(context);
            Iterator<PhoneAccountHandle> phoneAccounts = telecomManager.getCallCapablePhoneAccounts().listIterator();
            list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
            callsSubInfoList.add(null);
            while (phoneAccounts.hasNext()) {
                PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccounts.next());
                list.add((String) phoneAccount.getLabel());
                if (phoneAccount.hasCapabilities(4) && TextUtils.isDigitsOnly(phoneAccount.getAccountHandle().getId())) {
                    String phoneAccountId = phoneAccount.getAccountHandle().getId();
                    SubscriptionInfo sir = Utils.findRecordBySubId(context, Integer.parseInt(phoneAccountId));
                    callsSubInfoList.add(sir);
                } else {
                    callsSubInfoList.add(null);
                }
            }
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
        ListAdapter adapter = new SelectAccountListAdapter(id == 1 ? callsSubInfoList : subInfoList, builder.getContext(), R.layout.select_account_list_item, arr, id);
        switch (id) {
            case 0:
                builder.setTitle(R.string.select_sim_for_data);
                break;
            case 1:
                builder.setTitle(R.string.select_sim_for_calls);
                break;
            case 2:
                builder.setTitle(R.string.sim_card_select_title);
                break;
            case 10:
                builder.setTitle(R.string.select_sim_for_master);
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
        }
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
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            if (convertView == null) {
                rowView = inflater.inflate(this.mResId, (ViewGroup) null);
                holder = new ViewHolder();
                holder.title = (TextView) rowView.findViewById(R.id.title);
                holder.summary = (TextView) rowView.findViewById(R.id.summary);
                holder.icon = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }
            SubscriptionInfo sir = this.mSubInfoList.get(position);
            if (sir == null) {
                holder.title.setText(getItem(position));
                holder.summary.setText("");
                holder.icon.setImageDrawable(SimDialogActivity.this.getResources().getDrawable(R.drawable.ic_live_help));
                holder.icon.setAlpha(0.54f);
            } else {
                holder.title.setText(sir.getDisplayName());
                holder.summary.setText(sir.getNumber());
                holder.icon.setImageBitmap(sir.createIconBitmap(this.mContext));
            }
            return rowView;
        }

        private class ViewHolder {
            ImageView icon;
            TextView summary;
            TextView title;

            private ViewHolder() {
            }
        }
    }
}

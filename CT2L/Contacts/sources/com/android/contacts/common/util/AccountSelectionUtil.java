package com.android.contacts.common.util;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.vcard.ImportVCardActivity;
import java.util.List;

public class AccountSelectionUtil {
    public static Uri mPath;
    public static boolean mVCardShare = false;

    public static class AccountSelectedListener implements DialogInterface.OnClickListener {
        protected final List<AccountWithDataSet> mAccountList;
        private final Activity mActivity;
        private final int mResId;
        private final int mSubscriptionId;

        public AccountSelectedListener(Activity activity, List<AccountWithDataSet> accountList, int resId, int subscriptionId) {
            if (accountList == null || accountList.size() == 0) {
                Log.e("AccountSelectionUtil", "The size of Account list is 0.");
            }
            this.mActivity = activity;
            this.mAccountList = accountList;
            this.mResId = resId;
            this.mSubscriptionId = subscriptionId;
        }

        public AccountSelectedListener(Activity activity, List<AccountWithDataSet> accountList, int resId) {
            this(activity, accountList, resId, -1);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            AccountSelectionUtil.doImport(this.mActivity, this.mResId, this.mAccountList.get(which), this.mSubscriptionId);
        }
    }

    public static Dialog getSelectAccountDialog(Activity activity, int resId, DialogInterface.OnClickListener onClickListener, DialogInterface.OnCancelListener onCancelListener) {
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(activity);
        List<AccountWithDataSet> writableAccountList = accountTypes.getAccounts(true);
        Log.i("AccountSelectionUtil", "The number of available accounts: " + writableAccountList.size());
        Context dialogContext = new ContextThemeWrapper(activity, R.style.Theme.Light);
        final LayoutInflater dialogInflater = (LayoutInflater) dialogContext.getSystemService("layout_inflater");
        ArrayAdapter<AccountWithDataSet> accountAdapter = new ArrayAdapter<AccountWithDataSet>(activity, R.layout.simple_list_item_2, writableAccountList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = dialogInflater.inflate(R.layout.simple_list_item_2, parent, false);
                }
                TextView text1 = (TextView) convertView.findViewById(R.id.text1);
                TextView text2 = (TextView) convertView.findViewById(R.id.text2);
                AccountWithDataSet account = getItem(position);
                AccountType accountType = accountTypes.getAccountType(account.type, account.dataSet);
                Context context = getContext();
                text1.setText(account.name);
                text2.setText(accountType.getDisplayLabel(context));
                return convertView;
            }
        };
        if (onClickListener == null) {
            AccountSelectedListener accountSelectedListener = new AccountSelectedListener(activity, writableAccountList, resId);
            onClickListener = accountSelectedListener;
        }
        if (onCancelListener == null) {
            onCancelListener = new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                }
            };
        }
        return new AlertDialog.Builder(activity).setTitle(com.android.contacts.R.string.dialog_new_contact_account).setSingleChoiceItems(accountAdapter, 0, onClickListener).setOnCancelListener(onCancelListener).create();
    }

    public static void doImport(Activity activity, int resId, AccountWithDataSet account, int subscriptionId) {
        switch (resId) {
            case com.android.contacts.R.string.import_from_sim:
                doImportFromSim(activity, account, subscriptionId);
                break;
            case com.android.contacts.R.string.import_from_sdcard:
                doImportFromSdCard(activity, account);
                break;
        }
    }

    public static void doImportFromSim(Context context, AccountWithDataSet account, int subscriptionId) {
        Intent importIntent = new Intent("android.intent.action.VIEW");
        importIntent.setType("vnd.android.cursor.item/sim-contact");
        if (account != null) {
            importIntent.putExtra("account_name", account.name);
            importIntent.putExtra("account_type", account.type);
            importIntent.putExtra("data_set", account.dataSet);
        }
        importIntent.putExtra("subscription_id", Integer.valueOf(subscriptionId));
        importIntent.putExtra("mode", 0);
        importIntent.putExtra("slot", SubscriptionManager.getSlotId(subscriptionId));
        importIntent.setClassName("com.android.contacts", "com.android.contacts.activities.SimContactsSelectActivity");
        context.startActivity(importIntent);
    }

    public static void doImportFromSdCard(Activity activity, AccountWithDataSet account) {
        Intent importIntent = new Intent(activity, (Class<?>) ImportVCardActivity.class);
        if (account != null) {
            importIntent.putExtra("account_name", account.name);
            importIntent.putExtra("account_type", account.type);
            importIntent.putExtra("data_set", account.dataSet);
        }
        if (mVCardShare) {
            importIntent.setAction("android.intent.action.VIEW");
            importIntent.setData(mPath);
        }
        mVCardShare = false;
        mPath = null;
        activity.startActivityForResult(importIntent, 0);
    }
}

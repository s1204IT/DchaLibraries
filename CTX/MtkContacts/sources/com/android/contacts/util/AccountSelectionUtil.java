package com.android.contacts.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import com.android.contacts.R;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.vcard.ImportVCardActivity;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.util.Log;
import java.util.List;

public class AccountSelectionUtil {
    public static Uri mPath;
    public static boolean mVCardShare = false;

    public static class AccountSelectedListener implements DialogInterface.OnClickListener {
        protected final List<AccountWithDataSet> mAccountList;
        private final Activity mActivity;
        private final int mResId;
        private final int mSubscriptionId;

        public AccountSelectedListener(Activity activity, List<AccountWithDataSet> list, int i, int i2) {
            if (list == null || list.size() == 0) {
                Log.e("AccountSelectionUtil", "The size of Account list is 0.");
            }
            this.mActivity = activity;
            this.mAccountList = list;
            this.mResId = i;
            this.mSubscriptionId = i2;
        }

        public AccountSelectedListener(Activity activity, List<AccountWithDataSet> list, int i) {
            this(activity, list, i, -1);
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            AccountSelectionUtil.doImport(this.mActivity, this.mResId, this.mAccountList.get(i), this.mSubscriptionId);
        }
    }

    public static void doImport(Activity activity, int i, AccountWithDataSet accountWithDataSet, int i2) {
        if (i == R.string.import_from_sim) {
            doImportFromSim(activity, accountWithDataSet, i2);
        } else if (i == R.string.import_from_vcf_file) {
            doImportFromVcfFile(activity, accountWithDataSet);
        }
    }

    public static void doImportFromSim(Context context, AccountWithDataSet accountWithDataSet, int i) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setType("vnd.android.cursor.item/sim-contact");
        if (accountWithDataSet != null) {
            intent.putExtra("account_name", accountWithDataSet.name);
            intent.putExtra("account_type", accountWithDataSet.type);
            intent.putExtra("data_set", accountWithDataSet.dataSet);
            if (accountWithDataSet instanceof AccountWithDataSetEx) {
                intent.putExtra("subscription", ((AccountWithDataSetEx) accountWithDataSet).getSubId());
            }
        }
        intent.putExtra("subscription_id", Integer.valueOf(i));
        intent.setClassName("com.android.phone", "com.android.phone.SimContacts");
        context.startActivity(intent);
    }

    public static void doImportFromVcfFile(Activity activity, AccountWithDataSet accountWithDataSet) {
        Intent intent = new Intent(activity, (Class<?>) ImportVCardActivity.class);
        if (accountWithDataSet != null) {
            intent.putExtra("account_name", accountWithDataSet.name);
            intent.putExtra("account_type", accountWithDataSet.type);
            intent.putExtra("data_set", accountWithDataSet.dataSet);
        }
        if (mVCardShare) {
            intent.setAction("android.intent.action.VIEW");
            intent.setData(mPath);
        }
        mVCardShare = false;
        mPath = null;
        activity.startActivityForResult(intent, 11111);
    }

    public static void doImportFromSdCard(Context context, String str, AccountWithDataSet accountWithDataSet) {
        Log.i("AccountSelectionUtil", "[doImportFromSdCard]sourceStorage = " + Log.anonymize(str));
        Intent intent = new Intent(context, (Class<?>) ImportVCardActivity.class);
        if (accountWithDataSet != null) {
            intent.putExtra("account_name", accountWithDataSet.name);
            intent.putExtra("account_type", accountWithDataSet.type);
            intent.putExtra("data_set", accountWithDataSet.dataSet);
            intent.putExtra("source_path", str);
        }
        ((Activity) context).startActivityForResult(intent, 11111);
    }
}

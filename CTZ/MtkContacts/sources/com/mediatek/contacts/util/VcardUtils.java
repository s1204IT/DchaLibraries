package com.mediatek.contacts.util;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.AccountSelectionUtil;
import com.android.contacts.util.ContactsNotificationChannelsUtil;
import com.mediatek.storage.StorageManagerEx;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VcardUtils {
    private static String sSimAccountType = "SIM Account";
    private static String sUsimAccountType = "USIM Account";
    private static String sRuimAccountType = "RUIM Account";
    private static String sCsimAccountType = "CSIM Account";

    public static void showErrorInfo(final int i, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MtkToast.toast(activity.getApplicationContext(), i);
                activity.finish();
            }
        });
    }

    public static List<AccountWithDataSet> addNonSimAccount(List<AccountWithDataSet> list) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            AccountWithDataSet accountWithDataSet = list.get(i);
            if (!accountWithDataSet.type.equals(sSimAccountType) && !accountWithDataSet.type.equals(sUsimAccountType) && !accountWithDataSet.type.equals(sRuimAccountType) && !accountWithDataSet.type.equals(sCsimAccountType)) {
                Log.d("VcardUtils", "[addNonSimAccount]account1.type : " + accountWithDataSet.type);
                arrayList.add(list.get(i));
            }
        }
        Log.d("VcardUtils", "[addNonSimAccount]accountlist1.size() : " + arrayList.size());
        return arrayList;
    }

    public static Dialog getSelectAccountDialog(Activity activity, int i, DialogInterface.OnClickListener onClickListener, DialogInterface.OnCancelListener onCancelListener) {
        final AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(activity);
        List<AccountWithDataSet> accounts = accountTypeManager.getAccounts(true);
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < accounts.size(); i2++) {
            AccountWithDataSet accountWithDataSet = accounts.get(i2);
            if (!accountWithDataSet.type.equals(sSimAccountType) && !accountWithDataSet.type.equals(sUsimAccountType) && !accountWithDataSet.type.equals(sRuimAccountType) && !accountWithDataSet.type.equals(sCsimAccountType)) {
                arrayList.add(accounts.get(i2));
            }
        }
        final LayoutInflater layoutInflater = (LayoutInflater) new ContextThemeWrapper(activity, R.style.Theme.Light).getSystemService("layout_inflater");
        ArrayAdapter<AccountWithDataSet> arrayAdapter = new ArrayAdapter<AccountWithDataSet>(activity, com.android.contacts.R.layout.mtk_selectaccountactivity, arrayList) {
            @Override
            public View getView(int i3, View view, ViewGroup viewGroup) {
                if (view == null) {
                    view = layoutInflater.inflate(com.android.contacts.R.layout.mtk_selectaccountactivity, viewGroup, false);
                }
                TextView textView = (TextView) view.findViewById(R.id.text1);
                TextView textView2 = (TextView) view.findViewById(R.id.text2);
                AccountWithDataSet item = getItem(i3);
                AccountType accountType = accountTypeManager.getAccountType(item.type, item.dataSet);
                Context context = getContext();
                textView.setText(item.name);
                textView2.setText(accountType.getDisplayLabel(context));
                return view;
            }
        };
        if (onClickListener == null) {
            onClickListener = new AccountSelectionUtil.AccountSelectedListener(activity, accounts, i);
        }
        if (onCancelListener == null) {
            onCancelListener = new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    dialogInterface.dismiss();
                }
            };
        }
        return new AlertDialog.Builder(activity).setTitle(com.android.contacts.R.string.dialog_new_contact_account).setSingleChoiceItems(arrayAdapter, 0, onClickListener).setOnCancelListener(onCancelListener).create();
    }

    public static File getDirectory(String str, String str2) {
        return str == null ? new File(str2) : new File(str);
    }

    public static String getExternalPath(String str) {
        if (str != null) {
            return str;
        }
        if (ContactsPortableUtils.MTK_STORAGE_SUPPORT) {
            return StorageManagerEx.getExternalStoragePath();
        }
        return null;
    }

    public static String getVolumeName(String str, Activity activity) {
        StorageVolume[] volumeList = ((StorageManager) activity.getApplicationContext().getSystemService("storage")).getVolumeList();
        String description = null;
        if (volumeList != null) {
            for (StorageVolume storageVolume : volumeList) {
                if (str != null && storageVolume.getPath().equals(str)) {
                    description = storageVolume.getDescription(activity);
                }
            }
        }
        return description;
    }

    public static void showFailureNotification(final Context context, String str, String str2, int i, Handler handler) {
        final String str3 = context.getString(com.android.contacts.R.string.vcard_import_failed_v2) + " " + str2;
        ((NotificationManager) context.getSystemService("notification")).notify("VCardServiceFailure", i, constructImportFailureNotification(context, str, str3));
        handler.post(new Runnable() {
            @Override
            public void run() {
                MtkToast.toast(context, str3, 1);
            }
        });
    }

    public static Notification constructImportFailureNotification(Context context, String str, String str2) {
        ContactsNotificationChannelsUtil.createDefaultChannel(context);
        return new Notification.Builder(context).setAutoCancel(true).setChannelId(ContactsNotificationChannelsUtil.DEFAULT_CHANNEL).setSmallIcon(R.drawable.stat_notify_error).setContentTitle(str2).setContentText(str).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context.getPackageName(), (Uri) null), 0)).getNotification();
    }
}

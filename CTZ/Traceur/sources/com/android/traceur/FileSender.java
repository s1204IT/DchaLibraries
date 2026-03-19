package com.android.traceur;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemProperties;
import android.support.v4.content.FileProvider;
import android.util.Patterns;
import java.io.File;

public class FileSender {
    public static void postNotification(Context context, File file) {
        Uri uriForFile = getUriForFile(context, file);
        Intent intentBuildSendIntent = buildSendIntent(context, uriForFile);
        intentBuildSendIntent.addFlags(268435456);
        NotificationManager.from(context).notify(file.getName(), 0, new Notification.Builder(context, "system-tracing").setSmallIcon(R.drawable.stat_sys_adb).setContentTitle(context.getString(R.string.trace_saved)).setTicker(context.getString(R.string.trace_saved)).setContentText(context.getString(R.string.tap_to_share)).setContentIntent(PendingIntent.getActivity(context, uriForFile.hashCode(), intentBuildSendIntent, 1342177280)).setAutoCancel(true).setLocalOnly(true).setColor(context.getColor(android.R.color.car_colorPrimary)).build());
    }

    private static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(context, "com.android.traceur.files", file);
    }

    private static Intent buildSendIntent(Context context, Uri uri) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.addFlags(1);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("application/vnd.android.systrace");
        intent.putExtra("android.intent.extra.SUBJECT", uri.getLastPathSegment());
        intent.putExtra("android.intent.extra.TEXT", SystemProperties.get("ro.build.description"));
        intent.putExtra("android.intent.extra.STREAM", uri);
        Account accountFindSendToAccount = findSendToAccount(context);
        if (accountFindSendToAccount != null) {
            intent.putExtra("android.intent.extra.EMAIL", new String[]{accountFindSendToAccount.name});
        }
        return intent;
    }

    private static Account findSendToAccount(Context context) {
        AccountManager accountManager = (AccountManager) context.getSystemService("account");
        String str = SystemProperties.get("sendbug.preferred.domain");
        if (!str.startsWith("@")) {
            str = "@" + str;
        }
        Account account = null;
        for (Account account2 : accountManager.getAccounts()) {
            if (Patterns.EMAIL_ADDRESS.matcher(account2.name).matches()) {
                if (!str.isEmpty()) {
                    if (!account2.name.endsWith(str)) {
                        account = account2;
                    } else {
                        return account2;
                    }
                } else {
                    return account2;
                }
            }
        }
        return account;
    }
}

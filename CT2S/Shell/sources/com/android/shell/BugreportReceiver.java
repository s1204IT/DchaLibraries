package com.android.shell;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.FileUtils;
import android.os.SystemProperties;
import android.support.v4.content.FileProvider;
import android.util.Patterns;
import com.google.android.collect.Lists;
import java.io.File;
import java.util.ArrayList;

public class BugreportReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent notifIntent;
        final File bugreportFile = getFileExtra(intent, "android.intent.extra.BUGREPORT");
        File screenshotFile = getFileExtra(intent, "android.intent.extra.SCREENSHOT");
        Uri bugreportUri = FileProvider.getUriForFile(context, "com.android.shell", bugreportFile);
        Uri screenshotUri = FileProvider.getUriForFile(context, "com.android.shell", screenshotFile);
        Intent sendIntent = buildSendIntent(context, bugreportUri, screenshotUri);
        if (BugreportPrefs.getWarningState(context, 1) == 1) {
            notifIntent = buildWarningIntent(context, sendIntent);
        } else {
            notifIntent = sendIntent;
        }
        notifIntent.addFlags(268435456);
        Notification.Builder builder = new Notification.Builder(context).setSmallIcon(android.R.drawable.item_background_borderless_material_dark).setContentTitle(context.getString(R.string.bugreport_finished_title)).setTicker(context.getString(R.string.bugreport_finished_title)).setContentText(context.getString(R.string.bugreport_finished_text)).setContentIntent(PendingIntent.getActivity(context, 0, notifIntent, 268435456)).setAutoCancel(true).setLocalOnly(true).setColor(context.getResources().getColor(android.R.color.system_accent3_600));
        NotificationManager.from(context).notify("Shell", 0, builder.build());
        final BroadcastReceiver.PendingResult result = goAsync();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                FileUtils.deleteOlderFiles(bugreportFile.getParentFile(), 8, 604800000L);
                result.finish();
                return null;
            }
        }.execute(new Void[0]);
    }

    private static Intent buildWarningIntent(Context context, Intent sendIntent) {
        Intent intent = new Intent(context, (Class<?>) BugreportWarningActivity.class);
        intent.putExtra("android.intent.extra.INTENT", sendIntent);
        return intent;
    }

    private static Intent buildSendIntent(Context context, Uri bugreportUri, Uri screenshotUri) {
        Intent intent = new Intent("android.intent.action.SEND_MULTIPLE");
        intent.addFlags(1);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("application/vnd.android.bugreport");
        intent.putExtra("android.intent.extra.SUBJECT", bugreportUri.getLastPathSegment());
        intent.putExtra("android.intent.extra.TEXT", SystemProperties.get("ro.build.description"));
        ArrayList<Uri> attachments = Lists.newArrayList(new Uri[]{bugreportUri, screenshotUri});
        intent.putParcelableArrayListExtra("android.intent.extra.STREAM", attachments);
        Account sendToAccount = findSendToAccount(context);
        if (sendToAccount != null) {
            intent.putExtra("android.intent.extra.EMAIL", new String[]{sendToAccount.name});
        }
        return intent;
    }

    private static Account findSendToAccount(Context context) {
        AccountManager am = (AccountManager) context.getSystemService("account");
        String preferredDomain = SystemProperties.get("sendbug.preferred.domain");
        if (!preferredDomain.startsWith("@")) {
            preferredDomain = "@" + preferredDomain;
        }
        Account[] accounts = am.getAccounts();
        Account foundAccount = null;
        int len$ = accounts.length;
        for (int i$ = 0; i$ < len$; i$++) {
            Account account = accounts[i$];
            if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches()) {
                if (!preferredDomain.isEmpty() && !account.name.endsWith(preferredDomain)) {
                    foundAccount = account;
                } else {
                    return account;
                }
            }
        }
        return foundAccount;
    }

    private static File getFileExtra(Intent intent, String key) {
        String path = intent.getStringExtra(key);
        if (path != null) {
            return new File(path);
        }
        return null;
    }
}

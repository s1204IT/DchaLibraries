package com.android.storagemanager.deletionhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import com.android.settingslib.Utils;
import com.android.storagemanager.R;
import java.util.concurrent.TimeUnit;

public class StorageManagerUpsellDialog extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private Clock mClock;
    private static final long DISMISS_SHORT_DELAY = TimeUnit.DAYS.toMillis(14);
    private static final long DISMISS_LONG_DELAY = TimeUnit.DAYS.toMillis(90);
    private static final long NO_THANKS_SHORT_DELAY = TimeUnit.DAYS.toMillis(90);

    public static StorageManagerUpsellDialog newInstance(long j) {
        StorageManagerUpsellDialog storageManagerUpsellDialog = new StorageManagerUpsellDialog();
        Bundle bundle = new Bundle(1);
        bundle.putLong("freed_bytes", j);
        storageManagerUpsellDialog.setArguments(bundle);
        return storageManagerUpsellDialog;
    }

    protected void setClock(Clock clock) {
        this.mClock = clock;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        long j = getArguments().getLong("freed_bytes");
        Context context = getContext();
        return new AlertDialog.Builder(context).setTitle(context.getString(R.string.deletion_helper_upsell_title)).setMessage(context.getString(R.string.deletion_helper_upsell_summary, Formatter.formatFileSize(context, j))).setPositiveButton(R.string.deletion_helper_upsell_activate, this).setNegativeButton(R.string.deletion_helper_upsell_cancel, this).create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            Settings.Secure.putInt(getActivity().getContentResolver(), "automatic_storage_manager_enabled", 1);
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences(getContext());
            int i2 = sharedPreferences.getInt("no_thanks_count", 0) + 1;
            SharedPreferences.Editor editorEdit = sharedPreferences.edit();
            editorEdit.putInt("no_thanks_count", i2);
            long noThanksDelay = getNoThanksDelay(i2);
            editorEdit.putLong("next_show_time", noThanksDelay != -1 ? getCurrentTime() + noThanksDelay : -1L);
            editorEdit.apply();
        }
        finishActivity();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        SharedPreferences sharedPreferences = getSharedPreferences(getContext());
        int i = sharedPreferences.getInt("dismissed_count", 0) + 1;
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.putInt("dismissed_count", i);
        editorEdit.putLong("next_show_time", getCurrentTime() + getDismissDelay(i));
        editorEdit.apply();
        finishActivity();
    }

    public static boolean shouldShow(Context context, long j) {
        if (Utils.isStorageManagerEnabled(context)) {
            return false;
        }
        long j2 = getSharedPreferences(context).getLong("next_show_time", 0L);
        return j2 != -1 && j >= j2;
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("StorageManagerUpsellDialog", 0);
    }

    private static long getNoThanksDelay(int i) {
        if (i > 3) {
            return -1L;
        }
        return NO_THANKS_SHORT_DELAY;
    }

    private static long getDismissDelay(int i) {
        return i > 9 ? DISMISS_LONG_DELAY : DISMISS_SHORT_DELAY;
    }

    private void finishActivity() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    private long getCurrentTime() {
        if (this.mClock == null) {
            this.mClock = new Clock();
        }
        return this.mClock.currentTimeMillis();
    }

    protected static class Clock {
        protected Clock() {
        }

        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    }
}

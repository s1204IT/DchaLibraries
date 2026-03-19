package com.android.internal.widget;

import android.os.AsyncTask;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LockPatternChecker {

    public interface OnVerifyCallback {
        void onVerified(byte[] bArr, int i);
    }

    public interface OnCheckCallback {
        void onChecked(boolean z, int i);

        default void onEarlyMatched() {
        }

        default void onCancelled() {
        }
    }

    public static AsyncTask<?, ?, ?> verifyPattern(final LockPatternUtils lockPatternUtils, final List<LockPatternView.Cell> list, final long j, final int i, final OnVerifyCallback onVerifyCallback) {
        AsyncTask<Void, Void, byte[]> asyncTask = new AsyncTask<Void, Void, byte[]>() {
            private int mThrottleTimeout;
            private List<LockPatternView.Cell> patternCopy;

            @Override
            protected void onPreExecute() {
                this.patternCopy = new ArrayList(list);
            }

            @Override
            protected byte[] doInBackground(Void... voidArr) {
                try {
                    return lockPatternUtils.verifyPattern(this.patternCopy, j, i);
                } catch (LockPatternUtils.RequestThrottledException e) {
                    this.mThrottleTimeout = e.getTimeoutMs();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(byte[] bArr) {
                onVerifyCallback.onVerified(bArr, this.mThrottleTimeout);
            }
        };
        asyncTask.execute(new Void[0]);
        return asyncTask;
    }

    public static AsyncTask<?, ?, ?> checkPattern(final LockPatternUtils lockPatternUtils, final List<LockPatternView.Cell> list, final int i, final OnCheckCallback onCheckCallback) {
        AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
            private int mThrottleTimeout;
            private List<LockPatternView.Cell> patternCopy;

            @Override
            protected void onPreExecute() {
                this.patternCopy = new ArrayList(list);
            }

            @Override
            protected Boolean doInBackground(Void... voidArr) {
                try {
                    LockPatternUtils lockPatternUtils2 = lockPatternUtils;
                    List<LockPatternView.Cell> list2 = this.patternCopy;
                    int i2 = i;
                    OnCheckCallback onCheckCallback2 = onCheckCallback;
                    Objects.requireNonNull(onCheckCallback2);
                    return Boolean.valueOf(lockPatternUtils2.checkPattern(list2, i2, new $$Lambda$TTC7hNz7BTsLwhNRb2L5kl7mdU(onCheckCallback2)));
                } catch (LockPatternUtils.RequestThrottledException e) {
                    this.mThrottleTimeout = e.getTimeoutMs();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean bool) {
                onCheckCallback.onChecked(bool.booleanValue(), this.mThrottleTimeout);
            }

            @Override
            protected void onCancelled() {
                onCheckCallback.onCancelled();
            }
        };
        asyncTask.execute(new Void[0]);
        return asyncTask;
    }

    public static AsyncTask<?, ?, ?> verifyPassword(final LockPatternUtils lockPatternUtils, final String str, final long j, final int i, final OnVerifyCallback onVerifyCallback) {
        AsyncTask<Void, Void, byte[]> asyncTask = new AsyncTask<Void, Void, byte[]>() {
            private int mThrottleTimeout;

            @Override
            protected byte[] doInBackground(Void... voidArr) {
                try {
                    return lockPatternUtils.verifyPassword(str, j, i);
                } catch (LockPatternUtils.RequestThrottledException e) {
                    this.mThrottleTimeout = e.getTimeoutMs();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(byte[] bArr) {
                onVerifyCallback.onVerified(bArr, this.mThrottleTimeout);
            }
        };
        asyncTask.execute(new Void[0]);
        return asyncTask;
    }

    public static AsyncTask<?, ?, ?> verifyTiedProfileChallenge(final LockPatternUtils lockPatternUtils, final String str, final boolean z, final long j, final int i, final OnVerifyCallback onVerifyCallback) {
        AsyncTask<Void, Void, byte[]> asyncTask = new AsyncTask<Void, Void, byte[]>() {
            private int mThrottleTimeout;

            @Override
            protected byte[] doInBackground(Void... voidArr) {
                try {
                    return lockPatternUtils.verifyTiedProfileChallenge(str, z, j, i);
                } catch (LockPatternUtils.RequestThrottledException e) {
                    this.mThrottleTimeout = e.getTimeoutMs();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(byte[] bArr) {
                onVerifyCallback.onVerified(bArr, this.mThrottleTimeout);
            }
        };
        asyncTask.execute(new Void[0]);
        return asyncTask;
    }

    public static AsyncTask<?, ?, ?> checkPassword(final LockPatternUtils lockPatternUtils, final String str, final int i, final OnCheckCallback onCheckCallback) {
        AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
            private int mThrottleTimeout;

            @Override
            protected Boolean doInBackground(Void... voidArr) {
                try {
                    LockPatternUtils lockPatternUtils2 = lockPatternUtils;
                    String str2 = str;
                    int i2 = i;
                    OnCheckCallback onCheckCallback2 = onCheckCallback;
                    Objects.requireNonNull(onCheckCallback2);
                    return Boolean.valueOf(lockPatternUtils2.checkPassword(str2, i2, new $$Lambda$TTC7hNz7BTsLwhNRb2L5kl7mdU(onCheckCallback2)));
                } catch (LockPatternUtils.RequestThrottledException e) {
                    this.mThrottleTimeout = e.getTimeoutMs();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean bool) {
                onCheckCallback.onChecked(bool.booleanValue(), this.mThrottleTimeout);
            }

            @Override
            protected void onCancelled() {
                onCheckCallback.onCancelled();
            }
        };
        asyncTask.execute(new Void[0]);
        return asyncTask;
    }
}

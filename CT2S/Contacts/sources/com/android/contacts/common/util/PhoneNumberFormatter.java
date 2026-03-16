package com.android.contacts.common.util;

import android.content.Context;
import android.os.AsyncTask;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.widget.TextView;
import com.android.contacts.common.GeoUtil;

public final class PhoneNumberFormatter {

    private static class TextWatcherLoadAsyncTask extends AsyncTask<Void, Void, PhoneNumberFormattingTextWatcher> {
        private final String mCountryCode;
        private final TextView mTextView;

        public TextWatcherLoadAsyncTask(String countryCode, TextView textView) {
            this.mCountryCode = countryCode;
            this.mTextView = textView;
        }

        @Override
        protected PhoneNumberFormattingTextWatcher doInBackground(Void... params) {
            return new PhoneNumberFormattingTextWatcher(this.mCountryCode);
        }

        @Override
        protected void onPostExecute(PhoneNumberFormattingTextWatcher watcher) {
            if (watcher != null && !isCancelled()) {
                this.mTextView.addTextChangedListener(watcher);
            }
        }
    }

    public static final void setPhoneNumberFormattingTextWatcher(Context context, TextView textView) {
        new TextWatcherLoadAsyncTask(GeoUtil.getCurrentCountryIso(context), textView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }
}

package com.android.phone.common;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.provider.CallLog;

public class CallLogAsync {
    private static final String TAG = "CallLogAsync";

    public interface OnLastOutgoingCallComplete {
        void lastOutgoingCall(String str);
    }

    public static class GetLastOutgoingCallArgs {
        public final OnLastOutgoingCallComplete callback;
        public final Context context;

        public GetLastOutgoingCallArgs(Context context, OnLastOutgoingCallComplete onLastOutgoingCallComplete) {
            this.context = context;
            this.callback = onLastOutgoingCallComplete;
        }
    }

    public AsyncTask getLastOutgoingCall(GetLastOutgoingCallArgs getLastOutgoingCallArgs) {
        assertUiThread();
        return new GetLastOutgoingCallTask(getLastOutgoingCallArgs.callback).execute(getLastOutgoingCallArgs);
    }

    private class GetLastOutgoingCallTask extends AsyncTask<GetLastOutgoingCallArgs, Void, String> {
        private final OnLastOutgoingCallComplete mCallback;
        private String mNumber;

        public GetLastOutgoingCallTask(OnLastOutgoingCallComplete onLastOutgoingCallComplete) {
            this.mCallback = onLastOutgoingCallComplete;
        }

        @Override
        protected String doInBackground(GetLastOutgoingCallArgs... getLastOutgoingCallArgsArr) {
            int length = getLastOutgoingCallArgsArr.length;
            String lastOutgoingCall = "";
            for (GetLastOutgoingCallArgs getLastOutgoingCallArgs : getLastOutgoingCallArgsArr) {
                lastOutgoingCall = CallLog.Calls.getLastOutgoingCall(getLastOutgoingCallArgs.context);
            }
            return lastOutgoingCall;
        }

        @Override
        protected void onPostExecute(String str) {
            CallLogAsync.this.assertUiThread();
            this.mCallback.lastOutgoingCall(str);
        }
    }

    private void assertUiThread() {
        if (!Looper.getMainLooper().equals(Looper.myLooper())) {
            throw new RuntimeException("Not on the UI thread!");
        }
    }
}

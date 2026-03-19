package com.android.server.telecom.settings;

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BlockedNumberContract;

public class BlockNumberTaskFragment extends Fragment {
    Listener mListener;
    private BlockNumberTask mTask;

    public interface Listener {
        void onBlocked(String str, boolean z);
    }

    private class BlockNumberTask extends AsyncTask<String, Void, Boolean> {
        private String mNumber;

        private BlockNumberTask() {
        }

        @Override
        protected Boolean doInBackground(String... strArr) {
            this.mNumber = strArr[0];
            if (BlockedNumberContract.isBlocked(BlockNumberTaskFragment.this.getContext(), this.mNumber)) {
                return false;
            }
            ContentResolver contentResolver = BlockNumberTaskFragment.this.getContext().getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put("original_number", this.mNumber);
            contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, contentValues);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            BlockNumberTaskFragment.this.mTask = null;
            if (BlockNumberTaskFragment.this.mListener != null) {
                BlockNumberTaskFragment.this.mListener.onBlocked(this.mNumber, !bool.booleanValue());
            }
            BlockNumberTaskFragment.this.mListener = null;
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        if (this.mTask != null) {
            this.mTask.cancel(true);
        }
        super.onDestroy();
    }

    public void blockIfNotAlreadyBlocked(String str, Listener listener) {
        this.mListener = listener;
        this.mTask = new BlockNumberTask();
        this.mTask.execute(str);
    }
}

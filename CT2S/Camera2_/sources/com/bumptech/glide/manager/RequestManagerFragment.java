package com.bumptech.glide.manager;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.util.Log;
import com.bumptech.glide.RequestManager;

@TargetApi(11)
public class RequestManagerFragment extends Fragment {
    private static String TAG = "RequestManagerFragment";
    private RequestManager requestManager;

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    public RequestManager getRequestManager() {
        return this.requestManager;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.requestManager != null) {
            this.requestManager.onStart();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.requestManager != null) {
            try {
                this.requestManager.onStop();
            } catch (RuntimeException e) {
                Log.e(TAG, "exception during onStop", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.requestManager != null) {
            try {
                this.requestManager.onDestroy();
            } catch (RuntimeException e) {
                Log.e(TAG, "exception during onDestroy", e);
            }
        }
    }
}

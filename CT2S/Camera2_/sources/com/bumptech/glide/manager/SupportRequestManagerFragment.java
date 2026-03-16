package com.bumptech.glide.manager;

import android.support.v4.app.Fragment;
import com.bumptech.glide.RequestManager;

public class SupportRequestManagerFragment extends Fragment {
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
            this.requestManager.onStop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.requestManager != null) {
            this.requestManager.onDestroy();
        }
    }
}

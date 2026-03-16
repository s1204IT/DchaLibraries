package com.bumptech.glide.manager;

import com.bumptech.glide.RequestManager;

public interface RequestManagerLifecycleFragment {
    RequestManager getRequestManager();

    void onDestroy();

    void onStart();

    void onStop();

    void setRequestManager(RequestManager requestManager);
}

package com.android.settings.core;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import com.android.settings.SubSettings;

public class SubSettingLauncher {
    private final Context mContext;
    private final LaunchRequest mLaunchRequest;
    private boolean mLaunched;

    public SubSettingLauncher(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must be non-null.");
        }
        this.mContext = context;
        this.mLaunchRequest = new LaunchRequest();
    }

    public SubSettingLauncher setDestination(String str) {
        this.mLaunchRequest.destinationName = str;
        return this;
    }

    public SubSettingLauncher setTitle(int i) {
        return setTitle(null, i);
    }

    public SubSettingLauncher setTitle(String str, int i) {
        this.mLaunchRequest.titleResPackageName = str;
        this.mLaunchRequest.titleResId = i;
        this.mLaunchRequest.title = null;
        return this;
    }

    public SubSettingLauncher setTitle(CharSequence charSequence) {
        this.mLaunchRequest.title = charSequence;
        return this;
    }

    public SubSettingLauncher setArguments(Bundle bundle) {
        this.mLaunchRequest.arguments = bundle;
        return this;
    }

    public SubSettingLauncher setSourceMetricsCategory(int i) {
        this.mLaunchRequest.sourceMetricsCategory = i;
        return this;
    }

    public SubSettingLauncher setResultListener(Fragment fragment, int i) {
        this.mLaunchRequest.mRequestCode = i;
        this.mLaunchRequest.mResultListener = fragment;
        return this;
    }

    public SubSettingLauncher addFlags(int i) {
        LaunchRequest launchRequest = this.mLaunchRequest;
        launchRequest.flags = i | launchRequest.flags;
        return this;
    }

    public SubSettingLauncher setUserHandle(UserHandle userHandle) {
        this.mLaunchRequest.userHandle = userHandle;
        return this;
    }

    public void launch() {
        if (this.mLaunched) {
            throw new IllegalStateException("This launcher has already been executed. Do not reuse");
        }
        boolean z = true;
        this.mLaunched = true;
        Intent intent = toIntent();
        boolean z2 = (this.mLaunchRequest.userHandle == null || this.mLaunchRequest.userHandle.getIdentifier() == UserHandle.myUserId()) ? false : true;
        if (this.mLaunchRequest.mResultListener == null) {
            z = false;
        }
        if (z2 && z) {
            launchForResultAsUser(intent, this.mLaunchRequest.userHandle, this.mLaunchRequest.mResultListener, this.mLaunchRequest.mRequestCode);
            return;
        }
        if (z2 && !z) {
            launchAsUser(intent, this.mLaunchRequest.userHandle);
        } else if (!z2 && z) {
            launchForResult(this.mLaunchRequest.mResultListener, intent, this.mLaunchRequest.mRequestCode);
        } else {
            launch(intent);
        }
    }

    public Intent toIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClass(this.mContext, SubSettings.class);
        if (TextUtils.isEmpty(this.mLaunchRequest.destinationName)) {
            throw new IllegalArgumentException("Destination fragment must be set");
        }
        intent.putExtra(":settings:show_fragment", this.mLaunchRequest.destinationName);
        if (this.mLaunchRequest.sourceMetricsCategory < 0) {
            throw new IllegalArgumentException("Source metrics category must be set");
        }
        intent.putExtra(":settings:source_metrics", this.mLaunchRequest.sourceMetricsCategory);
        intent.putExtra(":settings:show_fragment_args", this.mLaunchRequest.arguments);
        intent.putExtra(":settings:show_fragment_title_res_package_name", this.mLaunchRequest.titleResPackageName);
        intent.putExtra(":settings:show_fragment_title_resid", this.mLaunchRequest.titleResId);
        intent.putExtra(":settings:show_fragment_title", this.mLaunchRequest.title);
        intent.putExtra(":settings:show_fragment_as_shortcut", this.mLaunchRequest.isShortCut);
        intent.addFlags(this.mLaunchRequest.flags);
        return intent;
    }

    void launch(Intent intent) {
        this.mContext.startActivity(intent);
    }

    void launchAsUser(Intent intent, UserHandle userHandle) {
        intent.addFlags(268435456);
        intent.addFlags(32768);
        this.mContext.startActivityAsUser(intent, userHandle);
    }

    void launchForResultAsUser(Intent intent, UserHandle userHandle, Fragment fragment, int i) {
        fragment.getActivity().startActivityForResultAsUser(intent, i, userHandle);
    }

    private void launchForResult(Fragment fragment, Intent intent, int i) {
        fragment.startActivityForResult(intent, i);
    }

    static class LaunchRequest {
        Bundle arguments;
        String destinationName;
        int flags;
        boolean isShortCut;
        int mRequestCode;
        Fragment mResultListener;
        int sourceMetricsCategory = -100;
        CharSequence title;
        int titleResId;
        String titleResPackageName;
        UserHandle userHandle;

        LaunchRequest() {
        }
    }
}

package com.bumptech.glide.manager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import com.bumptech.glide.RequestManager;

public class RequestManagerRetriever {
    static final String TAG = "com.bumptech.glide.manager";
    private static RequestManager applicationManager;

    public static RequestManager get(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("You cannot start a load on a null Context");
        }
        if (context instanceof FragmentActivity) {
            return get((FragmentActivity) context);
        }
        if (context instanceof Activity) {
            return get((Activity) context);
        }
        if (applicationManager == null) {
            applicationManager = new RequestManager(context.getApplicationContext());
        }
        return applicationManager;
    }

    @TargetApi(17)
    public static RequestManager get(FragmentActivity activity) {
        if (Build.VERSION.SDK_INT >= 11 && activity.isDestroyed()) {
            throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
        }
        FragmentManager fm = activity.getSupportFragmentManager();
        return supportFragmentGet(activity, fm);
    }

    public static RequestManager get(Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        }
        if (fragment.isDetached()) {
            throw new IllegalArgumentException("You cannot start a load on a detached fragment");
        }
        FragmentManager fm = fragment.getChildFragmentManager();
        return supportFragmentGet(fragment.getActivity(), fm);
    }

    @TargetApi(17)
    public static RequestManager get(Activity activity) {
        if (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed()) {
            throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
        }
        android.app.FragmentManager fm = activity.getFragmentManager();
        return fragmentGet(activity, fm);
    }

    @TargetApi(17)
    public static RequestManager get(android.app.Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        }
        if (Build.VERSION.SDK_INT >= 13 && fragment.isDetached()) {
            throw new IllegalArgumentException("You cannot start a load on a detached fragment");
        }
        if (Build.VERSION.SDK_INT < 17) {
            return get(fragment.getActivity().getApplicationContext());
        }
        android.app.FragmentManager fm = fragment.getChildFragmentManager();
        return fragmentGet(fragment.getActivity(), fm);
    }

    @TargetApi(11)
    static RequestManager fragmentGet(Context context, android.app.FragmentManager fm) {
        RequestManagerFragment current = (RequestManagerFragment) fm.findFragmentByTag(TAG);
        if (current == null) {
            current = new RequestManagerFragment();
            fm.beginTransaction().add(current, TAG).commitAllowingStateLoss();
            fm.executePendingTransactions();
        }
        RequestManager requestManager = current.getRequestManager();
        if (requestManager == null) {
            RequestManager requestManager2 = new RequestManager(context);
            current.setRequestManager(requestManager2);
            return requestManager2;
        }
        return requestManager;
    }

    static RequestManager supportFragmentGet(Context context, FragmentManager fm) {
        SupportRequestManagerFragment current = (SupportRequestManagerFragment) fm.findFragmentByTag(TAG);
        if (current == null) {
            current = new SupportRequestManagerFragment();
            fm.beginTransaction().add(current, TAG).commitAllowingStateLoss();
            fm.executePendingTransactions();
        }
        RequestManager requestManager = current.getRequestManager();
        if (requestManager == null) {
            RequestManager requestManager2 = new RequestManager(context);
            current.setRequestManager(requestManager2);
            return requestManager2;
        }
        return requestManager;
    }
}

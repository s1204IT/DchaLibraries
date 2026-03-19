package com.mediatek.camera.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSettingView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SettingFragment extends PreferenceFragment {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SettingFragment.class.getSimpleName());
    private List<ICameraSettingView> mSettingViewList = new ArrayList();
    private StateListener mStateListener;
    private Toolbar mToolbar;

    public interface StateListener {
        void onCreate();

        void onDestroy();

        void onPause();

        void onResume();
    }

    public void setStateListener(StateListener stateListener) {
        this.mStateListener = stateListener;
    }

    @Override
    public void onCreate(Bundle bundle) {
        LogHelper.d(TAG, "[onCreate]");
        if (this.mStateListener != null) {
            this.mStateListener.onCreate();
        }
        super.onCreate(bundle);
        this.mToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (this.mToolbar != null) {
            this.mToolbar.setTitle(getActivity().getResources().getString(R.string.setting_title));
            this.mToolbar.setTitleTextColor(getActivity().getResources().getColor(android.R.color.white));
            this.mToolbar.setNavigationIcon(getActivity().getResources().getDrawable(R.drawable.ic_setting_up));
            this.mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LogHelper.i(SettingFragment.TAG, "[onClick], activity:" + SettingFragment.this.getActivity());
                    if (SettingFragment.this.getActivity() != null) {
                        SettingFragment.this.getActivity().getFragmentManager().popBackStack();
                    }
                }
            });
        }
        addPreferencesFromResource(R.xml.camera_preferences);
        synchronized (this) {
            Iterator<ICameraSettingView> it = this.mSettingViewList.iterator();
            while (it.hasNext()) {
                it.next().loadView(this);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        LogHelper.d(TAG, "[onActivityCreated]");
        super.onActivityCreated(bundle);
    }

    @Override
    public void onResume() {
        LogHelper.d(TAG, "[onResume]");
        super.onResume();
        if (this.mToolbar != null) {
            this.mToolbar.setTitle(getActivity().getResources().getString(R.string.setting_title));
        }
        synchronized (this) {
            Iterator<ICameraSettingView> it = this.mSettingViewList.iterator();
            while (it.hasNext()) {
                it.next().refreshView();
            }
        }
        if (this.mStateListener != null) {
            this.mStateListener.onResume();
        }
    }

    @Override
    public void onPause() {
        LogHelper.d(TAG, "[onPause]");
        super.onPause();
        if (this.mStateListener != null) {
            this.mStateListener.onPause();
        }
    }

    @Override
    public void onDestroy() {
        LogHelper.d(TAG, "[onDestroy]");
        super.onDestroy();
        synchronized (this) {
            Iterator<ICameraSettingView> it = this.mSettingViewList.iterator();
            while (it.hasNext()) {
                it.next().unloadView();
            }
        }
        if (this.mStateListener != null) {
            this.mStateListener.onDestroy();
        }
    }

    public synchronized void addSettingView(ICameraSettingView iCameraSettingView) {
        if (iCameraSettingView == null) {
            LogHelper.w(TAG, "[addSettingView], view:" + iCameraSettingView, new Throwable());
            return;
        }
        if (!this.mSettingViewList.contains(iCameraSettingView)) {
            this.mSettingViewList.add(iCameraSettingView);
        }
    }

    public synchronized void removeSettingView(ICameraSettingView iCameraSettingView) {
        this.mSettingViewList.remove(iCameraSettingView);
    }

    public synchronized void refreshSettingView() {
        Iterator<ICameraSettingView> it = this.mSettingViewList.iterator();
        while (it.hasNext()) {
            it.next().refreshView();
        }
    }

    public synchronized boolean hasVisibleChild() {
        boolean z;
        z = false;
        Iterator<ICameraSettingView> it = this.mSettingViewList.iterator();
        while (it.hasNext()) {
            if (it.next().isEnabled()) {
                z = true;
            }
        }
        return z;
    }
}

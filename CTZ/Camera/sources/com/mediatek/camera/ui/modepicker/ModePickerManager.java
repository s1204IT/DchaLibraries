package com.mediatek.camera.ui.modepicker;

import android.app.FragmentTransaction;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.widget.RotateImageView;
import com.mediatek.camera.ui.AbstractViewManager;
import com.mediatek.camera.ui.modepicker.ModePickerFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class ModePickerManager extends AbstractViewManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ModePickerManager.class.getSimpleName());
    private final IApp mApp;
    private String mCurrentCameraId;
    private String mCurrentModeName;
    private ModePickerFragment mFragment;
    private boolean mIsSettingIconVisible;
    private OnModeChangedListener mModeChangeListener;
    private RotateImageView mModePickerView;
    private ModeProvider mModeProvider;
    private View.OnClickListener mSettingClickedListener;

    public interface OnModeChangedListener {
        void onModeChanged(String str);
    }

    public class ModeInfo {
        public String mName;
        public Drawable mSelectedIcon;
        public String[] mSupportedCameraIds;
        public Drawable mUnselectedIcon;

        public ModeInfo() {
        }
    }

    public ModePickerManager(IApp iApp, ViewGroup viewGroup) {
        super(iApp, viewGroup);
        this.mCurrentCameraId = "0";
        this.mApp = iApp;
    }

    @Override
    protected View getView() {
        this.mModePickerView = (RotateImageView) this.mParentView.findViewById(R.id.mode);
        return this.mModePickerView;
    }

    @Override
    public void onPause() {
        super.onPause();
        hideModePickerFragment();
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        if (this.mFragment != null) {
            this.mFragment.setEnabled(z);
        }
    }

    public void registerModeProvider(ModeProvider modeProvider) {
        LogHelper.d(TAG, "registerModeProvider ");
        this.mModeProvider = modeProvider;
    }

    public void onPreviewStarted(final String str) {
        LogHelper.d(TAG, "onPreviewStarted  previewCameraId " + str);
        this.mCurrentCameraId = str;
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ModePickerManager.this.mFragment != null) {
                    ModePickerManager.this.mFragment.refreshModeList(ModePickerManager.this.createModePickerList(str));
                }
            }
        });
    }

    public void updateCurrentModeItem(final IAppUi.ModeItem modeItem) {
        this.mCurrentModeName = modeItem.mModeName;
        this.mApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ModePickerManager.this.updateModePickerView(modeItem);
            }
        });
    }

    public void setModeChangeListener(OnModeChangedListener onModeChangedListener) {
        this.mModeChangeListener = onModeChangedListener;
    }

    public void setSettingClickedListener(View.OnClickListener onClickListener) {
        this.mSettingClickedListener = onClickListener;
    }

    public void setSettingIconVisible(boolean z) {
        this.mIsSettingIconVisible = z;
    }

    private class FragmentStateListener implements ModePickerFragment.StateListener {
        private FragmentStateListener() {
        }

        @Override
        public void onCreate() {
            ((ViewGroup) ModePickerManager.this.mApp.getActivity().findViewById(R.id.app_ui_root)).setVisibility(8);
            ModePickerManager.this.mApp.getActivity().findViewById(R.id.preview_layout_container).setVisibility(8);
            ModePickerManager.this.mApp.registerOnOrientationChangeListener(ModePickerManager.this.mFragment);
            IAppUi appUi = ModePickerManager.this.mApp.getAppUi();
            ModePickerManager.this.mApp.getAppUi();
            appUi.setUIEnabled(3, false);
        }

        @Override
        public void onResume() {
        }

        @Override
        public void onPause() {
        }

        @Override
        public void onDestroy() {
            ((ViewGroup) ModePickerManager.this.mApp.getActivity().findViewById(R.id.app_ui_root)).setVisibility(0);
            ModePickerManager.this.mApp.getActivity().findViewById(R.id.preview_layout_container).setVisibility(0);
            ModePickerManager.this.mApp.unregisterOnOrientationChangeListener(ModePickerManager.this.mFragment);
            IAppUi appUi = ModePickerManager.this.mApp.getAppUi();
            ModePickerManager.this.mApp.getAppUi();
            appUi.setUIEnabled(3, true);
            ModePickerManager.this.mFragment = null;
        }
    }

    private List<ModeInfo> createModePickerList(String str) {
        Map<String, IAppUi.ModeItem> modes2 = this.mModeProvider.getModes2();
        ConcurrentSkipListMap concurrentSkipListMap = new ConcurrentSkipListMap();
        for (IAppUi.ModeItem modeItem : modes2.values()) {
            for (int i = 0; i < modeItem.mSupportedCameraIds.length; i++) {
                if (modeItem.mSupportedCameraIds[i].equals(str)) {
                    LogHelper.d(TAG, "find one mode = " + modeItem.mModeName);
                    ModeInfo modeInfo = new ModeInfo();
                    modeInfo.mName = modeItem.mModeName;
                    modeInfo.mSelectedIcon = modeItem.mModeSelectedIcon;
                    modeInfo.mUnselectedIcon = modeItem.mModeUnselectedIcon;
                    modeInfo.mSupportedCameraIds = modeItem.mSupportedCameraIds;
                    concurrentSkipListMap.put(Integer.valueOf(modeItem.mPriority), modeInfo);
                }
            }
        }
        return new ArrayList(concurrentSkipListMap.values());
    }

    private class OnModeSelectedListenerImpl implements ModePickerFragment.OnModeSelectedListener {
        private OnModeSelectedListenerImpl() {
        }

        @Override
        public boolean onModeSelected(ModeInfo modeInfo) {
            if (modeInfo == null || !ModePickerManager.this.isCameraIDSupported(modeInfo.mSupportedCameraIds, ModePickerManager.this.mCurrentCameraId)) {
                return false;
            }
            if (modeInfo.mSelectedIcon != null) {
                ModePickerManager.this.mModePickerView.setImageDrawable(modeInfo.mSelectedIcon);
            } else {
                ModePickerManager.this.mModePickerView.setImageResource(R.drawable.ic_normal_mode_selected);
            }
            ModePickerManager.this.mCurrentModeName = modeInfo.mName;
            ModePickerManager.this.mModeChangeListener.onModeChanged(ModePickerManager.this.mCurrentModeName);
            return true;
        }
    }

    private void updateModePickerView(IAppUi.ModeItem modeItem) {
        String action = this.mApp.getActivity().getIntent().getAction();
        if (!"android.media.action.IMAGE_CAPTURE".equals(action) && !"android.media.action.VIDEO_CAPTURE".equals(action)) {
            if (modeItem != null) {
                if (modeItem.mModeSelectedIcon != null) {
                    this.mModePickerView.setImageDrawable(modeItem.mModeSelectedIcon);
                } else {
                    this.mModePickerView.setImageResource(R.drawable.ic_normal_mode_selected);
                }
            }
            this.mModePickerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (ModePickerManager.this.mFragment != null) {
                        return;
                    }
                    FragmentTransaction fragmentTransactionBeginTransaction = ModePickerManager.this.mApp.getActivity().getFragmentManager().beginTransaction();
                    ModePickerManager.this.mFragment = new ModePickerFragment();
                    ModePickerManager.this.mFragment.setStateListener(new FragmentStateListener());
                    ModePickerManager.this.mFragment.setModeSelectedListener(new OnModeSelectedListenerImpl());
                    ModePickerManager.this.mFragment.setSettingIconVisible(ModePickerManager.this.mIsSettingIconVisible);
                    if (ModePickerManager.this.mIsSettingIconVisible) {
                        ModePickerManager.this.mFragment.setSettingClickedListener(ModePickerManager.this.mSettingClickedListener);
                    }
                    ModePickerManager.this.mFragment.updateCurrentModeName(ModePickerManager.this.mCurrentModeName);
                    ModePickerManager.this.mFragment.refreshModeList(ModePickerManager.this.createModePickerList(ModePickerManager.this.mCurrentCameraId));
                    fragmentTransactionBeginTransaction.replace(R.id.activity_root, ModePickerManager.this.mFragment, "ModePickerFragment");
                    fragmentTransactionBeginTransaction.addToBackStack("ModePickerFragment");
                    fragmentTransactionBeginTransaction.commitAllowingStateLoss();
                }
            });
            return;
        }
        this.mModePickerView.setImageResource(R.drawable.ic_setting);
        this.mModePickerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ModePickerManager.this.mSettingClickedListener != null) {
                    ModePickerManager.this.mSettingClickedListener.onClick(view);
                }
            }
        });
    }

    private void hideModePickerFragment() {
        this.mApp.getActivity().getFragmentManager().popBackStackImmediate("ModePickerFragment", 1);
    }

    private boolean isCameraIDSupported(String[] strArr, String str) {
        LogHelper.d(TAG, "isCameraIDSupported [] = " + strArr.toString());
        int length = strArr.length;
        for (int i = 0; i < length; i++) {
            if (strArr[i].equals(str)) {
                return true;
            }
        }
        return false;
    }
}

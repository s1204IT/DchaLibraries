package com.mediatek.camera.ui.modepicker;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.mediatek.camera.R;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.ui.modepicker.ModeItemAdapter;
import com.mediatek.camera.ui.modepicker.ModePickerManager;
import java.util.List;

public class ModePickerFragment extends Fragment implements IApp.OnOrientationChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ModePickerFragment.class.getSimpleName());
    private ModeItemAdapter mAdapter;
    private String mCurrentModeName;
    private boolean mIsClickEnabled;
    private List<ModePickerManager.ModeInfo> mModeList;
    private OnModeSelectedListener mModeSelectedListener;
    private int mOrientation;
    private RecyclerView mRecyclerView;
    private View.OnClickListener mSettingClickedListener;
    private int mSettingVisibility;
    private StateListener mStateListener;

    public interface OnModeSelectedListener {
        boolean onModeSelected(ModePickerManager.ModeInfo modeInfo);
    }

    public interface StateListener {
        void onCreate();

        void onDestroy();

        void onPause();

        void onResume();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.mode_fragment, viewGroup, false);
        CameraUtil.rotateViewOrientation(viewInflate, CameraUtil.calculateRotateLayoutCompensate(getActivity()), false);
        View viewFindViewById = viewInflate.findViewById(R.id.setting_view);
        viewFindViewById.setVisibility(this.mSettingVisibility);
        if (CameraUtil.isHasNavigationBar(getActivity())) {
            int navigationBarHeight = CameraUtil.getNavigationBarHeight(getActivity());
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewFindViewById.getLayoutParams();
            ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin += navigationBarHeight;
            viewFindViewById.setLayoutParams(layoutParams);
        }
        viewFindViewById.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogHelper.d(ModePickerFragment.TAG, "onClick: mSettingClickedListener = " + ModePickerFragment.this.mSettingClickedListener + ", mIsClickEnabled = false");
                if (ModePickerFragment.this.mIsClickEnabled && ModePickerFragment.this.mSettingClickedListener != null) {
                    ModePickerFragment.this.getActivity().getFragmentManager().popBackStack();
                    ModePickerFragment.this.mSettingClickedListener.onClick(view);
                }
            }
        });
        this.mRecyclerView = (RecyclerView) viewInflate.findViewById(R.id.mode_list);
        this.mAdapter = new ModeItemAdapter(getActivity(), new OnViewItemClickListenerImpl());
        this.mAdapter.updateCurrentModeName(this.mCurrentModeName);
        this.mAdapter.setModeList(this.mModeList);
        this.mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        this.mRecyclerView.setAdapter(this.mAdapter);
        this.mRecyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                if (ModePickerFragment.this.mRecyclerView != null) {
                    CameraUtil.rotateRotateLayoutChildView(ModePickerFragment.this.getActivity(), ModePickerFragment.this.mRecyclerView, ModePickerFragment.this.mOrientation, false);
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
            }
        });
        this.mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean z) {
            }
        });
        return viewInflate;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mIsClickEnabled = true;
        if (this.mStateListener != null) {
            this.mStateListener.onCreate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mStateListener != null) {
            this.mStateListener.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mStateListener != null) {
            this.mStateListener.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mAdapter = null;
        if (this.mStateListener != null) {
            this.mStateListener.onDestroy();
        }
    }

    @Override
    public void onOrientationChanged(int i) {
        this.mOrientation = i;
        if (this.mRecyclerView != null && getActivity() != null) {
            CameraUtil.rotateRotateLayoutChildView(getActivity(), this.mRecyclerView, i, true);
        }
    }

    public void refreshModeList(List<ModePickerManager.ModeInfo> list) {
        this.mModeList = list;
        if (this.mAdapter != null) {
            this.mAdapter.setModeList(list);
        }
    }

    public void setStateListener(StateListener stateListener) {
        this.mStateListener = stateListener;
    }

    public void updateCurrentModeName(String str) {
        this.mCurrentModeName = str;
    }

    public void setSettingClickedListener(View.OnClickListener onClickListener) {
        this.mSettingClickedListener = onClickListener;
    }

    public void setModeSelectedListener(OnModeSelectedListener onModeSelectedListener) {
        this.mModeSelectedListener = onModeSelectedListener;
    }

    public void setSettingIconVisible(boolean z) {
        this.mSettingVisibility = z ? 0 : 8;
    }

    public void setEnabled(boolean z) {
        this.mIsClickEnabled = z;
    }

    private class OnViewItemClickListenerImpl implements ModeItemAdapter.OnViewItemClickListener {
        private OnViewItemClickListenerImpl() {
        }

        @Override
        public boolean onItemCLicked(ModePickerManager.ModeInfo modeInfo) {
            ModePickerFragment.this.getActivity().getFragmentManager().popBackStack();
            return ModePickerFragment.this.mIsClickEnabled && ModePickerFragment.this.mModeSelectedListener.onModeSelected(modeInfo);
        }
    }
}

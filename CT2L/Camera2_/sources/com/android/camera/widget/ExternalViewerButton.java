package com.android.camera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageButton;
import com.android.camera.debug.Log;
import com.android.camera2.R;

public class ExternalViewerButton extends ImageButton {
    private static final Log.Tag TAG = new Log.Tag("ExtViewerButton");
    private final SparseArray<Cling> mClingMap;
    private int mState;

    public ExternalViewerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mState = 0;
        this.mClingMap = new SparseArray<>();
        updateClingVisibility();
    }

    @Override
    protected void onVisibilityChanged(View v, int visibility) {
        super.onVisibilityChanged(v, visibility);
        if (this.mClingMap != null) {
            updateClingVisibility();
        }
    }

    public void setClingForViewer(int viewerType, Cling cling) {
        if (cling == null) {
            Log.w(TAG, "Cannot set a null cling for viewer");
        } else {
            this.mClingMap.put(viewerType, cling);
            cling.setReferenceView(this);
        }
    }

    public void clearClingForViewer(int viewerType) {
        Cling cling = this.mClingMap.get(viewerType);
        if (cling == null) {
            Log.w(TAG, "Cling does not exist for the given viewer type: " + viewerType);
        }
        cling.setReferenceView(null);
        this.mClingMap.remove(viewerType);
    }

    public Cling getClingForViewer(int viewerType) {
        return this.mClingMap.get(viewerType);
    }

    public void setState(int state) {
        int newVisibility;
        this.mState = state;
        if (state == 0) {
            newVisibility = 8;
        } else {
            setImageResource(getViewButtonResource(state));
            newVisibility = 0;
        }
        if (newVisibility != getVisibility()) {
            setVisibility(newVisibility);
        } else if (newVisibility == 0) {
            updateClingVisibility();
        }
    }

    public void hideClings() {
        for (int i = 0; i < this.mClingMap.size(); i++) {
            this.mClingMap.valueAt(i).setVisibility(4);
        }
    }

    private int getViewButtonResource(int state) {
        switch (state) {
            case 1:
                return R.drawable.ic_view_photosphere;
            case 2:
                return R.drawable.ic_refocus_normal;
            default:
                return R.drawable.ic_control_play;
        }
    }

    public void updateClingVisibility() {
        Cling cling;
        hideClings();
        if (isShown() && (cling = this.mClingMap.get(this.mState)) != null) {
            cling.adjustPosition();
            cling.setVisibility(0);
        }
    }
}

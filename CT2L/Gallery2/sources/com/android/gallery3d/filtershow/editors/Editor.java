package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.SwapButton;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageShow;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import java.util.ArrayList;
import java.util.Collection;

public class Editor implements SeekBar.OnSeekBarChangeListener, SwapButton.SwapButtonListener {
    private Button mButton;
    protected Context mContext;
    Button mEditTitle;
    protected Button mFilterTitle;
    protected FrameLayout mFrameLayout;
    protected int mID;
    protected ImageShow mImageShow;
    protected SeekBar mSeekBar;
    protected View mView;
    public static byte SHOW_VALUE_UNDEFINED = -1;
    public static byte SHOW_VALUE_OFF = 0;
    public static byte SHOW_VALUE_INT = 1;
    private final String LOGTAG = "Editor";
    protected boolean mChangesGeometry = false;
    protected FilterRepresentation mLocalRepresentation = null;
    protected byte mShowParameter = SHOW_VALUE_UNDEFINED;

    public static void hackFixStrings(Menu menu) {
        int count = menu.size();
        for (int i = 0; i < count; i++) {
            MenuItem item = menu.getItem(i);
            item.setTitle(item.getTitle().toString().toUpperCase());
        }
    }

    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        return effectName.toUpperCase() + " " + parameterValue;
    }

    protected Editor(int id) {
        this.mID = id;
    }

    public int getID() {
        return this.mID;
    }

    public boolean showsSeekBar() {
        return true;
    }

    public void setUpEditorUI(View actionButton, View editControl, Button editTitle, Button stateButton) {
        this.mEditTitle = editTitle;
        this.mFilterTitle = stateButton;
        this.mButton = editTitle;
        MasterImage.getImage().resetGeometryImages(false);
        setUtilityPanelUI(actionButton, editControl);
    }

    public boolean showsPopupIndicator() {
        return false;
    }

    public void setUtilityPanelUI(View actionButton, View editControl) {
        Context context = editControl.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        LinearLayout lp = (LinearLayout) inflater.inflate(R.layout.filtershow_seekbar, (ViewGroup) editControl, true);
        this.mSeekBar = (SeekBar) lp.findViewById(R.id.primarySeekBar);
        this.mSeekBar.setOnSeekBarChangeListener(this);
        this.mSeekBar.setVisibility(8);
        if (context.getResources().getConfiguration().orientation == 1 && showsSeekBar()) {
            this.mSeekBar.setVisibility(0);
        }
        if (this.mButton != null) {
            setMenuIcon(showsPopupIndicator());
        }
    }

    @Override
    public void onProgressChanged(SeekBar sbar, int progress, boolean arg2) {
    }

    public void createEditor(Context context, FrameLayout frameLayout) {
        this.mContext = context;
        this.mFrameLayout = frameLayout;
        this.mLocalRepresentation = null;
    }

    protected void unpack(int viewid, int layoutid) {
        if (this.mView == null) {
            this.mView = this.mFrameLayout.findViewById(viewid);
            if (this.mView == null) {
                LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
                this.mView = inflater.inflate(layoutid, (ViewGroup) this.mFrameLayout, false);
                this.mFrameLayout.addView(this.mView, this.mView.getLayoutParams());
            }
        }
        this.mImageShow = findImageShow(this.mView);
    }

    private ImageShow findImageShow(View view) {
        if (view instanceof ImageShow) {
            return (ImageShow) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup vg = (ViewGroup) view;
        int n = vg.getChildCount();
        for (int i = 0; i < n; i++) {
            View v = vg.getChildAt(i);
            if (v instanceof ImageShow) {
                return (ImageShow) v;
            }
            if (v instanceof ViewGroup) {
                return findImageShow(v);
            }
        }
        return null;
    }

    public View getTopLevelView() {
        return this.mView;
    }

    public ImageShow getImageShow() {
        return this.mImageShow;
    }

    public void setVisibility(int visible) {
        this.mView.setVisibility(visible);
    }

    public FilterRepresentation getLocalRepresentation() {
        if (this.mLocalRepresentation == null) {
            ImagePreset preset = MasterImage.getImage().getPreset();
            FilterRepresentation filterRepresentation = MasterImage.getImage().getCurrentFilterRepresentation();
            this.mLocalRepresentation = preset.getFilterRepresentationCopyFrom(filterRepresentation);
            if (this.mShowParameter == SHOW_VALUE_UNDEFINED && filterRepresentation != null) {
                boolean show = filterRepresentation.showParameterValue();
                this.mShowParameter = show ? SHOW_VALUE_INT : SHOW_VALUE_OFF;
            }
        }
        return this.mLocalRepresentation;
    }

    public void commitLocalRepresentation() {
        commitLocalRepresentation(getLocalRepresentation());
    }

    public void commitLocalRepresentation(FilterRepresentation rep) {
        ArrayList<FilterRepresentation> filter = new ArrayList<>(1);
        filter.add(rep);
        commitLocalRepresentation(filter);
    }

    public void commitLocalRepresentation(Collection<FilterRepresentation> reps) {
        ImagePreset preset = MasterImage.getImage().getPreset();
        preset.updateFilterRepresentations(reps);
        if (this.mButton != null) {
            updateText();
        }
        if (this.mChangesGeometry) {
            MasterImage.getImage().resetGeometryImages(true);
        }
        MasterImage.getImage().invalidateFiltersOnly();
        preset.fillImageStateAdapter(MasterImage.getImage().getState());
    }

    public void finalApplyCalled() {
        commitLocalRepresentation();
    }

    protected void updateText() {
        String s = "";
        if (this.mLocalRepresentation != null) {
            s = this.mContext.getString(this.mLocalRepresentation.getTextId());
        }
        this.mButton.setText(calculateUserMessage(this.mContext, s, ""));
    }

    public void reflectCurrentFilter() {
        this.mLocalRepresentation = null;
        FilterRepresentation representation = getLocalRepresentation();
        if (representation != null && this.mFilterTitle != null && representation.getTextId() != 0) {
            String text = this.mContext.getString(representation.getTextId()).toUpperCase();
            this.mFilterTitle.setText(text);
            updateText();
        }
    }

    public boolean useUtilityPanel() {
        return true;
    }

    public void openUtilityPanel(LinearLayout mAccessoryViewList) {
        setMenuIcon(showsPopupIndicator());
        if (this.mImageShow != null) {
            this.mImageShow.openUtilityPanel(mAccessoryViewList);
        }
    }

    protected void setMenuIcon(boolean on) {
        this.mEditTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, on ? R.drawable.filtershow_menu_marker_rtl : 0, 0);
    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void swapLeft(MenuItem item) {
    }

    @Override
    public void swapRight(MenuItem item) {
    }

    public void detach() {
        if (this.mImageShow != null) {
            this.mImageShow.detach();
        }
    }
}

package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.controller.BasicParameterInt;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterVignetteRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageVignette;

public class EditorVignette extends ParametricEditor {
    private SwapButton mButton;
    private SeekBar mContrastBar;
    private TextView mContrastValue;
    String mCurrentlyEditing;
    private SeekBar mExposureBar;
    private TextView mExposureValue;
    private SeekBar mFalloffBar;
    private TextView mFalloffValue;
    private final Handler mHandler;
    ImageVignette mImageVignette;
    int[] mMenuStrings;
    private SeekBar mSaturationBar;
    private TextView mSaturationValue;
    private SeekBar mVignetteBar;
    private TextView mVignetteValue;

    public EditorVignette() {
        super(R.id.vignetteEditor, R.layout.filtershow_vignette_editor, R.id.imageVignette);
        this.mHandler = new Handler();
        this.mMenuStrings = new int[]{R.string.vignette_main, R.string.vignette_exposure, R.string.vignette_saturation, R.string.vignette_contrast, R.string.vignette_falloff};
        this.mCurrentlyEditing = null;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        this.mImageVignette = (ImageVignette) this.mImageShow;
        this.mImageVignette.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        if (useCompact(this.mContext)) {
            super.reflectCurrentFilter();
            FilterRepresentation rep = getLocalRepresentation();
            if (rep != null && (getLocalRepresentation() instanceof FilterVignetteRepresentation)) {
                FilterVignetteRepresentation drawRep = (FilterVignetteRepresentation) rep;
                this.mImageVignette.setRepresentation(drawRep);
            }
            updateText();
            return;
        }
        this.mLocalRepresentation = null;
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterVignetteRepresentation)) {
            FilterVignetteRepresentation rep2 = (FilterVignetteRepresentation) getLocalRepresentation();
            int[] mode = {0, 1, 2, 3, 4};
            SeekBar[] sliders = {this.mVignetteBar, this.mExposureBar, this.mSaturationBar, this.mContrastBar, this.mFalloffBar};
            TextView[] label = {this.mVignetteValue, this.mExposureValue, this.mSaturationValue, this.mContrastValue, this.mFalloffValue};
            for (int i = 0; i < mode.length; i++) {
                BasicParameterInt p = rep2.getFilterParameter(mode[i]);
                int value = p.getValue();
                sliders[i].setMax(p.getMaximum() - p.getMinimum());
                sliders[i].setProgress(value - p.getMinimum());
                label[i].setText("" + value);
            }
            this.mImageVignette.setRepresentation(rep2);
            String text = this.mContext.getString(rep2.getTextId()).toUpperCase();
            this.mFilterTitle.setText(text);
            updateText();
        }
    }

    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || !(rep instanceof FilterVignetteRepresentation)) {
            return "";
        }
        FilterVignetteRepresentation csrep = (FilterVignetteRepresentation) rep;
        int mode = csrep.getParameterMode();
        String paramString = this.mContext.getString(this.mMenuStrings[mode]);
        int val = csrep.getCurrentParameter();
        return paramString + (val > 0 ? " +" : " ") + val;
    }

    @Override
    public void openUtilityPanel(LinearLayout accessoryViewList) {
        this.mButton = (SwapButton) accessoryViewList.findViewById(R.id.applyEffect);
        this.mButton.setText(this.mContext.getString(R.string.vignette_main));
        if (useCompact(this.mContext)) {
            final PopupMenu popupMenu = new PopupMenu(this.mImageShow.getActivity(), this.mButton);
            popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_vignette, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    EditorVignette.this.selectMenuItem(item);
                    return true;
                }
            });
            this.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    popupMenu.show();
                    ((FilterShowActivity) EditorVignette.this.mContext).onShowMenu(popupMenu);
                }
            });
            this.mButton.setListener(this);
            FilterVignetteRepresentation csrep = getVignetteRep();
            String menuString = this.mContext.getString(this.mMenuStrings[0]);
            switchToMode(csrep, 0, menuString);
            return;
        }
        this.mButton.setText(this.mContext.getString(R.string.vignette_main));
    }

    @Override
    public void setUtilityPanelUI(View actionButton, View editControl) {
        if (useCompact(this.mContext)) {
            super.setUtilityPanelUI(actionButton, editControl);
            return;
        }
        this.mActionButton = actionButton;
        this.mEditControl = editControl;
        this.mEditTitle.setCompoundDrawables(null, null, null, null);
        LinearLayout group = (LinearLayout) editControl;
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        LinearLayout controls = (LinearLayout) inflater.inflate(R.layout.filtershow_vignette_controls, (ViewGroup) group, false);
        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        controls.setLayoutParams(lp);
        group.removeAllViews();
        group.addView(controls);
        this.mVignetteBar = (SeekBar) controls.findViewById(R.id.mainVignetteSeekbar);
        this.mVignetteBar.setMax(200);
        this.mVignetteBar.setOnSeekBarChangeListener(this);
        this.mVignetteValue = (TextView) controls.findViewById(R.id.mainVignetteValue);
        this.mExposureBar = (SeekBar) controls.findViewById(R.id.exposureSeekBar);
        this.mExposureBar.setMax(200);
        this.mExposureBar.setOnSeekBarChangeListener(this);
        this.mExposureValue = (TextView) controls.findViewById(R.id.exposureValue);
        this.mSaturationBar = (SeekBar) controls.findViewById(R.id.saturationSeekBar);
        this.mSaturationBar.setMax(200);
        this.mSaturationBar.setOnSeekBarChangeListener(this);
        this.mSaturationValue = (TextView) controls.findViewById(R.id.saturationValue);
        this.mContrastBar = (SeekBar) controls.findViewById(R.id.contrastSeekBar);
        this.mContrastBar.setMax(200);
        this.mContrastBar.setOnSeekBarChangeListener(this);
        this.mContrastValue = (TextView) controls.findViewById(R.id.contrastValue);
        this.mFalloffBar = (SeekBar) controls.findViewById(R.id.falloffSeekBar);
        this.mFalloffBar.setMax(200);
        this.mFalloffBar.setOnSeekBarChangeListener(this);
        this.mFalloffValue = (TextView) controls.findViewById(R.id.falloffValue);
    }

    public int getParameterIndex(int id) {
        switch (id) {
            case R.id.editor_vignette_main:
                return 0;
            case R.id.editor_vignette_falloff:
                return 4;
            case R.id.editor_vignette_contrast:
                return 3;
            case R.id.editor_vignette_saturation:
                return 2;
            case R.id.editor_vignette_exposure:
                return 1;
            default:
                return -1;
        }
    }

    @Override
    public void detach() {
        if (this.mButton != null) {
            this.mButton.setListener(null);
            this.mButton.setOnClickListener(null);
        }
    }

    private void updateSeekBar(FilterVignetteRepresentation rep) {
        this.mControl.updateUI();
    }

    @Override
    protected Parameter getParameterToEdit(FilterRepresentation rep) {
        if (!(rep instanceof FilterVignetteRepresentation)) {
            return null;
        }
        FilterVignetteRepresentation csrep = (FilterVignetteRepresentation) rep;
        return csrep.getFilterParameter(csrep.getParameterMode());
    }

    private FilterVignetteRepresentation getVignetteRep() {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || !(rep instanceof FilterVignetteRepresentation)) {
            return null;
        }
        return (FilterVignetteRepresentation) rep;
    }

    protected void selectMenuItem(MenuItem item) {
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterVignetteRepresentation)) {
            FilterVignetteRepresentation csrep = (FilterVignetteRepresentation) getLocalRepresentation();
            switchToMode(csrep, getParameterIndex(item.getItemId()), item.getTitle().toString());
        }
    }

    protected void switchToMode(FilterVignetteRepresentation csrep, int mode, String title) {
        if (csrep != null) {
            csrep.setParameterMode(mode);
            this.mCurrentlyEditing = title;
            this.mButton.setText(this.mCurrentlyEditing);
            Parameter param = getParameterToEdit(csrep);
            control(param, this.mEditControl);
            updateSeekBar(csrep);
            this.mView.invalidate();
        }
    }

    @Override
    public void onProgressChanged(SeekBar sbar, int progress, boolean arg2) {
        FilterVignetteRepresentation rep = getVignetteRep();
        int value = progress;
        switch (sbar.getId()) {
            case R.id.exposureSeekBar:
                rep.setParameterMode(1);
                BasicParameterInt p = rep.getFilterParameter(rep.getParameterMode());
                value += p.getMinimum();
                this.mExposureValue.setText("" + value);
                break;
            case R.id.saturationSeekBar:
                rep.setParameterMode(2);
                BasicParameterInt p2 = rep.getFilterParameter(rep.getParameterMode());
                value += p2.getMinimum();
                this.mSaturationValue.setText("" + value);
                break;
            case R.id.contrastSeekBar:
                rep.setParameterMode(3);
                BasicParameterInt p3 = rep.getFilterParameter(rep.getParameterMode());
                value += p3.getMinimum();
                this.mContrastValue.setText("" + value);
                break;
            case R.id.falloffSeekBar:
                rep.setParameterMode(4);
                BasicParameterInt p4 = rep.getFilterParameter(rep.getParameterMode());
                value += p4.getMinimum();
                this.mFalloffValue.setText("" + value);
                break;
            case R.id.mainVignetteSeekbar:
                rep.setParameterMode(0);
                BasicParameterInt p5 = rep.getFilterParameter(rep.getParameterMode());
                value += p5.getMinimum();
                this.mVignetteValue.setText("" + value);
                break;
        }
        rep.setCurrentParameter(value);
        commitLocalRepresentation();
    }

    @Override
    public void swapLeft(MenuItem item) {
        super.swapLeft(item);
        this.mButton.setTranslationX(0.0f);
        this.mButton.animate().translationX(this.mButton.getWidth()).setDuration(SwapButton.ANIM_DURATION);
        Runnable updateButton = new Runnable() {
            @Override
            public void run() {
                EditorVignette.this.mButton.animate().cancel();
                EditorVignette.this.mButton.setTranslationX(0.0f);
            }
        };
        this.mHandler.postDelayed(updateButton, SwapButton.ANIM_DURATION);
        selectMenuItem(item);
    }

    @Override
    public void swapRight(MenuItem item) {
        super.swapRight(item);
        this.mButton.setTranslationX(0.0f);
        this.mButton.animate().translationX(-this.mButton.getWidth()).setDuration(SwapButton.ANIM_DURATION);
        Runnable updateButton = new Runnable() {
            @Override
            public void run() {
                EditorVignette.this.mButton.animate().cancel();
                EditorVignette.this.mButton.setTranslationX(0.0f);
            }
        };
        this.mHandler.postDelayed(updateButton, SwapButton.ANIM_DURATION);
        selectMenuItem(item);
    }
}

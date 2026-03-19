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
            FilterRepresentation localRepresentation = getLocalRepresentation();
            if (localRepresentation != null && (getLocalRepresentation() instanceof FilterVignetteRepresentation)) {
                this.mImageVignette.setRepresentation((FilterVignetteRepresentation) localRepresentation);
            }
            updateText();
            return;
        }
        this.mLocalRepresentation = null;
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterVignetteRepresentation)) {
            FilterVignetteRepresentation filterVignetteRepresentation = (FilterVignetteRepresentation) getLocalRepresentation();
            int[] iArr = {0, 1, 2, 3, 4};
            SeekBar[] seekBarArr = {this.mVignetteBar, this.mExposureBar, this.mSaturationBar, this.mContrastBar, this.mFalloffBar};
            TextView[] textViewArr = {this.mVignetteValue, this.mExposureValue, this.mSaturationValue, this.mContrastValue, this.mFalloffValue};
            for (int i = 0; i < iArr.length; i++) {
                BasicParameterInt filterParameter = filterVignetteRepresentation.getFilterParameter(iArr[i]);
                int value = filterParameter.getValue();
                seekBarArr[i].setMax(filterParameter.getMaximum() - filterParameter.getMinimum());
                seekBarArr[i].setProgress(value - filterParameter.getMinimum());
                textViewArr[i].setText("" + value);
            }
            this.mImageVignette.setRepresentation(filterVignetteRepresentation);
            this.mFilterTitle.setText(this.mContext.getString(filterVignetteRepresentation.getTextId()).toUpperCase());
            updateText();
        }
    }

    @Override
    public String calculateUserMessage(Context context, String str, Object obj) {
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation == 0 || !(localRepresentation instanceof FilterVignetteRepresentation)) {
            return "";
        }
        String string = this.mContext.getString(this.mMenuStrings[localRepresentation.getParameterMode()]);
        int currentParameter = localRepresentation.getCurrentParameter();
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        sb.append(currentParameter > 0 ? " +" : " ");
        sb.append(currentParameter);
        return sb.toString();
    }

    @Override
    public void openUtilityPanel(LinearLayout linearLayout) {
        this.mButton = (SwapButton) linearLayout.findViewById(R.id.applyEffect);
        this.mButton.setText(this.mContext.getString(R.string.vignette_main));
        if (useCompact(this.mContext)) {
            final PopupMenu popupMenu = new PopupMenu(this.mImageShow.getActivity(), this.mButton);
            popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_vignette, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    EditorVignette.this.selectMenuItem(menuItem);
                    return true;
                }
            });
            this.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popupMenu.show();
                    ((FilterShowActivity) EditorVignette.this.mContext).onShowMenu(popupMenu);
                }
            });
            this.mButton.setListener(this);
            switchToMode(getVignetteRep(), 0, this.mContext.getString(this.mMenuStrings[0]));
            return;
        }
        this.mButton.setText(this.mContext.getString(R.string.vignette_main));
    }

    @Override
    public void setUtilityPanelUI(View view, View view2) {
        if (useCompact(this.mContext)) {
            super.setUtilityPanelUI(view, view2);
            return;
        }
        this.mActionButton = view;
        this.mEditControl = view2;
        this.mEditTitle.setCompoundDrawables(null, null, null, null);
        LinearLayout linearLayout = (LinearLayout) view2;
        LinearLayout linearLayout2 = (LinearLayout) ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.filtershow_vignette_controls, (ViewGroup) linearLayout, false);
        linearLayout2.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        linearLayout.removeAllViews();
        linearLayout.addView(linearLayout2);
        this.mVignetteBar = (SeekBar) linearLayout2.findViewById(R.id.mainVignetteSeekbar);
        this.mVignetteBar.setMax(200);
        this.mVignetteBar.setOnSeekBarChangeListener(this);
        this.mVignetteValue = (TextView) linearLayout2.findViewById(R.id.mainVignetteValue);
        this.mExposureBar = (SeekBar) linearLayout2.findViewById(R.id.exposureSeekBar);
        this.mExposureBar.setMax(200);
        this.mExposureBar.setOnSeekBarChangeListener(this);
        this.mExposureValue = (TextView) linearLayout2.findViewById(R.id.exposureValue);
        this.mSaturationBar = (SeekBar) linearLayout2.findViewById(R.id.saturationSeekBar);
        this.mSaturationBar.setMax(200);
        this.mSaturationBar.setOnSeekBarChangeListener(this);
        this.mSaturationValue = (TextView) linearLayout2.findViewById(R.id.saturationValue);
        this.mContrastBar = (SeekBar) linearLayout2.findViewById(R.id.contrastSeekBar);
        this.mContrastBar.setMax(200);
        this.mContrastBar.setOnSeekBarChangeListener(this);
        this.mContrastValue = (TextView) linearLayout2.findViewById(R.id.contrastValue);
        this.mFalloffBar = (SeekBar) linearLayout2.findViewById(R.id.falloffSeekBar);
        this.mFalloffBar.setMax(200);
        this.mFalloffBar.setOnSeekBarChangeListener(this);
        this.mFalloffValue = (TextView) linearLayout2.findViewById(R.id.falloffValue);
    }

    public int getParameterIndex(int i) {
        switch (i) {
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
        if (this.mButton == null) {
            return;
        }
        this.mButton.setListener(null);
        this.mButton.setOnClickListener(null);
    }

    private void updateSeekBar(FilterVignetteRepresentation filterVignetteRepresentation) {
        this.mControl.updateUI();
    }

    @Override
    protected Parameter getParameterToEdit(FilterRepresentation filterRepresentation) {
        if (filterRepresentation instanceof FilterVignetteRepresentation) {
            return filterRepresentation.getFilterParameter(filterRepresentation.getParameterMode());
        }
        return null;
    }

    private FilterVignetteRepresentation getVignetteRep() {
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation != 0 && (localRepresentation instanceof FilterVignetteRepresentation)) {
            return localRepresentation;
        }
        return null;
    }

    protected void selectMenuItem(MenuItem menuItem) {
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterVignetteRepresentation)) {
            switchToMode((FilterVignetteRepresentation) getLocalRepresentation(), getParameterIndex(menuItem.getItemId()), menuItem.getTitle().toString());
        }
    }

    protected void switchToMode(FilterVignetteRepresentation filterVignetteRepresentation, int i, String str) {
        if (filterVignetteRepresentation == null) {
            return;
        }
        filterVignetteRepresentation.setParameterMode(i);
        this.mCurrentlyEditing = str;
        this.mButton.setText(this.mCurrentlyEditing);
        control(getParameterToEdit(filterVignetteRepresentation), this.mEditControl);
        updateSeekBar(filterVignetteRepresentation);
        this.mView.invalidate();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        FilterVignetteRepresentation vignetteRep = getVignetteRep();
        switch (seekBar.getId()) {
            case R.id.exposureSeekBar:
                vignetteRep.setParameterMode(1);
                i += vignetteRep.getFilterParameter(vignetteRep.getParameterMode()).getMinimum();
                this.mExposureValue.setText("" + i);
                break;
            case R.id.saturationSeekBar:
                vignetteRep.setParameterMode(2);
                i += vignetteRep.getFilterParameter(vignetteRep.getParameterMode()).getMinimum();
                this.mSaturationValue.setText("" + i);
                break;
            case R.id.contrastSeekBar:
                vignetteRep.setParameterMode(3);
                i += vignetteRep.getFilterParameter(vignetteRep.getParameterMode()).getMinimum();
                this.mContrastValue.setText("" + i);
                break;
            case R.id.falloffSeekBar:
                vignetteRep.setParameterMode(4);
                i += vignetteRep.getFilterParameter(vignetteRep.getParameterMode()).getMinimum();
                this.mFalloffValue.setText("" + i);
                break;
            case R.id.mainVignetteSeekbar:
                vignetteRep.setParameterMode(0);
                i += vignetteRep.getFilterParameter(vignetteRep.getParameterMode()).getMinimum();
                this.mVignetteValue.setText("" + i);
                break;
        }
        vignetteRep.setCurrentParameter(i);
        commitLocalRepresentation();
    }

    @Override
    public void swapLeft(MenuItem menuItem) {
        super.swapLeft(menuItem);
        this.mButton.setTranslationX(0.0f);
        this.mButton.animate().translationX(this.mButton.getWidth()).setDuration(SwapButton.ANIM_DURATION);
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                EditorVignette.this.mButton.animate().cancel();
                EditorVignette.this.mButton.setTranslationX(0.0f);
            }
        }, SwapButton.ANIM_DURATION);
        selectMenuItem(menuItem);
    }

    @Override
    public void swapRight(MenuItem menuItem) {
        super.swapRight(menuItem);
        this.mButton.setTranslationX(0.0f);
        this.mButton.animate().translationX(-this.mButton.getWidth()).setDuration(SwapButton.ANIM_DURATION);
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                EditorVignette.this.mButton.animate().cancel();
                EditorVignette.this.mButton.setTranslationX(0.0f);
            }
        }, SwapButton.ANIM_DURATION);
        selectMenuItem(menuItem);
    }
}

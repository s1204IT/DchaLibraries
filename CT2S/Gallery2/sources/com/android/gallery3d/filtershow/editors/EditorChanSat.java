package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.controller.BasicParameterStyle;
import com.android.gallery3d.filtershow.controller.BitmapCaller;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.filters.FilterChanSatRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class EditorChanSat extends ParametricEditor implements SeekBar.OnSeekBarChangeListener, FilterView {
    private final String LOGTAG;
    private SeekBar mBlueBar;
    private TextView mBlueValue;
    private SwapButton mButton;
    String mCurrentlyEditing;
    private SeekBar mCyanBar;
    private TextView mCyanValue;
    private SeekBar mGreenBar;
    private TextView mGreenValue;
    private final Handler mHandler;
    private SeekBar mMagentaBar;
    private TextView mMagentaValue;
    private SeekBar mMainBar;
    private TextView mMainValue;
    int[] mMenuStrings;
    private SeekBar mRedBar;
    private TextView mRedValue;
    private SeekBar mYellowBar;
    private TextView mYellowValue;

    public EditorChanSat() {
        super(R.id.editorChanSat, R.layout.filtershow_default_editor, R.id.basicEditor);
        this.LOGTAG = "EditorGrunge";
        this.mHandler = new Handler();
        this.mMenuStrings = new int[]{R.string.editor_chan_sat_main, R.string.editor_chan_sat_red, R.string.editor_chan_sat_yellow, R.string.editor_chan_sat_green, R.string.editor_chan_sat_cyan, R.string.editor_chan_sat_blue, R.string.editor_chan_sat_magenta};
        this.mCurrentlyEditing = null;
    }

    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || !(rep instanceof FilterChanSatRepresentation)) {
            return "";
        }
        FilterChanSatRepresentation csrep = (FilterChanSatRepresentation) rep;
        int mode = csrep.getParameterMode();
        String paramString = this.mContext.getString(this.mMenuStrings[mode]);
        int val = csrep.getCurrentParameter();
        return paramString + (val > 0 ? " +" : " ") + val;
    }

    @Override
    public void openUtilityPanel(LinearLayout accessoryViewList) {
        this.mButton = (SwapButton) accessoryViewList.findViewById(R.id.applyEffect);
        this.mButton.setText(this.mContext.getString(R.string.editor_chan_sat_main));
        if (useCompact(this.mContext)) {
            final PopupMenu popupMenu = new PopupMenu(this.mImageShow.getActivity(), this.mButton);
            popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_chan_sat, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    EditorChanSat.this.selectMenuItem(item);
                    return true;
                }
            });
            this.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    popupMenu.show();
                    ((FilterShowActivity) EditorChanSat.this.mContext).onShowMenu(popupMenu);
                }
            });
            this.mButton.setListener(this);
            FilterChanSatRepresentation csrep = getChanSatRep();
            String menuString = this.mContext.getString(this.mMenuStrings[0]);
            switchToMode(csrep, 0, menuString);
            return;
        }
        this.mButton.setText(this.mContext.getString(R.string.saturation));
    }

    @Override
    public void reflectCurrentFilter() {
        if (useCompact(this.mContext)) {
            super.reflectCurrentFilter();
            updateText();
            return;
        }
        this.mLocalRepresentation = null;
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterChanSatRepresentation)) {
            FilterChanSatRepresentation rep = (FilterChanSatRepresentation) getLocalRepresentation();
            int value = rep.getValue(0);
            this.mMainBar.setProgress(value + 100);
            this.mMainValue.setText("" + value);
            int value2 = rep.getValue(1);
            this.mRedBar.setProgress(value2 + 100);
            this.mRedValue.setText("" + value2);
            int value3 = rep.getValue(2);
            this.mYellowBar.setProgress(value3 + 100);
            this.mYellowValue.setText("" + value3);
            int value4 = rep.getValue(3);
            this.mGreenBar.setProgress(value4 + 100);
            this.mGreenValue.setText("" + value4);
            int value5 = rep.getValue(4);
            this.mCyanBar.setProgress(value5 + 100);
            this.mCyanValue.setText("" + value5);
            int value6 = rep.getValue(5);
            this.mBlueBar.setProgress(value6 + 100);
            this.mBlueValue.setText("" + value6);
            int value7 = rep.getValue(6);
            this.mMagentaBar.setProgress(value7 + 100);
            this.mMagentaValue.setText("" + value7);
            String text = this.mContext.getString(rep.getTextId()).toUpperCase();
            this.mFilterTitle.setText(text);
            updateText();
        }
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
        LinearLayout controls = (LinearLayout) inflater.inflate(R.layout.filtershow_saturation_controls, (ViewGroup) group, false);
        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        controls.setLayoutParams(lp);
        group.removeAllViews();
        group.addView(controls);
        this.mMainBar = (SeekBar) controls.findViewById(R.id.mainSeekbar);
        this.mMainBar.setMax(200);
        this.mMainBar.setOnSeekBarChangeListener(this);
        this.mMainValue = (TextView) controls.findViewById(R.id.mainValue);
        this.mRedBar = (SeekBar) controls.findViewById(R.id.redSeekBar);
        this.mRedBar.setMax(200);
        this.mRedBar.setOnSeekBarChangeListener(this);
        this.mRedValue = (TextView) controls.findViewById(R.id.redValue);
        this.mYellowBar = (SeekBar) controls.findViewById(R.id.yellowSeekBar);
        this.mYellowBar.setMax(200);
        this.mYellowBar.setOnSeekBarChangeListener(this);
        this.mYellowValue = (TextView) controls.findViewById(R.id.yellowValue);
        this.mGreenBar = (SeekBar) controls.findViewById(R.id.greenSeekBar);
        this.mGreenBar.setMax(200);
        this.mGreenBar.setOnSeekBarChangeListener(this);
        this.mGreenValue = (TextView) controls.findViewById(R.id.greenValue);
        this.mCyanBar = (SeekBar) controls.findViewById(R.id.cyanSeekBar);
        this.mCyanBar.setMax(200);
        this.mCyanBar.setOnSeekBarChangeListener(this);
        this.mCyanValue = (TextView) controls.findViewById(R.id.cyanValue);
        this.mBlueBar = (SeekBar) controls.findViewById(R.id.blueSeekBar);
        this.mBlueBar.setMax(200);
        this.mBlueBar.setOnSeekBarChangeListener(this);
        this.mBlueValue = (TextView) controls.findViewById(R.id.blueValue);
        this.mMagentaBar = (SeekBar) controls.findViewById(R.id.magentaSeekBar);
        this.mMagentaBar.setMax(200);
        this.mMagentaBar.setOnSeekBarChangeListener(this);
        this.mMagentaValue = (TextView) controls.findViewById(R.id.magentaValue);
    }

    public int getParameterIndex(int id) {
        switch (id) {
            case R.id.editor_chan_sat_main:
                return 0;
            case R.id.editor_chan_sat_red:
                return 1;
            case R.id.editor_chan_sat_yellow:
                return 2;
            case R.id.editor_chan_sat_green:
                return 3;
            case R.id.editor_chan_sat_cyan:
                return 4;
            case R.id.editor_chan_sat_blue:
                return 5;
            case R.id.editor_chan_sat_magenta:
                return 6;
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

    private void updateSeekBar(FilterChanSatRepresentation rep) {
        this.mControl.updateUI();
    }

    @Override
    protected Parameter getParameterToEdit(FilterRepresentation rep) {
        if (!(rep instanceof FilterChanSatRepresentation)) {
            return null;
        }
        FilterChanSatRepresentation csrep = (FilterChanSatRepresentation) rep;
        Parameter param = csrep.getFilterParameter(csrep.getParameterMode());
        if (param instanceof BasicParameterStyle) {
            param.setFilterView(this);
            return param;
        }
        return param;
    }

    private FilterChanSatRepresentation getChanSatRep() {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || !(rep instanceof FilterChanSatRepresentation)) {
            return null;
        }
        return (FilterChanSatRepresentation) rep;
    }

    @Override
    public void computeIcon(int n, BitmapCaller caller) {
        FilterChanSatRepresentation rep = getChanSatRep();
        if (rep != null) {
            FilterChanSatRepresentation rep2 = (FilterChanSatRepresentation) rep.copy();
            ImagePreset preset = new ImagePreset();
            preset.addFilter(rep2);
            Bitmap src = MasterImage.getImage().getThumbnailBitmap();
            caller.available(src);
        }
    }

    protected void selectMenuItem(MenuItem item) {
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterChanSatRepresentation)) {
            FilterChanSatRepresentation csrep = (FilterChanSatRepresentation) getLocalRepresentation();
            switchToMode(csrep, getParameterIndex(item.getItemId()), item.getTitle().toString());
        }
    }

    protected void switchToMode(FilterChanSatRepresentation csrep, int mode, String title) {
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
        FilterChanSatRepresentation rep = getChanSatRep();
        int value = progress - 100;
        switch (sbar.getId()) {
            case R.id.redSeekBar:
                rep.setParameterMode(1);
                this.mRedValue.setText("" + value);
                break;
            case R.id.yellowSeekBar:
                rep.setParameterMode(2);
                this.mYellowValue.setText("" + value);
                break;
            case R.id.greenSeekBar:
                rep.setParameterMode(3);
                this.mGreenValue.setText("" + value);
                break;
            case R.id.cyanSeekBar:
                rep.setParameterMode(4);
                this.mCyanValue.setText("" + value);
                break;
            case R.id.blueSeekBar:
                rep.setParameterMode(5);
                this.mBlueValue.setText("" + value);
                break;
            case R.id.magentaSeekBar:
                rep.setParameterMode(6);
                this.mMagentaValue.setText("" + value);
                break;
            case R.id.mainSeekbar:
                rep.setParameterMode(0);
                this.mMainValue.setText("" + value);
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
                EditorChanSat.this.mButton.animate().cancel();
                EditorChanSat.this.mButton.setTranslationX(0.0f);
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
                EditorChanSat.this.mButton.animate().cancel();
                EditorChanSat.this.mButton.setTranslationX(0.0f);
            }
        };
        this.mHandler.postDelayed(updateButton, SwapButton.ANIM_DURATION);
        selectMenuItem(item);
    }
}

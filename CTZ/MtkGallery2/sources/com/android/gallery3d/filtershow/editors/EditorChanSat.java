package com.android.gallery3d.filtershow.editors;

import android.content.Context;
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
    public String calculateUserMessage(Context context, String str, Object obj) {
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation == 0 || !(localRepresentation instanceof FilterChanSatRepresentation)) {
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
        this.mButton.setText(this.mContext.getString(R.string.editor_chan_sat_main));
        if (useCompact(this.mContext)) {
            final PopupMenu popupMenu = new PopupMenu(this.mImageShow.getActivity(), this.mButton);
            popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_chan_sat, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    EditorChanSat.this.selectMenuItem(menuItem);
                    return true;
                }
            });
            this.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popupMenu.show();
                    ((FilterShowActivity) EditorChanSat.this.mContext).onShowMenu(popupMenu);
                }
            });
            this.mButton.setListener(this);
            switchToMode(getChanSatRep(), 0, this.mContext.getString(this.mMenuStrings[0]));
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
            FilterChanSatRepresentation filterChanSatRepresentation = (FilterChanSatRepresentation) getLocalRepresentation();
            int value = filterChanSatRepresentation.getValue(0);
            this.mMainBar.setProgress(value + 100);
            this.mMainValue.setText("" + value);
            int value2 = filterChanSatRepresentation.getValue(1);
            this.mRedBar.setProgress(value2 + 100);
            this.mRedValue.setText("" + value2);
            int value3 = filterChanSatRepresentation.getValue(2);
            this.mYellowBar.setProgress(value3 + 100);
            this.mYellowValue.setText("" + value3);
            int value4 = filterChanSatRepresentation.getValue(3);
            this.mGreenBar.setProgress(value4 + 100);
            this.mGreenValue.setText("" + value4);
            int value5 = filterChanSatRepresentation.getValue(4);
            this.mCyanBar.setProgress(value5 + 100);
            this.mCyanValue.setText("" + value5);
            int value6 = filterChanSatRepresentation.getValue(5);
            this.mBlueBar.setProgress(value6 + 100);
            this.mBlueValue.setText("" + value6);
            int value7 = filterChanSatRepresentation.getValue(6);
            this.mMagentaBar.setProgress(value7 + 100);
            this.mMagentaValue.setText("" + value7);
            this.mFilterTitle.setText(this.mContext.getString(filterChanSatRepresentation.getTextId()).toUpperCase());
            updateText();
        }
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
        LinearLayout linearLayout2 = (LinearLayout) ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.filtershow_saturation_controls, (ViewGroup) linearLayout, false);
        linearLayout2.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        linearLayout.removeAllViews();
        linearLayout.addView(linearLayout2);
        this.mMainBar = (SeekBar) linearLayout2.findViewById(R.id.mainSeekbar);
        this.mMainBar.setMax(200);
        this.mMainBar.setOnSeekBarChangeListener(this);
        this.mMainValue = (TextView) linearLayout2.findViewById(R.id.mainValue);
        this.mRedBar = (SeekBar) linearLayout2.findViewById(R.id.redSeekBar);
        this.mRedBar.setMax(200);
        this.mRedBar.setOnSeekBarChangeListener(this);
        this.mRedValue = (TextView) linearLayout2.findViewById(R.id.redValue);
        this.mYellowBar = (SeekBar) linearLayout2.findViewById(R.id.yellowSeekBar);
        this.mYellowBar.setMax(200);
        this.mYellowBar.setOnSeekBarChangeListener(this);
        this.mYellowValue = (TextView) linearLayout2.findViewById(R.id.yellowValue);
        this.mGreenBar = (SeekBar) linearLayout2.findViewById(R.id.greenSeekBar);
        this.mGreenBar.setMax(200);
        this.mGreenBar.setOnSeekBarChangeListener(this);
        this.mGreenValue = (TextView) linearLayout2.findViewById(R.id.greenValue);
        this.mCyanBar = (SeekBar) linearLayout2.findViewById(R.id.cyanSeekBar);
        this.mCyanBar.setMax(200);
        this.mCyanBar.setOnSeekBarChangeListener(this);
        this.mCyanValue = (TextView) linearLayout2.findViewById(R.id.cyanValue);
        this.mBlueBar = (SeekBar) linearLayout2.findViewById(R.id.blueSeekBar);
        this.mBlueBar.setMax(200);
        this.mBlueBar.setOnSeekBarChangeListener(this);
        this.mBlueValue = (TextView) linearLayout2.findViewById(R.id.blueValue);
        this.mMagentaBar = (SeekBar) linearLayout2.findViewById(R.id.magentaSeekBar);
        this.mMagentaBar.setMax(200);
        this.mMagentaBar.setOnSeekBarChangeListener(this);
        this.mMagentaValue = (TextView) linearLayout2.findViewById(R.id.magentaValue);
    }

    public int getParameterIndex(int i) {
        switch (i) {
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
        if (this.mButton == null) {
            return;
        }
        this.mButton.setListener(null);
        this.mButton.setOnClickListener(null);
    }

    private void updateSeekBar(FilterChanSatRepresentation filterChanSatRepresentation) {
        this.mControl.updateUI();
    }

    @Override
    protected Parameter getParameterToEdit(FilterRepresentation filterRepresentation) {
        if (filterRepresentation instanceof FilterChanSatRepresentation) {
            Parameter filterParameter = filterRepresentation.getFilterParameter(filterRepresentation.getParameterMode());
            if (filterParameter instanceof BasicParameterStyle) {
                filterParameter.setFilterView(this);
            }
            return filterParameter;
        }
        return null;
    }

    private FilterChanSatRepresentation getChanSatRep() {
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation != 0 && (localRepresentation instanceof FilterChanSatRepresentation)) {
            return localRepresentation;
        }
        return null;
    }

    @Override
    public void computeIcon(int i, BitmapCaller bitmapCaller) {
        FilterChanSatRepresentation chanSatRep = getChanSatRep();
        if (chanSatRep == null) {
            return;
        }
        new ImagePreset().addFilter((FilterChanSatRepresentation) chanSatRep.copy());
        bitmapCaller.available(MasterImage.getImage().getThumbnailBitmap());
    }

    protected void selectMenuItem(MenuItem menuItem) {
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterChanSatRepresentation)) {
            switchToMode((FilterChanSatRepresentation) getLocalRepresentation(), getParameterIndex(menuItem.getItemId()), menuItem.getTitle().toString());
        }
    }

    protected void switchToMode(FilterChanSatRepresentation filterChanSatRepresentation, int i, String str) {
        if (filterChanSatRepresentation == null) {
            return;
        }
        filterChanSatRepresentation.setParameterMode(i);
        this.mCurrentlyEditing = str;
        this.mButton.setText(this.mCurrentlyEditing);
        control(getParameterToEdit(filterChanSatRepresentation), this.mEditControl);
        updateSeekBar(filterChanSatRepresentation);
        this.mView.invalidate();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        FilterChanSatRepresentation chanSatRep = getChanSatRep();
        int i2 = i - 100;
        switch (seekBar.getId()) {
            case R.id.redSeekBar:
                chanSatRep.setParameterMode(1);
                this.mRedValue.setText("" + i2);
                break;
            case R.id.yellowSeekBar:
                chanSatRep.setParameterMode(2);
                this.mYellowValue.setText("" + i2);
                break;
            case R.id.greenSeekBar:
                chanSatRep.setParameterMode(3);
                this.mGreenValue.setText("" + i2);
                break;
            case R.id.cyanSeekBar:
                chanSatRep.setParameterMode(4);
                this.mCyanValue.setText("" + i2);
                break;
            case R.id.blueSeekBar:
                chanSatRep.setParameterMode(5);
                this.mBlueValue.setText("" + i2);
                break;
            case R.id.magentaSeekBar:
                chanSatRep.setParameterMode(6);
                this.mMagentaValue.setText("" + i2);
                break;
            case R.id.mainSeekbar:
                chanSatRep.setParameterMode(0);
                this.mMainValue.setText("" + i2);
                break;
        }
        chanSatRep.setCurrentParameter(i2);
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
                EditorChanSat.this.mButton.animate().cancel();
                EditorChanSat.this.mButton.setTranslationX(0.0f);
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
                EditorChanSat.this.mButton.animate().cancel();
                EditorChanSat.this.mButton.setTranslationX(0.0f);
            }
        }, SwapButton.ANIM_DURATION);
        selectMenuItem(menuItem);
    }
}

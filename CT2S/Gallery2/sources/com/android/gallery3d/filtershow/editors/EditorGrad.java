package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.controller.Control;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.controller.ParameterActionAndInt;
import com.android.gallery3d.filtershow.filters.FilterGradRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageGrad;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class EditorGrad extends ParametricEditor implements SeekBar.OnSeekBarChangeListener, ParameterActionAndInt {
    ParamAdapter[] mAdapters;
    String mEffectName;
    ImageGrad mImageGrad;
    PopupMenu mPopupMenu;
    private int mSliderMode;

    public EditorGrad() {
        super(R.id.editorGrad, R.layout.filtershow_grad_editor, R.id.gradEditor);
        this.mEffectName = "";
        this.mSliderMode = 0;
        this.mAdapters = new ParamAdapter[3];
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        this.mImageGrad = (ImageGrad) this.mImageShow;
        this.mImageGrad.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        FilterRepresentation tmpRep = getLocalRepresentation();
        if (tmpRep instanceof FilterGradRepresentation) {
            FilterGradRepresentation rep = (FilterGradRepresentation) tmpRep;
            rep.showParameterValue();
            this.mImageGrad.setRepresentation(rep);
        }
    }

    public void updateSeekBar(FilterGradRepresentation rep) {
        if (ParametricEditor.useCompact(this.mContext)) {
            this.mControl.updateUI();
        } else {
            updateParameters();
        }
    }

    @Override
    public void onProgressChanged(SeekBar sbar, int progress, boolean arg2) {
        FilterRepresentation tmpRep = getLocalRepresentation();
        if (tmpRep instanceof FilterGradRepresentation) {
            FilterGradRepresentation rep = (FilterGradRepresentation) tmpRep;
            int min = rep.getParameterMin(this.mSliderMode);
            int value = progress + min;
            rep.setParameter(this.mSliderMode, value);
            this.mView.invalidate();
            commitLocalRepresentation();
        }
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        Button view = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        if (useCompact(this.mContext)) {
            view.setText(this.mContext.getString(R.string.editor_grad_brightness));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    EditorGrad.this.showPopupMenu(accessoryViewList);
                }
            });
            setUpPopupMenu(view);
            setEffectName();
            return;
        }
        view.setText(this.mContext.getString(R.string.grad));
    }

    private void updateMenuItems(FilterGradRepresentation rep) {
        rep.getNumberOfBands();
    }

    public void setEffectName() {
        if (this.mPopupMenu != null) {
            MenuItem item = this.mPopupMenu.getMenu().findItem(R.id.editor_grad_brightness);
            this.mEffectName = item.getTitle().toString();
        }
    }

    @Override
    public void setUtilityPanelUI(View actionButton, View editControl) {
        if (ParametricEditor.useCompact(this.mContext)) {
            super.setUtilityPanelUI(actionButton, editControl);
            return;
        }
        this.mSeekBar = (SeekBar) editControl.findViewById(R.id.primarySeekBar);
        if (this.mSeekBar != null) {
            this.mSeekBar.setVisibility(8);
        }
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        LinearLayout lp = (LinearLayout) inflater.inflate(R.layout.filtershow_grad_ui, (ViewGroup) editControl, true);
        this.mAdapters[0] = new ParamAdapter(R.id.gradContrastSeekBar, R.id.gradContrastValue, lp, 2);
        this.mAdapters[1] = new ParamAdapter(R.id.gradBrightnessSeekBar, R.id.gradBrightnessValue, lp, 0);
        this.mAdapters[2] = new ParamAdapter(R.id.gradSaturationSeekBar, R.id.gradSaturationValue, lp, 1);
        lp.findViewById(R.id.gradAddButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditorGrad.this.fireLeftAction();
            }
        });
        lp.findViewById(R.id.gradDelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditorGrad.this.fireRightAction();
            }
        });
        setMenuIcon(false);
    }

    public void updateParameters() {
        FilterGradRepresentation rep = getGradRepresentation();
        for (int i = 0; i < this.mAdapters.length; i++) {
            this.mAdapters[i].updateValues(rep);
        }
    }

    private class ParamAdapter implements SeekBar.OnSeekBarChangeListener {
        int mMode;
        SeekBar mSlider;
        TextView mTextView;
        int mMin = -100;
        int mMax = 100;

        public ParamAdapter(int seekId, int textId, LinearLayout layout, int mode) {
            this.mSlider = (SeekBar) layout.findViewById(seekId);
            this.mTextView = (TextView) layout.findViewById(textId);
            this.mSlider.setMax(this.mMax - this.mMin);
            this.mMode = mode;
            FilterGradRepresentation rep = EditorGrad.this.getGradRepresentation();
            if (rep != null) {
                updateValues(rep);
            }
            this.mSlider.setOnSeekBarChangeListener(this);
        }

        public void updateValues(FilterGradRepresentation rep) {
            int value = rep.getParameter(this.mMode);
            this.mTextView.setText(Integer.toString(value));
            this.mSlider.setProgress(value - this.mMin);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            FilterGradRepresentation rep = EditorGrad.this.getGradRepresentation();
            int value = progress + this.mMin;
            rep.setParameter(this.mMode, value);
            if (EditorGrad.this.mSliderMode != this.mMode) {
                EditorGrad.this.mSliderMode = this.mMode;
                EditorGrad.this.mEffectName = EditorGrad.this.mContext.getResources().getString(getModeNameid(this.mMode));
                EditorGrad.this.mEffectName = EditorGrad.this.mEffectName.toUpperCase();
            }
            this.mTextView.setText(Integer.toString(value));
            EditorGrad.this.mView.invalidate();
            EditorGrad.this.commitLocalRepresentation();
        }

        private int getModeNameid(int mode) {
            switch (mode) {
                case 0:
                    return R.string.editor_grad_brightness;
                case 1:
                    return R.string.editor_grad_saturation;
                case 2:
                    return R.string.editor_grad_contrast;
                default:
                    return 0;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    private void showPopupMenu(LinearLayout accessoryViewList) {
        Button button = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        if (button != null) {
            if (this.mPopupMenu == null) {
                setUpPopupMenu(button);
            }
            this.mPopupMenu.show();
            ((FilterShowActivity) this.mContext).onShowMenu(this.mPopupMenu);
        }
    }

    private void setUpPopupMenu(Button button) {
        this.mPopupMenu = new PopupMenu(this.mImageShow.getActivity(), button);
        this.mPopupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_grad, this.mPopupMenu.getMenu());
        FilterGradRepresentation rep = (FilterGradRepresentation) getLocalRepresentation();
        if (rep != null) {
            updateMenuItems(rep);
            hackFixStrings(this.mPopupMenu.getMenu());
            setEffectName();
            updateText();
            this.mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    FilterRepresentation tmpRep = EditorGrad.this.getLocalRepresentation();
                    if (tmpRep instanceof FilterGradRepresentation) {
                        FilterGradRepresentation rep2 = (FilterGradRepresentation) tmpRep;
                        int cmdID = item.getItemId();
                        switch (cmdID) {
                            case R.id.editor_grad_brightness:
                                EditorGrad.this.mSliderMode = 0;
                                EditorGrad.this.mEffectName = item.getTitle().toString();
                                break;
                            case R.id.editor_grad_saturation:
                                EditorGrad.this.mSliderMode = 1;
                                EditorGrad.this.mEffectName = item.getTitle().toString();
                                break;
                            case R.id.editor_grad_contrast:
                                EditorGrad.this.mSliderMode = 2;
                                EditorGrad.this.mEffectName = item.getTitle().toString();
                                break;
                        }
                        EditorGrad.this.updateMenuItems(rep2);
                        EditorGrad.this.updateSeekBar(rep2);
                        EditorGrad.this.commitLocalRepresentation();
                        EditorGrad.this.mView.invalidate();
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        FilterGradRepresentation rep = getGradRepresentation();
        if (rep == null) {
            return this.mEffectName;
        }
        int val = rep.getParameter(this.mSliderMode);
        return this.mEffectName.toUpperCase() + (val > 0 ? " +" : " ") + val;
    }

    private FilterGradRepresentation getGradRepresentation() {
        FilterRepresentation tmpRep = getLocalRepresentation();
        if (tmpRep instanceof FilterGradRepresentation) {
            return (FilterGradRepresentation) tmpRep;
        }
        return null;
    }

    @Override
    public int getMaximum() {
        FilterGradRepresentation rep = getGradRepresentation();
        if (rep == null) {
            return 0;
        }
        return rep.getParameterMax(this.mSliderMode);
    }

    @Override
    public int getMinimum() {
        FilterGradRepresentation rep = getGradRepresentation();
        if (rep == null) {
            return 0;
        }
        return rep.getParameterMin(this.mSliderMode);
    }

    @Override
    public int getValue() {
        FilterGradRepresentation rep = getGradRepresentation();
        if (rep == null) {
            return 0;
        }
        return rep.getParameter(this.mSliderMode);
    }

    @Override
    public void setValue(int value) {
        FilterGradRepresentation rep = getGradRepresentation();
        if (rep != null) {
            rep.setParameter(this.mSliderMode, value);
        }
    }

    @Override
    public String getParameterName() {
        return this.mEffectName;
    }

    @Override
    public String getParameterType() {
        return "ParameterActionAndInt";
    }

    @Override
    public void setController(Control c) {
    }

    @Override
    public void fireLeftAction() {
        FilterGradRepresentation rep = getGradRepresentation();
        if (rep != null) {
            rep.addBand(MasterImage.getImage().getOriginalBounds());
            updateMenuItems(rep);
            updateSeekBar(rep);
            commitLocalRepresentation();
            this.mView.invalidate();
        }
    }

    @Override
    public int getLeftIcon() {
        return R.drawable.ic_grad_add;
    }

    @Override
    public void fireRightAction() {
        FilterGradRepresentation rep = getGradRepresentation();
        if (rep != null) {
            rep.deleteCurrentBand();
            updateMenuItems(rep);
            updateSeekBar(rep);
            commitLocalRepresentation();
            this.mView.invalidate();
        }
    }

    @Override
    public int getRightIcon() {
        return R.drawable.ic_grad_del;
    }

    @Override
    public void setFilterView(FilterView editor) {
    }
}

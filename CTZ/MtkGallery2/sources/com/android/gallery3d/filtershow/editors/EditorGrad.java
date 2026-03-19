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
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation instanceof FilterGradRepresentation) {
            localRepresentation.showParameterValue();
            this.mImageGrad.setRepresentation(localRepresentation);
        }
    }

    public void updateSeekBar(FilterGradRepresentation filterGradRepresentation) {
        if (ParametricEditor.useCompact(this.mContext)) {
            this.mControl.updateUI();
        } else {
            updateParameters();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation instanceof FilterGradRepresentation) {
            localRepresentation.setParameter(this.mSliderMode, i + localRepresentation.getParameterMin(this.mSliderMode));
            this.mView.invalidate();
            commitLocalRepresentation();
        }
    }

    @Override
    public void openUtilityPanel(final LinearLayout linearLayout) {
        Button button = (Button) linearLayout.findViewById(R.id.applyEffect);
        if (useCompact(this.mContext)) {
            button.setText(this.mContext.getString(R.string.editor_grad_brightness));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditorGrad.this.showPopupMenu(linearLayout);
                }
            });
            setUpPopupMenu(button);
            setEffectName();
            return;
        }
        button.setText(this.mContext.getString(R.string.grad));
    }

    private void updateMenuItems(FilterGradRepresentation filterGradRepresentation) {
        filterGradRepresentation.getNumberOfBands();
    }

    public void setEffectName() {
        if (this.mPopupMenu != null) {
            this.mEffectName = this.mPopupMenu.getMenu().findItem(R.id.editor_grad_brightness).getTitle().toString();
        }
    }

    @Override
    public void setUtilityPanelUI(View view, View view2) {
        if (ParametricEditor.useCompact(this.mContext)) {
            super.setUtilityPanelUI(view, view2);
            return;
        }
        this.mSeekBar = (SeekBar) view2.findViewById(R.id.primarySeekBar);
        if (this.mSeekBar != null) {
            this.mSeekBar.setVisibility(8);
        }
        LinearLayout linearLayout = (LinearLayout) ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.filtershow_grad_ui, (ViewGroup) view2, true);
        this.mAdapters[0] = new ParamAdapter(R.id.gradContrastSeekBar, R.id.gradContrastValue, linearLayout, 2);
        this.mAdapters[1] = new ParamAdapter(R.id.gradBrightnessSeekBar, R.id.gradBrightnessValue, linearLayout, 0);
        this.mAdapters[2] = new ParamAdapter(R.id.gradSaturationSeekBar, R.id.gradSaturationValue, linearLayout, 1);
        linearLayout.findViewById(R.id.gradAddButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view3) {
                EditorGrad.this.fireLeftAction();
            }
        });
        linearLayout.findViewById(R.id.gradDelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view3) {
                EditorGrad.this.fireRightAction();
            }
        });
        setMenuIcon(false);
    }

    public void updateParameters() {
        FilterGradRepresentation gradRepresentation = getGradRepresentation();
        for (int i = 0; i < this.mAdapters.length; i++) {
            this.mAdapters[i].updateValues(gradRepresentation);
        }
    }

    private class ParamAdapter implements SeekBar.OnSeekBarChangeListener {
        int mMode;
        SeekBar mSlider;
        TextView mTextView;
        int mMin = -100;
        int mMax = 100;

        public ParamAdapter(int i, int i2, LinearLayout linearLayout, int i3) {
            this.mSlider = (SeekBar) linearLayout.findViewById(i);
            this.mTextView = (TextView) linearLayout.findViewById(i2);
            this.mSlider.setMax(this.mMax - this.mMin);
            this.mMode = i3;
            FilterGradRepresentation gradRepresentation = EditorGrad.this.getGradRepresentation();
            if (gradRepresentation != null) {
                updateValues(gradRepresentation);
            }
            this.mSlider.setOnSeekBarChangeListener(this);
        }

        public void updateValues(FilterGradRepresentation filterGradRepresentation) {
            int parameter = filterGradRepresentation.getParameter(this.mMode);
            this.mTextView.setText(Integer.toString(parameter));
            this.mSlider.setProgress(parameter - this.mMin);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            FilterGradRepresentation gradRepresentation = EditorGrad.this.getGradRepresentation();
            int i2 = i + this.mMin;
            gradRepresentation.setParameter(this.mMode, i2);
            if (EditorGrad.this.mSliderMode != this.mMode) {
                EditorGrad.this.mSliderMode = this.mMode;
                EditorGrad.this.mEffectName = EditorGrad.this.mContext.getResources().getString(getModeNameid(this.mMode));
                EditorGrad.this.mEffectName = EditorGrad.this.mEffectName.toUpperCase();
            }
            this.mTextView.setText(Integer.toString(i2));
            EditorGrad.this.mView.invalidate();
            EditorGrad.this.commitLocalRepresentation();
        }

        private int getModeNameid(int i) {
            switch (i) {
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

    private void showPopupMenu(LinearLayout linearLayout) {
        Button button = (Button) linearLayout.findViewById(R.id.applyEffect);
        if (button == null) {
            return;
        }
        if (this.mPopupMenu == null) {
            setUpPopupMenu(button);
        }
        this.mPopupMenu.show();
        ((FilterShowActivity) this.mContext).onShowMenu(this.mPopupMenu);
    }

    private void setUpPopupMenu(Button button) {
        this.mPopupMenu = new PopupMenu(this.mImageShow.getActivity(), button);
        this.mPopupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_grad, this.mPopupMenu.getMenu());
        ?? localRepresentation = getLocalRepresentation();
        if (!(localRepresentation instanceof FilterGradRepresentation) || localRepresentation == 0) {
            return;
        }
        updateMenuItems(localRepresentation);
        hackFixStrings(this.mPopupMenu.getMenu());
        setEffectName();
        updateText();
        this.mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                FilterRepresentation localRepresentation2 = EditorGrad.this.getLocalRepresentation();
                if (localRepresentation2 instanceof FilterGradRepresentation) {
                    FilterGradRepresentation filterGradRepresentation = (FilterGradRepresentation) localRepresentation2;
                    switch (menuItem.getItemId()) {
                        case R.id.editor_grad_brightness:
                            EditorGrad.this.mSliderMode = 0;
                            EditorGrad.this.mEffectName = menuItem.getTitle().toString();
                            break;
                        case R.id.editor_grad_saturation:
                            EditorGrad.this.mSliderMode = 1;
                            EditorGrad.this.mEffectName = menuItem.getTitle().toString();
                            break;
                        case R.id.editor_grad_contrast:
                            EditorGrad.this.mSliderMode = 2;
                            EditorGrad.this.mEffectName = menuItem.getTitle().toString();
                            break;
                    }
                    EditorGrad.this.updateMenuItems(filterGradRepresentation);
                    EditorGrad.this.updateSeekBar(filterGradRepresentation);
                    EditorGrad.this.commitLocalRepresentation();
                    EditorGrad.this.mView.invalidate();
                }
                return true;
            }
        });
    }

    @Override
    public String calculateUserMessage(Context context, String str, Object obj) {
        FilterGradRepresentation gradRepresentation = getGradRepresentation();
        if (gradRepresentation == null) {
            return this.mEffectName;
        }
        int parameter = gradRepresentation.getParameter(this.mSliderMode);
        StringBuilder sb = new StringBuilder();
        sb.append(this.mEffectName.toUpperCase());
        sb.append(parameter > 0 ? " +" : " ");
        sb.append(parameter);
        return sb.toString();
    }

    private FilterGradRepresentation getGradRepresentation() {
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation instanceof FilterGradRepresentation) {
            return localRepresentation;
        }
        return null;
    }

    @Override
    public int getMaximum() {
        FilterGradRepresentation gradRepresentation = getGradRepresentation();
        if (gradRepresentation == null) {
            return 0;
        }
        return gradRepresentation.getParameterMax(this.mSliderMode);
    }

    @Override
    public int getMinimum() {
        FilterGradRepresentation gradRepresentation = getGradRepresentation();
        if (gradRepresentation == null) {
            return 0;
        }
        return gradRepresentation.getParameterMin(this.mSliderMode);
    }

    @Override
    public int getValue() {
        FilterGradRepresentation gradRepresentation = getGradRepresentation();
        if (gradRepresentation == null) {
            return 0;
        }
        return gradRepresentation.getParameter(this.mSliderMode);
    }

    @Override
    public void setValue(int i) {
        FilterGradRepresentation gradRepresentation = getGradRepresentation();
        if (gradRepresentation == null) {
            return;
        }
        gradRepresentation.setParameter(this.mSliderMode, i);
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
    public void setController(Control control) {
    }

    @Override
    public void fireLeftAction() {
        FilterGradRepresentation gradRepresentation = getGradRepresentation();
        if (gradRepresentation == null) {
            return;
        }
        gradRepresentation.addBand(MasterImage.getImage().getOriginalBounds());
        updateMenuItems(gradRepresentation);
        updateSeekBar(gradRepresentation);
        commitLocalRepresentation();
        this.mView.invalidate();
    }

    @Override
    public int getLeftIcon() {
        return R.drawable.ic_grad_add;
    }

    @Override
    public void fireRightAction() {
        FilterGradRepresentation gradRepresentation = getGradRepresentation();
        if (gradRepresentation == null) {
            return;
        }
        gradRepresentation.deleteCurrentBand();
        updateMenuItems(gradRepresentation);
        updateSeekBar(gradRepresentation);
        commitLocalRepresentation();
        this.mView.invalidate();
    }

    @Override
    public int getRightIcon() {
        return R.drawable.ic_grad_del;
    }

    @Override
    public void setFilterView(FilterView filterView) {
    }
}

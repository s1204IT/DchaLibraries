package com.android.gallery3d.filtershow.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.colorpicker.ColorListener;
import com.android.gallery3d.filtershow.colorpicker.ColorOpacityView;
import com.android.gallery3d.filtershow.editors.Editor;

public class SliderOpacity implements Control {
    private ColorOpacityView mColorOpacityView;
    private Editor mEditor;
    private ParameterOpacity mParameter;

    @Override
    public void setUp(ViewGroup viewGroup, Parameter parameter, Editor editor) {
        viewGroup.removeAllViews();
        this.mEditor = editor;
        Context context = viewGroup.getContext();
        this.mParameter = (ParameterOpacity) parameter;
        this.mColorOpacityView = (ColorOpacityView) ((LinearLayout) ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.filtershow_opacity, viewGroup, true)).findViewById(R.id.opacityView);
        updateUI();
        this.mColorOpacityView.addColorListener(new ColorListener() {
            @Override
            public void setColor(float[] fArr) {
                SliderOpacity.this.mParameter.setValue((int) (255.0f * fArr[3]));
                SliderOpacity.this.mEditor.commitLocalRepresentation();
            }

            @Override
            public void addColorListener(ColorListener colorListener) {
            }
        });
    }

    @Override
    public void setPrameter(Parameter parameter) {
        this.mParameter = (ParameterOpacity) parameter;
        if (this.mColorOpacityView != null) {
            updateUI();
        }
    }

    @Override
    public void updateUI() {
        this.mColorOpacityView.setColor(this.mParameter.getColor());
    }
}

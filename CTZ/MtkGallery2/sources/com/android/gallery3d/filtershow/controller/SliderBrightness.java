package com.android.gallery3d.filtershow.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.colorpicker.ColorBrightnessView;
import com.android.gallery3d.filtershow.colorpicker.ColorListener;
import com.android.gallery3d.filtershow.editors.Editor;

public class SliderBrightness implements Control {
    private ColorBrightnessView mColorOpacityView;
    Editor mEditor;
    private ParameterBrightness mParameter;

    @Override
    public void setUp(ViewGroup viewGroup, Parameter parameter, Editor editor) {
        viewGroup.removeAllViews();
        this.mEditor = editor;
        Context context = viewGroup.getContext();
        this.mParameter = (ParameterBrightness) parameter;
        this.mColorOpacityView = (ColorBrightnessView) ((LinearLayout) ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.filtershow_brightness, viewGroup, true)).findViewById(R.id.brightnessView);
        updateUI();
        this.mColorOpacityView.addColorListener(new ColorListener() {
            @Override
            public void setColor(float[] fArr) {
                SliderBrightness.this.mParameter.setValue((int) (255.0f * fArr[3]));
                SliderBrightness.this.mEditor.commitLocalRepresentation();
            }

            @Override
            public void addColorListener(ColorListener colorListener) {
            }
        });
    }

    @Override
    public void setPrameter(Parameter parameter) {
        this.mParameter = (ParameterBrightness) parameter;
        if (this.mColorOpacityView != null) {
            updateUI();
        }
    }

    @Override
    public void updateUI() {
        this.mColorOpacityView.setColor(this.mParameter.getColor());
    }
}

package com.android.gallery3d.filtershow.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.colorpicker.ColorHueView;
import com.android.gallery3d.filtershow.colorpicker.ColorListener;
import com.android.gallery3d.filtershow.editors.Editor;

public class SliderHue implements Control {
    public static String LOGTAG = "SliderHue";
    private ColorHueView mColorOpacityView;
    Editor mEditor;
    private ParameterHue mParameter;

    @Override
    public void setUp(ViewGroup viewGroup, Parameter parameter, Editor editor) {
        viewGroup.removeAllViews();
        this.mEditor = editor;
        Context context = viewGroup.getContext();
        this.mParameter = (ParameterHue) parameter;
        this.mColorOpacityView = (ColorHueView) ((LinearLayout) ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.filtershow_hue, viewGroup, true)).findViewById(R.id.hueView);
        updateUI();
        this.mColorOpacityView.addColorListener(new ColorListener() {
            @Override
            public void setColor(float[] fArr) {
                SliderHue.this.mParameter.setValue((int) (360.0f * fArr[3]));
                SliderHue.this.mEditor.commitLocalRepresentation();
            }

            @Override
            public void addColorListener(ColorListener colorListener) {
            }
        });
    }

    @Override
    public void setPrameter(Parameter parameter) {
        this.mParameter = (ParameterHue) parameter;
        if (this.mColorOpacityView != null) {
            updateUI();
        }
    }

    @Override
    public void updateUI() {
        this.mColorOpacityView.setColor(this.mParameter.getColor());
    }
}

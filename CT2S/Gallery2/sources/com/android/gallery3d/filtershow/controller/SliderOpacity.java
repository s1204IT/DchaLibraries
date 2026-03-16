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
    public void setUp(ViewGroup container, Parameter parameter, Editor editor) {
        container.removeAllViews();
        this.mEditor = editor;
        Context context = container.getContext();
        this.mParameter = (ParameterOpacity) parameter;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        LinearLayout lp = (LinearLayout) inflater.inflate(R.layout.filtershow_opacity, container, true);
        this.mColorOpacityView = (ColorOpacityView) lp.findViewById(R.id.opacityView);
        updateUI();
        this.mColorOpacityView.addColorListener(new ColorListener() {
            @Override
            public void setColor(float[] hsvo) {
                SliderOpacity.this.mParameter.setValue((int) (255.0f * hsvo[3]));
                SliderOpacity.this.mEditor.commitLocalRepresentation();
            }

            @Override
            public void addColorListener(ColorListener l) {
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

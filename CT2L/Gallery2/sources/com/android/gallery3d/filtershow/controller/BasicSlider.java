package com.android.gallery3d.filtershow.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.Editor;

public class BasicSlider implements Control {
    Editor mEditor;
    private ParameterInteger mParameter;
    private SeekBar mSeekBar;

    @Override
    public void setUp(ViewGroup container, Parameter parameter, Editor editor) {
        container.removeAllViews();
        this.mEditor = editor;
        Context context = container.getContext();
        this.mParameter = (ParameterInteger) parameter;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        LinearLayout lp = (LinearLayout) inflater.inflate(R.layout.filtershow_seekbar, container, true);
        this.mSeekBar = (SeekBar) lp.findViewById(R.id.primarySeekBar);
        this.mSeekBar.setVisibility(0);
        updateUI();
        this.mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (BasicSlider.this.mParameter != null) {
                    BasicSlider.this.mParameter.setValue(BasicSlider.this.mParameter.getMinimum() + progress);
                    BasicSlider.this.mEditor.commitLocalRepresentation();
                }
            }
        });
    }

    @Override
    public void setPrameter(Parameter parameter) {
        this.mParameter = (ParameterInteger) parameter;
        if (this.mSeekBar != null) {
            updateUI();
        }
    }

    @Override
    public void updateUI() {
        this.mSeekBar.setMax(this.mParameter.getMaximum() - this.mParameter.getMinimum());
        this.mSeekBar.setProgress(this.mParameter.getValue() - this.mParameter.getMinimum());
    }
}

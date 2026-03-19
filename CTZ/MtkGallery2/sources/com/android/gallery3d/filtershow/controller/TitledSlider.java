package com.android.gallery3d.filtershow.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.Editor;

public class TitledSlider implements Control {
    private TextView mControlName;
    private TextView mControlValue;
    Editor mEditor;
    protected ParameterInteger mParameter;
    private SeekBar mSeekBar;
    View mTopView;
    private final String LOGTAG = "ParametricEditor";
    protected int mLayoutID = R.layout.filtershow_control_title_slider;

    @Override
    public void setUp(ViewGroup viewGroup, Parameter parameter, Editor editor) {
        viewGroup.removeAllViews();
        this.mEditor = editor;
        Context context = viewGroup.getContext();
        this.mParameter = (ParameterInteger) parameter;
        this.mTopView = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(this.mLayoutID, viewGroup, true);
        this.mTopView.setVisibility(0);
        this.mSeekBar = (SeekBar) this.mTopView.findViewById(R.id.controlValueSeekBar);
        this.mControlName = (TextView) this.mTopView.findViewById(R.id.controlName);
        this.mControlValue = (TextView) this.mTopView.findViewById(R.id.controlValue);
        updateUI();
        this.mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                if (TitledSlider.this.mParameter != null) {
                    TitledSlider.this.mParameter.setValue(i + TitledSlider.this.mParameter.getMinimum());
                    if (TitledSlider.this.mControlName != null) {
                        TitledSlider.this.mControlName.setText(TitledSlider.this.mParameter.getParameterName());
                    }
                    if (TitledSlider.this.mControlValue != null) {
                        TitledSlider.this.mControlValue.setText(Integer.toString(TitledSlider.this.mParameter.getValue()));
                    }
                    TitledSlider.this.mEditor.commitLocalRepresentation();
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
        if (this.mControlName != null && this.mParameter.getParameterName() != null) {
            this.mControlName.setText(this.mParameter.getParameterName().toUpperCase());
        }
        if (this.mControlValue != null) {
            this.mControlValue.setText(Integer.toString(this.mParameter.getValue()));
        }
        this.mSeekBar.setMax(this.mParameter.getMaximum() - this.mParameter.getMinimum());
        this.mSeekBar.setProgress(this.mParameter.getValue() - this.mParameter.getMinimum());
        this.mEditor.commitLocalRepresentation();
    }
}

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
    public void setUp(ViewGroup container, Parameter parameter, Editor editor) {
        container.removeAllViews();
        this.mEditor = editor;
        Context context = container.getContext();
        this.mParameter = (ParameterInteger) parameter;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mTopView = inflater.inflate(this.mLayoutID, container, true);
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
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (TitledSlider.this.mParameter != null) {
                    TitledSlider.this.mParameter.setValue(TitledSlider.this.mParameter.getMinimum() + progress);
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

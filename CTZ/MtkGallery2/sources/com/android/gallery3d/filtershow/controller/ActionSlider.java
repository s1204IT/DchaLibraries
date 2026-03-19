package com.android.gallery3d.filtershow.controller;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.Editor;

public class ActionSlider extends TitledSlider {
    ImageButton mLeftButton;
    ImageButton mRightButton;

    public ActionSlider() {
        this.mLayoutID = R.layout.filtershow_control_action_slider;
    }

    @Override
    public void setUp(ViewGroup viewGroup, Parameter parameter, Editor editor) {
        super.setUp(viewGroup, parameter, editor);
        this.mLeftButton = (ImageButton) this.mTopView.findViewById(R.id.leftActionButton);
        this.mLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ParameterActionAndInt) ActionSlider.this.mParameter).fireLeftAction();
            }
        });
        this.mRightButton = (ImageButton) this.mTopView.findViewById(R.id.rightActionButton);
        this.mRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ParameterActionAndInt) ActionSlider.this.mParameter).fireRightAction();
            }
        });
        updateUI();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (this.mLeftButton != null) {
            this.mLeftButton.setImageResource(((ParameterActionAndInt) this.mParameter).getLeftIcon());
        }
        if (this.mRightButton != null) {
            this.mRightButton.setImageResource(((ParameterActionAndInt) this.mParameter).getRightIcon());
        }
    }
}

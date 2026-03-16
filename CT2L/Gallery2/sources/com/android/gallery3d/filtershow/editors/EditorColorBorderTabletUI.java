package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.colorpicker.ColorCompareView;
import com.android.gallery3d.filtershow.colorpicker.ColorHueView;
import com.android.gallery3d.filtershow.colorpicker.ColorListener;
import com.android.gallery3d.filtershow.colorpicker.ColorOpacityView;
import com.android.gallery3d.filtershow.colorpicker.ColorSVRectView;
import com.android.gallery3d.filtershow.controller.BasicParameterInt;
import com.android.gallery3d.filtershow.controller.ParameterColor;
import com.android.gallery3d.filtershow.filters.FilterColorBorderRepresentation;
import java.util.Arrays;

public class EditorColorBorderTabletUI {
    private static int sIconDim = 120;
    private int[] ids = {R.id.draw_color_button01, R.id.draw_color_button02, R.id.draw_color_button03, R.id.draw_color_button04, R.id.draw_color_button05};
    private int[] mBasColors;
    private SeekBar mCBCornerSizeSeekBar;
    TextView mCBCornerSizeValue;
    private SeekBar mCBSizeSeekBar;
    TextView mCBSizeValue;
    private Button[] mColorButton;
    private ColorCompareView mColorCompareView;
    private EditorColorBorder mEditorDraw;
    private ColorHueView mHueView;
    private ColorOpacityView mOpacityView;
    private FilterColorBorderRepresentation mRep;
    private ColorSVRectView mSatValView;
    private int mSelected;
    private int mSelectedColorButton;
    private int mTransparent;

    public void setColorBorderRepresentation(FilterColorBorderRepresentation rep) {
        this.mRep = rep;
        BasicParameterInt size = (BasicParameterInt) this.mRep.getParam(0);
        this.mCBSizeSeekBar.setMax(size.getMaximum() - size.getMinimum());
        this.mCBSizeSeekBar.setProgress(size.getValue());
        BasicParameterInt radius = (BasicParameterInt) this.mRep.getParam(1);
        this.mCBCornerSizeSeekBar.setMax(radius.getMaximum() - radius.getMinimum());
        this.mCBCornerSizeSeekBar.setProgress(radius.getValue());
        ParameterColor color = (ParameterColor) this.mRep.getParam(2);
        this.mBasColors = color.getColorPalette();
        color.setValue(this.mBasColors[this.mSelectedColorButton]);
    }

    public EditorColorBorderTabletUI(EditorColorBorder editorDraw, Context context, View base) {
        this.mEditorDraw = editorDraw;
        this.mBasColors = editorDraw.mBasColors;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        LinearLayout lp = (LinearLayout) inflater.inflate(R.layout.filtershow_color_border_ui, (ViewGroup) base, true);
        Resources res = context.getResources();
        sIconDim = res.getDimensionPixelSize(R.dimen.draw_style_icon_dim);
        this.mCBCornerSizeSeekBar = (SeekBar) lp.findViewById(R.id.colorBorderCornerSizeSeekBar);
        this.mCBCornerSizeValue = (TextView) lp.findViewById(R.id.colorBorderCornerValue);
        this.mCBSizeSeekBar = (SeekBar) lp.findViewById(R.id.colorBorderSizeSeekBar);
        this.mCBSizeValue = (TextView) lp.findViewById(R.id.colorBorderSizeValue);
        setupCBSizeSeekBar(lp);
        setupCBCornerSizeSeekBar(lp);
        setupColor(lp, res);
    }

    private void setupCBSizeSeekBar(LinearLayout lp) {
        this.mCBSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                BasicParameterInt size = (BasicParameterInt) EditorColorBorderTabletUI.this.mRep.getParam(0);
                size.setValue(size.getMinimum() + progress);
                EditorColorBorderTabletUI.this.mCBSizeValue.setText(Integer.toString(size.getValue()));
                EditorColorBorderTabletUI.this.mEditorDraw.commitLocalRepresentation();
            }
        });
    }

    private void setupCBCornerSizeSeekBar(LinearLayout lp) {
        this.mCBCornerSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                BasicParameterInt size = (BasicParameterInt) EditorColorBorderTabletUI.this.mRep.getParam(1);
                size.setValue(size.getMinimum() + progress);
                EditorColorBorderTabletUI.this.mCBCornerSizeValue.setText(size.getValue() + "");
                EditorColorBorderTabletUI.this.mEditorDraw.commitLocalRepresentation();
            }
        });
    }

    private void setupColor(LinearLayout lp, Resources res) {
        final LinearLayout ctls = (LinearLayout) lp.findViewById(R.id.controls);
        final LinearLayout pick = (LinearLayout) lp.findViewById(R.id.colorPicker);
        Button b = (Button) lp.findViewById(R.id.draw_color_popupbutton);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean b2 = ctls.getVisibility() == 0;
                ctls.setVisibility(b2 ? 8 : 0);
                pick.setVisibility(b2 ? 0 : 8);
            }
        });
        this.mTransparent = res.getColor(R.color.color_chooser_unslected_border);
        this.mSelected = res.getColor(R.color.color_chooser_slected_border);
        this.mColorButton = new Button[this.ids.length];
        int i = 0;
        while (i < this.ids.length) {
            this.mColorButton[i] = (Button) lp.findViewById(this.ids[i]);
            float[] hsvo = new float[4];
            Color.colorToHSV(this.mBasColors[i], hsvo);
            hsvo[3] = ((this.mBasColors[i] >> 24) & 255) / 255.0f;
            this.mColorButton[i].setTag(hsvo);
            GradientDrawable sd = (GradientDrawable) this.mColorButton[i].getBackground();
            sd.setColor(this.mBasColors[i]);
            sd.setStroke(3, i == 0 ? this.mSelected : this.mTransparent);
            final int buttonNo = i;
            this.mColorButton[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    EditorColorBorderTabletUI.this.mSelectedColorButton = buttonNo;
                    float[] hsvo2 = Arrays.copyOf((float[]) EditorColorBorderTabletUI.this.mColorButton[buttonNo].getTag(), 4);
                    EditorColorBorderTabletUI.this.resetBorders();
                    if (EditorColorBorderTabletUI.this.mRep != null) {
                        ParameterColor pram = (ParameterColor) EditorColorBorderTabletUI.this.mRep.getParam(2);
                        pram.setValue(EditorColorBorderTabletUI.this.mBasColors[EditorColorBorderTabletUI.this.mSelectedColorButton]);
                        EditorColorBorderTabletUI.this.mEditorDraw.commitLocalRepresentation();
                        EditorColorBorderTabletUI.this.mHueView.setColor(hsvo2);
                        EditorColorBorderTabletUI.this.mSatValView.setColor(hsvo2);
                        EditorColorBorderTabletUI.this.mOpacityView.setColor(hsvo2);
                        EditorColorBorderTabletUI.this.mColorCompareView.setOrigColor(hsvo2);
                    }
                }
            });
            i++;
        }
        this.mHueView = (ColorHueView) lp.findViewById(R.id.ColorHueView);
        this.mSatValView = (ColorSVRectView) lp.findViewById(R.id.colorRectView);
        this.mOpacityView = (ColorOpacityView) lp.findViewById(R.id.colorOpacityView);
        this.mColorCompareView = (ColorCompareView) lp.findViewById(R.id.btnSelect);
        float[] hsvo2 = new float[4];
        Color.colorToHSV(this.mBasColors[0], hsvo2);
        hsvo2[3] = ((this.mBasColors[0] >> 24) & 255) / 255.0f;
        this.mColorCompareView.setOrigColor(hsvo2);
        ColorListener[] colorViews = {this.mHueView, this.mSatValView, this.mOpacityView, this.mColorCompareView};
        for (int i2 = 0; i2 < colorViews.length; i2++) {
            colorViews[i2].setColor(hsvo2);
            for (int j = 0; j < colorViews.length; j++) {
                if (i2 != j) {
                    colorViews[i2].addColorListener(colorViews[j]);
                }
            }
        }
        ColorListener colorListener = new ColorListener() {
            @Override
            public void setColor(float[] hsvo3) {
                int color = Color.HSVToColor((int) (hsvo3[3] * 255.0f), hsvo3);
                Button b2 = EditorColorBorderTabletUI.this.mColorButton[EditorColorBorderTabletUI.this.mSelectedColorButton];
                float[] f = (float[]) b2.getTag();
                System.arraycopy(hsvo3, 0, f, 0, 4);
                EditorColorBorderTabletUI.this.mBasColors[EditorColorBorderTabletUI.this.mSelectedColorButton] = color;
                GradientDrawable sd2 = (GradientDrawable) b2.getBackground();
                sd2.setColor(color);
                EditorColorBorderTabletUI.this.resetBorders();
                ParameterColor pram = (ParameterColor) EditorColorBorderTabletUI.this.mRep.getParam(2);
                pram.setValue(color);
                EditorColorBorderTabletUI.this.mEditorDraw.commitLocalRepresentation();
            }

            @Override
            public void addColorListener(ColorListener l) {
            }
        };
        for (ColorListener colorListener2 : colorViews) {
            colorListener2.addColorListener(colorListener);
        }
    }

    private void resetBorders() {
        int i = 0;
        while (i < this.ids.length) {
            Button button = this.mColorButton[i];
            GradientDrawable sd = (GradientDrawable) button.getBackground();
            sd.setColor(this.mBasColors[i]);
            sd.setStroke(3, this.mSelectedColorButton == i ? this.mSelected : this.mTransparent);
            i++;
        }
    }
}

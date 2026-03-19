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

    public void setColorBorderRepresentation(FilterColorBorderRepresentation filterColorBorderRepresentation) {
        this.mRep = filterColorBorderRepresentation;
        BasicParameterInt basicParameterInt = (BasicParameterInt) this.mRep.getParam(0);
        this.mCBSizeSeekBar.setMax(basicParameterInt.getMaximum() - basicParameterInt.getMinimum());
        this.mCBSizeSeekBar.setProgress(basicParameterInt.getValue());
        BasicParameterInt basicParameterInt2 = (BasicParameterInt) this.mRep.getParam(1);
        this.mCBCornerSizeSeekBar.setMax(basicParameterInt2.getMaximum() - basicParameterInt2.getMinimum());
        this.mCBCornerSizeSeekBar.setProgress(basicParameterInt2.getValue());
        ParameterColor parameterColor = (ParameterColor) this.mRep.getParam(2);
        this.mBasColors = parameterColor.getColorPalette();
        parameterColor.setValue(this.mBasColors[this.mSelectedColorButton]);
    }

    public EditorColorBorderTabletUI(EditorColorBorder editorColorBorder, Context context, View view) {
        this.mEditorDraw = editorColorBorder;
        this.mBasColors = editorColorBorder.mBasColors;
        LinearLayout linearLayout = (LinearLayout) ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.filtershow_color_border_ui, (ViewGroup) view, true);
        Resources resources = context.getResources();
        sIconDim = resources.getDimensionPixelSize(R.dimen.draw_style_icon_dim);
        this.mCBCornerSizeSeekBar = (SeekBar) linearLayout.findViewById(R.id.colorBorderCornerSizeSeekBar);
        this.mCBCornerSizeValue = (TextView) linearLayout.findViewById(R.id.colorBorderCornerValue);
        this.mCBSizeSeekBar = (SeekBar) linearLayout.findViewById(R.id.colorBorderSizeSeekBar);
        this.mCBSizeValue = (TextView) linearLayout.findViewById(R.id.colorBorderSizeValue);
        setupCBSizeSeekBar(linearLayout);
        setupCBCornerSizeSeekBar(linearLayout);
        setupColor(linearLayout, resources);
    }

    private void setupCBSizeSeekBar(LinearLayout linearLayout) {
        this.mCBSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                BasicParameterInt basicParameterInt = (BasicParameterInt) EditorColorBorderTabletUI.this.mRep.getParam(0);
                basicParameterInt.setValue(i + basicParameterInt.getMinimum());
                EditorColorBorderTabletUI.this.mCBSizeValue.setText(Integer.toString(basicParameterInt.getValue()));
                EditorColorBorderTabletUI.this.mEditorDraw.commitLocalRepresentation();
            }
        });
    }

    private void setupCBCornerSizeSeekBar(LinearLayout linearLayout) {
        this.mCBCornerSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                BasicParameterInt basicParameterInt = (BasicParameterInt) EditorColorBorderTabletUI.this.mRep.getParam(1);
                basicParameterInt.setValue(i + basicParameterInt.getMinimum());
                EditorColorBorderTabletUI.this.mCBCornerSizeValue.setText(basicParameterInt.getValue() + "");
                EditorColorBorderTabletUI.this.mEditorDraw.commitLocalRepresentation();
            }
        });
    }

    private void setupColor(LinearLayout linearLayout, Resources resources) {
        final LinearLayout linearLayout2 = (LinearLayout) linearLayout.findViewById(R.id.controls);
        final LinearLayout linearLayout3 = (LinearLayout) linearLayout.findViewById(R.id.colorPicker);
        ((Button) linearLayout.findViewById(R.id.draw_color_popupbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean z = linearLayout2.getVisibility() == 0;
                linearLayout2.setVisibility(z ? 8 : 0);
                linearLayout3.setVisibility(z ? 0 : 8);
            }
        });
        this.mTransparent = resources.getColor(R.color.color_chooser_unslected_border);
        this.mSelected = resources.getColor(R.color.color_chooser_slected_border);
        this.mColorButton = new Button[this.ids.length];
        final int i = 0;
        while (i < this.ids.length) {
            this.mColorButton[i] = (Button) linearLayout.findViewById(this.ids[i]);
            float[] fArr = new float[4];
            Color.colorToHSV(this.mBasColors[i], fArr);
            fArr[3] = (255 & (this.mBasColors[i] >> 24)) / 255.0f;
            this.mColorButton[i].setTag(fArr);
            GradientDrawable gradientDrawable = (GradientDrawable) this.mColorButton[i].getBackground();
            gradientDrawable.setColor(this.mBasColors[i]);
            gradientDrawable.setStroke(3, i == 0 ? this.mSelected : this.mTransparent);
            this.mColorButton[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditorColorBorderTabletUI.this.mSelectedColorButton = i;
                    float[] fArrCopyOf = Arrays.copyOf((float[]) EditorColorBorderTabletUI.this.mColorButton[i].getTag(), 4);
                    EditorColorBorderTabletUI.this.resetBorders();
                    if (EditorColorBorderTabletUI.this.mRep != null) {
                        ((ParameterColor) EditorColorBorderTabletUI.this.mRep.getParam(2)).setValue(EditorColorBorderTabletUI.this.mBasColors[EditorColorBorderTabletUI.this.mSelectedColorButton]);
                        EditorColorBorderTabletUI.this.mEditorDraw.commitLocalRepresentation();
                        EditorColorBorderTabletUI.this.mHueView.setColor(fArrCopyOf);
                        EditorColorBorderTabletUI.this.mSatValView.setColor(fArrCopyOf);
                        EditorColorBorderTabletUI.this.mOpacityView.setColor(fArrCopyOf);
                        EditorColorBorderTabletUI.this.mColorCompareView.setOrigColor(fArrCopyOf);
                    }
                }
            });
            i++;
        }
        this.mHueView = (ColorHueView) linearLayout.findViewById(R.id.ColorHueView);
        this.mSatValView = (ColorSVRectView) linearLayout.findViewById(R.id.colorRectView);
        this.mOpacityView = (ColorOpacityView) linearLayout.findViewById(R.id.colorOpacityView);
        this.mColorCompareView = (ColorCompareView) linearLayout.findViewById(R.id.btnSelect);
        float[] fArr2 = new float[4];
        Color.colorToHSV(this.mBasColors[0], fArr2);
        fArr2[3] = ((this.mBasColors[0] >> 24) & 255) / 255.0f;
        this.mColorCompareView.setOrigColor(fArr2);
        ColorListener[] colorListenerArr = {this.mHueView, this.mSatValView, this.mOpacityView, this.mColorCompareView};
        for (int i2 = 0; i2 < colorListenerArr.length; i2++) {
            colorListenerArr[i2].setColor(fArr2);
            for (int i3 = 0; i3 < colorListenerArr.length; i3++) {
                if (i2 != i3) {
                    colorListenerArr[i2].addColorListener(colorListenerArr[i3]);
                }
            }
        }
        ColorListener colorListener = new ColorListener() {
            @Override
            public void setColor(float[] fArr3) {
                int iHSVToColor = Color.HSVToColor((int) (fArr3[3] * 255.0f), fArr3);
                Button button = EditorColorBorderTabletUI.this.mColorButton[EditorColorBorderTabletUI.this.mSelectedColorButton];
                System.arraycopy(fArr3, 0, (float[]) button.getTag(), 0, 4);
                EditorColorBorderTabletUI.this.mBasColors[EditorColorBorderTabletUI.this.mSelectedColorButton] = iHSVToColor;
                ((GradientDrawable) button.getBackground()).setColor(iHSVToColor);
                EditorColorBorderTabletUI.this.resetBorders();
                ((ParameterColor) EditorColorBorderTabletUI.this.mRep.getParam(2)).setValue(iHSVToColor);
                EditorColorBorderTabletUI.this.mEditorDraw.commitLocalRepresentation();
            }

            @Override
            public void addColorListener(ColorListener colorListener2) {
            }
        };
        for (ColorListener colorListener2 : colorListenerArr) {
            colorListener2.addColorListener(colorListener);
        }
    }

    private void resetBorders() {
        int i = 0;
        while (i < this.ids.length) {
            GradientDrawable gradientDrawable = (GradientDrawable) this.mColorButton[i].getBackground();
            gradientDrawable.setColor(this.mBasColors[i]);
            gradientDrawable.setStroke(3, this.mSelectedColorButton == i ? this.mSelected : this.mTransparent);
            i++;
        }
    }
}

package com.android.gallery3d.filtershow.editors;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.android.gallery3d.filtershow.controller.BasicParameterStyle;
import com.android.gallery3d.filtershow.controller.ParameterColor;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;
import java.util.Arrays;

public class EditorDrawTabletUI {
    private static int sIconDim = 120;
    private int[] ids = {R.id.draw_color_button01, R.id.draw_color_button02, R.id.draw_color_button03, R.id.draw_color_button04, R.id.draw_color_button05};
    private int[] mBasColors;
    private int[] mBrushIcons;
    private Button[] mColorButton;
    private ColorCompareView mColorCompareView;
    private TextView mDrawSizeValue;
    private EditorDraw mEditorDraw;
    private ColorHueView mHueView;
    private ColorOpacityView mOpacityView;
    private FilterDrawRepresentation mRep;
    private ColorSVRectView mSatValView;
    private int mSelected;
    private int mSelectedColorButton;
    private int mSelectedStyleButton;
    private ImageButton[] mStyleButton;
    private int mTransparent;
    private SeekBar mdrawSizeSeekBar;

    public void setDrawRepresentation(FilterDrawRepresentation filterDrawRepresentation) {
        this.mRep = filterDrawRepresentation;
        BasicParameterInt basicParameterInt = (BasicParameterInt) this.mRep.getParam(0);
        this.mdrawSizeSeekBar.setMax(basicParameterInt.getMaximum() - basicParameterInt.getMinimum());
        this.mdrawSizeSeekBar.setProgress(basicParameterInt.getValue());
        ((ParameterColor) this.mRep.getParam(2)).setValue(this.mBasColors[this.mSelectedColorButton]);
        ((BasicParameterStyle) this.mRep.getParam(1)).setSelected(this.mSelectedStyleButton);
    }

    public EditorDrawTabletUI(EditorDraw editorDraw, Context context, LinearLayout linearLayout) {
        this.mEditorDraw = editorDraw;
        this.mBasColors = editorDraw.mBasColors;
        this.mBrushIcons = editorDraw.brushIcons;
        Resources resources = context.getResources();
        sIconDim = resources.getDimensionPixelSize(R.dimen.draw_style_icon_dim);
        LinearLayout linearLayout2 = (LinearLayout) linearLayout.findViewById(R.id.listStyles);
        this.mdrawSizeSeekBar = (SeekBar) linearLayout.findViewById(R.id.drawSizeSeekBar);
        this.mDrawSizeValue = (TextView) linearLayout.findViewById(R.id.drawSizeValue);
        ((Button) linearLayout.findViewById(R.id.clearButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditorDrawTabletUI.this.mEditorDraw.clearDrawing();
            }
        });
        this.mdrawSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                BasicParameterInt basicParameterInt = (BasicParameterInt) EditorDrawTabletUI.this.mRep.getParam(0);
                basicParameterInt.setValue(basicParameterInt.getMinimum() + i);
                EditorDrawTabletUI.this.mEditorDraw.commitLocalRepresentation();
                int minimum = i + basicParameterInt.getMinimum();
                TextView textView = EditorDrawTabletUI.this.mDrawSizeValue;
                StringBuilder sb = new StringBuilder();
                sb.append(minimum > 0 ? "+" : "");
                sb.append(minimum);
                textView.setText(sb.toString());
            }
        });
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(sIconDim, sIconDim);
        this.mStyleButton = new ImageButton[this.mBrushIcons.length];
        for (final int i = 0; i < this.mBrushIcons.length; i++) {
            ImageButton imageButton = new ImageButton(context);
            this.mStyleButton[i] = imageButton;
            imageButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageButton.setLayoutParams(layoutParams);
            imageButton.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), this.mBrushIcons[i]));
            imageButton.setBackgroundResource(android.R.color.transparent);
            linearLayout2.addView(imageButton);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditorDrawTabletUI.this.mSelectedStyleButton = i;
                    if (EditorDrawTabletUI.this.mRep == null) {
                        return;
                    }
                    ((BasicParameterStyle) EditorDrawTabletUI.this.mRep.getParam(1)).setSelected(i);
                    EditorDrawTabletUI.this.resetStyle();
                    EditorDrawTabletUI.this.mEditorDraw.commitLocalRepresentation();
                }
            });
        }
        final LinearLayout linearLayout3 = (LinearLayout) linearLayout.findViewById(R.id.controls);
        final LinearLayout linearLayout4 = (LinearLayout) linearLayout.findViewById(R.id.colorPicker);
        ((Button) linearLayout.findViewById(R.id.draw_color_popupbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean z = linearLayout3.getVisibility() == 0;
                linearLayout3.setVisibility(z ? 8 : 0);
                linearLayout4.setVisibility(z ? 0 : 8);
            }
        });
        this.mTransparent = resources.getColor(R.color.color_chooser_unslected_border);
        this.mSelected = resources.getColor(R.color.color_chooser_slected_border);
        this.mColorButton = new Button[this.ids.length];
        final int i2 = 0;
        while (i2 < this.ids.length) {
            this.mColorButton[i2] = (Button) linearLayout.findViewById(this.ids[i2]);
            float[] fArr = new float[4];
            Color.colorToHSV(this.mBasColors[i2], fArr);
            fArr[3] = (255 & (this.mBasColors[i2] >> 24)) / 255.0f;
            this.mColorButton[i2].setTag(fArr);
            GradientDrawable gradientDrawable = (GradientDrawable) this.mColorButton[i2].getBackground();
            gradientDrawable.setColor(this.mBasColors[i2]);
            gradientDrawable.setStroke(3, i2 == 0 ? this.mSelected : this.mTransparent);
            this.mColorButton[i2].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditorDrawTabletUI.this.mSelectedColorButton = i2;
                    float[] fArrCopyOf = Arrays.copyOf((float[]) EditorDrawTabletUI.this.mColorButton[i2].getTag(), 4);
                    EditorDrawTabletUI.this.resetBorders();
                    if (EditorDrawTabletUI.this.mRep != null) {
                        ((ParameterColor) EditorDrawTabletUI.this.mRep.getParam(2)).setValue(EditorDrawTabletUI.this.mBasColors[EditorDrawTabletUI.this.mSelectedColorButton]);
                        EditorDrawTabletUI.this.mEditorDraw.commitLocalRepresentation();
                        EditorDrawTabletUI.this.mHueView.setColor(fArrCopyOf);
                        EditorDrawTabletUI.this.mSatValView.setColor(fArrCopyOf);
                        EditorDrawTabletUI.this.mOpacityView.setColor(fArrCopyOf);
                        EditorDrawTabletUI.this.mColorCompareView.setColor(fArrCopyOf);
                        EditorDrawTabletUI.this.mColorCompareView.setOrigColor(fArrCopyOf);
                    }
                }
            });
            i2++;
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
        for (int i3 = 0; i3 < colorListenerArr.length; i3++) {
            colorListenerArr[i3].setColor(fArr2);
            for (int i4 = 0; i4 < colorListenerArr.length; i4++) {
                if (i3 != i4) {
                    colorListenerArr[i3].addColorListener(colorListenerArr[i4]);
                }
            }
        }
        ColorListener colorListener = new ColorListener() {
            @Override
            public void setColor(float[] fArr3) {
                int iHSVToColor = Color.HSVToColor((int) (fArr3[3] * 255.0f), fArr3);
                Button button = EditorDrawTabletUI.this.mColorButton[EditorDrawTabletUI.this.mSelectedColorButton];
                System.arraycopy(fArr3, 0, (float[]) button.getTag(), 0, 4);
                EditorDrawTabletUI.this.mBasColors[EditorDrawTabletUI.this.mSelectedColorButton] = iHSVToColor;
                ((GradientDrawable) button.getBackground()).setColor(iHSVToColor);
                EditorDrawTabletUI.this.resetBorders();
                ((ParameterColor) EditorDrawTabletUI.this.mRep.getParam(2)).setValue(iHSVToColor);
                EditorDrawTabletUI.this.mEditorDraw.commitLocalRepresentation();
            }

            @Override
            public void addColorListener(ColorListener colorListener2) {
            }
        };
        for (ColorListener colorListener2 : colorListenerArr) {
            colorListener2.addColorListener(colorListener);
        }
    }

    public void resetStyle() {
        int i = 0;
        while (i < this.mStyleButton.length) {
            this.mStyleButton[i].setBackgroundResource(i == this.mSelectedStyleButton ? android.R.color.holo_blue_light : android.R.color.transparent);
            i++;
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

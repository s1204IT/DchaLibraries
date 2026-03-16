package com.android.gallery3d.filtershow.editors;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
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

    public void setDrawRepresentation(FilterDrawRepresentation rep) {
        this.mRep = rep;
        BasicParameterInt size = (BasicParameterInt) this.mRep.getParam(0);
        this.mdrawSizeSeekBar.setMax(size.getMaximum() - size.getMinimum());
        this.mdrawSizeSeekBar.setProgress(size.getValue());
        ParameterColor color = (ParameterColor) this.mRep.getParam(2);
        color.setValue(this.mBasColors[this.mSelectedColorButton]);
        BasicParameterStyle style = (BasicParameterStyle) this.mRep.getParam(1);
        style.setSelected(this.mSelectedStyleButton);
    }

    public EditorDrawTabletUI(EditorDraw editorDraw, Context context, LinearLayout lp) {
        this.mEditorDraw = editorDraw;
        this.mBasColors = editorDraw.mBasColors;
        this.mBrushIcons = editorDraw.brushIcons;
        Resources res = context.getResources();
        sIconDim = res.getDimensionPixelSize(R.dimen.draw_style_icon_dim);
        LinearLayout buttonContainer = (LinearLayout) lp.findViewById(R.id.listStyles);
        this.mdrawSizeSeekBar = (SeekBar) lp.findViewById(R.id.drawSizeSeekBar);
        this.mDrawSizeValue = (TextView) lp.findViewById(R.id.drawSizeValue);
        Button clearButton = (Button) lp.findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
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
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                BasicParameterInt size = (BasicParameterInt) EditorDrawTabletUI.this.mRep.getParam(0);
                size.setValue(size.getMinimum() + progress);
                EditorDrawTabletUI.this.mEditorDraw.commitLocalRepresentation();
                int val = progress + size.getMinimum();
                EditorDrawTabletUI.this.mDrawSizeValue.setText((val > 0 ? "+" : "") + val);
            }
        });
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(sIconDim, sIconDim);
        this.mStyleButton = new ImageButton[this.mBrushIcons.length];
        for (int i = 0; i < this.mBrushIcons.length; i++) {
            ImageButton button = new ImageButton(context);
            this.mStyleButton[i] = button;
            button.setScaleType(ImageView.ScaleType.CENTER_CROP);
            button.setLayoutParams(params);
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), this.mBrushIcons[i]);
            button.setImageBitmap(bitmap);
            button.setBackgroundResource(android.R.color.transparent);
            buttonContainer.addView(button);
            final int current = i;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditorDrawTabletUI.this.mSelectedStyleButton = current;
                    if (EditorDrawTabletUI.this.mRep != null) {
                        BasicParameterStyle style = (BasicParameterStyle) EditorDrawTabletUI.this.mRep.getParam(1);
                        style.setSelected(current);
                        EditorDrawTabletUI.this.resetStyle();
                        EditorDrawTabletUI.this.mEditorDraw.commitLocalRepresentation();
                    }
                }
            });
        }
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
        int i2 = 0;
        while (i2 < this.ids.length) {
            this.mColorButton[i2] = (Button) lp.findViewById(this.ids[i2]);
            float[] hsvo = new float[4];
            Color.colorToHSV(this.mBasColors[i2], hsvo);
            hsvo[3] = ((this.mBasColors[i2] >> 24) & 255) / 255.0f;
            this.mColorButton[i2].setTag(hsvo);
            GradientDrawable sd = (GradientDrawable) this.mColorButton[i2].getBackground();
            sd.setColor(this.mBasColors[i2]);
            sd.setStroke(3, i2 == 0 ? this.mSelected : this.mTransparent);
            final int buttonNo = i2;
            this.mColorButton[i2].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    EditorDrawTabletUI.this.mSelectedColorButton = buttonNo;
                    float[] hsvo2 = Arrays.copyOf((float[]) EditorDrawTabletUI.this.mColorButton[buttonNo].getTag(), 4);
                    EditorDrawTabletUI.this.resetBorders();
                    if (EditorDrawTabletUI.this.mRep != null) {
                        ParameterColor pram = (ParameterColor) EditorDrawTabletUI.this.mRep.getParam(2);
                        pram.setValue(EditorDrawTabletUI.this.mBasColors[EditorDrawTabletUI.this.mSelectedColorButton]);
                        EditorDrawTabletUI.this.mEditorDraw.commitLocalRepresentation();
                        EditorDrawTabletUI.this.mHueView.setColor(hsvo2);
                        EditorDrawTabletUI.this.mSatValView.setColor(hsvo2);
                        EditorDrawTabletUI.this.mOpacityView.setColor(hsvo2);
                        EditorDrawTabletUI.this.mColorCompareView.setColor(hsvo2);
                        EditorDrawTabletUI.this.mColorCompareView.setOrigColor(hsvo2);
                    }
                }
            });
            i2++;
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
        for (int i3 = 0; i3 < colorViews.length; i3++) {
            colorViews[i3].setColor(hsvo2);
            for (int j = 0; j < colorViews.length; j++) {
                if (i3 != j) {
                    colorViews[i3].addColorListener(colorViews[j]);
                }
            }
        }
        ColorListener colorListener = new ColorListener() {
            @Override
            public void setColor(float[] hsvo3) {
                int color = Color.HSVToColor((int) (hsvo3[3] * 255.0f), hsvo3);
                Button b2 = EditorDrawTabletUI.this.mColorButton[EditorDrawTabletUI.this.mSelectedColorButton];
                float[] f = (float[]) b2.getTag();
                System.arraycopy(hsvo3, 0, f, 0, 4);
                EditorDrawTabletUI.this.mBasColors[EditorDrawTabletUI.this.mSelectedColorButton] = color;
                GradientDrawable sd2 = (GradientDrawable) b2.getBackground();
                sd2.setColor(color);
                EditorDrawTabletUI.this.resetBorders();
                ParameterColor pram = (ParameterColor) EditorDrawTabletUI.this.mRep.getParam(2);
                pram.setValue(color);
                EditorDrawTabletUI.this.mEditorDraw.commitLocalRepresentation();
            }

            @Override
            public void addColorListener(ColorListener l) {
            }
        };
        for (ColorListener colorListener2 : colorViews) {
            colorListener2.addColorListener(colorListener);
        }
    }

    public void resetStyle() {
        int i = 0;
        while (i < this.mStyleButton.length) {
            int rid = i == this.mSelectedStyleButton ? android.R.color.holo_blue_light : android.R.color.transparent;
            this.mStyleButton[i].setBackgroundResource(rid);
            i++;
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

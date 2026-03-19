package com.android.gallery3d.filtershow.controller;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.colorpicker.ColorListener;
import com.android.gallery3d.filtershow.colorpicker.ColorPickerDialog;
import com.android.gallery3d.filtershow.editors.Editor;
import java.util.Arrays;
import java.util.Vector;

public class ColorChooser implements Control {
    Context mContext;
    protected Editor mEditor;
    protected LinearLayout mLinearLayout;
    protected ParameterColor mParameter;
    private int mSelected;
    private View mTopView;
    private int mTransparent;
    private final String LOGTAG = "StyleChooser";
    private Vector<Button> mIconButton = new Vector<>();
    protected int mLayoutID = R.layout.filtershow_control_color_chooser;
    private int[] mButtonsID = {R.id.draw_color_button01, R.id.draw_color_button02, R.id.draw_color_button03, R.id.draw_color_button04, R.id.draw_color_button05};
    private Button[] mButton = new Button[this.mButtonsID.length];
    int mSelectedButton = 0;

    @Override
    public void setUp(ViewGroup viewGroup, Parameter parameter, Editor editor) {
        viewGroup.removeAllViews();
        Resources resources = viewGroup.getContext().getResources();
        this.mTransparent = resources.getColor(R.color.color_chooser_unslected_border);
        this.mSelected = resources.getColor(R.color.color_chooser_slected_border);
        this.mEditor = editor;
        this.mContext = viewGroup.getContext();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.draw_style_icon_dim);
        this.mParameter = (ParameterColor) parameter;
        this.mTopView = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(this.mLayoutID, viewGroup, true);
        this.mLinearLayout = (LinearLayout) this.mTopView.findViewById(R.id.listStyles);
        final int i = 0;
        this.mTopView.setVisibility(0);
        this.mIconButton.clear();
        new ActionBar.LayoutParams(dimensionPixelSize, dimensionPixelSize);
        int[] colorPalette = this.mParameter.getColorPalette();
        while (i < this.mButtonsID.length) {
            Button button = (Button) this.mTopView.findViewById(this.mButtonsID[i]);
            this.mButton[i] = button;
            float[] fArr = new float[4];
            Color.colorToHSV(colorPalette[i], fArr);
            fArr[3] = (255 & (colorPalette[i] >> 24)) / 255.0f;
            button.setTag(fArr);
            GradientDrawable gradientDrawable = (GradientDrawable) button.getBackground();
            gradientDrawable.setColor(colorPalette[i]);
            gradientDrawable.setStroke(3, this.mSelectedButton == i ? this.mSelected : this.mTransparent);
            if (this.mSelectedButton == i) {
                this.mParameter.setValue(Color.HSVToColor((int) (fArr[3] * 255.0f), fArr));
            }
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ColorChooser.this.selectColor(view, i);
                }
            });
            i++;
        }
        ((Button) this.mTopView.findViewById(R.id.draw_color_popupbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorChooser.this.showColorPicker();
            }
        });
    }

    public void setColorSet(int[] iArr) {
        int[] colorPalette = this.mParameter.getColorPalette();
        for (int i = 0; i < colorPalette.length; i++) {
            colorPalette[i] = iArr[i];
            float[] fArr = new float[4];
            Color.colorToHSV(colorPalette[i], fArr);
            fArr[3] = (255 & (colorPalette[i] >> 24)) / 255.0f;
            this.mButton[i].setTag(fArr);
            ((GradientDrawable) this.mButton[i].getBackground()).setColor(colorPalette[i]);
        }
    }

    public int[] getColorSet() {
        return this.mParameter.getColorPalette();
    }

    private void resetBorders() {
        int[] colorPalette = this.mParameter.getColorPalette();
        int i = 0;
        while (i < this.mButtonsID.length) {
            GradientDrawable gradientDrawable = (GradientDrawable) this.mButton[i].getBackground();
            gradientDrawable.setColor(colorPalette[i]);
            gradientDrawable.setStroke(3, this.mSelectedButton == i ? this.mSelected : this.mTransparent);
            i++;
        }
    }

    public void selectColor(View view, int i) {
        this.mSelectedButton = i;
        float[] fArr = (float[]) view.getTag();
        this.mParameter.setValue(Color.HSVToColor((int) (fArr[3] * 255.0f), fArr));
        resetBorders();
        this.mEditor.commitLocalRepresentation();
    }

    @Override
    public void setPrameter(Parameter parameter) {
        this.mParameter = (ParameterColor) parameter;
        updateUI();
    }

    @Override
    public void updateUI() {
        if (this.mParameter == null) {
        }
    }

    public void changeSelectedColor(float[] fArr) {
        int[] colorPalette = this.mParameter.getColorPalette();
        int iHSVToColor = Color.HSVToColor((int) (fArr[3] * 255.0f), fArr);
        Button button = this.mButton[this.mSelectedButton];
        Object tag = button.getTag();
        if (iHSVToColor == 0) {
            iHSVToColor = colorPalette[this.mSelectedButton];
            button.setTag(tag);
        } else {
            button.setTag(fArr);
        }
        ((GradientDrawable) button.getBackground()).setColor(iHSVToColor);
        colorPalette[this.mSelectedButton] = iHSVToColor;
        this.mParameter.setValue(iHSVToColor);
        this.mEditor.commitLocalRepresentation();
        button.invalidate();
    }

    public void showColorPicker() {
        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this.mContext, new ColorListener() {
            @Override
            public void setColor(float[] fArr) {
                ColorChooser.this.changeSelectedColor(fArr);
            }

            @Override
            public void addColorListener(ColorListener colorListener) {
            }
        });
        float[] fArr = (float[]) this.mButton[this.mSelectedButton].getTag();
        colorPickerDialog.setColor(Arrays.copyOf(fArr, 4));
        colorPickerDialog.setOrigColor(Arrays.copyOf(fArr, 4));
        colorPickerDialog.show();
    }
}

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
    public void setUp(ViewGroup container, Parameter parameter, Editor editor) {
        container.removeAllViews();
        Resources res = container.getContext().getResources();
        this.mTransparent = res.getColor(R.color.color_chooser_unslected_border);
        this.mSelected = res.getColor(R.color.color_chooser_slected_border);
        this.mEditor = editor;
        this.mContext = container.getContext();
        int iconDim = res.getDimensionPixelSize(R.dimen.draw_style_icon_dim);
        this.mParameter = (ParameterColor) parameter;
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        this.mTopView = inflater.inflate(this.mLayoutID, container, true);
        this.mLinearLayout = (LinearLayout) this.mTopView.findViewById(R.id.listStyles);
        this.mTopView.setVisibility(0);
        this.mIconButton.clear();
        new ActionBar.LayoutParams(iconDim, iconDim);
        int[] palette = this.mParameter.getColorPalette();
        int i = 0;
        while (i < this.mButtonsID.length) {
            Button button = (Button) this.mTopView.findViewById(this.mButtonsID[i]);
            this.mButton[i] = button;
            float[] hsvo = new float[4];
            Color.colorToHSV(palette[i], hsvo);
            hsvo[3] = ((palette[i] >> 24) & 255) / 255.0f;
            button.setTag(hsvo);
            GradientDrawable sd = (GradientDrawable) button.getBackground();
            sd.setColor(palette[i]);
            sd.setStroke(3, this.mSelectedButton == i ? this.mSelected : this.mTransparent);
            final int buttonNo = i;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    ColorChooser.this.selectColor(arg0, buttonNo);
                }
            });
            i++;
        }
        ((Button) this.mTopView.findViewById(R.id.draw_color_popupbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ColorChooser.this.showColorPicker();
            }
        });
    }

    public void setColorSet(int[] basColors) {
        int[] palette = this.mParameter.getColorPalette();
        for (int i = 0; i < palette.length; i++) {
            palette[i] = basColors[i];
            float[] hsvo = new float[4];
            Color.colorToHSV(palette[i], hsvo);
            hsvo[3] = ((palette[i] >> 24) & 255) / 255.0f;
            this.mButton[i].setTag(hsvo);
            GradientDrawable sd = (GradientDrawable) this.mButton[i].getBackground();
            sd.setColor(palette[i]);
        }
    }

    public int[] getColorSet() {
        return this.mParameter.getColorPalette();
    }

    private void resetBorders() {
        int[] palette = this.mParameter.getColorPalette();
        int i = 0;
        while (i < this.mButtonsID.length) {
            Button button = this.mButton[i];
            GradientDrawable sd = (GradientDrawable) button.getBackground();
            sd.setColor(palette[i]);
            sd.setStroke(3, this.mSelectedButton == i ? this.mSelected : this.mTransparent);
            i++;
        }
    }

    public void selectColor(View button, int buttonNo) {
        this.mSelectedButton = buttonNo;
        float[] hsvo = (float[]) button.getTag();
        this.mParameter.setValue(Color.HSVToColor((int) (hsvo[3] * 255.0f), hsvo));
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

    public void changeSelectedColor(float[] hsvo) {
        int[] palette = this.mParameter.getColorPalette();
        int c = Color.HSVToColor((int) (hsvo[3] * 255.0f), hsvo);
        Button button = this.mButton[this.mSelectedButton];
        GradientDrawable sd = (GradientDrawable) button.getBackground();
        sd.setColor(c);
        palette[this.mSelectedButton] = c;
        this.mParameter.setValue(Color.HSVToColor((int) (hsvo[3] * 255.0f), hsvo));
        button.setTag(hsvo);
        this.mEditor.commitLocalRepresentation();
        button.invalidate();
    }

    public void showColorPicker() {
        ColorListener cl = new ColorListener() {
            @Override
            public void setColor(float[] hsvo) {
                ColorChooser.this.changeSelectedColor(hsvo);
            }

            @Override
            public void addColorListener(ColorListener l) {
            }
        };
        ColorPickerDialog cpd = new ColorPickerDialog(this.mContext, cl);
        float[] c = (float[]) this.mButton[this.mSelectedButton].getTag();
        cpd.setColor(Arrays.copyOf(c, 4));
        cpd.setOrigColor(Arrays.copyOf(c, 4));
        cpd.show();
    }
}

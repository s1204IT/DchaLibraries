package com.android.gallery3d.filtershow.colorpicker;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ToggleButton;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;

public class ColorPickerDialog extends Dialog {
    ColorCompareView mColorCompareView;
    ColorHueView mColorHueView;
    ColorOpacityView mColorOpacityView;
    ColorSVRectView mColorSVRectView;
    float[] mHSVO;
    ToggleButton mSelectedButton;

    public ColorPickerDialog(Context context, final ColorListener cl) {
        super(context);
        this.mHSVO = new float[4];
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService("window");
        wm.getDefaultDisplay().getMetrics(metrics);
        int height = (metrics.heightPixels * 8) / 10;
        int width = (metrics.widthPixels * 8) / 10;
        getWindow().setLayout(width, height);
        requestWindowFeature(1);
        setContentView(R.layout.filtershow_color_picker);
        this.mColorHueView = (ColorHueView) findViewById(R.id.ColorHueView);
        this.mColorSVRectView = (ColorSVRectView) findViewById(R.id.colorRectView);
        this.mColorOpacityView = (ColorOpacityView) findViewById(R.id.colorOpacityView);
        this.mColorCompareView = (ColorCompareView) findViewById(R.id.btnSelect);
        float[] hsvo = {123.0f, 0.9f, 1.0f, 1.0f};
        ImageButton apply = (ImageButton) findViewById(R.id.applyColorPick);
        ImageButton cancel = (ImageButton) findViewById(R.id.cancelColorPick);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cl.setColor(ColorPickerDialog.this.mHSVO);
                ColorPickerDialog.this.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorPickerDialog.this.dismiss();
            }
        });
        ColorListener[] c = {this.mColorCompareView, this.mColorSVRectView, this.mColorOpacityView, this.mColorHueView};
        for (int i = 0; i < c.length; i++) {
            c[i].setColor(hsvo);
            for (int j = 0; j < c.length; j++) {
                if (i != j) {
                    c[i].addColorListener(c[j]);
                }
            }
        }
        ColorListener colorListener = new ColorListener() {
            @Override
            public void setColor(float[] hsvo2) {
                System.arraycopy(hsvo2, 0, ColorPickerDialog.this.mHSVO, 0, ColorPickerDialog.this.mHSVO.length);
                Color.HSVToColor(hsvo2);
                ColorPickerDialog.this.setButtonColor(ColorPickerDialog.this.mSelectedButton, hsvo2);
            }

            @Override
            public void addColorListener(ColorListener l) {
            }
        };
        for (ColorListener colorListener2 : c) {
            colorListener2.addColorListener(colorListener);
        }
        setOnShowListener((FilterShowActivity) context);
        setOnDismissListener((FilterShowActivity) context);
    }

    public void setOrigColor(float[] hsvo) {
        this.mColorCompareView.setOrigColor(hsvo);
    }

    public void setColor(float[] hsvo) {
        this.mColorOpacityView.setColor(hsvo);
        this.mColorHueView.setColor(hsvo);
        this.mColorSVRectView.setColor(hsvo);
        this.mColorCompareView.setColor(hsvo);
    }

    private void setButtonColor(ToggleButton button, float[] hsv) {
        if (button != null) {
            int color = Color.HSVToColor(hsv);
            button.setBackgroundColor(color);
            float[] fg = new float[3];
            fg[0] = (hsv[0] + 180.0f) % 360.0f;
            fg[1] = hsv[1];
            fg[2] = hsv[2] > 0.5f ? 0.1f : 0.9f;
            button.setTextColor(Color.HSVToColor(fg));
            button.setTag(hsv);
        }
    }
}

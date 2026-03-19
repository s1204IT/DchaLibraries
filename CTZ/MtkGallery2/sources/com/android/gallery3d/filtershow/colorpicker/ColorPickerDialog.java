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

    public ColorPickerDialog(Context context, final ColorListener colorListener) {
        super(context);
        this.mHSVO = new float[4];
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
        getWindow().setLayout((displayMetrics.widthPixels * 8) / 10, (displayMetrics.heightPixels * 8) / 10);
        requestWindowFeature(1);
        setContentView(R.layout.filtershow_color_picker);
        this.mColorHueView = (ColorHueView) findViewById(R.id.ColorHueView);
        this.mColorSVRectView = (ColorSVRectView) findViewById(R.id.colorRectView);
        this.mColorOpacityView = (ColorOpacityView) findViewById(R.id.colorOpacityView);
        this.mColorCompareView = (ColorCompareView) findViewById(R.id.btnSelect);
        float[] fArr = {123.0f, 0.9f, 1.0f, 1.0f};
        ImageButton imageButton = (ImageButton) findViewById(R.id.applyColorPick);
        ImageButton imageButton2 = (ImageButton) findViewById(R.id.cancelColorPick);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                colorListener.setColor(ColorPickerDialog.this.mHSVO);
                ColorPickerDialog.this.dismiss();
            }
        });
        imageButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorPickerDialog.this.dismiss();
            }
        });
        ColorListener[] colorListenerArr = {this.mColorCompareView, this.mColorSVRectView, this.mColorOpacityView, this.mColorHueView};
        for (int i = 0; i < colorListenerArr.length; i++) {
            colorListenerArr[i].setColor(fArr);
            for (int i2 = 0; i2 < colorListenerArr.length; i2++) {
                if (i != i2) {
                    colorListenerArr[i].addColorListener(colorListenerArr[i2]);
                }
            }
        }
        ColorListener colorListener2 = new ColorListener() {
            @Override
            public void setColor(float[] fArr2) {
                System.arraycopy(fArr2, 0, ColorPickerDialog.this.mHSVO, 0, ColorPickerDialog.this.mHSVO.length);
                Color.HSVToColor(fArr2);
                ColorPickerDialog.this.setButtonColor(ColorPickerDialog.this.mSelectedButton, fArr2);
            }

            @Override
            public void addColorListener(ColorListener colorListener3) {
            }
        };
        for (ColorListener colorListener3 : colorListenerArr) {
            colorListener3.addColorListener(colorListener2);
        }
        FilterShowActivity filterShowActivity = (FilterShowActivity) context;
        setOnShowListener(filterShowActivity);
        setOnDismissListener(filterShowActivity);
    }

    public void setOrigColor(float[] fArr) {
        this.mColorCompareView.setOrigColor(fArr);
    }

    public void setColor(float[] fArr) {
        this.mColorOpacityView.setColor(fArr);
        this.mColorHueView.setColor(fArr);
        this.mColorSVRectView.setColor(fArr);
        this.mColorCompareView.setColor(fArr);
    }

    private void setButtonColor(ToggleButton toggleButton, float[] fArr) {
        if (toggleButton == null) {
            return;
        }
        toggleButton.setBackgroundColor(Color.HSVToColor(fArr));
        float[] fArr2 = new float[3];
        fArr2[0] = (fArr[0] + 180.0f) % 360.0f;
        fArr2[1] = fArr[1];
        fArr2[2] = fArr[2] > 0.5f ? 0.1f : 0.9f;
        toggleButton.setTextColor(Color.HSVToColor(fArr2));
        toggleButton.setTag(fArr);
    }
}

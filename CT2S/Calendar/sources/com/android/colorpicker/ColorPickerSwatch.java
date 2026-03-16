package com.android.colorpicker;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class ColorPickerSwatch extends FrameLayout implements View.OnClickListener {
    private ImageView mCheckmarkImage;
    private int mColor;
    private OnColorSelectedListener mOnColorSelectedListener;
    private ImageView mSwatchImage;

    public interface OnColorSelectedListener {
        void onColorSelected(int i);
    }

    public ColorPickerSwatch(Context context, int color, boolean checked, OnColorSelectedListener listener) {
        super(context);
        this.mColor = color;
        this.mOnColorSelectedListener = listener;
        LayoutInflater.from(context).inflate(R.layout.color_picker_swatch, this);
        this.mSwatchImage = (ImageView) findViewById(R.id.color_picker_swatch);
        this.mCheckmarkImage = (ImageView) findViewById(R.id.color_picker_checkmark);
        setColor(color);
        setChecked(checked);
        setOnClickListener(this);
    }

    protected void setColor(int color) {
        Drawable[] colorDrawable = {getContext().getResources().getDrawable(R.drawable.color_picker_swatch)};
        this.mSwatchImage.setImageDrawable(new ColorStateDrawable(colorDrawable, color));
    }

    private void setChecked(boolean checked) {
        if (checked) {
            this.mCheckmarkImage.setVisibility(0);
        } else {
            this.mCheckmarkImage.setVisibility(8);
        }
    }

    @Override
    public void onClick(View v) {
        if (this.mOnColorSelectedListener != null) {
            this.mOnColorSelectedListener.onColorSelected(this.mColor);
        }
    }
}

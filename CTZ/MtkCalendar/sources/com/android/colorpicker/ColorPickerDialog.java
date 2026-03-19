package com.android.colorpicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import com.android.colorpicker.ColorPickerSwatch;

public class ColorPickerDialog extends DialogFragment implements ColorPickerSwatch.OnColorSelectedListener {
    protected AlertDialog mAlertDialog;
    protected int mColumns;
    protected ColorPickerSwatch.OnColorSelectedListener mListener;
    private ColorPickerPalette mPalette;
    private ProgressBar mProgress;
    protected int mSelectedColor;
    protected int mSize;
    protected int mTitleResId = R.string.color_picker_default_title;
    protected int[] mColors = null;
    protected String[] mColorContentDescriptions = null;

    public void initialize(int i, int[] iArr, int i2, int i3, int i4) {
        setArguments(i, i3, i4);
        setColors(iArr, i2);
    }

    public void setArguments(int i, int i2, int i3) {
        Bundle bundle = new Bundle();
        bundle.putInt("title_id", i);
        bundle.putInt("columns", i2);
        bundle.putInt("size", i3);
        setArguments(bundle);
    }

    public void setOnColorSelectedListener(ColorPickerSwatch.OnColorSelectedListener onColorSelectedListener) {
        this.mListener = onColorSelectedListener;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getArguments() != null) {
            this.mTitleResId = getArguments().getInt("title_id");
            this.mColumns = getArguments().getInt("columns");
            this.mSize = getArguments().getInt("size");
        }
        if (bundle != null) {
            this.mColors = bundle.getIntArray("colors");
            this.mSelectedColor = ((Integer) bundle.getSerializable("selected_color")).intValue();
            this.mColorContentDescriptions = bundle.getStringArray("color_content_descriptions");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Activity activity = getActivity();
        View viewInflate = LayoutInflater.from(getActivity()).inflate(R.layout.color_picker_dialog, (ViewGroup) null);
        this.mProgress = (ProgressBar) viewInflate.findViewById(android.R.id.progress);
        this.mPalette = (ColorPickerPalette) viewInflate.findViewById(R.id.color_picker);
        this.mPalette.init(this.mSize, this.mColumns, this);
        if (this.mColors != null) {
            showPaletteView();
        }
        this.mAlertDialog = new AlertDialog.Builder(activity).setTitle(this.mTitleResId).setView(viewInflate).create();
        return this.mAlertDialog;
    }

    @Override
    public void onColorSelected(int i) {
        if (this.mListener != null) {
            this.mListener.onColorSelected(i);
        }
        if (getTargetFragment() instanceof ColorPickerSwatch.OnColorSelectedListener) {
            ((ColorPickerSwatch.OnColorSelectedListener) getTargetFragment()).onColorSelected(i);
        }
        if (i != this.mSelectedColor) {
            this.mSelectedColor = i;
            this.mPalette.drawPalette(this.mColors, this.mSelectedColor);
        }
        dismiss();
    }

    public void showPaletteView() {
        if (this.mProgress != null && this.mPalette != null) {
            this.mProgress.setVisibility(8);
            refreshPalette();
            this.mPalette.setVisibility(0);
        }
    }

    public void showProgressBarView() {
        if (this.mProgress != null && this.mPalette != null) {
            this.mProgress.setVisibility(0);
            this.mPalette.setVisibility(8);
        }
    }

    public void setColors(int[] iArr, int i) {
        if (this.mColors != iArr || this.mSelectedColor != i) {
            this.mColors = iArr;
            this.mSelectedColor = i;
            refreshPalette();
        }
    }

    private void refreshPalette() {
        if (this.mPalette != null && this.mColors != null) {
            this.mPalette.drawPalette(this.mColors, this.mSelectedColor, this.mColorContentDescriptions);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putIntArray("colors", this.mColors);
        bundle.putSerializable("selected_color", Integer.valueOf(this.mSelectedColor));
        bundle.putStringArray("color_content_descriptions", this.mColorContentDescriptions);
    }
}

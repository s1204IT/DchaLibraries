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

    public void initialize(int titleResId, int[] colors, int selectedColor, int columns, int size) {
        setArguments(titleResId, columns, size);
        setColors(colors, selectedColor);
    }

    public void setArguments(int titleResId, int columns, int size) {
        Bundle bundle = new Bundle();
        bundle.putInt("title_id", titleResId);
        bundle.putInt("columns", columns);
        bundle.putInt("size", size);
        setArguments(bundle);
    }

    public void setOnColorSelectedListener(ColorPickerSwatch.OnColorSelectedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.mTitleResId = getArguments().getInt("title_id");
            this.mColumns = getArguments().getInt("columns");
            this.mSize = getArguments().getInt("size");
        }
        if (savedInstanceState != null) {
            this.mColors = savedInstanceState.getIntArray("colors");
            this.mSelectedColor = ((Integer) savedInstanceState.getSerializable("selected_color")).intValue();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.color_picker_dialog, (ViewGroup) null);
        this.mProgress = (ProgressBar) view.findViewById(android.R.id.progress);
        this.mPalette = (ColorPickerPalette) view.findViewById(R.id.color_picker);
        this.mPalette.init(this.mSize, this.mColumns, this);
        if (this.mColors != null) {
            showPaletteView();
        }
        this.mAlertDialog = new AlertDialog.Builder(activity).setTitle(this.mTitleResId).setView(view).create();
        return this.mAlertDialog;
    }

    @Override
    public void onColorSelected(int color) {
        if (this.mListener != null) {
            this.mListener.onColorSelected(color);
        }
        if (getTargetFragment() instanceof ColorPickerSwatch.OnColorSelectedListener) {
            ColorPickerSwatch.OnColorSelectedListener listener = (ColorPickerSwatch.OnColorSelectedListener) getTargetFragment();
            listener.onColorSelected(color);
        }
        if (color != this.mSelectedColor) {
            this.mSelectedColor = color;
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

    public void setColors(int[] colors, int selectedColor) {
        if (this.mColors != colors || this.mSelectedColor != selectedColor) {
            this.mColors = colors;
            this.mSelectedColor = selectedColor;
            refreshPalette();
        }
    }

    private void refreshPalette() {
        if (this.mPalette != null && this.mColors != null) {
            this.mPalette.drawPalette(this.mColors, this.mSelectedColor);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray("colors", this.mColors);
        outState.putSerializable("selected_color", Integer.valueOf(this.mSelectedColor));
    }
}

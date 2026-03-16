package com.android.colorpicker;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import com.android.colorpicker.ColorPickerSwatch;

public class ColorPickerPalette extends TableLayout {
    private String mDescription;
    private String mDescriptionSelected;
    private int mMarginSize;
    private int mNumColumns;
    public ColorPickerSwatch.OnColorSelectedListener mOnColorSelectedListener;
    private int mSwatchLength;

    public ColorPickerPalette(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPickerPalette(Context context) {
        super(context);
    }

    public void init(int size, int columns, ColorPickerSwatch.OnColorSelectedListener listener) {
        this.mNumColumns = columns;
        Resources res = getResources();
        if (size == 1) {
            this.mSwatchLength = res.getDimensionPixelSize(R.dimen.color_swatch_large);
            this.mMarginSize = res.getDimensionPixelSize(R.dimen.color_swatch_margins_large);
        } else {
            this.mSwatchLength = res.getDimensionPixelSize(R.dimen.color_swatch_small);
            this.mMarginSize = res.getDimensionPixelSize(R.dimen.color_swatch_margins_small);
        }
        this.mOnColorSelectedListener = listener;
        this.mDescription = res.getString(R.string.color_swatch_description);
        this.mDescriptionSelected = res.getString(R.string.color_swatch_description_selected);
    }

    private TableRow createTableRow() {
        TableRow row = new TableRow(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(-2, -2);
        row.setLayoutParams(params);
        return row;
    }

    public void drawPalette(int[] colors, int selectedColor) {
        if (colors != null) {
            removeAllViews();
            int tableElements = 0;
            int rowElements = 0;
            int rowNumber = 0;
            TableRow row = createTableRow();
            int len$ = colors.length;
            for (int i$ = 0; i$ < len$; i$++) {
                int color = colors[i$];
                tableElements++;
                View colorSwatch = createColorSwatch(color, selectedColor);
                setSwatchDescription(rowNumber, tableElements, rowElements, color == selectedColor, colorSwatch);
                addSwatchToRow(row, colorSwatch, rowNumber);
                rowElements++;
                if (rowElements == this.mNumColumns) {
                    addView(row);
                    row = createTableRow();
                    rowElements = 0;
                    rowNumber++;
                }
            }
            if (rowElements > 0) {
                while (rowElements != this.mNumColumns) {
                    addSwatchToRow(row, createBlankSpace(), rowNumber);
                    rowElements++;
                }
                addView(row);
            }
        }
    }

    private static void addSwatchToRow(TableRow row, View swatch, int rowNumber) {
        if (rowNumber % 2 == 0) {
            row.addView(swatch);
        } else {
            row.addView(swatch, 0);
        }
    }

    private void setSwatchDescription(int rowNumber, int index, int rowElements, boolean selected, View swatch) {
        int accessibilityIndex;
        String description;
        if (rowNumber % 2 == 0) {
            accessibilityIndex = index;
        } else {
            int rowMax = (rowNumber + 1) * this.mNumColumns;
            accessibilityIndex = rowMax - rowElements;
        }
        if (selected) {
            description = String.format(this.mDescriptionSelected, Integer.valueOf(accessibilityIndex));
        } else {
            description = String.format(this.mDescription, Integer.valueOf(accessibilityIndex));
        }
        swatch.setContentDescription(description);
    }

    private ImageView createBlankSpace() {
        ImageView view = new ImageView(getContext());
        TableRow.LayoutParams params = new TableRow.LayoutParams(this.mSwatchLength, this.mSwatchLength);
        params.setMargins(this.mMarginSize, this.mMarginSize, this.mMarginSize, this.mMarginSize);
        view.setLayoutParams(params);
        return view;
    }

    private ColorPickerSwatch createColorSwatch(int color, int selectedColor) {
        ColorPickerSwatch view = new ColorPickerSwatch(getContext(), color, color == selectedColor, this.mOnColorSelectedListener);
        TableRow.LayoutParams params = new TableRow.LayoutParams(this.mSwatchLength, this.mSwatchLength);
        params.setMargins(this.mMarginSize, this.mMarginSize, this.mMarginSize, this.mMarginSize);
        view.setLayoutParams(params);
        return view;
    }
}

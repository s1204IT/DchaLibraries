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

    public ColorPickerPalette(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public ColorPickerPalette(Context context) {
        super(context);
    }

    public void init(int i, int i2, ColorPickerSwatch.OnColorSelectedListener onColorSelectedListener) {
        this.mNumColumns = i2;
        Resources resources = getResources();
        if (i == 1) {
            this.mSwatchLength = resources.getDimensionPixelSize(R.dimen.color_swatch_large);
            this.mMarginSize = resources.getDimensionPixelSize(R.dimen.color_swatch_margins_large);
        } else {
            this.mSwatchLength = resources.getDimensionPixelSize(R.dimen.color_swatch_small);
            this.mMarginSize = resources.getDimensionPixelSize(R.dimen.color_swatch_margins_small);
        }
        this.mOnColorSelectedListener = onColorSelectedListener;
        this.mDescription = resources.getString(R.string.color_swatch_description);
        this.mDescriptionSelected = resources.getString(R.string.color_swatch_description_selected);
    }

    private TableRow createTableRow() {
        TableRow tableRow = new TableRow(getContext());
        tableRow.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
        return tableRow;
    }

    public void drawPalette(int[] iArr, int i) {
        drawPalette(iArr, i, null);
    }

    public void drawPalette(int[] iArr, int i, String[] strArr) {
        if (iArr == null) {
            return;
        }
        removeAllViews();
        TableRow tableRowCreateTableRow = createTableRow();
        int length = iArr.length;
        TableRow tableRowCreateTableRow2 = tableRowCreateTableRow;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 0; i5 < length; i5++) {
            int i6 = iArr[i5];
            ColorPickerSwatch colorPickerSwatchCreateColorSwatch = createColorSwatch(i6, i);
            TableRow tableRow = tableRowCreateTableRow2;
            setSwatchDescription(i2, i3, i4, i6 == i, colorPickerSwatchCreateColorSwatch, strArr);
            addSwatchToRow(tableRow, colorPickerSwatchCreateColorSwatch, i2);
            i3++;
            int i7 = i4 + 1;
            if (i7 == this.mNumColumns) {
                addView(tableRow);
                i2++;
                tableRowCreateTableRow2 = createTableRow();
                i4 = 0;
            } else {
                i4 = i7;
                tableRowCreateTableRow2 = tableRow;
            }
        }
        TableRow tableRow2 = tableRowCreateTableRow2;
        if (i4 > 0) {
            while (i4 != this.mNumColumns) {
                addSwatchToRow(tableRow2, createBlankSpace(), i2);
                i4++;
            }
            addView(tableRow2);
        }
    }

    private static void addSwatchToRow(TableRow tableRow, View view, int i) {
        if (i % 2 == 0) {
            tableRow.addView(view);
        } else {
            tableRow.addView(view, 0);
        }
    }

    private void setSwatchDescription(int i, int i2, int i3, boolean z, View view, String[] strArr) {
        int i4;
        String str;
        if (strArr != null && strArr.length > i2) {
            str = strArr[i2];
        } else {
            if (i % 2 == 0) {
                i4 = i2 + 1;
            } else {
                i4 = ((i + 1) * this.mNumColumns) - i3;
            }
            if (z) {
                str = String.format(this.mDescriptionSelected, Integer.valueOf(i4));
            } else {
                str = String.format(this.mDescription, Integer.valueOf(i4));
            }
        }
        view.setContentDescription(str);
    }

    private ImageView createBlankSpace() {
        ImageView imageView = new ImageView(getContext());
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(this.mSwatchLength, this.mSwatchLength);
        layoutParams.setMargins(this.mMarginSize, this.mMarginSize, this.mMarginSize, this.mMarginSize);
        imageView.setLayoutParams(layoutParams);
        return imageView;
    }

    private ColorPickerSwatch createColorSwatch(int i, int i2) {
        ColorPickerSwatch colorPickerSwatch = new ColorPickerSwatch(getContext(), i, i == i2, this.mOnColorSelectedListener);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(this.mSwatchLength, this.mSwatchLength);
        layoutParams.setMargins(this.mMarginSize, this.mMarginSize, this.mMarginSize, this.mMarginSize);
        colorPickerSwatch.setLayoutParams(layoutParams);
        return colorPickerSwatch;
    }
}

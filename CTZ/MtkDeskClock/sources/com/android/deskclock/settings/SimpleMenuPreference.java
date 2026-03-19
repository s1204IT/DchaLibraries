package com.android.deskclock.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.DropDownPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.android.deskclock.R;
import com.android.deskclock.Utils;

public class SimpleMenuPreference extends DropDownPreference {
    private SimpleMenuAdapter mAdapter;

    public SimpleMenuPreference(Context context) {
        this(context, null);
    }

    public SimpleMenuPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.dropdownPreferenceStyle);
    }

    public SimpleMenuPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SimpleMenuPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override
    protected ArrayAdapter createAdapter() {
        this.mAdapter = new SimpleMenuAdapter(getContext(), R.layout.simple_menu_dropdown_item);
        return this.mAdapter;
    }

    private static void restoreOriginalOrder(CharSequence[] charSequenceArr, int i) {
        CharSequence charSequence = charSequenceArr[0];
        System.arraycopy(charSequenceArr, 1, charSequenceArr, 0, i);
        charSequenceArr[i] = charSequence;
    }

    private static void swapSelectedToFront(CharSequence[] charSequenceArr, int i) {
        CharSequence charSequence = charSequenceArr[i];
        System.arraycopy(charSequenceArr, 0, charSequenceArr, 1, i);
        charSequenceArr[0] = charSequence;
    }

    private static void setSelectedPosition(CharSequence[] charSequenceArr, int i, int i2) {
        CharSequence charSequence = charSequenceArr[i2];
        restoreOriginalOrder(charSequenceArr, i);
        swapSelectedToFront(charSequenceArr, Utils.indexOf(charSequenceArr, charSequence));
    }

    @Override
    public void setSummary(CharSequence charSequence) {
        CharSequence[] entries = getEntries();
        int iIndexOf = Utils.indexOf(entries, charSequence);
        if (iIndexOf == -1) {
            throw new IllegalArgumentException("Illegal Summary");
        }
        int lastSelectedOriginalPosition = this.mAdapter.getLastSelectedOriginalPosition();
        this.mAdapter.setSelectedPosition(iIndexOf);
        setSelectedPosition(entries, lastSelectedOriginalPosition, iIndexOf);
        setSelectedPosition(getEntryValues(), lastSelectedOriginalPosition, iIndexOf);
        super.setSummary(charSequence);
    }

    private static final class SimpleMenuAdapter extends ArrayAdapter<CharSequence> {
        private int mLastSelectedOriginalPosition;

        SimpleMenuAdapter(Context context, int i) {
            super(context, i);
            this.mLastSelectedOriginalPosition = 0;
        }

        private void restoreOriginalOrder() {
            CharSequence item = getItem(0);
            remove(item);
            insert(item, this.mLastSelectedOriginalPosition);
        }

        private void swapSelectedToFront(int i) {
            CharSequence item = getItem(i);
            remove(item);
            insert(item, 0);
            this.mLastSelectedOriginalPosition = i;
        }

        int getLastSelectedOriginalPosition() {
            return this.mLastSelectedOriginalPosition;
        }

        void setSelectedPosition(int i) {
            setNotifyOnChange(false);
            CharSequence item = getItem(i);
            restoreOriginalOrder();
            swapSelectedToFront(getPosition(item));
            notifyDataSetChanged();
        }

        @Override
        public View getDropDownView(int i, View view, @NonNull ViewGroup viewGroup) {
            View dropDownView = super.getDropDownView(i, view, viewGroup);
            if (i == 0) {
                dropDownView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.white_08p));
            } else {
                dropDownView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
            }
            return dropDownView;
        }
    }
}

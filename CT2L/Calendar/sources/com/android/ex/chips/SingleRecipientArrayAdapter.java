package com.android.ex.chips;

import android.content.Context;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.android.ex.chips.DropdownChipLayouter;

class SingleRecipientArrayAdapter extends ArrayAdapter<RecipientEntry> {
    private final StateListDrawable mDeleteDrawable;
    private final DropdownChipLayouter mDropdownChipLayouter;

    public SingleRecipientArrayAdapter(Context context, RecipientEntry entry, DropdownChipLayouter dropdownChipLayouter, StateListDrawable deleteDrawable) {
        super(context, dropdownChipLayouter.getAlternateItemLayoutResId(DropdownChipLayouter.AdapterType.SINGLE_RECIPIENT), new RecipientEntry[]{entry});
        this.mDropdownChipLayouter = dropdownChipLayouter;
        this.mDeleteDrawable = deleteDrawable;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return this.mDropdownChipLayouter.bindView(convertView, parent, getItem(position), position, DropdownChipLayouter.AdapterType.SINGLE_RECIPIENT, null, this.mDeleteDrawable);
    }
}

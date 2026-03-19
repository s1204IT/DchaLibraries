package com.android.documentsui.dirlist;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Space;
import com.android.documentsui.selection.ItemDetailsLookup;

abstract class MessageHolder extends DocumentHolder {
    public MessageHolder(Context context, Space space) {
        super(context, space);
    }

    public MessageHolder(Context context, ViewGroup viewGroup, int i) {
        super(context, viewGroup, i);
    }

    @Override
    public ItemDetailsLookup.ItemDetails getItemDetails() {
        return null;
    }
}

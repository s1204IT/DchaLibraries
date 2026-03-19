package com.android.managedprovisioning.common;

import android.content.res.Resources;
import com.android.managedprovisioning.R;
import java.util.List;

public class StringConcatenator {
    private final Resources mResources;

    public StringConcatenator(Resources resources) {
        this.mResources = resources;
    }

    public String join(List<String> list) {
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return "";
        }
        int size = list.size();
        if (size == 1) {
            return list.get(0);
        }
        if (size == 2) {
            return this.mResources.getString(R.string.join_two_items, list.get(0), list.get(1));
        }
        String string = list.get(size - 2);
        int i = size - 3;
        while (i >= 0) {
            string = this.mResources.getString(i == 0 ? R.string.join_many_items_first : R.string.join_many_items_middle, list.get(i), string);
            i--;
        }
        return this.mResources.getString(R.string.join_many_items_last, string, list.get(size - 1));
    }
}

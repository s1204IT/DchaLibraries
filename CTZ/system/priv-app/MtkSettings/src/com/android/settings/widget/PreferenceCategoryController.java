package com.android.settings.widget;

import android.content.Context;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class PreferenceCategoryController extends BasePreferenceController {
    private final List<AbstractPreferenceController> mChildren;
    private final String mKey;

    public PreferenceCategoryController(Context context, String str) {
        super(context, str);
        this.mKey = str;
        this.mChildren = new ArrayList();
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mChildren == null || this.mChildren.isEmpty()) {
            return 2;
        }
        Iterator<AbstractPreferenceController> it = this.mChildren.iterator();
        while (it.hasNext()) {
            if (it.next().isAvailable()) {
                return 0;
            }
        }
        return 1;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return this.mKey;
    }

    public PreferenceCategoryController setChildren(List<AbstractPreferenceController> list) {
        this.mChildren.clear();
        if (list != null) {
            this.mChildren.addAll(list);
        }
        return this;
    }
}

package com.mediatek.systemui.qs.tiles.ext;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import com.android.systemui.plugins.qs.QSTile;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;

public class QsIconWrapper extends QSTile.Icon {
    private static SparseArray<QsIconWrapper> sQsIconWrapperMap = new SparseArray<>();
    private final IconIdWrapper mIconWrapper;

    public QsIconWrapper(IconIdWrapper iconIdWrapper) {
        this.mIconWrapper = iconIdWrapper;
    }

    public static QsIconWrapper get(int i, IconIdWrapper iconIdWrapper) {
        QsIconWrapper qsIconWrapper = sQsIconWrapperMap.get(i);
        if (qsIconWrapper == null) {
            QsIconWrapper qsIconWrapper2 = new QsIconWrapper(iconIdWrapper);
            sQsIconWrapperMap.put(i, qsIconWrapper2);
            return qsIconWrapper2;
        }
        return qsIconWrapper;
    }

    @Override
    public Drawable getDrawable(Context context) {
        return this.mIconWrapper.getDrawable();
    }

    @Override
    public int hashCode() {
        return this.mIconWrapper.hashCode();
    }

    public boolean equals(Object obj) {
        return this.mIconWrapper.equals(obj);
    }
}

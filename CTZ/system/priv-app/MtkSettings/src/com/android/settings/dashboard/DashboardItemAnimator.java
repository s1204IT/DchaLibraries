package com.android.settings.dashboard;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import com.android.settingslib.drawer.Tile;

/* loaded from: classes.dex */
public class DashboardItemAnimator extends DefaultItemAnimator {
    @Override // android.support.v7.widget.DefaultItemAnimator, android.support.v7.widget.SimpleItemAnimator
    public boolean animateChange(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder viewHolder2, int i, int i2, int i3, int i4) {
        if ((viewHolder.itemView.getTag() instanceof Tile) && viewHolder == viewHolder2) {
            if (!isRunning()) {
                i = (int) (i + ViewCompat.getTranslationX(viewHolder.itemView));
                i2 = (int) (i2 + ViewCompat.getTranslationY(viewHolder.itemView));
            }
            if (i == i3 && i2 == i4) {
                dispatchMoveFinished(viewHolder);
                return false;
            }
        }
        return super.animateChange(viewHolder, viewHolder2, i, i2, i3, i4);
    }
}

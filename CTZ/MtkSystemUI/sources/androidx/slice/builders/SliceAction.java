package androidx.slice.builders;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.support.v4.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.core.SliceActionImpl;

public class SliceAction implements androidx.slice.core.SliceAction {
    private SliceActionImpl mSliceAction;

    @Deprecated
    public SliceAction(PendingIntent action, Icon actionIcon, CharSequence actionTitle) {
        this(action, actionIcon, 0, actionTitle);
    }

    @Deprecated
    public SliceAction(PendingIntent action, Icon actionIcon, int imageMode, CharSequence actionTitle) {
        this(action, IconCompat.createFromIcon(actionIcon), imageMode, actionTitle);
    }

    public SliceAction(PendingIntent action, IconCompat actionIcon, int imageMode, CharSequence actionTitle) {
        this.mSliceAction = new SliceActionImpl(action, actionIcon, imageMode, actionTitle);
    }

    @Override
    public IconCompat getIcon() {
        return this.mSliceAction.getIcon();
    }

    @Override
    public boolean isToggle() {
        return this.mSliceAction.isToggle();
    }

    @Override
    public int getImageMode() {
        return this.mSliceAction.getImageMode();
    }

    public Slice buildSlice(Slice.Builder builder) {
        return this.mSliceAction.buildSlice(builder);
    }
}

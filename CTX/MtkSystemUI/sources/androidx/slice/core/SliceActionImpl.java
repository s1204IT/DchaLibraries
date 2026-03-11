package androidx.slice.core;

import android.app.PendingIntent;
import android.support.v4.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;

public class SliceActionImpl implements SliceAction {
    private PendingIntent mAction;
    private SliceItem mActionItem;
    private CharSequence mContentDescription;
    private IconCompat mIcon;
    private int mImageMode;
    private boolean mIsChecked;
    private boolean mIsToggle;
    private int mPriority;
    private SliceItem mSliceItem;
    private CharSequence mTitle;

    public SliceActionImpl(PendingIntent action, IconCompat actionIcon, int imageMode, CharSequence actionTitle) {
        this.mImageMode = 3;
        this.mPriority = -1;
        this.mAction = action;
        this.mIcon = actionIcon;
        this.mTitle = actionTitle;
        this.mImageMode = imageMode;
    }

    public SliceActionImpl(SliceItem slice) {
        int i;
        this.mImageMode = 3;
        this.mPriority = -1;
        this.mSliceItem = slice;
        SliceItem actionItem = SliceQuery.find(slice, "action");
        if (actionItem == null) {
            return;
        }
        this.mActionItem = actionItem;
        SliceItem iconItem = SliceQuery.find(actionItem.getSlice(), "image");
        if (iconItem != null) {
            this.mIcon = iconItem.getIcon();
            if (iconItem.hasHint("no_tint")) {
                i = iconItem.hasHint("large") ? 2 : 1;
            } else {
                i = 0;
            }
            this.mImageMode = i;
        }
        SliceItem titleItem = SliceQuery.find(actionItem.getSlice(), "text", "title", (String) null);
        if (titleItem != null) {
            this.mTitle = titleItem.getText();
        }
        SliceItem cdItem = SliceQuery.findSubtype(actionItem.getSlice(), "text", "content_description");
        if (cdItem != null) {
            this.mContentDescription = cdItem.getText();
        }
        this.mIsToggle = "toggle".equals(actionItem.getSubType());
        if (this.mIsToggle) {
            this.mIsChecked = actionItem.hasHint("selected");
        }
        SliceItem priority = SliceQuery.findSubtype(actionItem.getSlice(), "int", "priority");
        this.mPriority = priority != null ? priority.getInt() : -1;
    }

    public PendingIntent getAction() {
        return this.mAction != null ? this.mAction : this.mActionItem.getAction();
    }

    public SliceItem getActionItem() {
        return this.mActionItem;
    }

    @Override
    public IconCompat getIcon() {
        return this.mIcon;
    }

    public CharSequence getTitle() {
        return this.mTitle;
    }

    public CharSequence getContentDescription() {
        return this.mContentDescription;
    }

    @Override
    public boolean isToggle() {
        return this.mIsToggle;
    }

    public boolean isChecked() {
        return this.mIsChecked;
    }

    @Override
    public int getImageMode() {
        return this.mImageMode;
    }

    public boolean isDefaultToggle() {
        return this.mIsToggle && this.mIcon == null;
    }

    public SliceItem getSliceItem() {
        return this.mSliceItem;
    }

    public Slice buildSlice(Slice.Builder builder) {
        Slice.Builder sb = new Slice.Builder(builder);
        if (this.mIcon != null) {
            String[] hints = this.mImageMode == 0 ? new String[0] : new String[]{"no_tint"};
            sb.addIcon(this.mIcon, (String) null, hints);
        }
        if (this.mTitle != null) {
            sb.addText(this.mTitle, (String) null, "title");
        }
        if (this.mContentDescription != null) {
            sb.addText(this.mContentDescription, "content_description", new String[0]);
        }
        if (this.mIsToggle && this.mIsChecked) {
            sb.addHints("selected");
        }
        if (this.mPriority != -1) {
            sb.addInt(this.mPriority, "priority", new String[0]);
        }
        String subtype = this.mIsToggle ? "toggle" : null;
        builder.addHints("shortcut");
        builder.addAction(this.mAction, sb.build(), subtype);
        return builder.build();
    }
}

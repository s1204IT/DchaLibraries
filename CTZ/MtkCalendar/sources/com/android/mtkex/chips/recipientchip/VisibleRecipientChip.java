package com.android.mtkex.chips.recipientchip;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.android.mtkex.chips.RecipientEntry;

public class VisibleRecipientChip extends ReplacementDrawableSpan implements DrawableRecipientChip {
    private final SimpleRecipientChip mDelegate;

    public VisibleRecipientChip(Drawable drawable, RecipientEntry recipientEntry) {
        super(drawable);
        this.mDelegate = new SimpleRecipientChip(recipientEntry);
    }

    @Override
    public void setSelected(boolean z) {
        this.mDelegate.setSelected(z);
    }

    @Override
    public boolean isSelected() {
        return this.mDelegate.isSelected();
    }

    @Override
    public CharSequence getValue() {
        return this.mDelegate.getValue();
    }

    @Override
    public long getContactId() {
        return this.mDelegate.getContactId();
    }

    @Override
    public long getDataId() {
        return this.mDelegate.getDataId();
    }

    @Override
    public RecipientEntry getEntry() {
        return this.mDelegate.getEntry();
    }

    @Override
    public void setOriginalText(String str) {
        this.mDelegate.setOriginalText(str);
    }

    @Override
    public CharSequence getOriginalText() {
        return this.mDelegate.getOriginalText();
    }

    @Override
    public Rect getBounds() {
        return super.getBounds();
    }

    @Override
    public void draw(Canvas canvas) {
        this.mDrawable.draw(canvas);
    }

    public String toString() {
        return this.mDelegate.toString();
    }
}

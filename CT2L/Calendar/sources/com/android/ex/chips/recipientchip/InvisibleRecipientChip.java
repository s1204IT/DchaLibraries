package com.android.ex.chips.recipientchip;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.ReplacementSpan;
import com.android.ex.chips.RecipientEntry;

public class InvisibleRecipientChip extends ReplacementSpan implements DrawableRecipientChip {
    private final SimpleRecipientChip mDelegate;

    public InvisibleRecipientChip(RecipientEntry entry) {
        this.mDelegate = new SimpleRecipientChip(entry);
    }

    @Override
    public void setSelected(boolean selected) {
        this.mDelegate.setSelected(selected);
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
    public Long getDirectoryId() {
        return this.mDelegate.getDirectoryId();
    }

    @Override
    public String getLookupKey() {
        return this.mDelegate.getLookupKey();
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
    public void setOriginalText(String text) {
        this.mDelegate.setOriginalText(text);
    }

    @Override
    public CharSequence getOriginalText() {
        return this.mDelegate.getOriginalText();
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return 0;
    }

    @Override
    public Rect getBounds() {
        return new Rect(0, 0, 0, 0);
    }

    @Override
    public void draw(Canvas canvas) {
    }
}

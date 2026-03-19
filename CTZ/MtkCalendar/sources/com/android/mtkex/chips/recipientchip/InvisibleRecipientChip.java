package com.android.mtkex.chips.recipientchip;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.ReplacementSpan;
import com.android.mtkex.chips.RecipientEntry;

public class InvisibleRecipientChip extends ReplacementSpan implements DrawableRecipientChip {
    private final SimpleRecipientChip mDelegate;

    public InvisibleRecipientChip(RecipientEntry recipientEntry) {
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
    public void draw(Canvas canvas, CharSequence charSequence, int i, int i2, float f, int i3, int i4, int i5, Paint paint) {
    }

    @Override
    public int getSize(Paint paint, CharSequence charSequence, int i, int i2, Paint.FontMetricsInt fontMetricsInt) {
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

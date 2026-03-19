package com.android.mtkex.chips.recipientchip;

import android.graphics.Canvas;
import android.graphics.Rect;

public interface DrawableRecipientChip extends BaseRecipientChip {
    void draw(Canvas canvas);

    Rect getBounds();
}

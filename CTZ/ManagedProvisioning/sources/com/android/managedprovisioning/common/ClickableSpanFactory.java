package com.android.managedprovisioning.common;

import android.content.Intent;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

public class ClickableSpanFactory {
    private final int linkColor;

    public ClickableSpanFactory(int i) {
        this.linkColor = i;
    }

    public ClickableSpan create(final Intent intent) {
        return new ClickableSpan() {
            @Override
            public void onClick(View view) {
                view.playSoundEffect(0);
                view.getContext().startActivity(intent);
            }

            @Override
            public void updateDrawState(TextPaint textPaint) {
                super.updateDrawState(textPaint);
                textPaint.setUnderlineText(false);
                textPaint.setColor(ClickableSpanFactory.this.linkColor);
            }
        };
    }
}

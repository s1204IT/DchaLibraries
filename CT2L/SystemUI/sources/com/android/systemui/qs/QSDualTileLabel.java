package com.android.systemui.qs;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R;
import java.util.Objects;

public class QSDualTileLabel extends LinearLayout {
    private final Context mContext;
    private final TextView mFirstLine;
    private final ImageView mFirstLineCaret;
    private final int mHorizontalPaddingPx;
    private final TextView mSecondLine;
    private String mText;
    private final Runnable mUpdateText;

    public QSDualTileLabel(Context context) {
        super(context);
        this.mUpdateText = new Runnable() {
            @Override
            public void run() {
                QSDualTileLabel.this.updateText();
            }
        };
        this.mContext = context;
        setOrientation(1);
        this.mHorizontalPaddingPx = this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_dual_tile_padding_horizontal);
        this.mFirstLine = initTextView();
        this.mFirstLine.setPadding(this.mHorizontalPaddingPx, 0, this.mHorizontalPaddingPx, 0);
        LinearLayout firstLineLayout = new LinearLayout(this.mContext);
        firstLineLayout.setPadding(0, 0, 0, 0);
        firstLineLayout.setOrientation(0);
        firstLineLayout.setClickable(false);
        firstLineLayout.setBackground(null);
        firstLineLayout.addView(this.mFirstLine);
        this.mFirstLineCaret = new ImageView(this.mContext);
        this.mFirstLineCaret.setScaleType(ImageView.ScaleType.MATRIX);
        this.mFirstLineCaret.setClickable(false);
        firstLineLayout.addView(this.mFirstLineCaret);
        addView(firstLineLayout, newLinearLayoutParams());
        this.mSecondLine = initTextView();
        this.mSecondLine.setPadding(this.mHorizontalPaddingPx, 0, this.mHorizontalPaddingPx, 0);
        this.mSecondLine.setEllipsize(TextUtils.TruncateAt.END);
        this.mSecondLine.setVisibility(8);
        addView(this.mSecondLine, newLinearLayoutParams());
        addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (oldRight - oldLeft != right - left) {
                    QSDualTileLabel.this.rescheduleUpdateText();
                }
            }
        });
    }

    private static LinearLayout.LayoutParams newLinearLayoutParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.gravity = 1;
        return lp;
    }

    public void setFirstLineCaret(Drawable d) {
        this.mFirstLineCaret.setImageDrawable(d);
        if (d != null) {
            int h = d.getIntrinsicHeight();
            this.mFirstLine.setMinHeight(h);
            this.mFirstLine.setPadding(this.mHorizontalPaddingPx, 0, 0, 0);
        }
    }

    private TextView initTextView() {
        TextView tv = new TextView(this.mContext);
        tv.setPadding(0, 0, 0, 0);
        tv.setGravity(16);
        tv.setSingleLine(true);
        tv.setClickable(false);
        tv.setBackground(null);
        return tv;
    }

    public void setText(CharSequence text) {
        String newText = text == null ? null : text.toString().trim();
        if (!Objects.equals(newText, this.mText)) {
            this.mText = newText;
            rescheduleUpdateText();
        }
    }

    public String getText() {
        return this.mText;
    }

    public void setTextSize(int unit, float size) {
        this.mFirstLine.setTextSize(unit, size);
        this.mSecondLine.setTextSize(unit, size);
        rescheduleUpdateText();
    }

    public void setTextColor(int color) {
        this.mFirstLine.setTextColor(color);
        this.mSecondLine.setTextColor(color);
        rescheduleUpdateText();
    }

    public void setTypeface(Typeface tf) {
        this.mFirstLine.setTypeface(tf);
        this.mSecondLine.setTypeface(tf);
        rescheduleUpdateText();
    }

    public void rescheduleUpdateText() {
        removeCallbacks(this.mUpdateText);
        post(this.mUpdateText);
    }

    public void updateText() {
        if (getWidth() != 0) {
            if (TextUtils.isEmpty(this.mText)) {
                this.mFirstLine.setText((CharSequence) null);
                this.mSecondLine.setText((CharSequence) null);
                this.mSecondLine.setVisibility(8);
                return;
            }
            float maxWidth = (((getWidth() - this.mFirstLineCaret.getWidth()) - this.mHorizontalPaddingPx) - getPaddingLeft()) - getPaddingRight();
            float width = this.mFirstLine.getPaint().measureText(this.mText);
            if (width <= maxWidth) {
                this.mFirstLine.setText(this.mText);
                this.mSecondLine.setText((CharSequence) null);
                this.mSecondLine.setVisibility(8);
                return;
            }
            int n = this.mText.length();
            int lastWordBoundary = -1;
            boolean inWhitespace = false;
            int i = 1;
            while (i < n) {
                float width2 = this.mFirstLine.getPaint().measureText(this.mText.substring(0, i));
                boolean done = width2 > maxWidth;
                if (Character.isWhitespace(this.mText.charAt(i))) {
                    if (!inWhitespace && !done) {
                        lastWordBoundary = i;
                    }
                    inWhitespace = true;
                } else {
                    inWhitespace = false;
                }
                if (done) {
                    break;
                } else {
                    i++;
                }
            }
            if (lastWordBoundary == -1) {
                lastWordBoundary = i - 1;
            }
            this.mFirstLine.setText(this.mText.substring(0, lastWordBoundary));
            this.mSecondLine.setText(this.mText.substring(lastWordBoundary).trim());
            this.mSecondLine.setVisibility(0);
        }
    }
}

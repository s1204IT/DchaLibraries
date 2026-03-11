package android.support.v7.widget;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.appcompat.R$styleable;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

public class DialogTitle extends TextView {
    public DialogTitle(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DialogTitle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DialogTitle(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int lineCount;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Layout layout = getLayout();
        if (layout == null || (lineCount = layout.getLineCount()) <= 0) {
            return;
        }
        int ellipsisCount = layout.getEllipsisCount(lineCount - 1);
        if (ellipsisCount <= 0) {
            return;
        }
        setSingleLine(false);
        setMaxLines(2);
        TypedArray a = getContext().obtainStyledAttributes(null, R$styleable.TextAppearance, R.attr.textAppearanceMedium, R.style.TextAppearance.Medium);
        int textSize = a.getDimensionPixelSize(R$styleable.TextAppearance_android_textSize, 0);
        if (textSize != 0) {
            setTextSize(0, textSize);
        }
        a.recycle();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}

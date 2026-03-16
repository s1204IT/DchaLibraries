package jp.co.omronsoft.iwnnime.ml.candidate;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TabWidget;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.KeyboardManager;

public class SymbolCandidatesCategoryListTabWidget extends TabWidget {
    public SymbolCandidatesCategoryListTabWidget(Context context) {
        super(context);
    }

    public SymbolCandidatesCategoryListTabWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SymbolCandidatesCategoryListTabWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn != null) {
            int totalWidth = 0;
            int height = 0;
            int childCnt = getChildCount();
            if (childCnt >= 1) {
                for (int i = 0; i < childCnt; i++) {
                    View view = getChildAt(i);
                    totalWidth += view.getMeasuredWidth();
                    int tempHeight = view.getMeasuredHeight();
                    if (tempHeight > height) {
                        height = tempHeight;
                    }
                }
                KeyboardManager km = wnn.getCurrentKeyboardManager();
                int parentWidth = km.getKeyboardSize(true).x;
                View parent = (View) getParent();
                int parentWidth2 = (parentWidth - parent.getPaddingLeft()) - parent.getPaddingRight();
                if (totalWidth < parentWidth2) {
                    int diff = parentWidth2 - totalWidth;
                    int addPadding = diff / childCnt;
                    int paddingRest = diff % childCnt;
                    for (int i2 = 0; i2 < childCnt; i2++) {
                        View view2 = getChildAt(i2);
                        int width = view2.getMeasuredWidth() + addPadding;
                        if (i2 < paddingRest) {
                            width++;
                        }
                        view2.setLayoutParams(new LinearLayout.LayoutParams(width, height));
                    }
                    totalWidth = parentWidth2;
                }
                setMeasuredDimension(totalWidth, height);
            }
        }
    }
}

package android.inputmethodservice;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class CompactExtractEditLayout extends LinearLayout {
    private View mInputExtractAccessories;
    private View mInputExtractAction;
    private View mInputExtractEditText;
    private boolean mPerformLayoutChanges;

    public CompactExtractEditLayout(Context context) {
        super(context);
    }

    public CompactExtractEditLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompactExtractEditLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mInputExtractEditText = findViewById(R.id.inputExtractEditText);
        this.mInputExtractAccessories = findViewById(16909186);
        this.mInputExtractAction = findViewById(16909187);
        if (this.mInputExtractEditText == null || this.mInputExtractAccessories == null || this.mInputExtractAction == null) {
            return;
        }
        this.mPerformLayoutChanges = true;
    }

    private int applyFractionInt(int fraction, int whole) {
        return Math.round(getResources().getFraction(fraction, whole, whole));
    }

    private static void setLayoutHeight(View v, int px) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.height = px;
        v.setLayoutParams(lp);
    }

    private static void setLayoutMarginBottom(View v, int px) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        lp.bottomMargin = px;
        v.setLayoutParams(lp);
    }

    private void applyProportionalLayout(int screenWidthPx, int screenHeightPx) {
        if (getResources().getConfiguration().isScreenRound()) {
            setGravity(80);
        }
        setLayoutHeight(this, applyFractionInt(18022406, screenHeightPx));
        setPadding(applyFractionInt(18022407, screenWidthPx), 0, applyFractionInt(18022409, screenWidthPx), 0);
        setLayoutMarginBottom(this.mInputExtractEditText, applyFractionInt(18022410, screenHeightPx));
        setLayoutMarginBottom(this.mInputExtractAccessories, applyFractionInt(18022411, screenHeightPx));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mPerformLayoutChanges) {
            return;
        }
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        int heightPixels = dm.heightPixels;
        int widthPixels = dm.widthPixels;
        applyProportionalLayout(widthPixels, heightPixels);
    }
}

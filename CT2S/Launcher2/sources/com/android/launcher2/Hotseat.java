package com.android.launcher2;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.launcher.R;
import com.android.launcher2.CellLayout;

public class Hotseat extends FrameLayout {
    private int mAllAppsButtonRank;
    private int mCellCountX;
    private int mCellCountY;
    private CellLayout mContent;
    private boolean mIsLandscape;
    private Launcher mLauncher;
    private boolean mTransposeLayoutWithOrientation;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Hotseat, defStyle, 0);
        Resources r = context.getResources();
        this.mCellCountX = a.getInt(0, -1);
        this.mCellCountY = a.getInt(1, -1);
        this.mAllAppsButtonRank = r.getInteger(R.integer.hotseat_all_apps_index);
        this.mTransposeLayoutWithOrientation = r.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);
        this.mIsLandscape = context.getResources().getConfiguration().orientation == 2;
    }

    public void setup(Launcher launcher) {
        this.mLauncher = launcher;
        setOnKeyListener(new HotseatIconKeyEventListener());
    }

    CellLayout getLayout() {
        return this.mContent;
    }

    private boolean hasVerticalHotseat() {
        return this.mIsLandscape && this.mTransposeLayoutWithOrientation;
    }

    int getOrderInHotseat(int x, int y) {
        if (!hasVerticalHotseat()) {
            return x;
        }
        int x2 = (this.mContent.getCountY() - y) - 1;
        return x2;
    }

    int getCellXFromOrder(int rank) {
        if (hasVerticalHotseat()) {
            return 0;
        }
        return rank;
    }

    int getCellYFromOrder(int rank) {
        if (hasVerticalHotseat()) {
            return this.mContent.getCountY() - (rank + 1);
        }
        return 0;
    }

    public boolean isAllAppsButtonRank(int rank) {
        return rank == this.mAllAppsButtonRank;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (this.mCellCountX < 0) {
            this.mCellCountX = LauncherModel.getCellCountX();
        }
        if (this.mCellCountY < 0) {
            this.mCellCountY = LauncherModel.getCellCountY();
        }
        this.mContent = (CellLayout) findViewById(R.id.layout);
        this.mContent.setGridSize(this.mCellCountX, this.mCellCountY);
        this.mContent.setIsHotseat(true);
        resetLayout();
    }

    void resetLayout() {
        this.mContent.removeAllViewsInLayout();
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        BubbleTextView allAppsButton = (BubbleTextView) inflater.inflate(R.layout.application, (ViewGroup) this.mContent, false);
        allAppsButton.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, context.getResources().getDrawable(R.drawable.all_apps_button_icon), (Drawable) null, (Drawable) null);
        allAppsButton.setContentDescription(context.getString(R.string.all_apps_button_label));
        allAppsButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (Hotseat.this.mLauncher != null && (event.getAction() & 255) == 0) {
                    Hotseat.this.mLauncher.onTouchDownAllAppsButton(v);
                    return false;
                }
                return false;
            }
        });
        allAppsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Hotseat.this.mLauncher != null) {
                    Hotseat.this.mLauncher.onClickAllAppsButton(v);
                }
            }
        });
        int x = getCellXFromOrder(this.mAllAppsButtonRank);
        int y = getCellYFromOrder(this.mAllAppsButtonRank);
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(x, y, 1, 1);
        lp.canReorder = false;
        this.mContent.addViewToCellLayout(allAppsButton, -1, 0, lp, true);
    }
}

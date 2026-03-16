package com.android.internal.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.android.internal.R;
import java.lang.ref.WeakReference;

public class AlertController {
    private ListAdapter mAdapter;
    private int mAlertDialogLayout;
    private Button mButtonNegative;
    private Message mButtonNegativeMessage;
    private CharSequence mButtonNegativeText;
    private Button mButtonNeutral;
    private Message mButtonNeutralMessage;
    private CharSequence mButtonNeutralText;
    private int mButtonPanelSideLayout;
    private Button mButtonPositive;
    private Message mButtonPositiveMessage;
    private CharSequence mButtonPositiveText;
    private final Context mContext;
    private View mCustomTitleView;
    private final DialogInterface mDialogInterface;
    private boolean mForceInverseBackground;
    private Handler mHandler;
    private Drawable mIcon;
    private ImageView mIconView;
    private int mListItemLayout;
    private int mListLayout;
    private ListView mListView;
    private CharSequence mMessage;
    private TextView mMessageView;
    private int mMultiChoiceItemLayout;
    private ScrollView mScrollView;
    private int mSingleChoiceItemLayout;
    private CharSequence mTitle;
    private TextView mTitleView;
    private View mView;
    private int mViewLayoutResId;
    private int mViewSpacingBottom;
    private int mViewSpacingLeft;
    private int mViewSpacingRight;
    private int mViewSpacingTop;
    private final Window mWindow;
    private boolean mViewSpacingSpecified = false;
    private int mIconId = 0;
    private int mCheckedItem = -1;
    private int mButtonPanelLayoutHint = 0;
    private final View.OnClickListener mButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Message m;
            if (v == AlertController.this.mButtonPositive && AlertController.this.mButtonPositiveMessage != null) {
                m = Message.obtain(AlertController.this.mButtonPositiveMessage);
            } else {
                m = (v != AlertController.this.mButtonNegative || AlertController.this.mButtonNegativeMessage == null) ? (v != AlertController.this.mButtonNeutral || AlertController.this.mButtonNeutralMessage == null) ? null : Message.obtain(AlertController.this.mButtonNeutralMessage) : Message.obtain(AlertController.this.mButtonNegativeMessage);
            }
            if (m != null) {
                m.sendToTarget();
            }
            AlertController.this.mHandler.obtainMessage(1, AlertController.this.mDialogInterface).sendToTarget();
        }
    };

    private static final class ButtonHandler extends Handler {
        private static final int MSG_DISMISS_DIALOG = 1;
        private WeakReference<DialogInterface> mDialog;

        public ButtonHandler(DialogInterface dialog) {
            this.mDialog = new WeakReference<>(dialog);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case -3:
                case -2:
                case -1:
                    ((DialogInterface.OnClickListener) msg.obj).onClick(this.mDialog.get(), msg.what);
                    break;
                case 1:
                    ((DialogInterface) msg.obj).dismiss();
                    break;
            }
        }
    }

    private static boolean shouldCenterSingleButton(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.alertDialogCenterButtons, outValue, true);
        return outValue.data != 0;
    }

    public AlertController(Context context, DialogInterface di, Window window) {
        this.mContext = context;
        this.mDialogInterface = di;
        this.mWindow = window;
        this.mHandler = new ButtonHandler(di);
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.AlertDialog, 16842845, 0);
        this.mAlertDialogLayout = a.getResourceId(10, R.layout.alert_dialog);
        this.mButtonPanelSideLayout = a.getResourceId(11, 0);
        this.mListLayout = a.getResourceId(12, R.layout.select_dialog);
        this.mMultiChoiceItemLayout = a.getResourceId(13, 17367059);
        this.mSingleChoiceItemLayout = a.getResourceId(14, 17367058);
        this.mListItemLayout = a.getResourceId(15, 17367057);
        a.recycle();
    }

    static boolean canTextInput(View v) {
        if (v.onCheckIsTextEditor()) {
            return true;
        }
        if (!(v instanceof ViewGroup)) {
            return false;
        }
        ViewGroup vg = (ViewGroup) v;
        int i = vg.getChildCount();
        while (i > 0) {
            i--;
            if (canTextInput(vg.getChildAt(i))) {
                return true;
            }
        }
        return false;
    }

    public void installContent() {
        this.mWindow.requestFeature(1);
        int contentView = selectContentView();
        this.mWindow.setContentView(contentView);
        setupView();
        setupDecor();
    }

    private int selectContentView() {
        if (this.mButtonPanelSideLayout == 0) {
            return this.mAlertDialogLayout;
        }
        if (this.mButtonPanelLayoutHint == 1) {
            return this.mButtonPanelSideLayout;
        }
        return this.mAlertDialogLayout;
    }

    public void setTitle(CharSequence title) {
        this.mTitle = title;
        if (this.mTitleView != null) {
            this.mTitleView.setText(title);
        }
    }

    public void setCustomTitle(View customTitleView) {
        this.mCustomTitleView = customTitleView;
    }

    public void setMessage(CharSequence message) {
        this.mMessage = message;
        if (this.mMessageView != null) {
            this.mMessageView.setText(message);
        }
    }

    public void setView(int layoutResId) {
        this.mView = null;
        this.mViewLayoutResId = layoutResId;
        this.mViewSpacingSpecified = false;
    }

    public void setView(View view) {
        this.mView = view;
        this.mViewLayoutResId = 0;
        this.mViewSpacingSpecified = false;
    }

    public void setView(View view, int viewSpacingLeft, int viewSpacingTop, int viewSpacingRight, int viewSpacingBottom) {
        this.mView = view;
        this.mViewLayoutResId = 0;
        this.mViewSpacingSpecified = true;
        this.mViewSpacingLeft = viewSpacingLeft;
        this.mViewSpacingTop = viewSpacingTop;
        this.mViewSpacingRight = viewSpacingRight;
        this.mViewSpacingBottom = viewSpacingBottom;
    }

    public void setButtonPanelLayoutHint(int layoutHint) {
        this.mButtonPanelLayoutHint = layoutHint;
    }

    public void setButton(int whichButton, CharSequence text, DialogInterface.OnClickListener listener, Message msg) {
        if (msg == null && listener != null) {
            msg = this.mHandler.obtainMessage(whichButton, listener);
        }
        switch (whichButton) {
            case -3:
                this.mButtonNeutralText = text;
                this.mButtonNeutralMessage = msg;
                return;
            case -2:
                this.mButtonNegativeText = text;
                this.mButtonNegativeMessage = msg;
                return;
            case -1:
                this.mButtonPositiveText = text;
                this.mButtonPositiveMessage = msg;
                return;
            default:
                throw new IllegalArgumentException("Button does not exist");
        }
    }

    public void setIcon(int resId) {
        this.mIcon = null;
        this.mIconId = resId;
        if (this.mIconView != null) {
            if (resId != 0) {
                this.mIconView.setImageResource(this.mIconId);
            } else {
                this.mIconView.setVisibility(8);
            }
        }
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
        this.mIconId = 0;
        if (this.mIconView != null) {
            if (icon != null) {
                this.mIconView.setImageDrawable(icon);
            } else {
                this.mIconView.setVisibility(8);
            }
        }
    }

    public int getIconAttributeResId(int attrId) {
        TypedValue out = new TypedValue();
        this.mContext.getTheme().resolveAttribute(attrId, out, true);
        return out.resourceId;
    }

    public void setInverseBackgroundForced(boolean forceInverseBackground) {
        this.mForceInverseBackground = forceInverseBackground;
    }

    public ListView getListView() {
        return this.mListView;
    }

    public Button getButton(int whichButton) {
        switch (whichButton) {
            case -3:
                return this.mButtonNeutral;
            case -2:
                return this.mButtonNegative;
            case -1:
                return this.mButtonPositive;
            default:
                return null;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return this.mScrollView != null && this.mScrollView.executeKeyEvent(event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return this.mScrollView != null && this.mScrollView.executeKeyEvent(event);
    }

    private void setupDecor() {
        View decor = this.mWindow.getDecorView();
        final View parent = this.mWindow.findViewById(R.id.parentPanel);
        if (parent != null && decor != null) {
            decor.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    if (insets.isRound()) {
                        int roundOffset = AlertController.this.mContext.getResources().getDimensionPixelOffset(R.dimen.alert_dialog_round_padding);
                        parent.setPadding(roundOffset, roundOffset, roundOffset, roundOffset);
                    }
                    return insets.consumeSystemWindowInsets();
                }
            });
            decor.setFitsSystemWindows(true);
            decor.requestApplyInsets();
        }
    }

    private void setupView() {
        View customView;
        View divider;
        ViewGroup contentPanel = (ViewGroup) this.mWindow.findViewById(R.id.contentPanel);
        setupContent(contentPanel);
        boolean hasButtons = setupButtons();
        ViewGroup topPanel = (ViewGroup) this.mWindow.findViewById(R.id.topPanel);
        TypedArray a = this.mContext.obtainStyledAttributes(null, R.styleable.AlertDialog, 16842845, 0);
        boolean hasTitle = setupTitle(topPanel);
        View buttonPanel = this.mWindow.findViewById(R.id.buttonPanel);
        if (!hasButtons) {
            buttonPanel.setVisibility(8);
            View spacer = this.mWindow.findViewById(R.id.textSpacerNoButtons);
            if (spacer != null) {
                spacer.setVisibility(0);
            }
            this.mWindow.setCloseOnTouchOutsideIfNotSet(true);
        }
        FrameLayout customPanel = (FrameLayout) this.mWindow.findViewById(R.id.customPanel);
        if (this.mView != null) {
            customView = this.mView;
        } else if (this.mViewLayoutResId != 0) {
            LayoutInflater inflater = LayoutInflater.from(this.mContext);
            customView = inflater.inflate(this.mViewLayoutResId, (ViewGroup) customPanel, false);
        } else {
            customView = null;
        }
        boolean hasCustomView = customView != null;
        if (!hasCustomView || !canTextInput(customView)) {
            this.mWindow.setFlags(131072, 131072);
        }
        if (hasCustomView) {
            FrameLayout custom = (FrameLayout) this.mWindow.findViewById(16908331);
            custom.addView(customView, new ViewGroup.LayoutParams(-1, -1));
            if (this.mViewSpacingSpecified) {
                custom.setPadding(this.mViewSpacingLeft, this.mViewSpacingTop, this.mViewSpacingRight, this.mViewSpacingBottom);
            }
            if (this.mListView != null) {
                ((LinearLayout.LayoutParams) customPanel.getLayoutParams()).weight = 0.0f;
            }
        } else {
            customPanel.setVisibility(8);
        }
        if (hasTitle) {
            if (this.mMessage != null || customView != null || this.mListView != null) {
                divider = this.mWindow.findViewById(R.id.titleDivider);
            } else {
                divider = this.mWindow.findViewById(R.id.titleDividerTop);
            }
            if (divider != null) {
                divider.setVisibility(0);
            }
        }
        setBackground(a, topPanel, contentPanel, customPanel, buttonPanel, hasTitle, hasCustomView, hasButtons);
        a.recycle();
    }

    private boolean setupTitle(ViewGroup topPanel) {
        if (this.mCustomTitleView != null) {
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(-1, -2);
            topPanel.addView(this.mCustomTitleView, 0, lp);
            View titleTemplate = this.mWindow.findViewById(R.id.title_template);
            titleTemplate.setVisibility(8);
            return true;
        }
        this.mIconView = (ImageView) this.mWindow.findViewById(16908294);
        boolean hasTextTitle = TextUtils.isEmpty(this.mTitle) ? false : true;
        if (hasTextTitle) {
            this.mTitleView = (TextView) this.mWindow.findViewById(R.id.alertTitle);
            this.mTitleView.setText(this.mTitle);
            if (this.mIconId != 0) {
                this.mIconView.setImageResource(this.mIconId);
                return true;
            }
            if (this.mIcon != null) {
                this.mIconView.setImageDrawable(this.mIcon);
                return true;
            }
            this.mTitleView.setPadding(this.mIconView.getPaddingLeft(), this.mIconView.getPaddingTop(), this.mIconView.getPaddingRight(), this.mIconView.getPaddingBottom());
            this.mIconView.setVisibility(8);
            return true;
        }
        View titleTemplate2 = this.mWindow.findViewById(R.id.title_template);
        titleTemplate2.setVisibility(8);
        this.mIconView.setVisibility(8);
        topPanel.setVisibility(8);
        return false;
    }

    private void setupContent(ViewGroup contentPanel) {
        this.mScrollView = (ScrollView) this.mWindow.findViewById(R.id.scrollView);
        this.mScrollView.setFocusable(false);
        this.mMessageView = (TextView) this.mWindow.findViewById(16908299);
        if (this.mMessageView != null) {
            if (this.mMessage != null) {
                this.mMessageView.setText(this.mMessage);
            } else {
                this.mMessageView.setVisibility(8);
                this.mScrollView.removeView(this.mMessageView);
                if (this.mListView != null) {
                    ViewGroup scrollParent = (ViewGroup) this.mScrollView.getParent();
                    int childIndex = scrollParent.indexOfChild(this.mScrollView);
                    scrollParent.removeViewAt(childIndex);
                    scrollParent.addView(this.mListView, childIndex, new ViewGroup.LayoutParams(-1, -1));
                } else {
                    contentPanel.setVisibility(8);
                }
            }
            final View indicatorUp = this.mWindow.findViewById(R.id.scrollIndicatorUp);
            final View indicatorDown = this.mWindow.findViewById(R.id.scrollIndicatorDown);
            if (indicatorUp != null || indicatorDown != null) {
                if (this.mMessage != null) {
                    this.mScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                        @Override
                        public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                            AlertController.manageScrollIndicators(v, indicatorUp, indicatorDown);
                        }
                    });
                    this.mScrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            AlertController.manageScrollIndicators(AlertController.this.mScrollView, indicatorUp, indicatorDown);
                        }
                    });
                } else {
                    if (this.mListView != null) {
                        this.mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                            @Override
                            public void onScrollStateChanged(AbsListView view, int scrollState) {
                            }

                            @Override
                            public void onScroll(AbsListView v, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                                AlertController.manageScrollIndicators(v, indicatorUp, indicatorDown);
                            }
                        });
                        this.mListView.post(new Runnable() {
                            @Override
                            public void run() {
                                AlertController.manageScrollIndicators(AlertController.this.mListView, indicatorUp, indicatorDown);
                            }
                        });
                        return;
                    }
                    if (indicatorUp != null) {
                        contentPanel.removeView(indicatorUp);
                    }
                    if (indicatorDown != null) {
                        contentPanel.removeView(indicatorDown);
                    }
                }
            }
        }
    }

    private static void manageScrollIndicators(View v, View upIndicator, View downIndicator) {
        if (upIndicator != null) {
            upIndicator.setVisibility(v.canScrollVertically(-1) ? 0 : 4);
        }
        if (downIndicator != null) {
            downIndicator.setVisibility(v.canScrollVertically(1) ? 0 : 4);
        }
    }

    private boolean setupButtons() {
        int whichButtons = 0;
        this.mButtonPositive = (Button) this.mWindow.findViewById(16908313);
        this.mButtonPositive.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonPositiveText)) {
            this.mButtonPositive.setVisibility(8);
        } else {
            this.mButtonPositive.setText(this.mButtonPositiveText);
            this.mButtonPositive.setVisibility(0);
            whichButtons = 0 | 1;
        }
        this.mButtonNegative = (Button) this.mWindow.findViewById(16908314);
        this.mButtonNegative.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonNegativeText)) {
            this.mButtonNegative.setVisibility(8);
        } else {
            this.mButtonNegative.setText(this.mButtonNegativeText);
            this.mButtonNegative.setVisibility(0);
            whichButtons |= 2;
        }
        this.mButtonNeutral = (Button) this.mWindow.findViewById(16908315);
        this.mButtonNeutral.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonNeutralText)) {
            this.mButtonNeutral.setVisibility(8);
        } else {
            this.mButtonNeutral.setText(this.mButtonNeutralText);
            this.mButtonNeutral.setVisibility(0);
            whichButtons |= 4;
        }
        if (shouldCenterSingleButton(this.mContext)) {
            if (whichButtons == 1) {
                centerButton(this.mButtonPositive);
            } else if (whichButtons == 2) {
                centerButton(this.mButtonNegative);
            } else if (whichButtons == 4) {
                centerButton(this.mButtonNeutral);
            }
        }
        return whichButtons != 0;
    }

    private void centerButton(Button button) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.gravity = 1;
        params.weight = 0.5f;
        button.setLayoutParams(params);
        View leftSpacer = this.mWindow.findViewById(R.id.leftSpacer);
        if (leftSpacer != null) {
            leftSpacer.setVisibility(0);
        }
        View rightSpacer = this.mWindow.findViewById(R.id.rightSpacer);
        if (rightSpacer != null) {
            rightSpacer.setVisibility(0);
        }
    }

    private void setBackground(TypedArray a, View topPanel, View contentPanel, View customPanel, View buttonPanel, boolean hasTitle, boolean hasCustomView, boolean hasButtons) {
        int fullDark = 0;
        int topDark = 0;
        int centerDark = 0;
        int bottomDark = 0;
        int fullBright = 0;
        int topBright = 0;
        int centerBright = 0;
        int bottomBright = 0;
        int bottomMedium = 0;
        boolean needsDefaultBackgrounds = a.getBoolean(18, true);
        if (needsDefaultBackgrounds) {
            fullDark = R.drawable.popup_full_dark;
            topDark = R.drawable.popup_top_dark;
            centerDark = R.drawable.popup_center_dark;
            bottomDark = R.drawable.popup_bottom_dark;
            fullBright = R.drawable.popup_full_bright;
            topBright = R.drawable.popup_top_bright;
            centerBright = R.drawable.popup_center_bright;
            bottomBright = R.drawable.popup_bottom_bright;
            bottomMedium = R.drawable.popup_bottom_medium;
        }
        int topBright2 = a.getResourceId(5, topBright);
        int topDark2 = a.getResourceId(1, topDark);
        int centerBright2 = a.getResourceId(6, centerBright);
        int centerDark2 = a.getResourceId(2, centerDark);
        View[] views = new View[4];
        boolean[] light = new boolean[4];
        View lastView = null;
        boolean lastLight = false;
        int pos = 0;
        if (hasTitle) {
            views[0] = topPanel;
            light[0] = false;
            pos = 0 + 1;
        }
        if (contentPanel.getVisibility() == 8) {
            contentPanel = null;
        }
        views[pos] = contentPanel;
        light[pos] = this.mListView != null;
        int pos2 = pos + 1;
        if (hasCustomView) {
            views[pos2] = customPanel;
            light[pos2] = this.mForceInverseBackground;
            pos2++;
        }
        if (hasButtons) {
            views[pos2] = buttonPanel;
            light[pos2] = true;
        }
        boolean setView = false;
        for (int pos3 = 0; pos3 < views.length; pos3++) {
            View v = views[pos3];
            if (v != null) {
                if (lastView != null) {
                    if (!setView) {
                        lastView.setBackgroundResource(lastLight ? topBright2 : topDark2);
                    } else {
                        lastView.setBackgroundResource(lastLight ? centerBright2 : centerDark2);
                    }
                    setView = true;
                }
                lastView = v;
                lastLight = light[pos3];
            }
        }
        if (lastView != null) {
            if (setView) {
                int bottomBright2 = a.getResourceId(7, bottomBright);
                lastView.setBackgroundResource(lastLight ? hasButtons ? a.getResourceId(8, bottomMedium) : bottomBright2 : a.getResourceId(3, bottomDark));
            } else {
                lastView.setBackgroundResource(lastLight ? a.getResourceId(4, fullBright) : a.getResourceId(0, fullDark));
            }
        }
        ListView listView = this.mListView;
        if (listView != null && this.mAdapter != null) {
            listView.setAdapter(this.mAdapter);
            int checkedItem = this.mCheckedItem;
            if (checkedItem > -1) {
                listView.setItemChecked(checkedItem, true);
                listView.setSelection(checkedItem);
            }
        }
    }

    public static class RecycleListView extends ListView {
        boolean mRecycleOnMeasure;

        public RecycleListView(Context context) {
            super(context);
            this.mRecycleOnMeasure = true;
        }

        public RecycleListView(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.mRecycleOnMeasure = true;
        }

        public RecycleListView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            this.mRecycleOnMeasure = true;
        }

        public RecycleListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            this.mRecycleOnMeasure = true;
        }

        @Override
        protected boolean recycleOnMeasure() {
            return this.mRecycleOnMeasure;
        }
    }

    public static class AlertParams {
        public ListAdapter mAdapter;
        public boolean[] mCheckedItems;
        public final Context mContext;
        public Cursor mCursor;
        public View mCustomTitleView;
        public boolean mForceInverseBackground;
        public Drawable mIcon;
        public final LayoutInflater mInflater;
        public String mIsCheckedColumn;
        public boolean mIsMultiChoice;
        public boolean mIsSingleChoice;
        public CharSequence[] mItems;
        public String mLabelColumn;
        public CharSequence mMessage;
        public DialogInterface.OnClickListener mNegativeButtonListener;
        public CharSequence mNegativeButtonText;
        public DialogInterface.OnClickListener mNeutralButtonListener;
        public CharSequence mNeutralButtonText;
        public DialogInterface.OnCancelListener mOnCancelListener;
        public DialogInterface.OnMultiChoiceClickListener mOnCheckboxClickListener;
        public DialogInterface.OnClickListener mOnClickListener;
        public DialogInterface.OnDismissListener mOnDismissListener;
        public AdapterView.OnItemSelectedListener mOnItemSelectedListener;
        public DialogInterface.OnKeyListener mOnKeyListener;
        public OnPrepareListViewListener mOnPrepareListViewListener;
        public DialogInterface.OnClickListener mPositiveButtonListener;
        public CharSequence mPositiveButtonText;
        public CharSequence mTitle;
        public View mView;
        public int mViewLayoutResId;
        public int mViewSpacingBottom;
        public int mViewSpacingLeft;
        public int mViewSpacingRight;
        public int mViewSpacingTop;
        public int mIconId = 0;
        public int mIconAttrId = 0;
        public boolean mViewSpacingSpecified = false;
        public int mCheckedItem = -1;
        public boolean mRecycleOnMeasure = true;
        public boolean mCancelable = true;

        public interface OnPrepareListViewListener {
            void onPrepareListView(ListView listView);
        }

        public AlertParams(Context context) {
            this.mContext = context;
            this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void apply(AlertController dialog) {
            if (this.mCustomTitleView != null) {
                dialog.setCustomTitle(this.mCustomTitleView);
            } else {
                if (this.mTitle != null) {
                    dialog.setTitle(this.mTitle);
                }
                if (this.mIcon != null) {
                    dialog.setIcon(this.mIcon);
                }
                if (this.mIconId != 0) {
                    dialog.setIcon(this.mIconId);
                }
                if (this.mIconAttrId != 0) {
                    dialog.setIcon(dialog.getIconAttributeResId(this.mIconAttrId));
                }
            }
            if (this.mMessage != null) {
                dialog.setMessage(this.mMessage);
            }
            if (this.mPositiveButtonText != null) {
                dialog.setButton(-1, this.mPositiveButtonText, this.mPositiveButtonListener, null);
            }
            if (this.mNegativeButtonText != null) {
                dialog.setButton(-2, this.mNegativeButtonText, this.mNegativeButtonListener, null);
            }
            if (this.mNeutralButtonText != null) {
                dialog.setButton(-3, this.mNeutralButtonText, this.mNeutralButtonListener, null);
            }
            if (this.mForceInverseBackground) {
                dialog.setInverseBackgroundForced(true);
            }
            if (this.mItems != null || this.mCursor != null || this.mAdapter != null) {
                createListView(dialog);
            }
            if (this.mView != null) {
                if (this.mViewSpacingSpecified) {
                    dialog.setView(this.mView, this.mViewSpacingLeft, this.mViewSpacingTop, this.mViewSpacingRight, this.mViewSpacingBottom);
                    return;
                } else {
                    dialog.setView(this.mView);
                    return;
                }
            }
            if (this.mViewLayoutResId != 0) {
                dialog.setView(this.mViewLayoutResId);
            }
        }

        private void createListView(final AlertController dialog) {
            ListAdapter adapter;
            int i = 16908308;
            boolean z = false;
            final RecycleListView listView = (RecycleListView) this.mInflater.inflate(dialog.mListLayout, (ViewGroup) null);
            if (this.mIsMultiChoice) {
                if (this.mCursor == null) {
                    adapter = new ArrayAdapter<CharSequence>(this.mContext, dialog.mMultiChoiceItemLayout, i, this.mItems) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            if (AlertParams.this.mCheckedItems != null) {
                                boolean isItemChecked = AlertParams.this.mCheckedItems[position];
                                if (isItemChecked) {
                                    listView.setItemChecked(position, true);
                                }
                            }
                            return view;
                        }
                    };
                } else {
                    adapter = new CursorAdapter(this.mContext, this.mCursor, z) {
                        private final int mIsCheckedIndex;
                        private final int mLabelIndex;

                        {
                            Cursor cursor = getCursor();
                            this.mLabelIndex = cursor.getColumnIndexOrThrow(AlertParams.this.mLabelColumn);
                            this.mIsCheckedIndex = cursor.getColumnIndexOrThrow(AlertParams.this.mIsCheckedColumn);
                        }

                        @Override
                        public void bindView(View view, Context context, Cursor cursor) {
                            CheckedTextView text = (CheckedTextView) view.findViewById(16908308);
                            text.setText(cursor.getString(this.mLabelIndex));
                            listView.setItemChecked(cursor.getPosition(), cursor.getInt(this.mIsCheckedIndex) == 1);
                        }

                        @Override
                        public View newView(Context context, Cursor cursor, ViewGroup parent) {
                            return AlertParams.this.mInflater.inflate(dialog.mMultiChoiceItemLayout, parent, false);
                        }
                    };
                }
            } else {
                int layout = this.mIsSingleChoice ? dialog.mSingleChoiceItemLayout : dialog.mListItemLayout;
                if (this.mCursor == null) {
                    adapter = this.mAdapter != null ? this.mAdapter : new CheckedItemAdapter(this.mContext, layout, 16908308, this.mItems);
                } else {
                    adapter = new SimpleCursorAdapter(this.mContext, layout, this.mCursor, new String[]{this.mLabelColumn}, new int[]{16908308});
                }
            }
            if (this.mOnPrepareListViewListener != null) {
                this.mOnPrepareListViewListener.onPrepareListView(listView);
            }
            dialog.mAdapter = adapter;
            dialog.mCheckedItem = this.mCheckedItem;
            if (this.mOnClickListener != null) {
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        AlertParams.this.mOnClickListener.onClick(dialog.mDialogInterface, position);
                        if (!AlertParams.this.mIsSingleChoice) {
                            dialog.mDialogInterface.dismiss();
                        }
                    }
                });
            } else if (this.mOnCheckboxClickListener != null) {
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        if (AlertParams.this.mCheckedItems != null) {
                            AlertParams.this.mCheckedItems[position] = listView.isItemChecked(position);
                        }
                        AlertParams.this.mOnCheckboxClickListener.onClick(dialog.mDialogInterface, position, listView.isItemChecked(position));
                    }
                });
            }
            if (this.mOnItemSelectedListener != null) {
                listView.setOnItemSelectedListener(this.mOnItemSelectedListener);
            }
            if (this.mIsSingleChoice) {
                listView.setChoiceMode(1);
            } else if (this.mIsMultiChoice) {
                listView.setChoiceMode(2);
            }
            listView.mRecycleOnMeasure = this.mRecycleOnMeasure;
            dialog.mListView = listView;
        }
    }

    private static class CheckedItemAdapter extends ArrayAdapter<CharSequence> {
        public CheckedItemAdapter(Context context, int resource, int textViewResourceId, CharSequence[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
}

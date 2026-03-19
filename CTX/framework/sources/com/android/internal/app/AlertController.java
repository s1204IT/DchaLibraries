package com.android.internal.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.Window;
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
    public static final int MICRO = 1;
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
    protected ListView mListView;
    protected CharSequence mMessage;
    private Integer mMessageHyphenationFrequency;
    private MovementMethod mMessageMovementMethod;
    protected TextView mMessageView;
    private int mMultiChoiceItemLayout;
    protected ScrollView mScrollView;
    private boolean mShowTitle;
    private int mSingleChoiceItemLayout;
    private CharSequence mTitle;
    private TextView mTitleView;
    private View mView;
    private int mViewLayoutResId;
    private int mViewSpacingBottom;
    private int mViewSpacingLeft;
    private int mViewSpacingRight;
    private int mViewSpacingTop;
    protected final Window mWindow;
    private boolean mViewSpacingSpecified = false;
    private int mIconId = 0;
    private int mCheckedItem = -1;
    private int mButtonPanelLayoutHint = 0;
    private final View.OnClickListener mButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Message messageObtain;
            if (view == AlertController.this.mButtonPositive && AlertController.this.mButtonPositiveMessage != null) {
                messageObtain = Message.obtain(AlertController.this.mButtonPositiveMessage);
            } else {
                messageObtain = (view != AlertController.this.mButtonNegative || AlertController.this.mButtonNegativeMessage == null) ? (view != AlertController.this.mButtonNeutral || AlertController.this.mButtonNeutralMessage == null) ? null : Message.obtain(AlertController.this.mButtonNeutralMessage) : Message.obtain(AlertController.this.mButtonNegativeMessage);
            }
            if (messageObtain != null) {
                messageObtain.sendToTarget();
            }
            AlertController.this.mHandler.obtainMessage(1, AlertController.this.mDialogInterface).sendToTarget();
        }
    };

    private static final class ButtonHandler extends Handler {
        private static final int MSG_DISMISS_DIALOG = 1;
        private WeakReference<DialogInterface> mDialog;

        public ButtonHandler(DialogInterface dialogInterface) {
            this.mDialog = new WeakReference<>(dialogInterface);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 1) {
                switch (i) {
                    case -3:
                    case -2:
                    case -1:
                        ((DialogInterface.OnClickListener) message.obj).onClick(this.mDialog.get(), message.what);
                        break;
                }
            }
            ((DialogInterface) message.obj).dismiss();
        }
    }

    private static boolean shouldCenterSingleButton(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.alertDialogCenterButtons, typedValue, true);
        return typedValue.data != 0;
    }

    public static final AlertController create(Context context, DialogInterface dialogInterface, Window window) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(null, R.styleable.AlertDialog, 16842845, 0);
        int i = typedArrayObtainStyledAttributes.getInt(12, 0);
        typedArrayObtainStyledAttributes.recycle();
        if (i == 1) {
            return new MicroAlertController(context, dialogInterface, window);
        }
        return new AlertController(context, dialogInterface, window);
    }

    protected AlertController(Context context, DialogInterface dialogInterface, Window window) {
        this.mContext = context;
        this.mDialogInterface = dialogInterface;
        this.mWindow = window;
        this.mHandler = new ButtonHandler(dialogInterface);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(null, R.styleable.AlertDialog, 16842845, 0);
        this.mAlertDialogLayout = typedArrayObtainStyledAttributes.getResourceId(10, R.layout.alert_dialog);
        this.mButtonPanelSideLayout = typedArrayObtainStyledAttributes.getResourceId(11, 0);
        this.mListLayout = typedArrayObtainStyledAttributes.getResourceId(15, R.layout.select_dialog);
        this.mMultiChoiceItemLayout = typedArrayObtainStyledAttributes.getResourceId(16, 17367059);
        this.mSingleChoiceItemLayout = typedArrayObtainStyledAttributes.getResourceId(21, 17367058);
        this.mListItemLayout = typedArrayObtainStyledAttributes.getResourceId(14, 17367057);
        this.mShowTitle = typedArrayObtainStyledAttributes.getBoolean(20, true);
        typedArrayObtainStyledAttributes.recycle();
        window.requestFeature(1);
    }

    static boolean canTextInput(View view) {
        if (view.onCheckIsTextEditor()) {
            return true;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        int childCount = viewGroup.getChildCount();
        while (childCount > 0) {
            childCount--;
            if (canTextInput(viewGroup.getChildAt(childCount))) {
                return true;
            }
        }
        return false;
    }

    public void installContent(AlertParams alertParams) {
        alertParams.apply(this);
        installContent();
    }

    public void installContent() {
        this.mWindow.setContentView(selectContentView());
        setupView();
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

    public void setTitle(CharSequence charSequence) {
        this.mTitle = charSequence;
        if (this.mTitleView != null) {
            this.mTitleView.setText(charSequence);
        }
    }

    public void setCustomTitle(View view) {
        this.mCustomTitleView = view;
    }

    public void setMessage(CharSequence charSequence) {
        this.mMessage = charSequence;
        if (this.mMessageView != null) {
            this.mMessageView.setText(charSequence);
        }
    }

    public void setMessageMovementMethod(MovementMethod movementMethod) {
        this.mMessageMovementMethod = movementMethod;
        if (this.mMessageView != null) {
            this.mMessageView.setMovementMethod(movementMethod);
        }
    }

    public void setMessageHyphenationFrequency(int i) {
        this.mMessageHyphenationFrequency = Integer.valueOf(i);
        if (this.mMessageView != null) {
            this.mMessageView.setHyphenationFrequency(i);
        }
    }

    public void setView(int i) {
        this.mView = null;
        this.mViewLayoutResId = i;
        this.mViewSpacingSpecified = false;
    }

    public void setView(View view) {
        this.mView = view;
        this.mViewLayoutResId = 0;
        this.mViewSpacingSpecified = false;
    }

    public void setView(View view, int i, int i2, int i3, int i4) {
        this.mView = view;
        this.mViewLayoutResId = 0;
        this.mViewSpacingSpecified = true;
        this.mViewSpacingLeft = i;
        this.mViewSpacingTop = i2;
        this.mViewSpacingRight = i3;
        this.mViewSpacingBottom = i4;
    }

    public void setButtonPanelLayoutHint(int i) {
        this.mButtonPanelLayoutHint = i;
    }

    public void setButton(int i, CharSequence charSequence, DialogInterface.OnClickListener onClickListener, Message message) {
        if (message == null && onClickListener != null) {
            message = this.mHandler.obtainMessage(i, onClickListener);
        }
        switch (i) {
            case -3:
                this.mButtonNeutralText = charSequence;
                this.mButtonNeutralMessage = message;
                return;
            case -2:
                this.mButtonNegativeText = charSequence;
                this.mButtonNegativeMessage = message;
                return;
            case -1:
                this.mButtonPositiveText = charSequence;
                this.mButtonPositiveMessage = message;
                return;
            default:
                throw new IllegalArgumentException("Button does not exist");
        }
    }

    public void setIcon(int i) {
        this.mIcon = null;
        this.mIconId = i;
        if (this.mIconView != null) {
            if (i != 0) {
                this.mIconView.setVisibility(0);
                this.mIconView.setImageResource(this.mIconId);
            } else {
                this.mIconView.setVisibility(8);
            }
        }
    }

    public void setIcon(Drawable drawable) {
        this.mIcon = drawable;
        this.mIconId = 0;
        if (this.mIconView != null) {
            if (drawable != null) {
                this.mIconView.setVisibility(0);
                this.mIconView.setImageDrawable(drawable);
            } else {
                this.mIconView.setVisibility(8);
            }
        }
    }

    public int getIconAttributeResId(int i) {
        TypedValue typedValue = new TypedValue();
        this.mContext.getTheme().resolveAttribute(i, typedValue, true);
        return typedValue.resourceId;
    }

    public void setInverseBackgroundForced(boolean z) {
        this.mForceInverseBackground = z;
    }

    public ListView getListView() {
        return this.mListView;
    }

    public Button getButton(int i) {
        switch (i) {
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

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return this.mScrollView != null && this.mScrollView.executeKeyEvent(keyEvent);
    }

    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return this.mScrollView != null && this.mScrollView.executeKeyEvent(keyEvent);
    }

    private ViewGroup resolvePanel(View view, View view2) {
        if (view == null) {
            if (view2 instanceof ViewStub) {
                view2 = ((ViewStub) view2).inflate();
            }
            return (ViewGroup) view2;
        }
        if (view2 != null) {
            ViewParent parent = view2.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(view2);
            }
        }
        if (view instanceof ViewStub) {
            view = ((ViewStub) view).inflate();
        }
        return (ViewGroup) view;
    }

    private void setupView() {
        View viewFindViewById;
        View viewFindViewById2;
        View viewFindViewById3;
        View viewFindViewById4 = this.mWindow.findViewById(R.id.parentPanel);
        View viewFindViewById5 = viewFindViewById4.findViewById(R.id.topPanel);
        View viewFindViewById6 = viewFindViewById4.findViewById(R.id.contentPanel);
        View viewFindViewById7 = viewFindViewById4.findViewById(R.id.buttonPanel);
        ViewGroup viewGroup = (ViewGroup) viewFindViewById4.findViewById(R.id.customPanel);
        setupCustomContent(viewGroup);
        View viewFindViewById8 = viewGroup.findViewById(R.id.topPanel);
        View viewFindViewById9 = viewGroup.findViewById(R.id.contentPanel);
        View viewFindViewById10 = viewGroup.findViewById(R.id.buttonPanel);
        ViewGroup viewGroupResolvePanel = resolvePanel(viewFindViewById8, viewFindViewById5);
        ViewGroup viewGroupResolvePanel2 = resolvePanel(viewFindViewById9, viewFindViewById6);
        ViewGroup viewGroupResolvePanel3 = resolvePanel(viewFindViewById10, viewFindViewById7);
        setupContent(viewGroupResolvePanel2);
        setupButtons(viewGroupResolvePanel3);
        setupTitle(viewGroupResolvePanel);
        boolean z = (viewGroup == null || viewGroup.getVisibility() == 8) ? false : true;
        boolean z2 = (viewGroupResolvePanel == null || viewGroupResolvePanel.getVisibility() == 8) ? 0 : 1;
        boolean z3 = (viewGroupResolvePanel3 == null || viewGroupResolvePanel3.getVisibility() == 8) ? false : true;
        if (!z3) {
            if (viewGroupResolvePanel2 != null && (viewFindViewById3 = viewGroupResolvePanel2.findViewById(R.id.textSpacerNoButtons)) != null) {
                viewFindViewById3.setVisibility(0);
            }
            this.mWindow.setCloseOnTouchOutsideIfNotSet(true);
        }
        if (z2 != 0) {
            if (this.mScrollView != null) {
                this.mScrollView.setClipToPadding(true);
            }
            if (this.mMessage != null || this.mListView != null || z) {
                if (!z) {
                    viewFindViewById2 = viewGroupResolvePanel.findViewById(R.id.titleDividerNoCustom);
                } else {
                    viewFindViewById2 = null;
                }
                if (viewFindViewById2 == null) {
                    viewFindViewById2 = viewGroupResolvePanel.findViewById(R.id.titleDivider);
                }
            } else {
                viewFindViewById2 = viewGroupResolvePanel.findViewById(R.id.titleDividerTop);
            }
            if (viewFindViewById2 != null) {
                viewFindViewById2.setVisibility(0);
            }
        } else if (viewGroupResolvePanel2 != null && (viewFindViewById = viewGroupResolvePanel2.findViewById(R.id.textSpacerNoTitle)) != null) {
            viewFindViewById.setVisibility(0);
        }
        if (this.mListView instanceof RecycleListView) {
            ((RecycleListView) this.mListView).setHasDecor(z2, z3);
        }
        if (!z) {
            View view = this.mListView != null ? this.mListView : this.mScrollView;
            if (view != null) {
                view.setScrollIndicators((z3 ? 2 : 0) | z2, 3);
            }
        }
        TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.AlertDialog, 16842845, 0);
        setBackground(typedArrayObtainStyledAttributes, viewGroupResolvePanel, viewGroupResolvePanel2, viewGroup, viewGroupResolvePanel3, z2, z, z3);
        typedArrayObtainStyledAttributes.recycle();
    }

    private void setupCustomContent(ViewGroup viewGroup) {
        View viewInflate;
        if (this.mView != null) {
            viewInflate = this.mView;
        } else if (this.mViewLayoutResId != 0) {
            viewInflate = LayoutInflater.from(this.mContext).inflate(this.mViewLayoutResId, viewGroup, false);
        } else {
            viewInflate = null;
        }
        boolean z = viewInflate != null;
        if (!z || !canTextInput(viewInflate)) {
            this.mWindow.setFlags(131072, 131072);
        }
        if (z) {
            FrameLayout frameLayout = (FrameLayout) this.mWindow.findViewById(16908331);
            frameLayout.addView(viewInflate, new ViewGroup.LayoutParams(-1, -1));
            if (this.mViewSpacingSpecified) {
                frameLayout.setPadding(this.mViewSpacingLeft, this.mViewSpacingTop, this.mViewSpacingRight, this.mViewSpacingBottom);
            }
            if (this.mListView != null) {
                ((LinearLayout.LayoutParams) viewGroup.getLayoutParams()).weight = 0.0f;
                return;
            }
            return;
        }
        viewGroup.setVisibility(8);
    }

    protected void setupTitle(ViewGroup viewGroup) {
        if (this.mCustomTitleView != null && this.mShowTitle) {
            viewGroup.addView(this.mCustomTitleView, 0, new ViewGroup.LayoutParams(-1, -2));
            this.mWindow.findViewById(R.id.title_template).setVisibility(8);
            return;
        }
        this.mIconView = (ImageView) this.mWindow.findViewById(16908294);
        if ((!TextUtils.isEmpty(this.mTitle)) && this.mShowTitle) {
            this.mTitleView = (TextView) this.mWindow.findViewById(R.id.alertTitle);
            this.mTitleView.setText(this.mTitle);
            if (this.mIconId != 0) {
                this.mIconView.setImageResource(this.mIconId);
                return;
            } else if (this.mIcon != null) {
                this.mIconView.setImageDrawable(this.mIcon);
                return;
            } else {
                this.mTitleView.setPadding(this.mIconView.getPaddingLeft(), this.mIconView.getPaddingTop(), this.mIconView.getPaddingRight(), this.mIconView.getPaddingBottom());
                this.mIconView.setVisibility(8);
                return;
            }
        }
        this.mWindow.findViewById(R.id.title_template).setVisibility(8);
        this.mIconView.setVisibility(8);
        viewGroup.setVisibility(8);
    }

    protected void setupContent(ViewGroup viewGroup) {
        this.mScrollView = (ScrollView) viewGroup.findViewById(R.id.scrollView);
        this.mScrollView.setFocusable(false);
        this.mMessageView = (TextView) viewGroup.findViewById(16908299);
        if (this.mMessageView == null) {
            return;
        }
        if (this.mMessage != null) {
            this.mMessageView.setText(this.mMessage);
            if (this.mMessageMovementMethod != null) {
                this.mMessageView.setMovementMethod(this.mMessageMovementMethod);
            }
            if (this.mMessageHyphenationFrequency != null) {
                this.mMessageView.setHyphenationFrequency(this.mMessageHyphenationFrequency.intValue());
                return;
            }
            return;
        }
        this.mMessageView.setVisibility(8);
        this.mScrollView.removeView(this.mMessageView);
        if (this.mListView != null) {
            ViewGroup viewGroup2 = (ViewGroup) this.mScrollView.getParent();
            int iIndexOfChild = viewGroup2.indexOfChild(this.mScrollView);
            viewGroup2.removeViewAt(iIndexOfChild);
            viewGroup2.addView(this.mListView, iIndexOfChild, new ViewGroup.LayoutParams(-1, -1));
            return;
        }
        viewGroup.setVisibility(8);
    }

    private static void manageScrollIndicators(View view, View view2, View view3) {
        if (view2 != null) {
            view2.setVisibility(view.canScrollVertically(-1) ? 0 : 4);
        }
        if (view3 != null) {
            view3.setVisibility(view.canScrollVertically(1) ? 0 : 4);
        }
    }

    protected void setupButtons(ViewGroup viewGroup) {
        int i;
        this.mButtonPositive = (Button) viewGroup.findViewById(16908313);
        this.mButtonPositive.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonPositiveText)) {
            this.mButtonPositive.setVisibility(8);
            i = 0;
        } else {
            this.mButtonPositive.setText(this.mButtonPositiveText);
            this.mButtonPositive.setVisibility(0);
            i = 1;
        }
        this.mButtonNegative = (Button) viewGroup.findViewById(16908314);
        this.mButtonNegative.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonNegativeText)) {
            this.mButtonNegative.setVisibility(8);
        } else {
            this.mButtonNegative.setText(this.mButtonNegativeText);
            this.mButtonNegative.setVisibility(0);
            i |= 2;
        }
        this.mButtonNeutral = (Button) viewGroup.findViewById(16908315);
        this.mButtonNeutral.setOnClickListener(this.mButtonHandler);
        if (TextUtils.isEmpty(this.mButtonNeutralText)) {
            this.mButtonNeutral.setVisibility(8);
        } else {
            this.mButtonNeutral.setText(this.mButtonNeutralText);
            this.mButtonNeutral.setVisibility(0);
            i |= 4;
        }
        if (shouldCenterSingleButton(this.mContext)) {
            if (i == 1) {
                centerButton(this.mButtonPositive);
            } else if (i == 2) {
                centerButton(this.mButtonNegative);
            } else if (i == 4) {
                centerButton(this.mButtonNeutral);
            }
        }
        if (!(i != 0)) {
            viewGroup.setVisibility(8);
        }
    }

    private void centerButton(Button button) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) button.getLayoutParams();
        layoutParams.gravity = 1;
        layoutParams.weight = 0.5f;
        button.setLayoutParams(layoutParams);
        View viewFindViewById = this.mWindow.findViewById(R.id.leftSpacer);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(0);
        }
        View viewFindViewById2 = this.mWindow.findViewById(R.id.rightSpacer);
        if (viewFindViewById2 != null) {
            viewFindViewById2.setVisibility(0);
        }
    }

    private void setBackground(TypedArray typedArray, View view, View view2, View view3, View view4, boolean z, boolean z2, boolean z3) {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        View view5;
        int i11;
        if (typedArray.getBoolean(17, true)) {
            i = R.drawable.popup_full_dark;
            i2 = R.drawable.popup_top_dark;
            i3 = R.drawable.popup_center_dark;
            i4 = R.drawable.popup_bottom_dark;
            i5 = R.drawable.popup_full_bright;
            i6 = R.drawable.popup_top_bright;
            i7 = R.drawable.popup_center_bright;
            i8 = R.drawable.popup_bottom_bright;
            i9 = R.drawable.popup_bottom_medium;
        } else {
            i = 0;
            i2 = 0;
            i3 = 0;
            i4 = 0;
            i5 = 0;
            i6 = 0;
            i7 = 0;
            i8 = 0;
            i9 = 0;
        }
        int resourceId = typedArray.getResourceId(5, i6);
        int resourceId2 = typedArray.getResourceId(1, i2);
        int resourceId3 = typedArray.getResourceId(6, i7);
        int resourceId4 = typedArray.getResourceId(2, i3);
        View[] viewArr = new View[4];
        boolean[] zArr = new boolean[4];
        if (!z) {
            i10 = 0;
        } else {
            viewArr[0] = view;
            zArr[0] = false;
            i10 = 1;
        }
        viewArr[i10] = view2.getVisibility() == 8 ? null : view2;
        zArr[i10] = this.mListView != null;
        int i12 = i10 + 1;
        if (z2) {
            viewArr[i12] = view3;
            zArr[i12] = this.mForceInverseBackground;
            i12++;
        }
        if (z3) {
            viewArr[i12] = view4;
            zArr[i12] = true;
        }
        View view6 = null;
        int i13 = 0;
        boolean z4 = false;
        boolean z5 = false;
        while (true) {
            int i14 = resourceId2;
            if (i13 >= viewArr.length) {
                break;
            }
            View view7 = viewArr[i13];
            if (view7 != null) {
                if (view6 != null) {
                    if (!z4) {
                        if (z5) {
                            view5 = view7;
                            i11 = resourceId;
                        } else {
                            view5 = view7;
                            i11 = i14;
                        }
                        view6.setBackgroundResource(i11);
                    } else {
                        view5 = view7;
                        view6.setBackgroundResource(z5 ? resourceId3 : resourceId4);
                    }
                    z4 = true;
                } else {
                    view5 = view7;
                }
                z5 = zArr[i13];
                view6 = view5;
            }
            i13++;
            resourceId2 = i14;
        }
        if (view6 != null) {
            if (z4) {
                int resourceId5 = typedArray.getResourceId(7, i8);
                int resourceId6 = typedArray.getResourceId(8, i9);
                int resourceId7 = typedArray.getResourceId(3, i4);
                if (z5) {
                    resourceId7 = z3 ? resourceId6 : resourceId5;
                }
                view6.setBackgroundResource(resourceId7);
            } else {
                int resourceId8 = typedArray.getResourceId(4, i5);
                int resourceId9 = typedArray.getResourceId(0, i);
                if (!z5) {
                    resourceId8 = resourceId9;
                }
                view6.setBackgroundResource(resourceId8);
            }
        }
        ListView listView = this.mListView;
        if (listView != null && this.mAdapter != null) {
            listView.setAdapter(this.mAdapter);
            int i15 = this.mCheckedItem;
            if (i15 > -1) {
                listView.setItemChecked(i15, true);
                listView.setSelectionFromTop(i15, typedArray.getDimensionPixelSize(19, 0));
            }
        }
    }

    public static class RecycleListView extends ListView {
        private final int mPaddingBottomNoButtons;
        private final int mPaddingTopNoTitle;
        boolean mRecycleOnMeasure;

        public RecycleListView(Context context) {
            this(context, null);
        }

        public RecycleListView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.mRecycleOnMeasure = true;
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RecycleListView);
            this.mPaddingBottomNoButtons = typedArrayObtainStyledAttributes.getDimensionPixelOffset(0, -1);
            this.mPaddingTopNoTitle = typedArrayObtainStyledAttributes.getDimensionPixelOffset(1, -1);
        }

        public void setHasDecor(boolean z, boolean z2) {
            if (!z2 || !z) {
                setPadding(getPaddingLeft(), z ? getPaddingTop() : this.mPaddingTopNoTitle, getPaddingRight(), z2 ? getPaddingBottom() : this.mPaddingBottomNoButtons);
            }
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

        public void apply(AlertController alertController) {
            if (this.mCustomTitleView != null) {
                alertController.setCustomTitle(this.mCustomTitleView);
            } else {
                if (this.mTitle != null) {
                    alertController.setTitle(this.mTitle);
                }
                if (this.mIcon != null) {
                    alertController.setIcon(this.mIcon);
                }
                if (this.mIconId != 0) {
                    alertController.setIcon(this.mIconId);
                }
                if (this.mIconAttrId != 0) {
                    alertController.setIcon(alertController.getIconAttributeResId(this.mIconAttrId));
                }
            }
            if (this.mMessage != null) {
                alertController.setMessage(this.mMessage);
            }
            if (this.mPositiveButtonText != null) {
                alertController.setButton(-1, this.mPositiveButtonText, this.mPositiveButtonListener, null);
            }
            if (this.mNegativeButtonText != null) {
                alertController.setButton(-2, this.mNegativeButtonText, this.mNegativeButtonListener, null);
            }
            if (this.mNeutralButtonText != null) {
                alertController.setButton(-3, this.mNeutralButtonText, this.mNeutralButtonListener, null);
            }
            if (this.mForceInverseBackground) {
                alertController.setInverseBackgroundForced(true);
            }
            if (this.mItems != null || this.mCursor != null || this.mAdapter != null) {
                createListView(alertController);
            }
            if (this.mView != null) {
                if (this.mViewSpacingSpecified) {
                    alertController.setView(this.mView, this.mViewSpacingLeft, this.mViewSpacingTop, this.mViewSpacingRight, this.mViewSpacingBottom);
                    return;
                } else {
                    alertController.setView(this.mView);
                    return;
                }
            }
            if (this.mViewLayoutResId != 0) {
                alertController.setView(this.mViewLayoutResId);
            }
        }

        private void createListView(final AlertController alertController) {
            ListAdapter checkedItemAdapter;
            final RecycleListView recycleListView = (RecycleListView) this.mInflater.inflate(alertController.mListLayout, (ViewGroup) null);
            if (this.mIsMultiChoice) {
                if (this.mCursor == null) {
                    checkedItemAdapter = new ArrayAdapter<CharSequence>(this.mContext, alertController.mMultiChoiceItemLayout, 16908308, this.mItems) {
                        @Override
                        public View getView(int i, View view, ViewGroup viewGroup) {
                            View view2 = super.getView(i, view, viewGroup);
                            if (AlertParams.this.mCheckedItems != null && AlertParams.this.mCheckedItems[i]) {
                                recycleListView.setItemChecked(i, true);
                            }
                            return view2;
                        }
                    };
                } else {
                    checkedItemAdapter = new CursorAdapter(this.mContext, this.mCursor, false) {
                        private final int mIsCheckedIndex;
                        private final int mLabelIndex;

                        {
                            Cursor cursor = getCursor();
                            this.mLabelIndex = cursor.getColumnIndexOrThrow(AlertParams.this.mLabelColumn);
                            this.mIsCheckedIndex = cursor.getColumnIndexOrThrow(AlertParams.this.mIsCheckedColumn);
                        }

                        @Override
                        public void bindView(View view, Context context, Cursor cursor) {
                            ((CheckedTextView) view.findViewById(16908308)).setText(cursor.getString(this.mLabelIndex));
                            recycleListView.setItemChecked(cursor.getPosition(), cursor.getInt(this.mIsCheckedIndex) == 1);
                        }

                        @Override
                        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
                            return AlertParams.this.mInflater.inflate(alertController.mMultiChoiceItemLayout, viewGroup, false);
                        }
                    };
                }
            } else {
                int i = this.mIsSingleChoice ? alertController.mSingleChoiceItemLayout : alertController.mListItemLayout;
                if (this.mCursor != null) {
                    checkedItemAdapter = new SimpleCursorAdapter(this.mContext, i, this.mCursor, new String[]{this.mLabelColumn}, new int[]{16908308});
                } else if (this.mAdapter != null) {
                    checkedItemAdapter = this.mAdapter;
                } else {
                    checkedItemAdapter = new CheckedItemAdapter(this.mContext, i, 16908308, this.mItems);
                }
            }
            if (this.mOnPrepareListViewListener != null) {
                this.mOnPrepareListViewListener.onPrepareListView(recycleListView);
            }
            alertController.mAdapter = checkedItemAdapter;
            alertController.mCheckedItem = this.mCheckedItem;
            if (this.mOnClickListener != null) {
                recycleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i2, long j) {
                        AlertParams.this.mOnClickListener.onClick(alertController.mDialogInterface, i2);
                        if (!AlertParams.this.mIsSingleChoice) {
                            alertController.mDialogInterface.dismiss();
                        }
                    }
                });
            } else if (this.mOnCheckboxClickListener != null) {
                recycleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i2, long j) {
                        if (AlertParams.this.mCheckedItems != null) {
                            AlertParams.this.mCheckedItems[i2] = recycleListView.isItemChecked(i2);
                        }
                        AlertParams.this.mOnCheckboxClickListener.onClick(alertController.mDialogInterface, i2, recycleListView.isItemChecked(i2));
                    }
                });
            }
            if (this.mOnItemSelectedListener != null) {
                recycleListView.setOnItemSelectedListener(this.mOnItemSelectedListener);
            }
            if (this.mIsSingleChoice) {
                recycleListView.setChoiceMode(1);
            } else if (this.mIsMultiChoice) {
                recycleListView.setChoiceMode(2);
            }
            recycleListView.mRecycleOnMeasure = this.mRecycleOnMeasure;
            alertController.mListView = recycleListView;
        }
    }

    private static class CheckedItemAdapter extends ArrayAdapter<CharSequence> {
        public CheckedItemAdapter(Context context, int i, int i2, CharSequence[] charSequenceArr) {
            super(context, i, i2, charSequenceArr);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }
    }
}

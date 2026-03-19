package android.widget;

import android.animation.ValueAnimator;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.UndoManager;
import android.content.UndoOperation;
import android.content.UndoOwner;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ParcelableParcel;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.ParcelableSpan;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.KeyListener;
import android.text.method.MetaKeyKeyListener;
import android.text.method.MovementMethod;
import android.text.method.WordIterator;
import android.text.style.EasyEditSpan;
import android.text.style.SuggestionRangeSpan;
import android.text.style.SuggestionSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.DisplayListCanvas;
import android.view.DragAndDropPermissions;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.RenderNode;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationManager;
import android.widget.AdapterView;
import android.widget.Editor;
import android.widget.Magnifier;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.transition.EpicenterTranslateClipReveal;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.view.FloatingActionMode;
import com.android.internal.widget.EditableInputConnection;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Editor {
    static final int BLINK = 500;
    private static final boolean DEBUG_UNDO = false;
    private static final int DRAG_SHADOW_MAX_TEXT_LENGTH = 20;
    static final int EXTRACT_NOTHING = -2;
    static final int EXTRACT_UNKNOWN = -1;
    private static final boolean FLAG_USE_MAGNIFIER = true;
    public static final int HANDLE_TYPE_SELECTION_END = 1;
    public static final int HANDLE_TYPE_SELECTION_START = 0;
    private static final float LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS = 0.5f;
    private static final int MENU_ITEM_ORDER_ASSIST = 0;
    private static final int MENU_ITEM_ORDER_AUTOFILL = 10;
    private static final int MENU_ITEM_ORDER_COPY = 5;
    private static final int MENU_ITEM_ORDER_CUT = 4;
    private static final int MENU_ITEM_ORDER_PASTE = 6;
    private static final int MENU_ITEM_ORDER_PASTE_AS_PLAIN_TEXT = 11;
    private static final int MENU_ITEM_ORDER_PROCESS_TEXT_INTENT_ACTIONS_START = 100;
    private static final int MENU_ITEM_ORDER_REDO = 3;
    private static final int MENU_ITEM_ORDER_REPLACE = 9;
    private static final int MENU_ITEM_ORDER_SECONDARY_ASSIST_ACTIONS_START = 50;
    private static final int MENU_ITEM_ORDER_SELECT_ALL = 8;
    private static final int MENU_ITEM_ORDER_SHARE = 7;
    private static final int MENU_ITEM_ORDER_UNDO = 2;
    private static final String TAG = "Editor";
    private static final int TAP_STATE_DOUBLE_TAP = 2;
    private static final int TAP_STATE_FIRST_TAP = 1;
    private static final int TAP_STATE_INITIAL = 0;
    private static final int TAP_STATE_TRIPLE_CLICK = 3;
    private static final String UNDO_OWNER_TAG = "Editor";
    private static final int UNSET_LINE = -1;
    private static final int UNSET_X_VALUE = -1;
    private Blink mBlink;
    private float mContextMenuAnchorX;
    private float mContextMenuAnchorY;
    private CorrectionHighlighter mCorrectionHighlighter;
    boolean mCreatedWithASelection;
    private final CursorAnchorInfoNotifier mCursorAnchorInfoNotifier;
    ActionMode.Callback mCustomInsertionActionModeCallback;
    ActionMode.Callback mCustomSelectionActionModeCallback;
    boolean mDiscardNextActionUp;
    CharSequence mError;
    private ErrorPopup mErrorPopup;
    boolean mErrorWasChanged;
    boolean mFrozenWithFocus;
    private final boolean mHapticTextHandleEnabled;
    boolean mIgnoreActionUpEvent;
    boolean mInBatchEditControllers;
    InputContentType mInputContentType;
    InputMethodState mInputMethodState;
    private Runnable mInsertionActionModeRunnable;
    private boolean mInsertionControllerEnabled;
    private InsertionPointCursorController mInsertionPointCursorController;
    boolean mIsBeingLongClicked;
    KeyListener mKeyListener;
    private int mLastButtonState;
    private float mLastDownPositionX;
    private float mLastDownPositionY;
    private float mLastUpPositionX;
    private float mLastUpPositionY;
    private final MagnifierMotionAnimator mMagnifierAnimator;
    private PositionListener mPositionListener;
    private boolean mPreserveSelection;
    final ProcessTextIntentActionsHandler mProcessTextIntentActionsHandler;
    private boolean mRenderCursorRegardlessTiming;
    private boolean mRequestingLinkActionMode;
    private boolean mRestartActionModeOnNextRefresh;
    boolean mSelectAllOnFocus;
    private Drawable mSelectHandleCenter;
    private Drawable mSelectHandleLeft;
    private Drawable mSelectHandleRight;
    private SelectionActionModeHelper mSelectionActionModeHelper;
    private boolean mSelectionControllerEnabled;
    SelectionModifierCursorController mSelectionModifierCursorController;
    boolean mSelectionMoved;
    private long mShowCursor;
    private boolean mShowErrorAfterAttach;
    private Runnable mShowSuggestionRunnable;
    private SpanController mSpanController;
    SpellChecker mSpellChecker;
    private final SuggestionHelper mSuggestionHelper;
    SuggestionRangeSpan mSuggestionRangeSpan;
    private SuggestionsPopupWindow mSuggestionsPopupWindow;
    private Rect mTempRect;
    private ActionMode mTextActionMode;
    boolean mTextIsSelectable;
    private TextRenderNode[] mTextRenderNodes;
    private final TextView mTextView;
    boolean mTouchFocusSelected;
    private boolean mUpdateWordIteratorText;
    private WordIterator mWordIterator;
    private WordIterator mWordIteratorWithText;
    private final UndoManager mUndoManager = new UndoManager();
    private UndoOwner mUndoOwner = this.mUndoManager.getOwner("Editor", this);
    final UndoInputFilter mUndoInputFilter = new UndoInputFilter(this);
    boolean mAllowUndo = true;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final Runnable mUpdateMagnifierRunnable = new Runnable() {
        @Override
        public void run() {
            Editor.this.mMagnifierAnimator.update();
        }
    };
    private final ViewTreeObserver.OnDrawListener mMagnifierOnDrawListener = new ViewTreeObserver.OnDrawListener() {
        @Override
        public void onDraw() {
            if (Editor.this.mMagnifierAnimator != null) {
                Editor.this.mTextView.post(Editor.this.mUpdateMagnifierRunnable);
            }
        }
    };
    int mInputType = 0;
    boolean mCursorVisible = true;
    boolean mShowSoftInputOnFocus = true;
    Drawable mDrawableForCursor = null;
    private int mTapState = 0;
    private long mLastTouchUpTime = 0;
    private final Runnable mShowFloatingToolbar = new Runnable() {
        @Override
        public void run() {
            if (Editor.this.mTextActionMode != null) {
                Editor.this.mTextActionMode.hide(0L);
            }
        }
    };
    boolean mIsInsertionActionModeStartPending = false;
    private final MenuItem.OnMenuItemClickListener mOnContextMenuItemClickListener = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            if (!Editor.this.mProcessTextIntentActionsHandler.performMenuItemAction(menuItem)) {
                return Editor.this.mTextView.onTextContextMenuItem(menuItem.getItemId());
            }
            return true;
        }
    };

    private interface CursorController extends ViewTreeObserver.OnTouchModeChangeListener {
        void hide();

        boolean isActive();

        boolean isCursorBeingModified();

        void onDetached();

        void show();
    }

    private interface EasyEditDeleteListener {
        void onDeleteClick(EasyEditSpan easyEditSpan);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface HandleType {
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface MagnifierHandleTrigger {
        public static final int INSERTION = 0;
        public static final int SELECTION_END = 2;
        public static final int SELECTION_START = 1;
    }

    @interface TextActionMode {
        public static final int INSERTION = 1;
        public static final int SELECTION = 0;
        public static final int TEXT_LINK = 2;
    }

    private interface TextViewPositionListener {
        void updatePosition(int i, int i2, boolean z, boolean z2);
    }

    private static class TextRenderNode {
        boolean isDirty = true;
        boolean needsToBeShifted = true;
        RenderNode renderNode;

        public TextRenderNode(String str) {
            this.renderNode = RenderNode.create(str, null);
        }

        boolean needsRecord() {
            return this.isDirty || !this.renderNode.isValid();
        }
    }

    Editor(TextView textView) {
        this.mCursorAnchorInfoNotifier = new CursorAnchorInfoNotifier();
        this.mSuggestionHelper = new SuggestionHelper();
        this.mTextView = textView;
        this.mTextView.setFilters(this.mTextView.getFilters());
        this.mProcessTextIntentActionsHandler = new ProcessTextIntentActionsHandler();
        this.mHapticTextHandleEnabled = this.mTextView.getContext().getResources().getBoolean(R.bool.config_enableHapticTextHandle);
        this.mMagnifierAnimator = new MagnifierMotionAnimator(new Magnifier(this.mTextView));
    }

    ParcelableParcel saveInstanceState() {
        ParcelableParcel parcelableParcel = new ParcelableParcel(getClass().getClassLoader());
        Parcel parcel = parcelableParcel.getParcel();
        this.mUndoManager.saveInstanceState(parcel);
        this.mUndoInputFilter.saveInstanceState(parcel);
        return parcelableParcel;
    }

    void restoreInstanceState(ParcelableParcel parcelableParcel) {
        Parcel parcel = parcelableParcel.getParcel();
        this.mUndoManager.restoreInstanceState(parcel, parcelableParcel.getClassLoader());
        this.mUndoInputFilter.restoreInstanceState(parcel);
        this.mUndoOwner = this.mUndoManager.getOwner("Editor", this);
    }

    void forgetUndoRedo() {
        UndoOwner[] undoOwnerArr = {this.mUndoOwner};
        this.mUndoManager.forgetUndos(undoOwnerArr, -1);
        this.mUndoManager.forgetRedos(undoOwnerArr, -1);
    }

    boolean canUndo() {
        return this.mAllowUndo && this.mUndoManager.countUndos(new UndoOwner[]{this.mUndoOwner}) > 0;
    }

    boolean canRedo() {
        return this.mAllowUndo && this.mUndoManager.countRedos(new UndoOwner[]{this.mUndoOwner}) > 0;
    }

    void undo() {
        if (!this.mAllowUndo) {
            return;
        }
        this.mUndoManager.undo(new UndoOwner[]{this.mUndoOwner}, 1);
    }

    void redo() {
        if (!this.mAllowUndo) {
            return;
        }
        this.mUndoManager.redo(new UndoOwner[]{this.mUndoOwner}, 1);
    }

    void replace() {
        if (this.mSuggestionsPopupWindow == null) {
            this.mSuggestionsPopupWindow = new SuggestionsPopupWindow();
        }
        hideCursorAndSpanControllers();
        this.mSuggestionsPopupWindow.show();
        Selection.setSelection((Spannable) this.mTextView.getText(), (this.mTextView.getSelectionStart() + this.mTextView.getSelectionEnd()) / 2);
    }

    void onAttachedToWindow() {
        if (this.mShowErrorAfterAttach) {
            showError();
            this.mShowErrorAfterAttach = false;
        }
        ViewTreeObserver viewTreeObserver = this.mTextView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            if (this.mInsertionPointCursorController != null) {
                viewTreeObserver.addOnTouchModeChangeListener(this.mInsertionPointCursorController);
            }
            if (this.mSelectionModifierCursorController != null) {
                this.mSelectionModifierCursorController.resetTouchOffsets();
                viewTreeObserver.addOnTouchModeChangeListener(this.mSelectionModifierCursorController);
            }
            viewTreeObserver.addOnDrawListener(this.mMagnifierOnDrawListener);
        }
        updateSpellCheckSpans(0, this.mTextView.getText().length(), true);
        if (this.mTextView.hasSelection()) {
            refreshTextActionMode();
        }
        getPositionListener().addSubscriber(this.mCursorAnchorInfoNotifier, true);
        resumeBlink();
    }

    void onDetachedFromWindow() {
        getPositionListener().removeSubscriber(this.mCursorAnchorInfoNotifier);
        if (this.mError != null) {
            hideError();
        }
        suspendBlink();
        if (this.mInsertionPointCursorController != null) {
            this.mInsertionPointCursorController.onDetached();
        }
        if (this.mSelectionModifierCursorController != null) {
            this.mSelectionModifierCursorController.onDetached();
        }
        if (this.mShowSuggestionRunnable != null) {
            this.mTextView.removeCallbacks(this.mShowSuggestionRunnable);
        }
        if (this.mInsertionActionModeRunnable != null) {
            this.mTextView.removeCallbacks(this.mInsertionActionModeRunnable);
        }
        this.mTextView.removeCallbacks(this.mShowFloatingToolbar);
        discardTextDisplayLists();
        if (this.mSpellChecker != null) {
            this.mSpellChecker.closeSession();
            this.mSpellChecker = null;
        }
        ViewTreeObserver viewTreeObserver = this.mTextView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.removeOnDrawListener(this.mMagnifierOnDrawListener);
        }
        hideCursorAndSpanControllers();
        stopTextActionModeWithPreservingSelection();
    }

    private void discardTextDisplayLists() {
        if (this.mTextRenderNodes != null) {
            for (int i = 0; i < this.mTextRenderNodes.length; i++) {
                RenderNode renderNode = this.mTextRenderNodes[i] != null ? this.mTextRenderNodes[i].renderNode : null;
                if (renderNode != null && renderNode.isValid()) {
                    renderNode.discardDisplayList();
                }
            }
        }
    }

    private void showError() {
        if (this.mTextView.getWindowToken() == null) {
            this.mShowErrorAfterAttach = true;
            return;
        }
        if (this.mErrorPopup == null) {
            TextView textView = (TextView) LayoutInflater.from(this.mTextView.getContext()).inflate(R.layout.textview_hint, (ViewGroup) null);
            float f = this.mTextView.getResources().getDisplayMetrics().density;
            this.mErrorPopup = new ErrorPopup(textView, (int) ((200.0f * f) + LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS), (int) ((50.0f * f) + LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS));
            this.mErrorPopup.setFocusable(false);
            this.mErrorPopup.setInputMethodMode(1);
        }
        TextView textView2 = (TextView) this.mErrorPopup.getContentView();
        chooseSize(this.mErrorPopup, this.mError, textView2);
        textView2.setText(this.mError);
        this.mErrorPopup.showAsDropDown(this.mTextView, getErrorX(), getErrorY(), 51);
        this.mErrorPopup.fixDirection(this.mErrorPopup.isAboveAnchor());
    }

    public void setError(CharSequence charSequence, Drawable drawable) {
        this.mError = TextUtils.stringOrSpannedString(charSequence);
        this.mErrorWasChanged = true;
        if (this.mError == null) {
            setErrorIcon(null);
            if (this.mErrorPopup != null) {
                if (this.mErrorPopup.isShowing()) {
                    this.mErrorPopup.dismiss();
                }
                this.mErrorPopup = null;
            }
            this.mShowErrorAfterAttach = false;
            return;
        }
        setErrorIcon(drawable);
        if (this.mTextView.isFocused()) {
            showError();
        }
    }

    private void setErrorIcon(Drawable drawable) {
        TextView.Drawables drawables = this.mTextView.mDrawables;
        if (drawables == null) {
            TextView textView = this.mTextView;
            TextView.Drawables drawables2 = new TextView.Drawables(this.mTextView.getContext());
            textView.mDrawables = drawables2;
            drawables = drawables2;
        }
        drawables.setErrorDrawable(drawable, this.mTextView);
        this.mTextView.resetResolvedDrawables();
        this.mTextView.invalidate();
        this.mTextView.requestLayout();
    }

    private void hideError() {
        if (this.mErrorPopup != null && this.mErrorPopup.isShowing()) {
            this.mErrorPopup.dismiss();
        }
        this.mShowErrorAfterAttach = false;
    }

    private int getErrorX() {
        float f = this.mTextView.getResources().getDisplayMetrics().density;
        TextView.Drawables drawables = this.mTextView.mDrawables;
        if (this.mTextView.getLayoutDirection() != 1) {
            return ((this.mTextView.getWidth() - this.mErrorPopup.getWidth()) - this.mTextView.getPaddingRight()) + ((-(drawables != null ? drawables.mDrawableSizeRight : 0)) / 2) + ((int) ((25.0f * f) + LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS));
        }
        return this.mTextView.getPaddingLeft() + (((drawables != null ? drawables.mDrawableSizeLeft : 0) / 2) - ((int) ((25.0f * f) + LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS)));
    }

    private int getErrorY() {
        int compoundPaddingTop = this.mTextView.getCompoundPaddingTop();
        int bottom = ((this.mTextView.getBottom() - this.mTextView.getTop()) - this.mTextView.getCompoundPaddingBottom()) - compoundPaddingTop;
        TextView.Drawables drawables = this.mTextView.mDrawables;
        int i = 0;
        if (this.mTextView.getLayoutDirection() != 1) {
            if (drawables != null) {
                i = drawables.mDrawableHeightRight;
            }
        } else if (drawables != null) {
            i = drawables.mDrawableHeightLeft;
        }
        return (((compoundPaddingTop + ((bottom - i) / 2)) + i) - this.mTextView.getHeight()) - ((int) ((2.0f * this.mTextView.getResources().getDisplayMetrics().density) + LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS));
    }

    void createInputContentTypeIfNeeded() {
        if (this.mInputContentType == null) {
            this.mInputContentType = new InputContentType();
        }
    }

    void createInputMethodStateIfNeeded() {
        if (this.mInputMethodState == null) {
            this.mInputMethodState = new InputMethodState();
        }
    }

    private boolean isCursorVisible() {
        return this.mCursorVisible && this.mTextView.isTextEditable();
    }

    boolean shouldRenderCursor() {
        if (isCursorVisible()) {
            return this.mRenderCursorRegardlessTiming || (SystemClock.uptimeMillis() - this.mShowCursor) % 1000 < 500;
        }
        return false;
    }

    void prepareCursorControllers() {
        boolean z;
        ViewGroup.LayoutParams layoutParams = this.mTextView.getRootView().getLayoutParams();
        if (layoutParams instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams layoutParams2 = (WindowManager.LayoutParams) layoutParams;
            z = layoutParams2.type < 1000 || layoutParams2.type > 1999;
        }
        boolean z2 = z && this.mTextView.getLayout() != null;
        this.mInsertionControllerEnabled = z2 && isCursorVisible();
        this.mSelectionControllerEnabled = z2 && this.mTextView.textCanBeSelected();
        if (!this.mInsertionControllerEnabled) {
            hideInsertionPointCursorController();
            if (this.mInsertionPointCursorController != null) {
                this.mInsertionPointCursorController.onDetached();
                this.mInsertionPointCursorController = null;
            }
        }
        if (!this.mSelectionControllerEnabled) {
            stopTextActionMode();
            if (this.mSelectionModifierCursorController != null) {
                this.mSelectionModifierCursorController.onDetached();
                this.mSelectionModifierCursorController = null;
            }
        }
    }

    void hideInsertionPointCursorController() {
        if (this.mInsertionPointCursorController != null) {
            this.mInsertionPointCursorController.hide();
        }
    }

    void hideCursorAndSpanControllers() {
        hideCursorControllers();
        hideSpanControllers();
    }

    private void hideSpanControllers() {
        if (this.mSpanController != null) {
            this.mSpanController.hide();
        }
    }

    private void hideCursorControllers() {
        if (this.mSuggestionsPopupWindow != null && (this.mTextView.isInExtractedMode() || !this.mSuggestionsPopupWindow.isShowingUp())) {
            this.mSuggestionsPopupWindow.hide();
        }
        hideInsertionPointCursorController();
    }

    private void updateSpellCheckSpans(int i, int i2, boolean z) {
        this.mTextView.removeAdjacentSuggestionSpans(i);
        this.mTextView.removeAdjacentSuggestionSpans(i2);
        if (this.mTextView.isTextEditable() && this.mTextView.isSuggestionsEnabled() && !this.mTextView.isInExtractedMode()) {
            if (this.mSpellChecker == null && z) {
                this.mSpellChecker = new SpellChecker(this.mTextView);
            }
            if (this.mSpellChecker != null) {
                this.mSpellChecker.spellCheck(i, i2);
            }
        }
    }

    void onScreenStateChanged(int i) {
        switch (i) {
            case 0:
                suspendBlink();
                break;
            case 1:
                resumeBlink();
                break;
        }
    }

    private void suspendBlink() {
        if (this.mBlink != null) {
            this.mBlink.cancel();
        }
    }

    private void resumeBlink() {
        if (this.mBlink != null) {
            this.mBlink.uncancel();
            makeBlink();
        }
    }

    void adjustInputType(boolean z, boolean z2, boolean z3, boolean z4) {
        if ((this.mInputType & 15) == 1) {
            if (z || z2) {
                this.mInputType = (this.mInputType & (-4081)) | 128;
            }
            if (z3) {
                this.mInputType = (this.mInputType & (-4081)) | 224;
                return;
            }
            return;
        }
        if ((this.mInputType & 15) == 2 && z4) {
            this.mInputType = (this.mInputType & (-4081)) | 16;
        }
    }

    private void chooseSize(PopupWindow popupWindow, CharSequence charSequence, TextView textView) {
        int paddingLeft = textView.getPaddingLeft() + textView.getPaddingRight();
        int paddingTop = textView.getPaddingTop() + textView.getPaddingBottom();
        StaticLayout staticLayoutBuild = StaticLayout.Builder.obtain(charSequence, 0, charSequence.length(), textView.getPaint(), this.mTextView.getResources().getDimensionPixelSize(R.dimen.textview_error_popup_default_width)).setUseLineSpacingFromFallbacks(textView.mUseFallbackLineSpacing).build();
        float fMax = 0.0f;
        for (int i = 0; i < staticLayoutBuild.getLineCount(); i++) {
            fMax = Math.max(fMax, staticLayoutBuild.getLineWidth(i));
        }
        popupWindow.setWidth(paddingLeft + ((int) Math.ceil(fMax)));
        popupWindow.setHeight(paddingTop + staticLayoutBuild.getHeight());
    }

    void setFrame() {
        if (this.mErrorPopup != null) {
            chooseSize(this.mErrorPopup, this.mError, (TextView) this.mErrorPopup.getContentView());
            this.mErrorPopup.update(this.mTextView, getErrorX(), getErrorY(), this.mErrorPopup.getWidth(), this.mErrorPopup.getHeight());
        }
    }

    private int getWordStart(int i) {
        int prevWordBeginningOnTwoWordsBoundary;
        if (getWordIteratorWithText().isOnPunctuation(getWordIteratorWithText().prevBoundary(i))) {
            prevWordBeginningOnTwoWordsBoundary = getWordIteratorWithText().getPunctuationBeginning(i);
        } else {
            prevWordBeginningOnTwoWordsBoundary = getWordIteratorWithText().getPrevWordBeginningOnTwoWordsBoundary(i);
        }
        if (prevWordBeginningOnTwoWordsBoundary == -1) {
            return i;
        }
        return prevWordBeginningOnTwoWordsBoundary;
    }

    private int getWordEnd(int i) {
        int nextWordEndOnTwoWordBoundary;
        if (getWordIteratorWithText().isAfterPunctuation(getWordIteratorWithText().nextBoundary(i))) {
            nextWordEndOnTwoWordBoundary = getWordIteratorWithText().getPunctuationEnd(i);
        } else {
            nextWordEndOnTwoWordBoundary = getWordIteratorWithText().getNextWordEndOnTwoWordBoundary(i);
        }
        if (nextWordEndOnTwoWordBoundary == -1) {
            return i;
        }
        return nextWordEndOnTwoWordBoundary;
    }

    private boolean needsToSelectAllToSelectWordOrParagraph() {
        if (this.mTextView.hasPasswordTransformationMethod()) {
            return true;
        }
        int inputType = this.mTextView.getInputType();
        int i = inputType & 15;
        int i2 = inputType & InputType.TYPE_MASK_VARIATION;
        return i == 2 || i == 3 || i == 4 || i2 == 16 || i2 == 32 || i2 == 208 || i2 == 176;
    }

    boolean selectCurrentWord() {
        int end;
        int iUnpackRangeStartFromLong;
        if (!this.mTextView.canSelectText()) {
            return false;
        }
        if (needsToSelectAllToSelectWordOrParagraph()) {
            return this.mTextView.selectAllText();
        }
        long lastTouchOffsets = getLastTouchOffsets();
        int iUnpackRangeStartFromLong2 = TextUtils.unpackRangeStartFromLong(lastTouchOffsets);
        int iUnpackRangeEndFromLong = TextUtils.unpackRangeEndFromLong(lastTouchOffsets);
        if (iUnpackRangeStartFromLong2 < 0 || iUnpackRangeStartFromLong2 > this.mTextView.getText().length() || iUnpackRangeEndFromLong < 0 || iUnpackRangeEndFromLong > this.mTextView.getText().length()) {
            return false;
        }
        URLSpan[] uRLSpanArr = (URLSpan[]) ((Spanned) this.mTextView.getText()).getSpans(iUnpackRangeStartFromLong2, iUnpackRangeEndFromLong, URLSpan.class);
        if (uRLSpanArr.length >= 1) {
            URLSpan uRLSpan = uRLSpanArr[0];
            int spanStart = ((Spanned) this.mTextView.getText()).getSpanStart(uRLSpan);
            end = ((Spanned) this.mTextView.getText()).getSpanEnd(uRLSpan);
            iUnpackRangeStartFromLong = spanStart;
        } else {
            WordIterator wordIterator = getWordIterator();
            wordIterator.setCharSequence(this.mTextView.getText(), iUnpackRangeStartFromLong2, iUnpackRangeEndFromLong);
            int beginning = wordIterator.getBeginning(iUnpackRangeStartFromLong2);
            end = wordIterator.getEnd(iUnpackRangeEndFromLong);
            if (beginning == -1 || end == -1 || beginning == end) {
                long charClusterRange = getCharClusterRange(iUnpackRangeStartFromLong2);
                iUnpackRangeStartFromLong = TextUtils.unpackRangeStartFromLong(charClusterRange);
                end = TextUtils.unpackRangeEndFromLong(charClusterRange);
            } else {
                iUnpackRangeStartFromLong = beginning;
            }
        }
        Selection.setSelection((Spannable) this.mTextView.getText(), iUnpackRangeStartFromLong, end);
        return end > iUnpackRangeStartFromLong;
    }

    private boolean selectCurrentParagraph() {
        if (!this.mTextView.canSelectText()) {
            return false;
        }
        if (needsToSelectAllToSelectWordOrParagraph()) {
            return this.mTextView.selectAllText();
        }
        long lastTouchOffsets = getLastTouchOffsets();
        long paragraphsRange = getParagraphsRange(TextUtils.unpackRangeStartFromLong(lastTouchOffsets), TextUtils.unpackRangeEndFromLong(lastTouchOffsets));
        int iUnpackRangeStartFromLong = TextUtils.unpackRangeStartFromLong(paragraphsRange);
        int iUnpackRangeEndFromLong = TextUtils.unpackRangeEndFromLong(paragraphsRange);
        if (iUnpackRangeStartFromLong >= iUnpackRangeEndFromLong) {
            return false;
        }
        Selection.setSelection((Spannable) this.mTextView.getText(), iUnpackRangeStartFromLong, iUnpackRangeEndFromLong);
        return true;
    }

    private long getParagraphsRange(int i, int i2) {
        Layout layout = this.mTextView.getLayout();
        if (layout == null) {
            return TextUtils.packRangeInLong(-1, -1);
        }
        CharSequence text = this.mTextView.getText();
        int lineForOffset = layout.getLineForOffset(i);
        while (lineForOffset > 0 && text.charAt(layout.getLineEnd(lineForOffset - 1) - 1) != '\n') {
            lineForOffset--;
        }
        int lineForOffset2 = layout.getLineForOffset(i2);
        while (lineForOffset2 < layout.getLineCount() - 1 && text.charAt(layout.getLineEnd(lineForOffset2) - 1) != '\n') {
            lineForOffset2++;
        }
        return TextUtils.packRangeInLong(layout.getLineStart(lineForOffset), layout.getLineEnd(lineForOffset2));
    }

    void onLocaleChanged() {
        this.mWordIterator = null;
        this.mWordIteratorWithText = null;
    }

    public WordIterator getWordIterator() {
        if (this.mWordIterator == null) {
            this.mWordIterator = new WordIterator(this.mTextView.getTextServicesLocale());
        }
        return this.mWordIterator;
    }

    private WordIterator getWordIteratorWithText() {
        if (this.mWordIteratorWithText == null) {
            this.mWordIteratorWithText = new WordIterator(this.mTextView.getTextServicesLocale());
            this.mUpdateWordIteratorText = true;
        }
        if (this.mUpdateWordIteratorText) {
            CharSequence text = this.mTextView.getText();
            this.mWordIteratorWithText.setCharSequence(text, 0, text.length());
            this.mUpdateWordIteratorText = false;
        }
        return this.mWordIteratorWithText;
    }

    private int getNextCursorOffset(int i, boolean z) {
        Layout layout = this.mTextView.getLayout();
        return layout == null ? i : z == layout.isRtlCharAt(i) ? layout.getOffsetToLeftOf(i) : layout.getOffsetToRightOf(i);
    }

    private long getCharClusterRange(int i) {
        if (i < this.mTextView.getText().length()) {
            int nextCursorOffset = getNextCursorOffset(i, true);
            return TextUtils.packRangeInLong(getNextCursorOffset(nextCursorOffset, false), nextCursorOffset);
        }
        if (i - 1 >= 0) {
            int nextCursorOffset2 = getNextCursorOffset(i, false);
            return TextUtils.packRangeInLong(nextCursorOffset2, getNextCursorOffset(nextCursorOffset2, true));
        }
        return TextUtils.packRangeInLong(i, i);
    }

    private boolean touchPositionIsInSelection() {
        int selectionStart = this.mTextView.getSelectionStart();
        int selectionEnd = this.mTextView.getSelectionEnd();
        if (selectionStart == selectionEnd) {
            return false;
        }
        if (selectionStart > selectionEnd) {
            Selection.setSelection((Spannable) this.mTextView.getText(), selectionEnd, selectionStart);
            selectionEnd = selectionStart;
            selectionStart = selectionEnd;
        }
        SelectionModifierCursorController selectionController = getSelectionController();
        return selectionController.getMinTouchOffset() >= selectionStart && selectionController.getMaxTouchOffset() < selectionEnd;
    }

    private PositionListener getPositionListener() {
        if (this.mPositionListener == null) {
            this.mPositionListener = new PositionListener();
        }
        return this.mPositionListener;
    }

    private boolean isOffsetVisible(int i) {
        Layout layout = this.mTextView.getLayout();
        if (layout == null) {
            return false;
        }
        int lineBottom = layout.getLineBottom(layout.getLineForOffset(i));
        return this.mTextView.isPositionVisible(((int) layout.getPrimaryHorizontal(i)) + this.mTextView.viewportToContentHorizontalOffset(), lineBottom + this.mTextView.viewportToContentVerticalOffset());
    }

    private boolean isPositionOnText(float f, float f2) {
        Layout layout = this.mTextView.getLayout();
        if (layout == null) {
            return false;
        }
        int lineAtCoordinate = this.mTextView.getLineAtCoordinate(f2);
        float fConvertToLocalHorizontalCoordinate = this.mTextView.convertToLocalHorizontalCoordinate(f);
        if (fConvertToLocalHorizontalCoordinate < layout.getLineLeft(lineAtCoordinate) || fConvertToLocalHorizontalCoordinate > layout.getLineRight(lineAtCoordinate)) {
            return false;
        }
        return true;
    }

    private void startDragAndDrop() {
        getSelectionActionModeHelper().onSelectionDrag();
        if (this.mTextView.isInExtractedMode()) {
            return;
        }
        int selectionStart = this.mTextView.getSelectionStart();
        int selectionEnd = this.mTextView.getSelectionEnd();
        this.mTextView.startDragAndDrop(ClipData.newPlainText(null, this.mTextView.getTransformedText(selectionStart, selectionEnd)), getTextThumbnailBuilder(selectionStart, selectionEnd), new DragLocalState(this.mTextView, selectionStart, selectionEnd), 256);
        stopTextActionMode();
        if (hasSelectionController()) {
            getSelectionController().resetTouchOffsets();
        }
    }

    public boolean performLongClick(boolean z) {
        if (!z && !isPositionOnText(this.mLastDownPositionX, this.mLastDownPositionY) && this.mInsertionControllerEnabled) {
            Selection.setSelection((Spannable) this.mTextView.getText(), this.mTextView.getOffsetForPosition(this.mLastDownPositionX, this.mLastDownPositionY));
            getInsertionController().show();
            this.mIsInsertionActionModeStartPending = true;
            MetricsLogger.action(this.mTextView.getContext(), MetricsProto.MetricsEvent.TEXT_LONGPRESS, 0);
            z = true;
        }
        if (!z && this.mTextActionMode != null) {
            if (touchPositionIsInSelection()) {
                startDragAndDrop();
                MetricsLogger.action(this.mTextView.getContext(), MetricsProto.MetricsEvent.TEXT_LONGPRESS, 2);
            } else {
                stopTextActionMode();
                selectCurrentWordAndStartDrag();
                MetricsLogger.action(this.mTextView.getContext(), MetricsProto.MetricsEvent.TEXT_LONGPRESS, 1);
            }
            z = true;
        }
        if (!z && (z = selectCurrentWordAndStartDrag())) {
            MetricsLogger.action(this.mTextView.getContext(), MetricsProto.MetricsEvent.TEXT_LONGPRESS, 1);
        }
        return z;
    }

    float getLastUpPositionX() {
        return this.mLastUpPositionX;
    }

    float getLastUpPositionY() {
        return this.mLastUpPositionY;
    }

    private long getLastTouchOffsets() {
        SelectionModifierCursorController selectionController = getSelectionController();
        return TextUtils.packRangeInLong(selectionController.getMinTouchOffset(), selectionController.getMaxTouchOffset());
    }

    void onFocusChanged(boolean z, int i) {
        this.mShowCursor = SystemClock.uptimeMillis();
        ensureEndedBatchEdit();
        if (z) {
            int selectionStart = this.mTextView.getSelectionStart();
            int selectionEnd = this.mTextView.getSelectionEnd();
            this.mCreatedWithASelection = this.mFrozenWithFocus && this.mTextView.hasSelection() && !(this.mSelectAllOnFocus && selectionStart == 0 && selectionEnd == this.mTextView.getText().length());
            if (!this.mFrozenWithFocus || selectionStart < 0 || selectionEnd < 0) {
                int lastTapPosition = getLastTapPosition();
                if (lastTapPosition >= 0) {
                    Selection.setSelection((Spannable) this.mTextView.getText(), lastTapPosition);
                }
                MovementMethod movementMethod = this.mTextView.getMovementMethod();
                if (movementMethod != null) {
                    movementMethod.onTakeFocus(this.mTextView, (Spannable) this.mTextView.getText(), i);
                }
                if ((this.mTextView.isInExtractedMode() || this.mSelectionMoved) && selectionStart >= 0 && selectionEnd >= 0) {
                    Selection.setSelection((Spannable) this.mTextView.getText(), selectionStart, selectionEnd);
                }
                if (this.mSelectAllOnFocus) {
                    this.mTextView.selectAllText();
                }
                this.mTouchFocusSelected = true;
            }
            this.mFrozenWithFocus = false;
            this.mSelectionMoved = false;
            if (this.mError != null) {
                showError();
            }
            makeBlink();
            return;
        }
        if (this.mError != null) {
            hideError();
        }
        this.mTextView.onEndBatchEdit();
        if (this.mTextView.isInExtractedMode()) {
            hideCursorAndSpanControllers();
            stopTextActionModeWithPreservingSelection();
        } else {
            hideCursorAndSpanControllers();
            if (this.mTextView.isTemporarilyDetached()) {
                stopTextActionModeWithPreservingSelection();
            } else {
                stopTextActionMode();
            }
            downgradeEasyCorrectionSpans();
        }
        if (this.mSelectionModifierCursorController != null) {
            this.mSelectionModifierCursorController.resetTouchOffsets();
        }
        ensureNoSelectionIfNonSelectable();
    }

    private void ensureNoSelectionIfNonSelectable() {
        if (!this.mTextView.textCanBeSelected() && this.mTextView.hasSelection()) {
            Selection.setSelection((Spannable) this.mTextView.getText(), this.mTextView.length(), this.mTextView.length());
        }
    }

    private void downgradeEasyCorrectionSpans() {
        CharSequence text = this.mTextView.getText();
        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;
            SuggestionSpan[] suggestionSpanArr = (SuggestionSpan[]) spannable.getSpans(0, spannable.length(), SuggestionSpan.class);
            for (int i = 0; i < suggestionSpanArr.length; i++) {
                int flags = suggestionSpanArr[i].getFlags();
                if ((flags & 1) != 0 && (flags & 2) == 0) {
                    suggestionSpanArr[i].setFlags(flags & (-2));
                }
            }
        }
    }

    void sendOnTextChanged(int i, int i2, int i3) {
        getSelectionActionModeHelper().onTextChanged(i, i2 + i);
        updateSpellCheckSpans(i, i3 + i, false);
        this.mUpdateWordIteratorText = true;
        hideCursorControllers();
        if (this.mSelectionModifierCursorController != null) {
            this.mSelectionModifierCursorController.resetTouchOffsets();
        }
        stopTextActionMode();
    }

    private int getLastTapPosition() {
        int minTouchOffset;
        if (this.mSelectionModifierCursorController != null && (minTouchOffset = this.mSelectionModifierCursorController.getMinTouchOffset()) >= 0) {
            if (minTouchOffset > this.mTextView.getText().length()) {
                return this.mTextView.getText().length();
            }
            return minTouchOffset;
        }
        return -1;
    }

    void onWindowFocusChanged(boolean z) {
        if (z) {
            if (this.mBlink != null) {
                this.mBlink.uncancel();
                makeBlink();
            }
            if (this.mTextView.hasSelection() && !extractedTextModeWillBeStarted()) {
                refreshTextActionMode();
                return;
            }
            return;
        }
        if (this.mBlink != null) {
            this.mBlink.cancel();
        }
        if (this.mInputContentType != null) {
            this.mInputContentType.enterDown = false;
        }
        hideCursorAndSpanControllers();
        stopTextActionModeWithPreservingSelection();
        if (this.mSuggestionsPopupWindow != null) {
            this.mSuggestionsPopupWindow.onParentLostFocus();
        }
        ensureEndedBatchEdit();
        ensureNoSelectionIfNonSelectable();
    }

    private void updateTapState(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            boolean zIsFromSource = motionEvent.isFromSource(8194);
            if ((this.mTapState == 1 || (this.mTapState == 2 && zIsFromSource)) && SystemClock.uptimeMillis() - this.mLastTouchUpTime <= ViewConfiguration.getDoubleTapTimeout()) {
                if (this.mTapState == 1) {
                    this.mTapState = 2;
                } else {
                    this.mTapState = 3;
                }
            } else {
                this.mTapState = 1;
            }
        }
        if (actionMasked == 1) {
            this.mLastTouchUpTime = SystemClock.uptimeMillis();
        }
    }

    private boolean shouldFilterOutTouchEvent(MotionEvent motionEvent) {
        if (!motionEvent.isFromSource(8194)) {
            return false;
        }
        boolean z = ((this.mLastButtonState ^ motionEvent.getButtonState()) & 1) != 0;
        int actionMasked = motionEvent.getActionMasked();
        if ((actionMasked == 0 || actionMasked == 1) && !z) {
            return true;
        }
        return actionMasked == 2 && !motionEvent.isButtonPressed(1);
    }

    void onTouchEvent(MotionEvent motionEvent) {
        boolean zShouldFilterOutTouchEvent = shouldFilterOutTouchEvent(motionEvent);
        this.mLastButtonState = motionEvent.getButtonState();
        if (zShouldFilterOutTouchEvent) {
            if (motionEvent.getActionMasked() == 1) {
                this.mDiscardNextActionUp = true;
                return;
            }
            return;
        }
        updateTapState(motionEvent);
        updateFloatingToolbarVisibility(motionEvent);
        if (hasSelectionController()) {
            getSelectionController().onTouchEvent(motionEvent);
        }
        if (this.mShowSuggestionRunnable != null) {
            this.mTextView.removeCallbacks(this.mShowSuggestionRunnable);
            this.mShowSuggestionRunnable = null;
        }
        if (motionEvent.getActionMasked() == 1) {
            this.mLastUpPositionX = motionEvent.getX();
            this.mLastUpPositionY = motionEvent.getY();
        }
        if (motionEvent.getActionMasked() == 0) {
            this.mLastDownPositionX = motionEvent.getX();
            this.mLastDownPositionY = motionEvent.getY();
            this.mTouchFocusSelected = false;
            this.mIgnoreActionUpEvent = false;
        }
    }

    private void updateFloatingToolbarVisibility(MotionEvent motionEvent) {
        if (this.mTextActionMode != null) {
            switch (motionEvent.getActionMasked()) {
                case 1:
                case 3:
                    showFloatingToolbar();
                    break;
                case 2:
                    hideFloatingToolbar(-1);
                    break;
            }
        }
    }

    void hideFloatingToolbar(int i) {
        if (this.mTextActionMode != null) {
            this.mTextView.removeCallbacks(this.mShowFloatingToolbar);
            this.mTextActionMode.hide(i);
        }
    }

    private void showFloatingToolbar() {
        if (this.mTextActionMode != null) {
            this.mTextView.postDelayed(this.mShowFloatingToolbar, ViewConfiguration.getDoubleTapTimeout());
            invalidateActionModeAsync();
        }
    }

    public void beginBatchEdit() {
        this.mInBatchEditControllers = true;
        InputMethodState inputMethodState = this.mInputMethodState;
        if (inputMethodState != null) {
            int i = inputMethodState.mBatchEditNesting + 1;
            inputMethodState.mBatchEditNesting = i;
            if (i == 1) {
                inputMethodState.mCursorChanged = false;
                inputMethodState.mChangedDelta = 0;
                if (inputMethodState.mContentChanged) {
                    inputMethodState.mChangedStart = 0;
                    inputMethodState.mChangedEnd = this.mTextView.getText().length();
                } else {
                    inputMethodState.mChangedStart = -1;
                    inputMethodState.mChangedEnd = -1;
                    inputMethodState.mContentChanged = false;
                }
                this.mUndoInputFilter.beginBatchEdit();
                this.mTextView.onBeginBatchEdit();
            }
        }
    }

    public void endBatchEdit() {
        this.mInBatchEditControllers = false;
        InputMethodState inputMethodState = this.mInputMethodState;
        if (inputMethodState != null) {
            int i = inputMethodState.mBatchEditNesting - 1;
            inputMethodState.mBatchEditNesting = i;
            if (i == 0) {
                finishBatchEdit(inputMethodState);
            }
        }
    }

    void ensureEndedBatchEdit() {
        InputMethodState inputMethodState = this.mInputMethodState;
        if (inputMethodState != null && inputMethodState.mBatchEditNesting != 0) {
            inputMethodState.mBatchEditNesting = 0;
            finishBatchEdit(inputMethodState);
        }
    }

    void finishBatchEdit(InputMethodState inputMethodState) {
        this.mTextView.onEndBatchEdit();
        this.mUndoInputFilter.endBatchEdit();
        if (inputMethodState.mContentChanged || inputMethodState.mSelectionModeChanged) {
            this.mTextView.updateAfterEdit();
            reportExtractedText();
        } else if (inputMethodState.mCursorChanged) {
            this.mTextView.invalidateCursor();
        }
        sendUpdateSelection();
        if (this.mTextActionMode != null) {
            CursorController selectionController = this.mTextView.hasSelection() ? getSelectionController() : getInsertionController();
            if (selectionController != null && !selectionController.isActive() && !selectionController.isCursorBeingModified()) {
                selectionController.show();
            }
        }
    }

    boolean extractText(ExtractedTextRequest extractedTextRequest, ExtractedText extractedText) {
        return extractTextInternal(extractedTextRequest, -1, -1, -1, extractedText);
    }

    private boolean extractTextInternal(ExtractedTextRequest extractedTextRequest, int i, int i2, int i3, ExtractedText extractedText) {
        CharSequence text;
        if (extractedTextRequest == null || extractedText == null || (text = this.mTextView.getText()) == null) {
            return false;
        }
        if (i != -2) {
            int length = text.length();
            if (i < 0) {
                extractedText.partialEndOffset = -1;
                extractedText.partialStartOffset = -1;
                i = 0;
            } else {
                int i4 = i2 + i3;
                if (text instanceof Spanned) {
                    Spanned spanned = (Spanned) text;
                    Object[] spans = spanned.getSpans(i, i4, ParcelableSpan.class);
                    int length2 = spans.length;
                    while (length2 > 0) {
                        length2--;
                        int spanStart = spanned.getSpanStart(spans[length2]);
                        if (spanStart < i) {
                            i = spanStart;
                        }
                        int spanEnd = spanned.getSpanEnd(spans[length2]);
                        if (spanEnd > i4) {
                            i4 = spanEnd;
                        }
                    }
                }
                extractedText.partialStartOffset = i;
                extractedText.partialEndOffset = i4 - i3;
                if (i <= length) {
                    if (i < 0) {
                        i = 0;
                    }
                } else {
                    i = length;
                }
                if (i4 <= length) {
                    length = i4 < 0 ? 0 : i4;
                }
            }
            if ((extractedTextRequest.flags & 1) != 0) {
                extractedText.text = text.subSequence(i, length);
            } else {
                extractedText.text = TextUtils.substring(text, i, length);
            }
        } else {
            extractedText.partialStartOffset = 0;
            extractedText.partialEndOffset = 0;
            extractedText.text = "";
        }
        extractedText.flags = 0;
        if (MetaKeyKeyListener.getMetaState(text, 2048) != 0) {
            extractedText.flags |= 2;
        }
        if (this.mTextView.isSingleLine()) {
            extractedText.flags |= 1;
        }
        extractedText.startOffset = 0;
        extractedText.selectionStart = this.mTextView.getSelectionStart();
        extractedText.selectionEnd = this.mTextView.getSelectionEnd();
        extractedText.hint = this.mTextView.getHint();
        return true;
    }

    boolean reportExtractedText() {
        InputMethodManager inputMethodManagerPeekInstance;
        InputMethodState inputMethodState = this.mInputMethodState;
        if (inputMethodState == null) {
            return false;
        }
        boolean z = inputMethodState.mContentChanged;
        if (!z && !inputMethodState.mSelectionModeChanged) {
            return false;
        }
        inputMethodState.mContentChanged = false;
        inputMethodState.mSelectionModeChanged = false;
        ExtractedTextRequest extractedTextRequest = inputMethodState.mExtractedTextRequest;
        if (extractedTextRequest == null || (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) == null) {
            return false;
        }
        if (inputMethodState.mChangedStart < 0 && !z) {
            inputMethodState.mChangedStart = -2;
        }
        if (!extractTextInternal(extractedTextRequest, inputMethodState.mChangedStart, inputMethodState.mChangedEnd, inputMethodState.mChangedDelta, inputMethodState.mExtractedText)) {
            return false;
        }
        inputMethodManagerPeekInstance.updateExtractedText(this.mTextView, extractedTextRequest.token, inputMethodState.mExtractedText);
        inputMethodState.mChangedStart = -1;
        inputMethodState.mChangedEnd = -1;
        inputMethodState.mChangedDelta = 0;
        inputMethodState.mContentChanged = false;
        return true;
    }

    private void sendUpdateSelection() {
        InputMethodManager inputMethodManagerPeekInstance;
        int i;
        int composingSpanEnd;
        if (this.mInputMethodState != null && this.mInputMethodState.mBatchEditNesting <= 0 && (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) != null) {
            int selectionStart = this.mTextView.getSelectionStart();
            int selectionEnd = this.mTextView.getSelectionEnd();
            if (this.mTextView.getText() instanceof Spannable) {
                Spannable spannable = (Spannable) this.mTextView.getText();
                int composingSpanStart = EditableInputConnection.getComposingSpanStart(spannable);
                composingSpanEnd = EditableInputConnection.getComposingSpanEnd(spannable);
                i = composingSpanStart;
            } else {
                i = -1;
                composingSpanEnd = -1;
            }
            inputMethodManagerPeekInstance.updateSelection(this.mTextView, selectionStart, selectionEnd, i, composingSpanEnd);
        }
    }

    void onDraw(Canvas canvas, Layout layout, Path path, Paint paint, int i) {
        Path path2;
        InputMethodManager inputMethodManagerPeekInstance;
        int selectionStart = this.mTextView.getSelectionStart();
        int selectionEnd = this.mTextView.getSelectionEnd();
        InputMethodState inputMethodState = this.mInputMethodState;
        if (inputMethodState != null && inputMethodState.mBatchEditNesting == 0 && (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) != null && inputMethodManagerPeekInstance.isActive(this.mTextView) && (inputMethodState.mContentChanged || inputMethodState.mSelectionModeChanged)) {
            reportExtractedText();
        }
        if (this.mCorrectionHighlighter != null) {
            this.mCorrectionHighlighter.draw(canvas, i);
        }
        if (path != null && selectionStart == selectionEnd && this.mDrawableForCursor != null) {
            drawCursor(canvas, i);
            path = null;
        }
        if (this.mSelectionActionModeHelper != null) {
            this.mSelectionActionModeHelper.onDraw(canvas);
            path2 = this.mSelectionActionModeHelper.isDrawingHighlight() ? null : path;
        }
        if (this.mTextView.canHaveDisplayList() && canvas.isHardwareAccelerated()) {
            drawHardwareAccelerated(canvas, layout, path2, paint, i);
        } else {
            layout.draw(canvas, path2, paint, i);
        }
    }

    private void drawHardwareAccelerated(Canvas canvas, Layout layout, Path path, Paint paint, int i) {
        int i2;
        ArraySet<Integer> arraySet;
        int i3;
        int[] iArr;
        DynamicLayout dynamicLayout;
        int iMax;
        ArraySet<Integer> arraySet2;
        DynamicLayout dynamicLayout2;
        int i4;
        boolean z;
        int i5;
        int i6;
        Editor editor = this;
        Canvas canvas2 = canvas;
        Layout layout2 = layout;
        long lineRangeForDraw = layout2.getLineRangeForDraw(canvas2);
        int iUnpackRangeStartFromLong = TextUtils.unpackRangeStartFromLong(lineRangeForDraw);
        int iUnpackRangeEndFromLong = TextUtils.unpackRangeEndFromLong(lineRangeForDraw);
        if (iUnpackRangeEndFromLong < 0) {
            return;
        }
        layout2.drawBackground(canvas2, path, paint, i, iUnpackRangeStartFromLong, iUnpackRangeEndFromLong);
        if (layout2 instanceof DynamicLayout) {
            if (editor.mTextRenderNodes == null) {
                editor.mTextRenderNodes = (TextRenderNode[]) ArrayUtils.emptyArray(TextRenderNode.class);
            }
            DynamicLayout dynamicLayout3 = (DynamicLayout) layout2;
            int[] blockEndLines = dynamicLayout3.getBlockEndLines();
            int[] blockIndices = dynamicLayout3.getBlockIndices();
            int numberOfBlocks = dynamicLayout3.getNumberOfBlocks();
            int indexFirstChangedBlock = dynamicLayout3.getIndexFirstChangedBlock();
            ArraySet<Integer> blocksAlwaysNeedToBeRedrawn = dynamicLayout3.getBlocksAlwaysNeedToBeRedrawn();
            int i7 = -1;
            int i8 = 0;
            boolean z2 = true;
            if (blocksAlwaysNeedToBeRedrawn != null) {
                for (int i9 = 0; i9 < blocksAlwaysNeedToBeRedrawn.size(); i9++) {
                    int blockIndex = dynamicLayout3.getBlockIndex(blocksAlwaysNeedToBeRedrawn.valueAt(i9).intValue());
                    if (blockIndex != -1 && editor.mTextRenderNodes[blockIndex] != null) {
                        editor.mTextRenderNodes[blockIndex].needsToBeShifted = true;
                    }
                }
            }
            int iBinarySearch = Arrays.binarySearch(blockEndLines, 0, numberOfBlocks, iUnpackRangeStartFromLong);
            if (iBinarySearch < 0) {
                iBinarySearch = -(iBinarySearch + 1);
            }
            int iMin = Math.min(indexFirstChangedBlock, iBinarySearch);
            int iDrawHardwareAcceleratedInner = 0;
            while (true) {
                if (iMin < numberOfBlocks) {
                    int i10 = blockIndices[iMin];
                    if (iMin >= indexFirstChangedBlock && i10 != i7 && editor.mTextRenderNodes[i10] != null) {
                        editor.mTextRenderNodes[i10].needsToBeShifted = z2;
                    }
                    if (blockEndLines[iMin] < iUnpackRangeStartFromLong) {
                        i4 = iMin;
                        z = z2;
                        i2 = i8;
                        i6 = indexFirstChangedBlock;
                        i3 = numberOfBlocks;
                        iArr = blockEndLines;
                        dynamicLayout = dynamicLayout3;
                        i5 = iUnpackRangeStartFromLong;
                        arraySet = blocksAlwaysNeedToBeRedrawn;
                    } else {
                        i4 = iMin;
                        z = z2;
                        i2 = i8;
                        i5 = iUnpackRangeStartFromLong;
                        arraySet = blocksAlwaysNeedToBeRedrawn;
                        i6 = indexFirstChangedBlock;
                        i3 = numberOfBlocks;
                        iArr = blockEndLines;
                        dynamicLayout = dynamicLayout3;
                        iDrawHardwareAcceleratedInner = editor.drawHardwareAcceleratedInner(canvas2, layout2, path, paint, i, blockEndLines, blockIndices, i4, i3, iDrawHardwareAcceleratedInner);
                        if (iArr[i4] >= iUnpackRangeEndFromLong) {
                            iMax = Math.max(i6, i4 + 1);
                            break;
                        }
                    }
                    iMin = i4 + 1;
                    dynamicLayout3 = dynamicLayout;
                    indexFirstChangedBlock = i6;
                    blocksAlwaysNeedToBeRedrawn = arraySet;
                    z2 = z;
                    i8 = i2;
                    iUnpackRangeStartFromLong = i5;
                    numberOfBlocks = i3;
                    blockEndLines = iArr;
                    i7 = -1;
                    canvas2 = canvas;
                    layout2 = layout;
                } else {
                    i2 = i8;
                    arraySet = blocksAlwaysNeedToBeRedrawn;
                    i3 = numberOfBlocks;
                    iArr = blockEndLines;
                    dynamicLayout = dynamicLayout3;
                    iMax = i3;
                    break;
                }
            }
            if (arraySet != null) {
                int iDrawHardwareAcceleratedInner2 = iDrawHardwareAcceleratedInner;
                int i11 = i2;
                while (i11 < arraySet.size()) {
                    int iIntValue = arraySet.valueAt(i11).intValue();
                    int blockIndex2 = dynamicLayout.getBlockIndex(iIntValue);
                    if (blockIndex2 == -1 || editor.mTextRenderNodes[blockIndex2] == null || editor.mTextRenderNodes[blockIndex2].needsToBeShifted) {
                        arraySet2 = arraySet;
                        dynamicLayout2 = dynamicLayout;
                        iDrawHardwareAcceleratedInner2 = editor.drawHardwareAcceleratedInner(canvas, layout, path, paint, i, iArr, blockIndices, iIntValue, i3, iDrawHardwareAcceleratedInner2);
                    } else {
                        arraySet2 = arraySet;
                        dynamicLayout2 = dynamicLayout;
                    }
                    i11++;
                    dynamicLayout = dynamicLayout2;
                    arraySet = arraySet2;
                    editor = this;
                }
            }
            dynamicLayout.setIndexFirstChangedBlock(iMax);
            return;
        }
        layout2.drawText(canvas2, iUnpackRangeStartFromLong, iUnpackRangeEndFromLong);
    }

    private int drawHardwareAcceleratedInner(Canvas canvas, Layout layout, Path path, Paint paint, int i, int[] iArr, int[] iArr2, int i2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        int i8 = iArr[i2];
        int availableDisplayListIndex = iArr2[i2];
        if (availableDisplayListIndex == -1) {
            availableDisplayListIndex = getAvailableDisplayListIndex(iArr2, i3, i4);
            iArr2[i2] = availableDisplayListIndex;
            if (this.mTextRenderNodes[availableDisplayListIndex] != null) {
                this.mTextRenderNodes[availableDisplayListIndex].isDirty = true;
            }
            i5 = availableDisplayListIndex + 1;
        } else {
            i5 = i4;
        }
        if (this.mTextRenderNodes[availableDisplayListIndex] == null) {
            this.mTextRenderNodes[availableDisplayListIndex] = new TextRenderNode("Text " + availableDisplayListIndex);
        }
        boolean zNeedsRecord = this.mTextRenderNodes[availableDisplayListIndex].needsRecord();
        RenderNode renderNode = this.mTextRenderNodes[availableDisplayListIndex].renderNode;
        if (this.mTextRenderNodes[availableDisplayListIndex].needsToBeShifted || zNeedsRecord) {
            if (i2 != 0) {
                i6 = iArr[i2 - 1] + 1;
            } else {
                i6 = 0;
            }
            int lineTop = layout.getLineTop(i6);
            int lineBottom = layout.getLineBottom(i8);
            int width = this.mTextView.getWidth();
            if (this.mTextView.getHorizontallyScrolling()) {
                float fMax = Float.MIN_VALUE;
                float fMin = Float.MAX_VALUE;
                for (int i9 = i6; i9 <= i8; i9++) {
                    fMin = Math.min(fMin, layout.getLineLeft(i9));
                    fMax = Math.max(fMax, layout.getLineRight(i9));
                }
                i7 = (int) fMin;
                width = (int) (fMax + LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS);
            } else {
                i7 = 0;
            }
            if (zNeedsRecord) {
                DisplayListCanvas displayListCanvasStart = renderNode.start(width - i7, lineBottom - lineTop);
                try {
                    displayListCanvasStart.translate(-i7, -lineTop);
                    layout.drawText(displayListCanvasStart, i6, i8);
                    this.mTextRenderNodes[availableDisplayListIndex].isDirty = false;
                } finally {
                    renderNode.end(displayListCanvasStart);
                    renderNode.setClipToBounds(false);
                }
            }
            renderNode.setLeftTopRightBottom(i7, lineTop, width, lineBottom);
            this.mTextRenderNodes[availableDisplayListIndex].needsToBeShifted = false;
        }
        ((DisplayListCanvas) canvas).drawRenderNode(renderNode);
        return i5;
    }

    private int getAvailableDisplayListIndex(int[] iArr, int i, int i2) {
        int length = this.mTextRenderNodes.length;
        while (i2 < length) {
            boolean z = false;
            int i3 = 0;
            while (true) {
                if (i3 >= i) {
                    break;
                }
                if (iArr[i3] != i2) {
                    i3++;
                } else {
                    z = true;
                    break;
                }
            }
            if (z) {
                i2++;
            } else {
                return i2;
            }
        }
        this.mTextRenderNodes = (TextRenderNode[]) GrowingArrayUtils.append(this.mTextRenderNodes, length, (Object) null);
        return length;
    }

    private void drawCursor(Canvas canvas, int i) {
        boolean z = i != 0;
        if (z) {
            canvas.translate(0.0f, i);
        }
        if (this.mDrawableForCursor != null) {
            this.mDrawableForCursor.draw(canvas);
        }
        if (z) {
            canvas.translate(0.0f, -i);
        }
    }

    void invalidateHandlesAndActionMode() {
        if (this.mSelectionModifierCursorController != null) {
            this.mSelectionModifierCursorController.invalidateHandles();
        }
        if (this.mInsertionPointCursorController != null) {
            this.mInsertionPointCursorController.invalidateHandle();
        }
        if (this.mTextActionMode != null) {
            invalidateActionMode();
        }
    }

    void invalidateTextDisplayList(Layout layout, int i, int i2) {
        if (this.mTextRenderNodes != null && (layout instanceof DynamicLayout)) {
            int lineForOffset = layout.getLineForOffset(i);
            int lineForOffset2 = layout.getLineForOffset(i2);
            DynamicLayout dynamicLayout = (DynamicLayout) layout;
            int[] blockEndLines = dynamicLayout.getBlockEndLines();
            int[] blockIndices = dynamicLayout.getBlockIndices();
            int numberOfBlocks = dynamicLayout.getNumberOfBlocks();
            int i3 = 0;
            while (i3 < numberOfBlocks && blockEndLines[i3] < lineForOffset) {
                i3++;
            }
            while (i3 < numberOfBlocks) {
                int i4 = blockIndices[i3];
                if (i4 != -1) {
                    this.mTextRenderNodes[i4].isDirty = true;
                }
                if (blockEndLines[i3] < lineForOffset2) {
                    i3++;
                } else {
                    return;
                }
            }
        }
    }

    void invalidateTextDisplayList() {
        if (this.mTextRenderNodes != null) {
            for (int i = 0; i < this.mTextRenderNodes.length; i++) {
                if (this.mTextRenderNodes[i] != null) {
                    this.mTextRenderNodes[i].isDirty = true;
                }
            }
        }
    }

    void updateCursorPosition() {
        if (this.mTextView.mCursorDrawableRes == 0) {
            this.mDrawableForCursor = null;
            return;
        }
        Layout layout = this.mTextView.getLayout();
        int selectionStart = this.mTextView.getSelectionStart();
        int lineForOffset = layout.getLineForOffset(selectionStart);
        updateCursorPosition(layout.getLineTop(lineForOffset), layout.getLineBottomWithoutSpacing(lineForOffset), layout.getPrimaryHorizontal(selectionStart, layout.shouldClampCursor(lineForOffset)));
    }

    void refreshTextActionMode() {
        if (extractedTextModeWillBeStarted()) {
            this.mRestartActionModeOnNextRefresh = false;
            return;
        }
        boolean zHasSelection = this.mTextView.hasSelection();
        SelectionModifierCursorController selectionController = getSelectionController();
        InsertionPointCursorController insertionController = getInsertionController();
        if ((selectionController != null && selectionController.isCursorBeingModified()) || (insertionController != null && insertionController.isCursorBeingModified())) {
            this.mRestartActionModeOnNextRefresh = false;
            return;
        }
        if (zHasSelection) {
            hideInsertionPointCursorController();
            if (this.mTextActionMode == null) {
                if (this.mRestartActionModeOnNextRefresh) {
                    startSelectionActionModeAsync(false);
                }
            } else if (selectionController == null || !selectionController.isActive()) {
                stopTextActionModeWithPreservingSelection();
                startSelectionActionModeAsync(false);
            } else {
                this.mTextActionMode.invalidateContentRect();
            }
        } else if (insertionController == null || !insertionController.isActive()) {
            stopTextActionMode();
        } else if (this.mTextActionMode != null) {
            this.mTextActionMode.invalidateContentRect();
        }
        this.mRestartActionModeOnNextRefresh = false;
    }

    void startInsertionActionMode() {
        if (this.mInsertionActionModeRunnable != null) {
            this.mTextView.removeCallbacks(this.mInsertionActionModeRunnable);
        }
        if (extractedTextModeWillBeStarted()) {
            return;
        }
        stopTextActionMode();
        this.mTextActionMode = this.mTextView.startActionMode(new TextActionModeCallback(1), 1);
        if (this.mTextActionMode != null && getInsertionController() != null) {
            getInsertionController().show();
        }
    }

    TextView getTextView() {
        return this.mTextView;
    }

    ActionMode getTextActionMode() {
        return this.mTextActionMode;
    }

    void setRestartActionModeOnNextRefresh(boolean z) {
        this.mRestartActionModeOnNextRefresh = z;
    }

    void startSelectionActionModeAsync(boolean z) {
        getSelectionActionModeHelper().startSelectionActionModeAsync(z);
    }

    void startLinkActionModeAsync(int i, int i2) {
        if (!(this.mTextView.getText() instanceof Spannable)) {
            return;
        }
        stopTextActionMode();
        this.mRequestingLinkActionMode = true;
        getSelectionActionModeHelper().startLinkActionModeAsync(i, i2);
    }

    void invalidateActionModeAsync() {
        getSelectionActionModeHelper().invalidateActionModeAsync();
    }

    private void invalidateActionMode() {
        if (this.mTextActionMode != null) {
            this.mTextActionMode.invalidate();
        }
    }

    private SelectionActionModeHelper getSelectionActionModeHelper() {
        if (this.mSelectionActionModeHelper == null) {
            this.mSelectionActionModeHelper = new SelectionActionModeHelper(this);
        }
        return this.mSelectionActionModeHelper;
    }

    private boolean selectCurrentWordAndStartDrag() {
        if (this.mInsertionActionModeRunnable != null) {
            this.mTextView.removeCallbacks(this.mInsertionActionModeRunnable);
        }
        if (extractedTextModeWillBeStarted() || !checkField()) {
            return false;
        }
        if (!this.mTextView.hasSelection() && !selectCurrentWord()) {
            return false;
        }
        stopTextActionModeWithPreservingSelection();
        getSelectionController().enterDrag(2);
        return true;
    }

    boolean checkField() {
        if (!this.mTextView.canSelectText() || !this.mTextView.requestFocus()) {
            Log.w("TextView", "TextView does not support text selection. Selection cancelled.");
            return false;
        }
        return true;
    }

    boolean startActionModeInternal(@TextActionMode int i) {
        InputMethodManager inputMethodManagerPeekInstance;
        if (extractedTextModeWillBeStarted()) {
            return false;
        }
        if (this.mTextActionMode != null) {
            invalidateActionMode();
            return false;
        }
        if (i != 2 && (!checkField() || !this.mTextView.hasSelection())) {
            return false;
        }
        this.mTextActionMode = this.mTextView.startActionMode(new TextActionModeCallback(i), 1);
        boolean z = this.mTextView.isTextEditable() || this.mTextView.isTextSelectable();
        if (i == 2 && !z && (this.mTextActionMode instanceof FloatingActionMode)) {
            ((FloatingActionMode) this.mTextActionMode).setOutsideTouchable(true, new PopupWindow.OnDismissListener() {
                @Override
                public final void onDismiss() {
                    this.f$0.stopTextActionMode();
                }
            });
        }
        boolean z2 = this.mTextActionMode != null;
        if (z2 && this.mTextView.isTextEditable() && !this.mTextView.isTextSelectable() && this.mShowSoftInputOnFocus && (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) != null) {
            inputMethodManagerPeekInstance.showSoftInput(this.mTextView, 0, null);
        }
        return z2;
    }

    private boolean extractedTextModeWillBeStarted() {
        InputMethodManager inputMethodManagerPeekInstance;
        return (this.mTextView.isInExtractedMode() || (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) == null || !inputMethodManagerPeekInstance.isFullscreenMode()) ? false : true;
    }

    private boolean shouldOfferToShowSuggestions() {
        CharSequence text = this.mTextView.getText();
        if (!(text instanceof Spannable)) {
            return false;
        }
        Spannable spannable = (Spannable) text;
        int selectionStart = this.mTextView.getSelectionStart();
        int selectionEnd = this.mTextView.getSelectionEnd();
        SuggestionSpan[] suggestionSpanArr = (SuggestionSpan[]) spannable.getSpans(selectionStart, selectionEnd, SuggestionSpan.class);
        if (suggestionSpanArr.length == 0) {
            return false;
        }
        if (selectionStart == selectionEnd) {
            for (SuggestionSpan suggestionSpan : suggestionSpanArr) {
                if (suggestionSpan.getSuggestions().length > 0) {
                    return true;
                }
            }
            return false;
        }
        int length = this.mTextView.getText().length();
        int iMax = 0;
        boolean z = false;
        int iMax2 = 0;
        int length2 = this.mTextView.getText().length();
        int iMin = length;
        for (int i = 0; i < suggestionSpanArr.length; i++) {
            int spanStart = spannable.getSpanStart(suggestionSpanArr[i]);
            int spanEnd = spannable.getSpanEnd(suggestionSpanArr[i]);
            iMin = Math.min(iMin, spanStart);
            iMax = Math.max(iMax, spanEnd);
            if (selectionStart >= spanStart && selectionStart <= spanEnd) {
                z = z || suggestionSpanArr[i].getSuggestions().length > 0;
                length2 = Math.min(length2, spanStart);
                iMax2 = Math.max(iMax2, spanEnd);
            }
        }
        return z && length2 < iMax2 && iMin >= length2 && iMax <= iMax2;
    }

    private boolean isCursorInsideEasyCorrectionSpan() {
        for (SuggestionSpan suggestionSpan : (SuggestionSpan[]) ((Spannable) this.mTextView.getText()).getSpans(this.mTextView.getSelectionStart(), this.mTextView.getSelectionEnd(), SuggestionSpan.class)) {
            if ((suggestionSpan.getFlags() & 1) != 0) {
                return true;
            }
        }
        return false;
    }

    void onTouchUpEvent(MotionEvent motionEvent) {
        boolean z;
        if (getSelectionActionModeHelper().resetSelection(getTextView().getOffsetForPosition(motionEvent.getX(), motionEvent.getY()))) {
            return;
        }
        if (!this.mSelectAllOnFocus || !this.mTextView.didTouchFocusSelect()) {
            z = false;
        } else {
            z = true;
        }
        hideCursorAndSpanControllers();
        stopTextActionMode();
        CharSequence text = this.mTextView.getText();
        if (!z && text.length() > 0) {
            int offsetForPosition = this.mTextView.getOffsetForPosition(motionEvent.getX(), motionEvent.getY());
            boolean z2 = !this.mRequestingLinkActionMode;
            if (z2) {
                Selection.setSelection((Spannable) text, offsetForPosition);
                if (this.mSpellChecker != null) {
                    this.mSpellChecker.onSelectionChanged();
                }
            }
            if (!extractedTextModeWillBeStarted()) {
                if (isCursorInsideEasyCorrectionSpan()) {
                    if (this.mInsertionActionModeRunnable != null) {
                        this.mTextView.removeCallbacks(this.mInsertionActionModeRunnable);
                    }
                    this.mShowSuggestionRunnable = new Runnable() {
                        @Override
                        public final void run() {
                            this.f$0.replace();
                        }
                    };
                    this.mTextView.postDelayed(this.mShowSuggestionRunnable, ViewConfiguration.getDoubleTapTimeout());
                    return;
                }
                if (hasInsertionController()) {
                    if (z2) {
                        getInsertionController().show();
                    } else {
                        getInsertionController().hide();
                    }
                }
            }
        }
    }

    protected void stopTextActionMode() {
        if (this.mTextActionMode != null) {
            this.mTextActionMode.finish();
        }
    }

    private void stopTextActionModeWithPreservingSelection() {
        if (this.mTextActionMode != null) {
            this.mRestartActionModeOnNextRefresh = true;
        }
        this.mPreserveSelection = true;
        stopTextActionMode();
        this.mPreserveSelection = false;
    }

    boolean hasInsertionController() {
        return this.mInsertionControllerEnabled;
    }

    boolean hasSelectionController() {
        return this.mSelectionControllerEnabled;
    }

    private InsertionPointCursorController getInsertionController() {
        if (!this.mInsertionControllerEnabled) {
            return null;
        }
        if (this.mInsertionPointCursorController == null) {
            this.mInsertionPointCursorController = new InsertionPointCursorController();
            this.mTextView.getViewTreeObserver().addOnTouchModeChangeListener(this.mInsertionPointCursorController);
        }
        return this.mInsertionPointCursorController;
    }

    SelectionModifierCursorController getSelectionController() {
        if (!this.mSelectionControllerEnabled) {
            return null;
        }
        if (this.mSelectionModifierCursorController == null) {
            this.mSelectionModifierCursorController = new SelectionModifierCursorController();
            this.mTextView.getViewTreeObserver().addOnTouchModeChangeListener(this.mSelectionModifierCursorController);
        }
        return this.mSelectionModifierCursorController;
    }

    @VisibleForTesting
    public Drawable getCursorDrawable() {
        return this.mDrawableForCursor;
    }

    private void updateCursorPosition(int i, int i2, float f) {
        if (this.mDrawableForCursor == null) {
            this.mDrawableForCursor = this.mTextView.getContext().getDrawable(this.mTextView.mCursorDrawableRes);
        }
        int iClampHorizontalPosition = clampHorizontalPosition(this.mDrawableForCursor, f);
        this.mDrawableForCursor.setBounds(iClampHorizontalPosition, i - this.mTempRect.top, this.mDrawableForCursor.getIntrinsicWidth() + iClampHorizontalPosition, i2 + this.mTempRect.bottom);
    }

    private int clampHorizontalPosition(Drawable drawable, float f) {
        float fMax = Math.max(LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS, f - LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS);
        if (this.mTempRect == null) {
            this.mTempRect = new Rect();
        }
        int intrinsicWidth = 0;
        if (drawable != null) {
            drawable.getPadding(this.mTempRect);
            intrinsicWidth = drawable.getIntrinsicWidth();
        } else {
            this.mTempRect.setEmpty();
        }
        int scrollX = this.mTextView.getScrollX();
        float f2 = fMax - scrollX;
        int width = (this.mTextView.getWidth() - this.mTextView.getCompoundPaddingLeft()) - this.mTextView.getCompoundPaddingRight();
        float f3 = width;
        if (f2 >= f3 - 1.0f) {
            return (width + scrollX) - (intrinsicWidth - this.mTempRect.right);
        }
        if (Math.abs(f2) <= 1.0f || (TextUtils.isEmpty(this.mTextView.getText()) && 1048576 - scrollX <= f3 + 1.0f && fMax <= 1.0f)) {
            return scrollX - this.mTempRect.left;
        }
        return ((int) fMax) - this.mTempRect.left;
    }

    public void onCommitCorrection(CorrectionInfo correctionInfo) {
        if (this.mCorrectionHighlighter == null) {
            this.mCorrectionHighlighter = new CorrectionHighlighter();
        } else {
            this.mCorrectionHighlighter.invalidate(false);
        }
        this.mCorrectionHighlighter.highlight(correctionInfo);
        this.mUndoInputFilter.freezeLastEdit();
    }

    void onScrollChanged() {
        if (this.mPositionListener != null) {
            this.mPositionListener.onScrollChanged();
        }
        if (this.mTextActionMode != null) {
            this.mTextActionMode.invalidateContentRect();
        }
    }

    private boolean shouldBlink() {
        int selectionStart;
        int selectionEnd;
        return isCursorVisible() && this.mTextView.isFocused() && (selectionStart = this.mTextView.getSelectionStart()) >= 0 && (selectionEnd = this.mTextView.getSelectionEnd()) >= 0 && selectionStart == selectionEnd;
    }

    void makeBlink() {
        if (shouldBlink()) {
            this.mShowCursor = SystemClock.uptimeMillis();
            if (this.mBlink == null) {
                this.mBlink = new Blink();
            }
            this.mTextView.removeCallbacks(this.mBlink);
            this.mTextView.postDelayed(this.mBlink, 500L);
            return;
        }
        if (this.mBlink != null) {
            this.mTextView.removeCallbacks(this.mBlink);
        }
    }

    private class Blink implements Runnable {
        private boolean mCancelled;

        private Blink() {
        }

        @Override
        public void run() {
            if (!this.mCancelled) {
                Editor.this.mTextView.removeCallbacks(this);
                if (Editor.this.shouldBlink()) {
                    if (Editor.this.mTextView.getLayout() != null) {
                        Editor.this.mTextView.invalidateCursorPath();
                    }
                    Editor.this.mTextView.postDelayed(this, 500L);
                }
            }
        }

        void cancel() {
            if (!this.mCancelled) {
                Editor.this.mTextView.removeCallbacks(this);
                this.mCancelled = true;
            }
        }

        void uncancel() {
            this.mCancelled = false;
        }
    }

    private View.DragShadowBuilder getTextThumbnailBuilder(int i, int i2) {
        TextView textView = (TextView) View.inflate(this.mTextView.getContext(), R.layout.text_drag_thumbnail, null);
        if (textView == null) {
            throw new IllegalArgumentException("Unable to inflate text drag thumbnail");
        }
        if (i2 - i > 20) {
            i2 = TextUtils.unpackRangeEndFromLong(getCharClusterRange(i + 20));
        }
        textView.setText(this.mTextView.getTransformedText(i, i2));
        textView.setTextColor(this.mTextView.getTextColors());
        textView.setTextAppearance(16);
        textView.setGravity(17);
        textView.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        textView.measure(iMakeMeasureSpec, iMakeMeasureSpec);
        textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
        textView.invalidate();
        return new View.DragShadowBuilder(textView);
    }

    private static class DragLocalState {
        public int end;
        public TextView sourceTextView;
        public int start;

        public DragLocalState(TextView textView, int i, int i2) {
            this.sourceTextView = textView;
            this.start = i;
            this.end = i2;
        }
    }

    void onDrop(DragEvent dragEvent) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        DragAndDropPermissions dragAndDropPermissionsObtain = DragAndDropPermissions.obtain(dragEvent);
        if (dragAndDropPermissionsObtain != null) {
            dragAndDropPermissionsObtain.takeTransient();
        }
        try {
            ClipData clipData = dragEvent.getClipData();
            int itemCount = clipData.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                spannableStringBuilder.append(clipData.getItemAt(i).coerceToStyledText(this.mTextView.getContext()));
            }
            this.mTextView.beginBatchEdit();
            this.mUndoInputFilter.freezeLastEdit();
            try {
                int offsetForPosition = this.mTextView.getOffsetForPosition(dragEvent.getX(), dragEvent.getY());
                Object localState = dragEvent.getLocalState();
                DragLocalState dragLocalState = null;
                if (localState instanceof DragLocalState) {
                    dragLocalState = (DragLocalState) localState;
                }
                boolean z = dragLocalState != null && dragLocalState.sourceTextView == this.mTextView;
                if (z && offsetForPosition >= dragLocalState.start && offsetForPosition < dragLocalState.end) {
                    return;
                }
                int length = this.mTextView.getText().length();
                Selection.setSelection((Spannable) this.mTextView.getText(), offsetForPosition);
                this.mTextView.replaceText_internal(offsetForPosition, offsetForPosition, spannableStringBuilder);
                if (z) {
                    int i2 = dragLocalState.start;
                    int i3 = dragLocalState.end;
                    if (offsetForPosition <= i2) {
                        int length2 = this.mTextView.getText().length() - length;
                        i2 += length2;
                        i3 += length2;
                    }
                    this.mTextView.deleteText_internal(i2, i3);
                    int iMax = Math.max(0, i2 - 1);
                    int iMin = Math.min(this.mTextView.getText().length(), i2 + 1);
                    int i4 = iMax + 1;
                    if (iMin > i4) {
                        CharSequence transformedText = this.mTextView.getTransformedText(iMax, iMin);
                        if (Character.isSpaceChar(transformedText.charAt(0)) && Character.isSpaceChar(transformedText.charAt(1))) {
                            this.mTextView.deleteText_internal(iMax, i4);
                        }
                    }
                }
            } finally {
                this.mTextView.endBatchEdit();
                this.mUndoInputFilter.freezeLastEdit();
            }
        } finally {
            if (dragAndDropPermissionsObtain != null) {
                dragAndDropPermissionsObtain.release();
            }
        }
    }

    public void addSpanWatchers(Spannable spannable) {
        int length = spannable.length();
        if (this.mKeyListener != null) {
            spannable.setSpan(this.mKeyListener, 0, length, 18);
        }
        if (this.mSpanController == null) {
            this.mSpanController = new SpanController();
        }
        spannable.setSpan(this.mSpanController, 0, length, 18);
    }

    void setContextMenuAnchor(float f, float f2) {
        this.mContextMenuAnchorX = f;
        this.mContextMenuAnchorY = f2;
    }

    void onCreateContextMenu(ContextMenu contextMenu) {
        int offsetForPosition;
        if (this.mIsBeingLongClicked || Float.isNaN(this.mContextMenuAnchorX) || Float.isNaN(this.mContextMenuAnchorY) || (offsetForPosition = this.mTextView.getOffsetForPosition(this.mContextMenuAnchorX, this.mContextMenuAnchorY)) == -1) {
            return;
        }
        stopTextActionModeWithPreservingSelection();
        if (this.mTextView.canSelectText()) {
            if (!(this.mTextView.hasSelection() && offsetForPosition >= this.mTextView.getSelectionStart() && offsetForPosition <= this.mTextView.getSelectionEnd())) {
                Selection.setSelection((Spannable) this.mTextView.getText(), offsetForPosition);
                stopTextActionMode();
            }
        }
        if (shouldOfferToShowSuggestions()) {
            SuggestionInfo[] suggestionInfoArr = new SuggestionInfo[5];
            int i = 0;
            while (true) {
                if (i >= suggestionInfoArr.length) {
                    break;
                }
                suggestionInfoArr[i] = new SuggestionInfo();
                i++;
            }
            SubMenu subMenuAddSubMenu = contextMenu.addSubMenu(0, 0, 9, R.string.replace);
            int suggestionInfo = this.mSuggestionHelper.getSuggestionInfo(suggestionInfoArr, null);
            for (int i2 = 0; i2 < suggestionInfo; i2++) {
                final SuggestionInfo suggestionInfo2 = suggestionInfoArr[i2];
                subMenuAddSubMenu.add(0, 0, i2, suggestionInfo2.mText).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Editor.this.replaceWithSuggestion(suggestionInfo2);
                        return true;
                    }
                });
            }
        }
        contextMenu.add(0, 16908338, 2, R.string.undo).setAlphabeticShortcut(DateFormat.TIME_ZONE).setOnMenuItemClickListener(this.mOnContextMenuItemClickListener).setEnabled(this.mTextView.canUndo());
        contextMenu.add(0, 16908339, 3, R.string.redo).setOnMenuItemClickListener(this.mOnContextMenuItemClickListener).setEnabled(this.mTextView.canRedo());
        contextMenu.add(0, 16908320, 4, 17039363).setAlphabeticShortcut(EpicenterTranslateClipReveal.StateProperty.TARGET_X).setOnMenuItemClickListener(this.mOnContextMenuItemClickListener).setEnabled(this.mTextView.canCut());
        contextMenu.add(0, 16908321, 5, 17039361).setAlphabeticShortcut('c').setOnMenuItemClickListener(this.mOnContextMenuItemClickListener).setEnabled(this.mTextView.canCopy());
        contextMenu.add(0, 16908322, 6, 17039371).setAlphabeticShortcut('v').setEnabled(this.mTextView.canPaste()).setOnMenuItemClickListener(this.mOnContextMenuItemClickListener);
        contextMenu.add(0, 16908337, 11, 17039385).setEnabled(this.mTextView.canPasteAsPlainText()).setOnMenuItemClickListener(this.mOnContextMenuItemClickListener);
        contextMenu.add(0, 16908341, 7, R.string.share).setEnabled(this.mTextView.canShare()).setOnMenuItemClickListener(this.mOnContextMenuItemClickListener);
        contextMenu.add(0, 16908319, 8, 17039373).setAlphabeticShortcut(DateFormat.AM_PM).setEnabled(this.mTextView.canSelectAllText()).setOnMenuItemClickListener(this.mOnContextMenuItemClickListener);
        contextMenu.add(0, 16908355, 10, 17039386).setEnabled(this.mTextView.canRequestAutofill()).setOnMenuItemClickListener(this.mOnContextMenuItemClickListener);
        this.mPreserveSelection = true;
    }

    private SuggestionSpan findEquivalentSuggestionSpan(SuggestionSpanInfo suggestionSpanInfo) {
        Editable editable = (Editable) this.mTextView.getText();
        if (editable.getSpanStart(suggestionSpanInfo.mSuggestionSpan) >= 0) {
            return suggestionSpanInfo.mSuggestionSpan;
        }
        for (SuggestionSpan suggestionSpan : (SuggestionSpan[]) editable.getSpans(suggestionSpanInfo.mSpanStart, suggestionSpanInfo.mSpanEnd, SuggestionSpan.class)) {
            if (editable.getSpanStart(suggestionSpan) == suggestionSpanInfo.mSpanStart && editable.getSpanEnd(suggestionSpan) == suggestionSpanInfo.mSpanEnd && suggestionSpan.equals(suggestionSpanInfo.mSuggestionSpan)) {
                return suggestionSpan;
            }
        }
        return null;
    }

    private void replaceWithSuggestion(SuggestionInfo suggestionInfo) {
        SuggestionSpan suggestionSpanFindEquivalentSuggestionSpan = findEquivalentSuggestionSpan(suggestionInfo.mSuggestionSpanInfo);
        if (suggestionSpanFindEquivalentSuggestionSpan == null) {
            return;
        }
        Editable editable = (Editable) this.mTextView.getText();
        int spanStart = editable.getSpanStart(suggestionSpanFindEquivalentSuggestionSpan);
        int spanEnd = editable.getSpanEnd(suggestionSpanFindEquivalentSuggestionSpan);
        if (spanStart < 0 || spanEnd <= spanStart) {
            return;
        }
        String strSubstring = TextUtils.substring(editable, spanStart, spanEnd);
        SuggestionSpan[] suggestionSpanArr = (SuggestionSpan[]) editable.getSpans(spanStart, spanEnd, SuggestionSpan.class);
        int length = suggestionSpanArr.length;
        int[] iArr = new int[length];
        int[] iArr2 = new int[length];
        int[] iArr3 = new int[length];
        for (int i = 0; i < length; i++) {
            SuggestionSpan suggestionSpan = suggestionSpanArr[i];
            iArr[i] = editable.getSpanStart(suggestionSpan);
            iArr2[i] = editable.getSpanEnd(suggestionSpan);
            iArr3[i] = editable.getSpanFlags(suggestionSpan);
            int flags = suggestionSpan.getFlags();
            if ((flags & 2) != 0) {
                suggestionSpan.setFlags(flags & (-3) & (-2));
            }
        }
        suggestionSpanFindEquivalentSuggestionSpan.notifySelection(this.mTextView.getContext(), strSubstring, suggestionInfo.mSuggestionIndex);
        String string = suggestionInfo.mText.subSequence(suggestionInfo.mSuggestionStart, suggestionInfo.mSuggestionEnd).toString();
        this.mTextView.replaceText_internal(spanStart, spanEnd, string);
        suggestionSpanFindEquivalentSuggestionSpan.getSuggestions()[suggestionInfo.mSuggestionIndex] = strSubstring;
        int length2 = string.length() - (spanEnd - spanStart);
        for (int i2 = 0; i2 < length; i2++) {
            if (iArr[i2] <= spanStart && iArr2[i2] >= spanEnd) {
                this.mTextView.setSpan_internal(suggestionSpanArr[i2], iArr[i2], iArr2[i2] + length2, iArr3[i2]);
            }
        }
        int i3 = spanEnd + length2;
        this.mTextView.setCursorPosition_internal(i3, i3);
    }

    private class SpanController implements SpanWatcher {
        private static final int DISPLAY_TIMEOUT_MS = 3000;
        private Runnable mHidePopup;
        private EasyEditPopupWindow mPopupWindow;

        private SpanController() {
        }

        private boolean isNonIntermediateSelectionSpan(Spannable spannable, Object obj) {
            return (Selection.SELECTION_START == obj || Selection.SELECTION_END == obj) && (spannable.getSpanFlags(obj) & 512) == 0;
        }

        @Override
        public void onSpanAdded(Spannable spannable, Object obj, int i, int i2) {
            if (isNonIntermediateSelectionSpan(spannable, obj)) {
                Editor.this.sendUpdateSelection();
                return;
            }
            if (obj instanceof EasyEditSpan) {
                if (this.mPopupWindow == null) {
                    this.mPopupWindow = new EasyEditPopupWindow();
                    this.mHidePopup = new Runnable() {
                        @Override
                        public void run() {
                            SpanController.this.hide();
                        }
                    };
                }
                if (this.mPopupWindow.mEasyEditSpan != null) {
                    this.mPopupWindow.mEasyEditSpan.setDeleteEnabled(false);
                }
                this.mPopupWindow.setEasyEditSpan((EasyEditSpan) obj);
                this.mPopupWindow.setOnDeleteListener(new EasyEditDeleteListener() {
                    @Override
                    public void onDeleteClick(EasyEditSpan easyEditSpan) {
                        Editable editable = (Editable) Editor.this.mTextView.getText();
                        int spanStart = editable.getSpanStart(easyEditSpan);
                        int spanEnd = editable.getSpanEnd(easyEditSpan);
                        if (spanStart >= 0 && spanEnd >= 0) {
                            SpanController.this.sendEasySpanNotification(1, easyEditSpan);
                            Editor.this.mTextView.deleteText_internal(spanStart, spanEnd);
                        }
                        editable.removeSpan(easyEditSpan);
                    }
                });
                if (Editor.this.mTextView.getWindowVisibility() != 0 || Editor.this.mTextView.getLayout() == null || Editor.this.extractedTextModeWillBeStarted()) {
                    return;
                }
                this.mPopupWindow.show();
                Editor.this.mTextView.removeCallbacks(this.mHidePopup);
                Editor.this.mTextView.postDelayed(this.mHidePopup, 3000L);
            }
        }

        @Override
        public void onSpanRemoved(Spannable spannable, Object obj, int i, int i2) {
            if (isNonIntermediateSelectionSpan(spannable, obj)) {
                Editor.this.sendUpdateSelection();
            } else if (this.mPopupWindow != null && obj == this.mPopupWindow.mEasyEditSpan) {
                hide();
            }
        }

        @Override
        public void onSpanChanged(Spannable spannable, Object obj, int i, int i2, int i3, int i4) {
            if (isNonIntermediateSelectionSpan(spannable, obj)) {
                Editor.this.sendUpdateSelection();
            } else if (this.mPopupWindow != null && (obj instanceof EasyEditSpan)) {
                EasyEditSpan easyEditSpan = (EasyEditSpan) obj;
                sendEasySpanNotification(2, easyEditSpan);
                spannable.removeSpan(easyEditSpan);
            }
        }

        public void hide() {
            if (this.mPopupWindow != null) {
                this.mPopupWindow.hide();
                Editor.this.mTextView.removeCallbacks(this.mHidePopup);
            }
        }

        private void sendEasySpanNotification(int i, EasyEditSpan easyEditSpan) {
            try {
                PendingIntent pendingIntent = easyEditSpan.getPendingIntent();
                if (pendingIntent != null) {
                    Intent intent = new Intent();
                    intent.putExtra(EasyEditSpan.EXTRA_TEXT_CHANGED_TYPE, i);
                    pendingIntent.send(Editor.this.mTextView.getContext(), 0, intent);
                }
            } catch (PendingIntent.CanceledException e) {
                Log.w("Editor", "PendingIntent for notification cannot be sent", e);
            }
        }
    }

    private class EasyEditPopupWindow extends PinnedPopupWindow implements View.OnClickListener {
        private static final int POPUP_TEXT_LAYOUT = 17367299;
        private TextView mDeleteTextView;
        private EasyEditSpan mEasyEditSpan;
        private EasyEditDeleteListener mOnDeleteListener;

        private EasyEditPopupWindow() {
            super();
        }

        @Override
        protected void createPopupWindow() {
            this.mPopupWindow = new PopupWindow(Editor.this.mTextView.getContext(), (AttributeSet) null, 16843464);
            this.mPopupWindow.setInputMethodMode(2);
            this.mPopupWindow.setClippingEnabled(true);
        }

        @Override
        protected void initContentView() {
            LinearLayout linearLayout = new LinearLayout(Editor.this.mTextView.getContext());
            linearLayout.setOrientation(0);
            this.mContentView = linearLayout;
            this.mContentView.setBackgroundResource(R.drawable.text_edit_side_paste_window);
            LayoutInflater layoutInflater = (LayoutInflater) Editor.this.mTextView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-2, -2);
            this.mDeleteTextView = (TextView) layoutInflater.inflate(17367299, (ViewGroup) null);
            this.mDeleteTextView.setLayoutParams(layoutParams);
            this.mDeleteTextView.setText(R.string.delete);
            this.mDeleteTextView.setOnClickListener(this);
            this.mContentView.addView(this.mDeleteTextView);
        }

        public void setEasyEditSpan(EasyEditSpan easyEditSpan) {
            this.mEasyEditSpan = easyEditSpan;
        }

        private void setOnDeleteListener(EasyEditDeleteListener easyEditDeleteListener) {
            this.mOnDeleteListener = easyEditDeleteListener;
        }

        @Override
        public void onClick(View view) {
            if (view == this.mDeleteTextView && this.mEasyEditSpan != null && this.mEasyEditSpan.isDeleteEnabled() && this.mOnDeleteListener != null) {
                this.mOnDeleteListener.onDeleteClick(this.mEasyEditSpan);
            }
        }

        @Override
        public void hide() {
            if (this.mEasyEditSpan != null) {
                this.mEasyEditSpan.setDeleteEnabled(false);
            }
            this.mOnDeleteListener = null;
            super.hide();
        }

        @Override
        protected int getTextOffset() {
            return ((Editable) Editor.this.mTextView.getText()).getSpanEnd(this.mEasyEditSpan);
        }

        @Override
        protected int getVerticalLocalPosition(int i) {
            return Editor.this.mTextView.getLayout().getLineBottomWithoutSpacing(i);
        }

        @Override
        protected int clipVertically(int i) {
            return i;
        }
    }

    private class PositionListener implements ViewTreeObserver.OnPreDrawListener {
        private static final int MAXIMUM_NUMBER_OF_LISTENERS = 7;
        private boolean[] mCanMove;
        private int mNumberOfListeners;
        private boolean mPositionHasChanged;
        private TextViewPositionListener[] mPositionListeners;
        private int mPositionX;
        private int mPositionXOnScreen;
        private int mPositionY;
        private int mPositionYOnScreen;
        private boolean mScrollHasChanged;
        final int[] mTempCoords;

        private PositionListener() {
            this.mPositionListeners = new TextViewPositionListener[7];
            this.mCanMove = new boolean[7];
            this.mPositionHasChanged = true;
            this.mTempCoords = new int[2];
        }

        public void addSubscriber(TextViewPositionListener textViewPositionListener, boolean z) {
            if (this.mNumberOfListeners == 0) {
                updatePosition();
                Editor.this.mTextView.getViewTreeObserver().addOnPreDrawListener(this);
            }
            int i = -1;
            for (int i2 = 0; i2 < 7; i2++) {
                TextViewPositionListener textViewPositionListener2 = this.mPositionListeners[i2];
                if (textViewPositionListener2 == textViewPositionListener) {
                    return;
                }
                if (i < 0 && textViewPositionListener2 == null) {
                    i = i2;
                }
            }
            this.mPositionListeners[i] = textViewPositionListener;
            this.mCanMove[i] = z;
            this.mNumberOfListeners++;
        }

        public void removeSubscriber(TextViewPositionListener textViewPositionListener) {
            int i = 0;
            while (true) {
                if (i >= 7) {
                    break;
                }
                if (this.mPositionListeners[i] != textViewPositionListener) {
                    i++;
                } else {
                    this.mPositionListeners[i] = null;
                    this.mNumberOfListeners--;
                    break;
                }
            }
            if (this.mNumberOfListeners == 0) {
                Editor.this.mTextView.getViewTreeObserver().removeOnPreDrawListener(this);
            }
        }

        public int getPositionX() {
            return this.mPositionX;
        }

        public int getPositionY() {
            return this.mPositionY;
        }

        public int getPositionXOnScreen() {
            return this.mPositionXOnScreen;
        }

        public int getPositionYOnScreen() {
            return this.mPositionYOnScreen;
        }

        @Override
        public boolean onPreDraw() {
            TextViewPositionListener textViewPositionListener;
            updatePosition();
            for (int i = 0; i < 7; i++) {
                if ((this.mPositionHasChanged || this.mScrollHasChanged || this.mCanMove[i]) && (textViewPositionListener = this.mPositionListeners[i]) != null) {
                    textViewPositionListener.updatePosition(this.mPositionX, this.mPositionY, this.mPositionHasChanged, this.mScrollHasChanged);
                }
            }
            this.mScrollHasChanged = false;
            return true;
        }

        private void updatePosition() {
            Editor.this.mTextView.getLocationInWindow(this.mTempCoords);
            this.mPositionHasChanged = (this.mTempCoords[0] == this.mPositionX && this.mTempCoords[1] == this.mPositionY) ? false : true;
            this.mPositionX = this.mTempCoords[0];
            this.mPositionY = this.mTempCoords[1];
            Editor.this.mTextView.getLocationOnScreen(this.mTempCoords);
            this.mPositionXOnScreen = this.mTempCoords[0];
            this.mPositionYOnScreen = this.mTempCoords[1];
        }

        public void onScrollChanged() {
            this.mScrollHasChanged = true;
        }
    }

    private abstract class PinnedPopupWindow implements TextViewPositionListener {
        int mClippingLimitLeft;
        int mClippingLimitRight;
        protected ViewGroup mContentView;
        protected PopupWindow mPopupWindow;
        int mPositionX;
        int mPositionY;

        protected abstract int clipVertically(int i);

        protected abstract void createPopupWindow();

        protected abstract int getTextOffset();

        protected abstract int getVerticalLocalPosition(int i);

        protected abstract void initContentView();

        protected void setUp() {
        }

        public PinnedPopupWindow() {
            setUp();
            createPopupWindow();
            this.mPopupWindow.setWindowLayoutType(1005);
            this.mPopupWindow.setWidth(-2);
            this.mPopupWindow.setHeight(-2);
            initContentView();
            this.mContentView.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
            this.mPopupWindow.setContentView(this.mContentView);
        }

        public void show() {
            Editor.this.getPositionListener().addSubscriber(this, false);
            computeLocalPosition();
            PositionListener positionListener = Editor.this.getPositionListener();
            updatePosition(positionListener.getPositionX(), positionListener.getPositionY());
        }

        protected void measureContent() {
            DisplayMetrics displayMetrics = Editor.this.mTextView.getResources().getDisplayMetrics();
            this.mContentView.measure(View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, Integer.MIN_VALUE));
        }

        private void computeLocalPosition() {
            measureContent();
            int measuredWidth = this.mContentView.getMeasuredWidth();
            int textOffset = getTextOffset();
            this.mPositionX = (int) (Editor.this.mTextView.getLayout().getPrimaryHorizontal(textOffset) - (measuredWidth / 2.0f));
            this.mPositionX += Editor.this.mTextView.viewportToContentHorizontalOffset();
            this.mPositionY = getVerticalLocalPosition(Editor.this.mTextView.getLayout().getLineForOffset(textOffset));
            this.mPositionY += Editor.this.mTextView.viewportToContentVerticalOffset();
        }

        private void updatePosition(int i, int i2) {
            int i3 = i + this.mPositionX;
            int iClipVertically = clipVertically(i2 + this.mPositionY);
            DisplayMetrics displayMetrics = Editor.this.mTextView.getResources().getDisplayMetrics();
            int iMax = Math.max(-this.mClippingLimitLeft, Math.min((displayMetrics.widthPixels - this.mContentView.getMeasuredWidth()) + this.mClippingLimitRight, i3));
            if (!isShowing()) {
                this.mPopupWindow.showAtLocation(Editor.this.mTextView, 0, iMax, iClipVertically);
            } else {
                this.mPopupWindow.update(iMax, iClipVertically, -1, -1);
            }
        }

        public void hide() {
            if (!isShowing()) {
                return;
            }
            this.mPopupWindow.dismiss();
            Editor.this.getPositionListener().removeSubscriber(this);
        }

        @Override
        public void updatePosition(int i, int i2, boolean z, boolean z2) {
            if (isShowing() && Editor.this.isOffsetVisible(getTextOffset())) {
                if (z2) {
                    computeLocalPosition();
                }
                updatePosition(i, i2);
                return;
            }
            hide();
        }

        public boolean isShowing() {
            return this.mPopupWindow.isShowing();
        }
    }

    private static final class SuggestionInfo {
        int mSuggestionEnd;
        int mSuggestionIndex;
        final SuggestionSpanInfo mSuggestionSpanInfo;
        int mSuggestionStart;
        final SpannableStringBuilder mText;

        private SuggestionInfo() {
            this.mSuggestionSpanInfo = new SuggestionSpanInfo();
            this.mText = new SpannableStringBuilder();
        }

        void clear() {
            this.mSuggestionSpanInfo.clear();
            this.mText.clear();
        }

        void setSpanInfo(SuggestionSpan suggestionSpan, int i, int i2) {
            this.mSuggestionSpanInfo.mSuggestionSpan = suggestionSpan;
            this.mSuggestionSpanInfo.mSpanStart = i;
            this.mSuggestionSpanInfo.mSpanEnd = i2;
        }
    }

    private static final class SuggestionSpanInfo {
        int mSpanEnd;
        int mSpanStart;
        SuggestionSpan mSuggestionSpan;

        private SuggestionSpanInfo() {
        }

        void clear() {
            this.mSuggestionSpan = null;
        }
    }

    private class SuggestionHelper {
        private final HashMap<SuggestionSpan, Integer> mSpansLengths;
        private final Comparator<SuggestionSpan> mSuggestionSpanComparator;

        private SuggestionHelper() {
            this.mSuggestionSpanComparator = new SuggestionSpanComparator();
            this.mSpansLengths = new HashMap<>();
        }

        private class SuggestionSpanComparator implements Comparator<SuggestionSpan> {
            private SuggestionSpanComparator() {
            }

            @Override
            public int compare(SuggestionSpan suggestionSpan, SuggestionSpan suggestionSpan2) {
                int flags = suggestionSpan.getFlags();
                int flags2 = suggestionSpan2.getFlags();
                if (flags != flags2) {
                    boolean z = (flags & 1) != 0;
                    boolean z2 = (flags2 & 1) != 0;
                    boolean z3 = (flags & 2) != 0;
                    boolean z4 = (flags2 & 2) != 0;
                    if (z && !z3) {
                        return -1;
                    }
                    if (z2 && !z4) {
                        return 1;
                    }
                    if (z3) {
                        return -1;
                    }
                    if (z4) {
                        return 1;
                    }
                }
                return ((Integer) SuggestionHelper.this.mSpansLengths.get(suggestionSpan)).intValue() - ((Integer) SuggestionHelper.this.mSpansLengths.get(suggestionSpan2)).intValue();
            }
        }

        private SuggestionSpan[] getSortedSuggestionSpans() {
            int selectionStart = Editor.this.mTextView.getSelectionStart();
            Spannable spannable = (Spannable) Editor.this.mTextView.getText();
            SuggestionSpan[] suggestionSpanArr = (SuggestionSpan[]) spannable.getSpans(selectionStart, selectionStart, SuggestionSpan.class);
            this.mSpansLengths.clear();
            for (SuggestionSpan suggestionSpan : suggestionSpanArr) {
                this.mSpansLengths.put(suggestionSpan, Integer.valueOf(spannable.getSpanEnd(suggestionSpan) - spannable.getSpanStart(suggestionSpan)));
            }
            Arrays.sort(suggestionSpanArr, this.mSuggestionSpanComparator);
            this.mSpansLengths.clear();
            return suggestionSpanArr;
        }

        public int getSuggestionInfo(SuggestionInfo[] suggestionInfoArr, SuggestionSpanInfo suggestionSpanInfo) {
            SuggestionSpanInfo suggestionSpanInfo2 = suggestionSpanInfo;
            Spannable spannable = (Spannable) Editor.this.mTextView.getText();
            SuggestionSpan[] sortedSuggestionSpans = getSortedSuggestionSpans();
            int i = 0;
            if (sortedSuggestionSpans.length == 0) {
                return 0;
            }
            int length = sortedSuggestionSpans.length;
            int i2 = 0;
            int i3 = 0;
            while (i2 < length) {
                SuggestionSpan suggestionSpan = sortedSuggestionSpans[i2];
                int spanStart = spannable.getSpanStart(suggestionSpan);
                int spanEnd = spannable.getSpanEnd(suggestionSpan);
                if (suggestionSpanInfo2 != null && (suggestionSpan.getFlags() & 2) != 0) {
                    suggestionSpanInfo2.mSuggestionSpan = suggestionSpan;
                    suggestionSpanInfo2.mSpanStart = spanStart;
                    suggestionSpanInfo2.mSpanEnd = spanEnd;
                }
                String[] suggestions = suggestionSpan.getSuggestions();
                int length2 = suggestions.length;
                int i4 = i3;
                for (int i5 = i; i5 < length2; i5++) {
                    String str = suggestions[i5];
                    int i6 = i;
                    while (true) {
                        if (i6 < i4) {
                            SuggestionInfo suggestionInfo = suggestionInfoArr[i6];
                            if (suggestionInfo.mText.toString().equals(str)) {
                                int i7 = suggestionInfo.mSuggestionSpanInfo.mSpanStart;
                                int i8 = suggestionInfo.mSuggestionSpanInfo.mSpanEnd;
                                if (spanStart == i7 && spanEnd == i8) {
                                    i = 0;
                                    break;
                                }
                            }
                            i6++;
                        } else {
                            SuggestionInfo suggestionInfo2 = suggestionInfoArr[i4];
                            suggestionInfo2.setSpanInfo(suggestionSpan, spanStart, spanEnd);
                            suggestionInfo2.mSuggestionIndex = i5;
                            i = 0;
                            suggestionInfo2.mSuggestionStart = 0;
                            suggestionInfo2.mSuggestionEnd = str.length();
                            suggestionInfo2.mText.replace(0, suggestionInfo2.mText.length(), (CharSequence) str);
                            i4++;
                            if (i4 >= suggestionInfoArr.length) {
                                return i4;
                            }
                        }
                    }
                }
                i2++;
                i3 = i4;
                suggestionSpanInfo2 = suggestionSpanInfo;
            }
            return i3;
        }
    }

    @VisibleForTesting
    public class SuggestionsPopupWindow extends PinnedPopupWindow implements AdapterView.OnItemClickListener {
        private static final int MAX_NUMBER_SUGGESTIONS = 5;
        private static final String USER_DICTIONARY_EXTRA_LOCALE = "locale";
        private static final String USER_DICTIONARY_EXTRA_WORD = "word";
        private TextView mAddToDictionaryButton;
        private int mContainerMarginTop;
        private int mContainerMarginWidth;
        private LinearLayout mContainerView;
        private Context mContext;
        private boolean mCursorWasVisibleBeforeSuggestions;
        private TextView mDeleteButton;
        private TextAppearanceSpan mHighlightSpan;
        private boolean mIsShowingUp;
        private final SuggestionSpanInfo mMisspelledSpanInfo;
        private int mNumberOfSuggestions;
        private SuggestionInfo[] mSuggestionInfos;
        private ListView mSuggestionListView;
        private SuggestionAdapter mSuggestionsAdapter;

        @Override
        public void hide() {
            super.hide();
        }

        @Override
        public boolean isShowing() {
            return super.isShowing();
        }

        @Override
        public void updatePosition(int i, int i2, boolean z, boolean z2) {
            super.updatePosition(i, i2, z, z2);
        }

        private class CustomPopupWindow extends PopupWindow {
            private CustomPopupWindow() {
            }

            @Override
            public void dismiss() {
                if (!isShowing()) {
                    return;
                }
                super.dismiss();
                Editor.this.getPositionListener().removeSubscriber(SuggestionsPopupWindow.this);
                ((Spannable) Editor.this.mTextView.getText()).removeSpan(Editor.this.mSuggestionRangeSpan);
                Editor.this.mTextView.setCursorVisible(SuggestionsPopupWindow.this.mCursorWasVisibleBeforeSuggestions);
                if (Editor.this.hasInsertionController() && !Editor.this.extractedTextModeWillBeStarted()) {
                    Editor.this.getInsertionController().show();
                }
            }
        }

        public SuggestionsPopupWindow() {
            super();
            this.mIsShowingUp = false;
            this.mMisspelledSpanInfo = new SuggestionSpanInfo();
            this.mCursorWasVisibleBeforeSuggestions = Editor.this.mCursorVisible;
        }

        @Override
        protected void setUp() {
            this.mContext = applyDefaultTheme(Editor.this.mTextView.getContext());
            this.mHighlightSpan = new TextAppearanceSpan(this.mContext, Editor.this.mTextView.mTextEditSuggestionHighlightStyle);
        }

        private Context applyDefaultTheme(Context context) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{R.attr.isLightTheme});
            int i = typedArrayObtainStyledAttributes.getBoolean(0, true) ? 16974410 : 16974411;
            typedArrayObtainStyledAttributes.recycle();
            return new ContextThemeWrapper(context, i);
        }

        @Override
        protected void createPopupWindow() {
            this.mPopupWindow = new CustomPopupWindow();
            this.mPopupWindow.setInputMethodMode(2);
            this.mPopupWindow.setBackgroundDrawable(new ColorDrawable(0));
            this.mPopupWindow.setFocusable(true);
            this.mPopupWindow.setClippingEnabled(false);
        }

        @Override
        protected void initContentView() {
            this.mContentView = (ViewGroup) ((LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(Editor.this.mTextView.mTextEditSuggestionContainerLayout, (ViewGroup) null);
            this.mContainerView = (LinearLayout) this.mContentView.findViewById(R.id.suggestionWindowContainer);
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mContainerView.getLayoutParams();
            this.mContainerMarginWidth = marginLayoutParams.leftMargin + marginLayoutParams.rightMargin;
            this.mContainerMarginTop = marginLayoutParams.topMargin;
            this.mClippingLimitLeft = marginLayoutParams.leftMargin;
            this.mClippingLimitRight = marginLayoutParams.rightMargin;
            this.mSuggestionListView = (ListView) this.mContentView.findViewById(R.id.suggestionContainer);
            this.mSuggestionsAdapter = new SuggestionAdapter();
            this.mSuggestionListView.setAdapter((ListAdapter) this.mSuggestionsAdapter);
            this.mSuggestionListView.setOnItemClickListener(this);
            this.mSuggestionInfos = new SuggestionInfo[5];
            for (int i = 0; i < this.mSuggestionInfos.length; i++) {
                this.mSuggestionInfos[i] = new SuggestionInfo();
            }
            this.mAddToDictionaryButton = (TextView) this.mContentView.findViewById(R.id.addToDictionaryButton);
            this.mAddToDictionaryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SuggestionSpan suggestionSpanFindEquivalentSuggestionSpan = Editor.this.findEquivalentSuggestionSpan(SuggestionsPopupWindow.this.mMisspelledSpanInfo);
                    if (suggestionSpanFindEquivalentSuggestionSpan != null) {
                        Editable editable = (Editable) Editor.this.mTextView.getText();
                        int spanStart = editable.getSpanStart(suggestionSpanFindEquivalentSuggestionSpan);
                        int spanEnd = editable.getSpanEnd(suggestionSpanFindEquivalentSuggestionSpan);
                        if (spanStart < 0 || spanEnd <= spanStart) {
                            return;
                        }
                        String strSubstring = TextUtils.substring(editable, spanStart, spanEnd);
                        if (BenesseExtension.getDchaState() == 0) {
                            Intent intent = new Intent(Settings.ACTION_USER_DICTIONARY_INSERT);
                            intent.putExtra("word", strSubstring);
                            intent.putExtra("locale", Editor.this.mTextView.getTextServicesLocale().toString());
                            intent.setFlags(intent.getFlags() | 268435456);
                            Editor.this.mTextView.getContext().startActivity(intent);
                        }
                        editable.removeSpan(SuggestionsPopupWindow.this.mMisspelledSpanInfo.mSuggestionSpan);
                        Selection.setSelection(editable, spanEnd);
                        Editor.this.updateSpellCheckSpans(spanStart, spanEnd, false);
                        SuggestionsPopupWindow.this.hideWithCleanUp();
                    }
                }
            });
            this.mDeleteButton = (TextView) this.mContentView.findViewById(R.id.deleteButton);
            this.mDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Editable editable = (Editable) Editor.this.mTextView.getText();
                    int spanStart = editable.getSpanStart(Editor.this.mSuggestionRangeSpan);
                    int spanEnd = editable.getSpanEnd(Editor.this.mSuggestionRangeSpan);
                    if (spanStart >= 0 && spanEnd > spanStart) {
                        if (spanEnd < editable.length() && Character.isSpaceChar(editable.charAt(spanEnd)) && (spanStart == 0 || Character.isSpaceChar(editable.charAt(spanStart - 1)))) {
                            spanEnd++;
                        }
                        Editor.this.mTextView.deleteText_internal(spanStart, spanEnd);
                    }
                    SuggestionsPopupWindow.this.hideWithCleanUp();
                }
            });
        }

        public boolean isShowingUp() {
            return this.mIsShowingUp;
        }

        public void onParentLostFocus() {
            this.mIsShowingUp = false;
        }

        private class SuggestionAdapter extends BaseAdapter {
            private LayoutInflater mInflater;

            private SuggestionAdapter() {
                this.mInflater = (LayoutInflater) SuggestionsPopupWindow.this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }

            @Override
            public int getCount() {
                return SuggestionsPopupWindow.this.mNumberOfSuggestions;
            }

            @Override
            public Object getItem(int i) {
                return SuggestionsPopupWindow.this.mSuggestionInfos[i];
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                TextView textView = (TextView) view;
                if (textView == null) {
                    textView = (TextView) this.mInflater.inflate(Editor.this.mTextView.mTextEditSuggestionItemLayout, viewGroup, false);
                }
                textView.setText(SuggestionsPopupWindow.this.mSuggestionInfos[i].mText);
                return textView;
            }
        }

        @VisibleForTesting
        public ViewGroup getContentViewForTesting() {
            return this.mContentView;
        }

        @Override
        public void show() {
            if (!(Editor.this.mTextView.getText() instanceof Editable) || Editor.this.extractedTextModeWillBeStarted()) {
                return;
            }
            if (updateSuggestions()) {
                this.mCursorWasVisibleBeforeSuggestions = Editor.this.mCursorVisible;
                Editor.this.mTextView.setCursorVisible(false);
                this.mIsShowingUp = true;
                super.show();
            }
            this.mSuggestionListView.setVisibility(this.mNumberOfSuggestions == 0 ? 8 : 0);
        }

        @Override
        protected void measureContent() {
            DisplayMetrics displayMetrics = Editor.this.mTextView.getResources().getDisplayMetrics();
            int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, Integer.MIN_VALUE);
            int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, Integer.MIN_VALUE);
            View view = null;
            int iMax = 0;
            for (int i = 0; i < this.mNumberOfSuggestions; i++) {
                view = this.mSuggestionsAdapter.getView(i, view, this.mContentView);
                view.getLayoutParams().width = -2;
                view.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
                iMax = Math.max(iMax, view.getMeasuredWidth());
            }
            if (this.mAddToDictionaryButton.getVisibility() != 8) {
                this.mAddToDictionaryButton.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
                iMax = Math.max(iMax, this.mAddToDictionaryButton.getMeasuredWidth());
            }
            this.mDeleteButton.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
            int iMax2 = Math.max(iMax, this.mDeleteButton.getMeasuredWidth()) + this.mContainerView.getPaddingLeft() + this.mContainerView.getPaddingRight() + this.mContainerMarginWidth;
            this.mContentView.measure(View.MeasureSpec.makeMeasureSpec(iMax2, 1073741824), iMakeMeasureSpec2);
            Drawable background = this.mPopupWindow.getBackground();
            if (background != null) {
                if (Editor.this.mTempRect == null) {
                    Editor.this.mTempRect = new Rect();
                }
                background.getPadding(Editor.this.mTempRect);
                iMax2 += Editor.this.mTempRect.left + Editor.this.mTempRect.right;
            }
            this.mPopupWindow.setWidth(iMax2);
        }

        @Override
        protected int getTextOffset() {
            return (Editor.this.mTextView.getSelectionStart() + Editor.this.mTextView.getSelectionStart()) / 2;
        }

        @Override
        protected int getVerticalLocalPosition(int i) {
            return Editor.this.mTextView.getLayout().getLineBottomWithoutSpacing(i) - this.mContainerMarginTop;
        }

        @Override
        protected int clipVertically(int i) {
            return Math.min(i, Editor.this.mTextView.getResources().getDisplayMetrics().heightPixels - this.mContentView.getMeasuredHeight());
        }

        private void hideWithCleanUp() {
            for (SuggestionInfo suggestionInfo : this.mSuggestionInfos) {
                suggestionInfo.clear();
            }
            this.mMisspelledSpanInfo.clear();
            hide();
        }

        private boolean updateSuggestions() {
            int underlineColor;
            Spannable spannable = (Spannable) Editor.this.mTextView.getText();
            this.mNumberOfSuggestions = Editor.this.mSuggestionHelper.getSuggestionInfo(this.mSuggestionInfos, this.mMisspelledSpanInfo);
            if (this.mNumberOfSuggestions == 0 && this.mMisspelledSpanInfo.mSuggestionSpan == null) {
                return false;
            }
            int length = Editor.this.mTextView.getText().length();
            int iMax = 0;
            for (int i = 0; i < this.mNumberOfSuggestions; i++) {
                SuggestionSpanInfo suggestionSpanInfo = this.mSuggestionInfos[i].mSuggestionSpanInfo;
                length = Math.min(length, suggestionSpanInfo.mSpanStart);
                iMax = Math.max(iMax, suggestionSpanInfo.mSpanEnd);
            }
            if (this.mMisspelledSpanInfo.mSuggestionSpan != null) {
                length = Math.min(length, this.mMisspelledSpanInfo.mSpanStart);
                iMax = Math.max(iMax, this.mMisspelledSpanInfo.mSpanEnd);
            }
            for (int i2 = 0; i2 < this.mNumberOfSuggestions; i2++) {
                highlightTextDifferences(this.mSuggestionInfos[i2], length, iMax);
            }
            int i3 = 8;
            if (this.mMisspelledSpanInfo.mSuggestionSpan != null && this.mMisspelledSpanInfo.mSpanStart >= 0 && this.mMisspelledSpanInfo.mSpanEnd > this.mMisspelledSpanInfo.mSpanStart) {
                i3 = 0;
            }
            this.mAddToDictionaryButton.setVisibility(i3);
            if (Editor.this.mSuggestionRangeSpan == null) {
                Editor.this.mSuggestionRangeSpan = new SuggestionRangeSpan();
            }
            if (this.mNumberOfSuggestions != 0) {
                underlineColor = this.mSuggestionInfos[0].mSuggestionSpanInfo.mSuggestionSpan.getUnderlineColor();
            } else {
                underlineColor = this.mMisspelledSpanInfo.mSuggestionSpan.getUnderlineColor();
            }
            if (underlineColor == 0) {
                Editor.this.mSuggestionRangeSpan.setBackgroundColor(Editor.this.mTextView.mHighlightColor);
            } else {
                Editor.this.mSuggestionRangeSpan.setBackgroundColor((underlineColor & 16777215) + (((int) (Color.alpha(underlineColor) * 0.4f)) << 24));
            }
            spannable.setSpan(Editor.this.mSuggestionRangeSpan, length, iMax, 33);
            this.mSuggestionsAdapter.notifyDataSetChanged();
            return true;
        }

        private void highlightTextDifferences(SuggestionInfo suggestionInfo, int i, int i2) {
            Spannable spannable = (Spannable) Editor.this.mTextView.getText();
            int i3 = suggestionInfo.mSuggestionSpanInfo.mSpanStart;
            int i4 = suggestionInfo.mSuggestionSpanInfo.mSpanEnd;
            suggestionInfo.mSuggestionStart = i3 - i;
            suggestionInfo.mSuggestionEnd = suggestionInfo.mSuggestionStart + suggestionInfo.mText.length();
            suggestionInfo.mText.setSpan(this.mHighlightSpan, 0, suggestionInfo.mText.length(), 33);
            String string = spannable.toString();
            suggestionInfo.mText.insert(0, (CharSequence) string.substring(i, i3));
            suggestionInfo.mText.append((CharSequence) string.substring(i4, i2));
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            Editor.this.replaceWithSuggestion(this.mSuggestionInfos[i]);
            hideWithCleanUp();
        }
    }

    private class TextActionModeCallback extends ActionMode.Callback2 {
        private final int mHandleHeight;
        private final boolean mHasSelection;
        private final Path mSelectionPath = new Path();
        private final RectF mSelectionBounds = new RectF();
        private final Map<MenuItem, View.OnClickListener> mAssistClickHandlers = new HashMap();

        TextActionModeCallback(@TextActionMode int i) {
            this.mHasSelection = i == 0 || (Editor.this.mTextIsSelectable && i == 2);
            if (!this.mHasSelection) {
                InsertionPointCursorController insertionController = Editor.this.getInsertionController();
                if (insertionController == null) {
                    this.mHandleHeight = 0;
                    return;
                } else {
                    insertionController.getHandle();
                    this.mHandleHeight = Editor.this.mSelectHandleCenter.getMinimumHeight();
                    return;
                }
            }
            SelectionModifierCursorController selectionController = Editor.this.getSelectionController();
            if (selectionController.mStartHandle == null) {
                selectionController.initDrawables();
                selectionController.initHandles();
                selectionController.hide();
            }
            this.mHandleHeight = Math.max(Editor.this.mSelectHandleLeft.getMinimumHeight(), Editor.this.mSelectHandleRight.getMinimumHeight());
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            this.mAssistClickHandlers.clear();
            actionMode.setTitle((CharSequence) null);
            actionMode.setSubtitle((CharSequence) null);
            actionMode.setTitleOptionalHint(true);
            populateMenuWithItems(menu);
            ActionMode.Callback customCallback = getCustomCallback();
            if (customCallback == null || customCallback.onCreateActionMode(actionMode, menu)) {
                if (Editor.this.mTextView.canProcessText()) {
                    Editor.this.mProcessTextIntentActionsHandler.onInitializeMenu(menu);
                }
                if (this.mHasSelection && !Editor.this.mTextView.hasTransientState()) {
                    Editor.this.mTextView.setHasTransientState(true);
                }
                return true;
            }
            Selection.setSelection((Spannable) Editor.this.mTextView.getText(), Editor.this.mTextView.getSelectionEnd());
            return false;
        }

        private ActionMode.Callback getCustomCallback() {
            if (this.mHasSelection) {
                return Editor.this.mCustomSelectionActionModeCallback;
            }
            return Editor.this.mCustomInsertionActionModeCallback;
        }

        private void populateMenuWithItems(Menu menu) {
            String selectedText;
            if (Editor.this.mTextView.canCut()) {
                menu.add(0, 16908320, 4, 17039363).setAlphabeticShortcut(EpicenterTranslateClipReveal.StateProperty.TARGET_X).setShowAsAction(2);
            }
            if (Editor.this.mTextView.canCopy()) {
                menu.add(0, 16908321, 5, 17039361).setAlphabeticShortcut('c').setShowAsAction(2);
            }
            if (Editor.this.mTextView.canPaste()) {
                menu.add(0, 16908322, 6, 17039371).setAlphabeticShortcut('v').setShowAsAction(2);
            }
            if (Editor.this.mTextView.canShare()) {
                menu.add(0, 16908341, 7, R.string.share).setShowAsAction(1);
            }
            if (Editor.this.mTextView.canRequestAutofill() && ((selectedText = Editor.this.mTextView.getSelectedText()) == null || selectedText.isEmpty())) {
                menu.add(0, 16908355, 10, 17039386).setShowAsAction(0);
            }
            if (Editor.this.mTextView.canPasteAsPlainText()) {
                menu.add(0, 16908337, 11, 17039385).setShowAsAction(1);
            }
            updateSelectAllItem(menu);
            updateReplaceItem(menu);
            updateAssistMenuItems(menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            updateSelectAllItem(menu);
            updateReplaceItem(menu);
            updateAssistMenuItems(menu);
            ActionMode.Callback customCallback = getCustomCallback();
            if (customCallback != null) {
                return customCallback.onPrepareActionMode(actionMode, menu);
            }
            return true;
        }

        private void updateSelectAllItem(Menu menu) {
            boolean zCanSelectAllText = Editor.this.mTextView.canSelectAllText();
            boolean z = menu.findItem(16908319) != null;
            if (zCanSelectAllText && !z) {
                menu.add(0, 16908319, 8, 17039373).setShowAsAction(1);
            } else if (!zCanSelectAllText && z) {
                menu.removeItem(16908319);
            }
        }

        private void updateReplaceItem(Menu menu) {
            boolean z = Editor.this.mTextView.isSuggestionsEnabled() && Editor.this.shouldOfferToShowSuggestions();
            boolean z2 = menu.findItem(16908340) != null;
            if (z && !z2) {
                menu.add(0, 16908340, 9, R.string.replace).setShowAsAction(1);
            } else if (!z && z2) {
                menu.removeItem(16908340);
            }
        }

        private void updateAssistMenuItems(Menu menu) {
            TextClassification textClassification;
            clearAssistMenuItems(menu);
            if (!shouldEnableAssistMenuItems() || (textClassification = Editor.this.getSelectionActionModeHelper().getTextClassification()) == null) {
                return;
            }
            if (!textClassification.getActions().isEmpty()) {
                addAssistMenuItem(menu, textClassification.getActions().get(0), 16908353, 0, 2).setIntent(textClassification.getIntent());
            } else if (hasLegacyAssistItem(textClassification)) {
                MenuItem intent = menu.add(16908353, 16908353, 0, textClassification.getLabel()).setIcon(textClassification.getIcon()).setIntent(textClassification.getIntent());
                intent.setShowAsAction(2);
                this.mAssistClickHandlers.put(intent, TextClassification.createIntentOnClickListener(TextClassification.createPendingIntent(Editor.this.mTextView.getContext(), textClassification.getIntent(), createAssistMenuItemPendingIntentRequestCode())));
            }
            int size = textClassification.getActions().size();
            for (int i = 1; i < size; i++) {
                addAssistMenuItem(menu, textClassification.getActions().get(i), 0, (50 + i) - 1, 0);
            }
        }

        private MenuItem addAssistMenuItem(Menu menu, RemoteAction remoteAction, int i, int i2, int i3) {
            MenuItem contentDescription = menu.add(16908353, i, i2, remoteAction.getTitle()).setContentDescription(remoteAction.getContentDescription());
            if (remoteAction.shouldShowIcon()) {
                contentDescription.setIcon(remoteAction.getIcon().loadDrawable(Editor.this.mTextView.getContext()));
            }
            contentDescription.setShowAsAction(i3);
            this.mAssistClickHandlers.put(contentDescription, TextClassification.createIntentOnClickListener(remoteAction.getActionIntent()));
            return contentDescription;
        }

        private void clearAssistMenuItems(Menu menu) {
            int i = 0;
            while (i < menu.size()) {
                MenuItem item = menu.getItem(i);
                if (item.getGroupId() == 16908353) {
                    menu.removeItem(item.getItemId());
                } else {
                    i++;
                }
            }
        }

        private boolean hasLegacyAssistItem(TextClassification textClassification) {
            return ((textClassification.getIcon() == null && TextUtils.isEmpty(textClassification.getLabel())) || (textClassification.getIntent() == null && textClassification.getOnClickListener() == null)) ? false : true;
        }

        private boolean onAssistMenuItemClicked(MenuItem menuItem) {
            Intent intent;
            Preconditions.checkArgument(menuItem.getGroupId() == 16908353);
            TextClassification textClassification = Editor.this.getSelectionActionModeHelper().getTextClassification();
            if (!shouldEnableAssistMenuItems() || textClassification == null) {
                return true;
            }
            View.OnClickListener onClickListenerCreateIntentOnClickListener = this.mAssistClickHandlers.get(menuItem);
            if (onClickListenerCreateIntentOnClickListener == null && (intent = menuItem.getIntent()) != null) {
                onClickListenerCreateIntentOnClickListener = TextClassification.createIntentOnClickListener(TextClassification.createPendingIntent(Editor.this.mTextView.getContext(), intent, createAssistMenuItemPendingIntentRequestCode()));
            }
            if (onClickListenerCreateIntentOnClickListener != null) {
                onClickListenerCreateIntentOnClickListener.onClick(Editor.this.mTextView);
                Editor.this.stopTextActionMode();
            }
            return true;
        }

        private int createAssistMenuItemPendingIntentRequestCode() {
            if (Editor.this.mTextView.hasSelection()) {
                return Editor.this.mTextView.getText().subSequence(Editor.this.mTextView.getSelectionStart(), Editor.this.mTextView.getSelectionEnd()).hashCode();
            }
            return 0;
        }

        private boolean shouldEnableAssistMenuItems() {
            return Editor.this.mTextView.isDeviceProvisioned() && TextClassificationManager.getSettings(Editor.this.mTextView.getContext()).isSmartTextShareEnabled();
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            Editor.this.getSelectionActionModeHelper().onSelectionAction(menuItem.getItemId());
            if (Editor.this.mProcessTextIntentActionsHandler.performMenuItemAction(menuItem)) {
                return true;
            }
            ActionMode.Callback customCallback = getCustomCallback();
            if (customCallback != null && customCallback.onActionItemClicked(actionMode, menuItem)) {
                return true;
            }
            if (menuItem.getGroupId() == 16908353 && onAssistMenuItemClicked(menuItem)) {
                return true;
            }
            return Editor.this.mTextView.onTextContextMenuItem(menuItem.getItemId());
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            Editor.this.getSelectionActionModeHelper().onDestroyActionMode();
            Editor.this.mTextActionMode = null;
            ActionMode.Callback customCallback = getCustomCallback();
            if (customCallback != null) {
                customCallback.onDestroyActionMode(actionMode);
            }
            if (!Editor.this.mPreserveSelection) {
                Selection.setSelection((Spannable) Editor.this.mTextView.getText(), Editor.this.mTextView.getSelectionEnd());
            }
            if (Editor.this.mSelectionModifierCursorController != null) {
                Editor.this.mSelectionModifierCursorController.hide();
            }
            this.mAssistClickHandlers.clear();
            Editor.this.mRequestingLinkActionMode = false;
        }

        @Override
        public void onGetContentRect(ActionMode actionMode, View view, Rect rect) {
            if (view.equals(Editor.this.mTextView) && Editor.this.mTextView.getLayout() != null) {
                if (Editor.this.mTextView.getSelectionStart() == Editor.this.mTextView.getSelectionEnd()) {
                    Layout layout = Editor.this.mTextView.getLayout();
                    int lineForOffset = layout.getLineForOffset(Editor.this.mTextView.getSelectionStart());
                    float fClampHorizontalPosition = Editor.this.clampHorizontalPosition(null, layout.getPrimaryHorizontal(Editor.this.mTextView.getSelectionStart()));
                    this.mSelectionBounds.set(fClampHorizontalPosition, layout.getLineTop(lineForOffset), fClampHorizontalPosition, layout.getLineBottom(lineForOffset) + this.mHandleHeight);
                } else {
                    this.mSelectionPath.reset();
                    Editor.this.mTextView.getLayout().getSelectionPath(Editor.this.mTextView.getSelectionStart(), Editor.this.mTextView.getSelectionEnd(), this.mSelectionPath);
                    this.mSelectionPath.computeBounds(this.mSelectionBounds, true);
                    this.mSelectionBounds.bottom += this.mHandleHeight;
                }
                float fViewportToContentHorizontalOffset = Editor.this.mTextView.viewportToContentHorizontalOffset();
                float fViewportToContentVerticalOffset = Editor.this.mTextView.viewportToContentVerticalOffset();
                rect.set((int) Math.floor(this.mSelectionBounds.left + fViewportToContentHorizontalOffset), (int) Math.floor(this.mSelectionBounds.top + fViewportToContentVerticalOffset), (int) Math.ceil(this.mSelectionBounds.right + fViewportToContentHorizontalOffset), (int) Math.ceil(this.mSelectionBounds.bottom + fViewportToContentVerticalOffset));
                return;
            }
            super.onGetContentRect(actionMode, view, rect);
        }
    }

    private final class CursorAnchorInfoNotifier implements TextViewPositionListener {
        final CursorAnchorInfo.Builder mSelectionInfoBuilder;
        final int[] mTmpIntOffset;
        final Matrix mViewToScreenMatrix;

        private CursorAnchorInfoNotifier() {
            this.mSelectionInfoBuilder = new CursorAnchorInfo.Builder();
            this.mTmpIntOffset = new int[2];
            this.mViewToScreenMatrix = new Matrix();
        }

        @Override
        public void updatePosition(int i, int i2, boolean z, boolean z2) {
            InputMethodManager inputMethodManagerPeekInstance;
            Layout layout;
            int i3;
            InputMethodState inputMethodState = Editor.this.mInputMethodState;
            if (inputMethodState == null || inputMethodState.mBatchEditNesting > 0 || (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) == null || !inputMethodManagerPeekInstance.isActive(Editor.this.mTextView) || !inputMethodManagerPeekInstance.isCursorAnchorInfoEnabled() || (layout = Editor.this.mTextView.getLayout()) == null) {
                return;
            }
            CursorAnchorInfo.Builder builder = this.mSelectionInfoBuilder;
            builder.reset();
            int selectionStart = Editor.this.mTextView.getSelectionStart();
            builder.setSelectionRange(selectionStart, Editor.this.mTextView.getSelectionEnd());
            this.mViewToScreenMatrix.set(Editor.this.mTextView.getMatrix());
            Editor.this.mTextView.getLocationOnScreen(this.mTmpIntOffset);
            this.mViewToScreenMatrix.postTranslate(this.mTmpIntOffset[0], this.mTmpIntOffset[1]);
            builder.setMatrix(this.mViewToScreenMatrix);
            float fViewportToContentHorizontalOffset = Editor.this.mTextView.viewportToContentHorizontalOffset();
            float fViewportToContentVerticalOffset = Editor.this.mTextView.viewportToContentVerticalOffset();
            CharSequence text = Editor.this.mTextView.getText();
            if (text instanceof Spannable) {
                Spannable spannable = (Spannable) text;
                int composingSpanStart = EditableInputConnection.getComposingSpanStart(spannable);
                int composingSpanEnd = EditableInputConnection.getComposingSpanEnd(spannable);
                if (composingSpanEnd < composingSpanStart) {
                    i3 = composingSpanStart;
                    composingSpanStart = composingSpanEnd;
                } else {
                    i3 = composingSpanEnd;
                }
                if (composingSpanStart >= 0 && composingSpanStart < i3) {
                    builder.setComposingText(composingSpanStart, text.subSequence(composingSpanStart, i3));
                    Editor.this.mTextView.populateCharacterBounds(builder, composingSpanStart, i3, fViewportToContentHorizontalOffset, fViewportToContentVerticalOffset);
                }
            }
            if (selectionStart >= 0) {
                int lineForOffset = layout.getLineForOffset(selectionStart);
                float primaryHorizontal = layout.getPrimaryHorizontal(selectionStart) + fViewportToContentHorizontalOffset;
                float lineTop = layout.getLineTop(lineForOffset) + fViewportToContentVerticalOffset;
                float lineBaseline = layout.getLineBaseline(lineForOffset) + fViewportToContentVerticalOffset;
                float lineBottomWithoutSpacing = layout.getLineBottomWithoutSpacing(lineForOffset) + fViewportToContentVerticalOffset;
                boolean zIsPositionVisible = Editor.this.mTextView.isPositionVisible(primaryHorizontal, lineTop);
                boolean zIsPositionVisible2 = Editor.this.mTextView.isPositionVisible(primaryHorizontal, lineBottomWithoutSpacing);
                int i4 = (zIsPositionVisible || zIsPositionVisible2) ? 1 : 0;
                if (!zIsPositionVisible || !zIsPositionVisible2) {
                    i4 |= 2;
                }
                builder.setInsertionMarkerLocation(primaryHorizontal, lineTop, lineBaseline, lineBottomWithoutSpacing, layout.isRtlCharAt(selectionStart) ? i4 | 4 : i4);
            }
            inputMethodManagerPeekInstance.updateCursorAnchorInfo(Editor.this.mTextView, builder.build());
        }
    }

    private static class MagnifierMotionAnimator {
        private static final long DURATION = 100;
        private float mAnimationCurrentX;
        private float mAnimationCurrentY;
        private float mAnimationStartX;
        private float mAnimationStartY;
        private final ValueAnimator mAnimator;
        private float mLastX;
        private float mLastY;
        private final Magnifier mMagnifier;
        private boolean mMagnifierIsShowing;

        private MagnifierMotionAnimator(Magnifier magnifier) {
            this.mMagnifier = magnifier;
            this.mAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            this.mAnimator.setDuration(DURATION);
            this.mAnimator.setInterpolator(new LinearInterpolator());
            this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    Editor.MagnifierMotionAnimator.lambda$new$0(this.f$0, valueAnimator);
                }
            });
        }

        public static void lambda$new$0(MagnifierMotionAnimator magnifierMotionAnimator, ValueAnimator valueAnimator) {
            magnifierMotionAnimator.mAnimationCurrentX = magnifierMotionAnimator.mAnimationStartX + ((magnifierMotionAnimator.mLastX - magnifierMotionAnimator.mAnimationStartX) * valueAnimator.getAnimatedFraction());
            magnifierMotionAnimator.mAnimationCurrentY = magnifierMotionAnimator.mAnimationStartY + ((magnifierMotionAnimator.mLastY - magnifierMotionAnimator.mAnimationStartY) * valueAnimator.getAnimatedFraction());
            magnifierMotionAnimator.mMagnifier.show(magnifierMotionAnimator.mAnimationCurrentX, magnifierMotionAnimator.mAnimationCurrentY);
        }

        private void show(float f, float f2) {
            boolean z;
            if (!this.mMagnifierIsShowing || f2 == this.mLastY) {
                z = false;
            } else {
                z = true;
            }
            if (z) {
                if (this.mAnimator.isRunning()) {
                    this.mAnimator.cancel();
                    this.mAnimationStartX = this.mAnimationCurrentX;
                    this.mAnimationStartY = this.mAnimationCurrentY;
                } else {
                    this.mAnimationStartX = this.mLastX;
                    this.mAnimationStartY = this.mLastY;
                }
                this.mAnimator.start();
            } else if (!this.mAnimator.isRunning()) {
                this.mMagnifier.show(f, f2);
            }
            this.mLastX = f;
            this.mLastY = f2;
            this.mMagnifierIsShowing = true;
        }

        private void update() {
            this.mMagnifier.update();
        }

        private void dismiss() {
            this.mMagnifier.dismiss();
            this.mAnimator.cancel();
            this.mMagnifierIsShowing = false;
        }
    }

    @VisibleForTesting
    public abstract class HandleView extends View implements TextViewPositionListener {
        private static final int HISTORY_SIZE = 5;
        private static final int TOUCH_UP_FILTER_DELAY_AFTER = 150;
        private static final int TOUCH_UP_FILTER_DELAY_BEFORE = 350;
        private final PopupWindow mContainer;
        protected Drawable mDrawable;
        protected Drawable mDrawableLtr;
        protected Drawable mDrawableRtl;
        private final Magnifier.Callback mHandlesVisibilityCallback;
        protected int mHorizontalGravity;
        protected int mHotspotX;
        private float mIdealVerticalOffset;
        private boolean mIsDragging;
        private int mLastParentX;
        private int mLastParentXOnScreen;
        private int mLastParentY;
        private int mLastParentYOnScreen;
        private int mMinSize;
        private int mNumberPreviousOffsets;
        private boolean mPositionHasChanged;
        private int mPositionX;
        private int mPositionY;
        protected int mPrevLine;
        protected int mPreviousLineTouched;
        protected int mPreviousOffset;
        private int mPreviousOffsetIndex;
        private final int[] mPreviousOffsets;
        private final long[] mPreviousOffsetsTimes;
        private float mTouchOffsetY;
        private float mTouchToWindowOffsetX;
        private float mTouchToWindowOffsetY;

        public abstract int getCurrentCursorOffset();

        protected abstract int getHorizontalGravity(boolean z);

        protected abstract int getHotspotX(Drawable drawable, boolean z);

        protected abstract int getMagnifierHandleTrigger();

        protected abstract void updatePosition(float f, float f2, boolean z);

        protected abstract void updateSelection(int i);

        private HandleView(Drawable drawable, Drawable drawable2, int i) {
            super(Editor.this.mTextView.getContext());
            this.mPreviousOffset = -1;
            this.mPositionHasChanged = true;
            this.mPrevLine = -1;
            this.mPreviousLineTouched = -1;
            this.mPreviousOffsetsTimes = new long[5];
            this.mPreviousOffsets = new int[5];
            this.mPreviousOffsetIndex = 0;
            this.mNumberPreviousOffsets = 0;
            this.mHandlesVisibilityCallback = new Magnifier.Callback() {
                @Override
                public void onOperationComplete() {
                    Point windowCoords = Editor.this.mMagnifierAnimator.mMagnifier.getWindowCoords();
                    if (windowCoords == null) {
                        return;
                    }
                    Rect rect = new Rect(windowCoords.x, windowCoords.y, windowCoords.x + Editor.this.mMagnifierAnimator.mMagnifier.getWidth(), windowCoords.y + Editor.this.mMagnifierAnimator.mMagnifier.getHeight());
                    HandleView.this.setVisible(!HandleView.this.handleOverlapsMagnifier(HandleView.this, rect));
                    HandleView otherSelectionHandle = HandleView.this.getOtherSelectionHandle();
                    if (otherSelectionHandle != null) {
                        otherSelectionHandle.setVisible(!HandleView.this.handleOverlapsMagnifier(otherSelectionHandle, rect));
                    }
                }
            };
            setId(i);
            this.mContainer = new PopupWindow(Editor.this.mTextView.getContext(), (AttributeSet) null, 16843464);
            this.mContainer.setSplitTouchEnabled(true);
            this.mContainer.setClippingEnabled(false);
            this.mContainer.setWindowLayoutType(1002);
            this.mContainer.setWidth(-2);
            this.mContainer.setHeight(-2);
            this.mContainer.setContentView(this);
            this.mDrawableLtr = drawable;
            this.mDrawableRtl = drawable2;
            this.mMinSize = Editor.this.mTextView.getContext().getResources().getDimensionPixelSize(R.dimen.text_handle_min_size);
            updateDrawable();
            float preferredHeight = getPreferredHeight();
            this.mTouchOffsetY = (-0.3f) * preferredHeight;
            this.mIdealVerticalOffset = 0.7f * preferredHeight;
        }

        public float getIdealVerticalOffset() {
            return this.mIdealVerticalOffset;
        }

        protected void updateDrawable() {
            Layout layout;
            if (this.mIsDragging || (layout = Editor.this.mTextView.getLayout()) == null) {
                return;
            }
            int currentCursorOffset = getCurrentCursorOffset();
            boolean zIsAtRtlRun = isAtRtlRun(layout, currentCursorOffset);
            Drawable drawable = this.mDrawable;
            this.mDrawable = zIsAtRtlRun ? this.mDrawableRtl : this.mDrawableLtr;
            this.mHotspotX = getHotspotX(this.mDrawable, zIsAtRtlRun);
            this.mHorizontalGravity = getHorizontalGravity(zIsAtRtlRun);
            if (drawable != this.mDrawable && isShowing()) {
                this.mPositionX = ((getCursorHorizontalPosition(layout, currentCursorOffset) - this.mHotspotX) - getHorizontalOffset()) + getCursorOffset();
                this.mPositionX += Editor.this.mTextView.viewportToContentHorizontalOffset();
                this.mPositionHasChanged = true;
                updatePosition(this.mLastParentX, this.mLastParentY, false, false);
                postInvalidate();
            }
        }

        private void startTouchUpFilter(int i) {
            this.mNumberPreviousOffsets = 0;
            addPositionToTouchUpFilter(i);
        }

        private void addPositionToTouchUpFilter(int i) {
            this.mPreviousOffsetIndex = (this.mPreviousOffsetIndex + 1) % 5;
            this.mPreviousOffsets[this.mPreviousOffsetIndex] = i;
            this.mPreviousOffsetsTimes[this.mPreviousOffsetIndex] = SystemClock.uptimeMillis();
            this.mNumberPreviousOffsets++;
        }

        private void filterOnTouchUp(boolean z) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            int i = this.mPreviousOffsetIndex;
            int iMin = Math.min(this.mNumberPreviousOffsets, 5);
            int i2 = i;
            int i3 = 0;
            while (i3 < iMin && jUptimeMillis - this.mPreviousOffsetsTimes[i2] < 150) {
                i3++;
                i2 = ((this.mPreviousOffsetIndex - i3) + 5) % 5;
            }
            if (i3 > 0 && i3 < iMin && jUptimeMillis - this.mPreviousOffsetsTimes[i2] > 350) {
                positionAtCursorOffset(this.mPreviousOffsets[i2], false, z);
            }
        }

        public boolean offsetHasBeenChanged() {
            return this.mNumberPreviousOffsets > 1;
        }

        @Override
        protected void onMeasure(int i, int i2) {
            setMeasuredDimension(getPreferredWidth(), getPreferredHeight());
        }

        @Override
        public void invalidate() {
            super.invalidate();
            if (isShowing()) {
                positionAtCursorOffset(getCurrentCursorOffset(), true, false);
            }
        }

        private int getPreferredWidth() {
            return Math.max(this.mDrawable.getIntrinsicWidth(), this.mMinSize);
        }

        private int getPreferredHeight() {
            return Math.max(this.mDrawable.getIntrinsicHeight(), this.mMinSize);
        }

        public void show() {
            if (isShowing()) {
                return;
            }
            Editor.this.getPositionListener().addSubscriber(this, true);
            this.mPreviousOffset = -1;
            positionAtCursorOffset(getCurrentCursorOffset(), false, false);
        }

        protected void dismiss() {
            this.mIsDragging = false;
            this.mContainer.dismiss();
            onDetached();
        }

        public void hide() {
            dismiss();
            Editor.this.getPositionListener().removeSubscriber(this);
        }

        public boolean isShowing() {
            return this.mContainer.isShowing();
        }

        private boolean shouldShow() {
            if (!this.mIsDragging) {
                if (Editor.this.mTextView.isInBatchEditMode()) {
                    return false;
                }
                return Editor.this.mTextView.isPositionVisible(this.mPositionX + this.mHotspotX + getHorizontalOffset(), this.mPositionY);
            }
            return true;
        }

        private void setVisible(boolean z) {
            this.mContainer.getContentView().setVisibility(z ? 0 : 4);
        }

        protected boolean isAtRtlRun(Layout layout, int i) {
            return layout.isRtlCharAt(i);
        }

        @VisibleForTesting
        public float getHorizontal(Layout layout, int i) {
            return layout.getPrimaryHorizontal(i);
        }

        protected int getOffsetAtCoordinate(Layout layout, int i, float f) {
            return Editor.this.mTextView.getOffsetAtCoordinate(i, f);
        }

        protected void positionAtCursorOffset(int i, boolean z, boolean z2) {
            boolean z3;
            if (Editor.this.mTextView.getLayout() != null) {
                Layout layout = Editor.this.mTextView.getLayout();
                if (i == this.mPreviousOffset) {
                    z3 = false;
                } else {
                    z3 = true;
                }
                if (z3 || z) {
                    if (z3) {
                        updateSelection(i);
                        if (z2 && Editor.this.mHapticTextHandleEnabled) {
                            Editor.this.mTextView.performHapticFeedback(9);
                        }
                        addPositionToTouchUpFilter(i);
                    }
                    int lineForOffset = layout.getLineForOffset(i);
                    this.mPrevLine = lineForOffset;
                    this.mPositionX = ((getCursorHorizontalPosition(layout, i) - this.mHotspotX) - getHorizontalOffset()) + getCursorOffset();
                    this.mPositionY = layout.getLineBottomWithoutSpacing(lineForOffset);
                    this.mPositionX += Editor.this.mTextView.viewportToContentHorizontalOffset();
                    this.mPositionY += Editor.this.mTextView.viewportToContentVerticalOffset();
                    this.mPreviousOffset = i;
                    this.mPositionHasChanged = true;
                    return;
                }
                return;
            }
            Editor.this.prepareCursorControllers();
        }

        int getCursorHorizontalPosition(Layout layout, int i) {
            return (int) (getHorizontal(layout, i) - Editor.LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS);
        }

        @Override
        public void updatePosition(int i, int i2, boolean z, boolean z2) {
            positionAtCursorOffset(getCurrentCursorOffset(), z2, false);
            if (z || this.mPositionHasChanged) {
                if (this.mIsDragging) {
                    if (i != this.mLastParentX || i2 != this.mLastParentY) {
                        this.mTouchToWindowOffsetX += i - this.mLastParentX;
                        this.mTouchToWindowOffsetY += i2 - this.mLastParentY;
                        this.mLastParentX = i;
                        this.mLastParentY = i2;
                    }
                    onHandleMoved();
                }
                if (shouldShow()) {
                    int[] iArr = {this.mPositionX + this.mHotspotX + getHorizontalOffset(), this.mPositionY};
                    Editor.this.mTextView.transformFromViewToWindowSpace(iArr);
                    iArr[0] = iArr[0] - (this.mHotspotX + getHorizontalOffset());
                    if (!isShowing()) {
                        this.mContainer.showAtLocation(Editor.this.mTextView, 0, iArr[0], iArr[1]);
                    } else {
                        this.mContainer.update(iArr[0], iArr[1], -1, -1);
                    }
                } else if (isShowing()) {
                    dismiss();
                }
                this.mPositionHasChanged = false;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int intrinsicWidth = this.mDrawable.getIntrinsicWidth();
            int horizontalOffset = getHorizontalOffset();
            this.mDrawable.setBounds(horizontalOffset, 0, intrinsicWidth + horizontalOffset, this.mDrawable.getIntrinsicHeight());
            this.mDrawable.draw(canvas);
        }

        private int getHorizontalOffset() {
            int preferredWidth = getPreferredWidth();
            int intrinsicWidth = this.mDrawable.getIntrinsicWidth();
            int i = this.mHorizontalGravity;
            if (i == 3) {
                return 0;
            }
            if (i != 5) {
                return (preferredWidth - intrinsicWidth) / 2;
            }
            return preferredWidth - intrinsicWidth;
        }

        protected int getCursorOffset() {
            return 0;
        }

        private boolean tooLargeTextForMagnifier() {
            float fRound = Math.round(Editor.this.mMagnifierAnimator.mMagnifier.getHeight() / Editor.this.mMagnifierAnimator.mMagnifier.getZoom());
            Paint.FontMetrics fontMetrics = Editor.this.mTextView.getPaint().getFontMetrics();
            return fontMetrics.descent - fontMetrics.ascent > fRound;
        }

        private boolean obtainMagnifierShowCoordinates(MotionEvent motionEvent, PointF pointF) {
            int selectionStart;
            int selectionEnd;
            boolean z;
            float lineLeft;
            float lineRight;
            int magnifierHandleTrigger = getMagnifierHandleTrigger();
            switch (magnifierHandleTrigger) {
                case 0:
                    selectionStart = Editor.this.mTextView.getSelectionStart();
                    selectionEnd = -1;
                    break;
                case 1:
                    selectionStart = Editor.this.mTextView.getSelectionStart();
                    selectionEnd = Editor.this.mTextView.getSelectionEnd();
                    break;
                case 2:
                    selectionStart = Editor.this.mTextView.getSelectionEnd();
                    selectionEnd = Editor.this.mTextView.getSelectionStart();
                    break;
                default:
                    selectionStart = -1;
                    selectionEnd = -1;
                    break;
            }
            if (selectionStart == -1) {
                return false;
            }
            Layout layout = Editor.this.mTextView.getLayout();
            int lineForOffset = layout.getLineForOffset(selectionStart);
            boolean z2 = selectionEnd != -1 && lineForOffset == layout.getLineForOffset(selectionEnd);
            if (z2) {
                if ((selectionStart < selectionEnd) != (getHorizontal(Editor.this.mTextView.getLayout(), selectionStart) < getHorizontal(Editor.this.mTextView.getLayout(), selectionEnd))) {
                    z = true;
                }
            } else {
                z = false;
            }
            Editor.this.mTextView.getLocationOnScreen(new int[2]);
            float rawX = motionEvent.getRawX() - r8[0];
            float totalPaddingLeft = Editor.this.mTextView.getTotalPaddingLeft() - Editor.this.mTextView.getScrollX();
            float totalPaddingLeft2 = Editor.this.mTextView.getTotalPaddingLeft() - Editor.this.mTextView.getScrollX();
            if (z2) {
                if ((magnifierHandleTrigger == 2) ^ z) {
                    lineLeft = totalPaddingLeft + getHorizontal(Editor.this.mTextView.getLayout(), selectionEnd);
                }
            } else {
                lineLeft = totalPaddingLeft + Editor.this.mTextView.getLayout().getLineLeft(lineForOffset);
            }
            if (z2) {
                if ((magnifierHandleTrigger == 1) ^ z) {
                    lineRight = totalPaddingLeft2 + getHorizontal(Editor.this.mTextView.getLayout(), selectionEnd);
                }
            } else {
                lineRight = totalPaddingLeft2 + Editor.this.mTextView.getLayout().getLineRight(lineForOffset);
            }
            float fRound = Math.round(Editor.this.mMagnifierAnimator.mMagnifier.getWidth() / Editor.this.mMagnifierAnimator.mMagnifier.getZoom()) / 2.0f;
            if (rawX < lineLeft - fRound || rawX > fRound + lineRight) {
                return false;
            }
            pointF.x = Math.max(lineLeft, Math.min(lineRight, rawX));
            pointF.y = (((Editor.this.mTextView.getLayout().getLineTop(lineForOffset) + Editor.this.mTextView.getLayout().getLineBottom(lineForOffset)) / 2.0f) + Editor.this.mTextView.getTotalPaddingTop()) - Editor.this.mTextView.getScrollY();
            return true;
        }

        private boolean handleOverlapsMagnifier(HandleView handleView, Rect rect) {
            PopupWindow popupWindow = handleView.mContainer;
            if (!popupWindow.hasDecorView()) {
                return false;
            }
            return Rect.intersects(new Rect(popupWindow.getDecorViewLayoutParams().x, popupWindow.getDecorViewLayoutParams().y, popupWindow.getDecorViewLayoutParams().x + popupWindow.getContentView().getWidth(), popupWindow.getDecorViewLayoutParams().y + popupWindow.getContentView().getHeight()), rect);
        }

        private HandleView getOtherSelectionHandle() {
            SelectionModifierCursorController selectionController = Editor.this.getSelectionController();
            if (selectionController == null || !selectionController.isActive()) {
                return null;
            }
            return selectionController.mStartHandle != this ? selectionController.mStartHandle : selectionController.mEndHandle;
        }

        protected final void updateMagnifier(MotionEvent motionEvent) {
            boolean z;
            if (Editor.this.mMagnifierAnimator == null) {
                return;
            }
            PointF pointF = new PointF();
            if (tooLargeTextForMagnifier() || !obtainMagnifierShowCoordinates(motionEvent, pointF)) {
                z = false;
            } else {
                z = true;
            }
            if (z) {
                Editor.this.mRenderCursorRegardlessTiming = true;
                Editor.this.mTextView.invalidateCursorPath();
                Editor.this.suspendBlink();
                Editor.this.mMagnifierAnimator.mMagnifier.setOnOperationCompleteCallback(this.mHandlesVisibilityCallback);
                Editor.this.mMagnifierAnimator.show(pointF.x, pointF.y);
                return;
            }
            dismissMagnifier();
        }

        protected final void dismissMagnifier() {
            if (Editor.this.mMagnifierAnimator != null) {
                Editor.this.mMagnifierAnimator.dismiss();
                Editor.this.mRenderCursorRegardlessTiming = false;
                Editor.this.resumeBlink();
                setVisible(true);
                HandleView otherSelectionHandle = getOtherSelectionHandle();
                if (otherSelectionHandle != null) {
                    otherSelectionHandle.setVisible(true);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            float fMin;
            Editor.this.updateFloatingToolbarVisibility(motionEvent);
            switch (motionEvent.getActionMasked()) {
                case 0:
                    startTouchUpFilter(getCurrentCursorOffset());
                    PositionListener positionListener = Editor.this.getPositionListener();
                    this.mLastParentX = positionListener.getPositionX();
                    this.mLastParentY = positionListener.getPositionY();
                    this.mLastParentXOnScreen = positionListener.getPositionXOnScreen();
                    this.mLastParentYOnScreen = positionListener.getPositionYOnScreen();
                    float rawX = (motionEvent.getRawX() - this.mLastParentXOnScreen) + this.mLastParentX;
                    float rawY = (motionEvent.getRawY() - this.mLastParentYOnScreen) + this.mLastParentY;
                    this.mTouchToWindowOffsetX = rawX - this.mPositionX;
                    this.mTouchToWindowOffsetY = rawY - this.mPositionY;
                    this.mIsDragging = true;
                    this.mPreviousLineTouched = -1;
                    return true;
                case 1:
                    filterOnTouchUp(motionEvent.isFromSource(4098));
                    this.mIsDragging = false;
                    updateDrawable();
                    return true;
                case 2:
                    float rawX2 = (motionEvent.getRawX() - this.mLastParentXOnScreen) + this.mLastParentX;
                    float rawY2 = (motionEvent.getRawY() - this.mLastParentYOnScreen) + this.mLastParentY;
                    float f = this.mTouchToWindowOffsetY - this.mLastParentY;
                    float f2 = (rawY2 - this.mPositionY) - this.mLastParentY;
                    if (f < this.mIdealVerticalOffset) {
                        fMin = Math.max(Math.min(f2, this.mIdealVerticalOffset), f);
                    } else {
                        fMin = Math.min(Math.max(f2, this.mIdealVerticalOffset), f);
                    }
                    this.mTouchToWindowOffsetY = fMin + this.mLastParentY;
                    updatePosition((rawX2 - this.mTouchToWindowOffsetX) + this.mHotspotX + getHorizontalOffset(), (rawY2 - this.mTouchToWindowOffsetY) + this.mTouchOffsetY, motionEvent.isFromSource(4098));
                    return true;
                case 3:
                    this.mIsDragging = false;
                    updateDrawable();
                    return true;
                default:
                    return true;
            }
        }

        public boolean isDragging() {
            return this.mIsDragging;
        }

        void onHandleMoved() {
        }

        public void onDetached() {
        }
    }

    private class InsertionHandleView extends HandleView {
        private static final int DELAY_BEFORE_HANDLE_FADES_OUT = 4000;
        private static final int RECENT_CUT_COPY_DURATION = 15000;
        private float mDownPositionX;
        private float mDownPositionY;
        private Runnable mHider;

        public InsertionHandleView(Drawable drawable) {
            super(drawable, drawable, R.id.insertion_handle);
        }

        @Override
        public void show() {
            super.show();
            long jUptimeMillis = SystemClock.uptimeMillis() - TextView.sLastCutCopyOrTextChangedTime;
            if (Editor.this.mInsertionActionModeRunnable != null && (Editor.this.mTapState == 2 || Editor.this.mTapState == 3 || Editor.this.isCursorInsideEasyCorrectionSpan())) {
                Editor.this.mTextView.removeCallbacks(Editor.this.mInsertionActionModeRunnable);
            }
            if (Editor.this.mTapState != 2 && Editor.this.mTapState != 3 && !Editor.this.isCursorInsideEasyCorrectionSpan() && jUptimeMillis < 15000 && Editor.this.mTextActionMode == null) {
                if (Editor.this.mInsertionActionModeRunnable == null) {
                    Editor.this.mInsertionActionModeRunnable = new Runnable() {
                        @Override
                        public void run() {
                            Editor.this.startInsertionActionMode();
                        }
                    };
                }
                Editor.this.mTextView.postDelayed(Editor.this.mInsertionActionModeRunnable, ViewConfiguration.getDoubleTapTimeout() + 1);
            }
            hideAfterDelay();
        }

        private void hideAfterDelay() {
            if (this.mHider == null) {
                this.mHider = new Runnable() {
                    @Override
                    public void run() {
                        InsertionHandleView.this.hide();
                    }
                };
            } else {
                removeHiderCallback();
            }
            Editor.this.mTextView.postDelayed(this.mHider, 4000L);
        }

        private void removeHiderCallback() {
            if (this.mHider != null) {
                Editor.this.mTextView.removeCallbacks(this.mHider);
            }
        }

        @Override
        protected int getHotspotX(Drawable drawable, boolean z) {
            return drawable.getIntrinsicWidth() / 2;
        }

        @Override
        protected int getHorizontalGravity(boolean z) {
            return 1;
        }

        @Override
        protected int getCursorOffset() {
            int cursorOffset = super.getCursorOffset();
            if (Editor.this.mDrawableForCursor != null) {
                Editor.this.mDrawableForCursor.getPadding(Editor.this.mTempRect);
                return cursorOffset + (((Editor.this.mDrawableForCursor.getIntrinsicWidth() - Editor.this.mTempRect.left) - Editor.this.mTempRect.right) / 2);
            }
            return cursorOffset;
        }

        @Override
        int getCursorHorizontalPosition(Layout layout, int i) {
            if (Editor.this.mDrawableForCursor != null) {
                return Editor.this.clampHorizontalPosition(Editor.this.mDrawableForCursor, getHorizontal(layout, i)) + Editor.this.mTempRect.left;
            }
            return super.getCursorHorizontalPosition(layout, i);
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            boolean zOnTouchEvent = super.onTouchEvent(motionEvent);
            switch (motionEvent.getActionMasked()) {
                case 0:
                    this.mDownPositionX = motionEvent.getRawX();
                    this.mDownPositionY = motionEvent.getRawY();
                    updateMagnifier(motionEvent);
                    return zOnTouchEvent;
                case 1:
                    if (offsetHasBeenChanged()) {
                        if (Editor.this.mTextActionMode != null) {
                            Editor.this.mTextActionMode.invalidateContentRect();
                        }
                    } else {
                        float rawX = this.mDownPositionX - motionEvent.getRawX();
                        float rawY = this.mDownPositionY - motionEvent.getRawY();
                        float f = (rawX * rawX) + (rawY * rawY);
                        int scaledTouchSlop = ViewConfiguration.get(Editor.this.mTextView.getContext()).getScaledTouchSlop();
                        if (f < scaledTouchSlop * scaledTouchSlop) {
                            if (Editor.this.mTextActionMode != null) {
                                Editor.this.stopTextActionMode();
                            } else {
                                Editor.this.startInsertionActionMode();
                            }
                        }
                    }
                    hideAfterDelay();
                    dismissMagnifier();
                    return zOnTouchEvent;
                case 2:
                    updateMagnifier(motionEvent);
                    return zOnTouchEvent;
                case 3:
                    hideAfterDelay();
                    dismissMagnifier();
                    return zOnTouchEvent;
                default:
                    return zOnTouchEvent;
            }
        }

        @Override
        public int getCurrentCursorOffset() {
            return Editor.this.mTextView.getSelectionStart();
        }

        @Override
        public void updateSelection(int i) {
            Selection.setSelection((Spannable) Editor.this.mTextView.getText(), i);
        }

        @Override
        protected void updatePosition(float f, float f2, boolean z) {
            Layout layout = Editor.this.mTextView.getLayout();
            int offsetAtCoordinate = -1;
            if (layout != null) {
                if (this.mPreviousLineTouched == -1) {
                    this.mPreviousLineTouched = Editor.this.mTextView.getLineAtCoordinate(f2);
                }
                int currentLineAdjustedForSlop = Editor.this.getCurrentLineAdjustedForSlop(layout, this.mPreviousLineTouched, f2);
                offsetAtCoordinate = getOffsetAtCoordinate(layout, currentLineAdjustedForSlop, f);
                this.mPreviousLineTouched = currentLineAdjustedForSlop;
            }
            positionAtCursorOffset(offsetAtCoordinate, false, z);
            if (Editor.this.mTextActionMode != null) {
                Editor.this.invalidateActionMode();
            }
        }

        @Override
        void onHandleMoved() {
            super.onHandleMoved();
            removeHiderCallback();
        }

        @Override
        public void onDetached() {
            super.onDetached();
            removeHiderCallback();
        }

        @Override
        protected int getMagnifierHandleTrigger() {
            return 0;
        }
    }

    @VisibleForTesting
    public final class SelectionHandleView extends HandleView {
        private final int mHandleType;
        private boolean mInWord;
        private boolean mLanguageDirectionChanged;
        private float mPrevX;
        private final float mTextViewEdgeSlop;
        private final int[] mTextViewLocation;
        private float mTouchWordDelta;

        public SelectionHandleView(Drawable drawable, Drawable drawable2, int i, int i2) {
            super(drawable, drawable2, i);
            this.mInWord = false;
            this.mLanguageDirectionChanged = false;
            this.mTextViewLocation = new int[2];
            this.mHandleType = i2;
            this.mTextViewEdgeSlop = ViewConfiguration.get(Editor.this.mTextView.getContext()).getScaledTouchSlop() * 4;
        }

        private boolean isStartHandle() {
            return this.mHandleType == 0;
        }

        @Override
        protected int getHotspotX(Drawable drawable, boolean z) {
            if (z == isStartHandle()) {
                return drawable.getIntrinsicWidth() / 4;
            }
            return (drawable.getIntrinsicWidth() * 3) / 4;
        }

        @Override
        protected int getHorizontalGravity(boolean z) {
            return z == isStartHandle() ? 3 : 5;
        }

        @Override
        public int getCurrentCursorOffset() {
            return isStartHandle() ? Editor.this.mTextView.getSelectionStart() : Editor.this.mTextView.getSelectionEnd();
        }

        @Override
        protected void updateSelection(int i) {
            if (isStartHandle()) {
                Selection.setSelection((Spannable) Editor.this.mTextView.getText(), i, Editor.this.mTextView.getSelectionEnd());
            } else {
                Selection.setSelection((Spannable) Editor.this.mTextView.getText(), Editor.this.mTextView.getSelectionStart(), i);
            }
            updateDrawable();
            if (Editor.this.mTextActionMode != null) {
                Editor.this.invalidateActionMode();
            }
        }

        @Override
        protected void updatePosition(float f, float f2, boolean z) {
            boolean z2;
            int i;
            int offsetToLeftOf;
            Layout layout = Editor.this.mTextView.getLayout();
            if (layout == null) {
                positionAndAdjustForCrossingHandles(Editor.this.mTextView.getOffsetForPosition(f, f2), z);
                return;
            }
            if (this.mPreviousLineTouched == -1) {
                this.mPreviousLineTouched = Editor.this.mTextView.getLineAtCoordinate(f2);
            }
            int selectionEnd = isStartHandle() ? Editor.this.mTextView.getSelectionEnd() : Editor.this.mTextView.getSelectionStart();
            int currentLineAdjustedForSlop = Editor.this.getCurrentLineAdjustedForSlop(layout, this.mPreviousLineTouched, f2);
            int offsetAtCoordinate = getOffsetAtCoordinate(layout, currentLineAdjustedForSlop, f);
            if ((isStartHandle() && offsetAtCoordinate >= selectionEnd) || (!isStartHandle() && offsetAtCoordinate <= selectionEnd)) {
                currentLineAdjustedForSlop = layout.getLineForOffset(selectionEnd);
                offsetAtCoordinate = getOffsetAtCoordinate(layout, currentLineAdjustedForSlop, f);
            }
            int wordEnd = Editor.this.getWordEnd(offsetAtCoordinate);
            int wordStart = Editor.this.getWordStart(offsetAtCoordinate);
            if (this.mPrevX == -1.0f) {
                this.mPrevX = f;
            }
            int currentCursorOffset = getCurrentCursorOffset();
            boolean zIsAtRtlRun = isAtRtlRun(layout, currentCursorOffset);
            boolean zIsAtRtlRun2 = isAtRtlRun(layout, offsetAtCoordinate);
            boolean zIsLevelBoundary = layout.isLevelBoundary(offsetAtCoordinate);
            boolean z3 = true;
            if (zIsLevelBoundary || ((zIsAtRtlRun && !zIsAtRtlRun2) || (!zIsAtRtlRun && zIsAtRtlRun2))) {
                this.mLanguageDirectionChanged = true;
                this.mTouchWordDelta = 0.0f;
                positionAndAdjustForCrossingHandles(offsetAtCoordinate, z);
                return;
            }
            boolean z4 = false;
            if (this.mLanguageDirectionChanged && !zIsLevelBoundary) {
                positionAndAdjustForCrossingHandles(offsetAtCoordinate, z);
                this.mTouchWordDelta = 0.0f;
                this.mLanguageDirectionChanged = false;
                return;
            }
            float f3 = f - this.mPrevX;
            boolean z5 = !isStartHandle() ? currentLineAdjustedForSlop <= this.mPreviousLineTouched : currentLineAdjustedForSlop >= this.mPreviousLineTouched;
            if (zIsAtRtlRun2 == isStartHandle()) {
                z2 = (f3 > 0.0f) | z5;
            } else {
                z2 = (f3 < 0.0f) | z5;
            }
            if (Editor.this.mTextView.getHorizontallyScrolling() && positionNearEdgeOfScrollingView(f, zIsAtRtlRun2)) {
                if (!isStartHandle() || Editor.this.mTextView.getScrollX() == 0) {
                    if (!isStartHandle()) {
                        if (Editor.this.mTextView.canScrollHorizontally(zIsAtRtlRun2 ? -1 : 1)) {
                            if ((z2 && ((isStartHandle() && offsetAtCoordinate < currentCursorOffset) || (!isStartHandle() && offsetAtCoordinate > currentCursorOffset))) || !z2) {
                                this.mTouchWordDelta = 0.0f;
                                if (zIsAtRtlRun2 == isStartHandle()) {
                                    offsetToLeftOf = layout.getOffsetToRightOf(this.mPreviousOffset);
                                } else {
                                    offsetToLeftOf = layout.getOffsetToLeftOf(this.mPreviousOffset);
                                }
                                positionAndAdjustForCrossingHandles(offsetToLeftOf, z);
                                return;
                            }
                        }
                    }
                }
            }
            if (z2) {
                int lineStart = isStartHandle() ? wordStart : wordEnd;
                if ((!this.mInWord || (!isStartHandle() ? currentLineAdjustedForSlop > this.mPrevLine : currentLineAdjustedForSlop < this.mPrevLine)) && zIsAtRtlRun2 == isAtRtlRun(layout, lineStart)) {
                    z4 = true;
                }
                if (z4) {
                    if (layout.getLineForOffset(lineStart) != currentLineAdjustedForSlop) {
                        lineStart = isStartHandle() ? layout.getLineStart(currentLineAdjustedForSlop) : layout.getLineEnd(currentLineAdjustedForSlop);
                    }
                    if (isStartHandle()) {
                        i = wordEnd - ((wordEnd - lineStart) / 2);
                    } else {
                        i = ((lineStart - wordStart) / 2) + wordStart;
                    }
                    if (!isStartHandle() || (offsetAtCoordinate > i && currentLineAdjustedForSlop >= this.mPrevLine)) {
                        if (isStartHandle() || (offsetAtCoordinate < i && currentLineAdjustedForSlop <= this.mPrevLine)) {
                            wordEnd = this.mPreviousOffset;
                        }
                    } else {
                        wordEnd = wordStart;
                    }
                } else {
                    wordEnd = offsetAtCoordinate;
                }
                if ((isStartHandle() && wordEnd < offsetAtCoordinate) || (!isStartHandle() && wordEnd > offsetAtCoordinate)) {
                    this.mTouchWordDelta = Editor.this.mTextView.convertToLocalHorizontalCoordinate(f) - getHorizontal(layout, wordEnd);
                } else {
                    this.mTouchWordDelta = 0.0f;
                }
                offsetAtCoordinate = wordEnd;
            } else {
                int offsetAtCoordinate2 = getOffsetAtCoordinate(layout, currentLineAdjustedForSlop, f - this.mTouchWordDelta);
                if (!isStartHandle() ? !(offsetAtCoordinate2 < this.mPreviousOffset || currentLineAdjustedForSlop < this.mPrevLine) : !(offsetAtCoordinate2 > this.mPreviousOffset || currentLineAdjustedForSlop > this.mPrevLine)) {
                    if (currentLineAdjustedForSlop != this.mPrevLine) {
                        if (isStartHandle()) {
                            wordEnd = wordStart;
                        }
                        if ((isStartHandle() && wordEnd < offsetAtCoordinate) || (!isStartHandle() && wordEnd > offsetAtCoordinate)) {
                            this.mTouchWordDelta = Editor.this.mTextView.convertToLocalHorizontalCoordinate(f) - getHorizontal(layout, wordEnd);
                        } else {
                            this.mTouchWordDelta = 0.0f;
                        }
                        offsetAtCoordinate = wordEnd;
                    } else {
                        offsetAtCoordinate = offsetAtCoordinate2;
                    }
                } else {
                    if ((isStartHandle() && offsetAtCoordinate2 < this.mPreviousOffset) || (!isStartHandle() && offsetAtCoordinate2 > this.mPreviousOffset)) {
                        this.mTouchWordDelta = Editor.this.mTextView.convertToLocalHorizontalCoordinate(f) - getHorizontal(layout, this.mPreviousOffset);
                    }
                    z3 = false;
                }
            }
            if (z3) {
                this.mPreviousLineTouched = currentLineAdjustedForSlop;
                positionAndAdjustForCrossingHandles(offsetAtCoordinate, z);
            }
            this.mPrevX = f;
        }

        @Override
        protected void positionAtCursorOffset(int i, boolean z, boolean z2) {
            super.positionAtCursorOffset(i, z, z2);
            this.mInWord = (i == -1 || Editor.this.getWordIteratorWithText().isBoundary(i)) ? false : true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            boolean zOnTouchEvent = super.onTouchEvent(motionEvent);
            switch (motionEvent.getActionMasked()) {
                case 0:
                    this.mTouchWordDelta = 0.0f;
                    this.mPrevX = -1.0f;
                    updateMagnifier(motionEvent);
                    return zOnTouchEvent;
                case 1:
                case 3:
                    dismissMagnifier();
                    return zOnTouchEvent;
                case 2:
                    updateMagnifier(motionEvent);
                    return zOnTouchEvent;
                default:
                    return zOnTouchEvent;
            }
        }

        private void positionAndAdjustForCrossingHandles(int i, boolean z) {
            int iUnpackRangeEndFromLong;
            int selectionEnd = isStartHandle() ? Editor.this.mTextView.getSelectionEnd() : Editor.this.mTextView.getSelectionStart();
            if ((isStartHandle() && i >= selectionEnd) || (!isStartHandle() && i <= selectionEnd)) {
                this.mTouchWordDelta = 0.0f;
                Layout layout = Editor.this.mTextView.getLayout();
                if (layout != null && i != selectionEnd) {
                    float horizontal = getHorizontal(layout, i);
                    float horizontal2 = getHorizontal(layout, selectionEnd, !isStartHandle());
                    float horizontal3 = getHorizontal(layout, this.mPreviousOffset);
                    if ((horizontal3 < horizontal2 && horizontal < horizontal2) || (horizontal3 > horizontal2 && horizontal > horizontal2)) {
                        int currentCursorOffset = getCurrentCursorOffset();
                        if (!isStartHandle()) {
                            currentCursorOffset = Math.max(currentCursorOffset - 1, 0);
                        }
                        long runRange = layout.getRunRange(currentCursorOffset);
                        if (isStartHandle()) {
                            iUnpackRangeEndFromLong = TextUtils.unpackRangeStartFromLong(runRange);
                        } else {
                            iUnpackRangeEndFromLong = TextUtils.unpackRangeEndFromLong(runRange);
                        }
                        positionAtCursorOffset(iUnpackRangeEndFromLong, false, z);
                        return;
                    }
                }
                i = Editor.this.getNextCursorOffset(selectionEnd, !isStartHandle());
            }
            positionAtCursorOffset(i, false, z);
        }

        private boolean positionNearEdgeOfScrollingView(float f, boolean z) {
            Editor.this.mTextView.getLocationOnScreen(this.mTextViewLocation);
            return z == isStartHandle() ? f > ((float) ((this.mTextViewLocation[0] + Editor.this.mTextView.getWidth()) - Editor.this.mTextView.getPaddingRight())) - this.mTextViewEdgeSlop : f < ((float) (this.mTextViewLocation[0] + Editor.this.mTextView.getPaddingLeft())) + this.mTextViewEdgeSlop;
        }

        @Override
        protected boolean isAtRtlRun(Layout layout, int i) {
            if (!isStartHandle()) {
                i = Math.max(i - 1, 0);
            }
            return layout.isRtlCharAt(i);
        }

        @Override
        public float getHorizontal(Layout layout, int i) {
            return getHorizontal(layout, i, isStartHandle());
        }

        private float getHorizontal(Layout layout, int i, boolean z) {
            int iMax;
            int lineForOffset = layout.getLineForOffset(i);
            boolean z2 = false;
            if (!z) {
                iMax = Math.max(i - 1, 0);
            } else {
                iMax = i;
            }
            boolean zIsRtlCharAt = layout.isRtlCharAt(iMax);
            if (layout.getParagraphDirection(lineForOffset) == -1) {
                z2 = true;
            }
            return zIsRtlCharAt == z2 ? layout.getPrimaryHorizontal(i) : layout.getSecondaryHorizontal(i);
        }

        @Override
        protected int getOffsetAtCoordinate(Layout layout, int i, float f) {
            float fConvertToLocalHorizontalCoordinate = Editor.this.mTextView.convertToLocalHorizontalCoordinate(f);
            boolean z = true;
            int offsetForHorizontal = layout.getOffsetForHorizontal(i, fConvertToLocalHorizontalCoordinate, true);
            if (!layout.isLevelBoundary(offsetForHorizontal)) {
                return offsetForHorizontal;
            }
            int offsetForHorizontal2 = layout.getOffsetForHorizontal(i, fConvertToLocalHorizontalCoordinate, false);
            int currentCursorOffset = getCurrentCursorOffset();
            int iAbs = Math.abs(offsetForHorizontal - currentCursorOffset);
            int iAbs2 = Math.abs(offsetForHorizontal2 - currentCursorOffset);
            if (iAbs < iAbs2) {
                return offsetForHorizontal;
            }
            if (iAbs > iAbs2) {
                return offsetForHorizontal2;
            }
            if (!isStartHandle()) {
                currentCursorOffset = Math.max(currentCursorOffset - 1, 0);
            }
            boolean zIsRtlCharAt = layout.isRtlCharAt(currentCursorOffset);
            if (layout.getParagraphDirection(i) != -1) {
                z = false;
            }
            return zIsRtlCharAt == z ? offsetForHorizontal : offsetForHorizontal2;
        }

        @Override
        protected int getMagnifierHandleTrigger() {
            if (isStartHandle()) {
                return 1;
            }
            return 2;
        }
    }

    private int getCurrentLineAdjustedForSlop(Layout layout, int i, float f) {
        int lineAtCoordinate = this.mTextView.getLineAtCoordinate(f);
        if (layout == null || i > layout.getLineCount() || layout.getLineCount() <= 0 || i < 0 || Math.abs(lineAtCoordinate - i) >= 2) {
            return lineAtCoordinate;
        }
        float fViewportToContentVerticalOffset = this.mTextView.viewportToContentVerticalOffset();
        int lineCount = layout.getLineCount();
        float lineHeight = this.mTextView.getLineHeight() * LINE_SLOP_MULTIPLIER_FOR_HANDLEVIEWS;
        float fMax = Math.max((layout.getLineTop(i) + fViewportToContentVerticalOffset) - lineHeight, layout.getLineTop(0) + fViewportToContentVerticalOffset + lineHeight);
        int i2 = lineCount - 1;
        float fMin = Math.min(layout.getLineBottom(i) + fViewportToContentVerticalOffset + lineHeight, (layout.getLineBottom(i2) + fViewportToContentVerticalOffset) - lineHeight);
        if (f <= fMax) {
            return Math.max(i - 1, 0);
        }
        if (f >= fMin) {
            return Math.min(i + 1, i2);
        }
        return i;
    }

    private class InsertionPointCursorController implements CursorController {
        private InsertionHandleView mHandle;

        private InsertionPointCursorController() {
        }

        @Override
        public void show() {
            getHandle().show();
            if (Editor.this.mSelectionModifierCursorController != null) {
                Editor.this.mSelectionModifierCursorController.hide();
            }
        }

        @Override
        public void hide() {
            if (this.mHandle != null) {
                this.mHandle.hide();
            }
        }

        @Override
        public void onTouchModeChanged(boolean z) {
            if (!z) {
                hide();
            }
        }

        private InsertionHandleView getHandle() {
            if (Editor.this.mSelectHandleCenter == null) {
                Editor.this.mSelectHandleCenter = Editor.this.mTextView.getContext().getDrawable(Editor.this.mTextView.mTextSelectHandleRes);
            }
            if (this.mHandle == null) {
                this.mHandle = Editor.this.new InsertionHandleView(Editor.this.mSelectHandleCenter);
            }
            return this.mHandle;
        }

        @Override
        public void onDetached() {
            Editor.this.mTextView.getViewTreeObserver().removeOnTouchModeChangeListener(this);
            if (this.mHandle != null) {
                this.mHandle.onDetached();
            }
        }

        @Override
        public boolean isCursorBeingModified() {
            return this.mHandle != null && this.mHandle.isDragging();
        }

        @Override
        public boolean isActive() {
            return this.mHandle != null && this.mHandle.isShowing();
        }

        public void invalidateHandle() {
            if (this.mHandle != null) {
                this.mHandle.invalidate();
            }
        }
    }

    class SelectionModifierCursorController implements CursorController {
        private static final int DRAG_ACCELERATOR_MODE_CHARACTER = 1;
        private static final int DRAG_ACCELERATOR_MODE_INACTIVE = 0;
        private static final int DRAG_ACCELERATOR_MODE_PARAGRAPH = 3;
        private static final int DRAG_ACCELERATOR_MODE_WORD = 2;
        private float mDownPositionX;
        private float mDownPositionY;
        private SelectionHandleView mEndHandle;
        private boolean mGestureStayedInTapRegion;
        private boolean mHaventMovedEnoughToStartDrag;
        private int mMaxTouchOffset;
        private int mMinTouchOffset;
        private SelectionHandleView mStartHandle;
        private int mStartOffset = -1;
        private int mLineSelectionIsOn = -1;
        private boolean mSwitchedLines = false;
        private int mDragAcceleratorMode = 0;

        SelectionModifierCursorController() {
            resetTouchOffsets();
        }

        @Override
        public void show() {
            if (Editor.this.mTextView.isInBatchEditMode()) {
                return;
            }
            initDrawables();
            initHandles();
        }

        private void initDrawables() {
            if (Editor.this.mSelectHandleLeft == null) {
                Editor.this.mSelectHandleLeft = Editor.this.mTextView.getContext().getDrawable(Editor.this.mTextView.mTextSelectHandleLeftRes);
            }
            if (Editor.this.mSelectHandleRight == null) {
                Editor.this.mSelectHandleRight = Editor.this.mTextView.getContext().getDrawable(Editor.this.mTextView.mTextSelectHandleRightRes);
            }
        }

        private void initHandles() {
            if (this.mStartHandle == null) {
                this.mStartHandle = Editor.this.new SelectionHandleView(Editor.this.mSelectHandleLeft, Editor.this.mSelectHandleRight, R.id.selection_start_handle, 0);
            }
            if (this.mEndHandle == null) {
                this.mEndHandle = Editor.this.new SelectionHandleView(Editor.this.mSelectHandleRight, Editor.this.mSelectHandleLeft, R.id.selection_end_handle, 1);
            }
            this.mStartHandle.show();
            this.mEndHandle.show();
            Editor.this.hideInsertionPointCursorController();
        }

        @Override
        public void hide() {
            if (this.mStartHandle != null) {
                this.mStartHandle.hide();
            }
            if (this.mEndHandle != null) {
                this.mEndHandle.hide();
            }
        }

        public void enterDrag(int i) {
            show();
            this.mDragAcceleratorMode = i;
            this.mStartOffset = Editor.this.mTextView.getOffsetForPosition(Editor.this.mLastDownPositionX, Editor.this.mLastDownPositionY);
            this.mLineSelectionIsOn = Editor.this.mTextView.getLineAtCoordinate(Editor.this.mLastDownPositionY);
            hide();
            Editor.this.mTextView.getParent().requestDisallowInterceptTouchEvent(true);
            Editor.this.mTextView.cancelLongPress();
        }

        public void onTouchEvent(MotionEvent motionEvent) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            boolean zIsFromSource = motionEvent.isFromSource(8194);
            switch (motionEvent.getActionMasked()) {
                case 0:
                    if (!Editor.this.extractedTextModeWillBeStarted()) {
                        int offsetForPosition = Editor.this.mTextView.getOffsetForPosition(x, y);
                        this.mMaxTouchOffset = offsetForPosition;
                        this.mMinTouchOffset = offsetForPosition;
                        if (this.mGestureStayedInTapRegion && (Editor.this.mTapState == 2 || Editor.this.mTapState == 3)) {
                            float f = x - this.mDownPositionX;
                            float f2 = y - this.mDownPositionY;
                            float f3 = (f * f) + (f2 * f2);
                            int scaledDoubleTapSlop = ViewConfiguration.get(Editor.this.mTextView.getContext()).getScaledDoubleTapSlop();
                            if ((f3 < ((float) (scaledDoubleTapSlop * scaledDoubleTapSlop))) && (zIsFromSource || Editor.this.isPositionOnText(x, y))) {
                                if (Editor.this.mTapState == 2) {
                                    Editor.this.selectCurrentWordAndStartDrag();
                                } else if (Editor.this.mTapState == 3) {
                                    selectCurrentParagraphAndStartDrag();
                                }
                                Editor.this.mDiscardNextActionUp = true;
                            }
                        }
                        this.mDownPositionX = x;
                        this.mDownPositionY = y;
                        this.mGestureStayedInTapRegion = true;
                        this.mHaventMovedEnoughToStartDrag = true;
                    } else {
                        hide();
                    }
                    break;
                case 1:
                    if (isDragAcceleratorActive()) {
                        updateSelection(motionEvent);
                        Editor.this.mTextView.getParent().requestDisallowInterceptTouchEvent(false);
                        resetDragAcceleratorState();
                        if (Editor.this.mTextView.hasSelection()) {
                            Editor.this.startSelectionActionModeAsync(this.mHaventMovedEnoughToStartDrag);
                        }
                        break;
                    }
                    break;
                case 2:
                    ViewConfiguration viewConfiguration = ViewConfiguration.get(Editor.this.mTextView.getContext());
                    int scaledTouchSlop = viewConfiguration.getScaledTouchSlop();
                    if (this.mGestureStayedInTapRegion || this.mHaventMovedEnoughToStartDrag) {
                        float f4 = x - this.mDownPositionX;
                        float f5 = y - this.mDownPositionY;
                        float f6 = (f4 * f4) + (f5 * f5);
                        if (this.mGestureStayedInTapRegion) {
                            int scaledDoubleTapTouchSlop = viewConfiguration.getScaledDoubleTapTouchSlop();
                            this.mGestureStayedInTapRegion = f6 <= ((float) (scaledDoubleTapTouchSlop * scaledDoubleTapTouchSlop));
                        }
                        if (this.mHaventMovedEnoughToStartDrag) {
                            this.mHaventMovedEnoughToStartDrag = f6 <= ((float) (scaledTouchSlop * scaledTouchSlop));
                        }
                    }
                    if (zIsFromSource && !isDragAcceleratorActive()) {
                        int offsetForPosition2 = Editor.this.mTextView.getOffsetForPosition(x, y);
                        if (Editor.this.mTextView.hasSelection() && ((!this.mHaventMovedEnoughToStartDrag || this.mStartOffset != offsetForPosition2) && offsetForPosition2 >= Editor.this.mTextView.getSelectionStart() && offsetForPosition2 <= Editor.this.mTextView.getSelectionEnd())) {
                            Editor.this.startDragAndDrop();
                        } else if (this.mStartOffset != offsetForPosition2) {
                            Editor.this.stopTextActionMode();
                            enterDrag(1);
                            Editor.this.mDiscardNextActionUp = true;
                            this.mHaventMovedEnoughToStartDrag = false;
                        }
                    }
                    if (this.mStartHandle == null || !this.mStartHandle.isShowing()) {
                        updateSelection(motionEvent);
                    }
                    break;
                case 5:
                case 6:
                    if (Editor.this.mTextView.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)) {
                        updateMinAndMaxOffsets(motionEvent);
                    }
                    break;
            }
        }

        private void updateSelection(MotionEvent motionEvent) {
            if (Editor.this.mTextView.getLayout() != null) {
                switch (this.mDragAcceleratorMode) {
                    case 1:
                        updateCharacterBasedSelection(motionEvent);
                        break;
                    case 2:
                        updateWordBasedSelection(motionEvent);
                        break;
                    case 3:
                        updateParagraphBasedSelection(motionEvent);
                        break;
                }
            }
        }

        private boolean selectCurrentParagraphAndStartDrag() {
            if (Editor.this.mInsertionActionModeRunnable != null) {
                Editor.this.mTextView.removeCallbacks(Editor.this.mInsertionActionModeRunnable);
            }
            Editor.this.stopTextActionMode();
            if (!Editor.this.selectCurrentParagraph()) {
                return false;
            }
            enterDrag(3);
            return true;
        }

        private void updateCharacterBasedSelection(MotionEvent motionEvent) {
            updateSelectionInternal(this.mStartOffset, Editor.this.mTextView.getOffsetForPosition(motionEvent.getX(), motionEvent.getY()), motionEvent.isFromSource(4098));
        }

        private void updateWordBasedSelection(MotionEvent motionEvent) {
            int currentLineAdjustedForSlop;
            float idealVerticalOffset;
            int wordStart;
            int wordEnd;
            if (this.mHaventMovedEnoughToStartDrag) {
                return;
            }
            boolean zIsFromSource = motionEvent.isFromSource(8194);
            ViewConfiguration viewConfiguration = ViewConfiguration.get(Editor.this.mTextView.getContext());
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            if (zIsFromSource) {
                currentLineAdjustedForSlop = Editor.this.mTextView.getLineAtCoordinate(y);
            } else {
                if (this.mSwitchedLines) {
                    int scaledTouchSlop = viewConfiguration.getScaledTouchSlop();
                    if (this.mStartHandle != null) {
                        idealVerticalOffset = this.mStartHandle.getIdealVerticalOffset();
                    } else {
                        idealVerticalOffset = scaledTouchSlop;
                    }
                    y -= idealVerticalOffset;
                }
                currentLineAdjustedForSlop = Editor.this.getCurrentLineAdjustedForSlop(Editor.this.mTextView.getLayout(), this.mLineSelectionIsOn, y);
                if (!this.mSwitchedLines && currentLineAdjustedForSlop != this.mLineSelectionIsOn) {
                    this.mSwitchedLines = true;
                    return;
                }
            }
            int offsetAtCoordinate = Editor.this.mTextView.getOffsetAtCoordinate(currentLineAdjustedForSlop, x);
            if (this.mStartOffset < offsetAtCoordinate) {
                wordStart = Editor.this.getWordEnd(offsetAtCoordinate);
                wordEnd = Editor.this.getWordStart(this.mStartOffset);
            } else {
                wordStart = Editor.this.getWordStart(offsetAtCoordinate);
                wordEnd = Editor.this.getWordEnd(this.mStartOffset);
                if (wordEnd == wordStart) {
                    wordStart = Editor.this.getNextCursorOffset(wordStart, false);
                }
            }
            this.mLineSelectionIsOn = currentLineAdjustedForSlop;
            updateSelectionInternal(wordEnd, wordStart, motionEvent.isFromSource(4098));
        }

        private void updateParagraphBasedSelection(MotionEvent motionEvent) {
            int offsetForPosition = Editor.this.mTextView.getOffsetForPosition(motionEvent.getX(), motionEvent.getY());
            long paragraphsRange = Editor.this.getParagraphsRange(Math.min(offsetForPosition, this.mStartOffset), Math.max(offsetForPosition, this.mStartOffset));
            updateSelectionInternal(TextUtils.unpackRangeStartFromLong(paragraphsRange), TextUtils.unpackRangeEndFromLong(paragraphsRange), motionEvent.isFromSource(4098));
        }

        private void updateSelectionInternal(int i, int i2, boolean z) {
            boolean z2 = z && Editor.this.mHapticTextHandleEnabled && !(Editor.this.mTextView.getSelectionStart() == i && Editor.this.mTextView.getSelectionEnd() == i2);
            Selection.setSelection((Spannable) Editor.this.mTextView.getText(), i, i2);
            if (z2) {
                Editor.this.mTextView.performHapticFeedback(9);
            }
        }

        private void updateMinAndMaxOffsets(MotionEvent motionEvent) {
            int pointerCount = motionEvent.getPointerCount();
            for (int i = 0; i < pointerCount; i++) {
                int offsetForPosition = Editor.this.mTextView.getOffsetForPosition(motionEvent.getX(i), motionEvent.getY(i));
                if (offsetForPosition < this.mMinTouchOffset) {
                    this.mMinTouchOffset = offsetForPosition;
                }
                if (offsetForPosition > this.mMaxTouchOffset) {
                    this.mMaxTouchOffset = offsetForPosition;
                }
            }
        }

        public int getMinTouchOffset() {
            return this.mMinTouchOffset;
        }

        public int getMaxTouchOffset() {
            return this.mMaxTouchOffset;
        }

        public void resetTouchOffsets() {
            this.mMaxTouchOffset = -1;
            this.mMinTouchOffset = -1;
            resetDragAcceleratorState();
        }

        private void resetDragAcceleratorState() {
            this.mStartOffset = -1;
            this.mDragAcceleratorMode = 0;
            this.mSwitchedLines = false;
            int selectionStart = Editor.this.mTextView.getSelectionStart();
            int selectionEnd = Editor.this.mTextView.getSelectionEnd();
            if (selectionStart < 0 || selectionEnd < 0) {
                Selection.removeSelection((Spannable) Editor.this.mTextView.getText());
            } else if (selectionStart > selectionEnd) {
                Selection.setSelection((Spannable) Editor.this.mTextView.getText(), selectionEnd, selectionStart);
            }
        }

        public boolean isSelectionStartDragged() {
            return this.mStartHandle != null && this.mStartHandle.isDragging();
        }

        @Override
        public boolean isCursorBeingModified() {
            return isDragAcceleratorActive() || isSelectionStartDragged() || (this.mEndHandle != null && this.mEndHandle.isDragging());
        }

        public boolean isDragAcceleratorActive() {
            return this.mDragAcceleratorMode != 0;
        }

        @Override
        public void onTouchModeChanged(boolean z) {
            if (!z) {
                hide();
            }
        }

        @Override
        public void onDetached() {
            Editor.this.mTextView.getViewTreeObserver().removeOnTouchModeChangeListener(this);
            if (this.mStartHandle != null) {
                this.mStartHandle.onDetached();
            }
            if (this.mEndHandle != null) {
                this.mEndHandle.onDetached();
            }
        }

        @Override
        public boolean isActive() {
            return this.mStartHandle != null && this.mStartHandle.isShowing();
        }

        public void invalidateHandles() {
            if (this.mStartHandle != null) {
                this.mStartHandle.invalidate();
            }
            if (this.mEndHandle != null) {
                this.mEndHandle.invalidate();
            }
        }
    }

    private class CorrectionHighlighter {
        private static final int FADE_OUT_DURATION = 400;
        private int mEnd;
        private long mFadingStartTime;
        private int mStart;
        private RectF mTempRectF;
        private final Path mPath = new Path();
        private final Paint mPaint = new Paint(1);

        public CorrectionHighlighter() {
            this.mPaint.setCompatibilityScaling(Editor.this.mTextView.getResources().getCompatibilityInfo().applicationScale);
            this.mPaint.setStyle(Paint.Style.FILL);
        }

        public void highlight(CorrectionInfo correctionInfo) {
            this.mStart = correctionInfo.getOffset();
            this.mEnd = this.mStart + correctionInfo.getNewText().length();
            this.mFadingStartTime = SystemClock.uptimeMillis();
            if (this.mStart < 0 || this.mEnd < 0) {
                stopAnimation();
            }
        }

        public void draw(Canvas canvas, int i) {
            if (updatePath() && updatePaint()) {
                if (i != 0) {
                    canvas.translate(0.0f, i);
                }
                canvas.drawPath(this.mPath, this.mPaint);
                if (i != 0) {
                    canvas.translate(0.0f, -i);
                }
                invalidate(true);
                return;
            }
            stopAnimation();
            invalidate(false);
        }

        private boolean updatePaint() {
            long jUptimeMillis = SystemClock.uptimeMillis() - this.mFadingStartTime;
            if (jUptimeMillis > 400) {
                return false;
            }
            this.mPaint.setColor((Editor.this.mTextView.mHighlightColor & 16777215) + (((int) (Color.alpha(Editor.this.mTextView.mHighlightColor) * (1.0f - (jUptimeMillis / 400.0f)))) << 24));
            return true;
        }

        private boolean updatePath() {
            Layout layout = Editor.this.mTextView.getLayout();
            if (layout == null) {
                return false;
            }
            int length = Editor.this.mTextView.getText().length();
            int iMin = Math.min(length, this.mStart);
            int iMin2 = Math.min(length, this.mEnd);
            this.mPath.reset();
            layout.getSelectionPath(iMin, iMin2, this.mPath);
            return true;
        }

        private void invalidate(boolean z) {
            if (Editor.this.mTextView.getLayout() == null) {
                return;
            }
            if (this.mTempRectF == null) {
                this.mTempRectF = new RectF();
            }
            this.mPath.computeBounds(this.mTempRectF, false);
            int compoundPaddingLeft = Editor.this.mTextView.getCompoundPaddingLeft();
            int extendedPaddingTop = Editor.this.mTextView.getExtendedPaddingTop() + Editor.this.mTextView.getVerticalOffset(true);
            if (z) {
                Editor.this.mTextView.postInvalidateOnAnimation(((int) this.mTempRectF.left) + compoundPaddingLeft, ((int) this.mTempRectF.top) + extendedPaddingTop, compoundPaddingLeft + ((int) this.mTempRectF.right), extendedPaddingTop + ((int) this.mTempRectF.bottom));
            } else {
                Editor.this.mTextView.postInvalidate((int) this.mTempRectF.left, (int) this.mTempRectF.top, (int) this.mTempRectF.right, (int) this.mTempRectF.bottom);
            }
        }

        private void stopAnimation() {
            Editor.this.mCorrectionHighlighter = null;
        }
    }

    private static class ErrorPopup extends PopupWindow {
        private boolean mAbove;
        private int mPopupInlineErrorAboveBackgroundId;
        private int mPopupInlineErrorBackgroundId;
        private final TextView mView;

        ErrorPopup(TextView textView, int i, int i2) {
            super(textView, i, i2);
            this.mAbove = false;
            this.mPopupInlineErrorBackgroundId = 0;
            this.mPopupInlineErrorAboveBackgroundId = 0;
            this.mView = textView;
            this.mPopupInlineErrorBackgroundId = getResourceId(this.mPopupInlineErrorBackgroundId, 293);
            this.mView.setBackgroundResource(this.mPopupInlineErrorBackgroundId);
        }

        void fixDirection(boolean z) {
            this.mAbove = z;
            if (z) {
                this.mPopupInlineErrorAboveBackgroundId = getResourceId(this.mPopupInlineErrorAboveBackgroundId, 292);
            } else {
                this.mPopupInlineErrorBackgroundId = getResourceId(this.mPopupInlineErrorBackgroundId, 293);
            }
            this.mView.setBackgroundResource(z ? this.mPopupInlineErrorAboveBackgroundId : this.mPopupInlineErrorBackgroundId);
        }

        private int getResourceId(int i, int i2) {
            if (i != 0) {
                return i;
            }
            TypedArray typedArrayObtainStyledAttributes = this.mView.getContext().obtainStyledAttributes(android.R.styleable.Theme);
            int resourceId = typedArrayObtainStyledAttributes.getResourceId(i2, 0);
            typedArrayObtainStyledAttributes.recycle();
            return resourceId;
        }

        @Override
        public void update(int i, int i2, int i3, int i4, boolean z) {
            super.update(i, i2, i3, i4, z);
            boolean zIsAboveAnchor = isAboveAnchor();
            if (zIsAboveAnchor != this.mAbove) {
                fixDirection(zIsAboveAnchor);
            }
        }
    }

    static class InputContentType {
        boolean enterDown;
        Bundle extras;
        int imeActionId;
        CharSequence imeActionLabel;
        LocaleList imeHintLocales;
        int imeOptions = 0;
        TextView.OnEditorActionListener onEditorActionListener;
        String privateImeOptions;

        InputContentType() {
        }
    }

    static class InputMethodState {
        int mBatchEditNesting;
        int mChangedDelta;
        int mChangedEnd;
        int mChangedStart;
        boolean mContentChanged;
        boolean mCursorChanged;
        final ExtractedText mExtractedText = new ExtractedText();
        ExtractedTextRequest mExtractedTextRequest;
        boolean mSelectionModeChanged;

        InputMethodState() {
        }
    }

    private static boolean isValidRange(CharSequence charSequence, int i, int i2) {
        return i >= 0 && i <= i2 && i2 <= charSequence.length();
    }

    @VisibleForTesting
    public SuggestionsPopupWindow getSuggestionsPopupWindowForTesting() {
        return this.mSuggestionsPopupWindow;
    }

    public static class UndoInputFilter implements InputFilter {
        private static final int MERGE_EDIT_MODE_FORCE_MERGE = 0;
        private static final int MERGE_EDIT_MODE_NEVER_MERGE = 1;
        private static final int MERGE_EDIT_MODE_NORMAL = 2;
        private final Editor mEditor;
        private boolean mExpanding;
        private boolean mHasComposition;
        private boolean mIsUserEdit;
        private boolean mPreviousOperationWasInSameBatchEdit;

        @Retention(RetentionPolicy.SOURCE)
        private @interface MergeMode {
        }

        public UndoInputFilter(Editor editor) {
            this.mEditor = editor;
        }

        public void saveInstanceState(Parcel parcel) {
            parcel.writeInt(this.mIsUserEdit ? 1 : 0);
            parcel.writeInt(this.mHasComposition ? 1 : 0);
            parcel.writeInt(this.mExpanding ? 1 : 0);
            parcel.writeInt(this.mPreviousOperationWasInSameBatchEdit ? 1 : 0);
        }

        public void restoreInstanceState(Parcel parcel) {
            this.mIsUserEdit = parcel.readInt() != 0;
            this.mHasComposition = parcel.readInt() != 0;
            this.mExpanding = parcel.readInt() != 0;
            this.mPreviousOperationWasInSameBatchEdit = parcel.readInt() != 0;
        }

        public void beginBatchEdit() {
            this.mIsUserEdit = true;
        }

        public void endBatchEdit() {
            this.mIsUserEdit = false;
            this.mPreviousOperationWasInSameBatchEdit = false;
        }

        @Override
        public CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4) {
            if (!canUndoEdit(charSequence, i, i2, spanned, i3, i4)) {
                return null;
            }
            boolean z = this.mHasComposition;
            this.mHasComposition = isComposition(charSequence);
            boolean z2 = this.mExpanding;
            int i5 = i2 - i;
            int i6 = i4 - i3;
            boolean z3 = true;
            if (i5 != i6) {
                this.mExpanding = i5 > i6;
                if (!z || this.mExpanding == z2) {
                }
            } else {
                z3 = false;
            }
            handleEdit(charSequence, i, i2, spanned, i3, i4, z3);
            return null;
        }

        void freezeLastEdit() {
            this.mEditor.mUndoManager.beginUpdate("Edit text");
            EditOperation lastEdit = getLastEdit();
            if (lastEdit != null) {
                lastEdit.mFrozen = true;
            }
            this.mEditor.mUndoManager.endUpdate();
        }

        private void handleEdit(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4, boolean z) {
            int i5;
            if (isInTextWatcher() || this.mPreviousOperationWasInSameBatchEdit) {
                i5 = 0;
            } else if (z) {
                i5 = 1;
            } else {
                i5 = 2;
            }
            String strSubstring = TextUtils.substring(charSequence, i, i2);
            EditOperation editOperation = new EditOperation(this.mEditor, TextUtils.substring(spanned, i3, i4), i3, strSubstring, this.mHasComposition);
            if (this.mHasComposition && TextUtils.equals(editOperation.mNewText, editOperation.mOldText)) {
                return;
            }
            recordEdit(editOperation, i5);
        }

        private EditOperation getLastEdit() {
            return (EditOperation) this.mEditor.mUndoManager.getLastOperation(EditOperation.class, this.mEditor.mUndoOwner, 1);
        }

        private void recordEdit(EditOperation editOperation, int i) {
            UndoManager undoManager = this.mEditor.mUndoManager;
            undoManager.beginUpdate("Edit text");
            EditOperation lastEdit = getLastEdit();
            if (lastEdit == null) {
                undoManager.addOperation(editOperation, 0);
            } else if (i == 0) {
                lastEdit.forceMergeWith(editOperation);
            } else if (!this.mIsUserEdit || i != 2 || !lastEdit.mergeWith(editOperation)) {
                undoManager.commitState(this.mEditor.mUndoOwner);
                undoManager.addOperation(editOperation, 0);
            }
            this.mPreviousOperationWasInSameBatchEdit = this.mIsUserEdit;
            undoManager.endUpdate();
        }

        private boolean canUndoEdit(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4) {
            if (this.mEditor.mAllowUndo && !this.mEditor.mUndoManager.isInUndo() && Editor.isValidRange(charSequence, i, i2) && Editor.isValidRange(spanned, i3, i4)) {
                return (i == i2 && i3 == i4) ? false : true;
            }
            return false;
        }

        private static boolean isComposition(CharSequence charSequence) {
            if (!(charSequence instanceof Spannable)) {
                return false;
            }
            Spannable spannable = (Spannable) charSequence;
            return EditableInputConnection.getComposingSpanStart(spannable) < EditableInputConnection.getComposingSpanEnd(spannable);
        }

        private boolean isInTextWatcher() {
            CharSequence text = this.mEditor.mTextView.getText();
            return (text instanceof SpannableStringBuilder) && ((SpannableStringBuilder) text).getTextWatcherDepth() > 0;
        }
    }

    public static class EditOperation extends UndoOperation<Editor> {
        public static final Parcelable.ClassLoaderCreator<EditOperation> CREATOR = new Parcelable.ClassLoaderCreator<EditOperation>() {
            @Override
            public EditOperation createFromParcel(Parcel parcel) {
                return new EditOperation(parcel, null);
            }

            @Override
            public EditOperation createFromParcel(Parcel parcel, ClassLoader classLoader) {
                return new EditOperation(parcel, classLoader);
            }

            @Override
            public EditOperation[] newArray(int i) {
                return new EditOperation[i];
            }
        };
        private static final int TYPE_DELETE = 1;
        private static final int TYPE_INSERT = 0;
        private static final int TYPE_REPLACE = 2;
        private boolean mFrozen;
        private boolean mIsComposition;
        private int mNewCursorPos;
        private String mNewText;
        private int mOldCursorPos;
        private String mOldText;
        private int mStart;
        private int mType;

        public EditOperation(Editor editor, String str, int i, String str2, boolean z) {
            super(editor.mUndoOwner);
            this.mOldText = str;
            this.mNewText = str2;
            if (this.mNewText.length() > 0 && this.mOldText.length() == 0) {
                this.mType = 0;
            } else if (this.mNewText.length() == 0 && this.mOldText.length() > 0) {
                this.mType = 1;
            } else {
                this.mType = 2;
            }
            this.mStart = i;
            this.mOldCursorPos = editor.mTextView.getSelectionStart();
            this.mNewCursorPos = i + this.mNewText.length();
            this.mIsComposition = z;
        }

        public EditOperation(Parcel parcel, ClassLoader classLoader) {
            super(parcel, classLoader);
            this.mType = parcel.readInt();
            this.mOldText = parcel.readString();
            this.mNewText = parcel.readString();
            this.mStart = parcel.readInt();
            this.mOldCursorPos = parcel.readInt();
            this.mNewCursorPos = parcel.readInt();
            this.mFrozen = parcel.readInt() == 1;
            this.mIsComposition = parcel.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mType);
            parcel.writeString(this.mOldText);
            parcel.writeString(this.mNewText);
            parcel.writeInt(this.mStart);
            parcel.writeInt(this.mOldCursorPos);
            parcel.writeInt(this.mNewCursorPos);
            parcel.writeInt(this.mFrozen ? 1 : 0);
            parcel.writeInt(this.mIsComposition ? 1 : 0);
        }

        private int getNewTextEnd() {
            return this.mStart + this.mNewText.length();
        }

        private int getOldTextEnd() {
            return this.mStart + this.mOldText.length();
        }

        @Override
        public void commit() {
        }

        @Override
        public void undo() {
            modifyText((Editable) getOwnerData().mTextView.getText(), this.mStart, getNewTextEnd(), this.mOldText, this.mStart, this.mOldCursorPos);
        }

        @Override
        public void redo() {
            modifyText((Editable) getOwnerData().mTextView.getText(), this.mStart, getOldTextEnd(), this.mNewText, this.mStart, this.mNewCursorPos);
        }

        private boolean mergeWith(EditOperation editOperation) {
            if (this.mFrozen) {
                return false;
            }
            switch (this.mType) {
            }
            return false;
        }

        private boolean mergeInsertWith(EditOperation editOperation) {
            if (editOperation.mType == 0) {
                if (getNewTextEnd() != editOperation.mStart) {
                    return false;
                }
                this.mNewText += editOperation.mNewText;
                this.mNewCursorPos = editOperation.mNewCursorPos;
                this.mFrozen = editOperation.mFrozen;
                this.mIsComposition = editOperation.mIsComposition;
                return true;
            }
            if (!this.mIsComposition || editOperation.mType != 2 || this.mStart > editOperation.mStart || getNewTextEnd() < editOperation.getOldTextEnd()) {
                return false;
            }
            this.mNewText = this.mNewText.substring(0, editOperation.mStart - this.mStart) + editOperation.mNewText + this.mNewText.substring(editOperation.getOldTextEnd() - this.mStart, this.mNewText.length());
            this.mNewCursorPos = editOperation.mNewCursorPos;
            this.mIsComposition = editOperation.mIsComposition;
            return true;
        }

        private boolean mergeDeleteWith(EditOperation editOperation) {
            if (editOperation.mType != 1 || this.mStart != editOperation.getOldTextEnd()) {
                return false;
            }
            this.mStart = editOperation.mStart;
            this.mOldText = editOperation.mOldText + this.mOldText;
            this.mNewCursorPos = editOperation.mNewCursorPos;
            this.mIsComposition = editOperation.mIsComposition;
            return true;
        }

        private boolean mergeReplaceWith(EditOperation editOperation) {
            if (editOperation.mType == 0 && getNewTextEnd() == editOperation.mStart) {
                this.mNewText += editOperation.mNewText;
                this.mNewCursorPos = editOperation.mNewCursorPos;
                return true;
            }
            if (!this.mIsComposition) {
                return false;
            }
            if (editOperation.mType == 1 && this.mStart <= editOperation.mStart && getNewTextEnd() >= editOperation.getOldTextEnd()) {
                this.mNewText = this.mNewText.substring(0, editOperation.mStart - this.mStart) + this.mNewText.substring(editOperation.getOldTextEnd() - this.mStart, this.mNewText.length());
                if (this.mNewText.isEmpty()) {
                    this.mType = 1;
                }
                this.mNewCursorPos = editOperation.mNewCursorPos;
                this.mIsComposition = editOperation.mIsComposition;
                return true;
            }
            if (editOperation.mType != 2 || this.mStart != editOperation.mStart || !TextUtils.equals(this.mNewText, editOperation.mOldText)) {
                return false;
            }
            this.mNewText = editOperation.mNewText;
            this.mNewCursorPos = editOperation.mNewCursorPos;
            this.mIsComposition = editOperation.mIsComposition;
            return true;
        }

        public void forceMergeWith(EditOperation editOperation) {
            if (!mergeWith(editOperation)) {
                Editable editable = (Editable) getOwnerData().mTextView.getText();
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(editable.toString());
                modifyText(spannableStringBuilder, this.mStart, getNewTextEnd(), this.mOldText, this.mStart, this.mOldCursorPos);
                SpannableStringBuilder spannableStringBuilder2 = new SpannableStringBuilder(editable.toString());
                modifyText(spannableStringBuilder2, editOperation.mStart, editOperation.getOldTextEnd(), editOperation.mNewText, editOperation.mStart, editOperation.mNewCursorPos);
                this.mType = 2;
                this.mNewText = spannableStringBuilder2.toString();
                this.mOldText = spannableStringBuilder.toString();
                this.mStart = 0;
                this.mNewCursorPos = editOperation.mNewCursorPos;
                this.mIsComposition = editOperation.mIsComposition;
            }
        }

        private static void modifyText(Editable editable, int i, int i2, CharSequence charSequence, int i3, int i4) {
            if (Editor.isValidRange(editable, i, i2) && i3 <= editable.length() - (i2 - i)) {
                if (i != i2) {
                    editable.delete(i, i2);
                }
                if (charSequence.length() != 0) {
                    editable.insert(i3, charSequence);
                }
            }
            if (i4 >= 0 && i4 <= editable.length()) {
                Selection.setSelection(editable, i4);
            }
        }

        private String getTypeString() {
            switch (this.mType) {
                case 0:
                    return "insert";
                case 1:
                    return "delete";
                case 2:
                    return "replace";
                default:
                    return "";
            }
        }

        public String toString() {
            return "[mType=" + getTypeString() + ", mOldText=" + this.mOldText + ", mNewText=" + this.mNewText + ", mStart=" + this.mStart + ", mOldCursorPos=" + this.mOldCursorPos + ", mNewCursorPos=" + this.mNewCursorPos + ", mFrozen=" + this.mFrozen + ", mIsComposition=" + this.mIsComposition + "]";
        }
    }

    static final class ProcessTextIntentActionsHandler {
        private final SparseArray<AccessibilityNodeInfo.AccessibilityAction> mAccessibilityActions;
        private final SparseArray<Intent> mAccessibilityIntents;
        private final Context mContext;
        private final Editor mEditor;
        private final PackageManager mPackageManager;
        private final String mPackageName;
        private final List<ResolveInfo> mSupportedActivities;
        private final TextView mTextView;

        private ProcessTextIntentActionsHandler(Editor editor) {
            this.mAccessibilityIntents = new SparseArray<>();
            this.mAccessibilityActions = new SparseArray<>();
            this.mSupportedActivities = new ArrayList();
            this.mEditor = (Editor) Preconditions.checkNotNull(editor);
            this.mTextView = (TextView) Preconditions.checkNotNull(this.mEditor.mTextView);
            this.mContext = (Context) Preconditions.checkNotNull(this.mTextView.getContext());
            this.mPackageManager = (PackageManager) Preconditions.checkNotNull(this.mContext.getPackageManager());
            this.mPackageName = (String) Preconditions.checkNotNull(this.mContext.getPackageName());
        }

        public void onInitializeMenu(Menu menu) {
            loadSupportedActivities();
            int size = this.mSupportedActivities.size();
            for (int i = 0; i < size; i++) {
                ResolveInfo resolveInfo = this.mSupportedActivities.get(i);
                menu.add(0, 0, 100 + i, getLabel(resolveInfo)).setIntent(createProcessTextIntentForResolveInfo(resolveInfo)).setShowAsAction(0);
            }
        }

        public boolean performMenuItemAction(MenuItem menuItem) {
            return fireIntent(menuItem.getIntent());
        }

        public void initializeAccessibilityActions() {
            this.mAccessibilityIntents.clear();
            this.mAccessibilityActions.clear();
            loadSupportedActivities();
            int i = 0;
            for (ResolveInfo resolveInfo : this.mSupportedActivities) {
                int i2 = 268435712 + i;
                this.mAccessibilityActions.put(i2, new AccessibilityNodeInfo.AccessibilityAction(i2, getLabel(resolveInfo)));
                this.mAccessibilityIntents.put(i2, createProcessTextIntentForResolveInfo(resolveInfo));
                i++;
            }
        }

        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
            for (int i = 0; i < this.mAccessibilityActions.size(); i++) {
                accessibilityNodeInfo.addAction(this.mAccessibilityActions.valueAt(i));
            }
        }

        public boolean performAccessibilityAction(int i) {
            return fireIntent(this.mAccessibilityIntents.get(i));
        }

        private boolean fireIntent(Intent intent) {
            if (intent != null && Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())) {
                intent.putExtra(Intent.EXTRA_PROCESS_TEXT, (String) TextUtils.trimToParcelableSize(this.mTextView.getSelectedText()));
                this.mEditor.mPreserveSelection = true;
                this.mTextView.startActivityForResult(intent, 100);
                return true;
            }
            return false;
        }

        private void loadSupportedActivities() {
            this.mSupportedActivities.clear();
            if (!this.mContext.canStartActivityForResult()) {
                return;
            }
            for (ResolveInfo resolveInfo : this.mTextView.getContext().getPackageManager().queryIntentActivities(createProcessTextIntent(), 0)) {
                if (isSupportedActivity(resolveInfo)) {
                    this.mSupportedActivities.add(resolveInfo);
                }
            }
        }

        private boolean isSupportedActivity(ResolveInfo resolveInfo) {
            return this.mPackageName.equals(resolveInfo.activityInfo.packageName) || (resolveInfo.activityInfo.exported && (resolveInfo.activityInfo.permission == null || this.mContext.checkSelfPermission(resolveInfo.activityInfo.permission) == 0));
        }

        private Intent createProcessTextIntentForResolveInfo(ResolveInfo resolveInfo) {
            return createProcessTextIntent().putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, !this.mTextView.isTextEditable()).setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        }

        private Intent createProcessTextIntent() {
            return new Intent().setAction(Intent.ACTION_PROCESS_TEXT).setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
        }

        private CharSequence getLabel(ResolveInfo resolveInfo) {
            return resolveInfo.loadLabel(this.mPackageManager);
        }
    }
}

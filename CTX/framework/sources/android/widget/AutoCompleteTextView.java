package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.PopupWindow;
import com.android.internal.R;
import java.lang.ref.WeakReference;

public class AutoCompleteTextView extends EditText implements Filter.FilterListener {
    static final boolean DEBUG = false;
    static final int EXPAND_MAX = 3;
    static final String TAG = "AutoCompleteTextView";
    private ListAdapter mAdapter;
    private boolean mBlockCompletion;
    private int mDropDownAnchorId;
    private boolean mDropDownDismissedOnCompletion;
    private Filter mFilter;
    private int mHintResource;
    private CharSequence mHintText;
    private TextView mHintView;
    private AdapterView.OnItemClickListener mItemClickListener;
    private AdapterView.OnItemSelectedListener mItemSelectedListener;
    private int mLastKeyCode;
    private PopupDataSetObserver mObserver;
    private boolean mOpenBefore;
    private final PassThroughClickListener mPassThroughClickListener;
    private final ListPopupWindow mPopup;
    private boolean mPopupCanBeUpdated;
    private final Context mPopupContext;
    private int mThreshold;
    private Validator mValidator;

    public interface OnDismissListener {
        void onDismiss();
    }

    public interface Validator {
        CharSequence fixText(CharSequence charSequence);

        boolean isValid(CharSequence charSequence);
    }

    public AutoCompleteTextView(Context context) {
        this(context, null);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842859);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributeSet, int i, int i2) {
        this(context, attributeSet, i, i2, null);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributeSet, int i, int i2, Resources.Theme theme) {
        TypedArray typedArrayObtainStyledAttributes;
        super(context, attributeSet, i, i2);
        this.mDropDownDismissedOnCompletion = true;
        this.mLastKeyCode = 0;
        this.mValidator = null;
        this.mPopupCanBeUpdated = true;
        TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(attributeSet, R.styleable.AutoCompleteTextView, i, i2);
        if (theme == null) {
            int resourceId = typedArrayObtainStyledAttributes2.getResourceId(8, 0);
            if (resourceId != 0) {
                this.mPopupContext = new ContextThemeWrapper(context, resourceId);
            } else {
                this.mPopupContext = context;
            }
        } else {
            this.mPopupContext = new ContextThemeWrapper(context, theme);
        }
        if (this.mPopupContext != context) {
            typedArrayObtainStyledAttributes = this.mPopupContext.obtainStyledAttributes(attributeSet, R.styleable.AutoCompleteTextView, i, i2);
        } else {
            typedArrayObtainStyledAttributes = typedArrayObtainStyledAttributes2;
        }
        Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(3);
        int layoutDimension = typedArrayObtainStyledAttributes.getLayoutDimension(5, -2);
        int layoutDimension2 = typedArrayObtainStyledAttributes.getLayoutDimension(7, -2);
        int resourceId2 = typedArrayObtainStyledAttributes.getResourceId(1, R.layout.simple_dropdown_hint);
        CharSequence text = typedArrayObtainStyledAttributes.getText(0);
        if (typedArrayObtainStyledAttributes != typedArrayObtainStyledAttributes2) {
            typedArrayObtainStyledAttributes.recycle();
        }
        this.mPopup = new ListPopupWindow(this.mPopupContext, attributeSet, i, i2);
        this.mPopup.setSoftInputMode(16);
        this.mPopup.setPromptPosition(1);
        this.mPopup.setListSelector(drawable);
        this.mPopup.setOnItemClickListener(new DropDownItemClickListener());
        this.mPopup.setWidth(layoutDimension);
        this.mPopup.setHeight(layoutDimension2);
        this.mHintResource = resourceId2;
        setCompletionHint(text);
        this.mDropDownAnchorId = typedArrayObtainStyledAttributes2.getResourceId(6, -1);
        this.mThreshold = typedArrayObtainStyledAttributes2.getInt(2, 2);
        typedArrayObtainStyledAttributes2.recycle();
        int inputType = getInputType();
        if ((inputType & 15) == 1) {
            setRawInputType(inputType | 65536);
        }
        setFocusable(true);
        addTextChangedListener(new MyWatcher());
        this.mPassThroughClickListener = new PassThroughClickListener();
        super.setOnClickListener(this.mPassThroughClickListener);
    }

    @Override
    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.mPassThroughClickListener.mWrapped = onClickListener;
    }

    private void onClickImpl() {
        if (isPopupShowing()) {
            ensureImeVisible(true);
        }
    }

    public void setCompletionHint(CharSequence charSequence) {
        this.mHintText = charSequence;
        if (charSequence == null) {
            this.mPopup.setPromptView(null);
            this.mHintView = null;
        } else {
            if (this.mHintView == null) {
                TextView textView = (TextView) LayoutInflater.from(this.mPopupContext).inflate(this.mHintResource, (ViewGroup) null).findViewById(16908308);
                textView.setText(this.mHintText);
                this.mHintView = textView;
                this.mPopup.setPromptView(textView);
                return;
            }
            this.mHintView.setText(charSequence);
        }
    }

    public CharSequence getCompletionHint() {
        return this.mHintText;
    }

    public int getDropDownWidth() {
        return this.mPopup.getWidth();
    }

    public void setDropDownWidth(int i) {
        this.mPopup.setWidth(i);
    }

    public int getDropDownHeight() {
        return this.mPopup.getHeight();
    }

    public void setDropDownHeight(int i) {
        this.mPopup.setHeight(i);
    }

    public int getDropDownAnchor() {
        return this.mDropDownAnchorId;
    }

    public void setDropDownAnchor(int i) {
        this.mDropDownAnchorId = i;
        this.mPopup.setAnchorView(null);
    }

    public Drawable getDropDownBackground() {
        return this.mPopup.getBackground();
    }

    public void setDropDownBackgroundDrawable(Drawable drawable) {
        this.mPopup.setBackgroundDrawable(drawable);
    }

    public void setDropDownBackgroundResource(int i) {
        this.mPopup.setBackgroundDrawable(getContext().getDrawable(i));
    }

    public void setDropDownVerticalOffset(int i) {
        this.mPopup.setVerticalOffset(i);
    }

    public int getDropDownVerticalOffset() {
        return this.mPopup.getVerticalOffset();
    }

    public void setDropDownHorizontalOffset(int i) {
        this.mPopup.setHorizontalOffset(i);
    }

    public int getDropDownHorizontalOffset() {
        return this.mPopup.getHorizontalOffset();
    }

    public void setDropDownAnimationStyle(int i) {
        this.mPopup.setAnimationStyle(i);
    }

    public int getDropDownAnimationStyle() {
        return this.mPopup.getAnimationStyle();
    }

    public boolean isDropDownAlwaysVisible() {
        return this.mPopup.isDropDownAlwaysVisible();
    }

    public void setDropDownAlwaysVisible(boolean z) {
        this.mPopup.setDropDownAlwaysVisible(z);
    }

    public boolean isDropDownDismissedOnCompletion() {
        return this.mDropDownDismissedOnCompletion;
    }

    public void setDropDownDismissedOnCompletion(boolean z) {
        this.mDropDownDismissedOnCompletion = z;
    }

    public int getThreshold() {
        return this.mThreshold;
    }

    public void setThreshold(int i) {
        if (i <= 0) {
            i = 1;
        }
        this.mThreshold = i;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        this.mItemClickListener = onItemClickListener;
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener onItemSelectedListener) {
        this.mItemSelectedListener = onItemSelectedListener;
    }

    @Deprecated
    public AdapterView.OnItemClickListener getItemClickListener() {
        return this.mItemClickListener;
    }

    @Deprecated
    public AdapterView.OnItemSelectedListener getItemSelectedListener() {
        return this.mItemSelectedListener;
    }

    public AdapterView.OnItemClickListener getOnItemClickListener() {
        return this.mItemClickListener;
    }

    public AdapterView.OnItemSelectedListener getOnItemSelectedListener() {
        return this.mItemSelectedListener;
    }

    public void setOnDismissListener(final OnDismissListener onDismissListener) {
        PopupWindow.OnDismissListener onDismissListener2;
        if (onDismissListener != null) {
            onDismissListener2 = new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    onDismissListener.onDismiss();
                }
            };
        } else {
            onDismissListener2 = null;
        }
        this.mPopup.setOnDismissListener(onDismissListener2);
    }

    public ListAdapter getAdapter() {
        return this.mAdapter;
    }

    public <T extends ListAdapter & Filterable> void setAdapter(T t) {
        if (this.mObserver == null) {
            this.mObserver = new PopupDataSetObserver();
        } else if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(this.mObserver);
        }
        this.mAdapter = t;
        if (this.mAdapter != null) {
            this.mFilter = ((Filterable) this.mAdapter).getFilter();
            t.registerDataSetObserver(this.mObserver);
        } else {
            this.mFilter = null;
        }
        this.mPopup.setAdapter(this.mAdapter);
    }

    @Override
    public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
        if (i == 4 && isPopupShowing() && !this.mPopup.isDropDownAlwaysVisible()) {
            if (keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
                KeyEvent.DispatcherState keyDispatcherState = getKeyDispatcherState();
                if (keyDispatcherState != null) {
                    keyDispatcherState.startTracking(keyEvent, this);
                }
                return true;
            }
            if (keyEvent.getAction() == 1) {
                KeyEvent.DispatcherState keyDispatcherState2 = getKeyDispatcherState();
                if (keyDispatcherState2 != null) {
                    keyDispatcherState2.handleUpEvent(keyEvent);
                }
                if (keyEvent.isTracking() && !keyEvent.isCanceled()) {
                    dismissDropDown();
                    return true;
                }
            }
        }
        return super.onKeyPreIme(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (this.mPopup.onKeyUp(i, keyEvent) && (i == 23 || i == 61 || i == 66)) {
            if (keyEvent.hasNoModifiers()) {
                performCompletion();
            }
            return true;
        }
        if (isPopupShowing() && i == 61 && keyEvent.hasNoModifiers()) {
            performCompletion();
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (this.mPopup.onKeyDown(i, keyEvent)) {
            return true;
        }
        if (!isPopupShowing() && i == 20 && keyEvent.hasNoModifiers()) {
            performValidation();
        }
        if (isPopupShowing() && i == 61 && keyEvent.hasNoModifiers()) {
            return true;
        }
        this.mLastKeyCode = i;
        boolean zOnKeyDown = super.onKeyDown(i, keyEvent);
        this.mLastKeyCode = 0;
        if (zOnKeyDown && isPopupShowing()) {
            clearListSelection();
        }
        return zOnKeyDown;
    }

    public boolean enoughToFilter() {
        return getText().length() >= this.mThreshold;
    }

    private class MyWatcher implements TextWatcher {
        private MyWatcher() {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            AutoCompleteTextView.this.doAfterTextChanged();
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            AutoCompleteTextView.this.doBeforeTextChanged();
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }
    }

    void doBeforeTextChanged() {
        if (this.mBlockCompletion) {
            return;
        }
        this.mOpenBefore = isPopupShowing();
    }

    void doAfterTextChanged() {
        if (this.mBlockCompletion) {
            return;
        }
        if (this.mOpenBefore && !isPopupShowing()) {
            return;
        }
        if (enoughToFilter()) {
            if (this.mFilter != null) {
                this.mPopupCanBeUpdated = true;
                performFiltering(getText(), this.mLastKeyCode);
                return;
            }
            return;
        }
        if (!this.mPopup.isDropDownAlwaysVisible()) {
            dismissDropDown();
        }
        if (this.mFilter != null) {
            this.mFilter.filter(null);
        }
    }

    public boolean isPopupShowing() {
        return this.mPopup.isShowing();
    }

    protected CharSequence convertSelectionToString(Object obj) {
        return this.mFilter.convertResultToString(obj);
    }

    public void clearListSelection() {
        this.mPopup.clearListSelection();
    }

    public void setListSelection(int i) {
        this.mPopup.setSelection(i);
    }

    public int getListSelection() {
        return this.mPopup.getSelectedItemPosition();
    }

    protected void performFiltering(CharSequence charSequence, int i) {
        this.mFilter.filter(charSequence, this);
    }

    public void performCompletion() {
        performCompletion(null, -1, -1L);
    }

    @Override
    public void onCommitCompletion(CompletionInfo completionInfo) {
        if (isPopupShowing()) {
            this.mPopup.performItemClick(completionInfo.getPosition());
        }
    }

    private void performCompletion(View view, int i, long j) {
        Object item;
        if (isPopupShowing()) {
            if (i < 0) {
                item = this.mPopup.getSelectedItem();
            } else {
                item = this.mAdapter.getItem(i);
            }
            if (item == null) {
                Log.w(TAG, "performCompletion: no selected item");
                return;
            }
            this.mBlockCompletion = true;
            replaceText(convertSelectionToString(item));
            this.mBlockCompletion = false;
            if (this.mItemClickListener != null) {
                ListPopupWindow listPopupWindow = this.mPopup;
                if (view == null || i < 0) {
                    view = listPopupWindow.getSelectedView();
                    i = listPopupWindow.getSelectedItemPosition();
                    j = listPopupWindow.getSelectedItemId();
                }
                this.mItemClickListener.onItemClick(listPopupWindow.getListView(), view, i, j);
            }
        }
        if (this.mDropDownDismissedOnCompletion && !this.mPopup.isDropDownAlwaysVisible()) {
            dismissDropDown();
        }
    }

    public boolean isPerformingCompletion() {
        return this.mBlockCompletion;
    }

    public void setText(CharSequence charSequence, boolean z) {
        if (z) {
            setText(charSequence);
            return;
        }
        this.mBlockCompletion = true;
        setText(charSequence);
        this.mBlockCompletion = false;
    }

    protected void replaceText(CharSequence charSequence) {
        clearComposingText();
        setText(charSequence);
        Editable text = getText();
        Selection.setSelection(text, text.length());
    }

    @Override
    public void onFilterComplete(int i) {
        updateDropDownForFilter(i);
    }

    private void updateDropDownForFilter(int i) {
        if (getWindowVisibility() == 8) {
            return;
        }
        boolean zIsDropDownAlwaysVisible = this.mPopup.isDropDownAlwaysVisible();
        boolean zEnoughToFilter = enoughToFilter();
        if ((i > 0 || zIsDropDownAlwaysVisible) && zEnoughToFilter) {
            if (hasFocus() && hasWindowFocus() && this.mPopupCanBeUpdated) {
                showDropDown();
                return;
            }
            return;
        }
        if (!zIsDropDownAlwaysVisible && isPopupShowing()) {
            dismissDropDown();
            this.mPopupCanBeUpdated = true;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (!z && !this.mPopup.isDropDownAlwaysVisible()) {
            dismissDropDown();
        }
    }

    @Override
    protected void onDisplayHint(int i) {
        super.onDisplayHint(i);
        if (i == 4 && !this.mPopup.isDropDownAlwaysVisible()) {
            dismissDropDown();
        }
    }

    @Override
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        super.onFocusChanged(z, i, rect);
        if (isTemporarilyDetached()) {
            return;
        }
        if (!z) {
            performValidation();
        }
        if (!z && !this.mPopup.isDropDownAlwaysVisible()) {
            dismissDropDown();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        dismissDropDown();
        super.onDetachedFromWindow();
    }

    public void dismissDropDown() {
        InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
        if (inputMethodManagerPeekInstance != null) {
            inputMethodManagerPeekInstance.displayCompletions(this, null);
        }
        this.mPopup.dismiss();
        this.mPopupCanBeUpdated = false;
    }

    @Override
    protected boolean setFrame(int i, int i2, int i3, int i4) {
        boolean frame = super.setFrame(i, i2, i3, i4);
        if (isPopupShowing()) {
            showDropDown();
        }
        return frame;
    }

    public void showDropDownAfterLayout() {
        this.mPopup.postShow();
    }

    public void ensureImeVisible(boolean z) {
        this.mPopup.setInputMethodMode(z ? 1 : 2);
        if (this.mPopup.isDropDownAlwaysVisible() || (this.mFilter != null && enoughToFilter())) {
            showDropDown();
        }
    }

    public boolean isInputMethodNotNeeded() {
        return this.mPopup.getInputMethodMode() == 2;
    }

    public void showDropDown() {
        buildImeCompletions();
        if (this.mPopup.getAnchorView() == null) {
            if (this.mDropDownAnchorId != -1) {
                this.mPopup.setAnchorView(getRootView().findViewById(this.mDropDownAnchorId));
            } else {
                this.mPopup.setAnchorView(this);
            }
        }
        if (!isPopupShowing()) {
            this.mPopup.setInputMethodMode(1);
            this.mPopup.setListItemExpandMax(3);
        }
        this.mPopup.show();
        this.mPopup.getListView().setOverScrollMode(0);
    }

    public void setForceIgnoreOutsideTouch(boolean z) {
        this.mPopup.setForceIgnoreOutsideTouch(z);
    }

    private void buildImeCompletions() {
        InputMethodManager inputMethodManagerPeekInstance;
        CompletionInfo[] completionInfoArr;
        ListAdapter listAdapter = this.mAdapter;
        if (listAdapter != null && (inputMethodManagerPeekInstance = InputMethodManager.peekInstance()) != null) {
            int iMin = Math.min(listAdapter.getCount(), 20);
            CompletionInfo[] completionInfoArr2 = new CompletionInfo[iMin];
            int i = 0;
            for (int i2 = 0; i2 < iMin; i2++) {
                if (listAdapter.isEnabled(i2)) {
                    completionInfoArr2[i] = new CompletionInfo(listAdapter.getItemId(i2), i, convertSelectionToString(listAdapter.getItem(i2)));
                    i++;
                }
            }
            if (i != iMin) {
                completionInfoArr = new CompletionInfo[i];
                System.arraycopy(completionInfoArr2, 0, completionInfoArr, 0, i);
            } else {
                completionInfoArr = completionInfoArr2;
            }
            inputMethodManagerPeekInstance.displayCompletions(this, completionInfoArr);
        }
    }

    public void setValidator(Validator validator) {
        this.mValidator = validator;
    }

    public Validator getValidator() {
        return this.mValidator;
    }

    public void performValidation() {
        if (this.mValidator == null) {
            return;
        }
        Editable text = getText();
        if (!TextUtils.isEmpty(text) && !this.mValidator.isValid(text)) {
            setText(this.mValidator.fixText(text));
        }
    }

    protected Filter getFilter() {
        return this.mFilter;
    }

    private class DropDownItemClickListener implements AdapterView.OnItemClickListener {
        private DropDownItemClickListener() {
        }

        @Override
        public void onItemClick(AdapterView adapterView, View view, int i, long j) {
            AutoCompleteTextView.this.performCompletion(view, i, j);
        }
    }

    private class PassThroughClickListener implements View.OnClickListener {
        private View.OnClickListener mWrapped;

        private PassThroughClickListener() {
        }

        @Override
        public void onClick(View view) {
            AutoCompleteTextView.this.onClickImpl();
            if (this.mWrapped != null) {
                this.mWrapped.onClick(view);
            }
        }
    }

    private static class PopupDataSetObserver extends DataSetObserver {
        private final WeakReference<AutoCompleteTextView> mViewReference;
        private final Runnable updateRunnable;

        private PopupDataSetObserver(AutoCompleteTextView autoCompleteTextView) {
            this.updateRunnable = new Runnable() {
                @Override
                public void run() {
                    ListAdapter listAdapter;
                    AutoCompleteTextView autoCompleteTextView2 = (AutoCompleteTextView) PopupDataSetObserver.this.mViewReference.get();
                    if (autoCompleteTextView2 != null && (listAdapter = autoCompleteTextView2.mAdapter) != null) {
                        autoCompleteTextView2.updateDropDownForFilter(listAdapter.getCount());
                    }
                }
            };
            this.mViewReference = new WeakReference<>(autoCompleteTextView);
        }

        @Override
        public void onChanged() {
            AutoCompleteTextView autoCompleteTextView = this.mViewReference.get();
            if (autoCompleteTextView != null && autoCompleteTextView.mAdapter != null) {
                autoCompleteTextView.post(this.updateRunnable);
            }
        }
    }
}

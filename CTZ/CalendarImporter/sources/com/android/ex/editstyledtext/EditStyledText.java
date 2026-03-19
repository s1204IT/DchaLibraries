package com.android.ex.editstyledtext;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.common.speech.LoggingEvents;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class EditStyledText extends EditText {
    private static final boolean DBG = true;
    public static final int DEFAULT_FOREGROUND_COLOR = -16777216;
    public static final int DEFAULT_TRANSPARENT_COLOR = 16777215;
    public static final int HINT_MSG_BIG_SIZE_ERROR = 5;
    public static final int HINT_MSG_COPY_BUF_BLANK = 1;
    public static final int HINT_MSG_END_COMPOSE = 7;
    public static final int HINT_MSG_END_PREVIEW = 6;
    public static final int HINT_MSG_NULL = 0;
    public static final int HINT_MSG_PUSH_COMPETE = 4;
    public static final int HINT_MSG_SELECT_END = 3;
    public static final int HINT_MSG_SELECT_START = 2;
    private static final int ID_CLEARSTYLES = 16776962;
    private static final int ID_COPY = 16908321;
    private static final int ID_CUT = 16908320;
    private static final int ID_HIDEEDIT = 16776964;
    private static final int ID_HORIZONTALLINE = 16776961;
    private static final int ID_PASTE = 16908322;
    private static final int ID_SELECT_ALL = 16908319;
    private static final int ID_SHOWEDIT = 16776963;
    private static final int ID_START_SELECTING_TEXT = 16908328;
    private static final int ID_STOP_SELECTING_TEXT = 16908329;
    public static final char IMAGECHAR = 65532;
    private static final int MAXIMAGEWIDTHDIP = 300;
    public static final int MODE_ALIGN = 6;
    public static final int MODE_BGCOLOR = 16;
    public static final int MODE_CANCEL = 18;
    public static final int MODE_CLEARSTYLES = 14;
    public static final int MODE_COLOR = 4;
    public static final int MODE_COPY = 1;
    public static final int MODE_CUT = 7;
    public static final int MODE_END_EDIT = 21;
    public static final int MODE_HORIZONTALLINE = 12;
    public static final int MODE_IMAGE = 15;
    public static final int MODE_MARQUEE = 10;
    public static final int MODE_NOTHING = 0;
    public static final int MODE_PASTE = 2;
    public static final int MODE_PREVIEW = 17;
    public static final int MODE_RESET = 22;
    public static final int MODE_SELECT = 5;
    public static final int MODE_SELECTALL = 11;
    public static final int MODE_SHOW_MENU = 23;
    public static final int MODE_SIZE = 3;
    public static final int MODE_START_EDIT = 20;
    public static final int MODE_STOP_SELECT = 13;
    public static final int MODE_SWING = 9;
    public static final int MODE_TELOP = 8;
    public static final int MODE_TEXTVIEWFUNCTION = 19;
    private static final int PRESSED = 16777233;
    private static final NoCopySpan.Concrete SELECTING = new NoCopySpan.Concrete();
    public static final int STATE_SELECTED = 2;
    public static final int STATE_SELECT_FIX = 3;
    public static final int STATE_SELECT_OFF = 0;
    public static final int STATE_SELECT_ON = 1;
    private static CharSequence STR_CLEARSTYLES = null;
    private static CharSequence STR_HORIZONTALLINE = null;
    private static CharSequence STR_PASTE = null;
    private static final String TAG = "EditStyledText";
    public static final char ZEROWIDTHCHAR = 8288;
    private StyledTextConverter mConverter;
    private Drawable mDefaultBackground;
    private StyledTextDialog mDialog;
    private ArrayList<EditStyledTextNotifier> mESTNotifiers;
    private InputConnection mInputConnection;
    private EditorManager mManager;
    private float mPaddingScale;

    public interface EditStyledTextNotifier {
        void cancelViewManager();

        boolean isButtonsFocused();

        void onStateChanged(int i, int i2);

        void sendHintMsg(int i);

        boolean sendOnTouchEvent(MotionEvent motionEvent);

        boolean showInsertImageSelectAlertDialog();

        boolean showMenuAlertDialog();

        boolean showPreview();
    }

    public interface StyledTextHtmlConverter {
        Spanned fromHtml(String str);

        Spanned fromHtml(String str, Html.ImageGetter imageGetter, Html.TagHandler tagHandler);

        String toHtml(Spanned spanned);

        String toHtml(Spanned spanned, boolean z);

        String toHtml(Spanned spanned, boolean z, int i, float f);
    }

    public EditStyledText(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPaddingScale = 0.0f;
        init();
    }

    public EditStyledText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaddingScale = 0.0f;
        init();
    }

    public EditStyledText(Context context) {
        super(context);
        this.mPaddingScale = 0.0f;
        init();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent;
        if (motionEvent.getAction() == 1) {
            cancelLongPress();
            boolean zIsEditting = isEditting();
            if (!zIsEditting) {
                onStartEdit();
            }
            int selectionStart = Selection.getSelectionStart(getText());
            int selectionEnd = Selection.getSelectionEnd(getText());
            zOnTouchEvent = super.onTouchEvent(motionEvent);
            if (isFocused() && getSelectState() == 0) {
                if (zIsEditting) {
                    this.mManager.showSoftKey(Selection.getSelectionStart(getText()), Selection.getSelectionEnd(getText()));
                } else {
                    this.mManager.showSoftKey(selectionStart, selectionEnd);
                }
            }
            this.mManager.onCursorMoved();
            this.mManager.unsetTextComposingMask();
        } else {
            zOnTouchEvent = super.onTouchEvent(motionEvent);
        }
        sendOnTouchEvent(motionEvent);
        return zOnTouchEvent;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedStyledTextState savedStyledTextState = new SavedStyledTextState(super.onSaveInstanceState());
        savedStyledTextState.mBackgroundColor = this.mManager.getBackgroundColor();
        return savedStyledTextState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedStyledTextState)) {
            super.onRestoreInstanceState(parcelable);
        } else {
            super.onRestoreInstanceState(parcelable.getSuperState());
            setBackgroundColor(parcelable.mBackgroundColor);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mManager != null) {
            this.mManager.onRefreshStyles();
        }
    }

    @Override
    public boolean onTextContextMenuItem(int i) {
        boolean z = getSelectionStart() != getSelectionEnd();
        switch (i) {
            case ID_HORIZONTALLINE:
                onInsertHorizontalLine();
                return DBG;
            case ID_CLEARSTYLES:
                onClearStyles();
                return DBG;
            case ID_SHOWEDIT:
                onStartEdit();
                return DBG;
            case ID_HIDEEDIT:
                onEndEdit();
                return DBG;
            default:
                switch (i) {
                    case 16908319:
                        onStartSelectAll();
                        return DBG;
                    case 16908320:
                        if (z) {
                            onStartCut();
                        } else {
                            this.mManager.onStartSelectAll(false);
                            onStartCut();
                        }
                        return DBG;
                    case 16908321:
                        if (z) {
                            onStartCopy();
                        } else {
                            this.mManager.onStartSelectAll(false);
                            onStartCopy();
                        }
                        return DBG;
                    case 16908322:
                        onStartPaste();
                        return DBG;
                    default:
                        switch (i) {
                            case 16908328:
                                onStartSelect();
                                this.mManager.blockSoftKey();
                                break;
                            case 16908329:
                                onFixSelectedItem();
                                break;
                        }
                        return super.onTextContextMenuItem(i);
                }
        }
    }

    @Override
    protected void onCreateContextMenu(ContextMenu contextMenu) {
        super.onCreateContextMenu(contextMenu);
        MenuHandler menuHandler = new MenuHandler();
        if (STR_HORIZONTALLINE != null) {
            contextMenu.add(0, ID_HORIZONTALLINE, 0, STR_HORIZONTALLINE).setOnMenuItemClickListener(menuHandler);
        }
        if (isStyledText() && STR_CLEARSTYLES != null) {
            contextMenu.add(0, ID_CLEARSTYLES, 0, STR_CLEARSTYLES).setOnMenuItemClickListener(menuHandler);
        }
        if (this.mManager.canPaste()) {
            contextMenu.add(0, 16908322, 0, STR_PASTE).setOnMenuItemClickListener(menuHandler).setAlphabeticShortcut('v');
        }
    }

    @Override
    protected void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (this.mManager != null) {
            this.mManager.updateSpanNextToCursor(getText(), i, i2, i3);
            this.mManager.updateSpanPreviousFromCursor(getText(), i, i2, i3);
            if (i3 > i2) {
                this.mManager.setTextComposingMask(i, i + i3);
            } else if (i2 < i3) {
                this.mManager.unsetTextComposingMask();
            }
            if (this.mManager.isWaitInput()) {
                if (i3 > i2) {
                    this.mManager.onCursorMoved();
                    onFixSelectedItem();
                } else if (i3 < i2) {
                    this.mManager.onAction(22);
                }
            }
        }
        super.onTextChanged(charSequence, i, i2, i3);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        this.mInputConnection = new StyledTextInputConnection(super.onCreateInputConnection(editorInfo), this);
        return this.mInputConnection;
    }

    @Override
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        super.onFocusChanged(z, i, rect);
        if (z) {
            onStartEdit();
        } else if (!isButtonsFocused()) {
            onEndEdit();
        }
    }

    private void init() {
        this.mConverter = new StyledTextConverter(this, new StyledTextHtmlStandard());
        this.mDialog = new StyledTextDialog(this);
        this.mManager = new EditorManager(this, this.mDialog);
        setMovementMethod(new StyledTextArrowKeyMethod(this.mManager));
        this.mDefaultBackground = getBackground();
        requestFocus();
    }

    public void setStyledTextHtmlConverter(StyledTextHtmlConverter styledTextHtmlConverter) {
        this.mConverter.setStyledTextHtmlConverter(styledTextHtmlConverter);
    }

    public void addEditStyledTextListener(EditStyledTextNotifier editStyledTextNotifier) {
        if (this.mESTNotifiers == null) {
            this.mESTNotifiers = new ArrayList<>();
        }
        this.mESTNotifiers.add(editStyledTextNotifier);
    }

    public void removeEditStyledTextListener(EditStyledTextNotifier editStyledTextNotifier) {
        int iIndexOf;
        if (this.mESTNotifiers != null && (iIndexOf = this.mESTNotifiers.indexOf(editStyledTextNotifier)) > 0) {
            this.mESTNotifiers.remove(iIndexOf);
        }
    }

    private void sendOnTouchEvent(MotionEvent motionEvent) {
        if (this.mESTNotifiers != null) {
            Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
            while (it.hasNext()) {
                it.next().sendOnTouchEvent(motionEvent);
            }
        }
    }

    public boolean isButtonsFocused() {
        boolean zIsButtonsFocused = false;
        if (this.mESTNotifiers != null) {
            Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
            while (it.hasNext()) {
                zIsButtonsFocused |= it.next().isButtonsFocused();
            }
        }
        return zIsButtonsFocused;
    }

    private void showPreview() {
        if (this.mESTNotifiers != null) {
            Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
            while (it.hasNext() && !it.next().showPreview()) {
            }
        }
    }

    private void cancelViewManagers() {
        if (this.mESTNotifiers != null) {
            Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
            while (it.hasNext()) {
                it.next().cancelViewManager();
            }
        }
    }

    private void showInsertImageSelectAlertDialog() {
        if (this.mESTNotifiers != null) {
            Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
            while (it.hasNext() && !it.next().showInsertImageSelectAlertDialog()) {
            }
        }
    }

    private void showMenuAlertDialog() {
        if (this.mESTNotifiers != null) {
            Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
            while (it.hasNext() && !it.next().showMenuAlertDialog()) {
            }
        }
    }

    private void sendHintMessage(int i) {
        if (this.mESTNotifiers != null) {
            Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
            while (it.hasNext()) {
                it.next().sendHintMsg(i);
            }
        }
    }

    private void notifyStateChanged(int i, int i2) {
        if (this.mESTNotifiers != null) {
            Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
            while (it.hasNext()) {
                it.next().onStateChanged(i, i2);
            }
        }
    }

    public void onStartEdit() {
        this.mManager.onAction(20);
    }

    public void onEndEdit() {
        this.mManager.onAction(21);
    }

    public void onResetEdit() {
        this.mManager.onAction(22);
    }

    public void onStartCopy() {
        this.mManager.onAction(1);
    }

    public void onStartCut() {
        this.mManager.onAction(7);
    }

    public void onStartPaste() {
        this.mManager.onAction(2);
    }

    public void onStartSize() {
        this.mManager.onAction(3);
    }

    public void onStartColor() {
        this.mManager.onAction(4);
    }

    public void onStartBackgroundColor() {
        this.mManager.onAction(16);
    }

    public void onStartAlign() {
        this.mManager.onAction(6);
    }

    public void onStartTelop() {
        this.mManager.onAction(8);
    }

    public void onStartSwing() {
        this.mManager.onAction(9);
    }

    public void onStartMarquee() {
        this.mManager.onAction(10);
    }

    public void onStartSelect() {
        this.mManager.onStartSelect(DBG);
    }

    public void onStartSelectAll() {
        this.mManager.onStartSelectAll(DBG);
    }

    public void onStartShowPreview() {
        this.mManager.onAction(17);
    }

    public void onStartShowMenuAlertDialog() {
        this.mManager.onStartShowMenuAlertDialog();
    }

    public void onStartAction(int i, boolean z) {
        this.mManager.onAction(i, z);
    }

    public void onFixSelectedItem() {
        this.mManager.onFixSelectedItem();
    }

    public void onInsertImage() {
        this.mManager.onAction(15);
    }

    public void onInsertImage(Uri uri) {
        this.mManager.onInsertImage(uri);
    }

    public void onInsertImage(int i) {
        this.mManager.onInsertImage(i);
    }

    public void onInsertHorizontalLine() {
        this.mManager.onAction(12);
    }

    public void onClearStyles() {
        this.mManager.onClearStyles();
    }

    public void onBlockSoftKey() {
        this.mManager.blockSoftKey();
    }

    public void onUnblockSoftKey() {
        this.mManager.unblockSoftKey();
    }

    public void onCancelViewManagers() {
        this.mManager.onCancelViewManagers();
    }

    private void onRefreshStyles() {
        this.mManager.onRefreshStyles();
    }

    private void onRefreshZeoWidthChar() {
        this.mManager.onRefreshZeoWidthChar();
    }

    public void setItemSize(int i) {
        this.mManager.setItemSize(i, DBG);
    }

    public void setItemColor(int i) {
        this.mManager.setItemColor(i, DBG);
    }

    public void setAlignment(Layout.Alignment alignment) {
        this.mManager.setAlignment(alignment);
    }

    @Override
    public void setBackgroundColor(int i) {
        if (i != 16777215) {
            super.setBackgroundColor(i);
        } else {
            setBackgroundDrawable(this.mDefaultBackground);
        }
        this.mManager.setBackgroundColor(i);
        onRefreshStyles();
    }

    public void setMarquee(int i) {
        this.mManager.setMarquee(i);
    }

    public void setHtml(String str) {
        this.mConverter.SetHtml(str);
    }

    public void setBuilder(AlertDialog.Builder builder) {
        this.mDialog.setBuilder(builder);
    }

    public void setColorAlertParams(CharSequence charSequence, CharSequence[] charSequenceArr, CharSequence[] charSequenceArr2, CharSequence charSequence2) {
        this.mDialog.setColorAlertParams(charSequence, charSequenceArr, charSequenceArr2, charSequence2);
    }

    public void setSizeAlertParams(CharSequence charSequence, CharSequence[] charSequenceArr, CharSequence[] charSequenceArr2, CharSequence[] charSequenceArr3) {
        this.mDialog.setSizeAlertParams(charSequence, charSequenceArr, charSequenceArr2, charSequenceArr3);
    }

    public void setAlignAlertParams(CharSequence charSequence, CharSequence[] charSequenceArr) {
        this.mDialog.setAlignAlertParams(charSequence, charSequenceArr);
    }

    public void setMarqueeAlertParams(CharSequence charSequence, CharSequence[] charSequenceArr) {
        this.mDialog.setMarqueeAlertParams(charSequence, charSequenceArr);
    }

    public void setContextMenuStrings(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        STR_HORIZONTALLINE = charSequence;
        STR_CLEARSTYLES = charSequence2;
        STR_PASTE = charSequence3;
    }

    public boolean isEditting() {
        return this.mManager.isEditting();
    }

    public boolean isStyledText() {
        return this.mManager.isStyledText();
    }

    public boolean isSoftKeyBlocked() {
        return this.mManager.isSoftKeyBlocked();
    }

    public int getEditMode() {
        return this.mManager.getEditMode();
    }

    public int getSelectState() {
        return this.mManager.getSelectState();
    }

    public String getHtml() {
        return this.mConverter.getHtml(DBG);
    }

    public String getHtml(boolean z) {
        return this.mConverter.getHtml(z);
    }

    public String getHtml(ArrayList<Uri> arrayList, boolean z) {
        this.mConverter.getUriArray(arrayList, getText());
        return this.mConverter.getHtml(z);
    }

    public String getPreviewHtml() {
        return this.mConverter.getPreviewHtml();
    }

    public int getBackgroundColor() {
        return this.mManager.getBackgroundColor();
    }

    public EditorManager getEditStyledTextManager() {
        return this.mManager;
    }

    public int getForegroundColor(int i) {
        if (i < 0 || i > getText().length()) {
            return DEFAULT_FOREGROUND_COLOR;
        }
        ForegroundColorSpan[] foregroundColorSpanArr = (ForegroundColorSpan[]) getText().getSpans(i, i, ForegroundColorSpan.class);
        if (foregroundColorSpanArr.length <= 0) {
            return DEFAULT_FOREGROUND_COLOR;
        }
        return foregroundColorSpanArr[0].getForegroundColor();
    }

    private void finishComposingText() {
        if (this.mInputConnection != null && !this.mManager.mTextIsFinishedFlag) {
            this.mInputConnection.finishComposingText();
            this.mManager.mTextIsFinishedFlag = DBG;
        }
    }

    private float getPaddingScale() {
        if (this.mPaddingScale <= 0.0f) {
            this.mPaddingScale = getContext().getResources().getDisplayMetrics().density;
        }
        return this.mPaddingScale;
    }

    private int dipToPx(int i) {
        if (this.mPaddingScale <= 0.0f) {
            this.mPaddingScale = getContext().getResources().getDisplayMetrics().density;
        }
        return (int) (((double) (i * getPaddingScale())) + 0.5d);
    }

    private int getMaxImageWidthDip() {
        return MAXIMAGEWIDTHDIP;
    }

    private int getMaxImageWidthPx() {
        return dipToPx(MAXIMAGEWIDTHDIP);
    }

    public void addAction(int i, EditModeActions.EditModeActionBase editModeActionBase) {
        this.mManager.addAction(i, editModeActionBase);
    }

    public void addInputExtra(boolean z, String str) {
        Bundle inputExtras = super.getInputExtras(z);
        if (inputExtras != null) {
            inputExtras.putBoolean(str, DBG);
        }
    }

    private static void startSelecting(View view, Spannable spannable) {
        spannable.setSpan(SELECTING, 0, 0, PRESSED);
    }

    private static void stopSelecting(View view, Spannable spannable) {
        spannable.removeSpan(SELECTING);
    }

    private class EditorManager {
        private static final String LOG_TAG = "EditStyledText.EditorManager";
        private EditModeActions mActions;
        private BackgroundColorSpan mComposingTextMask;
        private SpannableStringBuilder mCopyBuffer;
        private EditStyledText mEST;
        private SoftKeyReceiver mSkr;
        private boolean mEditFlag = false;
        private boolean mSoftKeyBlockFlag = false;
        private boolean mKeepNonLineSpan = false;
        private boolean mWaitInputFlag = false;
        private boolean mTextIsFinishedFlag = false;
        private int mMode = 0;
        private int mState = 0;
        private int mCurStart = 0;
        private int mCurEnd = 0;
        private int mColorWaitInput = EditStyledText.DEFAULT_TRANSPARENT_COLOR;
        private int mSizeWaitInput = 0;
        private int mBackgroundColor = EditStyledText.DEFAULT_TRANSPARENT_COLOR;

        EditorManager(EditStyledText editStyledText, StyledTextDialog styledTextDialog) {
            this.mEST = editStyledText;
            this.mActions = EditStyledText.this.new EditModeActions(this.mEST, this, styledTextDialog);
            this.mSkr = new SoftKeyReceiver(this.mEST);
        }

        public void addAction(int i, EditModeActions.EditModeActionBase editModeActionBase) {
            this.mActions.addAction(i, editModeActionBase);
        }

        public void onAction(int i) {
            onAction(i, EditStyledText.DBG);
        }

        public void onAction(int i, boolean z) {
            this.mActions.onAction(i);
            if (z) {
                this.mEST.notifyStateChanged(this.mMode, this.mState);
            }
        }

        private void startEdit() {
            resetEdit();
            showSoftKey();
        }

        public void onStartSelect(boolean z) {
            Log.d(LOG_TAG, "--- onClickSelect");
            this.mMode = 5;
            if (this.mState == 0) {
                this.mActions.onSelectAction();
            } else {
                unsetSelect();
                this.mActions.onSelectAction();
            }
            if (z) {
                this.mEST.notifyStateChanged(this.mMode, this.mState);
            }
        }

        public void onCursorMoved() {
            Log.d(LOG_TAG, "--- onClickView");
            if (this.mState == 1 || this.mState == 2) {
                this.mActions.onSelectAction();
                this.mEST.notifyStateChanged(this.mMode, this.mState);
            }
        }

        public void onStartSelectAll(boolean z) {
            Log.d(LOG_TAG, "--- onClickSelectAll");
            handleSelectAll();
            if (z) {
                this.mEST.notifyStateChanged(this.mMode, this.mState);
            }
        }

        public void onStartShowMenuAlertDialog() {
            this.mActions.onAction(23);
        }

        public void onFixSelectedItem() {
            Log.d(LOG_TAG, "--- onFixSelectedItem");
            fixSelectionAndDoNextAction();
            this.mEST.notifyStateChanged(this.mMode, this.mState);
        }

        public void onInsertImage(Uri uri) {
            this.mActions.onAction(15, uri);
            this.mEST.notifyStateChanged(this.mMode, this.mState);
        }

        public void onInsertImage(int i) {
            this.mActions.onAction(15, Integer.valueOf(i));
            this.mEST.notifyStateChanged(this.mMode, this.mState);
        }

        private void insertImageFromUri(Uri uri) {
            insertImageSpan(new EditStyledTextSpans.RescalableImageSpan(this.mEST.getContext(), uri, this.mEST.getMaxImageWidthPx()), this.mEST.getSelectionStart());
        }

        private void insertImageFromResId(int i) {
            insertImageSpan(new EditStyledTextSpans.RescalableImageSpan(this.mEST.getContext(), i, this.mEST.getMaxImageWidthDip()), this.mEST.getSelectionStart());
        }

        private void insertHorizontalLine() {
            Log.d(LOG_TAG, "--- onInsertHorizontalLine:");
            int selectionStart = this.mEST.getSelectionStart();
            if (selectionStart > 0 && this.mEST.getText().charAt(selectionStart - 1) != '\n') {
                this.mEST.getText().insert(selectionStart, "\n");
                selectionStart++;
            }
            int i = selectionStart + 1;
            insertImageSpan(new EditStyledTextSpans.HorizontalLineSpan(EditStyledText.DEFAULT_FOREGROUND_COLOR, this.mEST.getWidth(), this.mEST.getText()), selectionStart);
            this.mEST.getText().insert(i, "\n");
            this.mEST.setSelection(i + 1);
            this.mEST.notifyStateChanged(this.mMode, this.mState);
        }

        private void clearStyles(CharSequence charSequence) {
            Log.d(EditStyledText.TAG, "--- onClearStyles");
            int length = charSequence.length();
            if (charSequence instanceof Editable) {
                Editable editable = (Editable) charSequence;
                for (Object obj : editable.getSpans(0, length, Object.class)) {
                    if ((obj instanceof ParagraphStyle) || (obj instanceof QuoteSpan) || ((obj instanceof CharacterStyle) && !(obj instanceof UnderlineSpan))) {
                        if ((obj instanceof ImageSpan) || (obj instanceof EditStyledTextSpans.HorizontalLineSpan)) {
                            editable.replace(editable.getSpanStart(obj), editable.getSpanEnd(obj), LoggingEvents.EXTRA_CALLING_APP_NAME);
                        }
                        editable.removeSpan(obj);
                    }
                }
            }
        }

        public void onClearStyles() {
            this.mActions.onAction(14);
        }

        public void onCancelViewManagers() {
            this.mActions.onAction(18);
        }

        private void clearStyles() {
            Log.d(LOG_TAG, "--- onClearStyles");
            clearStyles(this.mEST.getText());
            this.mEST.setBackgroundDrawable(this.mEST.mDefaultBackground);
            this.mBackgroundColor = EditStyledText.DEFAULT_TRANSPARENT_COLOR;
            onRefreshZeoWidthChar();
        }

        public void onRefreshZeoWidthChar() {
            Editable text = this.mEST.getText();
            int i = 0;
            while (i < text.length()) {
                if (text.charAt(i) == 8288) {
                    text.replace(i, i + 1, LoggingEvents.EXTRA_CALLING_APP_NAME);
                    i--;
                }
                i++;
            }
        }

        public void onRefreshStyles() {
            Log.d(LOG_TAG, "--- onRefreshStyles");
            Editable text = this.mEST.getText();
            int length = text.length();
            int width = this.mEST.getWidth();
            EditStyledTextSpans.HorizontalLineSpan[] horizontalLineSpanArr = (EditStyledTextSpans.HorizontalLineSpan[]) text.getSpans(0, length, EditStyledTextSpans.HorizontalLineSpan.class);
            for (EditStyledTextSpans.HorizontalLineSpan horizontalLineSpan : horizontalLineSpanArr) {
                horizontalLineSpan.resetWidth(width);
            }
            for (EditStyledTextSpans.MarqueeSpan marqueeSpan : (EditStyledTextSpans.MarqueeSpan[]) text.getSpans(0, length, EditStyledTextSpans.MarqueeSpan.class)) {
                marqueeSpan.resetColor(this.mEST.getBackgroundColor());
            }
            if (horizontalLineSpanArr.length > 0) {
                text.replace(0, 1, LoggingEvents.EXTRA_CALLING_APP_NAME + text.charAt(0));
            }
        }

        public void setBackgroundColor(int i) {
            this.mBackgroundColor = i;
        }

        public void setItemSize(int i, boolean z) {
            Log.d(LOG_TAG, "--- setItemSize");
            if (isWaitingNextAction()) {
                this.mSizeWaitInput = i;
                return;
            }
            if (this.mState == 2 || this.mState == 3) {
                if (i > 0) {
                    changeSizeSelectedText(i);
                }
                if (z) {
                    resetEdit();
                }
            }
        }

        public void setItemColor(int i, boolean z) {
            Log.d(LOG_TAG, "--- setItemColor");
            if (isWaitingNextAction()) {
                this.mColorWaitInput = i;
                return;
            }
            if (this.mState == 2 || this.mState == 3) {
                if (i != 16777215) {
                    changeColorSelectedText(i);
                }
                if (z) {
                    resetEdit();
                }
            }
        }

        public void setAlignment(Layout.Alignment alignment) {
            if (this.mState == 2 || this.mState == 3) {
                changeAlign(alignment);
                resetEdit();
            }
        }

        public void setTelop() {
            if (this.mState == 2 || this.mState == 3) {
                addTelop();
                resetEdit();
            }
        }

        public void setSwing() {
            if (this.mState == 2 || this.mState == 3) {
                addSwing();
                resetEdit();
            }
        }

        public void setMarquee(int i) {
            if (this.mState == 2 || this.mState == 3) {
                addMarquee(i);
                resetEdit();
            }
        }

        public void setTextComposingMask(int i, int i2) {
            int foregroundColor;
            Log.d(EditStyledText.TAG, "--- setTextComposingMask:" + i + "," + i2);
            int iMin = Math.min(i, i2);
            int iMax = Math.max(i, i2);
            if (isWaitInput() && this.mColorWaitInput != 16777215) {
                foregroundColor = this.mColorWaitInput;
            } else {
                foregroundColor = this.mEST.getForegroundColor(iMin);
            }
            int backgroundColor = this.mEST.getBackgroundColor();
            Log.d(EditStyledText.TAG, "--- fg:" + Integer.toHexString(foregroundColor) + ",bg:" + Integer.toHexString(backgroundColor) + "," + isWaitInput() + ",," + this.mMode);
            if (foregroundColor == backgroundColor) {
                int i3 = Integer.MIN_VALUE | (~(backgroundColor | EditStyledText.DEFAULT_FOREGROUND_COLOR));
                if (this.mComposingTextMask == null || this.mComposingTextMask.getBackgroundColor() != i3) {
                    this.mComposingTextMask = new BackgroundColorSpan(i3);
                }
                this.mEST.getText().setSpan(this.mComposingTextMask, iMin, iMax, 33);
            }
        }

        private void setEditMode(int i) {
            this.mMode = i;
        }

        private void setSelectState(int i) {
            this.mState = i;
        }

        public void unsetTextComposingMask() {
            Log.d(EditStyledText.TAG, "--- unsetTextComposingMask");
            if (this.mComposingTextMask != null) {
                this.mEST.getText().removeSpan(this.mComposingTextMask);
                this.mComposingTextMask = null;
            }
        }

        public boolean isEditting() {
            return this.mEditFlag;
        }

        public boolean isStyledText() {
            Editable text = this.mEST.getText();
            int length = text.length();
            if (((ParagraphStyle[]) text.getSpans(0, length, ParagraphStyle.class)).length > 0 || ((QuoteSpan[]) text.getSpans(0, length, QuoteSpan.class)).length > 0 || ((CharacterStyle[]) text.getSpans(0, length, CharacterStyle.class)).length > 0 || this.mBackgroundColor != 16777215) {
                return EditStyledText.DBG;
            }
            return false;
        }

        public boolean isSoftKeyBlocked() {
            return this.mSoftKeyBlockFlag;
        }

        public boolean isWaitInput() {
            return this.mWaitInputFlag;
        }

        public int getBackgroundColor() {
            return this.mBackgroundColor;
        }

        public int getEditMode() {
            return this.mMode;
        }

        public int getSelectState() {
            return this.mState;
        }

        public int getSelectionStart() {
            return this.mCurStart;
        }

        public int getSelectionEnd() {
            return this.mCurEnd;
        }

        public int getSizeWaitInput() {
            return this.mSizeWaitInput;
        }

        public int getColorWaitInput() {
            return this.mColorWaitInput;
        }

        private void setInternalSelection(int i, int i2) {
            this.mCurStart = i;
            this.mCurEnd = i2;
        }

        public void updateSpanPreviousFromCursor(Editable editable, int i, int i2, int i3) {
            int iFindLineEnd;
            Log.d(LOG_TAG, "updateSpanPrevious:" + i + "," + i2 + "," + i3);
            int i4 = i + i3;
            int iMin = Math.min(i, i4);
            int iMax = Math.max(i, i4);
            for (Object obj : editable.getSpans(iMin, iMin, Object.class)) {
                if ((obj instanceof ForegroundColorSpan) || (obj instanceof AbsoluteSizeSpan) || (obj instanceof EditStyledTextSpans.MarqueeSpan) || (obj instanceof AlignmentSpan)) {
                    int spanStart = editable.getSpanStart(obj);
                    int spanEnd = editable.getSpanEnd(obj);
                    Log.d(LOG_TAG, "spantype:" + obj.getClass() + "," + spanStart);
                    if ((obj instanceof EditStyledTextSpans.MarqueeSpan) || (obj instanceof AlignmentSpan)) {
                        iFindLineEnd = findLineEnd(this.mEST.getText(), iMax);
                    } else {
                        iFindLineEnd = this.mKeepNonLineSpan ? spanEnd : iMax;
                    }
                    if (spanEnd < iFindLineEnd) {
                        Log.d(LOG_TAG, "updateSpanPrevious: extend span");
                        editable.setSpan(obj, spanStart, iFindLineEnd, 33);
                    }
                } else if (obj instanceof EditStyledTextSpans.HorizontalLineSpan) {
                    int spanStart2 = editable.getSpanStart(obj);
                    int spanEnd2 = editable.getSpanEnd(obj);
                    if (i2 > i3) {
                        editable.replace(spanStart2, spanEnd2, LoggingEvents.EXTRA_CALLING_APP_NAME);
                        editable.removeSpan(obj);
                    } else if (spanEnd2 == i4 && i4 < editable.length() && this.mEST.getText().charAt(i4) != '\n') {
                        this.mEST.getText().insert(i4, "\n");
                    }
                }
            }
        }

        public void updateSpanNextToCursor(Editable editable, int i, int i2, int i3) {
            int iFindLineStart;
            Log.d(LOG_TAG, "updateSpanNext:" + i + "," + i2 + "," + i3);
            int i4 = i + i3;
            int iMin = Math.min(i, i4);
            int iMax = Math.max(i, i4);
            for (Object obj : editable.getSpans(iMax, iMax, Object.class)) {
                boolean z = obj instanceof EditStyledTextSpans.MarqueeSpan;
                if (z || (obj instanceof AlignmentSpan)) {
                    int spanStart = editable.getSpanStart(obj);
                    int spanEnd = editable.getSpanEnd(obj);
                    Log.d(LOG_TAG, "spantype:" + obj.getClass() + "," + spanEnd);
                    if (z || (obj instanceof AlignmentSpan)) {
                        iFindLineStart = findLineStart(this.mEST.getText(), iMin);
                    } else {
                        iFindLineStart = iMin;
                    }
                    if (iFindLineStart < spanStart && i2 > i3) {
                        editable.removeSpan(obj);
                    } else if (spanStart > iMin) {
                        editable.setSpan(obj, iMin, spanEnd, 33);
                    }
                } else if ((obj instanceof EditStyledTextSpans.HorizontalLineSpan) && editable.getSpanStart(obj) == i4 && i4 > 0 && this.mEST.getText().charAt(i4 - 1) != '\n') {
                    this.mEST.getText().insert(i4, "\n");
                    this.mEST.setSelection(i4);
                }
            }
        }

        public boolean canPaste() {
            if (this.mCopyBuffer == null || this.mCopyBuffer.length() <= 0 || removeImageChar(this.mCopyBuffer).length() != 0) {
                return false;
            }
            return EditStyledText.DBG;
        }

        private void endEdit() {
            Log.d(LOG_TAG, "--- handleCancel");
            this.mMode = 0;
            this.mState = 0;
            this.mEditFlag = false;
            this.mColorWaitInput = EditStyledText.DEFAULT_TRANSPARENT_COLOR;
            this.mSizeWaitInput = 0;
            this.mWaitInputFlag = false;
            this.mSoftKeyBlockFlag = false;
            this.mKeepNonLineSpan = false;
            this.mTextIsFinishedFlag = false;
            unsetSelect();
            this.mEST.setOnClickListener(null);
            unblockSoftKey();
        }

        private void fixSelectionAndDoNextAction() {
            Log.d(LOG_TAG, "--- handleComplete:" + this.mCurStart + "," + this.mCurEnd);
            if (!this.mEditFlag) {
                return;
            }
            if (this.mCurStart == this.mCurEnd) {
                Log.d(LOG_TAG, "--- cancel handle complete:" + this.mCurStart);
                resetEdit();
                return;
            }
            if (this.mState == 2) {
                this.mState = 3;
            }
            this.mActions.doNext(this.mMode);
            EditStyledText.stopSelecting(this.mEST, this.mEST.getText());
        }

        private SpannableStringBuilder removeImageChar(SpannableStringBuilder spannableStringBuilder) {
            SpannableStringBuilder spannableStringBuilder2 = new SpannableStringBuilder(spannableStringBuilder);
            for (DynamicDrawableSpan dynamicDrawableSpan : (DynamicDrawableSpan[]) spannableStringBuilder2.getSpans(0, spannableStringBuilder2.length(), DynamicDrawableSpan.class)) {
                if ((dynamicDrawableSpan instanceof EditStyledTextSpans.HorizontalLineSpan) || (dynamicDrawableSpan instanceof EditStyledTextSpans.RescalableImageSpan)) {
                    spannableStringBuilder2.replace(spannableStringBuilder2.getSpanStart(dynamicDrawableSpan), spannableStringBuilder2.getSpanEnd(dynamicDrawableSpan), (CharSequence) LoggingEvents.EXTRA_CALLING_APP_NAME);
                }
            }
            return spannableStringBuilder2;
        }

        private void copyToClipBoard() {
            this.mCopyBuffer = (SpannableStringBuilder) this.mEST.getText().subSequence(Math.min(getSelectionStart(), getSelectionEnd()), Math.max(getSelectionStart(), getSelectionEnd()));
            SpannableStringBuilder spannableStringBuilderRemoveImageChar = removeImageChar(this.mCopyBuffer);
            ((ClipboardManager) EditStyledText.this.getContext().getSystemService("clipboard")).setText(spannableStringBuilderRemoveImageChar);
            dumpSpannableString(spannableStringBuilderRemoveImageChar);
            dumpSpannableString(this.mCopyBuffer);
        }

        private void cutToClipBoard() {
            copyToClipBoard();
            this.mEST.getText().delete(Math.min(getSelectionStart(), getSelectionEnd()), Math.max(getSelectionStart(), getSelectionEnd()));
        }

        private boolean isClipBoardChanged(CharSequence charSequence) {
            Log.d(EditStyledText.TAG, "--- isClipBoardChanged:" + ((Object) charSequence));
            if (this.mCopyBuffer == null) {
                return EditStyledText.DBG;
            }
            int length = charSequence.length();
            SpannableStringBuilder spannableStringBuilderRemoveImageChar = removeImageChar(this.mCopyBuffer);
            Log.d(EditStyledText.TAG, "--- clipBoard:" + length + "," + ((Object) spannableStringBuilderRemoveImageChar) + ((Object) charSequence));
            if (length != spannableStringBuilderRemoveImageChar.length()) {
                return EditStyledText.DBG;
            }
            for (int i = 0; i < length; i++) {
                if (charSequence.charAt(i) != spannableStringBuilderRemoveImageChar.charAt(i)) {
                    return EditStyledText.DBG;
                }
            }
            return false;
        }

        private void pasteFromClipboard() {
            int iMin = Math.min(this.mEST.getSelectionStart(), this.mEST.getSelectionEnd());
            int iMax = Math.max(this.mEST.getSelectionStart(), this.mEST.getSelectionEnd());
            Selection.setSelection(this.mEST.getText(), iMax);
            ClipboardManager clipboardManager = (ClipboardManager) EditStyledText.this.getContext().getSystemService("clipboard");
            this.mKeepNonLineSpan = EditStyledText.DBG;
            this.mEST.getText().replace(iMin, iMax, clipboardManager.getText());
            if (!isClipBoardChanged(clipboardManager.getText())) {
                Log.d(EditStyledText.TAG, "--- handlePaste: startPasteImage");
                for (EditStyledTextSpans.RescalableImageSpan rescalableImageSpan : (DynamicDrawableSpan[]) this.mCopyBuffer.getSpans(0, this.mCopyBuffer.length(), DynamicDrawableSpan.class)) {
                    int spanStart = this.mCopyBuffer.getSpanStart(rescalableImageSpan);
                    if (rescalableImageSpan instanceof EditStyledTextSpans.HorizontalLineSpan) {
                        insertImageSpan(new EditStyledTextSpans.HorizontalLineSpan(EditStyledText.DEFAULT_FOREGROUND_COLOR, this.mEST.getWidth(), this.mEST.getText()), spanStart + iMin);
                    } else if (rescalableImageSpan instanceof EditStyledTextSpans.RescalableImageSpan) {
                        insertImageSpan(new EditStyledTextSpans.RescalableImageSpan(this.mEST.getContext(), rescalableImageSpan.getContentUri(), this.mEST.getMaxImageWidthPx()), spanStart + iMin);
                    }
                }
            }
        }

        private void handleSelectAll() {
            if (!this.mEditFlag) {
                return;
            }
            this.mActions.onAction(11);
        }

        private void selectAll() {
            Selection.selectAll(this.mEST.getText());
            this.mCurStart = this.mEST.getSelectionStart();
            this.mCurEnd = this.mEST.getSelectionEnd();
            this.mMode = 5;
            this.mState = 3;
        }

        private void resetEdit() {
            endEdit();
            this.mEditFlag = EditStyledText.DBG;
            this.mEST.notifyStateChanged(this.mMode, this.mState);
        }

        private void setSelection() {
            Log.d(LOG_TAG, "--- onSelect:" + this.mCurStart + "," + this.mCurEnd);
            if (this.mCurStart >= 0 && this.mCurStart <= this.mEST.getText().length() && this.mCurEnd >= 0 && this.mCurEnd <= this.mEST.getText().length()) {
                if (this.mCurStart < this.mCurEnd) {
                    this.mEST.setSelection(this.mCurStart, this.mCurEnd);
                    this.mState = 2;
                    return;
                } else if (this.mCurStart > this.mCurEnd) {
                    this.mEST.setSelection(this.mCurEnd, this.mCurStart);
                    this.mState = 2;
                    return;
                } else {
                    this.mState = 1;
                    return;
                }
            }
            Log.e(LOG_TAG, "Select is on, but cursor positions are illigal.:" + this.mEST.getText().length() + "," + this.mCurStart + "," + this.mCurEnd);
        }

        private void unsetSelect() {
            Log.d(LOG_TAG, "--- offSelect");
            EditStyledText.stopSelecting(this.mEST, this.mEST.getText());
            int selectionStart = this.mEST.getSelectionStart();
            this.mEST.setSelection(selectionStart, selectionStart);
            this.mState = 0;
        }

        private void setSelectStartPos() {
            Log.d(LOG_TAG, "--- setSelectStartPos");
            this.mCurStart = this.mEST.getSelectionStart();
            this.mState = 1;
        }

        private void setSelectEndPos() {
            if (this.mEST.getSelectionEnd() == this.mCurStart) {
                setEndPos(this.mEST.getSelectionStart());
            } else {
                setEndPos(this.mEST.getSelectionEnd());
            }
        }

        public void setEndPos(int i) {
            Log.d(LOG_TAG, "--- setSelectedEndPos:" + i);
            this.mCurEnd = i;
            setSelection();
        }

        private boolean isWaitingNextAction() {
            Log.d(LOG_TAG, "--- waitingNext:" + this.mCurStart + "," + this.mCurEnd + "," + this.mState);
            if (this.mCurStart == this.mCurEnd && this.mState == 3) {
                waitSelection();
                return EditStyledText.DBG;
            }
            resumeSelection();
            return false;
        }

        private void waitSelection() {
            Log.d(LOG_TAG, "--- waitSelection");
            this.mWaitInputFlag = EditStyledText.DBG;
            if (this.mCurStart == this.mCurEnd) {
                this.mState = 1;
            } else {
                this.mState = 2;
            }
            EditStyledText.startSelecting(this.mEST, this.mEST.getText());
        }

        private void resumeSelection() {
            Log.d(LOG_TAG, "--- resumeSelection");
            this.mWaitInputFlag = false;
            this.mState = 3;
            EditStyledText.stopSelecting(this.mEST, this.mEST.getText());
        }

        private boolean isTextSelected() {
            if (this.mState == 2 || this.mState == 3) {
                return EditStyledText.DBG;
            }
            return false;
        }

        private void setStyledTextSpan(Object obj, int i, int i2) {
            Log.d(LOG_TAG, "--- setStyledTextSpan:" + this.mMode + "," + i + "," + i2);
            int iMin = Math.min(i, i2);
            int iMax = Math.max(i, i2);
            this.mEST.getText().setSpan(obj, iMin, iMax, 33);
            Selection.setSelection(this.mEST.getText(), iMax);
        }

        private void setLineStyledTextSpan(Object obj) {
            int iMin = Math.min(this.mCurStart, this.mCurEnd);
            int iMax = Math.max(this.mCurStart, this.mCurEnd);
            int selectionStart = this.mEST.getSelectionStart();
            int iFindLineStart = findLineStart(this.mEST.getText(), iMin);
            int iFindLineEnd = findLineEnd(this.mEST.getText(), iMax);
            if (iFindLineStart == iFindLineEnd) {
                this.mEST.getText().insert(iFindLineEnd, "\n");
                setStyledTextSpan(obj, iFindLineStart, iFindLineEnd + 1);
            } else {
                setStyledTextSpan(obj, iFindLineStart, iFindLineEnd);
            }
            Selection.setSelection(this.mEST.getText(), selectionStart);
        }

        private void changeSizeSelectedText(int i) {
            if (this.mCurStart != this.mCurEnd) {
                setStyledTextSpan(new AbsoluteSizeSpan(i), this.mCurStart, this.mCurEnd);
            } else {
                Log.e(LOG_TAG, "---changeSize: Size of the span is zero");
            }
        }

        private void changeColorSelectedText(int i) {
            if (this.mCurStart != this.mCurEnd) {
                setStyledTextSpan(new ForegroundColorSpan(i), this.mCurStart, this.mCurEnd);
            } else {
                Log.e(LOG_TAG, "---changeColor: Size of the span is zero");
            }
        }

        private void changeAlign(Layout.Alignment alignment) {
            setLineStyledTextSpan(new AlignmentSpan.Standard(alignment));
        }

        private void addTelop() {
            addMarquee(1);
        }

        private void addSwing() {
            addMarquee(0);
        }

        private void addMarquee(int i) {
            Log.d(LOG_TAG, "--- addMarquee:" + i);
            setLineStyledTextSpan(new EditStyledTextSpans.MarqueeSpan(i, this.mEST.getBackgroundColor()));
        }

        private void insertImageSpan(DynamicDrawableSpan dynamicDrawableSpan, int i) {
            Log.d(LOG_TAG, "--- insertImageSpan:");
            if (dynamicDrawableSpan != null && dynamicDrawableSpan.getDrawable() != null) {
                this.mEST.getText().insert(i, "￼");
                this.mEST.getText().setSpan(dynamicDrawableSpan, i, i + 1, 33);
                this.mEST.notifyStateChanged(this.mMode, this.mState);
            } else {
                Log.e(LOG_TAG, "--- insertImageSpan: null span was inserted");
                this.mEST.sendHintMessage(5);
            }
        }

        private int findLineStart(Editable editable, int i) {
            int i2 = i;
            while (i2 > 0 && editable.charAt(i2 - 1) != '\n') {
                i2--;
            }
            Log.d(LOG_TAG, "--- findLineStart:" + i + "," + editable.length() + "," + i2);
            return i2;
        }

        private int findLineEnd(Editable editable, int i) {
            int i2 = i;
            while (true) {
                if (i2 >= editable.length()) {
                    break;
                }
                if (editable.charAt(i2) != '\n') {
                    i2++;
                } else {
                    i2++;
                    break;
                }
            }
            Log.d(LOG_TAG, "--- findLineEnd:" + i + "," + editable.length() + "," + i2);
            return i2;
        }

        private void dumpSpannableString(CharSequence charSequence) {
            if (charSequence instanceof Spannable) {
                Spannable spannable = (Spannable) charSequence;
                int length = spannable.length();
                Log.d(EditStyledText.TAG, "--- dumpSpannableString, txt:" + ((Object) spannable) + ", len:" + length);
                for (Object obj : spannable.getSpans(0, length, Object.class)) {
                    Log.d(EditStyledText.TAG, "--- dumpSpannableString, class:" + obj + "," + spannable.getSpanStart(obj) + "," + spannable.getSpanEnd(obj) + "," + spannable.getSpanFlags(obj));
                }
            }
        }

        public void showSoftKey() {
            showSoftKey(this.mEST.getSelectionStart(), this.mEST.getSelectionEnd());
        }

        public void showSoftKey(int i, int i2) {
            Log.d(LOG_TAG, "--- showsoftkey");
            if (!this.mEST.isFocused() || isSoftKeyBlocked()) {
                return;
            }
            this.mSkr.mNewStart = Selection.getSelectionStart(this.mEST.getText());
            this.mSkr.mNewEnd = Selection.getSelectionEnd(this.mEST.getText());
            if (((InputMethodManager) EditStyledText.this.getContext().getSystemService("input_method")).showSoftInput(this.mEST, 0, this.mSkr) && this.mSkr != null) {
                Selection.setSelection(EditStyledText.this.getText(), i, i2);
            }
        }

        public void hideSoftKey() {
            Log.d(LOG_TAG, "--- hidesoftkey");
            if (!this.mEST.isFocused()) {
                return;
            }
            this.mSkr.mNewStart = Selection.getSelectionStart(this.mEST.getText());
            this.mSkr.mNewEnd = Selection.getSelectionEnd(this.mEST.getText());
            ((InputMethodManager) this.mEST.getContext().getSystemService("input_method")).hideSoftInputFromWindow(this.mEST.getWindowToken(), 0, this.mSkr);
        }

        public void blockSoftKey() {
            Log.d(LOG_TAG, "--- blockSoftKey:");
            hideSoftKey();
            this.mSoftKeyBlockFlag = EditStyledText.DBG;
        }

        public void unblockSoftKey() {
            Log.d(LOG_TAG, "--- unblockSoftKey:");
            this.mSoftKeyBlockFlag = false;
        }
    }

    private class StyledTextHtmlStandard implements StyledTextHtmlConverter {
        private StyledTextHtmlStandard() {
        }

        @Override
        public String toHtml(Spanned spanned) {
            return Html.toHtml(spanned);
        }

        @Override
        public String toHtml(Spanned spanned, boolean z) {
            return Html.toHtml(spanned);
        }

        @Override
        public String toHtml(Spanned spanned, boolean z, int i, float f) {
            return Html.toHtml(spanned);
        }

        @Override
        public Spanned fromHtml(String str) {
            return Html.fromHtml(str);
        }

        @Override
        public Spanned fromHtml(String str, Html.ImageGetter imageGetter, Html.TagHandler tagHandler) {
            return Html.fromHtml(str, imageGetter, tagHandler);
        }
    }

    private class StyledTextConverter {
        private EditStyledText mEST;
        private StyledTextHtmlConverter mHtml;

        public StyledTextConverter(EditStyledText editStyledText, StyledTextHtmlConverter styledTextHtmlConverter) {
            this.mEST = editStyledText;
            this.mHtml = styledTextHtmlConverter;
        }

        public void setStyledTextHtmlConverter(StyledTextHtmlConverter styledTextHtmlConverter) {
            this.mHtml = styledTextHtmlConverter;
        }

        public String getHtml(boolean z) {
            this.mEST.clearComposingText();
            this.mEST.onRefreshZeoWidthChar();
            String html = this.mHtml.toHtml(this.mEST.getText(), z);
            Log.d(EditStyledText.TAG, "--- getHtml:" + html);
            return html;
        }

        public String getPreviewHtml() {
            this.mEST.clearComposingText();
            this.mEST.onRefreshZeoWidthChar();
            String html = this.mHtml.toHtml(this.mEST.getText(), EditStyledText.DBG, EditStyledText.this.getMaxImageWidthDip(), EditStyledText.this.getPaddingScale());
            int backgroundColor = this.mEST.getBackgroundColor();
            String str = String.format("<body bgcolor=\"#%02X%02X%02X\">%s</body>", Integer.valueOf(Color.red(backgroundColor)), Integer.valueOf(Color.green(backgroundColor)), Integer.valueOf(Color.blue(backgroundColor)), html);
            Log.d(EditStyledText.TAG, "--- getPreviewHtml:" + str + "," + this.mEST.getWidth());
            return str;
        }

        public void getUriArray(ArrayList<Uri> arrayList, Editable editable) {
            arrayList.clear();
            Log.d(EditStyledText.TAG, "--- getUriArray:");
            int length = editable.length();
            int i = 0;
            while (i < editable.length()) {
                int iNextSpanTransition = editable.nextSpanTransition(i, length, ImageSpan.class);
                ImageSpan[] imageSpanArr = (ImageSpan[]) editable.getSpans(i, iNextSpanTransition, ImageSpan.class);
                for (int i2 = 0; i2 < imageSpanArr.length; i2++) {
                    Log.d(EditStyledText.TAG, "--- getUriArray: foundArray" + imageSpanArr[i2].getSource());
                    arrayList.add(Uri.parse(imageSpanArr[i2].getSource()));
                }
                i = iNextSpanTransition;
            }
        }

        public void SetHtml(String str) {
            this.mEST.setText(this.mHtml.fromHtml(str, new Html.ImageGetter() {
                @Override
                public Drawable getDrawable(String str2) {
                    Bitmap bitmapDecodeStream;
                    Log.d(EditStyledText.TAG, "--- sethtml: src=" + str2);
                    if (!str2.startsWith("content://")) {
                        return null;
                    }
                    Uri uri = Uri.parse(str2);
                    try {
                        System.gc();
                        InputStream inputStreamOpenInputStream = StyledTextConverter.this.mEST.getContext().getContentResolver().openInputStream(uri);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = EditStyledText.DBG;
                        BitmapFactory.decodeStream(inputStreamOpenInputStream, null, options);
                        inputStreamOpenInputStream.close();
                        InputStream inputStreamOpenInputStream2 = StyledTextConverter.this.mEST.getContext().getContentResolver().openInputStream(uri);
                        int maxImageWidthPx = options.outWidth;
                        int maxImageWidthPx2 = options.outHeight;
                        if (options.outWidth > EditStyledText.this.getMaxImageWidthPx()) {
                            maxImageWidthPx = EditStyledText.this.getMaxImageWidthPx();
                            maxImageWidthPx2 = (maxImageWidthPx2 * EditStyledText.this.getMaxImageWidthPx()) / options.outWidth;
                            bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenInputStream2, new Rect(0, 0, maxImageWidthPx, maxImageWidthPx2), null);
                        } else {
                            bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenInputStream2);
                        }
                        BitmapDrawable bitmapDrawable = new BitmapDrawable(StyledTextConverter.this.mEST.getContext().getResources(), bitmapDecodeStream);
                        bitmapDrawable.setBounds(0, 0, maxImageWidthPx, maxImageWidthPx2);
                        inputStreamOpenInputStream2.close();
                        return bitmapDrawable;
                    } catch (Exception e) {
                        Log.e(EditStyledText.TAG, "--- set html: Failed to loaded content " + uri, e);
                        return null;
                    } catch (OutOfMemoryError e2) {
                        Log.e(EditStyledText.TAG, "OutOfMemoryError");
                        StyledTextConverter.this.mEST.setHint(5);
                        return null;
                    }
                }
            }, null));
        }
    }

    private static class SoftKeyReceiver extends ResultReceiver {
        EditStyledText mEST;
        int mNewEnd;
        int mNewStart;

        SoftKeyReceiver(EditStyledText editStyledText) {
            super(editStyledText.getHandler());
            this.mEST = editStyledText;
        }

        @Override
        protected void onReceiveResult(int i, Bundle bundle) {
            if (i != 2) {
                Selection.setSelection(this.mEST.getText(), this.mNewStart, this.mNewEnd);
            }
        }
    }

    public static class SavedStyledTextState extends View.BaseSavedState {
        public int mBackgroundColor;

        SavedStyledTextState(Parcelable parcelable) {
            super(parcelable);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.mBackgroundColor);
        }

        public String toString() {
            return "EditStyledText.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " bgcolor=" + this.mBackgroundColor + "}";
        }
    }

    private static class StyledTextDialog {
        private static final int TYPE_BACKGROUND = 1;
        private static final int TYPE_FOREGROUND = 0;
        private AlertDialog mAlertDialog;
        private CharSequence[] mAlignNames;
        private CharSequence mAlignTitle;
        private AlertDialog.Builder mBuilder;
        private CharSequence mColorDefaultMessage;
        private CharSequence[] mColorInts;
        private CharSequence[] mColorNames;
        private CharSequence mColorTitle;
        private EditStyledText mEST;
        private CharSequence[] mMarqueeNames;
        private CharSequence mMarqueeTitle;
        private CharSequence[] mSizeDisplayInts;
        private CharSequence[] mSizeNames;
        private CharSequence[] mSizeSendInts;
        private CharSequence mSizeTitle;

        public StyledTextDialog(EditStyledText editStyledText) {
            this.mEST = editStyledText;
        }

        public void setBuilder(AlertDialog.Builder builder) {
            this.mBuilder = builder;
        }

        public void setColorAlertParams(CharSequence charSequence, CharSequence[] charSequenceArr, CharSequence[] charSequenceArr2, CharSequence charSequence2) {
            this.mColorTitle = charSequence;
            this.mColorNames = charSequenceArr;
            this.mColorInts = charSequenceArr2;
            this.mColorDefaultMessage = charSequence2;
        }

        public void setSizeAlertParams(CharSequence charSequence, CharSequence[] charSequenceArr, CharSequence[] charSequenceArr2, CharSequence[] charSequenceArr3) {
            this.mSizeTitle = charSequence;
            this.mSizeNames = charSequenceArr;
            this.mSizeDisplayInts = charSequenceArr2;
            this.mSizeSendInts = charSequenceArr3;
        }

        public void setAlignAlertParams(CharSequence charSequence, CharSequence[] charSequenceArr) {
            this.mAlignTitle = charSequence;
            this.mAlignNames = charSequenceArr;
        }

        public void setMarqueeAlertParams(CharSequence charSequence, CharSequence[] charSequenceArr) {
            this.mMarqueeTitle = charSequence;
            this.mMarqueeNames = charSequenceArr;
        }

        private boolean checkColorAlertParams() {
            Log.d(EditStyledText.TAG, "--- checkParams");
            if (this.mBuilder == null) {
                Log.e(EditStyledText.TAG, "--- builder is null.");
                return false;
            }
            if (this.mColorTitle == null || this.mColorNames == null || this.mColorInts == null) {
                Log.e(EditStyledText.TAG, "--- color alert params are null.");
                return false;
            }
            if (this.mColorNames.length != this.mColorInts.length) {
                Log.e(EditStyledText.TAG, "--- the length of color alert params are different.");
                return false;
            }
            return EditStyledText.DBG;
        }

        private boolean checkSizeAlertParams() {
            Log.d(EditStyledText.TAG, "--- checkParams");
            if (this.mBuilder == null) {
                Log.e(EditStyledText.TAG, "--- builder is null.");
                return false;
            }
            if (this.mSizeTitle == null || this.mSizeNames == null || this.mSizeDisplayInts == null || this.mSizeSendInts == null) {
                Log.e(EditStyledText.TAG, "--- size alert params are null.");
                return false;
            }
            if (this.mSizeNames.length != this.mSizeDisplayInts.length && this.mSizeSendInts.length != this.mSizeDisplayInts.length) {
                Log.e(EditStyledText.TAG, "--- the length of size alert params are different.");
                return false;
            }
            return EditStyledText.DBG;
        }

        private boolean checkAlignAlertParams() {
            Log.d(EditStyledText.TAG, "--- checkAlignAlertParams");
            if (this.mBuilder == null) {
                Log.e(EditStyledText.TAG, "--- builder is null.");
                return false;
            }
            if (this.mAlignTitle == null) {
                Log.e(EditStyledText.TAG, "--- align alert params are null.");
                return false;
            }
            return EditStyledText.DBG;
        }

        private boolean checkMarqueeAlertParams() {
            Log.d(EditStyledText.TAG, "--- checkMarqueeAlertParams");
            if (this.mBuilder == null) {
                Log.e(EditStyledText.TAG, "--- builder is null.");
                return false;
            }
            if (this.mMarqueeTitle == null) {
                Log.e(EditStyledText.TAG, "--- Marquee alert params are null.");
                return false;
            }
            return EditStyledText.DBG;
        }

        private void buildDialogue(CharSequence charSequence, CharSequence[] charSequenceArr, DialogInterface.OnClickListener onClickListener) {
            this.mBuilder.setTitle(charSequence);
            this.mBuilder.setIcon(0);
            this.mBuilder.setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null);
            this.mBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    StyledTextDialog.this.mEST.onStartEdit();
                }
            });
            this.mBuilder.setItems(charSequenceArr, onClickListener);
            this.mBuilder.setView((View) null);
            this.mBuilder.setCancelable(EditStyledText.DBG);
            this.mBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Log.d(EditStyledText.TAG, "--- oncancel");
                    StyledTextDialog.this.mEST.onStartEdit();
                }
            });
            this.mBuilder.show();
        }

        private void buildAndShowColorDialogue(int i, CharSequence charSequence, int[] iArr) {
            int iDipToPx = this.mEST.dipToPx(50);
            int iDipToPx2 = this.mEST.dipToPx(2);
            int iDipToPx3 = this.mEST.dipToPx(15);
            this.mBuilder.setTitle(charSequence);
            this.mBuilder.setIcon(0);
            LinearLayout linearLayout = null;
            this.mBuilder.setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null);
            this.mBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    StyledTextDialog.this.mEST.onStartEdit();
                }
            });
            this.mBuilder.setItems((CharSequence[]) null, (DialogInterface.OnClickListener) null);
            LinearLayout linearLayout2 = new LinearLayout(this.mEST.getContext());
            linearLayout2.setOrientation(1);
            linearLayout2.setGravity(1);
            linearLayout2.setPadding(iDipToPx3, iDipToPx3, iDipToPx3, iDipToPx3);
            for (int i2 = 0; i2 < iArr.length; i2++) {
                if (i2 % 5 == 0) {
                    LinearLayout linearLayout3 = new LinearLayout(this.mEST.getContext());
                    linearLayout2.addView(linearLayout3);
                    linearLayout = linearLayout3;
                }
                Button button = new Button(this.mEST.getContext());
                button.setHeight(iDipToPx);
                button.setWidth(iDipToPx);
                button.setBackgroundDrawable(new ColorPaletteDrawable(iArr[i2], iDipToPx, iDipToPx, iDipToPx2));
                button.setDrawingCacheBackgroundColor(iArr[i2]);
                if (i == 0) {
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            StyledTextDialog.this.mEST.setItemColor(view.getDrawingCacheBackgroundColor());
                            if (StyledTextDialog.this.mAlertDialog != null) {
                                StyledTextDialog.this.mAlertDialog.setView(null);
                                StyledTextDialog.this.mAlertDialog.dismiss();
                                StyledTextDialog.this.mAlertDialog = null;
                                return;
                            }
                            Log.e(EditStyledText.TAG, "--- buildAndShowColorDialogue: can't find alertDialog");
                        }
                    });
                } else if (i == 1) {
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            StyledTextDialog.this.mEST.setBackgroundColor(view.getDrawingCacheBackgroundColor());
                            if (StyledTextDialog.this.mAlertDialog != null) {
                                StyledTextDialog.this.mAlertDialog.setView(null);
                                StyledTextDialog.this.mAlertDialog.dismiss();
                                StyledTextDialog.this.mAlertDialog = null;
                                return;
                            }
                            Log.e(EditStyledText.TAG, "--- buildAndShowColorDialogue: can't find alertDialog");
                        }
                    });
                }
                linearLayout.addView(button);
            }
            if (i == 1) {
                this.mBuilder.setPositiveButton(this.mColorDefaultMessage, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i3) {
                        StyledTextDialog.this.mEST.setBackgroundColor(EditStyledText.DEFAULT_TRANSPARENT_COLOR);
                    }
                });
            } else if (i == 0) {
                this.mBuilder.setPositiveButton(this.mColorDefaultMessage, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i3) {
                        StyledTextDialog.this.mEST.setItemColor(EditStyledText.DEFAULT_FOREGROUND_COLOR);
                    }
                });
            }
            this.mBuilder.setView(linearLayout2);
            this.mBuilder.setCancelable(EditStyledText.DBG);
            this.mBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    StyledTextDialog.this.mEST.onStartEdit();
                }
            });
            this.mAlertDialog = this.mBuilder.show();
        }

        private void onShowForegroundColorAlertDialog() {
            Log.d(EditStyledText.TAG, "--- onShowForegroundColorAlertDialog");
            if (!checkColorAlertParams()) {
                return;
            }
            int[] iArr = new int[this.mColorInts.length];
            for (int i = 0; i < iArr.length; i++) {
                iArr[i] = Integer.parseInt((String) this.mColorInts[i], 16) - 16777216;
            }
            buildAndShowColorDialogue(0, this.mColorTitle, iArr);
        }

        private void onShowBackgroundColorAlertDialog() {
            Log.d(EditStyledText.TAG, "--- onShowBackgroundColorAlertDialog");
            if (!checkColorAlertParams()) {
                return;
            }
            int[] iArr = new int[this.mColorInts.length];
            for (int i = 0; i < iArr.length; i++) {
                iArr[i] = Integer.parseInt((String) this.mColorInts[i], 16) - 16777216;
            }
            buildAndShowColorDialogue(1, this.mColorTitle, iArr);
        }

        private void onShowSizeAlertDialog() {
            Log.d(EditStyledText.TAG, "--- onShowSizeAlertDialog");
            if (!checkSizeAlertParams()) {
                return;
            }
            buildDialogue(this.mSizeTitle, this.mSizeNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Log.d(EditStyledText.TAG, "mBuilder.onclick:" + i);
                    StyledTextDialog.this.mEST.setItemSize(StyledTextDialog.this.mEST.dipToPx(Integer.parseInt((String) StyledTextDialog.this.mSizeDisplayInts[i])));
                }
            });
        }

        private void onShowAlignAlertDialog() {
            Log.d(EditStyledText.TAG, "--- onShowAlignAlertDialog");
            if (!checkAlignAlertParams()) {
                return;
            }
            buildDialogue(this.mAlignTitle, this.mAlignNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
                    switch (i) {
                        case 0:
                            alignment = Layout.Alignment.ALIGN_NORMAL;
                            break;
                        case 1:
                            alignment = Layout.Alignment.ALIGN_CENTER;
                            break;
                        case 2:
                            alignment = Layout.Alignment.ALIGN_OPPOSITE;
                            break;
                        default:
                            Log.e(EditStyledText.TAG, "--- onShowAlignAlertDialog: got illigal align.");
                            break;
                    }
                    StyledTextDialog.this.mEST.setAlignment(alignment);
                }
            });
        }

        private void onShowMarqueeAlertDialog() {
            Log.d(EditStyledText.TAG, "--- onShowMarqueeAlertDialog");
            if (!checkMarqueeAlertParams()) {
                return;
            }
            buildDialogue(this.mMarqueeTitle, this.mMarqueeNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Log.d(EditStyledText.TAG, "mBuilder.onclick:" + i);
                    StyledTextDialog.this.mEST.setMarquee(i);
                }
            });
        }
    }

    private class MenuHandler implements MenuItem.OnMenuItemClickListener {
        private MenuHandler() {
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            return EditStyledText.this.onTextContextMenuItem(menuItem.getItemId());
        }
    }

    private static class StyledTextArrowKeyMethod extends ArrowKeyMovementMethod {
        String LOG_TAG = "StyledTextArrowKeyMethod";
        EditorManager mManager;

        StyledTextArrowKeyMethod(EditorManager editorManager) {
            this.mManager = editorManager;
        }

        @Override
        public boolean onKeyDown(TextView textView, Spannable spannable, int i, KeyEvent keyEvent) {
            Log.d(this.LOG_TAG, "---onkeydown:" + i);
            this.mManager.unsetTextComposingMask();
            if (this.mManager.getSelectState() == 1 || this.mManager.getSelectState() == 2) {
                return executeDown(textView, spannable, i);
            }
            return super.onKeyDown(textView, spannable, i, keyEvent);
        }

        private int getEndPos(TextView textView) {
            if (textView.getSelectionStart() == this.mManager.getSelectionStart()) {
                return textView.getSelectionEnd();
            }
            return textView.getSelectionStart();
        }

        @Override
        protected boolean up(TextView textView, Spannable spannable) {
            int lineStart;
            Log.d(this.LOG_TAG, "--- up:");
            Layout layout = textView.getLayout();
            int endPos = getEndPos(textView);
            int lineForOffset = layout.getLineForOffset(endPos);
            if (lineForOffset > 0) {
                int paragraphDirection = layout.getParagraphDirection(lineForOffset);
                int i = lineForOffset - 1;
                if (paragraphDirection == layout.getParagraphDirection(i)) {
                    lineStart = layout.getOffsetForHorizontal(i, layout.getPrimaryHorizontal(endPos));
                } else {
                    lineStart = layout.getLineStart(i);
                }
                this.mManager.setEndPos(lineStart);
                this.mManager.onCursorMoved();
            }
            return EditStyledText.DBG;
        }

        @Override
        protected boolean down(TextView textView, Spannable spannable) {
            int lineStart;
            Log.d(this.LOG_TAG, "--- down:");
            Layout layout = textView.getLayout();
            int endPos = getEndPos(textView);
            int lineForOffset = layout.getLineForOffset(endPos);
            if (lineForOffset < layout.getLineCount() - 1) {
                int paragraphDirection = layout.getParagraphDirection(lineForOffset);
                int i = lineForOffset + 1;
                if (paragraphDirection == layout.getParagraphDirection(i)) {
                    lineStart = layout.getOffsetForHorizontal(i, layout.getPrimaryHorizontal(endPos));
                } else {
                    lineStart = layout.getLineStart(i);
                }
                this.mManager.setEndPos(lineStart);
                this.mManager.onCursorMoved();
            }
            return EditStyledText.DBG;
        }

        @Override
        protected boolean left(TextView textView, Spannable spannable) {
            Log.d(this.LOG_TAG, "--- left:");
            this.mManager.setEndPos(textView.getLayout().getOffsetToLeftOf(getEndPos(textView)));
            this.mManager.onCursorMoved();
            return EditStyledText.DBG;
        }

        @Override
        protected boolean right(TextView textView, Spannable spannable) {
            Log.d(this.LOG_TAG, "--- right:");
            this.mManager.setEndPos(textView.getLayout().getOffsetToRightOf(getEndPos(textView)));
            this.mManager.onCursorMoved();
            return EditStyledText.DBG;
        }

        private boolean executeDown(TextView textView, Spannable spannable, int i) {
            Log.d(this.LOG_TAG, "--- executeDown: " + i);
            switch (i) {
                case 19:
                    return false | up(textView, spannable);
                case 20:
                    return false | down(textView, spannable);
                case 21:
                    return false | left(textView, spannable);
                case EditStyledText.MODE_RESET:
                    return false | right(textView, spannable);
                case EditStyledText.MODE_SHOW_MENU:
                    this.mManager.onFixSelectedItem();
                    return EditStyledText.DBG;
                default:
                    return false;
            }
        }
    }

    public static class StyledTextInputConnection extends InputConnectionWrapper {
        EditStyledText mEST;

        public StyledTextInputConnection(InputConnection inputConnection, EditStyledText editStyledText) {
            super(inputConnection, EditStyledText.DBG);
            this.mEST = editStyledText;
        }

        @Override
        public boolean commitText(CharSequence charSequence, int i) {
            Log.d(EditStyledText.TAG, "--- commitText:");
            this.mEST.mManager.unsetTextComposingMask();
            return super.commitText(charSequence, i);
        }

        @Override
        public boolean finishComposingText() {
            Log.d(EditStyledText.TAG, "--- finishcomposing:");
            if (!this.mEST.isSoftKeyBlocked() && !this.mEST.isButtonsFocused() && !this.mEST.isEditting()) {
                this.mEST.onEndEdit();
            }
            return super.finishComposingText();
        }
    }

    public static class EditStyledTextSpans {
        private static final String LOG_TAG = "EditStyledTextSpan";

        public static class HorizontalLineSpan extends DynamicDrawableSpan {
            HorizontalLineDrawable mDrawable;

            public HorizontalLineSpan(int i, int i2, Spannable spannable) {
                super(0);
                this.mDrawable = new HorizontalLineDrawable(i, i2, spannable);
            }

            @Override
            public Drawable getDrawable() {
                return this.mDrawable;
            }

            public void resetWidth(int i) {
                this.mDrawable.renewBounds(i);
            }

            public int getColor() {
                return this.mDrawable.getPaint().getColor();
            }
        }

        public static class MarqueeSpan extends CharacterStyle {
            public static final int ALTERNATE = 1;
            public static final int NOTHING = 2;
            public static final int SCROLL = 0;
            private int mMarqueeColor;
            private int mType;

            public MarqueeSpan(int i, int i2) {
                this.mType = i;
                checkType(i);
                this.mMarqueeColor = getMarqueeColor(i, i2);
            }

            public MarqueeSpan(int i) {
                this(i, EditStyledText.DEFAULT_TRANSPARENT_COLOR);
            }

            public int getType() {
                return this.mType;
            }

            public void resetColor(int i) {
                this.mMarqueeColor = getMarqueeColor(this.mType, i);
            }

            private int getMarqueeColor(int i, int i2) {
                int iAlpha = Color.alpha(i2);
                int iRed = Color.red(i2);
                int iGreen = Color.green(i2);
                int iBlue = Color.blue(i2);
                if (iAlpha == 0) {
                    iAlpha = 128;
                }
                switch (i) {
                    case 0:
                        iRed = iRed > 128 ? iRed / 2 : (255 - iRed) / 2;
                        break;
                    case 1:
                        iGreen = iGreen > 128 ? iGreen / 2 : (255 - iGreen) / 2;
                        break;
                    case 2:
                        return EditStyledText.DEFAULT_TRANSPARENT_COLOR;
                    default:
                        Log.e(EditStyledText.TAG, "--- getMarqueeColor: got illigal marquee ID.");
                        return EditStyledText.DEFAULT_TRANSPARENT_COLOR;
                }
                return Color.argb(iAlpha, iRed, iGreen, iBlue);
            }

            private boolean checkType(int i) {
                if (i == 0 || i == 1) {
                    return EditStyledText.DBG;
                }
                Log.e(EditStyledTextSpans.LOG_TAG, "--- Invalid type of MarqueeSpan");
                return false;
            }

            @Override
            public void updateDrawState(TextPaint textPaint) {
                textPaint.bgColor = this.mMarqueeColor;
            }
        }

        public static class RescalableImageSpan extends ImageSpan {
            private final int MAXWIDTH;
            Uri mContentUri;
            private Context mContext;
            private Drawable mDrawable;
            public int mIntrinsicHeight;
            public int mIntrinsicWidth;

            public RescalableImageSpan(Context context, Uri uri, int i) {
                super(context, uri);
                this.mIntrinsicWidth = -1;
                this.mIntrinsicHeight = -1;
                this.mContext = context;
                this.mContentUri = uri;
                this.MAXWIDTH = i;
            }

            public RescalableImageSpan(Context context, int i, int i2) {
                super(context, i);
                this.mIntrinsicWidth = -1;
                this.mIntrinsicHeight = -1;
                this.mContext = context;
                this.MAXWIDTH = i2;
            }

            @Override
            public Drawable getDrawable() {
                Bitmap bitmapDecodeStream;
                if (this.mDrawable != null) {
                    return this.mDrawable;
                }
                if (this.mContentUri != null) {
                    System.gc();
                    try {
                        InputStream inputStreamOpenInputStream = this.mContext.getContentResolver().openInputStream(this.mContentUri);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = EditStyledText.DBG;
                        BitmapFactory.decodeStream(inputStreamOpenInputStream, null, options);
                        inputStreamOpenInputStream.close();
                        InputStream inputStreamOpenInputStream2 = this.mContext.getContentResolver().openInputStream(this.mContentUri);
                        int i = options.outWidth;
                        int i2 = options.outHeight;
                        this.mIntrinsicWidth = i;
                        this.mIntrinsicHeight = i2;
                        if (options.outWidth > this.MAXWIDTH) {
                            i = this.MAXWIDTH;
                            i2 = (i2 * this.MAXWIDTH) / options.outWidth;
                            bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenInputStream2, new Rect(0, 0, i, i2), null);
                        } else {
                            bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenInputStream2);
                        }
                        this.mDrawable = new BitmapDrawable(this.mContext.getResources(), bitmapDecodeStream);
                        this.mDrawable.setBounds(0, 0, i, i2);
                        inputStreamOpenInputStream2.close();
                    } catch (Exception e) {
                        Log.e(EditStyledTextSpans.LOG_TAG, "Failed to loaded content " + this.mContentUri, e);
                        return null;
                    } catch (OutOfMemoryError e2) {
                        Log.e(EditStyledTextSpans.LOG_TAG, "OutOfMemoryError");
                        return null;
                    }
                } else {
                    this.mDrawable = super.getDrawable();
                    rescaleBigImage(this.mDrawable);
                    this.mIntrinsicWidth = this.mDrawable.getIntrinsicWidth();
                    this.mIntrinsicHeight = this.mDrawable.getIntrinsicHeight();
                }
                return this.mDrawable;
            }

            public boolean isOverSize() {
                if (getDrawable().getIntrinsicWidth() > this.MAXWIDTH) {
                    return EditStyledText.DBG;
                }
                return false;
            }

            public Uri getContentUri() {
                return this.mContentUri;
            }

            private void rescaleBigImage(Drawable drawable) {
                Log.d(EditStyledTextSpans.LOG_TAG, "--- rescaleBigImage:");
                if (this.MAXWIDTH < 0) {
                    return;
                }
                int intrinsicWidth = drawable.getIntrinsicWidth();
                int intrinsicHeight = drawable.getIntrinsicHeight();
                Log.d(EditStyledTextSpans.LOG_TAG, "--- rescaleBigImage:" + intrinsicWidth + "," + intrinsicHeight + "," + this.MAXWIDTH);
                if (intrinsicWidth > this.MAXWIDTH) {
                    intrinsicWidth = this.MAXWIDTH;
                    intrinsicHeight = (intrinsicHeight * this.MAXWIDTH) / intrinsicWidth;
                }
                drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
            }
        }

        public static class HorizontalLineDrawable extends ShapeDrawable {
            private static boolean DBG_HL = false;
            private Spannable mSpannable;
            private int mWidth;

            public HorizontalLineDrawable(int i, int i2, Spannable spannable) {
                super(new RectShape());
                this.mSpannable = spannable;
                this.mWidth = i2;
                renewColor(i);
                renewBounds(i2);
            }

            @Override
            public void draw(Canvas canvas) {
                renewColor();
                canvas.drawRect(new Rect(0, 9, this.mWidth, 11), getPaint());
            }

            public void renewBounds(int i) {
                if (DBG_HL) {
                    Log.d(EditStyledTextSpans.LOG_TAG, "--- renewBounds:" + i);
                }
                if (i > 20) {
                    i -= 20;
                }
                this.mWidth = i;
                setBounds(0, 0, i, 20);
            }

            private void renewColor(int i) {
                if (DBG_HL) {
                    Log.d(EditStyledTextSpans.LOG_TAG, "--- renewColor:" + i);
                }
                getPaint().setColor(i);
            }

            private void renewColor() {
                HorizontalLineSpan parentSpan = getParentSpan();
                Spannable spannable = this.mSpannable;
                ForegroundColorSpan[] foregroundColorSpanArr = (ForegroundColorSpan[]) spannable.getSpans(spannable.getSpanStart(parentSpan), spannable.getSpanEnd(parentSpan), ForegroundColorSpan.class);
                if (DBG_HL) {
                    Log.d(EditStyledTextSpans.LOG_TAG, "--- renewColor:" + foregroundColorSpanArr.length);
                }
                if (foregroundColorSpanArr.length > 0) {
                    renewColor(foregroundColorSpanArr[foregroundColorSpanArr.length - 1].getForegroundColor());
                }
            }

            private HorizontalLineSpan getParentSpan() {
                Spannable spannable = this.mSpannable;
                HorizontalLineSpan[] horizontalLineSpanArr = (HorizontalLineSpan[]) spannable.getSpans(0, spannable.length(), HorizontalLineSpan.class);
                if (horizontalLineSpanArr.length > 0) {
                    for (HorizontalLineSpan horizontalLineSpan : horizontalLineSpanArr) {
                        if (horizontalLineSpan.getDrawable() == this) {
                            return horizontalLineSpan;
                        }
                    }
                }
                Log.e(EditStyledTextSpans.LOG_TAG, "---renewBounds: Couldn't find");
                return null;
            }
        }
    }

    public static class ColorPaletteDrawable extends ShapeDrawable {
        private Rect mRect;

        public ColorPaletteDrawable(int i, int i2, int i3, int i4) {
            super(new RectShape());
            this.mRect = new Rect(i4, i4, i2 - i4, i3 - i4);
            getPaint().setColor(i);
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawRect(this.mRect, getPaint());
        }
    }

    public class EditModeActions {
        private static final boolean DBG = true;
        private static final String TAG = "EditModeActions";
        private StyledTextDialog mDialog;
        private EditStyledText mEST;
        private EditorManager mManager;
        private int mMode = 0;
        private HashMap<Integer, EditModeActionBase> mActionMap = new HashMap<>();
        private NothingAction mNothingAction = new NothingAction();
        private CopyAction mCopyAction = new CopyAction();
        private PasteAction mPasteAction = new PasteAction();
        private SelectAction mSelectAction = new SelectAction();
        private CutAction mCutAction = new CutAction();
        private SelectAllAction mSelectAllAction = new SelectAllAction();
        private HorizontalLineAction mHorizontalLineAction = new HorizontalLineAction();
        private StopSelectionAction mStopSelectionAction = new StopSelectionAction();
        private ClearStylesAction mClearStylesAction = new ClearStylesAction();
        private ImageAction mImageAction = new ImageAction();
        private BackgroundColorAction mBackgroundColorAction = new BackgroundColorAction();
        private PreviewAction mPreviewAction = new PreviewAction();
        private CancelAction mCancelEditAction = new CancelAction();
        private TextViewAction mTextViewAction = new TextViewAction();
        private StartEditAction mStartEditAction = new StartEditAction();
        private EndEditAction mEndEditAction = new EndEditAction();
        private ResetAction mResetAction = new ResetAction();
        private ShowMenuAction mShowMenuAction = new ShowMenuAction();
        private AlignAction mAlignAction = new AlignAction();
        private TelopAction mTelopAction = new TelopAction();
        private SwingAction mSwingAction = new SwingAction();
        private MarqueeDialogAction mMarqueeDialogAction = new MarqueeDialogAction();
        private ColorAction mColorAction = new ColorAction();
        private SizeAction mSizeAction = new SizeAction();

        EditModeActions(EditStyledText editStyledText, EditorManager editorManager, StyledTextDialog styledTextDialog) {
            this.mEST = editStyledText;
            this.mManager = editorManager;
            this.mDialog = styledTextDialog;
            this.mActionMap.put(0, this.mNothingAction);
            this.mActionMap.put(1, this.mCopyAction);
            this.mActionMap.put(2, this.mPasteAction);
            this.mActionMap.put(5, this.mSelectAction);
            this.mActionMap.put(7, this.mCutAction);
            this.mActionMap.put(11, this.mSelectAllAction);
            this.mActionMap.put(12, this.mHorizontalLineAction);
            this.mActionMap.put(13, this.mStopSelectionAction);
            this.mActionMap.put(14, this.mClearStylesAction);
            this.mActionMap.put(15, this.mImageAction);
            this.mActionMap.put(16, this.mBackgroundColorAction);
            this.mActionMap.put(17, this.mPreviewAction);
            this.mActionMap.put(18, this.mCancelEditAction);
            this.mActionMap.put(19, this.mTextViewAction);
            this.mActionMap.put(20, this.mStartEditAction);
            this.mActionMap.put(21, this.mEndEditAction);
            this.mActionMap.put(22, this.mResetAction);
            this.mActionMap.put(23, this.mShowMenuAction);
            this.mActionMap.put(6, this.mAlignAction);
            this.mActionMap.put(8, this.mTelopAction);
            this.mActionMap.put(9, this.mSwingAction);
            this.mActionMap.put(10, this.mMarqueeDialogAction);
            this.mActionMap.put(4, this.mColorAction);
            this.mActionMap.put(3, this.mSizeAction);
        }

        public void addAction(int i, EditModeActionBase editModeActionBase) {
            this.mActionMap.put(Integer.valueOf(i), editModeActionBase);
        }

        public void onAction(int i, Object[] objArr) {
            getAction(i).addParams(objArr);
            this.mMode = i;
            doNext(i);
        }

        public void onAction(int i, Object obj) {
            onAction(i, new Object[]{obj});
        }

        public void onAction(int i) {
            onAction(i, (Object[]) null);
        }

        public void onSelectAction() {
            doNext(5);
        }

        private EditModeActionBase getAction(int i) {
            if (this.mActionMap.containsKey(Integer.valueOf(i))) {
                return this.mActionMap.get(Integer.valueOf(i));
            }
            return null;
        }

        public boolean doNext() {
            return doNext(this.mMode);
        }

        public boolean doNext(int i) {
            Log.d(TAG, "--- do the next action: " + i + "," + this.mManager.getSelectState());
            EditModeActionBase action = getAction(i);
            if (action == null) {
                Log.e(TAG, "--- invalid action error.");
                return false;
            }
            switch (this.mManager.getSelectState()) {
                case 3:
                    if (!this.mManager.isWaitInput()) {
                    }
                    break;
            }
            return false;
        }

        public class EditModeActionBase {
            private Object[] mParams;

            public EditModeActionBase() {
            }

            protected boolean canOverWrap() {
                return false;
            }

            protected boolean canSelect() {
                return false;
            }

            protected boolean canWaitInput() {
                return false;
            }

            protected boolean needSelection() {
                return false;
            }

            protected boolean isLine() {
                return false;
            }

            protected boolean doNotSelected() {
                return false;
            }

            protected boolean doStartPosIsSelected() {
                return doNotSelected();
            }

            protected boolean doEndPosIsSelected() {
                return doStartPosIsSelected();
            }

            protected boolean doSelectionIsFixed() {
                return doEndPosIsSelected();
            }

            protected boolean doSelectionIsFixedAndWaitingInput() {
                return doEndPosIsSelected();
            }

            protected boolean fixSelection() {
                EditModeActions.this.mEST.finishComposingText();
                EditModeActions.this.mManager.setSelectState(3);
                return EditModeActions.DBG;
            }

            protected void addParams(Object[] objArr) {
                this.mParams = objArr;
            }

            protected Object getParam(int i) {
                if (this.mParams == null || i > this.mParams.length) {
                    Log.d(EditModeActions.TAG, "--- Number of the parameter is out of bound.");
                    return null;
                }
                return this.mParams[i];
            }
        }

        public class NothingAction extends EditModeActionBase {
            public NothingAction() {
                super();
            }
        }

        public class TextViewActionBase extends EditModeActionBase {
            public TextViewActionBase() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                if (EditModeActions.this.mManager.getEditMode() == 0 || EditModeActions.this.mManager.getEditMode() == 5) {
                    EditModeActions.this.mManager.setEditMode(EditModeActions.this.mMode);
                    EditModeActions.this.onSelectAction();
                    return EditModeActions.DBG;
                }
                return false;
            }

            @Override
            protected boolean doEndPosIsSelected() {
                if (EditModeActions.this.mManager.getEditMode() == 0 || EditModeActions.this.mManager.getEditMode() == 5) {
                    EditModeActions.this.mManager.setEditMode(EditModeActions.this.mMode);
                    fixSelection();
                    EditModeActions.this.doNext();
                    return EditModeActions.DBG;
                }
                if (EditModeActions.this.mManager.getEditMode() != EditModeActions.this.mMode) {
                    EditModeActions.this.mManager.resetEdit();
                    EditModeActions.this.mManager.setEditMode(EditModeActions.this.mMode);
                    EditModeActions.this.doNext();
                    return EditModeActions.DBG;
                }
                return false;
            }
        }

        public class TextViewAction extends TextViewActionBase {
            public TextViewAction() {
                super();
            }

            @Override
            protected boolean doEndPosIsSelected() {
                if (super.doEndPosIsSelected()) {
                    return EditModeActions.DBG;
                }
                ?? param = getParam(0);
                if (param != 0 && (param instanceof Integer)) {
                    EditModeActions.this.mEST.onTextContextMenuItem(param.intValue());
                }
                EditModeActions.this.mManager.resetEdit();
                return EditModeActions.DBG;
            }
        }

        public class CopyAction extends TextViewActionBase {
            public CopyAction() {
                super();
            }

            @Override
            protected boolean doEndPosIsSelected() {
                if (super.doEndPosIsSelected()) {
                    return EditModeActions.DBG;
                }
                EditModeActions.this.mManager.copyToClipBoard();
                EditModeActions.this.mManager.resetEdit();
                return EditModeActions.DBG;
            }
        }

        public class CutAction extends TextViewActionBase {
            public CutAction() {
                super();
            }

            @Override
            protected boolean doEndPosIsSelected() {
                if (super.doEndPosIsSelected()) {
                    return EditModeActions.DBG;
                }
                EditModeActions.this.mManager.cutToClipBoard();
                EditModeActions.this.mManager.resetEdit();
                return EditModeActions.DBG;
            }
        }

        public class SelectAction extends EditModeActionBase {
            public SelectAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                if (EditModeActions.this.mManager.isTextSelected()) {
                    Log.e(EditModeActions.TAG, "Selection is off, but selected");
                }
                EditModeActions.this.mManager.setSelectStartPos();
                EditModeActions.this.mEST.sendHintMessage(3);
                return EditModeActions.DBG;
            }

            @Override
            protected boolean doStartPosIsSelected() {
                if (EditModeActions.this.mManager.isTextSelected()) {
                    Log.e(EditModeActions.TAG, "Selection now start, but selected");
                }
                EditModeActions.this.mManager.setSelectEndPos();
                EditModeActions.this.mEST.sendHintMessage(4);
                if (EditModeActions.this.mManager.getEditMode() != 5) {
                    EditModeActions.this.doNext(EditModeActions.this.mManager.getEditMode());
                    return EditModeActions.DBG;
                }
                return EditModeActions.DBG;
            }

            @Override
            protected boolean doSelectionIsFixed() {
                return false;
            }
        }

        public class PasteAction extends EditModeActionBase {
            public PasteAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mManager.pasteFromClipboard();
                EditModeActions.this.mManager.resetEdit();
                return EditModeActions.DBG;
            }
        }

        public class SelectAllAction extends EditModeActionBase {
            public SelectAllAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mManager.selectAll();
                return EditModeActions.DBG;
            }
        }

        public class HorizontalLineAction extends EditModeActionBase {
            public HorizontalLineAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mManager.insertHorizontalLine();
                return EditModeActions.DBG;
            }
        }

        public class ClearStylesAction extends EditModeActionBase {
            public ClearStylesAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mManager.clearStyles();
                return EditModeActions.DBG;
            }
        }

        public class StopSelectionAction extends EditModeActionBase {
            public StopSelectionAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mManager.fixSelectionAndDoNextAction();
                return EditModeActions.DBG;
            }
        }

        public class CancelAction extends EditModeActionBase {
            public CancelAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mEST.cancelViewManagers();
                return EditModeActions.DBG;
            }
        }

        public class ImageAction extends EditModeActionBase {
            public ImageAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                ?? param = getParam(0);
                if (param == 0) {
                    EditModeActions.this.mEST.showInsertImageSelectAlertDialog();
                    return EditModeActions.DBG;
                }
                if (param instanceof Uri) {
                    EditModeActions.this.mManager.insertImageFromUri(param);
                    return EditModeActions.DBG;
                }
                if (param instanceof Integer) {
                    EditModeActions.this.mManager.insertImageFromResId(param.intValue());
                    return EditModeActions.DBG;
                }
                return EditModeActions.DBG;
            }
        }

        public class BackgroundColorAction extends EditModeActionBase {
            public BackgroundColorAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mDialog.onShowBackgroundColorAlertDialog();
                return EditModeActions.DBG;
            }
        }

        public class PreviewAction extends EditModeActionBase {
            public PreviewAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mEST.showPreview();
                return EditModeActions.DBG;
            }
        }

        public class StartEditAction extends EditModeActionBase {
            public StartEditAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mManager.startEdit();
                return EditModeActions.DBG;
            }
        }

        public class EndEditAction extends EditModeActionBase {
            public EndEditAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mManager.endEdit();
                return EditModeActions.DBG;
            }
        }

        public class ResetAction extends EditModeActionBase {
            public ResetAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mManager.resetEdit();
                return EditModeActions.DBG;
            }
        }

        public class ShowMenuAction extends EditModeActionBase {
            public ShowMenuAction() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                EditModeActions.this.mEST.showMenuAlertDialog();
                return EditModeActions.DBG;
            }
        }

        public class SetSpanActionBase extends EditModeActionBase {
            public SetSpanActionBase() {
                super();
            }

            @Override
            protected boolean doNotSelected() {
                if (EditModeActions.this.mManager.getEditMode() == 0 || EditModeActions.this.mManager.getEditMode() == 5) {
                    EditModeActions.this.mManager.setEditMode(EditModeActions.this.mMode);
                    EditModeActions.this.mManager.setInternalSelection(EditModeActions.this.mEST.getSelectionStart(), EditModeActions.this.mEST.getSelectionEnd());
                    fixSelection();
                    EditModeActions.this.doNext();
                    return EditModeActions.DBG;
                }
                if (EditModeActions.this.mManager.getEditMode() == EditModeActions.this.mMode) {
                    return false;
                }
                Log.d(EditModeActions.TAG, "--- setspanactionbase" + EditModeActions.this.mManager.getEditMode() + "," + EditModeActions.this.mMode);
                if (!EditModeActions.this.mManager.isWaitInput()) {
                    EditModeActions.this.mManager.resetEdit();
                    EditModeActions.this.mManager.setEditMode(EditModeActions.this.mMode);
                } else {
                    EditModeActions.this.mManager.setEditMode(0);
                    EditModeActions.this.mManager.setSelectState(0);
                }
                EditModeActions.this.doNext();
                return EditModeActions.DBG;
            }

            @Override
            protected boolean doStartPosIsSelected() {
                if (EditModeActions.this.mManager.getEditMode() == 0 || EditModeActions.this.mManager.getEditMode() == 5) {
                    EditModeActions.this.mManager.setEditMode(EditModeActions.this.mMode);
                    EditModeActions.this.onSelectAction();
                    return EditModeActions.DBG;
                }
                return doNotSelected();
            }

            @Override
            protected boolean doEndPosIsSelected() {
                if (EditModeActions.this.mManager.getEditMode() == 0 || EditModeActions.this.mManager.getEditMode() == 5) {
                    EditModeActions.this.mManager.setEditMode(EditModeActions.this.mMode);
                    fixSelection();
                    EditModeActions.this.doNext();
                    return EditModeActions.DBG;
                }
                return doStartPosIsSelected();
            }

            @Override
            protected boolean doSelectionIsFixed() {
                if (!doEndPosIsSelected()) {
                    EditModeActions.this.mEST.sendHintMessage(0);
                    return false;
                }
                return EditModeActions.DBG;
            }
        }

        public class AlignAction extends SetSpanActionBase {
            public AlignAction() {
                super();
            }

            @Override
            protected boolean doSelectionIsFixed() {
                if (super.doSelectionIsFixed()) {
                    return EditModeActions.DBG;
                }
                EditModeActions.this.mDialog.onShowAlignAlertDialog();
                return EditModeActions.DBG;
            }
        }

        public class TelopAction extends SetSpanActionBase {
            public TelopAction() {
                super();
            }

            @Override
            protected boolean doSelectionIsFixed() {
                if (super.doSelectionIsFixed()) {
                    return EditModeActions.DBG;
                }
                EditModeActions.this.mManager.setTelop();
                return EditModeActions.DBG;
            }
        }

        public class SwingAction extends SetSpanActionBase {
            public SwingAction() {
                super();
            }

            @Override
            protected boolean doSelectionIsFixed() {
                if (super.doSelectionIsFixed()) {
                    return EditModeActions.DBG;
                }
                EditModeActions.this.mManager.setSwing();
                return EditModeActions.DBG;
            }
        }

        public class MarqueeDialogAction extends SetSpanActionBase {
            public MarqueeDialogAction() {
                super();
            }

            @Override
            protected boolean doSelectionIsFixed() {
                if (super.doSelectionIsFixed()) {
                    return EditModeActions.DBG;
                }
                EditModeActions.this.mDialog.onShowMarqueeAlertDialog();
                return EditModeActions.DBG;
            }
        }

        public class ColorAction extends SetSpanActionBase {
            public ColorAction() {
                super();
            }

            @Override
            protected boolean doSelectionIsFixed() {
                if (super.doSelectionIsFixed()) {
                    return EditModeActions.DBG;
                }
                EditModeActions.this.mDialog.onShowForegroundColorAlertDialog();
                return EditModeActions.DBG;
            }

            @Override
            protected boolean doSelectionIsFixedAndWaitingInput() {
                if (super.doSelectionIsFixedAndWaitingInput()) {
                    return EditModeActions.DBG;
                }
                int sizeWaitInput = EditModeActions.this.mManager.getSizeWaitInput();
                EditModeActions.this.mManager.setItemColor(EditModeActions.this.mManager.getColorWaitInput(), false);
                if (!EditModeActions.this.mManager.isWaitInput()) {
                    EditModeActions.this.mManager.setItemSize(sizeWaitInput, false);
                    EditModeActions.this.mManager.resetEdit();
                } else {
                    fixSelection();
                    EditModeActions.this.mDialog.onShowForegroundColorAlertDialog();
                }
                return EditModeActions.DBG;
            }
        }

        public class SizeAction extends SetSpanActionBase {
            public SizeAction() {
                super();
            }

            @Override
            protected boolean doSelectionIsFixed() {
                if (super.doSelectionIsFixed()) {
                    return EditModeActions.DBG;
                }
                EditModeActions.this.mDialog.onShowSizeAlertDialog();
                return EditModeActions.DBG;
            }

            @Override
            protected boolean doSelectionIsFixedAndWaitingInput() {
                if (super.doSelectionIsFixedAndWaitingInput()) {
                    return EditModeActions.DBG;
                }
                int colorWaitInput = EditModeActions.this.mManager.getColorWaitInput();
                EditModeActions.this.mManager.setItemSize(EditModeActions.this.mManager.getSizeWaitInput(), false);
                if (!EditModeActions.this.mManager.isWaitInput()) {
                    EditModeActions.this.mManager.setItemColor(colorWaitInput, false);
                    EditModeActions.this.mManager.resetEdit();
                } else {
                    fixSelection();
                    EditModeActions.this.mDialog.onShowSizeAlertDialog();
                }
                return EditModeActions.DBG;
            }
        }
    }
}

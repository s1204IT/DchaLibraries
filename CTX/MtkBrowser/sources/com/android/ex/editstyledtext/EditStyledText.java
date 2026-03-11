package com.android.ex.editstyledtext;

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class EditStyledText extends EditText {
    private static final NoCopySpan.Concrete SELECTING = new NoCopySpan.Concrete();
    private static CharSequence STR_CLEARSTYLES;
    private static CharSequence STR_HORIZONTALLINE;
    private static CharSequence STR_PASTE;
    private Drawable mDefaultBackground;
    private ArrayList<EditStyledTextNotifier> mESTNotifiers;
    private InputConnection mInputConnection;
    private EditorManager mManager;

    public class EditModeActions {
        private HashMap<Integer, EditModeActionBase> mActionMap;
        private EditorManager mManager;
        private int mMode;

        public class EditModeActionBase {
            private Object[] mParams;

            protected void addParams(Object[] objArr) {
                this.mParams = objArr;
            }

            protected boolean doEndPosIsSelected() {
                return doStartPosIsSelected();
            }

            protected boolean doNotSelected() {
                return false;
            }

            protected boolean doSelectionIsFixed() {
                return doEndPosIsSelected();
            }

            protected boolean doSelectionIsFixedAndWaitingInput() {
                return doEndPosIsSelected();
            }

            protected boolean doStartPosIsSelected() {
                return doNotSelected();
            }
        }

        private EditModeActionBase getAction(int i) {
            if (this.mActionMap.containsKey(Integer.valueOf(i))) {
                return this.mActionMap.get(Integer.valueOf(i));
            }
            return null;
        }

        public boolean doNext(int i) {
            Log.d("EditModeActions", "--- do the next action: " + i + "," + this.mManager.getSelectState());
            EditModeActionBase action = getAction(i);
            if (action == null) {
                Log.e("EditModeActions", "--- invalid action error.");
                return false;
            }
            switch (this.mManager.getSelectState()) {
                case 0:
                    return action.doNotSelected();
                case 1:
                    return action.doStartPosIsSelected();
                case 2:
                    return action.doEndPosIsSelected();
                case 3:
                    return this.mManager.isWaitInput() ? action.doSelectionIsFixedAndWaitingInput() : action.doSelectionIsFixed();
                default:
                    return false;
            }
        }

        public void onAction(int i) {
            onAction(i, null);
        }

        public void onAction(int i, Object[] objArr) {
            getAction(i).addParams(objArr);
            this.mMode = i;
            doNext(i);
        }

        public void onSelectAction() {
            doNext(5);
        }
    }

    public interface EditStyledTextNotifier {
        boolean isButtonsFocused();

        void onStateChanged(int i, int i2);

        boolean sendOnTouchEvent(MotionEvent motionEvent);
    }

    public static class EditStyledTextSpans {

        public static class HorizontalLineDrawable extends ShapeDrawable {
            private static boolean DBG_HL = false;
            private Spannable mSpannable;
            private int mWidth;

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
                Log.e("EditStyledTextSpan", "---renewBounds: Couldn't find");
                return null;
            }

            private void renewColor() {
                HorizontalLineSpan parentSpan = getParentSpan();
                Spannable spannable = this.mSpannable;
                ForegroundColorSpan[] foregroundColorSpanArr = (ForegroundColorSpan[]) spannable.getSpans(spannable.getSpanStart(parentSpan), spannable.getSpanEnd(parentSpan), ForegroundColorSpan.class);
                if (DBG_HL) {
                    Log.d("EditStyledTextSpan", "--- renewColor:" + foregroundColorSpanArr.length);
                }
                if (foregroundColorSpanArr.length > 0) {
                    renewColor(foregroundColorSpanArr[foregroundColorSpanArr.length - 1].getForegroundColor());
                }
            }

            private void renewColor(int i) {
                if (DBG_HL) {
                    Log.d("EditStyledTextSpan", "--- renewColor:" + i);
                }
                getPaint().setColor(i);
            }

            @Override
            public void draw(Canvas canvas) {
                renewColor();
                canvas.drawRect(new Rect(0, 9, this.mWidth, 11), getPaint());
            }

            public void renewBounds(int i) {
                if (DBG_HL) {
                    Log.d("EditStyledTextSpan", "--- renewBounds:" + i);
                }
                if (i > 20) {
                    i -= 20;
                }
                this.mWidth = i;
                setBounds(0, 0, i, 20);
            }
        }

        public static class HorizontalLineSpan extends DynamicDrawableSpan {
            HorizontalLineDrawable mDrawable;

            @Override
            public Drawable getDrawable() {
                return this.mDrawable;
            }

            public void resetWidth(int i) {
                this.mDrawable.renewBounds(i);
            }
        }

        public static class MarqueeSpan extends CharacterStyle {
            private int mMarqueeColor;
            private int mType;

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
                        iRed = iRed <= 128 ? (255 - iRed) / 2 : iRed / 2;
                        break;
                    case 1:
                        iGreen = iGreen <= 128 ? (255 - iGreen) / 2 : iGreen / 2;
                        break;
                    case 2:
                        return 16777215;
                    default:
                        Log.e("EditStyledText", "--- getMarqueeColor: got illigal marquee ID.");
                        return 16777215;
                }
                return Color.argb(iAlpha, iRed, iGreen, iBlue);
            }

            public void resetColor(int i) {
                this.mMarqueeColor = getMarqueeColor(this.mType, i);
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

            private void rescaleBigImage(Drawable drawable) {
                Log.d("EditStyledTextSpan", "--- rescaleBigImage:");
                if (this.MAXWIDTH < 0) {
                    return;
                }
                int intrinsicWidth = drawable.getIntrinsicWidth();
                int intrinsicHeight = drawable.getIntrinsicHeight();
                Log.d("EditStyledTextSpan", "--- rescaleBigImage:" + intrinsicWidth + "," + intrinsicHeight + "," + this.MAXWIDTH);
                if (intrinsicWidth > this.MAXWIDTH) {
                    intrinsicWidth = this.MAXWIDTH;
                    intrinsicHeight = (intrinsicHeight * this.MAXWIDTH) / intrinsicWidth;
                }
                drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
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
                        options.inJustDecodeBounds = true;
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
                        Log.e("EditStyledTextSpan", "Failed to loaded content " + this.mContentUri, e);
                        return null;
                    } catch (OutOfMemoryError e2) {
                        Log.e("EditStyledTextSpan", "OutOfMemoryError");
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
        }
    }

    private class EditorManager {
        private EditModeActions mActions;
        private int mBackgroundColor;
        private int mColorWaitInput;
        private BackgroundColorSpan mComposingTextMask;
        private SpannableStringBuilder mCopyBuffer;
        private int mCurEnd;
        private int mCurStart;
        private EditStyledText mEST;
        private boolean mEditFlag;
        private boolean mKeepNonLineSpan;
        private int mMode;
        private int mSizeWaitInput;
        private SoftKeyReceiver mSkr;
        private boolean mSoftKeyBlockFlag;
        private int mState;
        private boolean mTextIsFinishedFlag;
        private boolean mWaitInputFlag;
        final EditStyledText this$0;

        private void endEdit() {
            Log.d("EditStyledText.EditorManager", "--- handleCancel");
            this.mMode = 0;
            this.mState = 0;
            this.mEditFlag = false;
            this.mColorWaitInput = 16777215;
            this.mSizeWaitInput = 0;
            this.mWaitInputFlag = false;
            this.mSoftKeyBlockFlag = false;
            this.mKeepNonLineSpan = false;
            this.mTextIsFinishedFlag = false;
            unsetSelect();
            this.mEST.setOnClickListener(null);
            unblockSoftKey();
        }

        private int findLineEnd(Editable editable, int i) {
            int i2 = i;
            while (true) {
                if (i2 >= editable.length()) {
                    break;
                }
                if (editable.charAt(i2) == '\n') {
                    i2++;
                    break;
                }
                i2++;
            }
            Log.d("EditStyledText.EditorManager", "--- findLineEnd:" + i + "," + editable.length() + "," + i2);
            return i2;
        }

        private int findLineStart(Editable editable, int i) {
            int i2 = i;
            while (i2 > 0 && editable.charAt(i2 - 1) != '\n') {
                i2--;
            }
            Log.d("EditStyledText.EditorManager", "--- findLineStart:" + i + "," + editable.length() + "," + i2);
            return i2;
        }

        private void fixSelectionAndDoNextAction() {
            Log.d("EditStyledText.EditorManager", "--- handleComplete:" + this.mCurStart + "," + this.mCurEnd);
            if (this.mEditFlag) {
                if (this.mCurStart != this.mCurEnd) {
                    if (this.mState == 2) {
                        this.mState = 3;
                    }
                    this.mActions.doNext(this.mMode);
                    EditStyledText.stopSelecting(this.mEST, this.mEST.getText());
                    return;
                }
                Log.d("EditStyledText.EditorManager", "--- cancel handle complete:" + this.mCurStart);
                resetEdit();
            }
        }

        private void handleSelectAll() {
            if (this.mEditFlag) {
                this.mActions.onAction(11);
            }
        }

        private SpannableStringBuilder removeImageChar(SpannableStringBuilder spannableStringBuilder) {
            SpannableStringBuilder spannableStringBuilder2 = new SpannableStringBuilder(spannableStringBuilder);
            for (DynamicDrawableSpan dynamicDrawableSpan : (DynamicDrawableSpan[]) spannableStringBuilder2.getSpans(0, spannableStringBuilder2.length(), DynamicDrawableSpan.class)) {
                if ((dynamicDrawableSpan instanceof EditStyledTextSpans.HorizontalLineSpan) || (dynamicDrawableSpan instanceof EditStyledTextSpans.RescalableImageSpan)) {
                    spannableStringBuilder2.replace(spannableStringBuilder2.getSpanStart(dynamicDrawableSpan), spannableStringBuilder2.getSpanEnd(dynamicDrawableSpan), (CharSequence) "");
                }
            }
            return spannableStringBuilder2;
        }

        private void resetEdit() {
            endEdit();
            this.mEditFlag = true;
            this.mEST.notifyStateChanged(this.mMode, this.mState);
        }

        private void unsetSelect() {
            Log.d("EditStyledText.EditorManager", "--- offSelect");
            EditStyledText.stopSelecting(this.mEST, this.mEST.getText());
            int selectionStart = this.mEST.getSelectionStart();
            this.mEST.setSelection(selectionStart, selectionStart);
            this.mState = 0;
        }

        public void blockSoftKey() {
            Log.d("EditStyledText.EditorManager", "--- blockSoftKey:");
            hideSoftKey();
            this.mSoftKeyBlockFlag = true;
        }

        public boolean canPaste() {
            return this.mCopyBuffer != null && this.mCopyBuffer.length() > 0 && removeImageChar(this.mCopyBuffer).length() == 0;
        }

        public int getBackgroundColor() {
            return this.mBackgroundColor;
        }

        public int getSelectState() {
            return this.mState;
        }

        public void hideSoftKey() {
            Log.d("EditStyledText.EditorManager", "--- hidesoftkey");
            if (this.mEST.isFocused()) {
                this.mSkr.mNewStart = Selection.getSelectionStart(this.mEST.getText());
                this.mSkr.mNewEnd = Selection.getSelectionEnd(this.mEST.getText());
                ((InputMethodManager) this.mEST.getContext().getSystemService("input_method")).hideSoftInputFromWindow(this.mEST.getWindowToken(), 0, this.mSkr);
            }
        }

        public boolean isEditting() {
            return this.mEditFlag;
        }

        public boolean isSoftKeyBlocked() {
            return this.mSoftKeyBlockFlag;
        }

        public boolean isStyledText() {
            Editable text = this.mEST.getText();
            int length = text.length();
            return ((ParagraphStyle[]) text.getSpans(0, length, ParagraphStyle.class)).length > 0 || ((QuoteSpan[]) text.getSpans(0, length, QuoteSpan.class)).length > 0 || ((CharacterStyle[]) text.getSpans(0, length, CharacterStyle.class)).length > 0 || this.mBackgroundColor != 16777215;
        }

        public boolean isWaitInput() {
            return this.mWaitInputFlag;
        }

        public void onAction(int i) {
            onAction(i, true);
        }

        public void onAction(int i, boolean z) {
            this.mActions.onAction(i);
            if (z) {
                this.mEST.notifyStateChanged(this.mMode, this.mState);
            }
        }

        public void onClearStyles() {
            this.mActions.onAction(14);
        }

        public void onCursorMoved() {
            Log.d("EditStyledText.EditorManager", "--- onClickView");
            if (this.mState == 1 || this.mState == 2) {
                this.mActions.onSelectAction();
                this.mEST.notifyStateChanged(this.mMode, this.mState);
            }
        }

        public void onFixSelectedItem() {
            Log.d("EditStyledText.EditorManager", "--- onFixSelectedItem");
            fixSelectionAndDoNextAction();
            this.mEST.notifyStateChanged(this.mMode, this.mState);
        }

        public void onRefreshStyles() {
            Log.d("EditStyledText.EditorManager", "--- onRefreshStyles");
            Editable text = this.mEST.getText();
            int length = text.length();
            int width = this.mEST.getWidth();
            EditStyledTextSpans.HorizontalLineSpan[] horizontalLineSpanArr = (EditStyledTextSpans.HorizontalLineSpan[]) text.getSpans(0, length, EditStyledTextSpans.HorizontalLineSpan.class);
            for (EditStyledTextSpans.HorizontalLineSpan horizontalLineSpan : horizontalLineSpanArr) {
                horizontalLineSpan.resetWidth(width);
            }
            EditStyledTextSpans.MarqueeSpan[] marqueeSpanArr = (EditStyledTextSpans.MarqueeSpan[]) text.getSpans(0, length, EditStyledTextSpans.MarqueeSpan.class);
            for (EditStyledTextSpans.MarqueeSpan marqueeSpan : marqueeSpanArr) {
                marqueeSpan.resetColor(this.mEST.getBackgroundColor());
            }
            if (horizontalLineSpanArr.length > 0) {
                text.replace(0, 1, "" + text.charAt(0));
            }
        }

        public void onStartSelect(boolean z) {
            Log.d("EditStyledText.EditorManager", "--- onClickSelect");
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

        public void onStartSelectAll(boolean z) {
            Log.d("EditStyledText.EditorManager", "--- onClickSelectAll");
            handleSelectAll();
            if (z) {
                this.mEST.notifyStateChanged(this.mMode, this.mState);
            }
        }

        public void setBackgroundColor(int i) {
            this.mBackgroundColor = i;
        }

        public void setTextComposingMask(int i, int i2) {
            Log.d("EditStyledText", "--- setTextComposingMask:" + i + "," + i2);
            int iMin = Math.min(i, i2);
            int iMax = Math.max(i, i2);
            int foregroundColor = (!isWaitInput() || this.mColorWaitInput == 16777215) ? this.mEST.getForegroundColor(iMin) : this.mColorWaitInput;
            int backgroundColor = this.mEST.getBackgroundColor();
            Log.d("EditStyledText", "--- fg:" + Integer.toHexString(foregroundColor) + ",bg:" + Integer.toHexString(backgroundColor) + "," + isWaitInput() + ",," + this.mMode);
            if (foregroundColor == backgroundColor) {
                int i3 = Integer.MIN_VALUE | ((backgroundColor | (-16777216)) ^ (-1));
                if (this.mComposingTextMask == null || this.mComposingTextMask.getBackgroundColor() != i3) {
                    this.mComposingTextMask = new BackgroundColorSpan(i3);
                }
                this.mEST.getText().setSpan(this.mComposingTextMask, iMin, iMax, 33);
            }
        }

        public void showSoftKey(int i, int i2) {
            Log.d("EditStyledText.EditorManager", "--- showsoftkey");
            if (!this.mEST.isFocused() || isSoftKeyBlocked()) {
                return;
            }
            this.mSkr.mNewStart = Selection.getSelectionStart(this.mEST.getText());
            this.mSkr.mNewEnd = Selection.getSelectionEnd(this.mEST.getText());
            if (!((InputMethodManager) this.this$0.getContext().getSystemService("input_method")).showSoftInput(this.mEST, 0, this.mSkr) || this.mSkr == null) {
                return;
            }
            Selection.setSelection(this.this$0.getText(), i, i2);
        }

        public void unblockSoftKey() {
            Log.d("EditStyledText.EditorManager", "--- unblockSoftKey:");
            this.mSoftKeyBlockFlag = false;
        }

        public void unsetTextComposingMask() {
            Log.d("EditStyledText", "--- unsetTextComposingMask");
            if (this.mComposingTextMask != null) {
                this.mEST.getText().removeSpan(this.mComposingTextMask);
                this.mComposingTextMask = null;
            }
        }

        public void updateSpanNextToCursor(Editable editable, int i, int i2, int i3) {
            Log.d("EditStyledText.EditorManager", "updateSpanNext:" + i + "," + i2 + "," + i3);
            int i4 = i + i3;
            int iMin = Math.min(i, i4);
            int iMax = Math.max(i, i4);
            Object[] spans = editable.getSpans(iMax, iMax, Object.class);
            int length = spans.length;
            for (int i5 = 0; i5 < length; i5++) {
                Object obj = spans[i5];
                boolean z = obj instanceof EditStyledTextSpans.MarqueeSpan;
                if (z || (obj instanceof AlignmentSpan)) {
                    int spanStart = editable.getSpanStart(obj);
                    int spanEnd = editable.getSpanEnd(obj);
                    Log.d("EditStyledText.EditorManager", "spantype:" + obj.getClass() + "," + spanEnd);
                    if (((z || (obj instanceof AlignmentSpan)) ? findLineStart(this.mEST.getText(), iMin) : iMin) < spanStart && i2 > i3) {
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

        public void updateSpanPreviousFromCursor(Editable editable, int i, int i2, int i3) {
            Log.d("EditStyledText.EditorManager", "updateSpanPrevious:" + i + "," + i2 + "," + i3);
            int i4 = i + i3;
            int iMin = Math.min(i, i4);
            int iMax = Math.max(i, i4);
            Object[] spans = editable.getSpans(iMin, iMin, Object.class);
            for (Object obj : spans) {
                if ((obj instanceof ForegroundColorSpan) || (obj instanceof AbsoluteSizeSpan) || (obj instanceof EditStyledTextSpans.MarqueeSpan) || (obj instanceof AlignmentSpan)) {
                    int spanStart = editable.getSpanStart(obj);
                    int spanEnd = editable.getSpanEnd(obj);
                    Log.d("EditStyledText.EditorManager", "spantype:" + obj.getClass() + "," + spanStart);
                    int iFindLineEnd = ((obj instanceof EditStyledTextSpans.MarqueeSpan) || (obj instanceof AlignmentSpan)) ? findLineEnd(this.mEST.getText(), iMax) : this.mKeepNonLineSpan ? spanEnd : iMax;
                    if (spanEnd < iFindLineEnd) {
                        Log.d("EditStyledText.EditorManager", "updateSpanPrevious: extend span");
                        editable.setSpan(obj, spanStart, iFindLineEnd, 33);
                    }
                } else if (obj instanceof EditStyledTextSpans.HorizontalLineSpan) {
                    int spanStart2 = editable.getSpanStart(obj);
                    int spanEnd2 = editable.getSpanEnd(obj);
                    if (i2 > i3) {
                        editable.replace(spanStart2, spanEnd2, "");
                        editable.removeSpan(obj);
                    } else if (spanEnd2 == i4 && i4 < editable.length() && this.mEST.getText().charAt(i4) != '\n') {
                        this.mEST.getText().insert(i4, "\n");
                    }
                }
            }
        }
    }

    private class MenuHandler implements MenuItem.OnMenuItemClickListener {
        final EditStyledText this$0;

        private MenuHandler(EditStyledText editStyledText) {
            this.this$0 = editStyledText;
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            return this.this$0.onTextContextMenuItem(menuItem.getItemId());
        }
    }

    public static class SavedStyledTextState extends View.BaseSavedState {
        public int mBackgroundColor;

        SavedStyledTextState(Parcelable parcelable) {
            super(parcelable);
        }

        public String toString() {
            return "EditStyledText.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " bgcolor=" + this.mBackgroundColor + "}";
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.mBackgroundColor);
        }
    }

    private static class SoftKeyReceiver extends ResultReceiver {
        EditStyledText mEST;
        int mNewEnd;
        int mNewStart;

        @Override
        protected void onReceiveResult(int i, Bundle bundle) {
            if (i != 2) {
                Selection.setSelection(this.mEST.getText(), this.mNewStart, this.mNewEnd);
            }
        }
    }

    public static class StyledTextInputConnection extends InputConnectionWrapper {
        EditStyledText mEST;

        public StyledTextInputConnection(InputConnection inputConnection, EditStyledText editStyledText) {
            super(inputConnection, true);
            this.mEST = editStyledText;
        }

        @Override
        public boolean commitText(CharSequence charSequence, int i) {
            Log.d("EditStyledText", "--- commitText:");
            this.mEST.mManager.unsetTextComposingMask();
            return super.commitText(charSequence, i);
        }

        @Override
        public boolean finishComposingText() {
            Log.d("EditStyledText", "--- finishcomposing:");
            if (!this.mEST.isSoftKeyBlocked() && !this.mEST.isButtonsFocused() && !this.mEST.isEditting()) {
                this.mEST.onEndEdit();
            }
            return super.finishComposingText();
        }
    }

    public void notifyStateChanged(int i, int i2) {
        if (this.mESTNotifiers != null) {
            Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
            while (it.hasNext()) {
                it.next().onStateChanged(i, i2);
            }
        }
    }

    private void onRefreshStyles() {
        this.mManager.onRefreshStyles();
    }

    private void sendOnTouchEvent(MotionEvent motionEvent) {
        if (this.mESTNotifiers != null) {
            Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
            while (it.hasNext()) {
                it.next().sendOnTouchEvent(motionEvent);
            }
        }
    }

    public static void stopSelecting(View view, Spannable spannable) {
        spannable.removeSpan(SELECTING);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mManager != null) {
            this.mManager.onRefreshStyles();
        }
    }

    public int getBackgroundColor() {
        return this.mManager.getBackgroundColor();
    }

    public int getForegroundColor(int i) {
        if (i < 0 || i > getText().length()) {
            return -16777216;
        }
        ForegroundColorSpan[] foregroundColorSpanArr = (ForegroundColorSpan[]) getText().getSpans(i, i, ForegroundColorSpan.class);
        if (foregroundColorSpanArr.length > 0) {
            return foregroundColorSpanArr[0].getForegroundColor();
        }
        return -16777216;
    }

    public int getSelectState() {
        return this.mManager.getSelectState();
    }

    public boolean isButtonsFocused() {
        boolean zIsButtonsFocused = false;
        if (this.mESTNotifiers == null) {
            return false;
        }
        Iterator<EditStyledTextNotifier> it = this.mESTNotifiers.iterator();
        while (true) {
            boolean z = zIsButtonsFocused;
            if (!it.hasNext()) {
                return z;
            }
            zIsButtonsFocused = it.next().isButtonsFocused() | z;
        }
    }

    public boolean isEditting() {
        return this.mManager.isEditting();
    }

    public boolean isSoftKeyBlocked() {
        return this.mManager.isSoftKeyBlocked();
    }

    public boolean isStyledText() {
        return this.mManager.isStyledText();
    }

    public void onClearStyles() {
        this.mManager.onClearStyles();
    }

    @Override
    protected void onCreateContextMenu(ContextMenu contextMenu) {
        super.onCreateContextMenu(contextMenu);
        MenuHandler menuHandler = new MenuHandler();
        if (STR_HORIZONTALLINE != null) {
            contextMenu.add(0, 16776961, 0, STR_HORIZONTALLINE).setOnMenuItemClickListener(menuHandler);
        }
        if (isStyledText() && STR_CLEARSTYLES != null) {
            contextMenu.add(0, 16776962, 0, STR_CLEARSTYLES).setOnMenuItemClickListener(menuHandler);
        }
        if (this.mManager.canPaste()) {
            contextMenu.add(0, R.id.paste, 0, STR_PASTE).setOnMenuItemClickListener(menuHandler).setAlphabeticShortcut('v');
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        this.mInputConnection = new StyledTextInputConnection(super.onCreateInputConnection(editorInfo), this);
        return this.mInputConnection;
    }

    public void onEndEdit() {
        this.mManager.onAction(21);
    }

    public void onFixSelectedItem() {
        this.mManager.onFixSelectedItem();
    }

    @Override
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        super.onFocusChanged(z, i, rect);
        if (z) {
            onStartEdit();
        } else {
            if (isButtonsFocused()) {
                return;
            }
            onEndEdit();
        }
    }

    public void onInsertHorizontalLine() {
        this.mManager.onAction(12);
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedStyledTextState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedStyledTextState savedStyledTextState = (SavedStyledTextState) parcelable;
        super.onRestoreInstanceState(savedStyledTextState.getSuperState());
        setBackgroundColor(savedStyledTextState.mBackgroundColor);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedStyledTextState savedStyledTextState = new SavedStyledTextState(super.onSaveInstanceState());
        savedStyledTextState.mBackgroundColor = this.mManager.getBackgroundColor();
        return savedStyledTextState;
    }

    public void onStartCopy() {
        this.mManager.onAction(1);
    }

    public void onStartCut() {
        this.mManager.onAction(7);
    }

    public void onStartEdit() {
        this.mManager.onAction(20);
    }

    public void onStartPaste() {
        this.mManager.onAction(2);
    }

    public void onStartSelect() {
        this.mManager.onStartSelect(true);
    }

    public void onStartSelectAll() {
        this.mManager.onStartSelectAll(true);
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
    public boolean onTextContextMenuItem(int i) {
        boolean z = getSelectionStart() != getSelectionEnd();
        switch (i) {
            case 16776961:
                onInsertHorizontalLine();
                return true;
            case 16776962:
                onClearStyles();
                return true;
            case 16776963:
                onStartEdit();
                return true;
            case 16776964:
                onEndEdit();
                return true;
            default:
                switch (i) {
                    case R.id.selectAll:
                        onStartSelectAll();
                        return true;
                    case R.id.cut:
                        if (z) {
                            onStartCut();
                            return true;
                        }
                        this.mManager.onStartSelectAll(false);
                        onStartCut();
                        return true;
                    case R.id.copy:
                        if (z) {
                            onStartCopy();
                            return true;
                        }
                        this.mManager.onStartSelectAll(false);
                        onStartCopy();
                        return true;
                    case R.id.paste:
                        onStartPaste();
                        return true;
                    default:
                        switch (i) {
                            case R.id.startSelectingText:
                                onStartSelect();
                                this.mManager.blockSoftKey();
                                break;
                            case R.id.stopSelectingText:
                                onFixSelectedItem();
                                break;
                        }
                        return super.onTextContextMenuItem(i);
                }
        }
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
    public void setBackgroundColor(int i) {
        if (i != 16777215) {
            super.setBackgroundColor(i);
        } else {
            setBackgroundDrawable(this.mDefaultBackground);
        }
        this.mManager.setBackgroundColor(i);
        onRefreshStyles();
    }
}

package android.media;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.Cea708CCParser;
import android.media.ClosedCaptionWidget;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.widget.RelativeLayout;
import com.android.internal.widget.SubtitleView;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

class Cea708CCWidget extends ClosedCaptionWidget implements Cea708CCParser.DisplayListener {
    private final CCHandler mCCHandler;

    public Cea708CCWidget(Context context) {
        this(context, null);
    }

    public Cea708CCWidget(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public Cea708CCWidget(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public Cea708CCWidget(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mCCHandler = new CCHandler((CCLayout) this.mClosedCaptionLayout);
    }

    @Override
    public ClosedCaptionWidget.ClosedCaptionLayout createCaptionLayout(Context context) {
        return new CCLayout(context);
    }

    @Override
    public void emitEvent(Cea708CCParser.CaptionEvent captionEvent) {
        this.mCCHandler.processCaptionEvent(captionEvent);
        setSize(getWidth(), getHeight());
        if (this.mListener != null) {
            this.mListener.onChanged(this);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ((ViewGroup) this.mClosedCaptionLayout).draw(canvas);
    }

    static class ScaledLayout extends ViewGroup {
        private static final boolean DEBUG = false;
        private static final String TAG = "ScaledLayout";
        private static final Comparator<Rect> mRectTopLeftSorter = new Comparator<Rect>() {
            @Override
            public int compare(Rect rect, Rect rect2) {
                if (rect.top != rect2.top) {
                    return rect.top - rect2.top;
                }
                return rect.left - rect2.left;
            }
        };
        private Rect[] mRectArray;

        public ScaledLayout(Context context) {
            super(context);
        }

        static class ScaledLayoutParams extends ViewGroup.LayoutParams {
            public static final float SCALE_UNSPECIFIED = -1.0f;
            public float scaleEndCol;
            public float scaleEndRow;
            public float scaleStartCol;
            public float scaleStartRow;

            public ScaledLayoutParams(float f, float f2, float f3, float f4) {
                super(-1, -1);
                this.scaleStartRow = f;
                this.scaleEndRow = f2;
                this.scaleStartCol = f3;
                this.scaleEndCol = f4;
            }

            public ScaledLayoutParams(Context context, AttributeSet attributeSet) {
                super(-1, -1);
            }
        }

        @Override
        public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
            return new ScaledLayoutParams(getContext(), attributeSet);
        }

        @Override
        protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
            return layoutParams instanceof ScaledLayoutParams;
        }

        @Override
        protected void onMeasure(int i, int i2) {
            int i3;
            int size = View.MeasureSpec.getSize(i);
            int size2 = View.MeasureSpec.getSize(i2);
            int paddingLeft = (size - getPaddingLeft()) - getPaddingRight();
            int paddingTop = (size2 - getPaddingTop()) - getPaddingBottom();
            int childCount = getChildCount();
            this.mRectArray = new Rect[childCount];
            int i4 = 0;
            while (i4 < childCount) {
                View childAt = getChildAt(i4);
                ViewGroup.LayoutParams layoutParams = childAt.getLayoutParams();
                if (!(layoutParams instanceof ScaledLayoutParams)) {
                    throw new RuntimeException("A child of ScaledLayout cannot have the UNSPECIFIED scale factors");
                }
                ScaledLayoutParams scaledLayoutParams = (ScaledLayoutParams) layoutParams;
                float f = scaledLayoutParams.scaleStartRow;
                float f2 = scaledLayoutParams.scaleEndRow;
                float f3 = scaledLayoutParams.scaleStartCol;
                float f4 = scaledLayoutParams.scaleEndCol;
                if (f < 0.0f || f > 1.0f) {
                    throw new RuntimeException("A child of ScaledLayout should have a range of scaleStartRow between 0 and 1");
                }
                if (f2 < f || f > 1.0f) {
                    throw new RuntimeException("A child of ScaledLayout should have a range of scaleEndRow between scaleStartRow and 1");
                }
                if (f4 < 0.0f || f4 > 1.0f) {
                    throw new RuntimeException("A child of ScaledLayout should have a range of scaleStartCol between 0 and 1");
                }
                if (f4 < f3 || f4 > 1.0f) {
                    throw new RuntimeException("A child of ScaledLayout should have a range of scaleEndCol between scaleStartCol and 1");
                }
                float f5 = paddingLeft;
                int i5 = paddingLeft;
                float f6 = paddingTop;
                int i6 = size;
                int i7 = size2;
                int i8 = childCount;
                this.mRectArray[i4] = new Rect((int) (f3 * f5), (int) (f * f6), (int) (f4 * f5), (int) (f2 * f6));
                int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec((int) (f5 * (f4 - f3)), 1073741824);
                childAt.measure(iMakeMeasureSpec, View.MeasureSpec.makeMeasureSpec(0, 0));
                if (childAt.getMeasuredHeight() > this.mRectArray[i4].height()) {
                    int measuredHeight = ((childAt.getMeasuredHeight() - this.mRectArray[i4].height()) + 1) / 2;
                    this.mRectArray[i4].bottom += measuredHeight;
                    this.mRectArray[i4].top -= measuredHeight;
                    if (this.mRectArray[i4].top < 0) {
                        this.mRectArray[i4].bottom -= this.mRectArray[i4].top;
                        this.mRectArray[i4].top = 0;
                    }
                    if (this.mRectArray[i4].bottom > paddingTop) {
                        this.mRectArray[i4].top -= this.mRectArray[i4].bottom - paddingTop;
                        this.mRectArray[i4].bottom = paddingTop;
                    }
                }
                childAt.measure(iMakeMeasureSpec, View.MeasureSpec.makeMeasureSpec((int) (f6 * (f2 - f)), 1073741824));
                i4++;
                paddingLeft = i5;
                size = i6;
                size2 = i7;
                childCount = i8;
            }
            int i9 = size;
            int i10 = size2;
            int i11 = childCount;
            int[] iArr = new int[i11];
            Rect[] rectArr = new Rect[i11];
            int i12 = 0;
            for (int i13 = 0; i13 < i11; i13++) {
                if (getChildAt(i13).getVisibility() == 0) {
                    iArr[i12] = i12;
                    rectArr[i12] = this.mRectArray[i13];
                    i12++;
                }
            }
            Arrays.sort(rectArr, 0, i12, mRectTopLeftSorter);
            int i14 = 0;
            while (true) {
                i3 = i12 - 1;
                if (i14 >= i3) {
                    break;
                }
                int i15 = i14 + 1;
                for (int i16 = i15; i16 < i12; i16++) {
                    if (Rect.intersects(rectArr[i14], rectArr[i16])) {
                        iArr[i16] = iArr[i14];
                        rectArr[i16].set(rectArr[i16].left, rectArr[i14].bottom, rectArr[i16].right, rectArr[i14].bottom + rectArr[i16].height());
                    }
                }
                i14 = i15;
            }
            while (i3 >= 0) {
                if (rectArr[i3].bottom > paddingTop) {
                    int i17 = rectArr[i3].bottom - paddingTop;
                    for (int i18 = 0; i18 <= i3; i18++) {
                        if (iArr[i3] == iArr[i18]) {
                            rectArr[i18].set(rectArr[i18].left, rectArr[i18].top - i17, rectArr[i18].right, rectArr[i18].bottom - i17);
                        }
                    }
                }
                i3--;
            }
            setMeasuredDimension(i9, i10);
        }

        @Override
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            int paddingLeft = getPaddingLeft();
            int paddingTop = getPaddingTop();
            int childCount = getChildCount();
            for (int i5 = 0; i5 < childCount; i5++) {
                View childAt = getChildAt(i5);
                if (childAt.getVisibility() != 8) {
                    childAt.layout(this.mRectArray[i5].left + paddingLeft, this.mRectArray[i5].top + paddingTop, this.mRectArray[i5].right + paddingTop, this.mRectArray[i5].bottom + paddingLeft);
                }
            }
        }

        @Override
        public void dispatchDraw(Canvas canvas) {
            int paddingLeft = getPaddingLeft();
            int paddingTop = getPaddingTop();
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = getChildAt(i);
                if (childAt.getVisibility() != 8) {
                    if (i < this.mRectArray.length) {
                        int i2 = this.mRectArray[i].left + paddingLeft;
                        int i3 = this.mRectArray[i].top + paddingTop;
                        int iSave = canvas.save();
                        canvas.translate(i2, i3);
                        childAt.draw(canvas);
                        canvas.restoreToCount(iSave);
                    } else {
                        return;
                    }
                }
            }
        }
    }

    static class CCLayout extends ScaledLayout implements ClosedCaptionWidget.ClosedCaptionLayout {
        private static final float SAFE_TITLE_AREA_SCALE_END_X = 0.9f;
        private static final float SAFE_TITLE_AREA_SCALE_END_Y = 0.9f;
        private static final float SAFE_TITLE_AREA_SCALE_START_X = 0.1f;
        private static final float SAFE_TITLE_AREA_SCALE_START_Y = 0.1f;
        private final ScaledLayout mSafeTitleAreaLayout;

        public CCLayout(Context context) {
            super(context);
            this.mSafeTitleAreaLayout = new ScaledLayout(context);
            addView(this.mSafeTitleAreaLayout, new ScaledLayout.ScaledLayoutParams(0.1f, 0.9f, 0.1f, 0.9f));
        }

        public void addOrUpdateViewToSafeTitleArea(CCWindowLayout cCWindowLayout, ScaledLayout.ScaledLayoutParams scaledLayoutParams) {
            if (this.mSafeTitleAreaLayout.indexOfChild(cCWindowLayout) < 0) {
                this.mSafeTitleAreaLayout.addView(cCWindowLayout, scaledLayoutParams);
            } else {
                this.mSafeTitleAreaLayout.updateViewLayout(cCWindowLayout, scaledLayoutParams);
            }
        }

        public void removeViewFromSafeTitleArea(CCWindowLayout cCWindowLayout) {
            this.mSafeTitleAreaLayout.removeView(cCWindowLayout);
        }

        @Override
        public void setCaptionStyle(CaptioningManager.CaptionStyle captionStyle) {
            int childCount = this.mSafeTitleAreaLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                ((CCWindowLayout) this.mSafeTitleAreaLayout.getChildAt(i)).setCaptionStyle(captionStyle);
            }
        }

        @Override
        public void setFontScale(float f) {
            int childCount = this.mSafeTitleAreaLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                ((CCWindowLayout) this.mSafeTitleAreaLayout.getChildAt(i)).setFontScale(f);
            }
        }
    }

    static class CCHandler implements Handler.Callback {
        private static final int CAPTION_ALL_WINDOWS_BITMAP = 255;
        private static final long CAPTION_CLEAR_INTERVAL_MS = 60000;
        private static final int CAPTION_WINDOWS_MAX = 8;
        private static final boolean DEBUG = false;
        private static final int MSG_CAPTION_CLEAR = 2;
        private static final int MSG_DELAY_CANCEL = 1;
        private static final String TAG = "CCHandler";
        private static final int TENTHS_OF_SECOND_IN_MILLIS = 100;
        private final CCLayout mCCLayout;
        private CCWindowLayout mCurrentWindowLayout;
        private boolean mIsDelayed = false;
        private final CCWindowLayout[] mCaptionWindowLayouts = new CCWindowLayout[8];
        private final ArrayList<Cea708CCParser.CaptionEvent> mPendingCaptionEvents = new ArrayList<>();
        private final Handler mHandler = new Handler(this);

        public CCHandler(CCLayout cCLayout) {
            this.mCCLayout = cCLayout;
        }

        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    delayCancel();
                    break;
                case 2:
                    clearWindows(255);
                    break;
            }
            return true;
        }

        public void processCaptionEvent(Cea708CCParser.CaptionEvent captionEvent) {
            if (this.mIsDelayed) {
                this.mPendingCaptionEvents.add(captionEvent);
            }
            switch (captionEvent.type) {
                case 1:
                    sendBufferToCurrentWindow((String) captionEvent.obj);
                    break;
                case 2:
                    sendControlToCurrentWindow(((Character) captionEvent.obj).charValue());
                    break;
                case 3:
                    setCurrentWindowLayout(((Integer) captionEvent.obj).intValue());
                    break;
                case 4:
                    clearWindows(((Integer) captionEvent.obj).intValue());
                    break;
                case 5:
                    displayWindows(((Integer) captionEvent.obj).intValue());
                    break;
                case 6:
                    hideWindows(((Integer) captionEvent.obj).intValue());
                    break;
                case 7:
                    toggleWindows(((Integer) captionEvent.obj).intValue());
                    break;
                case 8:
                    deleteWindows(((Integer) captionEvent.obj).intValue());
                    break;
                case 9:
                    delay(((Integer) captionEvent.obj).intValue());
                    break;
                case 10:
                    delayCancel();
                    break;
                case 11:
                    reset();
                    break;
                case 12:
                    setPenAttr((Cea708CCParser.CaptionPenAttr) captionEvent.obj);
                    break;
                case 13:
                    setPenColor((Cea708CCParser.CaptionPenColor) captionEvent.obj);
                    break;
                case 14:
                    setPenLocation((Cea708CCParser.CaptionPenLocation) captionEvent.obj);
                    break;
                case 15:
                    setWindowAttr((Cea708CCParser.CaptionWindowAttr) captionEvent.obj);
                    break;
                case 16:
                    defineWindow((Cea708CCParser.CaptionWindow) captionEvent.obj);
                    break;
            }
        }

        private void setCurrentWindowLayout(int i) {
            CCWindowLayout cCWindowLayout;
            if (i < 0 || i >= this.mCaptionWindowLayouts.length || (cCWindowLayout = this.mCaptionWindowLayouts[i]) == null) {
                return;
            }
            this.mCurrentWindowLayout = cCWindowLayout;
        }

        private ArrayList<CCWindowLayout> getWindowsFromBitmap(int i) {
            CCWindowLayout cCWindowLayout;
            ArrayList<CCWindowLayout> arrayList = new ArrayList<>();
            for (int i2 = 0; i2 < 8; i2++) {
                if (((1 << i2) & i) != 0 && (cCWindowLayout = this.mCaptionWindowLayouts[i2]) != null) {
                    arrayList.add(cCWindowLayout);
                }
            }
            return arrayList;
        }

        private void clearWindows(int i) {
            if (i == 0) {
                return;
            }
            Iterator<CCWindowLayout> it = getWindowsFromBitmap(i).iterator();
            while (it.hasNext()) {
                it.next().clear();
            }
        }

        private void displayWindows(int i) {
            if (i == 0) {
                return;
            }
            Iterator<CCWindowLayout> it = getWindowsFromBitmap(i).iterator();
            while (it.hasNext()) {
                it.next().show();
            }
        }

        private void hideWindows(int i) {
            if (i == 0) {
                return;
            }
            Iterator<CCWindowLayout> it = getWindowsFromBitmap(i).iterator();
            while (it.hasNext()) {
                it.next().hide();
            }
        }

        private void toggleWindows(int i) {
            if (i == 0) {
                return;
            }
            for (CCWindowLayout cCWindowLayout : getWindowsFromBitmap(i)) {
                if (cCWindowLayout.isShown()) {
                    cCWindowLayout.hide();
                } else {
                    cCWindowLayout.show();
                }
            }
        }

        private void deleteWindows(int i) {
            if (i == 0) {
                return;
            }
            for (CCWindowLayout cCWindowLayout : getWindowsFromBitmap(i)) {
                cCWindowLayout.removeFromCaptionView();
                this.mCaptionWindowLayouts[cCWindowLayout.getCaptionWindowId()] = null;
            }
        }

        public void reset() {
            this.mCurrentWindowLayout = null;
            this.mIsDelayed = false;
            this.mPendingCaptionEvents.clear();
            for (int i = 0; i < 8; i++) {
                if (this.mCaptionWindowLayouts[i] != null) {
                    this.mCaptionWindowLayouts[i].removeFromCaptionView();
                }
                this.mCaptionWindowLayouts[i] = null;
            }
            this.mCCLayout.setVisibility(4);
            this.mHandler.removeMessages(2);
        }

        private void setWindowAttr(Cea708CCParser.CaptionWindowAttr captionWindowAttr) {
            if (this.mCurrentWindowLayout != null) {
                this.mCurrentWindowLayout.setWindowAttr(captionWindowAttr);
            }
        }

        private void defineWindow(Cea708CCParser.CaptionWindow captionWindow) {
            int i;
            if (captionWindow == null || (i = captionWindow.id) < 0 || i >= this.mCaptionWindowLayouts.length) {
                return;
            }
            CCWindowLayout cCWindowLayout = this.mCaptionWindowLayouts[i];
            if (cCWindowLayout == null) {
                cCWindowLayout = new CCWindowLayout(this.mCCLayout.getContext());
            }
            cCWindowLayout.initWindow(this.mCCLayout, captionWindow);
            this.mCaptionWindowLayouts[i] = cCWindowLayout;
            this.mCurrentWindowLayout = cCWindowLayout;
        }

        private void delay(int i) {
            if (i < 0 || i > 255) {
                return;
            }
            this.mIsDelayed = true;
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), i * 100);
        }

        private void delayCancel() {
            this.mIsDelayed = false;
            processPendingBuffer();
        }

        private void processPendingBuffer() {
            Iterator<Cea708CCParser.CaptionEvent> it = this.mPendingCaptionEvents.iterator();
            while (it.hasNext()) {
                processCaptionEvent(it.next());
            }
            this.mPendingCaptionEvents.clear();
        }

        private void sendControlToCurrentWindow(char c) {
            if (this.mCurrentWindowLayout != null) {
                this.mCurrentWindowLayout.sendControl(c);
            }
        }

        private void sendBufferToCurrentWindow(String str) {
            if (this.mCurrentWindowLayout != null) {
                this.mCurrentWindowLayout.sendBuffer(str);
                this.mHandler.removeMessages(2);
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 60000L);
            }
        }

        private void setPenAttr(Cea708CCParser.CaptionPenAttr captionPenAttr) {
            if (this.mCurrentWindowLayout != null) {
                this.mCurrentWindowLayout.setPenAttr(captionPenAttr);
            }
        }

        private void setPenColor(Cea708CCParser.CaptionPenColor captionPenColor) {
            if (this.mCurrentWindowLayout != null) {
                this.mCurrentWindowLayout.setPenColor(captionPenColor);
            }
        }

        private void setPenLocation(Cea708CCParser.CaptionPenLocation captionPenLocation) {
            if (this.mCurrentWindowLayout != null) {
                this.mCurrentWindowLayout.setPenLocation(captionPenLocation.row, captionPenLocation.column);
            }
        }
    }

    static class CCWindowLayout extends RelativeLayout implements View.OnLayoutChangeListener {
        private static final int ANCHOR_HORIZONTAL_16_9_MAX = 209;
        private static final int ANCHOR_HORIZONTAL_MODE_CENTER = 1;
        private static final int ANCHOR_HORIZONTAL_MODE_LEFT = 0;
        private static final int ANCHOR_HORIZONTAL_MODE_RIGHT = 2;
        private static final int ANCHOR_MODE_DIVIDER = 3;
        private static final int ANCHOR_RELATIVE_POSITIONING_MAX = 99;
        private static final int ANCHOR_VERTICAL_MAX = 74;
        private static final int ANCHOR_VERTICAL_MODE_BOTTOM = 2;
        private static final int ANCHOR_VERTICAL_MODE_CENTER = 1;
        private static final int ANCHOR_VERTICAL_MODE_TOP = 0;
        private static final int MAX_COLUMN_COUNT_16_9 = 42;
        private static final float PROPORTION_PEN_SIZE_LARGE = 1.25f;
        private static final float PROPORTION_PEN_SIZE_SMALL = 0.75f;
        private static final String TAG = "CCWindowLayout";
        private final SpannableStringBuilder mBuilder;
        private CCLayout mCCLayout;
        private CCView mCCView;
        private CaptioningManager.CaptionStyle mCaptionStyle;
        private int mCaptionWindowId;
        private final List<CharacterStyle> mCharacterStyles;
        private float mFontScale;
        private int mLastCaptionLayoutHeight;
        private int mLastCaptionLayoutWidth;
        private int mRow;
        private int mRowLimit;
        private float mTextSize;
        private String mWidestChar;

        public CCWindowLayout(Context context) {
            this(context, null);
        }

        public CCWindowLayout(Context context, AttributeSet attributeSet) {
            this(context, attributeSet, 0);
        }

        public CCWindowLayout(Context context, AttributeSet attributeSet, int i) {
            this(context, attributeSet, i, 0);
        }

        public CCWindowLayout(Context context, AttributeSet attributeSet, int i, int i2) {
            super(context, attributeSet, i, i2);
            this.mRowLimit = 0;
            this.mBuilder = new SpannableStringBuilder();
            this.mCharacterStyles = new ArrayList();
            this.mRow = -1;
            this.mCCView = new CCView(context);
            addView(this.mCCView, new RelativeLayout.LayoutParams(-2, -2));
            CaptioningManager captioningManager = (CaptioningManager) context.getSystemService(Context.CAPTIONING_SERVICE);
            this.mFontScale = captioningManager.getFontScale();
            setCaptionStyle(captioningManager.getUserStyle());
            this.mCCView.setText("");
            updateWidestChar();
        }

        public void setCaptionStyle(CaptioningManager.CaptionStyle captionStyle) {
            this.mCaptionStyle = captionStyle;
            this.mCCView.setCaptionStyle(captionStyle);
        }

        public void setFontScale(float f) {
            this.mFontScale = f;
            updateTextSize();
        }

        public int getCaptionWindowId() {
            return this.mCaptionWindowId;
        }

        public void setCaptionWindowId(int i) {
            this.mCaptionWindowId = i;
        }

        public void clear() {
            clearText();
            hide();
        }

        public void show() {
            setVisibility(0);
            requestLayout();
        }

        public void hide() {
            setVisibility(4);
            requestLayout();
        }

        public void setPenAttr(Cea708CCParser.CaptionPenAttr captionPenAttr) {
            this.mCharacterStyles.clear();
            if (captionPenAttr.italic) {
                this.mCharacterStyles.add(new StyleSpan(2));
            }
            if (captionPenAttr.underline) {
                this.mCharacterStyles.add(new UnderlineSpan());
            }
            int i = captionPenAttr.penSize;
            if (i == 0) {
                this.mCharacterStyles.add(new RelativeSizeSpan(PROPORTION_PEN_SIZE_SMALL));
            } else if (i == 2) {
                this.mCharacterStyles.add(new RelativeSizeSpan(PROPORTION_PEN_SIZE_LARGE));
            }
            int i2 = captionPenAttr.penOffset;
            if (i2 == 0) {
                this.mCharacterStyles.add(new SubscriptSpan());
            } else if (i2 == 2) {
                this.mCharacterStyles.add(new SuperscriptSpan());
            }
        }

        public void setPenColor(Cea708CCParser.CaptionPenColor captionPenColor) {
        }

        public void setPenLocation(int i, int i2) {
            if (this.mRow >= 0) {
                for (int i3 = this.mRow; i3 < i; i3++) {
                    appendText("\n");
                }
            }
            this.mRow = i;
        }

        public void setWindowAttr(Cea708CCParser.CaptionWindowAttr captionWindowAttr) {
        }

        public void sendBuffer(String str) {
            appendText(str);
        }

        public void sendControl(char c) {
        }

        public void initWindow(CCLayout cCLayout, Cea708CCParser.CaptionWindow captionWindow) {
            float f;
            if (this.mCCLayout != cCLayout) {
                if (this.mCCLayout != null) {
                    this.mCCLayout.removeOnLayoutChangeListener(this);
                }
                this.mCCLayout = cCLayout;
                this.mCCLayout.addOnLayoutChangeListener(this);
                updateWidestChar();
            }
            float fMax = captionWindow.anchorVertical / (captionWindow.relativePositioning ? 99 : 74);
            float fMax2 = captionWindow.anchorHorizontal / (captionWindow.relativePositioning ? 99 : 209);
            float f2 = 0.0f;
            if (fMax < 0.0f || fMax > 1.0f) {
                Log.i(TAG, "The vertical position of the anchor point should be at the range of 0 and 1 but " + fMax);
                fMax = Math.max(0.0f, Math.min(fMax, 1.0f));
            }
            if (fMax2 < 0.0f || fMax2 > 1.0f) {
                Log.i(TAG, "The horizontal position of the anchor point should be at the range of 0 and 1 but " + fMax2);
                fMax2 = Math.max(0.0f, Math.min(fMax2, 1.0f));
            }
            int i = 17;
            int i2 = captionWindow.anchorId % 3;
            int i3 = captionWindow.anchorId / 3;
            switch (i2) {
                case 0:
                    this.mCCView.setAlignment(Layout.Alignment.ALIGN_NORMAL);
                    f = 1.0f;
                    i = 3;
                    break;
                case 1:
                    float fMin = Math.min(1.0f - fMax2, fMax2);
                    int iMin = Math.min(getScreenColumnCount(), captionWindow.columnCount + 1);
                    StringBuilder sb = new StringBuilder();
                    for (int i4 = 0; i4 < iMin; i4++) {
                        sb.append(this.mWidestChar);
                    }
                    Paint paint = new Paint();
                    paint.setTypeface(this.mCaptionStyle.getTypeface());
                    paint.setTextSize(this.mTextSize);
                    float fMeasureText = this.mCCLayout.getWidth() > 0 ? (paint.measureText(sb.toString()) / 2.0f) / (this.mCCLayout.getWidth() * 0.8f) : 0.0f;
                    if (fMeasureText <= 0.0f || fMeasureText >= fMax2) {
                        this.mCCView.setAlignment(Layout.Alignment.ALIGN_CENTER);
                        float f3 = fMax2 - fMin;
                        float f4 = fMax2 + fMin;
                        i = 1;
                        f = f4;
                        fMax2 = f3;
                    } else {
                        this.mCCView.setAlignment(Layout.Alignment.ALIGN_NORMAL);
                        fMax2 -= fMeasureText;
                        f = 1.0f;
                        i = 3;
                    }
                    break;
                case 2:
                    i = 5;
                    this.mCCView.setAlignment(Layout.Alignment.ALIGN_RIGHT);
                    f = fMax2;
                    fMax2 = 0.0f;
                    break;
                default:
                    fMax2 = 0.0f;
                    f = 1.0f;
                    break;
            }
            switch (i3) {
                case 0:
                    i |= 48;
                    f2 = fMax;
                    fMax = 1.0f;
                    break;
                case 1:
                    i |= 16;
                    float fMin2 = Math.min(1.0f - fMax, fMax);
                    float f5 = fMax - fMin2;
                    fMax += fMin2;
                    f2 = f5;
                    break;
                case 2:
                    i |= 80;
                    break;
                default:
                    fMax = 1.0f;
                    break;
            }
            this.mCCLayout.addOrUpdateViewToSafeTitleArea(this, new ScaledLayout.ScaledLayoutParams(f2, fMax, fMax2, f));
            setCaptionWindowId(captionWindow.id);
            setRowLimit(captionWindow.rowCount);
            setGravity(i);
            if (captionWindow.visible) {
                show();
            } else {
                hide();
            }
        }

        @Override
        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            int i9 = i3 - i;
            int i10 = i4 - i2;
            if (i9 != this.mLastCaptionLayoutWidth || i10 != this.mLastCaptionLayoutHeight) {
                this.mLastCaptionLayoutWidth = i9;
                this.mLastCaptionLayoutHeight = i10;
                updateTextSize();
            }
        }

        private void updateWidestChar() {
            Paint paint = new Paint();
            paint.setTypeface(this.mCaptionStyle.getTypeface());
            Charset charsetForName = Charset.forName("ISO-8859-1");
            float f = 0.0f;
            for (int i = 0; i < 256; i++) {
                String str = new String(new byte[]{(byte) i}, charsetForName);
                float fMeasureText = paint.measureText(str);
                if (f < fMeasureText) {
                    this.mWidestChar = str;
                    f = fMeasureText;
                }
            }
            updateTextSize();
        }

        private void updateTextSize() {
            if (this.mCCLayout == null) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            int screenColumnCount = getScreenColumnCount();
            for (int i = 0; i < screenColumnCount; i++) {
                sb.append(this.mWidestChar);
            }
            String string = sb.toString();
            Paint paint = new Paint();
            paint.setTypeface(this.mCaptionStyle.getTypeface());
            float f = 0.0f;
            float f2 = 255.0f;
            while (f < f2) {
                float f3 = (f + f2) / 2.0f;
                paint.setTextSize(f3);
                if (this.mCCLayout.getWidth() * 0.8f > paint.measureText(string)) {
                    f = f3 + 0.01f;
                } else {
                    f2 = f3 - 0.01f;
                }
            }
            this.mTextSize = f2 * this.mFontScale;
            this.mCCView.setTextSize(this.mTextSize);
        }

        private int getScreenColumnCount() {
            return 42;
        }

        public void removeFromCaptionView() {
            if (this.mCCLayout != null) {
                this.mCCLayout.removeViewFromSafeTitleArea(this);
                this.mCCLayout.removeOnLayoutChangeListener(this);
                this.mCCLayout = null;
            }
        }

        public void setText(String str) {
            updateText(str, false);
        }

        public void appendText(String str) {
            updateText(str, true);
        }

        public void clearText() {
            this.mBuilder.clear();
            this.mCCView.setText("");
        }

        private void updateText(String str, boolean z) {
            if (!z) {
                this.mBuilder.clear();
            }
            if (str != null && str.length() > 0) {
                int length = this.mBuilder.length();
                this.mBuilder.append((CharSequence) str);
                Iterator<CharacterStyle> it = this.mCharacterStyles.iterator();
                while (it.hasNext()) {
                    this.mBuilder.setSpan(it.next(), length, this.mBuilder.length(), 33);
                }
            }
            String[] strArrSplit = TextUtils.split(this.mBuilder.toString(), "\n");
            this.mBuilder.delete(0, this.mBuilder.length() - TextUtils.join("\n", Arrays.copyOfRange(strArrSplit, Math.max(0, strArrSplit.length - (this.mRowLimit + 1)), strArrSplit.length)).length());
            int length2 = this.mBuilder.length() - 1;
            int i = 0;
            while (i <= length2 && this.mBuilder.charAt(i) <= ' ') {
                i++;
            }
            int i2 = length2;
            while (i2 >= i && this.mBuilder.charAt(i2) <= ' ') {
                i2--;
            }
            if (i == 0 && i2 == length2) {
                this.mCCView.setText(this.mBuilder);
                return;
            }
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            spannableStringBuilder.append((CharSequence) this.mBuilder);
            if (i2 < length2) {
                spannableStringBuilder.delete(i2 + 1, length2 + 1);
            }
            if (i > 0) {
                spannableStringBuilder.delete(0, i);
            }
            this.mCCView.setText(spannableStringBuilder);
        }

        public void setRowLimit(int i) {
            if (i < 0) {
                throw new IllegalArgumentException("A rowLimit should have a positive number");
            }
            this.mRowLimit = i;
        }
    }

    static class CCView extends SubtitleView {
        private static final CaptioningManager.CaptionStyle DEFAULT_CAPTION_STYLE = CaptioningManager.CaptionStyle.DEFAULT;

        public CCView(Context context) {
            this(context, null);
        }

        public CCView(Context context, AttributeSet attributeSet) {
            this(context, attributeSet, 0);
        }

        public CCView(Context context, AttributeSet attributeSet, int i) {
            this(context, attributeSet, i, 0);
        }

        public CCView(Context context, AttributeSet attributeSet, int i, int i2) {
            super(context, attributeSet, i, i2);
        }

        public void setCaptionStyle(CaptioningManager.CaptionStyle captionStyle) {
            setForegroundColor(captionStyle.hasForegroundColor() ? captionStyle.foregroundColor : DEFAULT_CAPTION_STYLE.foregroundColor);
            setBackgroundColor(captionStyle.hasBackgroundColor() ? captionStyle.backgroundColor : DEFAULT_CAPTION_STYLE.backgroundColor);
            setEdgeType(captionStyle.hasEdgeType() ? captionStyle.edgeType : DEFAULT_CAPTION_STYLE.edgeType);
            setEdgeColor(captionStyle.hasEdgeColor() ? captionStyle.edgeColor : DEFAULT_CAPTION_STYLE.edgeColor);
            setTypeface(captionStyle.getTypeface());
        }
    }
}

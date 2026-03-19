package android.inputmethodservice;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.android.internal.R;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyboardView extends View implements View.OnClickListener {
    private static final int DEBOUNCE_TIME = 70;
    private static final boolean DEBUG = false;
    private static final int DELAY_AFTER_PREVIEW = 70;
    private static final int DELAY_BEFORE_PREVIEW = 0;
    private static final int MSG_LONGPRESS = 4;
    private static final int MSG_REMOVE_PREVIEW = 2;
    private static final int MSG_REPEAT = 3;
    private static final int MSG_SHOW_PREVIEW = 1;
    private static final int MULTITAP_INTERVAL = 800;
    private static final int NOT_A_KEY = -1;
    private static final int REPEAT_INTERVAL = 50;
    private static final int REPEAT_START_DELAY = 400;
    private boolean mAbortKey;
    private AccessibilityManager mAccessibilityManager;
    private AudioManager mAudioManager;
    private float mBackgroundDimAmount;
    private Bitmap mBuffer;
    private Canvas mCanvas;
    private Rect mClipRegion;
    private final int[] mCoordinates;
    private int mCurrentKey;
    private int mCurrentKeyIndex;
    private long mCurrentKeyTime;
    private Rect mDirtyRect;
    private boolean mDisambiguateSwipe;
    private int[] mDistances;
    private int mDownKey;
    private long mDownTime;
    private boolean mDrawPending;
    private GestureDetector mGestureDetector;
    Handler mHandler;
    private boolean mHeadsetRequiredToHearPasswordsAnnounced;
    private boolean mInMultiTap;
    private Keyboard.Key mInvalidatedKey;
    private Drawable mKeyBackground;
    private int[] mKeyIndices;
    private int mKeyTextColor;
    private int mKeyTextSize;
    private Keyboard mKeyboard;
    private OnKeyboardActionListener mKeyboardActionListener;
    private boolean mKeyboardChanged;
    private Keyboard.Key[] mKeys;
    private int mLabelTextSize;
    private int mLastCodeX;
    private int mLastCodeY;
    private int mLastKey;
    private long mLastKeyTime;
    private long mLastMoveTime;
    private int mLastSentIndex;
    private long mLastTapTime;
    private int mLastX;
    private int mLastY;
    private KeyboardView mMiniKeyboard;
    private Map<Keyboard.Key, View> mMiniKeyboardCache;
    private View mMiniKeyboardContainer;
    private int mMiniKeyboardOffsetX;
    private int mMiniKeyboardOffsetY;
    private boolean mMiniKeyboardOnScreen;
    private int mOldPointerCount;
    private float mOldPointerX;
    private float mOldPointerY;
    private Rect mPadding;
    private Paint mPaint;
    private PopupWindow mPopupKeyboard;
    private int mPopupLayout;
    private View mPopupParent;
    private int mPopupPreviewX;
    private int mPopupPreviewY;
    private int mPopupX;
    private int mPopupY;
    private boolean mPossiblePoly;
    private boolean mPreviewCentered;
    private int mPreviewHeight;
    private StringBuilder mPreviewLabel;
    private int mPreviewOffset;
    private PopupWindow mPreviewPopup;
    private TextView mPreviewText;
    private int mPreviewTextSizeLarge;
    private boolean mProximityCorrectOn;
    private int mProximityThreshold;
    private int mRepeatKeyIndex;
    private int mShadowColor;
    private float mShadowRadius;
    private boolean mShowPreview;
    private boolean mShowTouchPoints;
    private int mStartX;
    private int mStartY;
    private int mSwipeThreshold;
    private SwipeTracker mSwipeTracker;
    private int mTapCount;
    private int mVerticalCorrection;
    private static final int[] KEY_DELETE = {-5};
    private static final int[] LONG_PRESSABLE_STATE_SET = {16843324};
    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static int MAX_NEARBY_KEYS = 12;

    public interface OnKeyboardActionListener {
        void onKey(int i, int[] iArr);

        void onPress(int i);

        void onRelease(int i);

        void onText(CharSequence charSequence);

        void swipeDown();

        void swipeLeft();

        void swipeRight();

        void swipeUp();
    }

    public KeyboardView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.keyboardViewStyle);
    }

    public KeyboardView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public KeyboardView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mCurrentKeyIndex = -1;
        this.mCoordinates = new int[2];
        this.mPreviewCentered = false;
        this.mShowPreview = true;
        this.mShowTouchPoints = true;
        this.mCurrentKey = -1;
        this.mDownKey = -1;
        this.mKeyIndices = new int[12];
        this.mRepeatKeyIndex = -1;
        this.mClipRegion = new Rect(0, 0, 0, 0);
        this.mSwipeTracker = new SwipeTracker();
        this.mOldPointerCount = 1;
        this.mDistances = new int[MAX_NEARBY_KEYS];
        this.mPreviewLabel = new StringBuilder(1);
        this.mDirtyRect = new Rect();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, android.R.styleable.KeyboardView, i, i2);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
        int resourceId = 0;
        for (int i3 = 0; i3 < indexCount; i3++) {
            int index = typedArrayObtainStyledAttributes.getIndex(i3);
            switch (index) {
                case 0:
                    this.mShadowColor = typedArrayObtainStyledAttributes.getColor(index, 0);
                    break;
                case 1:
                    this.mShadowRadius = typedArrayObtainStyledAttributes.getFloat(index, 0.0f);
                    break;
                case 2:
                    this.mKeyBackground = typedArrayObtainStyledAttributes.getDrawable(index);
                    break;
                case 3:
                    this.mKeyTextSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 18);
                    break;
                case 4:
                    this.mLabelTextSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 14);
                    break;
                case 5:
                    this.mKeyTextColor = typedArrayObtainStyledAttributes.getColor(index, -16777216);
                    break;
                case 6:
                    resourceId = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                    break;
                case 7:
                    this.mPreviewOffset = typedArrayObtainStyledAttributes.getDimensionPixelOffset(index, 0);
                    break;
                case 8:
                    this.mPreviewHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, 80);
                    break;
                case 9:
                    this.mVerticalCorrection = typedArrayObtainStyledAttributes.getDimensionPixelOffset(index, 0);
                    break;
                case 10:
                    this.mPopupLayout = typedArrayObtainStyledAttributes.getResourceId(index, 0);
                    break;
            }
        }
        this.mBackgroundDimAmount = this.mContext.obtainStyledAttributes(R.styleable.Theme).getFloat(2, 0.5f);
        this.mPreviewPopup = new PopupWindow(context);
        if (resourceId != 0) {
            this.mPreviewText = (TextView) layoutInflater.inflate(resourceId, (ViewGroup) null);
            this.mPreviewTextSizeLarge = (int) this.mPreviewText.getTextSize();
            this.mPreviewPopup.setContentView(this.mPreviewText);
            this.mPreviewPopup.setBackgroundDrawable(null);
        } else {
            this.mShowPreview = false;
        }
        this.mPreviewPopup.setTouchable(false);
        this.mPopupKeyboard = new PopupWindow(context);
        this.mPopupKeyboard.setBackgroundDrawable(null);
        this.mPopupParent = this;
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setTextSize(0);
        this.mPaint.setTextAlign(Paint.Align.CENTER);
        this.mPaint.setAlpha(255);
        this.mPadding = new Rect(0, 0, 0, 0);
        this.mMiniKeyboardCache = new HashMap();
        this.mKeyBackground.getPadding(this.mPadding);
        this.mSwipeThreshold = (int) (500.0f * getResources().getDisplayMetrics().density);
        this.mDisambiguateSwipe = getResources().getBoolean(R.bool.config_swipeDisambiguation);
        this.mAccessibilityManager = AccessibilityManager.getInstance(context);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        resetMultiTap();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initGestureDetector();
        if (this.mHandler == null) {
            this.mHandler = new Handler() {
                @Override
                public void handleMessage(Message message) {
                    switch (message.what) {
                        case 1:
                            KeyboardView.this.showKey(message.arg1);
                            break;
                        case 2:
                            KeyboardView.this.mPreviewText.setVisibility(4);
                            break;
                        case 3:
                            if (KeyboardView.this.repeatKey()) {
                                sendMessageDelayed(Message.obtain(this, 3), 50L);
                            }
                            break;
                        case 4:
                            KeyboardView.this.openPopupIfRequired((MotionEvent) message.obj);
                            break;
                    }
                }
            };
        }
    }

    private void initGestureDetector() {
        if (this.mGestureDetector == null) {
            this.mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
                    if (KeyboardView.this.mPossiblePoly) {
                        return false;
                    }
                    float fAbs = Math.abs(f);
                    float fAbs2 = Math.abs(f2);
                    float x = motionEvent2.getX() - motionEvent.getX();
                    float y = motionEvent2.getY() - motionEvent.getY();
                    int width = KeyboardView.this.getWidth() / 2;
                    int height = KeyboardView.this.getHeight() / 2;
                    KeyboardView.this.mSwipeTracker.computeCurrentVelocity(1000);
                    float xVelocity = KeyboardView.this.mSwipeTracker.getXVelocity();
                    float yVelocity = KeyboardView.this.mSwipeTracker.getYVelocity();
                    boolean z = true;
                    if (f <= KeyboardView.this.mSwipeThreshold || fAbs2 >= fAbs || x <= width) {
                        if (f >= (-KeyboardView.this.mSwipeThreshold) || fAbs2 >= fAbs || x >= (-width)) {
                            if (f2 >= (-KeyboardView.this.mSwipeThreshold) || fAbs >= fAbs2 || y >= (-height)) {
                                if (f2 > KeyboardView.this.mSwipeThreshold && fAbs < fAbs2 / 2.0f && y > height) {
                                    if (!KeyboardView.this.mDisambiguateSwipe || yVelocity >= f2 / 4.0f) {
                                        KeyboardView.this.swipeDown();
                                        return true;
                                    }
                                } else {
                                    z = false;
                                }
                            } else if (!KeyboardView.this.mDisambiguateSwipe || yVelocity <= f2 / 4.0f) {
                                KeyboardView.this.swipeUp();
                                return true;
                            }
                        } else if (!KeyboardView.this.mDisambiguateSwipe || xVelocity <= f / 4.0f) {
                            KeyboardView.this.swipeLeft();
                            return true;
                        }
                    } else if (!KeyboardView.this.mDisambiguateSwipe || xVelocity >= f / 4.0f) {
                        KeyboardView.this.swipeRight();
                        return true;
                    }
                    if (z) {
                        KeyboardView.this.detectAndSendKey(KeyboardView.this.mDownKey, KeyboardView.this.mStartX, KeyboardView.this.mStartY, motionEvent.getEventTime());
                    }
                    return false;
                }
            });
            this.mGestureDetector.setIsLongpressEnabled(false);
        }
    }

    public void setOnKeyboardActionListener(OnKeyboardActionListener onKeyboardActionListener) {
        this.mKeyboardActionListener = onKeyboardActionListener;
    }

    protected OnKeyboardActionListener getOnKeyboardActionListener() {
        return this.mKeyboardActionListener;
    }

    public void setKeyboard(Keyboard keyboard) {
        if (this.mKeyboard != null) {
            showPreview(-1);
        }
        removeMessages();
        this.mKeyboard = keyboard;
        List<Keyboard.Key> keys = this.mKeyboard.getKeys();
        this.mKeys = (Keyboard.Key[]) keys.toArray(new Keyboard.Key[keys.size()]);
        requestLayout();
        this.mKeyboardChanged = true;
        invalidateAllKeys();
        computeProximityThreshold(keyboard);
        this.mMiniKeyboardCache.clear();
        this.mAbortKey = true;
    }

    public Keyboard getKeyboard() {
        return this.mKeyboard;
    }

    public boolean setShifted(boolean z) {
        if (this.mKeyboard != null && this.mKeyboard.setShifted(z)) {
            invalidateAllKeys();
            return true;
        }
        return false;
    }

    public boolean isShifted() {
        if (this.mKeyboard != null) {
            return this.mKeyboard.isShifted();
        }
        return false;
    }

    public void setPreviewEnabled(boolean z) {
        this.mShowPreview = z;
    }

    public boolean isPreviewEnabled() {
        return this.mShowPreview;
    }

    public void setVerticalCorrection(int i) {
    }

    public void setPopupParent(View view) {
        this.mPopupParent = view;
    }

    public void setPopupOffset(int i, int i2) {
        this.mMiniKeyboardOffsetX = i;
        this.mMiniKeyboardOffsetY = i2;
        if (this.mPreviewPopup.isShowing()) {
            this.mPreviewPopup.dismiss();
        }
    }

    public void setProximityCorrectionEnabled(boolean z) {
        this.mProximityCorrectOn = z;
    }

    public boolean isProximityCorrectionEnabled() {
        return this.mProximityCorrectOn;
    }

    @Override
    public void onClick(View view) {
        dismissPopupKeyboard();
    }

    private CharSequence adjustCase(CharSequence charSequence) {
        if (this.mKeyboard.isShifted() && charSequence != null && charSequence.length() < 3 && Character.isLowerCase(charSequence.charAt(0))) {
            return charSequence.toString().toUpperCase();
        }
        return charSequence;
    }

    @Override
    public void onMeasure(int i, int i2) {
        if (this.mKeyboard == null) {
            setMeasuredDimension(this.mPaddingLeft + this.mPaddingRight, this.mPaddingTop + this.mPaddingBottom);
            return;
        }
        int minWidth = this.mKeyboard.getMinWidth() + this.mPaddingLeft + this.mPaddingRight;
        if (View.MeasureSpec.getSize(i) < minWidth + 10) {
            minWidth = View.MeasureSpec.getSize(i);
        }
        setMeasuredDimension(minWidth, this.mKeyboard.getHeight() + this.mPaddingTop + this.mPaddingBottom);
    }

    private void computeProximityThreshold(Keyboard keyboard) {
        Keyboard.Key[] keyArr;
        if (keyboard == null || (keyArr = this.mKeys) == null) {
            return;
        }
        int length = keyArr.length;
        int iMin = 0;
        for (Keyboard.Key key : keyArr) {
            iMin += Math.min(key.width, key.height) + key.gap;
        }
        if (iMin < 0 || length == 0) {
            return;
        }
        this.mProximityThreshold = (int) ((iMin * 1.4f) / length);
        this.mProximityThreshold *= this.mProximityThreshold;
    }

    @Override
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (this.mKeyboard != null) {
            this.mKeyboard.resize(i, i2);
        }
        this.mBuffer = null;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mDrawPending || this.mBuffer == null || this.mKeyboardChanged) {
            onBufferDraw();
        }
        canvas.drawBitmap(this.mBuffer, 0.0f, 0.0f, (Paint) null);
    }

    private void onBufferDraw() {
        String string;
        boolean z;
        Drawable drawable;
        Rect rect;
        if (this.mBuffer == null || this.mKeyboardChanged) {
            if (this.mBuffer == null || (this.mKeyboardChanged && (this.mBuffer.getWidth() != getWidth() || this.mBuffer.getHeight() != getHeight()))) {
                this.mBuffer = Bitmap.createBitmap(Math.max(1, getWidth()), Math.max(1, getHeight()), Bitmap.Config.ARGB_8888);
                this.mCanvas = new Canvas(this.mBuffer);
            }
            invalidateAllKeys();
            this.mKeyboardChanged = false;
        }
        if (this.mKeyboard == null) {
            return;
        }
        this.mCanvas.save();
        Canvas canvas = this.mCanvas;
        canvas.clipRect(this.mDirtyRect);
        Paint paint = this.mPaint;
        Drawable drawable2 = this.mKeyBackground;
        Rect rect2 = this.mClipRegion;
        Rect rect3 = this.mPadding;
        int i = this.mPaddingLeft;
        int i2 = this.mPaddingTop;
        Keyboard.Key[] keyArr = this.mKeys;
        Keyboard.Key key = this.mInvalidatedKey;
        paint.setColor(this.mKeyTextColor);
        boolean z2 = key != null && canvas.getClipBounds(rect2) && (key.x + i) - 1 <= rect2.left && (key.y + i2) - 1 <= rect2.top && ((key.x + key.width) + i) + 1 >= rect2.right && ((key.y + key.height) + i2) + 1 >= rect2.bottom;
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        int length = keyArr.length;
        int i3 = 0;
        while (i3 < length) {
            Keyboard.Key key2 = keyArr[i3];
            if (z2 && key != key2) {
                drawable = drawable2;
                z = z2;
                rect = rect3;
            } else {
                drawable2.setState(key2.getCurrentDrawableState());
                if (key2.label != null) {
                    string = adjustCase(key2.label).toString();
                } else {
                    string = null;
                }
                Rect bounds = drawable2.getBounds();
                z = z2;
                if (key2.width != bounds.right || key2.height != bounds.bottom) {
                    drawable2.setBounds(0, 0, key2.width, key2.height);
                }
                canvas.translate(key2.x + i, key2.y + i2);
                drawable2.draw(canvas);
                if (string != null) {
                    if (string.length() > 1 && key2.codes.length < 2) {
                        paint.setTextSize(this.mLabelTextSize);
                        paint.setTypeface(Typeface.DEFAULT_BOLD);
                    } else {
                        paint.setTextSize(this.mKeyTextSize);
                        paint.setTypeface(Typeface.DEFAULT);
                    }
                    paint.setShadowLayer(this.mShadowRadius, 0.0f, 0.0f, this.mShadowColor);
                    canvas.drawText(string, (((key2.width - rect3.left) - rect3.right) / 2) + rect3.left, (((key2.height - rect3.top) - rect3.bottom) / 2) + ((paint.getTextSize() - paint.descent()) / 2.0f) + rect3.top, paint);
                    paint.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
                } else {
                    if (key2.icon != null) {
                        canvas.translate(((((key2.width - rect3.left) - rect3.right) - key2.icon.getIntrinsicWidth()) / 2) + rect3.left, ((((key2.height - rect3.top) - rect3.bottom) - key2.icon.getIntrinsicHeight()) / 2) + rect3.top);
                        drawable = drawable2;
                        rect = rect3;
                        key2.icon.setBounds(0, 0, key2.icon.getIntrinsicWidth(), key2.icon.getIntrinsicHeight());
                        key2.icon.draw(canvas);
                        canvas.translate(-r2, -r3);
                    }
                    canvas.translate((-key2.x) - i, (-key2.y) - i2);
                }
                drawable = drawable2;
                rect = rect3;
                canvas.translate((-key2.x) - i, (-key2.y) - i2);
            }
            i3++;
            z2 = z;
            drawable2 = drawable;
            rect3 = rect;
        }
        this.mInvalidatedKey = null;
        if (this.mMiniKeyboardOnScreen) {
            paint.setColor(((int) (this.mBackgroundDimAmount * 255.0f)) << 24);
            canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), paint);
        }
        this.mCanvas.restore();
        this.mDrawPending = false;
        this.mDirtyRect.setEmpty();
    }

    private int getKeyIndices(int i, int i2, int[] iArr) {
        int iSquaredDistanceFrom;
        Keyboard.Key[] keyArr;
        int i3 = i;
        int i4 = i2;
        Keyboard.Key[] keyArr2 = this.mKeys;
        int i5 = this.mProximityThreshold + 1;
        Arrays.fill(this.mDistances, Integer.MAX_VALUE);
        int[] nearestKeys = this.mKeyboard.getNearestKeys(i3, i4);
        int length = nearestKeys.length;
        int i6 = 0;
        int i7 = i5;
        int i8 = 0;
        int i9 = -1;
        int i10 = -1;
        while (i8 < length) {
            Keyboard.Key key = keyArr2[nearestKeys[i8]];
            boolean zIsInside = key.isInside(i3, i4);
            if (zIsInside) {
                i9 = nearestKeys[i8];
            }
            if (this.mProximityCorrectOn) {
                iSquaredDistanceFrom = key.squaredDistanceFrom(i3, i4);
                if (iSquaredDistanceFrom < this.mProximityThreshold) {
                    if (key.codes[i6] > 32) {
                        int length2 = key.codes.length;
                        if (iSquaredDistanceFrom < i7) {
                            i10 = nearestKeys[i8];
                            i7 = iSquaredDistanceFrom;
                        }
                        if (iArr == null) {
                            keyArr = keyArr2;
                        } else {
                            for (int i11 = i6; i11 < this.mDistances.length; i11++) {
                                if (this.mDistances[i11] > iSquaredDistanceFrom) {
                                    int i12 = i11 + length2;
                                    keyArr = keyArr2;
                                    System.arraycopy(this.mDistances, i11, this.mDistances, i12, (this.mDistances.length - i11) - length2);
                                    System.arraycopy(iArr, i11, iArr, i12, (iArr.length - i11) - length2);
                                    for (int i13 = 0; i13 < length2; i13++) {
                                        int i14 = i11 + i13;
                                        iArr[i14] = key.codes[i13];
                                        this.mDistances[i14] = iSquaredDistanceFrom;
                                    }
                                }
                            }
                            keyArr = keyArr2;
                        }
                    }
                }
                i8++;
                keyArr2 = keyArr;
                i3 = i;
                i4 = i2;
                i6 = 0;
            } else {
                iSquaredDistanceFrom = i6;
            }
            if (zIsInside) {
            }
            i8++;
            keyArr2 = keyArr;
            i3 = i;
            i4 = i2;
            i6 = 0;
        }
        return i9 == -1 ? i10 : i9;
    }

    private void detectAndSendKey(int i, int i2, int i3, long j) {
        if (i != -1 && i < this.mKeys.length) {
            Keyboard.Key key = this.mKeys[i];
            if (key.text != null) {
                this.mKeyboardActionListener.onText(key.text);
                this.mKeyboardActionListener.onRelease(-1);
            } else {
                int i4 = key.codes[0];
                int[] iArr = new int[MAX_NEARBY_KEYS];
                Arrays.fill(iArr, -1);
                getKeyIndices(i2, i3, iArr);
                if (this.mInMultiTap) {
                    if (this.mTapCount != -1) {
                        this.mKeyboardActionListener.onKey(-5, KEY_DELETE);
                    } else {
                        this.mTapCount = 0;
                    }
                    i4 = key.codes[this.mTapCount];
                }
                this.mKeyboardActionListener.onKey(i4, iArr);
                this.mKeyboardActionListener.onRelease(i4);
            }
            this.mLastSentIndex = i;
            this.mLastTapTime = j;
        }
    }

    private CharSequence getPreviewText(Keyboard.Key key) {
        if (this.mInMultiTap) {
            this.mPreviewLabel.setLength(0);
            this.mPreviewLabel.append((char) key.codes[this.mTapCount >= 0 ? this.mTapCount : 0]);
            return adjustCase(this.mPreviewLabel);
        }
        return adjustCase(key.label);
    }

    private void showPreview(int i) {
        int i2 = this.mCurrentKeyIndex;
        PopupWindow popupWindow = this.mPreviewPopup;
        this.mCurrentKeyIndex = i;
        Keyboard.Key[] keyArr = this.mKeys;
        if (i2 != this.mCurrentKeyIndex) {
            if (i2 != -1 && keyArr.length > i2) {
                Keyboard.Key key = keyArr[i2];
                key.onReleased(this.mCurrentKeyIndex == -1);
                invalidateKey(i2);
                int i3 = key.codes[0];
                sendAccessibilityEventForUnicodeCharacter(256, i3);
                sendAccessibilityEventForUnicodeCharacter(65536, i3);
            }
            if (this.mCurrentKeyIndex != -1 && keyArr.length > this.mCurrentKeyIndex) {
                Keyboard.Key key2 = keyArr[this.mCurrentKeyIndex];
                key2.onPressed();
                invalidateKey(this.mCurrentKeyIndex);
                int i4 = key2.codes[0];
                sendAccessibilityEventForUnicodeCharacter(128, i4);
                sendAccessibilityEventForUnicodeCharacter(32768, i4);
            }
        }
        if (i2 != this.mCurrentKeyIndex && this.mShowPreview) {
            this.mHandler.removeMessages(1);
            if (popupWindow.isShowing() && i == -1) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 70L);
            }
            if (i != -1) {
                if (popupWindow.isShowing() && this.mPreviewText.getVisibility() == 0) {
                    showKey(i);
                } else {
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, i, 0), 0L);
                }
            }
        }
    }

    private void showKey(int i) {
        PopupWindow popupWindow = this.mPreviewPopup;
        Keyboard.Key[] keyArr = this.mKeys;
        if (i < 0 || i >= this.mKeys.length) {
            return;
        }
        Keyboard.Key key = keyArr[i];
        if (key.icon != null) {
            this.mPreviewText.setCompoundDrawables(null, null, null, key.iconPreview != null ? key.iconPreview : key.icon);
            this.mPreviewText.setText((CharSequence) null);
        } else {
            this.mPreviewText.setCompoundDrawables(null, null, null, null);
            this.mPreviewText.setText(getPreviewText(key));
            if (key.label.length() > 1 && key.codes.length < 2) {
                this.mPreviewText.setTextSize(0, this.mKeyTextSize);
                this.mPreviewText.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                this.mPreviewText.setTextSize(0, this.mPreviewTextSizeLarge);
                this.mPreviewText.setTypeface(Typeface.DEFAULT);
            }
        }
        this.mPreviewText.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
        int iMax = Math.max(this.mPreviewText.getMeasuredWidth(), key.width + this.mPreviewText.getPaddingLeft() + this.mPreviewText.getPaddingRight());
        int i2 = this.mPreviewHeight;
        ViewGroup.LayoutParams layoutParams = this.mPreviewText.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = iMax;
            layoutParams.height = i2;
        }
        if (!this.mPreviewCentered) {
            this.mPopupPreviewX = (key.x - this.mPreviewText.getPaddingLeft()) + this.mPaddingLeft;
            this.mPopupPreviewY = (key.y - i2) + this.mPreviewOffset;
        } else {
            this.mPopupPreviewX = 160 - (this.mPreviewText.getMeasuredWidth() / 2);
            this.mPopupPreviewY = -this.mPreviewText.getMeasuredHeight();
        }
        this.mHandler.removeMessages(2);
        getLocationInWindow(this.mCoordinates);
        int[] iArr = this.mCoordinates;
        iArr[0] = iArr[0] + this.mMiniKeyboardOffsetX;
        int[] iArr2 = this.mCoordinates;
        iArr2[1] = iArr2[1] + this.mMiniKeyboardOffsetY;
        this.mPreviewText.getBackground().setState(key.popupResId != 0 ? LONG_PRESSABLE_STATE_SET : EMPTY_STATE_SET);
        this.mPopupPreviewX += this.mCoordinates[0];
        this.mPopupPreviewY += this.mCoordinates[1];
        getLocationOnScreen(this.mCoordinates);
        if (this.mPopupPreviewY + this.mCoordinates[1] < 0) {
            if (key.x + key.width <= getWidth() / 2) {
                this.mPopupPreviewX += (int) (((double) key.width) * 2.5d);
            } else {
                this.mPopupPreviewX -= (int) (((double) key.width) * 2.5d);
            }
            this.mPopupPreviewY += i2;
        }
        if (popupWindow.isShowing()) {
            popupWindow.update(this.mPopupPreviewX, this.mPopupPreviewY, iMax, i2);
        } else {
            popupWindow.setWidth(iMax);
            popupWindow.setHeight(i2);
            popupWindow.showAtLocation(this.mPopupParent, 0, this.mPopupPreviewX, this.mPopupPreviewY);
        }
        this.mPreviewText.setVisibility(0);
    }

    private void sendAccessibilityEventForUnicodeCharacter(int i, int i2) {
        String string;
        if (this.mAccessibilityManager.isEnabled()) {
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(i);
            onInitializeAccessibilityEvent(accessibilityEventObtain);
            if (i2 != 10) {
                switch (i2) {
                    case -6:
                        string = this.mContext.getString(R.string.keyboardview_keycode_alt);
                        break;
                    case -5:
                        string = this.mContext.getString(R.string.keyboardview_keycode_delete);
                        break;
                    case -4:
                        string = this.mContext.getString(R.string.keyboardview_keycode_done);
                        break;
                    case -3:
                        string = this.mContext.getString(R.string.keyboardview_keycode_cancel);
                        break;
                    case -2:
                        string = this.mContext.getString(R.string.keyboardview_keycode_mode_change);
                        break;
                    case -1:
                        string = this.mContext.getString(R.string.keyboardview_keycode_shift);
                        break;
                    default:
                        string = String.valueOf((char) i2);
                        break;
                }
            } else {
                string = this.mContext.getString(R.string.keyboardview_keycode_enter);
            }
            accessibilityEventObtain.getText().add(string);
            this.mAccessibilityManager.sendAccessibilityEvent(accessibilityEventObtain);
        }
    }

    public void invalidateAllKeys() {
        this.mDirtyRect.union(0, 0, getWidth(), getHeight());
        this.mDrawPending = true;
        invalidate();
    }

    public void invalidateKey(int i) {
        if (this.mKeys == null || i < 0 || i >= this.mKeys.length) {
            return;
        }
        Keyboard.Key key = this.mKeys[i];
        this.mInvalidatedKey = key;
        this.mDirtyRect.union(key.x + this.mPaddingLeft, key.y + this.mPaddingTop, key.x + key.width + this.mPaddingLeft, key.y + key.height + this.mPaddingTop);
        onBufferDraw();
        invalidate(key.x + this.mPaddingLeft, key.y + this.mPaddingTop, key.x + key.width + this.mPaddingLeft, key.y + key.height + this.mPaddingTop);
    }

    private boolean openPopupIfRequired(MotionEvent motionEvent) {
        if (this.mPopupLayout == 0 || this.mCurrentKey < 0 || this.mCurrentKey >= this.mKeys.length) {
            return false;
        }
        boolean zOnLongPress = onLongPress(this.mKeys[this.mCurrentKey]);
        if (zOnLongPress) {
            this.mAbortKey = true;
            showPreview(-1);
        }
        return zOnLongPress;
    }

    protected boolean onLongPress(Keyboard.Key key) {
        Keyboard keyboard;
        int i = key.popupResId;
        if (i == 0) {
            return false;
        }
        this.mMiniKeyboardContainer = this.mMiniKeyboardCache.get(key);
        if (this.mMiniKeyboardContainer == null) {
            this.mMiniKeyboardContainer = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(this.mPopupLayout, (ViewGroup) null);
            this.mMiniKeyboard = (KeyboardView) this.mMiniKeyboardContainer.findViewById(16908326);
            View viewFindViewById = this.mMiniKeyboardContainer.findViewById(16908327);
            if (viewFindViewById != null) {
                viewFindViewById.setOnClickListener(this);
            }
            this.mMiniKeyboard.setOnKeyboardActionListener(new OnKeyboardActionListener() {
                @Override
                public void onKey(int i2, int[] iArr) {
                    KeyboardView.this.mKeyboardActionListener.onKey(i2, iArr);
                    KeyboardView.this.dismissPopupKeyboard();
                }

                @Override
                public void onText(CharSequence charSequence) {
                    KeyboardView.this.mKeyboardActionListener.onText(charSequence);
                    KeyboardView.this.dismissPopupKeyboard();
                }

                @Override
                public void swipeLeft() {
                }

                @Override
                public void swipeRight() {
                }

                @Override
                public void swipeUp() {
                }

                @Override
                public void swipeDown() {
                }

                @Override
                public void onPress(int i2) {
                    KeyboardView.this.mKeyboardActionListener.onPress(i2);
                }

                @Override
                public void onRelease(int i2) {
                    KeyboardView.this.mKeyboardActionListener.onRelease(i2);
                }
            });
            if (key.popupCharacters != null) {
                keyboard = new Keyboard(getContext(), i, key.popupCharacters, -1, getPaddingRight() + getPaddingLeft());
            } else {
                keyboard = new Keyboard(getContext(), i);
            }
            this.mMiniKeyboard.setKeyboard(keyboard);
            this.mMiniKeyboard.setPopupParent(this);
            this.mMiniKeyboardContainer.measure(View.MeasureSpec.makeMeasureSpec(getWidth(), Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(getHeight(), Integer.MIN_VALUE));
            this.mMiniKeyboardCache.put(key, this.mMiniKeyboardContainer);
        } else {
            this.mMiniKeyboard = (KeyboardView) this.mMiniKeyboardContainer.findViewById(16908326);
        }
        getLocationInWindow(this.mCoordinates);
        this.mPopupX = key.x + this.mPaddingLeft;
        this.mPopupY = key.y + this.mPaddingTop;
        this.mPopupX = (this.mPopupX + key.width) - this.mMiniKeyboardContainer.getMeasuredWidth();
        this.mPopupY -= this.mMiniKeyboardContainer.getMeasuredHeight();
        int paddingRight = this.mPopupX + this.mMiniKeyboardContainer.getPaddingRight() + this.mCoordinates[0];
        int paddingBottom = this.mPopupY + this.mMiniKeyboardContainer.getPaddingBottom() + this.mCoordinates[1];
        this.mMiniKeyboard.setPopupOffset(paddingRight < 0 ? 0 : paddingRight, paddingBottom);
        this.mMiniKeyboard.setShifted(isShifted());
        this.mPopupKeyboard.setContentView(this.mMiniKeyboardContainer);
        this.mPopupKeyboard.setWidth(this.mMiniKeyboardContainer.getMeasuredWidth());
        this.mPopupKeyboard.setHeight(this.mMiniKeyboardContainer.getMeasuredHeight());
        this.mPopupKeyboard.showAtLocation(this, 0, paddingRight, paddingBottom);
        this.mMiniKeyboardOnScreen = true;
        invalidateAllKeys();
        return true;
    }

    @Override
    public boolean onHoverEvent(MotionEvent motionEvent) {
        if (!this.mAccessibilityManager.isTouchExplorationEnabled() || motionEvent.getPointerCount() != 1) {
            return true;
        }
        int action = motionEvent.getAction();
        if (action != 7) {
            switch (action) {
                case 9:
                    motionEvent.setAction(0);
                    break;
                case 10:
                    motionEvent.setAction(1);
                    break;
            }
        } else {
            motionEvent.setAction(2);
        }
        return onTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        int action = motionEvent.getAction();
        long eventTime = motionEvent.getEventTime();
        boolean zOnModifiedTouchEvent = true;
        if (pointerCount != this.mOldPointerCount) {
            if (pointerCount == 1) {
                MotionEvent motionEventObtain = MotionEvent.obtain(eventTime, eventTime, 0, motionEvent.getX(), motionEvent.getY(), motionEvent.getMetaState());
                boolean zOnModifiedTouchEvent2 = onModifiedTouchEvent(motionEventObtain, false);
                motionEventObtain.recycle();
                zOnModifiedTouchEvent = action == 1 ? onModifiedTouchEvent(motionEvent, true) : zOnModifiedTouchEvent2;
            } else {
                MotionEvent motionEventObtain2 = MotionEvent.obtain(eventTime, eventTime, 1, this.mOldPointerX, this.mOldPointerY, motionEvent.getMetaState());
                zOnModifiedTouchEvent = onModifiedTouchEvent(motionEventObtain2, true);
                motionEventObtain2.recycle();
            }
        } else if (pointerCount == 1) {
            zOnModifiedTouchEvent = onModifiedTouchEvent(motionEvent, false);
            this.mOldPointerX = motionEvent.getX();
            this.mOldPointerY = motionEvent.getY();
        }
        this.mOldPointerCount = pointerCount;
        return zOnModifiedTouchEvent;
    }

    private boolean onModifiedTouchEvent(MotionEvent motionEvent, boolean z) {
        boolean z2;
        int i;
        int i2;
        int x = ((int) motionEvent.getX()) - this.mPaddingLeft;
        int y = ((int) motionEvent.getY()) - this.mPaddingTop;
        if (y >= (-this.mVerticalCorrection)) {
            y += this.mVerticalCorrection;
        }
        int action = motionEvent.getAction();
        long eventTime = motionEvent.getEventTime();
        int keyIndices = getKeyIndices(x, y, null);
        this.mPossiblePoly = z;
        if (action == 0) {
            this.mSwipeTracker.clear();
        }
        this.mSwipeTracker.addMovement(motionEvent);
        if (this.mAbortKey && action != 0 && action != 3) {
            return true;
        }
        if (this.mGestureDetector.onTouchEvent(motionEvent)) {
            showPreview(-1);
            this.mHandler.removeMessages(3);
            this.mHandler.removeMessages(4);
            return true;
        }
        if (this.mMiniKeyboardOnScreen && action != 3) {
            return true;
        }
        switch (action) {
            case 0:
                this.mAbortKey = false;
                this.mStartX = x;
                this.mStartY = y;
                this.mLastCodeX = x;
                this.mLastCodeY = y;
                this.mLastKeyTime = 0L;
                this.mCurrentKeyTime = 0L;
                this.mLastKey = -1;
                this.mCurrentKey = keyIndices;
                this.mDownKey = keyIndices;
                this.mDownTime = motionEvent.getEventTime();
                this.mLastMoveTime = this.mDownTime;
                checkMultiTap(eventTime, keyIndices);
                this.mKeyboardActionListener.onPress(keyIndices != -1 ? this.mKeys[keyIndices].codes[0] : 0);
                if (this.mCurrentKey >= 0 && this.mKeys[this.mCurrentKey].repeatable) {
                    this.mRepeatKeyIndex = this.mCurrentKey;
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3), 400L);
                    repeatKey();
                    if (this.mAbortKey) {
                        this.mRepeatKeyIndex = -1;
                        break;
                    }
                } else {
                    if (this.mCurrentKey != -1) {
                        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4, motionEvent), LONGPRESS_TIMEOUT);
                    }
                    showPreview(keyIndices);
                    break;
                }
                break;
            case 1:
                removeMessages();
                if (keyIndices == this.mCurrentKey) {
                    this.mCurrentKeyTime += eventTime - this.mLastMoveTime;
                } else {
                    resetMultiTap();
                    this.mLastKey = this.mCurrentKey;
                    this.mLastKeyTime = (this.mCurrentKeyTime + eventTime) - this.mLastMoveTime;
                    this.mCurrentKey = keyIndices;
                    this.mCurrentKeyTime = 0L;
                }
                if (this.mCurrentKeyTime >= this.mLastKeyTime || this.mCurrentKeyTime >= 70 || this.mLastKey == -1) {
                    i = x;
                    i2 = y;
                } else {
                    this.mCurrentKey = this.mLastKey;
                    i = this.mLastCodeX;
                    i2 = this.mLastCodeY;
                }
                showPreview(-1);
                Arrays.fill(this.mKeyIndices, -1);
                if (this.mRepeatKeyIndex == -1 && !this.mMiniKeyboardOnScreen && !this.mAbortKey) {
                    detectAndSendKey(this.mCurrentKey, i, i2, eventTime);
                }
                invalidateKey(keyIndices);
                this.mRepeatKeyIndex = -1;
                x = i;
                y = i2;
                break;
            case 2:
                if (keyIndices != -1) {
                    if (this.mCurrentKey == -1) {
                        this.mCurrentKey = keyIndices;
                        this.mCurrentKeyTime = eventTime - this.mDownTime;
                    } else if (keyIndices != this.mCurrentKey) {
                        if (this.mRepeatKeyIndex == -1) {
                            resetMultiTap();
                            this.mLastKey = this.mCurrentKey;
                            this.mLastCodeX = this.mLastX;
                            this.mLastCodeY = this.mLastY;
                            this.mLastKeyTime = (this.mCurrentKeyTime + eventTime) - this.mLastMoveTime;
                            this.mCurrentKey = keyIndices;
                            this.mCurrentKeyTime = 0L;
                        }
                    } else {
                        this.mCurrentKeyTime += eventTime - this.mLastMoveTime;
                        z2 = true;
                        if (!z2) {
                        }
                        showPreview(this.mCurrentKey);
                        this.mLastMoveTime = eventTime;
                    }
                    z2 = false;
                    if (!z2) {
                    }
                    showPreview(this.mCurrentKey);
                    this.mLastMoveTime = eventTime;
                } else {
                    z2 = false;
                    if (!z2) {
                        this.mHandler.removeMessages(4);
                        if (keyIndices != -1) {
                            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4, motionEvent), LONGPRESS_TIMEOUT);
                        }
                    }
                    showPreview(this.mCurrentKey);
                    this.mLastMoveTime = eventTime;
                }
                break;
            case 3:
                removeMessages();
                dismissPopupKeyboard();
                this.mAbortKey = true;
                showPreview(-1);
                invalidateKey(this.mCurrentKey);
                break;
        }
        this.mLastX = x;
        this.mLastY = y;
        return true;
    }

    private boolean repeatKey() {
        Keyboard.Key key = this.mKeys[this.mRepeatKeyIndex];
        detectAndSendKey(this.mCurrentKey, key.x, key.y, this.mLastTapTime);
        return true;
    }

    protected void swipeRight() {
        this.mKeyboardActionListener.swipeRight();
    }

    protected void swipeLeft() {
        this.mKeyboardActionListener.swipeLeft();
    }

    protected void swipeUp() {
        this.mKeyboardActionListener.swipeUp();
    }

    protected void swipeDown() {
        this.mKeyboardActionListener.swipeDown();
    }

    public void closing() {
        if (this.mPreviewPopup.isShowing()) {
            this.mPreviewPopup.dismiss();
        }
        removeMessages();
        dismissPopupKeyboard();
        this.mBuffer = null;
        this.mCanvas = null;
        this.mMiniKeyboardCache.clear();
    }

    private void removeMessages() {
        if (this.mHandler != null) {
            this.mHandler.removeMessages(3);
            this.mHandler.removeMessages(4);
            this.mHandler.removeMessages(1);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        closing();
    }

    private void dismissPopupKeyboard() {
        if (this.mPopupKeyboard.isShowing()) {
            this.mPopupKeyboard.dismiss();
            this.mMiniKeyboardOnScreen = false;
            invalidateAllKeys();
        }
    }

    public boolean handleBack() {
        if (this.mPopupKeyboard.isShowing()) {
            dismissPopupKeyboard();
            return true;
        }
        return false;
    }

    private void resetMultiTap() {
        this.mLastSentIndex = -1;
        this.mTapCount = 0;
        this.mLastTapTime = -1L;
        this.mInMultiTap = false;
    }

    private void checkMultiTap(long j, int i) {
        if (i == -1) {
            return;
        }
        Keyboard.Key key = this.mKeys[i];
        if (key.codes.length <= 1) {
            if (j > this.mLastTapTime + 800 || i != this.mLastSentIndex) {
                resetMultiTap();
                return;
            }
            return;
        }
        this.mInMultiTap = true;
        if (j < this.mLastTapTime + 800 && i == this.mLastSentIndex) {
            this.mTapCount = (this.mTapCount + 1) % key.codes.length;
        } else {
            this.mTapCount = -1;
        }
    }

    private static class SwipeTracker {
        static final int LONGEST_PAST_TIME = 200;
        static final int NUM_PAST = 4;
        final long[] mPastTime;
        final float[] mPastX;
        final float[] mPastY;
        float mXVelocity;
        float mYVelocity;

        private SwipeTracker() {
            this.mPastX = new float[4];
            this.mPastY = new float[4];
            this.mPastTime = new long[4];
        }

        public void clear() {
            this.mPastTime[0] = 0;
        }

        public void addMovement(MotionEvent motionEvent) {
            long eventTime = motionEvent.getEventTime();
            int historySize = motionEvent.getHistorySize();
            for (int i = 0; i < historySize; i++) {
                addPoint(motionEvent.getHistoricalX(i), motionEvent.getHistoricalY(i), motionEvent.getHistoricalEventTime(i));
            }
            addPoint(motionEvent.getX(), motionEvent.getY(), eventTime);
        }

        private void addPoint(float f, float f2, long j) {
            long[] jArr = this.mPastTime;
            int i = -1;
            int i2 = 0;
            while (i2 < 4 && jArr[i2] != 0) {
                if (jArr[i2] < j - 200) {
                    i = i2;
                }
                i2++;
            }
            if (i2 == 4 && i < 0) {
                i = 0;
            }
            if (i == i2) {
                i--;
            }
            float[] fArr = this.mPastX;
            float[] fArr2 = this.mPastY;
            if (i >= 0) {
                int i3 = i + 1;
                int i4 = (4 - i) - 1;
                System.arraycopy(fArr, i3, fArr, 0, i4);
                System.arraycopy(fArr2, i3, fArr2, 0, i4);
                System.arraycopy(jArr, i3, jArr, 0, i4);
                i2 -= i3;
            }
            fArr[i2] = f;
            fArr2[i2] = f2;
            jArr[i2] = j;
            int i5 = i2 + 1;
            if (i5 < 4) {
                jArr[i5] = 0;
            }
        }

        public void computeCurrentVelocity(int i) {
            computeCurrentVelocity(i, Float.MAX_VALUE);
        }

        public void computeCurrentVelocity(int i, float f) {
            float[] fArr;
            float[] fArr2 = this.mPastX;
            float[] fArr3 = this.mPastY;
            long[] jArr = this.mPastTime;
            int i2 = 0;
            float f2 = fArr2[0];
            float f3 = fArr3[0];
            long j = jArr[0];
            while (i2 < 4 && jArr[i2] != 0) {
                i2++;
            }
            int i3 = 1;
            float f4 = 0.0f;
            float f5 = 0.0f;
            while (i3 < i2) {
                int i4 = (int) (jArr[i3] - j);
                if (i4 == 0) {
                    fArr = fArr2;
                } else {
                    float f6 = i4;
                    float f7 = (fArr2[i3] - f2) / f6;
                    fArr = fArr2;
                    float f8 = i;
                    float f9 = f7 * f8;
                    if (f4 != 0.0f) {
                        f9 = (f4 + f9) * 0.5f;
                    }
                    float f10 = ((fArr3[i3] - f3) / f6) * f8;
                    f5 = f5 == 0.0f ? f10 : (f5 + f10) * 0.5f;
                    f4 = f9;
                }
                i3++;
                fArr2 = fArr;
            }
            this.mXVelocity = f4 < 0.0f ? Math.max(f4, -f) : Math.min(f4, f);
            this.mYVelocity = f5 < 0.0f ? Math.max(f5, -f) : Math.min(f5, f);
        }

        public float getXVelocity() {
            return this.mXVelocity;
        }

        public float getYVelocity() {
            return this.mYVelocity;
        }
    }
}

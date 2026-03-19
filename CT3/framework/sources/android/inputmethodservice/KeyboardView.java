package android.inputmethodservice;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
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
    private static final int[] LONG_PRESSABLE_STATE_SET = {R.attr.state_long_pressable};
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

    public KeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 18219138);
    }

    public KeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public KeyboardView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
        this.mSwipeTracker = new SwipeTracker(null);
        this.mOldPointerCount = 1;
        this.mDistances = new int[MAX_NEARBY_KEYS];
        this.mPreviewLabel = new StringBuilder(1);
        this.mDirtyRect = new Rect();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyboardView, defStyleAttr, defStyleRes);
        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int previewLayout = 0;
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case 0:
                    this.mShadowColor = a.getColor(attr, 0);
                    break;
                case 1:
                    this.mShadowRadius = a.getFloat(attr, 0.0f);
                    break;
                case 2:
                    this.mKeyBackground = a.getDrawable(attr);
                    break;
                case 3:
                    this.mKeyTextSize = a.getDimensionPixelSize(attr, 18);
                    break;
                case 4:
                    this.mLabelTextSize = a.getDimensionPixelSize(attr, 14);
                    break;
                case 5:
                    this.mKeyTextColor = a.getColor(attr, -16777216);
                    break;
                case 6:
                    previewLayout = a.getResourceId(attr, 0);
                    break;
                case 7:
                    this.mPreviewOffset = a.getDimensionPixelOffset(attr, 0);
                    break;
                case 8:
                    this.mPreviewHeight = a.getDimensionPixelSize(attr, 80);
                    break;
                case 9:
                    this.mVerticalCorrection = a.getDimensionPixelOffset(attr, 0);
                    break;
                case 10:
                    this.mPopupLayout = a.getResourceId(attr, 0);
                    break;
            }
        }
        this.mBackgroundDimAmount = this.mContext.obtainStyledAttributes(com.android.internal.R.styleable.Theme).getFloat(2, 0.5f);
        this.mPreviewPopup = new PopupWindow(context);
        if (previewLayout != 0) {
            this.mPreviewText = (TextView) inflate.inflate(previewLayout, (ViewGroup) null);
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
        this.mPaint.setTextSize(0.0f);
        this.mPaint.setTextAlign(Paint.Align.CENTER);
        this.mPaint.setAlpha(255);
        this.mPadding = new Rect(0, 0, 0, 0);
        this.mMiniKeyboardCache = new HashMap();
        this.mKeyBackground.getPadding(this.mPadding);
        this.mSwipeThreshold = (int) (getResources().getDisplayMetrics().density * 500.0f);
        this.mDisambiguateSwipe = getResources().getBoolean(17956940);
        this.mAccessibilityManager = AccessibilityManager.getInstance(context);
        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        resetMultiTap();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initGestureDetector();
        if (this.mHandler != null) {
            return;
        }
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        KeyboardView.this.showKey(msg.arg1);
                        break;
                    case 2:
                        KeyboardView.this.mPreviewText.setVisibility(4);
                        break;
                    case 3:
                        if (KeyboardView.this.repeatKey()) {
                            Message repeat = Message.obtain(this, 3);
                            sendMessageDelayed(repeat, 50L);
                        }
                        break;
                    case 4:
                        KeyboardView.this.openPopupIfRequired((MotionEvent) msg.obj);
                        break;
                }
            }
        };
    }

    private void initGestureDetector() {
        if (this.mGestureDetector != null) {
            return;
        }
        this.mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
                if (KeyboardView.this.mPossiblePoly) {
                    return false;
                }
                float absX = Math.abs(velocityX);
                float absY = Math.abs(velocityY);
                float deltaX = me2.getX() - me1.getX();
                float deltaY = me2.getY() - me1.getY();
                int travelX = KeyboardView.this.getWidth() / 2;
                int travelY = KeyboardView.this.getHeight() / 2;
                KeyboardView.this.mSwipeTracker.computeCurrentVelocity(1000);
                float endingVelocityX = KeyboardView.this.mSwipeTracker.getXVelocity();
                float endingVelocityY = KeyboardView.this.mSwipeTracker.getYVelocity();
                boolean sendDownKey = false;
                if (velocityX <= KeyboardView.this.mSwipeThreshold || absY >= absX || deltaX <= travelX) {
                    if (velocityX >= (-KeyboardView.this.mSwipeThreshold) || absY >= absX || deltaX >= (-travelX)) {
                        if (velocityY >= (-KeyboardView.this.mSwipeThreshold) || absX >= absY || deltaY >= (-travelY)) {
                            if (velocityY > KeyboardView.this.mSwipeThreshold && absX < absY / 2.0f && deltaY > travelY) {
                                if (KeyboardView.this.mDisambiguateSwipe && endingVelocityY < velocityY / 4.0f) {
                                    sendDownKey = true;
                                } else {
                                    KeyboardView.this.swipeDown();
                                    return true;
                                }
                            }
                        } else if (KeyboardView.this.mDisambiguateSwipe && endingVelocityY > velocityY / 4.0f) {
                            sendDownKey = true;
                        } else {
                            KeyboardView.this.swipeUp();
                            return true;
                        }
                    } else if (KeyboardView.this.mDisambiguateSwipe && endingVelocityX > velocityX / 4.0f) {
                        sendDownKey = true;
                    } else {
                        KeyboardView.this.swipeLeft();
                        return true;
                    }
                } else if (KeyboardView.this.mDisambiguateSwipe && endingVelocityX < velocityX / 4.0f) {
                    sendDownKey = true;
                } else {
                    KeyboardView.this.swipeRight();
                    return true;
                }
                if (sendDownKey) {
                    KeyboardView.this.detectAndSendKey(KeyboardView.this.mDownKey, KeyboardView.this.mStartX, KeyboardView.this.mStartY, me1.getEventTime());
                    return false;
                }
                return false;
            }
        });
        this.mGestureDetector.setIsLongpressEnabled(false);
    }

    public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
        this.mKeyboardActionListener = listener;
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

    public boolean setShifted(boolean shifted) {
        if (this.mKeyboard != null && this.mKeyboard.setShifted(shifted)) {
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

    public void setPreviewEnabled(boolean previewEnabled) {
        this.mShowPreview = previewEnabled;
    }

    public boolean isPreviewEnabled() {
        return this.mShowPreview;
    }

    public void setVerticalCorrection(int verticalOffset) {
    }

    public void setPopupParent(View v) {
        this.mPopupParent = v;
    }

    public void setPopupOffset(int x, int y) {
        this.mMiniKeyboardOffsetX = x;
        this.mMiniKeyboardOffsetY = y;
        if (!this.mPreviewPopup.isShowing()) {
            return;
        }
        this.mPreviewPopup.dismiss();
    }

    public void setProximityCorrectionEnabled(boolean enabled) {
        this.mProximityCorrectOn = enabled;
    }

    public boolean isProximityCorrectionEnabled() {
        return this.mProximityCorrectOn;
    }

    @Override
    public void onClick(View v) {
        dismissPopupKeyboard();
    }

    private CharSequence adjustCase(CharSequence label) {
        if (this.mKeyboard.isShifted() && label != null && label.length() < 3 && Character.isLowerCase(label.charAt(0))) {
            return label.toString().toUpperCase();
        }
        return label;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mKeyboard == null) {
            setMeasuredDimension(this.mPaddingLeft + this.mPaddingRight, this.mPaddingTop + this.mPaddingBottom);
            return;
        }
        int width = this.mKeyboard.getMinWidth() + this.mPaddingLeft + this.mPaddingRight;
        if (View.MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
            width = View.MeasureSpec.getSize(widthMeasureSpec);
        }
        setMeasuredDimension(width, this.mKeyboard.getHeight() + this.mPaddingTop + this.mPaddingBottom);
    }

    private void computeProximityThreshold(Keyboard keyboard) {
        Keyboard.Key[] keys;
        if (keyboard == null || (keys = this.mKeys) == null) {
            return;
        }
        int length = keys.length;
        int dimensionSum = 0;
        for (Keyboard.Key key : keys) {
            dimensionSum += Math.min(key.width, key.height) + key.gap;
        }
        if (dimensionSum < 0 || length == 0) {
            return;
        }
        this.mProximityThreshold = (int) ((dimensionSum * 1.4f) / length);
        this.mProximityThreshold *= this.mProximityThreshold;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mKeyboard != null) {
            this.mKeyboard.resize(w, h);
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
        if (this.mBuffer == null || this.mKeyboardChanged) {
            if (this.mBuffer == null || (this.mKeyboardChanged && (this.mBuffer.getWidth() != getWidth() || this.mBuffer.getHeight() != getHeight()))) {
                int width = Math.max(1, getWidth());
                int height = Math.max(1, getHeight());
                this.mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                this.mCanvas = new Canvas(this.mBuffer);
            }
            invalidateAllKeys();
            this.mKeyboardChanged = false;
        }
        Canvas canvas = this.mCanvas;
        canvas.clipRect(this.mDirtyRect, Region.Op.REPLACE);
        if (this.mKeyboard == null) {
            return;
        }
        Paint paint = this.mPaint;
        Drawable keyBackground = this.mKeyBackground;
        Rect clipRegion = this.mClipRegion;
        Rect padding = this.mPadding;
        int kbdPaddingLeft = this.mPaddingLeft;
        int kbdPaddingTop = this.mPaddingTop;
        Keyboard.Key[] keys = this.mKeys;
        Keyboard.Key invalidKey = this.mInvalidatedKey;
        paint.setColor(this.mKeyTextColor);
        boolean drawSingleKey = false;
        if (invalidKey != null && canvas.getClipBounds(clipRegion) && (invalidKey.x + kbdPaddingLeft) - 1 <= clipRegion.left && (invalidKey.y + kbdPaddingTop) - 1 <= clipRegion.top && invalidKey.x + invalidKey.width + kbdPaddingLeft + 1 >= clipRegion.right && invalidKey.y + invalidKey.height + kbdPaddingTop + 1 >= clipRegion.bottom) {
            drawSingleKey = true;
        }
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        for (Keyboard.Key key : keys) {
            if (!drawSingleKey || invalidKey == key) {
                int[] drawableState = key.getCurrentDrawableState();
                keyBackground.setState(drawableState);
                String string = key.label == null ? null : adjustCase(key.label).toString();
                Rect bounds = keyBackground.getBounds();
                if (key.width != bounds.right || key.height != bounds.bottom) {
                    keyBackground.setBounds(0, 0, key.width, key.height);
                }
                canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);
                keyBackground.draw(canvas);
                if (string != null) {
                    if (string.length() > 1 && key.codes.length < 2) {
                        paint.setTextSize(this.mLabelTextSize);
                        paint.setTypeface(Typeface.DEFAULT_BOLD);
                    } else {
                        paint.setTextSize(this.mKeyTextSize);
                        paint.setTypeface(Typeface.DEFAULT);
                    }
                    paint.setShadowLayer(this.mShadowRadius, 0.0f, 0.0f, this.mShadowColor);
                    canvas.drawText(string, (((key.width - padding.left) - padding.right) / 2) + padding.left, (((key.height - padding.top) - padding.bottom) / 2) + ((paint.getTextSize() - paint.descent()) / 2.0f) + padding.top, paint);
                    paint.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
                } else if (key.icon != null) {
                    int drawableX = ((((key.width - padding.left) - padding.right) - key.icon.getIntrinsicWidth()) / 2) + padding.left;
                    int drawableY = ((((key.height - padding.top) - padding.bottom) - key.icon.getIntrinsicHeight()) / 2) + padding.top;
                    canvas.translate(drawableX, drawableY);
                    key.icon.setBounds(0, 0, key.icon.getIntrinsicWidth(), key.icon.getIntrinsicHeight());
                    key.icon.draw(canvas);
                    canvas.translate(-drawableX, -drawableY);
                }
                canvas.translate((-key.x) - kbdPaddingLeft, (-key.y) - kbdPaddingTop);
            }
        }
        this.mInvalidatedKey = null;
        if (this.mMiniKeyboardOnScreen) {
            paint.setColor(((int) (this.mBackgroundDimAmount * 255.0f)) << 24);
            canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), paint);
        }
        this.mDrawPending = false;
        this.mDirtyRect.setEmpty();
    }

    private int getKeyIndices(int x, int y, int[] allKeys) {
        Keyboard.Key[] keys = this.mKeys;
        int primaryIndex = -1;
        int closestKey = -1;
        int closestKeyDist = this.mProximityThreshold + 1;
        Arrays.fill(this.mDistances, Integer.MAX_VALUE);
        int[] nearestKeyIndices = this.mKeyboard.getNearestKeys(x, y);
        int keyCount = nearestKeyIndices.length;
        for (int i = 0; i < keyCount; i++) {
            Keyboard.Key key = keys[nearestKeyIndices[i]];
            int dist = 0;
            boolean isInside = key.isInside(x, y);
            if (isInside) {
                primaryIndex = nearestKeyIndices[i];
            }
            if (((this.mProximityCorrectOn && (dist = key.squaredDistanceFrom(x, y)) < this.mProximityThreshold) || isInside) && key.codes[0] > 32) {
                int nCodes = key.codes.length;
                if (dist < closestKeyDist) {
                    closestKeyDist = dist;
                    closestKey = nearestKeyIndices[i];
                }
                if (allKeys != null) {
                    int j = 0;
                    while (true) {
                        if (j >= this.mDistances.length) {
                            break;
                        }
                        if (this.mDistances[j] <= dist) {
                            j++;
                        } else {
                            System.arraycopy(this.mDistances, j, this.mDistances, j + nCodes, (this.mDistances.length - j) - nCodes);
                            System.arraycopy(allKeys, j, allKeys, j + nCodes, (allKeys.length - j) - nCodes);
                            for (int c = 0; c < nCodes; c++) {
                                allKeys[j + c] = key.codes[c];
                                this.mDistances[j + c] = dist;
                            }
                        }
                    }
                }
            }
        }
        if (primaryIndex == -1) {
            int primaryIndex2 = closestKey;
            return primaryIndex2;
        }
        return primaryIndex;
    }

    private void detectAndSendKey(int index, int x, int y, long eventTime) {
        if (index == -1 || index >= this.mKeys.length) {
            return;
        }
        Keyboard.Key key = this.mKeys[index];
        if (key.text != null) {
            this.mKeyboardActionListener.onText(key.text);
            this.mKeyboardActionListener.onRelease(-1);
        } else {
            int code = key.codes[0];
            int[] codes = new int[MAX_NEARBY_KEYS];
            Arrays.fill(codes, -1);
            getKeyIndices(x, y, codes);
            if (this.mInMultiTap) {
                if (this.mTapCount != -1) {
                    this.mKeyboardActionListener.onKey(-5, KEY_DELETE);
                } else {
                    this.mTapCount = 0;
                }
                code = key.codes[this.mTapCount];
            }
            this.mKeyboardActionListener.onKey(code, codes);
            this.mKeyboardActionListener.onRelease(code);
        }
        this.mLastSentIndex = index;
        this.mLastTapTime = eventTime;
    }

    private CharSequence getPreviewText(Keyboard.Key key) {
        if (this.mInMultiTap) {
            this.mPreviewLabel.setLength(0);
            this.mPreviewLabel.append((char) key.codes[this.mTapCount >= 0 ? this.mTapCount : 0]);
            return adjustCase(this.mPreviewLabel);
        }
        return adjustCase(key.label);
    }

    private void showPreview(int keyIndex) {
        int oldKeyIndex = this.mCurrentKeyIndex;
        PopupWindow previewPopup = this.mPreviewPopup;
        this.mCurrentKeyIndex = keyIndex;
        Keyboard.Key[] keys = this.mKeys;
        if (oldKeyIndex != this.mCurrentKeyIndex) {
            if (oldKeyIndex != -1 && keys.length > oldKeyIndex) {
                Keyboard.Key oldKey = keys[oldKeyIndex];
                oldKey.onReleased(this.mCurrentKeyIndex == -1);
                invalidateKey(oldKeyIndex);
                int keyCode = oldKey.codes[0];
                sendAccessibilityEventForUnicodeCharacter(256, keyCode);
                sendAccessibilityEventForUnicodeCharacter(65536, keyCode);
            }
            if (this.mCurrentKeyIndex != -1 && keys.length > this.mCurrentKeyIndex) {
                Keyboard.Key newKey = keys[this.mCurrentKeyIndex];
                newKey.onPressed();
                invalidateKey(this.mCurrentKeyIndex);
                int keyCode2 = newKey.codes[0];
                sendAccessibilityEventForUnicodeCharacter(128, keyCode2);
                sendAccessibilityEventForUnicodeCharacter(32768, keyCode2);
            }
        }
        if (oldKeyIndex == this.mCurrentKeyIndex || !this.mShowPreview) {
            return;
        }
        this.mHandler.removeMessages(1);
        if (previewPopup.isShowing() && keyIndex == -1) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 70L);
        }
        if (keyIndex == -1) {
            return;
        }
        if (previewPopup.isShowing() && this.mPreviewText.getVisibility() == 0) {
            showKey(keyIndex);
        } else {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, keyIndex, 0), 0L);
        }
    }

    private void showKey(int keyIndex) {
        PopupWindow previewPopup = this.mPreviewPopup;
        Keyboard.Key[] keys = this.mKeys;
        if (keyIndex < 0 || keyIndex >= this.mKeys.length) {
            return;
        }
        Keyboard.Key key = keys[keyIndex];
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
        int popupWidth = Math.max(this.mPreviewText.getMeasuredWidth(), key.width + this.mPreviewText.getPaddingLeft() + this.mPreviewText.getPaddingRight());
        int popupHeight = this.mPreviewHeight;
        ViewGroup.LayoutParams lp = this.mPreviewText.getLayoutParams();
        if (lp != null) {
            lp.width = popupWidth;
            lp.height = popupHeight;
        }
        if (!this.mPreviewCentered) {
            this.mPopupPreviewX = (key.x - this.mPreviewText.getPaddingLeft()) + this.mPaddingLeft;
            this.mPopupPreviewY = (key.y - popupHeight) + this.mPreviewOffset;
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
            this.mPopupPreviewY += popupHeight;
        }
        if (previewPopup.isShowing()) {
            previewPopup.update(this.mPopupPreviewX, this.mPopupPreviewY, popupWidth, popupHeight);
        } else {
            previewPopup.setWidth(popupWidth);
            previewPopup.setHeight(popupHeight);
            previewPopup.showAtLocation(this.mPopupParent, 0, this.mPopupPreviewX, this.mPopupPreviewY);
        }
        this.mPreviewText.setVisibility(0);
    }

    private void sendAccessibilityEventForUnicodeCharacter(int eventType, int code) {
        String text;
        if (!this.mAccessibilityManager.isEnabled()) {
            return;
        }
        AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        onInitializeAccessibilityEvent(event);
        boolean speakPassword = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD, 0, -3) != 0;
        if (speakPassword || this.mAudioManager.isBluetoothA2dpOn() || this.mAudioManager.isWiredHeadsetOn()) {
            switch (code) {
                case -6:
                    text = this.mContext.getString(17040553);
                    break;
                case -5:
                    text = this.mContext.getString(17040555);
                    break;
                case -4:
                    text = this.mContext.getString(17040556);
                    break;
                case -3:
                    text = this.mContext.getString(17040554);
                    break;
                case -2:
                    text = this.mContext.getString(17040557);
                    break;
                case -1:
                    text = this.mContext.getString(17040558);
                    break;
                case 10:
                    text = this.mContext.getString(17040559);
                    break;
                default:
                    text = String.valueOf((char) code);
                    break;
            }
        } else if (!this.mHeadsetRequiredToHearPasswordsAnnounced) {
            if (eventType == 256) {
                this.mHeadsetRequiredToHearPasswordsAnnounced = true;
            }
            text = this.mContext.getString(17040566);
        } else {
            text = this.mContext.getString(17040567);
        }
        event.getText().add(text);
        this.mAccessibilityManager.sendAccessibilityEvent(event);
    }

    public void invalidateAllKeys() {
        this.mDirtyRect.union(0, 0, getWidth(), getHeight());
        this.mDrawPending = true;
        invalidate();
    }

    public void invalidateKey(int keyIndex) {
        if (this.mKeys == null || keyIndex < 0 || keyIndex >= this.mKeys.length) {
            return;
        }
        Keyboard.Key key = this.mKeys[keyIndex];
        this.mInvalidatedKey = key;
        this.mDirtyRect.union(key.x + this.mPaddingLeft, key.y + this.mPaddingTop, key.x + key.width + this.mPaddingLeft, key.y + key.height + this.mPaddingTop);
        onBufferDraw();
        invalidate(key.x + this.mPaddingLeft, key.y + this.mPaddingTop, key.x + key.width + this.mPaddingLeft, key.y + key.height + this.mPaddingTop);
    }

    private boolean openPopupIfRequired(MotionEvent me) {
        if (this.mPopupLayout == 0 || this.mCurrentKey < 0 || this.mCurrentKey >= this.mKeys.length) {
            return false;
        }
        Keyboard.Key popupKey = this.mKeys[this.mCurrentKey];
        boolean result = onLongPress(popupKey);
        if (result) {
            this.mAbortKey = true;
            showPreview(-1);
        }
        return result;
    }

    protected boolean onLongPress(Keyboard.Key popupKey) {
        Keyboard keyboard;
        int popupKeyboardId = popupKey.popupResId;
        if (popupKeyboardId == 0) {
            return false;
        }
        this.mMiniKeyboardContainer = this.mMiniKeyboardCache.get(popupKey);
        if (this.mMiniKeyboardContainer == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mMiniKeyboardContainer = inflater.inflate(this.mPopupLayout, (ViewGroup) null);
            this.mMiniKeyboard = (KeyboardView) this.mMiniKeyboardContainer.findViewById(R.id.keyboardView);
            View closeButton = this.mMiniKeyboardContainer.findViewById(R.id.closeButton);
            if (closeButton != null) {
                closeButton.setOnClickListener(this);
            }
            this.mMiniKeyboard.setOnKeyboardActionListener(new OnKeyboardActionListener() {
                @Override
                public void onKey(int primaryCode, int[] keyCodes) {
                    KeyboardView.this.mKeyboardActionListener.onKey(primaryCode, keyCodes);
                    KeyboardView.this.dismissPopupKeyboard();
                }

                @Override
                public void onText(CharSequence text) {
                    KeyboardView.this.mKeyboardActionListener.onText(text);
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
                public void onPress(int primaryCode) {
                    KeyboardView.this.mKeyboardActionListener.onPress(primaryCode);
                }

                @Override
                public void onRelease(int primaryCode) {
                    KeyboardView.this.mKeyboardActionListener.onRelease(primaryCode);
                }
            });
            if (popupKey.popupCharacters != null) {
                keyboard = new Keyboard(getContext(), popupKeyboardId, popupKey.popupCharacters, -1, getPaddingRight() + getPaddingLeft());
            } else {
                keyboard = new Keyboard(getContext(), popupKeyboardId);
            }
            this.mMiniKeyboard.setKeyboard(keyboard);
            this.mMiniKeyboard.setPopupParent(this);
            this.mMiniKeyboardContainer.measure(View.MeasureSpec.makeMeasureSpec(getWidth(), Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(getHeight(), Integer.MIN_VALUE));
            this.mMiniKeyboardCache.put(popupKey, this.mMiniKeyboardContainer);
        } else {
            this.mMiniKeyboard = (KeyboardView) this.mMiniKeyboardContainer.findViewById(R.id.keyboardView);
        }
        getLocationInWindow(this.mCoordinates);
        this.mPopupX = popupKey.x + this.mPaddingLeft;
        this.mPopupY = popupKey.y + this.mPaddingTop;
        this.mPopupX = (this.mPopupX + popupKey.width) - this.mMiniKeyboardContainer.getMeasuredWidth();
        this.mPopupY -= this.mMiniKeyboardContainer.getMeasuredHeight();
        int x = this.mPopupX + this.mMiniKeyboardContainer.getPaddingRight() + this.mCoordinates[0];
        int y = this.mPopupY + this.mMiniKeyboardContainer.getPaddingBottom() + this.mCoordinates[1];
        this.mMiniKeyboard.setPopupOffset(x < 0 ? 0 : x, y);
        this.mMiniKeyboard.setShifted(isShifted());
        this.mPopupKeyboard.setContentView(this.mMiniKeyboardContainer);
        this.mPopupKeyboard.setWidth(this.mMiniKeyboardContainer.getMeasuredWidth());
        this.mPopupKeyboard.setHeight(this.mMiniKeyboardContainer.getMeasuredHeight());
        this.mPopupKeyboard.showAtLocation(this, 0, x, y);
        this.mMiniKeyboardOnScreen = true;
        invalidateAllKeys();
        return true;
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (!this.mAccessibilityManager.isTouchExplorationEnabled() || event.getPointerCount() != 1) {
            return true;
        }
        int action = event.getAction();
        switch (action) {
            case 7:
                event.setAction(2);
                break;
            case 9:
                event.setAction(0);
                break;
            case 10:
                event.setAction(1);
                break;
        }
        return onTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        boolean result;
        int pointerCount = me.getPointerCount();
        int action = me.getAction();
        long now = me.getEventTime();
        if (pointerCount != this.mOldPointerCount) {
            if (pointerCount == 1) {
                MotionEvent down = MotionEvent.obtain(now, now, 0, me.getX(), me.getY(), me.getMetaState());
                result = onModifiedTouchEvent(down, false);
                down.recycle();
                if (action == 1) {
                    result = onModifiedTouchEvent(me, true);
                }
            } else {
                MotionEvent up = MotionEvent.obtain(now, now, 1, this.mOldPointerX, this.mOldPointerY, me.getMetaState());
                result = onModifiedTouchEvent(up, true);
                up.recycle();
            }
        } else if (pointerCount == 1) {
            result = onModifiedTouchEvent(me, false);
            this.mOldPointerX = me.getX();
            this.mOldPointerY = me.getY();
        } else {
            result = true;
        }
        this.mOldPointerCount = pointerCount;
        return result;
    }

    private boolean onModifiedTouchEvent(MotionEvent me, boolean possiblePoly) {
        int touchX = ((int) me.getX()) - this.mPaddingLeft;
        int touchY = ((int) me.getY()) - this.mPaddingTop;
        if (touchY >= (-this.mVerticalCorrection)) {
            touchY += this.mVerticalCorrection;
        }
        int action = me.getAction();
        long eventTime = me.getEventTime();
        int keyIndex = getKeyIndices(touchX, touchY, null);
        this.mPossiblePoly = possiblePoly;
        if (action == 0) {
            this.mSwipeTracker.clear();
        }
        this.mSwipeTracker.addMovement(me);
        if (this.mAbortKey && action != 0 && action != 3) {
            return true;
        }
        if (this.mGestureDetector.onTouchEvent(me)) {
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
                this.mStartX = touchX;
                this.mStartY = touchY;
                this.mLastCodeX = touchX;
                this.mLastCodeY = touchY;
                this.mLastKeyTime = 0L;
                this.mCurrentKeyTime = 0L;
                this.mLastKey = -1;
                this.mCurrentKey = keyIndex;
                this.mDownKey = keyIndex;
                this.mDownTime = me.getEventTime();
                this.mLastMoveTime = this.mDownTime;
                checkMultiTap(eventTime, keyIndex);
                this.mKeyboardActionListener.onPress(keyIndex != -1 ? this.mKeys[keyIndex].codes[0] : 0);
                if (this.mCurrentKey >= 0 && this.mKeys[this.mCurrentKey].repeatable) {
                    this.mRepeatKeyIndex = this.mCurrentKey;
                    Message msg = this.mHandler.obtainMessage(3);
                    this.mHandler.sendMessageDelayed(msg, 400L);
                    repeatKey();
                    if (this.mAbortKey) {
                        this.mRepeatKeyIndex = -1;
                        break;
                    }
                } else {
                    if (this.mCurrentKey != -1) {
                        Message msg2 = this.mHandler.obtainMessage(4, me);
                        this.mHandler.sendMessageDelayed(msg2, LONGPRESS_TIMEOUT);
                    }
                    showPreview(keyIndex);
                    break;
                }
                break;
            case 1:
                removeMessages();
                if (keyIndex == this.mCurrentKey) {
                    this.mCurrentKeyTime += eventTime - this.mLastMoveTime;
                } else {
                    resetMultiTap();
                    this.mLastKey = this.mCurrentKey;
                    this.mLastKeyTime = (this.mCurrentKeyTime + eventTime) - this.mLastMoveTime;
                    this.mCurrentKey = keyIndex;
                    this.mCurrentKeyTime = 0L;
                }
                if (this.mCurrentKeyTime < this.mLastKeyTime && this.mCurrentKeyTime < 70 && this.mLastKey != -1) {
                    this.mCurrentKey = this.mLastKey;
                    touchX = this.mLastCodeX;
                    touchY = this.mLastCodeY;
                }
                showPreview(-1);
                Arrays.fill(this.mKeyIndices, -1);
                if (this.mRepeatKeyIndex == -1 && !this.mMiniKeyboardOnScreen && !this.mAbortKey) {
                    detectAndSendKey(this.mCurrentKey, touchX, touchY, eventTime);
                }
                invalidateKey(keyIndex);
                this.mRepeatKeyIndex = -1;
                break;
            case 2:
                boolean continueLongPress = false;
                if (keyIndex != -1) {
                    if (this.mCurrentKey == -1) {
                        this.mCurrentKey = keyIndex;
                        this.mCurrentKeyTime = eventTime - this.mDownTime;
                    } else if (keyIndex == this.mCurrentKey) {
                        this.mCurrentKeyTime += eventTime - this.mLastMoveTime;
                        continueLongPress = true;
                    } else if (this.mRepeatKeyIndex == -1) {
                        resetMultiTap();
                        this.mLastKey = this.mCurrentKey;
                        this.mLastCodeX = this.mLastX;
                        this.mLastCodeY = this.mLastY;
                        this.mLastKeyTime = (this.mCurrentKeyTime + eventTime) - this.mLastMoveTime;
                        this.mCurrentKey = keyIndex;
                        this.mCurrentKeyTime = 0L;
                    }
                }
                if (!continueLongPress) {
                    this.mHandler.removeMessages(4);
                    if (keyIndex != -1) {
                        Message msg3 = this.mHandler.obtainMessage(4, me);
                        this.mHandler.sendMessageDelayed(msg3, LONGPRESS_TIMEOUT);
                    }
                }
                showPreview(this.mCurrentKey);
                this.mLastMoveTime = eventTime;
                break;
            case 3:
                removeMessages();
                dismissPopupKeyboard();
                this.mAbortKey = true;
                showPreview(-1);
                invalidateKey(this.mCurrentKey);
                break;
        }
        this.mLastX = touchX;
        this.mLastY = touchY;
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
        if (this.mHandler == null) {
            return;
        }
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(1);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        closing();
    }

    private void dismissPopupKeyboard() {
        if (!this.mPopupKeyboard.isShowing()) {
            return;
        }
        this.mPopupKeyboard.dismiss();
        this.mMiniKeyboardOnScreen = false;
        invalidateAllKeys();
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

    private void checkMultiTap(long eventTime, int keyIndex) {
        if (keyIndex == -1) {
            return;
        }
        Keyboard.Key key = this.mKeys[keyIndex];
        if (key.codes.length <= 1) {
            if (eventTime <= this.mLastTapTime + 800 && keyIndex == this.mLastSentIndex) {
                return;
            }
            resetMultiTap();
            return;
        }
        this.mInMultiTap = true;
        if (eventTime < this.mLastTapTime + 800 && keyIndex == this.mLastSentIndex) {
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

        SwipeTracker(SwipeTracker swipeTracker) {
            this();
        }

        private SwipeTracker() {
            this.mPastX = new float[4];
            this.mPastY = new float[4];
            this.mPastTime = new long[4];
        }

        public void clear() {
            this.mPastTime[0] = 0;
        }

        public void addMovement(MotionEvent ev) {
            long time = ev.getEventTime();
            int N = ev.getHistorySize();
            for (int i = 0; i < N; i++) {
                addPoint(ev.getHistoricalX(i), ev.getHistoricalY(i), ev.getHistoricalEventTime(i));
            }
            addPoint(ev.getX(), ev.getY(), time);
        }

        private void addPoint(float x, float y, long time) {
            int drop = -1;
            long[] pastTime = this.mPastTime;
            int i = 0;
            while (i < 4 && pastTime[i] != 0) {
                if (pastTime[i] < time - 200) {
                    drop = i;
                }
                i++;
            }
            if (i == 4 && drop < 0) {
                drop = 0;
            }
            if (drop == i) {
                drop--;
            }
            float[] pastX = this.mPastX;
            float[] pastY = this.mPastY;
            if (drop >= 0) {
                int start = drop + 1;
                int count = (4 - drop) - 1;
                System.arraycopy(pastX, start, pastX, 0, count);
                System.arraycopy(pastY, start, pastY, 0, count);
                System.arraycopy(pastTime, start, pastTime, 0, count);
                i -= drop + 1;
            }
            pastX[i] = x;
            pastY[i] = y;
            pastTime[i] = time;
            int i2 = i + 1;
            if (i2 >= 4) {
                return;
            }
            pastTime[i2] = 0;
        }

        public void computeCurrentVelocity(int units) {
            computeCurrentVelocity(units, Float.MAX_VALUE);
        }

        public void computeCurrentVelocity(int units, float maxVelocity) {
            float[] pastX = this.mPastX;
            float[] pastY = this.mPastY;
            long[] pastTime = this.mPastTime;
            float oldestX = pastX[0];
            float oldestY = pastY[0];
            long oldestTime = pastTime[0];
            float accumX = 0.0f;
            float accumY = 0.0f;
            int N = 0;
            while (N < 4 && pastTime[N] != 0) {
                N++;
            }
            for (int i = 1; i < N; i++) {
                int dur = (int) (pastTime[i] - oldestTime);
                if (dur != 0) {
                    float dist = pastX[i] - oldestX;
                    float vel = (dist / dur) * units;
                    accumX = accumX == 0.0f ? vel : (accumX + vel) * 0.5f;
                    float dist2 = pastY[i] - oldestY;
                    float vel2 = (dist2 / dur) * units;
                    accumY = accumY == 0.0f ? vel2 : (accumY + vel2) * 0.5f;
                }
            }
            this.mXVelocity = accumX < 0.0f ? Math.max(accumX, -maxVelocity) : Math.min(accumX, maxVelocity);
            this.mYVelocity = accumY < 0.0f ? Math.max(accumY, -maxVelocity) : Math.min(accumY, maxVelocity);
        }

        public float getXVelocity() {
            return this.mXVelocity;
        }

        public float getYVelocity() {
            return this.mYVelocity;
        }
    }
}

package jp.co.omronsoft.iwnnime.ml;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import java.util.List;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.KeyboardView;

public class FlickKeyboardView extends KeyboardView {
    private static final int ALPHA_NUM_MODE = 1;
    private static final int KANA_MODE = 0;
    private float mDrawableHeight;
    private float mDrawableWidth;
    private boolean mFlickDetectMode;
    private int mFlickDivisionNumber;
    private RectF mFlickIgnoreAreaAroundTap;
    private OnFlickKeyboardActionListener mFlickKeyboardActionListener;
    private RectF mFlickNeutralArea;
    private float mFlickSensitivity;
    private int mFlickedKey;
    private boolean mHasMoved;
    private int mMiniKeyboardOffsetX;
    private int mMiniKeyboardOffsetY;
    private float mMinimumMoveEventDistance;
    private int mModeCycleCount;
    private int[] mOffsetInWindow;
    private int mPrePopupBottom;
    private int mPrePopupHeight;
    private int mPrePopupWidth;
    private int mPressX;
    private int mPressY;
    private PopupWindow mPreviewPopup;
    private TextView mPreviewText;
    private float mPreviewTextSize;
    private boolean mShowPreview;
    private boolean mSlideModeStart;
    private static final float FLICK_GUIDE_HINT_TEXT_RATE = 0.75f;
    private static final float[] FLICK_GUIDE_TEXT_RATE_TABLE = {0.65f, FLICK_GUIDE_HINT_TEXT_RATE};
    private static final float[] FLICK_GUIDE_PADDING_RATE_TABLE = {0.125f, 0.0f};
    private static final int[] FLICK_LONG_PRESSABLE_STATE_SET = {android.R.attr.state_long_pressable};

    public FlickKeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlickKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mShowPreview = false;
        this.mPreviewPopup = null;
        this.mPrePopupHeight = 0;
        this.mPrePopupWidth = 0;
        this.mPrePopupBottom = 0;
        this.mDrawableHeight = 0.0f;
        this.mDrawableWidth = 0.0f;
        this.mFlickDetectMode = false;
        this.mFlickedKey = -1;
        this.mHasMoved = false;
        this.mFlickDetectMode = false;
        this.mSlideModeStart = false;
        this.mFlickSensitivity = context.getResources().getInteger(R.integer.flick_sensitivity_preference_default) / 100.0f;
        this.mFlickDivisionNumber = context.getResources().getInteger(R.integer.flick_sensitivity_division_number);
        this.mMinimumMoveEventDistance = context.getResources().getDimensionPixelSize(R.dimen.minimum_move_event_distance);
    }

    public void initPopupView(IWnnIME parent) {
        LayoutInflater inflate = parent.getLayoutInflater();
        this.mPreviewPopup = new PopupWindow(parent);
        this.mPreviewText = (TextView) inflate.inflate(R.layout.keyboard_key_preview, (ViewGroup) null);
        this.mPreviewTextSize = this.mPreviewText.getTextSize();
        this.mPreviewPopup.setContentView(this.mPreviewText);
        this.mPreviewPopup.setBackgroundDrawable(null);
        this.mPreviewPopup.setTouchable(false);
        this.mPopupParent = this;
        createSlidePopup(parent);
    }

    @Override
    public void setPreviewEnabled(boolean previewEnabled) {
        super.setPreviewEnabled(previewEnabled);
        this.mShowPreview = previewEnabled;
    }

    @Override
    protected boolean isPreviewEnabled() {
        return this.mShowPreview;
    }

    @Override
    public void setPopupOffset(int x, int y) {
        super.setPopupOffset(x, y);
        this.mMiniKeyboardOffsetX = x;
        this.mMiniKeyboardOffsetY = y;
        if (this.mPreviewPopup.isShowing()) {
            this.mPreviewPopup.dismiss();
        }
    }

    public void setOnKeyboardActionListener(OnFlickKeyboardActionListener listener) {
        super.setOnKeyboardActionListener((KeyboardView.OnKeyboardActionListener) listener);
        this.mFlickKeyboardActionListener = listener;
    }

    public void setFlickDetectMode(boolean flick, int keycode) {
        this.mFlickDetectMode = flick;
        this.mFlickedKey = -1;
        this.mSlideModeStart = false;
        if (flick) {
            int x = this.mPressX - getPaddingLeft();
            int y = (this.mPressY + this.mVerticalCorrection) - getPaddingTop();
            List<Keyboard.Key> keys = this.mKeyboard.getKeys();
            int positionY1stRow = keys.get(0).y;
            if (y < positionY1stRow) {
                y = positionY1stRow;
            }
            int[] keyidx = this.mKeyboard.getNearestKeys(x, y);
            if (keyidx.length > 0) {
                int keyCount = keyidx.length;
                int i = 0;
                while (i < keyCount) {
                    Keyboard.Key key = keys.get(keyidx[i]);
                    if (key.codes[0] == keycode) {
                        break;
                    } else {
                        i++;
                    }
                }
                if (i >= keyCount) {
                    i = 0;
                }
                this.mFlickedKey = keyidx[i];
                if (super.isPreviewEnabled()) {
                    this.mShowPreview = true;
                    super.setPreviewEnabled(false);
                }
                if (this.mPreviewPopup.isShowing()) {
                    this.mPreviewPopup.dismiss();
                }
                flick(0);
                return;
            }
            this.mFlickDetectMode = false;
            return;
        }
        this.mPreviewPopup.dismiss();
        if (this.mShowPreview) {
            super.setPreviewEnabled(true);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (WnnAccessibility.isAccessibility(IWnnIME.getCurrentIme())) {
            return true;
        }
        return handleTouchEvent(ev);
    }

    @Override
    public boolean handleTouchEvent(MotionEvent ev) {
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn == null || this.mIgnoreTouchEvent || ev.getPointerId(ev.getActionIndex()) > 0 || !isShown()) {
            return true;
        }
        switch (ev.getActionMasked()) {
            case 0:
            case 5:
                this.mTouchKeyCode = -1;
                wnn.cancelToast();
                this.mHasMoved = false;
                if (this.mMiniKeyboardOnScreen) {
                    dismissPopupKeyboard();
                    return true;
                }
                this.mPressX = (int) ev.getX();
                this.mPressY = (int) ev.getY();
                Keyboard.Key touchKey = getKey(this.mPressX, this.mPressY);
                if (touchKey != null) {
                    if (touchKey.repeatable) {
                        this.mTouchKeyCode = touchKey.codes[0];
                    }
                    int touchCode = touchKey.codes[0];
                    this.mFlickKeyboardActionListener.onPress(touchCode);
                    if (touchCode == -412) {
                        String undoKey = getContext().getResources().getString(R.string.ti_key_12key_undo_txt);
                        if (touchKey.label != null && touchKey.label.equals(undoKey)) {
                            touchKey.popupResId = 0;
                        } else {
                            touchKey.popupResId = 1;
                        }
                    }
                    if (this.mFlickDetectMode) {
                        float pressX = ev.getX();
                        float pressY = ev.getY();
                        Keyboard.Key flickedKey = this.mKeyboard.getKeys().get(this.mFlickedKey);
                        int flickedKeyXpos = (flickedKey.x + getPaddingLeft()) - flickedKey.gap;
                        int flickedKeyYpos = ((flickedKey.y + getPaddingTop()) - flickedKey.vgap) - this.mVerticalCorrection;
                        float dividedWidth = (flickedKey.width + flickedKey.gap) / this.mFlickDivisionNumber;
                        float dividedHeight = (flickedKey.height + flickedKey.vgap) / this.mFlickDivisionNumber;
                        if (flickedKey.y < flickedKey.height) {
                            flickedKeyYpos = 0;
                            dividedHeight = (((getPaddingTop() + flickedKey.y) + flickedKey.height) - this.mVerticalCorrection) / this.mFlickDivisionNumber;
                        }
                        int divisionIndexX = (int) ((pressX - flickedKeyXpos) / dividedWidth);
                        int divisionIndexY = (int) ((pressY - flickedKeyYpos) / dividedHeight);
                        float neutralLeft = flickedKeyXpos + (divisionIndexX * dividedWidth);
                        float neutralTop = flickedKeyYpos + (divisionIndexY * dividedHeight);
                        float additionalWidth = dividedWidth * this.mFlickSensitivity;
                        float additionalHeight = dividedHeight * this.mFlickSensitivity;
                        this.mFlickNeutralArea = new RectF(neutralLeft - additionalWidth, neutralTop - additionalHeight, neutralLeft + dividedWidth + additionalWidth, neutralTop + dividedHeight + additionalHeight);
                        this.mFlickIgnoreAreaAroundTap = new RectF(pressX - this.mMinimumMoveEventDistance, pressY - this.mMinimumMoveEventDistance, this.mMinimumMoveEventDistance + pressX, this.mMinimumMoveEventDistance + pressY);
                    }
                }
                break;
                break;
            case 1:
            case 6:
                Keyboard.Key currentKey = getKey((int) ev.getX(), (int) ev.getY());
                if (this.mTouchKeyCode != -1 && (currentKey == null || this.mTouchKeyCode != currentKey.codes[0])) {
                    MotionEvent motionEvent = MotionEvent.obtain(ev.getDownTime(), ev.getEventTime(), 3, ev.getX(), ev.getY(), ev.getMetaState());
                    boolean zHandleTouchEvent = super.handleTouchEvent(motionEvent);
                    motionEvent.recycle();
                    return zHandleTouchEvent;
                }
                if (this.mPreviewPopup.isShowing()) {
                    this.mPreviewPopup.dismiss();
                    if (currentKey != null) {
                        currentKey.previewPosX = -1;
                    }
                }
                break;
                break;
            case 2:
                Keyboard.Key currentKey2 = getKey((int) ev.getX(), (int) ev.getY());
                if (this.mTouchKeyCode != -1 && (currentKey2 == null || this.mTouchKeyCode != currentKey2.codes[0])) {
                    MotionEvent motionEvent2 = MotionEvent.obtain(ev.getDownTime(), ev.getEventTime(), 3, ev.getX(), ev.getY(), ev.getMetaState());
                    boolean zHandleTouchEvent2 = super.handleTouchEvent(motionEvent2);
                    motionEvent2.recycle();
                    return zHandleTouchEvent2;
                }
                if (this.mFlickDetectMode) {
                    if (!this.mHasMoved) {
                        if (this.mFlickIgnoreAreaAroundTap.contains(ev.getX(), ev.getY())) {
                            return true;
                        }
                        this.mHasMoved = true;
                    }
                    float getX = ev.getX();
                    float getY = ev.getY();
                    Keyboard.Key flickedkey = this.mKeyboard.getKeys().get(this.mFlickedKey);
                    int code = flickedkey.codes[0];
                    if (code == -114) {
                        Resources res = wnn.getResources();
                        DisplayMetrics dm = res.getDisplayMetrics();
                        int slideWidth = Math.round(res.getFraction(R.fraction.keyboard_qwerty_mode_change_keywidth, dm.widthPixels, dm.widthPixels));
                        int rightx = flickedkey.previewPosX + getPaddingLeft() + slideWidth;
                        this.mSlidePopupWidth = (this.mModeCycleCount + 1) * slideWidth;
                        if (getX < getWidth() - 1) {
                            if (getX > rightx) {
                                int index = (int) (((getX - rightx) / slideWidth) + 1.0f);
                                if (index > this.mModeCycleCount) {
                                    index = this.mModeCycleCount;
                                }
                                int direction = index + 4;
                                if (!this.mSlideModeStart) {
                                    this.mPreviewPopup.dismiss();
                                }
                                this.mSlideModeStart = true;
                                flick(direction);
                                clearSlidePopupFocused();
                                setSlidePopupFocused(wnn, index);
                            } else if (this.mSlideModeStart) {
                                flick(10);
                                clearSlidePopupFocused();
                            } else {
                                flick(9);
                            }
                        }
                        return true;
                    }
                    if (this.mFlickNeutralArea.contains(getX, getY)) {
                        flick(0);
                        return true;
                    }
                    float absX = Math.abs(getX - this.mFlickNeutralArea.centerX());
                    float absY = Math.abs(getY - this.mFlickNeutralArea.centerY());
                    float keyProportion = this.mFlickNeutralArea.width() / this.mFlickNeutralArea.height();
                    if (absX > absY * keyProportion) {
                        if (getX >= this.mFlickNeutralArea.right) {
                            flick(1);
                        } else {
                            flick(-1);
                        }
                    } else if (getY >= this.mFlickNeutralArea.bottom) {
                        flick(-2);
                    } else {
                        flick(2);
                    }
                    return true;
                }
                break;
        }
        return super.handleTouchEvent(ev);
    }

    public void flick(int direction) {
        this.mFlickKeyboardActionListener.onFlick(this.mFlickedKey, direction);
    }

    public void setSlidePopup(int[] enableKeyMode) {
        if (this.mShowPreview) {
            setSlidePopupDisplayList(enableKeyMode);
            showSlidePopup();
        }
    }

    public void setFlickedKeyTop(CharSequence label, boolean flickable) {
        Keyboard.Key key;
        if (this.mShowPreview) {
            List<Keyboard.Key> keys = this.mKeyboard.getKeys();
            if (this.mFlickedKey >= 0 && keys.size() > this.mFlickedKey && (key = keys.get(this.mFlickedKey)) != null) {
                Drawable previewDrawable = null;
                if (label != null) {
                    previewDrawable = getFlickedKeyTop(key, label);
                }
                showFlickPopup(previewDrawable, flickable, false);
            }
        }
    }

    private Drawable getFlickedKeyTop(Keyboard.Key key, CharSequence label) {
        Resources res = getResources();
        if (res == null) {
            return null;
        }
        float textSize = getPreviewTextSize();
        float width = this.mDrawableWidth;
        if (width < 1.0f) {
            width = 1.0f;
        }
        float height = this.mDrawableHeight;
        if (height < 1.0f) {
            height = 1.0f;
        }
        Bitmap bitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(width / 2.0f, height / 2.0f);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize);
        int color = getKeyPreviewColor(key);
        paint.setColor(color);
        float offset = (paint.getTextSize() - paint.descent()) / 2.0f;
        canvas.drawText(label.toString(), 0.0f, offset, paint);
        Drawable flickGuide = new BitmapDrawable(res, bitmap);
        flickGuide.setBounds(0, 0, (int) width, (int) height);
        return flickGuide;
    }

    public void setFlickedKeyGuide(boolean flickable) {
        Keyboard.Key key;
        if (this.mShowPreview) {
            List<Keyboard.Key> keys = this.mKeyboard.getKeys();
            if (this.mFlickedKey >= 0 && keys.size() > this.mFlickedKey && (key = keys.get(this.mFlickedKey)) != null) {
                Drawable previewDrawable = getFlickedKeyGuide(key);
                showFlickPopup(previewDrawable, flickable, true);
            }
        }
    }

    private Drawable getFlickedKeyGuide(Keyboard.Key key) {
        Resources res;
        IWnnIME wnn;
        DefaultSoftKeyboard defaultSoftKeyboard;
        String[] flickTable;
        if (this.mKeyboard == null || (res = getResources()) == null || (wnn = IWnnIME.getCurrentIme()) == null || (defaultSoftKeyboard = wnn.getCurrentDefaultSoftKeyboard()) == null || (flickTable = defaultSoftKeyboard.getTableForFlickGuide()) == null) {
            return null;
        }
        boolean isKanaMode = DefaultSoftKeyboard.isKanaMode(this.mKeyboard.getKeyboardMode());
        int index = 1;
        if (isKanaMode) {
            index = 0;
        }
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(getKeyPreviewColor(key));
        paint.setTextSize(getPreviewTextSize());
        float baseTextSize = paint.getTextSize();
        float paddingY = baseTextSize / 2.0f;
        float gap = baseTextSize * FLICK_GUIDE_PADDING_RATE_TABLE[index];
        float textSize = baseTextSize * FLICK_GUIDE_TEXT_RATE_TABLE[index];
        paint.setTextSize(textSize);
        Paint hintPaint = new Paint(paint);
        float hintTextSize = textSize * FLICK_GUIDE_HINT_TEXT_RATE;
        hintPaint.setTextSize(hintTextSize);
        float centerY = (textSize - paint.descent()) / 2.0f;
        float hintCenterY = (hintTextSize - hintPaint.descent()) / 2.0f;
        float width = (2.0f * hintTextSize) + textSize + (2.0f * gap);
        float padding = (key.contentWidth - width) / 2.0f;
        if (padding > 0.0f) {
            width = key.contentWidth;
            if (padding < paddingY) {
                paddingY = padding;
            }
        }
        float height = width + paddingY;
        this.mDrawableHeight = height;
        this.mDrawableWidth = width;
        if (key.mIsPreviewSkin) {
            return null;
        }
        if (width < 1.0f) {
            width = 1.0f;
        }
        if (height < 1.0f) {
            height = 1.0f;
        }
        Bitmap bitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(width / 2.0f, height / 2.0f);
        if (flickTable.length == 1) {
            if (!draw2line(canvas, key, flickTable[0], paint, null)) {
                return null;
            }
        } else {
            float rightPosX = (textSize / 2.0f) + gap + (hintTextSize / 2.0f);
            float leftPosX = -rightPosX;
            float upPosY = -(centerY + gap + hintPaint.descent());
            float downPosY = centerY + gap + hintTextSize;
            for (int flickIndex = 0; flickIndex < flickTable.length; flickIndex++) {
                String drawChar = flickTable[flickIndex];
                if (drawChar != null) {
                    String drawChar2 = drawChar.toUpperCase();
                    switch (flickIndex) {
                        case 0:
                            canvas.drawText(drawChar2, 0.0f, centerY, paint);
                            break;
                        case 1:
                            canvas.drawText(drawChar2, leftPosX, hintCenterY, hintPaint);
                            break;
                        case 2:
                            canvas.drawText(drawChar2, 0.0f, upPosY, hintPaint);
                            break;
                        case 3:
                            canvas.drawText(drawChar2, rightPosX, hintCenterY, hintPaint);
                            break;
                        case 4:
                            canvas.drawText(drawChar2, 0.0f, downPosY, hintPaint);
                            break;
                    }
                }
            }
        }
        Drawable flickGuide = new BitmapDrawable(res, bitmap);
        flickGuide.setBounds(0, 0, (int) width, (int) height);
        return flickGuide;
    }

    private float getPreviewTextSize() {
        return this.mPreviewTextSize * this.mTextScaleRate;
    }

    private void showFlickPopup(Drawable image, boolean flickable, boolean isGuide) {
        List<Keyboard.Key> keys;
        Drawable previewDrawable;
        int[] state;
        if (this.mKeyboard != null && this.mPreviewText != null && this.mPreviewPopup != null && (keys = this.mKeyboard.getKeys()) != null && this.mFlickedKey >= 0 && keys.size() > this.mFlickedKey) {
            PopupWindow previewPopup = this.mPreviewPopup;
            Keyboard.Key key = keys.get(this.mFlickedKey);
            if (key != null) {
                if (WnnUtility.isFunctionKey(key)) {
                    previewPopup.dismiss();
                    return;
                }
                if (image != null) {
                    previewDrawable = image;
                } else if (key.iconPreview != null) {
                    previewDrawable = key.iconPreview;
                } else {
                    previewDrawable = key.icon;
                }
                if (previewDrawable != null) {
                    TextView previewText = this.mPreviewText;
                    setKeyPreviewBackground(previewText, key);
                    previewDrawable.clearColorFilter();
                    if (!key.mIsPreviewSkin) {
                        int color = getKeyPreviewColor(key);
                        previewDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    }
                    previewText.setCompoundDrawables(null, null, null, previewDrawable);
                    previewText.setText((CharSequence) null);
                    previewText.setPressed(flickable);
                    int popupHeight = this.mPrePopupHeight;
                    int popupWidth = this.mPrePopupWidth;
                    int bottom = this.mPrePopupBottom;
                    if (isGuide) {
                        int height = (int) (key.height * 1.5f);
                        int width = key.width;
                        int drawableHeight = previewDrawable.getMinimumHeight();
                        int drawableWidth = previewDrawable.getMinimumWidth() + previewText.getPaddingLeft() + previewText.getPaddingRight();
                        popupHeight = Math.max(height, drawableHeight);
                        this.mPrePopupHeight = popupHeight;
                        popupWidth = Math.max(width, drawableWidth);
                        this.mPrePopupWidth = popupWidth;
                        bottom = popupHeight - drawableHeight;
                        this.mPrePopupBottom = bottom;
                    }
                    previewText.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
                    ViewGroup.LayoutParams lp = previewText.getLayoutParams();
                    if (lp != null) {
                        lp.width = popupWidth;
                        lp.height = popupHeight;
                    } else {
                        previewText.setLayoutParams(new ViewGroup.LayoutParams(popupWidth, popupHeight));
                    }
                    if (key.previewPosX == -1) {
                        key.previewPosX = (key.x + (key.width / 2)) - (popupWidth / 2);
                    }
                    int positionY = (key.y - popupHeight) + this.mPreviewOffset + 20;
                    setOffsetInWindow();
                    int tempPreviewPosBottom = positionY + popupHeight;
                    int keyPosBottom = key.y + key.height;
                    int previewKeyGap = keyPosBottom - tempPreviewPosBottom;
                    int popupHeight2 = popupHeight + previewKeyGap;
                    previewText.setPadding(previewText.getPaddingLeft(), previewText.getPaddingTop(), previewText.getPaddingRight(), bottom + previewKeyGap);
                    if (key.popupResId == 0) {
                        state = EMPTY_STATE_SET;
                    } else {
                        state = FLICK_LONG_PRESSABLE_STATE_SET;
                    }
                    previewText.getBackground().setState(state);
                    previewPopup.setContentView(previewText);
                    showPopup(key.previewPosX, positionY, popupHeight2, popupWidth);
                }
            }
        }
    }

    private void showSlidePopup() {
        List<Keyboard.Key> keys = this.mKeyboard.getKeys();
        if (this.mFlickedKey >= 0 && keys.size() > this.mFlickedKey) {
            PopupWindow previewPopup = this.mPreviewPopup;
            Keyboard.Key key = keys.get(this.mFlickedKey);
            int positionX = key.previewPosX;
            int positionY = (key.y - this.mPreviewHeight) + this.mPreviewOffset;
            setOffsetInWindow();
            previewPopup.setContentView(this.mSlidePopupLayout);
            showPopup(positionX, positionY + 20, this.mPreviewHeight, this.mSlidePopupWidth);
        }
    }

    private void setOffsetInWindow() {
        if (this.mOffsetInWindow == null) {
            this.mOffsetInWindow = new int[2];
        }
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn != null) {
            KeyboardManager km = wnn.getCurrentKeyboardManager();
            Point pos = km.getKeyboardPosition();
            this.mOffsetInWindow[0] = pos.x + this.mMiniKeyboardOffsetX + getPaddingLeft();
            this.mOffsetInWindow[1] = pos.y + this.mMiniKeyboardOffsetY + getPaddingTop();
        }
    }

    private void showPopup(int positionX, int positionY, int popupHeight, int popupWidth) {
        PopupWindow previewPopup = this.mPreviewPopup;
        if (previewPopup.isShowing()) {
            previewPopup.update(this.mOffsetInWindow[0] + positionX, this.mOffsetInWindow[1] + positionY, popupWidth, popupHeight, true);
            return;
        }
        previewPopup.setWidth(popupWidth);
        previewPopup.setHeight(popupHeight);
        previewPopup.showAtLocation(this.mPopupParent, 0, this.mOffsetInWindow[0] + positionX, this.mOffsetInWindow[1] + positionY);
        WnnUtility.addFlagsForPopupWindow(IWnnIME.getCurrentIme(), previewPopup);
    }

    @Override
    protected boolean onLongPress(Keyboard.Key key, MotionEvent me) {
        if (this.mSlideModeStart || this.mIsInputTypeNull) {
            return false;
        }
        if (this.mFlickKeyboardActionListener.onLongPress(key)) {
            setFlickDetectMode(false, 0);
            return true;
        }
        return super.onLongPress(key, me);
    }

    public void setFlickSensitivity(int thres) {
        this.mFlickSensitivity = thres / 100.0f;
    }

    @Override
    public void closing() {
        super.closing();
        if (this.mPreviewPopup != null && this.mPreviewPopup.isShowing()) {
            this.mPreviewPopup.dismiss();
        }
    }

    public void clearFlickedKeyTop() {
        this.mPreviewPopup.dismiss();
    }

    public void setModeCycleCount(int count) {
        this.mModeCycleCount = count;
    }

    @Override
    public Keyboard.Key getKey(int positionX, int positionY) {
        Keyboard.Key positionKey = null;
        int keyboardX = positionX - getPaddingLeft();
        int keyboardY = (this.mVerticalCorrection + positionY) - getPaddingTop();
        List<Keyboard.Key> keys = this.mKeyboard.getKeys();
        int positionY1stRow = keys.get(0).y;
        if (keyboardY < positionY1stRow) {
            keyboardY = positionY1stRow;
        }
        int[] keyIndices = this.mKeyboard.getNearestKeys(keyboardX, keyboardY);
        int keyCount = keyIndices.length;
        if (keyCount > 0) {
            for (int i : keyIndices) {
                Keyboard.Key key = keys.get(i);
                if (key.isInside(keyboardX, keyboardY)) {
                    positionKey = key;
                }
            }
        }
        return positionKey;
    }

    @Override
    public void clearWindowInfo() {
        super.clearWindowInfo();
        this.mOffsetInWindow = null;
    }
}

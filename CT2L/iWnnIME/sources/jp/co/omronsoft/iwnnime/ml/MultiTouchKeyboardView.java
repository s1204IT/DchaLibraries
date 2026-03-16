package jp.co.omronsoft.iwnnime.ml;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.KeyboardView;
import jp.co.omronsoft.iwnnime.ml.candidate.CandidatesManager;

public class MultiTouchKeyboardView extends KeyboardView {
    private static final int MULTI_TOUCH_MAX = 10;
    private static final int MULTI_TOUCH_STOP_RANGE = 80;
    private int TEMPORARILY_ID_NONE;
    private boolean mCaps;
    private OnFlickKeyboardActionListener mFlickKeyboardActionListener;
    private ArrayList<Integer> mIgnoreTouchId;
    private boolean mIsRepeatKeyTouch;
    private boolean mIsTemporarilyInputed;
    private Keyboard mNormalKeyboard;
    private Keyboard mShiftKeyboard;
    private boolean mShifted;
    private int mTemporarilyId;
    private int mTemporarilyKeycode;
    private LinkedList<TouchPoint> mTouchPoints;

    static class TouchPoint {
        public int mId;
        public int mMetaState;
        public float mX;
        public float mY;

        public TouchPoint(int id, float x, float y, int metaState) {
            this.mId = id;
            this.mX = x;
            this.mY = y;
            this.mMetaState = metaState;
        }

        public TouchPoint(int index, MotionEvent event) {
            this.mId = event.getPointerId(index);
            this.mX = event.getX(index);
            this.mY = event.getY(index);
            this.mMetaState = event.getMetaState();
        }
    }

    public MultiTouchKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.TEMPORARILY_ID_NONE = -1;
        this.mTouchPoints = new LinkedList<>();
        this.mIgnoreTouchId = new ArrayList<>();
        this.mIsRepeatKeyTouch = false;
        this.mShiftKeyboard = null;
        this.mNormalKeyboard = null;
        this.mTemporarilyId = this.TEMPORARILY_ID_NONE;
        this.mIsTemporarilyInputed = false;
        this.mTemporarilyKeycode = 0;
        this.mShifted = false;
        this.mCaps = false;
    }

    public MultiTouchKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.TEMPORARILY_ID_NONE = -1;
        this.mTouchPoints = new LinkedList<>();
        this.mIgnoreTouchId = new ArrayList<>();
        this.mIsRepeatKeyTouch = false;
        this.mShiftKeyboard = null;
        this.mNormalKeyboard = null;
        this.mTemporarilyId = this.TEMPORARILY_ID_NONE;
        this.mIsTemporarilyInputed = false;
        this.mTemporarilyKeycode = 0;
        this.mShifted = false;
        this.mCaps = false;
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
        if (wnn == null) {
            return true;
        }
        CandidatesManager candidateManager = wnn.getCurrentCandidatesManager();
        if (candidateManager.isReadMoreButtonPressed()) {
            return true;
        }
        KeyboardManager km = wnn.getCurrentKeyboardManager();
        if (km.isProcessingKeyboardMenu() || this.mIgnoreTouchEvent || !isShown()) {
            return true;
        }
        int action = ev.getActionMasked();
        switch (action) {
            case 0:
            case 5:
                wnn.cancelToast();
                break;
        }
        return true;
    }

    private boolean onTouchEventDown(MotionEvent event) {
        long time = event.getEventTime();
        int index = event.getActionIndex();
        int id = event.getPointerId(index);
        this.mTouchKeyCode = -1;
        if (this.mMiniKeyboardOnScreen) {
            dismissPopupKeyboard();
            return true;
        }
        if (index >= 10) {
            return true;
        }
        this.mIgnoreTouchId.remove(Integer.valueOf(id));
        TouchPoint point = new TouchPoint(index, event);
        Keyboard.Key key = getKey((int) point.mX, (int) point.mY);
        if (key != null) {
            if (this.mFlickKeyboardActionListener != null) {
                this.mFlickKeyboardActionListener.onPress(key.codes[0]);
            }
            if (key.repeatable) {
                this.mTouchKeyCode = key.codes[0];
                this.mIsRepeatKeyTouch = true;
                executeQueueTouchEvent(event, -1);
            } else if (!this.mShifted && key.codes[0] == -1) {
                if (this.mTemporarilyId != this.TEMPORARILY_ID_NONE) {
                    setIgnoreTouchId(this.mTemporarilyId);
                    this.mTemporarilyId = this.TEMPORARILY_ID_NONE;
                }
                executeQueueTouchEvent(event, -1);
                if (!this.mShifted) {
                    this.mTouchPoints.add(point);
                    executeQueueTouchEvent(event, -1);
                    this.mIgnoreTouchId.remove(Integer.valueOf(id));
                }
                this.mTemporarilyId = id;
                this.mIsTemporarilyInputed = false;
                this.mTemporarilyKeycode = key.codes[0];
                return executeTouchAction(time, 0, point);
            }
            this.mTouchPoints.add(point);
            if (!this.mIsRepeatKeyTouch || key.repeatable) {
                return executeTouchAction(time, 0, point);
            }
            return true;
        }
        setIgnoreTouchId(id);
        return true;
    }

    private boolean onTouchEventMove(MotionEvent event) {
        long time = event.getEventTime();
        int lastIndex = event.getPointerCount() - 1;
        if (lastIndex >= 10) {
            lastIndex = 9;
        }
        int lastId = event.getPointerId(lastIndex);
        Keyboard.Key currentKey = getKey((int) event.getX(), (int) event.getY());
        if (this.mTouchKeyCode != -1 && (currentKey == null || this.mTouchKeyCode != currentKey.codes[0])) {
            cancelTouchEvent();
        }
        if (lastIndex == 0 && lastId == this.mTemporarilyId) {
            float y = event.getY(lastIndex);
            if (y >= 0.0f) {
                return executeTouchAction(time, 2, new TouchPoint(lastIndex, event));
            }
        } else {
            int size = this.mTouchPoints.size();
            if (size > 0) {
                lastId = this.mTouchPoints.get(size - 1).mId;
                lastIndex = event.findPointerIndex(lastId);
                for (int i = 0; i < size; i++) {
                    TouchPoint point = this.mTouchPoints.get(i);
                    int pointerIndex = event.findPointerIndex(point.mId);
                    if (pointerIndex >= 0 && pointerIndex != lastIndex && (Math.abs(event.getX(pointerIndex) - point.mX) > 80.0f || Math.abs(event.getY(pointerIndex) - point.mY) > 80.0f)) {
                        setIgnoreTouchId(point.mId);
                        this.mTouchPoints.remove(i);
                        size = this.mTouchPoints.size();
                    }
                }
            }
            if (!this.mIgnoreTouchId.contains(Integer.valueOf(lastId)) && size != 0) {
                float y2 = event.getY(lastIndex);
                if (y2 >= 0.0f) {
                    TouchPoint point2 = new TouchPoint(lastIndex, event);
                    this.mTouchPoints.removeLast();
                    this.mTouchPoints.add(point2);
                    return executeTouchAction(time, 2, point2);
                }
            }
        }
        return true;
    }

    private boolean onTouchEventUp(MotionEvent event) {
        long time = event.getEventTime();
        int index = event.getActionIndex();
        int id = event.getPointerId(index);
        Keyboard.Key currentKey = getKey((int) event.getX(), (int) event.getY());
        if (this.mTouchKeyCode != -1 && (currentKey == null || this.mTouchKeyCode != currentKey.codes[0])) {
            cancelTouchEvent();
        }
        if (index >= 10) {
            return true;
        }
        if (id == this.mTemporarilyId) {
            this.mTemporarilyId = this.TEMPORARILY_ID_NONE;
            executeQueueTouchEvent(event, -1);
            TouchPoint point = new TouchPoint(index, event);
            boolean result = executeTouchAction(time, 3, point);
            if (this.mIsTemporarilyInputed) {
                switch (this.mTemporarilyKeycode) {
                    case -1:
                        setShifted(false);
                        break;
                }
                return result;
            }
            return result;
        }
        if (this.mIgnoreTouchId.contains(Integer.valueOf(id))) {
            return true;
        }
        return executeQueueTouchEvent(event, id);
    }

    private boolean executeQueueTouchEvent(MotionEvent event, int stopId) {
        long time = event.getEventTime();
        boolean result = true;
        TouchPoint point = this.mTouchPoints.poll();
        while (point != null) {
            Keyboard.Key pointKey = getKey((int) point.mX, (int) point.mY);
            if (pointKey != null && pointKey.repeatable) {
                this.mIsRepeatKeyTouch = false;
            } else {
                executeTouchAction(time, 0, point);
            }
            result = executeTouchAction(time, 1, point);
            if (stopId == point.mId) {
                break;
            }
            setIgnoreTouchId(point.mId);
            TouchPoint point2 = this.mTouchPoints.poll();
            point = point2;
        }
        if (this.mTemporarilyId != this.TEMPORARILY_ID_NONE) {
            this.mIsTemporarilyInputed = true;
        }
        return result;
    }

    private boolean executeTouchAction(long time, int action, TouchPoint point) {
        if (this.mIgnoreTouchEvent) {
            return true;
        }
        MotionEvent motionEvent = MotionEvent.obtain(time, time, action, point.mX, point.mY, point.mMetaState);
        motionEvent.setSource(4098);
        boolean result = super.handleTouchEvent(motionEvent);
        motionEvent.recycle();
        return result;
    }

    @Override
    protected boolean onLongPress(Keyboard.Key popupKey, MotionEvent me) {
        boolean result;
        if (this.mIsInputTypeNull) {
            return false;
        }
        if (this.mFlickKeyboardActionListener != null && this.mFlickKeyboardActionListener.onLongPress(popupKey)) {
            result = true;
        } else {
            result = super.onLongPress(popupKey, me);
        }
        if (result) {
            this.mTouchPoints.clear();
            return result;
        }
        return result;
    }

    @Override
    public void setKeyboard(Keyboard keyboard) {
        if (keyboard != null) {
            super.setKeyboard(keyboard);
            setCapsLock(false);
        }
    }

    public void setKeyboard(Keyboard normalKeyboard, Keyboard shiftKeyboard) {
        if (this.mNormalKeyboard != normalKeyboard && this.mShiftKeyboard != shiftKeyboard && normalKeyboard != shiftKeyboard) {
            cancelTouchEvent();
        }
        this.mNormalKeyboard = normalKeyboard;
        this.mShiftKeyboard = shiftKeyboard;
        setShifted(this.mShifted);
    }

    public void setCapsLock(boolean capslock) {
        Keyboard.Key key;
        setCapsLockMode(capslock);
        if (this.mShiftKeyboard != null && (key = this.mShiftKeyboard.getShiftKey()) != null) {
            key.on = capslock;
            if (getKeyboard() == this.mShiftKeyboard) {
                int index = this.mShiftKeyboard.getKeys().indexOf(key);
                invalidateKey(index);
            }
        }
    }

    public void setCapsLockMode(boolean capslock) {
        this.mCaps = capslock;
    }

    @Override
    public boolean setShifted(boolean shift) {
        if (this.mTemporarilyId == this.TEMPORARILY_ID_NONE) {
            this.mShifted = shift;
            if (shift) {
                if (this.mShiftKeyboard != null) {
                    this.mShiftKeyboard.setShifted(true);
                }
                setKeyboard(this.mShiftKeyboard);
                return true;
            }
            setKeyboard(this.mNormalKeyboard);
            return true;
        }
        return false;
    }

    @Override
    public boolean isShifted() {
        return this.mShifted;
    }

    public boolean isCapsLock() {
        return this.mCaps;
    }

    public void cancelTouchEvent() {
        TouchPoint lastPoint;
        this.mTemporarilyId = this.TEMPORARILY_ID_NONE;
        this.mIsRepeatKeyTouch = false;
        if (this.mTouchPoints.size() > 0 && (lastPoint = this.mTouchPoints.getLast()) != null) {
            executeTouchAction(System.currentTimeMillis(), 3, lastPoint);
        }
        TouchPoint point = this.mTouchPoints.poll();
        while (point != null) {
            setIgnoreTouchId(point.mId);
            TouchPoint point2 = this.mTouchPoints.poll();
            point = point2;
        }
    }

    public void copyEnterKeyState() {
        if (this.mNormalKeyboard != null && this.mShiftKeyboard != null) {
            int normalKeyboardIndex = this.mNormalKeyboard.getKeyIndex(DefaultSoftKeyboard.KEYCODE_ENTER);
            int shiftKeyboardIndex = this.mShiftKeyboard.getKeyIndex(DefaultSoftKeyboard.KEYCODE_ENTER);
            if (normalKeyboardIndex != -1 && shiftKeyboardIndex != -1) {
                Keyboard.Key normalkey = this.mNormalKeyboard.getKey(normalKeyboardIndex);
                Keyboard.Key shiftkey = this.mShiftKeyboard.getKey(shiftKeyboardIndex);
                if (this.mShifted) {
                    normalkey.label = shiftkey.label;
                    normalkey.icon = shiftkey.icon;
                    normalkey.iconPreview = shiftkey.iconPreview;
                } else {
                    shiftkey.label = normalkey.label;
                    shiftkey.icon = normalkey.icon;
                    shiftkey.iconPreview = normalkey.iconPreview;
                }
            }
        }
    }

    public void setOnKeyboardActionListener(OnFlickKeyboardActionListener listener) {
        super.setOnKeyboardActionListener((KeyboardView.OnKeyboardActionListener) listener);
        this.mFlickKeyboardActionListener = listener;
    }

    private void setIgnoreTouchId(int id) {
        Integer setId = Integer.valueOf(id);
        if (!this.mIgnoreTouchId.contains(setId)) {
            this.mIgnoreTouchId.add(setId);
        }
    }
}

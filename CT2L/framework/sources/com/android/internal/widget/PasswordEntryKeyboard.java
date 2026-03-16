package com.android.internal.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import com.android.internal.R;

public class PasswordEntryKeyboard extends Keyboard {
    public static final int KEYCODE_SPACE = 32;
    private static final int SHIFT_LOCKED = 2;
    private static final int SHIFT_OFF = 0;
    private static final int SHIFT_ON = 1;
    static int sSpacebarVerticalCorrection;
    private Keyboard.Key mEnterKey;
    private Keyboard.Key mF1Key;
    private Drawable[] mOldShiftIcons;
    private Drawable mShiftIcon;
    private Keyboard.Key[] mShiftKeys;
    private Drawable mShiftLockIcon;
    private int mShiftState;
    private Keyboard.Key mSpaceKey;

    public PasswordEntryKeyboard(Context context, int xmlLayoutResId) {
        this(context, xmlLayoutResId, 0);
    }

    public PasswordEntryKeyboard(Context context, int xmlLayoutResId, int width, int height) {
        this(context, xmlLayoutResId, 0, width, height);
    }

    public PasswordEntryKeyboard(Context context, int xmlLayoutResId, int mode) {
        super(context, xmlLayoutResId, mode);
        this.mOldShiftIcons = new Drawable[]{null, null};
        this.mShiftKeys = new Keyboard.Key[]{null, null};
        this.mShiftState = 0;
        init(context);
    }

    public PasswordEntryKeyboard(Context context, int xmlLayoutResId, int mode, int width, int height) {
        super(context, xmlLayoutResId, mode, width, height);
        this.mOldShiftIcons = new Drawable[]{null, null};
        this.mShiftKeys = new Keyboard.Key[]{null, null};
        this.mShiftState = 0;
        init(context);
    }

    private void init(Context context) {
        Resources res = context.getResources();
        this.mShiftIcon = context.getDrawable(R.drawable.sym_keyboard_shift);
        this.mShiftLockIcon = context.getDrawable(R.drawable.sym_keyboard_shift_locked);
        sSpacebarVerticalCorrection = res.getDimensionPixelOffset(R.dimen.password_keyboard_spacebar_vertical_correction);
    }

    public PasswordEntryKeyboard(Context context, int layoutTemplateResId, CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
        this.mOldShiftIcons = new Drawable[]{null, null};
        this.mShiftKeys = new Keyboard.Key[]{null, null};
        this.mShiftState = 0;
    }

    @Override
    protected Keyboard.Key createKeyFromXml(Resources res, Keyboard.Row parent, int x, int y, XmlResourceParser parser) {
        LatinKey key = new LatinKey(res, parent, x, y, parser);
        int code = key.codes[0];
        if (code >= 0 && code != 10 && (code < 32 || code > 127)) {
            key.label = " ";
            key.setEnabled(false);
        }
        switch (key.codes[0]) {
            case PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES:
                this.mF1Key = key;
                return key;
            case 10:
                this.mEnterKey = key;
                return key;
            case 32:
                this.mSpaceKey = key;
                return key;
            default:
                return key;
        }
    }

    void setEnterKeyResources(Resources res, int previewId, int iconId, int labelId) {
        if (this.mEnterKey != null) {
            this.mEnterKey.popupCharacters = null;
            this.mEnterKey.popupResId = 0;
            this.mEnterKey.text = null;
            this.mEnterKey.iconPreview = res.getDrawable(previewId);
            this.mEnterKey.icon = res.getDrawable(iconId);
            this.mEnterKey.label = res.getText(labelId);
            if (this.mEnterKey.iconPreview != null) {
                this.mEnterKey.iconPreview.setBounds(0, 0, this.mEnterKey.iconPreview.getIntrinsicWidth(), this.mEnterKey.iconPreview.getIntrinsicHeight());
            }
        }
    }

    void enableShiftLock() {
        int i = 0;
        int[] arr$ = getShiftKeyIndices();
        for (int index : arr$) {
            if (index >= 0 && i < this.mShiftKeys.length) {
                this.mShiftKeys[i] = getKeys().get(index);
                if (this.mShiftKeys[i] instanceof LatinKey) {
                    ((LatinKey) this.mShiftKeys[i]).enableShiftLock();
                }
                this.mOldShiftIcons[i] = this.mShiftKeys[i].icon;
                i++;
            }
        }
    }

    void setShiftLocked(boolean shiftLocked) {
        Keyboard.Key[] arr$ = this.mShiftKeys;
        for (Keyboard.Key shiftKey : arr$) {
            if (shiftKey != null) {
                shiftKey.on = shiftLocked;
                shiftKey.icon = this.mShiftLockIcon;
            }
        }
        this.mShiftState = shiftLocked ? 2 : 1;
    }

    @Override
    public boolean setShifted(boolean shiftState) {
        boolean shiftChanged = false;
        if (!shiftState) {
            shiftChanged = this.mShiftState != 0;
            this.mShiftState = 0;
        } else if (this.mShiftState == 0) {
            shiftChanged = this.mShiftState == 0;
            this.mShiftState = 1;
        }
        for (int i = 0; i < this.mShiftKeys.length; i++) {
            if (this.mShiftKeys[i] != null) {
                if (!shiftState) {
                    this.mShiftKeys[i].on = false;
                    this.mShiftKeys[i].icon = this.mOldShiftIcons[i];
                } else if (this.mShiftState == 0) {
                    this.mShiftKeys[i].on = false;
                    this.mShiftKeys[i].icon = this.mShiftIcon;
                }
            }
        }
        return shiftChanged;
    }

    @Override
    public boolean isShifted() {
        return this.mShiftKeys[0] != null ? this.mShiftState != 0 : super.isShifted();
    }

    static class LatinKey extends Keyboard.Key {
        private boolean mEnabled;
        private boolean mShiftLockEnabled;

        public LatinKey(Resources res, Keyboard.Row parent, int x, int y, XmlResourceParser parser) {
            super(res, parent, x, y, parser);
            this.mEnabled = true;
            if (this.popupCharacters != null && this.popupCharacters.length() == 0) {
                this.popupResId = 0;
            }
        }

        void setEnabled(boolean enabled) {
            this.mEnabled = enabled;
        }

        void enableShiftLock() {
            this.mShiftLockEnabled = true;
        }

        @Override
        public void onReleased(boolean inside) {
            if (!this.mShiftLockEnabled) {
                super.onReleased(inside);
            } else {
                this.pressed = !this.pressed;
            }
        }

        @Override
        public boolean isInside(int x, int y) {
            if (!this.mEnabled) {
                return false;
            }
            int code = this.codes[0];
            if (code == -1 || code == -5) {
                y -= this.height / 10;
                if (code == -1) {
                    x += this.width / 6;
                }
                if (code == -5) {
                    x -= this.width / 6;
                }
            } else if (code == 32) {
                y += PasswordEntryKeyboard.sSpacebarVerticalCorrection;
            }
            return super.isInside(x, y);
        }
    }
}

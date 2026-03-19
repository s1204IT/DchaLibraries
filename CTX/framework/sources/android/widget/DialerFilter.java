package android.widget;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.text.method.KeyListener;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;

@Deprecated
public class DialerFilter extends RelativeLayout {
    public static final int DIGITS_AND_LETTERS = 1;
    public static final int DIGITS_AND_LETTERS_NO_DIGITS = 2;
    public static final int DIGITS_AND_LETTERS_NO_LETTERS = 3;
    public static final int DIGITS_ONLY = 4;
    public static final int LETTERS_ONLY = 5;
    EditText mDigits;
    EditText mHint;
    ImageView mIcon;
    InputFilter[] mInputFilters;
    private boolean mIsQwerty;
    EditText mLetters;
    int mMode;
    EditText mPrimary;

    public DialerFilter(Context context) {
        super(context);
    }

    public DialerFilter(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mInputFilters = new InputFilter[]{new InputFilter.AllCaps()};
        this.mHint = (EditText) findViewById(16908293);
        if (this.mHint == null) {
            throw new IllegalStateException("DialerFilter must have a child EditText named hint");
        }
        this.mHint.setFilters(this.mInputFilters);
        this.mLetters = this.mHint;
        this.mLetters.setKeyListener(TextKeyListener.getInstance());
        this.mLetters.setMovementMethod(null);
        this.mLetters.setFocusable(false);
        this.mPrimary = (EditText) findViewById(16908300);
        if (this.mPrimary == null) {
            throw new IllegalStateException("DialerFilter must have a child EditText named primary");
        }
        this.mPrimary.setFilters(this.mInputFilters);
        this.mDigits = this.mPrimary;
        this.mDigits.setKeyListener(DialerKeyListener.getInstance());
        this.mDigits.setMovementMethod(null);
        this.mDigits.setFocusable(false);
        this.mIcon = (ImageView) findViewById(16908294);
        setFocusable(true);
        this.mIsQwerty = true;
        setMode(1);
    }

    @Override
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        super.onFocusChanged(z, i, rect);
        if (this.mIcon != null) {
            this.mIcon.setVisibility(z ? 0 : 8);
        }
    }

    public boolean isQwertyKeyboard() {
        return this.mIsQwerty;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        boolean zOnKeyDown = false;
        switch (i) {
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                break;
            default:
                switch (i) {
                    case 66:
                        break;
                    case 67:
                        switch (this.mMode) {
                            case 1:
                                zOnKeyDown = this.mDigits.onKeyDown(i, keyEvent) & this.mLetters.onKeyDown(i, keyEvent);
                                break;
                            case 2:
                                zOnKeyDown = this.mLetters.onKeyDown(i, keyEvent);
                                if (this.mLetters.getText().length() == this.mDigits.getText().length()) {
                                    setMode(1);
                                }
                                break;
                            case 3:
                                if (this.mDigits.getText().length() == this.mLetters.getText().length()) {
                                    this.mLetters.onKeyDown(i, keyEvent);
                                    setMode(1);
                                }
                                zOnKeyDown = this.mDigits.onKeyDown(i, keyEvent);
                                break;
                            case 4:
                                zOnKeyDown = this.mDigits.onKeyDown(i, keyEvent);
                                break;
                            case 5:
                                zOnKeyDown = this.mLetters.onKeyDown(i, keyEvent);
                                break;
                        }
                        break;
                    default:
                        switch (this.mMode) {
                            case 1:
                                zOnKeyDown = this.mLetters.onKeyDown(i, keyEvent);
                                if (KeyEvent.isModifierKey(i)) {
                                    this.mDigits.onKeyDown(i, keyEvent);
                                    zOnKeyDown = true;
                                } else if (keyEvent.isPrintingKey() || i == 62 || i == 61) {
                                    if (keyEvent.getMatch(DialerKeyListener.CHARACTERS) != 0) {
                                        zOnKeyDown &= this.mDigits.onKeyDown(i, keyEvent);
                                    } else {
                                        setMode(2);
                                    }
                                }
                                break;
                            case 2:
                            case 5:
                                zOnKeyDown = this.mLetters.onKeyDown(i, keyEvent);
                                break;
                            case 3:
                            case 4:
                                zOnKeyDown = this.mDigits.onKeyDown(i, keyEvent);
                                break;
                        }
                        break;
                }
                break;
        }
        if (zOnKeyDown) {
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return this.mLetters.onKeyUp(i, keyEvent) || this.mDigits.onKeyUp(i, keyEvent);
    }

    public int getMode() {
        return this.mMode;
    }

    public void setMode(int i) {
        switch (i) {
            case 1:
                makeDigitsPrimary();
                this.mLetters.setVisibility(0);
                this.mDigits.setVisibility(0);
                break;
            case 2:
                makeLettersPrimary();
                this.mLetters.setVisibility(0);
                this.mDigits.setVisibility(4);
                break;
            case 3:
                makeDigitsPrimary();
                this.mLetters.setVisibility(4);
                this.mDigits.setVisibility(0);
                break;
            case 4:
                makeDigitsPrimary();
                this.mLetters.setVisibility(8);
                this.mDigits.setVisibility(0);
                break;
            case 5:
                makeLettersPrimary();
                this.mLetters.setVisibility(0);
                this.mDigits.setVisibility(8);
                break;
        }
        int i2 = this.mMode;
        this.mMode = i;
        onModeChange(i2, i);
    }

    private void makeLettersPrimary() {
        if (this.mPrimary == this.mDigits) {
            swapPrimaryAndHint(true);
        }
    }

    private void makeDigitsPrimary() {
        if (this.mPrimary == this.mLetters) {
            swapPrimaryAndHint(false);
        }
    }

    private void swapPrimaryAndHint(boolean z) {
        Editable text = this.mLetters.getText();
        Editable text2 = this.mDigits.getText();
        KeyListener keyListener = this.mLetters.getKeyListener();
        KeyListener keyListener2 = this.mDigits.getKeyListener();
        if (z) {
            this.mLetters = this.mPrimary;
            this.mDigits = this.mHint;
        } else {
            this.mLetters = this.mHint;
            this.mDigits = this.mPrimary;
        }
        this.mLetters.setKeyListener(keyListener);
        this.mLetters.setText(text);
        Editable text3 = this.mLetters.getText();
        Selection.setSelection(text3, text3.length());
        this.mDigits.setKeyListener(keyListener2);
        this.mDigits.setText(text2);
        Editable text4 = this.mDigits.getText();
        Selection.setSelection(text4, text4.length());
        this.mPrimary.setFilters(this.mInputFilters);
        this.mHint.setFilters(this.mInputFilters);
    }

    public CharSequence getLetters() {
        if (this.mLetters.getVisibility() == 0) {
            return this.mLetters.getText();
        }
        return "";
    }

    public CharSequence getDigits() {
        if (this.mDigits.getVisibility() == 0) {
            return this.mDigits.getText();
        }
        return "";
    }

    public CharSequence getFilterText() {
        if (this.mMode != 4) {
            return getLetters();
        }
        return getDigits();
    }

    public void append(String str) {
        switch (this.mMode) {
            case 1:
                this.mDigits.getText().append((CharSequence) str);
                this.mLetters.getText().append((CharSequence) str);
                break;
            case 2:
            case 5:
                this.mLetters.getText().append((CharSequence) str);
                break;
            case 3:
            case 4:
                this.mDigits.getText().append((CharSequence) str);
                break;
        }
    }

    public void clearText() {
        this.mLetters.getText().clear();
        this.mDigits.getText().clear();
        if (this.mIsQwerty) {
            setMode(1);
        } else {
            setMode(4);
        }
    }

    public void setLettersWatcher(TextWatcher textWatcher) {
        Editable text = this.mLetters.getText();
        text.setSpan(textWatcher, 0, text.length(), 18);
    }

    public void setDigitsWatcher(TextWatcher textWatcher) {
        Editable text = this.mDigits.getText();
        text.setSpan(textWatcher, 0, text.length(), 18);
    }

    public void setFilterWatcher(TextWatcher textWatcher) {
        if (this.mMode != 4) {
            setLettersWatcher(textWatcher);
        } else {
            setDigitsWatcher(textWatcher);
        }
    }

    public void removeFilterWatcher(TextWatcher textWatcher) {
        Editable text;
        if (this.mMode != 4) {
            text = this.mLetters.getText();
        } else {
            text = this.mDigits.getText();
        }
        text.removeSpan(textWatcher);
    }

    protected void onModeChange(int i, int i2) {
    }
}

package android.inputmethodservice;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class ExtractEditText extends EditText {
    private InputMethodService mIME;
    private int mSettingExtractedText;

    public ExtractEditText(Context context) {
        super(context, null);
    }

    public ExtractEditText(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.editTextStyle);
    }

    public ExtractEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ExtractEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    void setIME(InputMethodService ime) {
        this.mIME = ime;
    }

    public void startInternalChanges() {
        this.mSettingExtractedText++;
    }

    public void finishInternalChanges() {
        this.mSettingExtractedText--;
    }

    @Override
    public void setExtractedText(ExtractedText text) {
        try {
            this.mSettingExtractedText++;
            super.setExtractedText(text);
        } finally {
            this.mSettingExtractedText--;
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (this.mSettingExtractedText != 0 || this.mIME == null || selStart < 0 || selEnd < 0) {
            return;
        }
        this.mIME.onExtractedSelectionChanged(selStart, selEnd);
    }

    @Override
    public boolean performClick() {
        if (!super.performClick() && this.mIME != null) {
            this.mIME.onExtractedTextClicked();
            return true;
        }
        return false;
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == 16908319 || id == 16908340) {
            return super.onTextContextMenuItem(id);
        }
        if (this.mIME != null && this.mIME.onExtractTextContextMenuItem(id)) {
            if (id == 16908321 || id == 16908322) {
                stopTextActionMode();
                return true;
            }
            return true;
        }
        return super.onTextContextMenuItem(id);
    }

    @Override
    public boolean isInputMethodTarget() {
        return true;
    }

    public boolean hasVerticalScrollBar() {
        return computeVerticalScrollRange() > computeVerticalScrollExtent();
    }

    @Override
    public boolean hasWindowFocus() {
        return isEnabled();
    }

    @Override
    public boolean isFocused() {
        return isEnabled();
    }

    @Override
    public boolean hasFocus() {
        return isEnabled();
    }

    protected void viewClicked(InputMethodManager imm) {
        if (this.mIME == null) {
            return;
        }
        this.mIME.onViewClicked(false);
    }

    public boolean isInExtractedMode() {
        return true;
    }

    protected void deleteText_internal(int start, int end) {
        this.mIME.onExtractedDeleteText(start, end);
    }

    protected void replaceText_internal(int start, int end, CharSequence text) {
        this.mIME.onExtractedReplaceText(start, end, text);
    }

    protected void setSpan_internal(Object span, int start, int end, int flags) {
        this.mIME.onExtractedSetSpan(span, start, end, flags);
    }

    protected void setCursorPosition_internal(int start, int end) {
        this.mIME.onExtractedSelectionChanged(start, end);
    }
}

package android.inputmethodservice;

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

    public ExtractEditText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 16842862);
    }

    public ExtractEditText(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public ExtractEditText(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    void setIME(InputMethodService inputMethodService) {
        this.mIME = inputMethodService;
    }

    public void startInternalChanges() {
        this.mSettingExtractedText++;
    }

    public void finishInternalChanges() {
        this.mSettingExtractedText--;
    }

    @Override
    public void setExtractedText(ExtractedText extractedText) {
        try {
            this.mSettingExtractedText++;
            super.setExtractedText(extractedText);
        } finally {
            this.mSettingExtractedText--;
        }
    }

    @Override
    protected void onSelectionChanged(int i, int i2) {
        if (this.mSettingExtractedText == 0 && this.mIME != null && i >= 0 && i2 >= 0) {
            this.mIME.onExtractedSelectionChanged(i, i2);
        }
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
    public boolean onTextContextMenuItem(int i) {
        if (i == 16908319 || i == 16908340) {
            return super.onTextContextMenuItem(i);
        }
        if (this.mIME != null && this.mIME.onExtractTextContextMenuItem(i)) {
            if (i == 16908321 || i == 16908322) {
                stopTextActionMode();
                return true;
            }
            return true;
        }
        return super.onTextContextMenuItem(i);
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

    @Override
    protected void viewClicked(InputMethodManager inputMethodManager) {
        if (this.mIME != null) {
            this.mIME.onViewClicked(false);
        }
    }

    @Override
    public boolean isInExtractedMode() {
        return true;
    }

    @Override
    protected void deleteText_internal(int i, int i2) {
        this.mIME.onExtractedDeleteText(i, i2);
    }

    @Override
    protected void replaceText_internal(int i, int i2, CharSequence charSequence) {
        this.mIME.onExtractedReplaceText(i, i2, charSequence);
    }

    @Override
    protected void setSpan_internal(Object obj, int i, int i2, int i3) {
        this.mIME.onExtractedSetSpan(obj, i, i2, i3);
    }

    @Override
    protected void setCursorPosition_internal(int i, int i2) {
        this.mIME.onExtractedSelectionChanged(i, i2);
    }
}

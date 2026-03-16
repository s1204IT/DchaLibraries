package com.android.contacts.editor;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.contacts.R;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.util.PhoneNumberFormatter;

public class TextFieldsEditorView extends LabeledEditorView {
    private static final String TAG = TextFieldsEditorView.class.getSimpleName();
    private ImageView mExpansionView;
    private View mExpansionViewContainer;
    private EditText[] mFieldEditTexts;
    private ViewGroup mFields;
    private boolean mHasShortAndLongForms;
    private boolean mHideOptional;
    private int mHintTextColor;
    private int mHintTextColorUnfocused;
    private int mMinFieldHeight;
    private int mPreviousViewHeight;
    private View.OnFocusChangeListener mTextFocusChangeListener;

    public TextFieldsEditorView(Context context) {
        super(context);
        this.mFieldEditTexts = null;
        this.mFields = null;
        this.mHideOptional = true;
        this.mTextFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                TextFieldsEditorView.this.setHintColorDark(TextFieldsEditorView.this.findFocus() != null);
                if (TextFieldsEditorView.this.getEditorListener() != null) {
                    TextFieldsEditorView.this.getEditorListener().onRequest(6);
                }
                TextFieldsEditorView.this.rebuildLabel();
            }
        };
    }

    public TextFieldsEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFieldEditTexts = null;
        this.mFields = null;
        this.mHideOptional = true;
        this.mTextFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                TextFieldsEditorView.this.setHintColorDark(TextFieldsEditorView.this.findFocus() != null);
                if (TextFieldsEditorView.this.getEditorListener() != null) {
                    TextFieldsEditorView.this.getEditorListener().onRequest(6);
                }
                TextFieldsEditorView.this.rebuildLabel();
            }
        };
    }

    public TextFieldsEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFieldEditTexts = null;
        this.mFields = null;
        this.mHideOptional = true;
        this.mTextFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                TextFieldsEditorView.this.setHintColorDark(TextFieldsEditorView.this.findFocus() != null);
                if (TextFieldsEditorView.this.getEditorListener() != null) {
                    TextFieldsEditorView.this.getEditorListener().onRequest(6);
                }
                TextFieldsEditorView.this.rebuildLabel();
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);
        this.mMinFieldHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.editor_min_line_item_height);
        this.mFields = (ViewGroup) findViewById(R.id.editors);
        this.mHintTextColor = getResources().getColor(R.color.secondary_text_color);
        this.mHintTextColorUnfocused = getResources().getColor(R.color.editor_disabled_text_color);
        this.mExpansionView = (ImageView) findViewById(R.id.expansion_view);
        this.mExpansionViewContainer = findViewById(R.id.expansion_view_container);
        if (this.mExpansionViewContainer != null) {
            this.mExpansionViewContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextFieldsEditorView.this.mPreviousViewHeight = TextFieldsEditorView.this.mFields.getHeight();
                    View focusedChild = TextFieldsEditorView.this.getFocusedChild();
                    int focusedViewId = focusedChild == null ? -1 : focusedChild.getId();
                    TextFieldsEditorView.this.mHideOptional = !TextFieldsEditorView.this.mHideOptional;
                    TextFieldsEditorView.this.onOptionalFieldVisibilityChange();
                    TextFieldsEditorView.this.rebuildValues();
                    View newFocusView = TextFieldsEditorView.this.findViewById(focusedViewId);
                    if (newFocusView == null || newFocusView.getVisibility() == 8) {
                        newFocusView = TextFieldsEditorView.this;
                    }
                    newFocusView.requestFocus();
                    EditorAnimator.getInstance().slideAndFadeIn(TextFieldsEditorView.this.mFields, TextFieldsEditorView.this.mPreviousViewHeight);
                }
            });
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mFieldEditTexts != null) {
            for (int index = 0; index < this.mFieldEditTexts.length; index++) {
                this.mFieldEditTexts[index].setEnabled(!isReadOnly() && enabled);
            }
        }
        if (this.mExpansionView != null) {
            this.mExpansionView.setEnabled(!isReadOnly() && enabled);
        }
        if (this.mExpansionViewContainer != null) {
            this.mExpansionViewContainer.setEnabled(enabled);
        }
    }

    public void setHintColorDark(boolean isHintDark) {
        if (this.mFieldEditTexts != null) {
            EditText[] arr$ = this.mFieldEditTexts;
            for (EditText text : arr$) {
                if (isHintDark) {
                    text.setHintTextColor(this.mHintTextColor);
                } else {
                    text.setHintTextColor(this.mHintTextColorUnfocused);
                }
            }
        }
    }

    private void setupExpansionView(boolean shouldExist, boolean collapsed) {
        if (shouldExist) {
            this.mExpansionViewContainer.setVisibility(0);
            this.mExpansionView.setImageResource(collapsed ? R.drawable.ic_menu_expander_minimized_holo_light : R.drawable.ic_menu_expander_maximized_holo_light);
        } else {
            this.mExpansionViewContainer.setVisibility(8);
        }
    }

    @Override
    protected void requestFocusForFirstEditField() {
        if (this.mFieldEditTexts != null && this.mFieldEditTexts.length != 0) {
            EditText firstField = null;
            boolean anyFieldHasFocus = false;
            EditText[] arr$ = this.mFieldEditTexts;
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                EditText editText = arr$[i$];
                if (firstField == null && editText.getVisibility() == 0) {
                    firstField = editText;
                }
                if (!editText.hasFocus()) {
                    i$++;
                } else {
                    anyFieldHasFocus = true;
                    break;
                }
            }
            if (!anyFieldHasFocus && firstField != null) {
                firstField.requestFocus();
            }
        }
    }

    public void setValue(int field, String value) {
        this.mFieldEditTexts[field].setText(value);
    }

    @Override
    public void setValues(DataKind kind, ValuesDelta entry, RawContactDelta state, boolean readOnly, ViewIdGenerator vig) {
        super.setValues(kind, entry, state, readOnly, vig);
        if (this.mFieldEditTexts != null) {
            EditText[] arr$ = this.mFieldEditTexts;
            for (EditText fieldEditText : arr$) {
                this.mFields.removeView(fieldEditText);
            }
        }
        boolean hidePossible = false;
        int fieldCount = kind.fieldList.size();
        this.mFieldEditTexts = new EditText[fieldCount];
        for (int index = 0; index < fieldCount; index++) {
            AccountType.EditField field = kind.fieldList.get(index);
            EditText fieldView = new EditText(this.mContext);
            fieldView.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            fieldView.setTextSize(0, getResources().getDimension(R.dimen.editor_form_text_size));
            fieldView.setHintTextColor(this.mHintTextColorUnfocused);
            this.mFieldEditTexts[index] = fieldView;
            fieldView.setId(vig.getId(state, kind, entry, index));
            if (field.titleRes > 0) {
                fieldView.setHint(field.titleRes);
            }
            int inputType = field.inputType;
            fieldView.setInputType(inputType);
            if (inputType == 3) {
                PhoneNumberFormatter.setPhoneNumberFormattingTextWatcher(this.mContext, fieldView);
                fieldView.setTextDirection(3);
            }
            if (field.minLines > 1) {
                fieldView.setMinLines(field.minLines);
            } else {
                fieldView.setMinHeight(this.mMinFieldHeight);
            }
            fieldView.setImeOptions(5);
            final String column = field.column;
            String value = entry.getAsString(column);
            fieldView.setText(value);
            setDeleteButtonVisible(value != null);
            fieldView.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    TextFieldsEditorView.this.onFieldChanged(column, s.toString());
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });
            fieldView.setEnabled(isEnabled() && !readOnly);
            fieldView.setOnFocusChangeListener(this.mTextFocusChangeListener);
            if (field.shortForm) {
                hidePossible = true;
                this.mHasShortAndLongForms = true;
                fieldView.setVisibility(this.mHideOptional ? 0 : 8);
            } else if (field.longForm) {
                hidePossible = true;
                this.mHasShortAndLongForms = true;
                fieldView.setVisibility(this.mHideOptional ? 8 : 0);
            } else {
                boolean couldHide = !ContactsUtils.isGraphic(value) && field.optional;
                boolean willHide = this.mHideOptional && couldHide;
                fieldView.setVisibility(willHide ? 8 : 0);
                hidePossible = hidePossible || couldHide;
            }
            this.mFields.addView(fieldView);
        }
        if (this.mExpansionView != null) {
            setupExpansionView(hidePossible, this.mHideOptional);
            this.mExpansionView.setEnabled(!readOnly && isEnabled());
        }
        updateEmptiness();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.mFields.getChildCount(); i++) {
            EditText editText = (EditText) this.mFields.getChildAt(i);
            if (!TextUtils.isEmpty(editText.getText())) {
                return false;
            }
        }
        return true;
    }

    public boolean areOptionalFieldsVisible() {
        return !this.mHideOptional;
    }

    public boolean hasShortAndLongForms() {
        return this.mHasShortAndLongForms;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.mHideOptional = this.mHideOptional;
        int numChildren = this.mFieldEditTexts == null ? 0 : this.mFieldEditTexts.length;
        ss.mVisibilities = new int[numChildren];
        for (int i = 0; i < numChildren; i++) {
            ss.mVisibilities[i] = this.mFieldEditTexts[i].getVisibility();
        }
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mHideOptional = ss.mHideOptional;
        int mFieldEditTextsLength = this.mFieldEditTexts == null ? 0 : this.mFieldEditTexts.length;
        int numChildren = Math.min(mFieldEditTextsLength, ss.mVisibilities.length);
        for (int i = 0; i < numChildren; i++) {
            this.mFieldEditTexts[i].setVisibility(ss.mVisibilities[i]);
        }
    }

    private static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public boolean mHideOptional;
        public int[] mVisibilities;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.mVisibilities = new int[in.readInt()];
            in.readIntArray(this.mVisibilities);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.mVisibilities.length);
            out.writeIntArray(this.mVisibilities);
        }
    }

    @Override
    public void clearAllFields() {
        if (this.mFieldEditTexts != null) {
            EditText[] arr$ = this.mFieldEditTexts;
            for (EditText fieldEditText : arr$) {
                fieldEditText.setText("");
            }
        }
    }
}

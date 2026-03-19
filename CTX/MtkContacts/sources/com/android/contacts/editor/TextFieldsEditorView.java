package com.android.contacts.editor;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.TtsSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.compat.PhoneNumberUtilsCompat;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.PhoneNumberFormatter;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.editor.ContactEditorUtilsEx;
import com.mediatek.contacts.util.Log;

public class TextFieldsEditorView extends LabeledEditorView {
    private static final String TAG = TextFieldsEditorView.class.getSimpleName();
    protected String mCollapseButtonDescription;
    protected String mCollapsedAnnouncement;
    protected String mExpandButtonDescription;
    protected String mExpandedAnnouncement;
    protected ImageView mExpansionView;
    protected View mExpansionViewContainer;
    private EditText[] mFieldEditTexts;
    private ViewGroup mFields;
    private String mFixedDisplayName;
    private String mFixedPhonetic;
    private boolean mHasShortAndLongForms;
    private boolean mHideOptional;
    private int mHintTextColorUnfocused;
    private int mMinFieldHeight;
    private int mPreviousViewHeight;
    private View.OnFocusChangeListener mTextFocusChangeListener;
    private boolean needInputInitialize;

    public TextFieldsEditorView(Context context) {
        super(context);
        this.mFieldEditTexts = null;
        this.mFields = null;
        this.mHideOptional = true;
        this.mFixedPhonetic = "";
        this.mFixedDisplayName = "";
        this.mTextFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean z) {
                if (TextFieldsEditorView.this.getEditorListener() != null) {
                    TextFieldsEditorView.this.getEditorListener().onRequest(6);
                }
                TextFieldsEditorView.this.rebuildLabel();
                if (z) {
                    TextFieldsEditorView.this.needInputInitialize = true;
                }
            }
        };
    }

    public TextFieldsEditorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mFieldEditTexts = null;
        this.mFields = null;
        this.mHideOptional = true;
        this.mFixedPhonetic = "";
        this.mFixedDisplayName = "";
        this.mTextFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean z) {
                if (TextFieldsEditorView.this.getEditorListener() != null) {
                    TextFieldsEditorView.this.getEditorListener().onRequest(6);
                }
                TextFieldsEditorView.this.rebuildLabel();
                if (z) {
                    TextFieldsEditorView.this.needInputInitialize = true;
                }
            }
        };
    }

    public TextFieldsEditorView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mFieldEditTexts = null;
        this.mFields = null;
        this.mHideOptional = true;
        this.mFixedPhonetic = "";
        this.mFixedDisplayName = "";
        this.mTextFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean z) {
                if (TextFieldsEditorView.this.getEditorListener() != null) {
                    TextFieldsEditorView.this.getEditorListener().onRequest(6);
                }
                TextFieldsEditorView.this.rebuildLabel();
                if (z) {
                    TextFieldsEditorView.this.needInputInitialize = true;
                }
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        Log.d(TAG, "[onFinishInflate] beg");
        super.onFinishInflate();
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);
        this.mMinFieldHeight = getContext().getResources().getDimensionPixelSize(R.dimen.editor_min_line_item_height);
        this.mFields = (ViewGroup) findViewById(R.id.editors);
        this.mHintTextColorUnfocused = getResources().getColor(R.color.editor_disabled_text_color);
        this.mExpansionView = (ImageView) findViewById(R.id.expansion_view);
        this.mCollapseButtonDescription = getResources().getString(R.string.collapse_fields_description);
        this.mCollapsedAnnouncement = getResources().getString(R.string.announce_collapsed_fields);
        this.mExpandButtonDescription = getResources().getString(R.string.expand_fields_description);
        this.mExpandedAnnouncement = getResources().getString(R.string.announce_expanded_fields);
        this.mExpansionViewContainer = findViewById(R.id.expansion_view_container);
        if (this.mExpansionViewContainer != null) {
            this.mExpansionViewContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TextFieldsEditorView.TAG, "[mExpansionViewContainer] onClick");
                    TextFieldsEditorView.this.mPreviousViewHeight = TextFieldsEditorView.this.mFields.getHeight();
                    View viewFindFocus = TextFieldsEditorView.this.findFocus();
                    int id = viewFindFocus == null ? -1 : viewFindFocus.getId();
                    InputMethodManager inputMethodManager = (InputMethodManager) TextFieldsEditorView.this.getContext().getSystemService("input_method");
                    if (inputMethodManager != null && view != null) {
                        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    TextFieldsEditorView.this.mHideOptional = !TextFieldsEditorView.this.mHideOptional;
                    TextFieldsEditorView.this.onOptionalFieldVisibilityChange();
                    TextFieldsEditorView.this.rebuildValues();
                    View viewFindViewById = TextFieldsEditorView.this.findViewById(id);
                    if (viewFindViewById == null || viewFindViewById.getVisibility() == 8) {
                        viewFindViewById = TextFieldsEditorView.this;
                    }
                    viewFindViewById.requestFocus();
                    EditorAnimator.getInstance().slideAndFadeIn(TextFieldsEditorView.this.mFields, TextFieldsEditorView.this.mPreviousViewHeight);
                    TextFieldsEditorView.this.announceForAccessibility(TextFieldsEditorView.this.mHideOptional ? TextFieldsEditorView.this.mCollapsedAnnouncement : TextFieldsEditorView.this.mExpandedAnnouncement);
                }
            });
        }
        Log.d(TAG, "[onFinishInflate] end");
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        if (this.mFieldEditTexts != null) {
            for (int i = 0; i < this.mFieldEditTexts.length; i++) {
                this.mFieldEditTexts[i].setEnabled(!isReadOnly() && z);
            }
        }
        if (this.mExpansionView != null) {
            this.mExpansionView.setEnabled(!isReadOnly() && z);
        }
    }

    private void setupExpansionView(boolean z, boolean z2) {
        int i;
        Context context = getContext();
        if (z2) {
            i = R.drawable.quantum_ic_expand_more_vd_theme_24;
        } else {
            i = R.drawable.quantum_ic_expand_less_vd_theme_24;
        }
        this.mExpansionView.setImageDrawable(context.getDrawable(i));
        this.mExpansionView.setContentDescription(z2 ? this.mExpandButtonDescription : this.mCollapseButtonDescription);
        Log.d(TAG, "[setupExpansionView] shouldExist =" + z);
        this.mExpansionViewContainer.setVisibility(z ? 0 : 4);
    }

    @Override
    protected void requestFocusForFirstEditField() {
        if (this.mFieldEditTexts != null && this.mFieldEditTexts.length != 0) {
            EditText[] editTextArr = this.mFieldEditTexts;
            boolean z = false;
            int length = editTextArr.length;
            EditText editText = null;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                EditText editText2 = editTextArr[i];
                if (editText == null && editText2.getVisibility() == 0) {
                    editText = editText2;
                }
                if (!editText2.hasFocus()) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            }
            if (!z && editText != null) {
                editText.requestFocus();
            }
        }
    }

    private boolean isUnFixed(Editable editable) {
        Object[] spans = editable.getSpans(0, editable.length(), Object.class);
        if (spans == null) {
            return false;
        }
        boolean z = false;
        for (Object obj : spans) {
            if ((editable.getSpanFlags(obj) & 256) == 256) {
                z = true;
            }
        }
        return z;
    }

    private String getNameField(String str) {
        EditText editText;
        if ("data3".equals(str)) {
            editText = (EditText) this.mFields.getChildAt(1);
        } else if ("data2".equals(str)) {
            editText = (EditText) this.mFields.getChildAt(3);
        } else if ("data5".equals(str)) {
            editText = (EditText) this.mFields.getChildAt(2);
        } else {
            editText = null;
        }
        if (editText != null) {
            return editText.getText().toString();
        }
        return "";
    }

    @Override
    public void setValues(DataKind dataKind, ValuesDelta valuesDelta, final RawContactDelta rawContactDelta, boolean z, ViewIdGenerator viewIdGenerator) {
        AccountType.EditField editField;
        boolean z2;
        boolean z3;
        DataKind dataKind2 = dataKind;
        ValuesDelta valuesDelta2 = valuesDelta;
        if (dataKind2 == null || dataKind2.fieldList == null) {
            return;
        }
        super.setValues(dataKind, valuesDelta, rawContactDelta, z, viewIdGenerator);
        ?? r10 = 0;
        if (this.mFieldEditTexts != null) {
            for (EditText editText : this.mFieldEditTexts) {
                this.mFields.removeView(editText);
            }
        }
        int size = dataKind2.fieldList == null ? 0 : dataKind2.fieldList.size();
        Log.d(TAG, "[setValues] loop kind.fieldList, fieldCount=" + size);
        this.mFieldEditTexts = new EditText[size];
        boolean z4 = false;
        int i = 0;
        boolean z5 = false;
        while (i < size) {
            AccountType.EditField editField2 = dataKind2.fieldList.get(i);
            Log.sensitive(TAG, "[setValues] index=" + i + ", field=" + editField2);
            final ?? editText2 = new EditText(getContext());
            editText2.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            editText2.setTextSize(r10, getResources().getDimension(R.dimen.editor_form_text_size));
            editText2.setHintTextColor(this.mHintTextColorUnfocused);
            this.mFieldEditTexts[i] = editText2;
            editText2.setId(viewIdGenerator.getId(rawContactDelta, dataKind2, valuesDelta2, i));
            if (editField2.titleRes > 0) {
                editText2.setHint(editField2.titleRes);
            }
            if ("vnd.android.cursor.item/phone_v2".equals(dataKind2.mimeType)) {
                GlobalEnv.getSimAasEditor().updateView(rawContactDelta, editText2, valuesDelta2, 1);
            }
            final int i2 = editField2.inputType;
            editText2.setInputType(i2);
            if (i2 == 3) {
                PhoneNumberFormatter.setPhoneNumberFormattingTextWatcher(getContext(), editText2, rawContactDelta.isContactInsert());
                editText2.setTextDirection(3);
            }
            editText2.setTextAlignment(5);
            if (editField2.minLines > 1) {
                editText2.setMinLines(editField2.minLines);
            } else {
                editText2.setMinHeight(this.mMinFieldHeight);
            }
            editText2.setImeOptions(33554437);
            final String str = editField2.column;
            String asString = valuesDelta2.getAsString(str);
            if ("vnd.android.cursor.item/phone_v2".equals(dataKind2.mimeType)) {
                editText2.setText(PhoneNumberUtilsCompat.createTtsSpannable(asString));
                editField = editField2;
                z2 = false;
            } else {
                editField = editField2;
                z2 = false;
                editText2.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ContactEditorUtilsEx.getFieldEditorLengthLimit(i2))});
                editText2.setText(asString);
            }
            Log.d(TAG, "[setValues] fieldView.setText()=" + Log.anonymize(editText2.getText().toString()));
            boolean z6 = (TextUtils.isEmpty(asString) || z4) ? z4 : true;
            AccountType.EditField editField3 = editField;
            editText2.addTextChangedListener(new TextWatcher() {
                private int mStart = 0;

                @Override
                public void afterTextChanged(Editable editable) {
                    TextFieldsEditorView.this.onFieldChanged(str, editable.toString());
                    if (!DataKind.PSEUDO_MIME_TYPE_NAME.equals(TextFieldsEditorView.this.getKind().mimeType)) {
                        return;
                    }
                    String string = editable.toString();
                    int length = string.length() - TextFieldsEditorView.this.mFixedDisplayName.length();
                    if (TextFieldsEditorView.this.isUnFixed(editable) || length == 0) {
                        TextFieldsEditorView.this.updatePhonetic(str, TextFieldsEditorView.this.mFixedPhonetic + string.substring(this.mStart, string.length()));
                    } else {
                        TextFieldsEditorView.this.mFixedPhonetic = TextFieldsEditorView.this.getPhonetic(str);
                        TextFieldsEditorView.this.mFixedDisplayName = string;
                    }
                    ExtensionManager.getInstance();
                    ExtensionManager.getRcsExtension().setTextChangedListener(rawContactDelta, editText2, i2, editable.toString());
                }

                @Override
                public void beforeTextChanged(CharSequence charSequence, int i3, int i4, int i5) {
                    if (DataKind.PSEUDO_MIME_TYPE_NAME.equals(TextFieldsEditorView.this.getKind().mimeType) && TextFieldsEditorView.this.needInputInitialize) {
                        TextFieldsEditorView.this.mFixedPhonetic = TextFieldsEditorView.this.getPhonetic(str);
                        TextFieldsEditorView.this.mFixedDisplayName = TextFieldsEditorView.this.getNameField(str);
                        TextFieldsEditorView.this.needInputInitialize = false;
                    }
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i3, int i4, int i5) {
                    this.mStart = i3;
                    if (!"vnd.android.cursor.item/phone_v2".equals(TextFieldsEditorView.this.getKind().mimeType) || !(charSequence instanceof Spannable)) {
                        return;
                    }
                    Spannable spannable = (Spannable) charSequence;
                    for (TtsSpan ttsSpan : (TtsSpan[]) spannable.getSpans(0, charSequence.length(), TtsSpan.class)) {
                        spannable.removeSpan(ttsSpan);
                    }
                    PhoneNumberUtilsCompat.addTtsSpan(spannable, 0, charSequence.length());
                }
            });
            editText2.setEnabled((!isEnabled() || z) ? z2 : true);
            editText2.setOnFocusChangeListener(this.mTextFocusChangeListener);
            if (editField3.shortForm) {
                this.mHasShortAndLongForms = true;
                editText2.setVisibility(this.mHideOptional ? z2 : 8);
                z5 = true;
            } else {
                if (editField3.longForm) {
                    z3 = true;
                    this.mHasShortAndLongForms = true;
                    editText2.setVisibility(this.mHideOptional ? 8 : z2);
                } else {
                    z3 = true;
                    boolean z7 = (ContactsUtils.isGraphic(asString) || !editField3.optional) ? z2 : true;
                    editText2.setVisibility((!this.mHideOptional || !z7) ? z2 : true ? 8 : z2);
                    if (!z5 && !z7) {
                        z3 = z2;
                    }
                }
                z5 = z3;
            }
            this.mFields.addView(editText2);
            i++;
            r10 = z2;
            z4 = z6;
            dataKind2 = dataKind;
            valuesDelta2 = valuesDelta;
        }
        ?? r15 = r10;
        setDeleteButtonVisible(z4);
        if (this.mExpansionView != null) {
            setupExpansionView(z5, this.mHideOptional);
            this.mExpansionView.setEnabled((z || !isEnabled()) ? r15 == true ? 1 : 0 : true);
        }
        updateEmptiness();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.mFields.getChildCount(); i++) {
            if (!TextUtils.isEmpty(((EditText) this.mFields.getChildAt(i)).getText())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        int length;
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mHideOptional = this.mHideOptional;
        if (this.mFieldEditTexts != null) {
            length = this.mFieldEditTexts.length;
        } else {
            length = 0;
        }
        savedState.mVisibilities = new int[length];
        for (int i = 0; i < length; i++) {
            savedState.mVisibilities[i] = this.mFieldEditTexts[i].getVisibility();
        }
        savedState.mSelfVisibility = getVisibility();
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mHideOptional = savedState.mHideOptional;
        int iMin = Math.min(this.mFieldEditTexts == null ? 0 : this.mFieldEditTexts.length, savedState.mVisibilities == null ? 0 : savedState.mVisibilities.length);
        for (int i = 0; i < iMin; i++) {
            this.mFieldEditTexts[i].setVisibility(savedState.mVisibilities[i]);
        }
        rebuildValues();
        setVisibility(savedState.mSelfVisibility);
    }

    private static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        public boolean mHideOptional;
        public int mSelfVisibility;
        public int[] mVisibilities;

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.mVisibilities = new int[parcel.readInt()];
            parcel.readIntArray(this.mVisibilities);
            this.mHideOptional = parcel.readInt() == 1;
            this.mSelfVisibility = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.mVisibilities.length);
            parcel.writeIntArray(this.mVisibilities);
            parcel.writeInt(this.mHideOptional ? 1 : 0);
            parcel.writeInt(this.mSelfVisibility);
        }
    }

    @Override
    public void clearAllFields() {
        if (this.mFieldEditTexts != null) {
            for (EditText editText : this.mFieldEditTexts) {
                editText.setText("");
            }
        }
    }
}

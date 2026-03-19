package com.android.contacts.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.editor.Editor;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.DialogManager;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class LabeledEditorView extends LinearLayout implements Editor, DialogManager.DialogShowingView {
    public static final AccountType.EditType CUSTOM_SELECTION = new AccountType.EditType(0, 0);
    private ImageView mDelete;
    protected View mDeleteContainer;
    private DialogManager mDialogManager;
    private EditTypeAdapter mEditTypeAdapter;
    private ValuesDelta mEntry;
    private boolean mIsAttachedToWindow;
    private boolean mIsDeletable;
    private DataKind mKind;
    private Spinner mLabel;
    private Editor.EditorListener mListener;
    protected int mMinLineItemHeight;
    private boolean mReadOnly;
    private int mSelectedLabelIndex;
    private AdapterView.OnItemSelectedListener mSpinnerListener;
    private RawContactDelta mState;
    private AccountType.EditType mType;
    private ViewIdGenerator mViewIdGenerator;
    private boolean mWasEmpty;

    protected abstract void requestFocusForFirstEditField();

    public LabeledEditorView(Context context) {
        super(context);
        this.mWasEmpty = true;
        this.mIsDeletable = true;
        this.mDialogManager = null;
        this.mSpinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                if (GlobalEnv.getSimAasEditor().isPrimaryNumber(LabeledEditorView.this.mKind, LabeledEditorView.this.mEntry, LabeledEditorView.this.mState)) {
                    Log.d("LabeledEditorView", "[mSpinnerListener.onItemSelected] primary number'sspinner is invisible, directly return");
                } else {
                    LabeledEditorView.this.onTypeSelectionChange(i);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };
        init(context);
    }

    public LabeledEditorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mWasEmpty = true;
        this.mIsDeletable = true;
        this.mDialogManager = null;
        this.mSpinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                if (GlobalEnv.getSimAasEditor().isPrimaryNumber(LabeledEditorView.this.mKind, LabeledEditorView.this.mEntry, LabeledEditorView.this.mState)) {
                    Log.d("LabeledEditorView", "[mSpinnerListener.onItemSelected] primary number'sspinner is invisible, directly return");
                } else {
                    LabeledEditorView.this.onTypeSelectionChange(i);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };
        init(context);
    }

    public LabeledEditorView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mWasEmpty = true;
        this.mIsDeletable = true;
        this.mDialogManager = null;
        this.mSpinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i2, long j) {
                if (GlobalEnv.getSimAasEditor().isPrimaryNumber(LabeledEditorView.this.mKind, LabeledEditorView.this.mEntry, LabeledEditorView.this.mState)) {
                    Log.d("LabeledEditorView", "[mSpinnerListener.onItemSelected] primary number'sspinner is invisible, directly return");
                } else {
                    LabeledEditorView.this.onTypeSelectionChange(i2);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };
        init(context);
    }

    private void init(Context context) {
        this.mMinLineItemHeight = context.getResources().getDimensionPixelSize(R.dimen.editor_min_line_item_height);
    }

    @Override
    protected void onFinishInflate() {
        this.mLabel = (Spinner) findViewById(R.id.spinner);
        this.mLabel.setId(-1);
        this.mLabel.setOnItemSelectedListener(this.mSpinnerListener);
        ViewSelectedFilter.suppressViewSelectedEvent(this.mLabel);
        this.mDelete = (ImageView) findViewById(R.id.delete_button);
        this.mDeleteContainer = findViewById(R.id.delete_button_container);
        this.mDeleteContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (LabeledEditorView.this.mIsAttachedToWindow && LabeledEditorView.this.mListener != null) {
                            LabeledEditorView.this.mListener.onDeleteRequested(LabeledEditorView.this);
                        }
                    }
                });
            }
        });
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), (int) getResources().getDimension(R.dimen.editor_padding_between_editor_views));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mIsAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mIsAttachedToWindow = false;
    }

    public void markDeleted() {
        this.mEntry.markDeleted();
    }

    @Override
    public void deleteEditor() {
        markDeleted();
        EditorAnimator.getInstance().removeEditorView(this);
    }

    public boolean isReadOnly() {
        return this.mReadOnly;
    }

    private void setupLabelButton(boolean z) {
        if (z) {
            this.mLabel.setEnabled(!this.mReadOnly && isEnabled());
            this.mLabel.setVisibility(0);
        } else {
            this.mLabel.setVisibility(8);
        }
    }

    private void setupDeleteButton() {
        if (this.mIsDeletable) {
            boolean z = false;
            this.mDeleteContainer.setVisibility(0);
            ImageView imageView = this.mDelete;
            if (!this.mReadOnly && isEnabled()) {
                z = true;
            }
            imageView.setEnabled(z);
            return;
        }
        this.mDeleteContainer.setVisibility(4);
    }

    public void setDeleteButtonVisible(boolean z) {
        if (this.mIsDeletable) {
            this.mDeleteContainer.setVisibility(z ? 0 : 4);
        }
    }

    protected void onOptionalFieldVisibilityChange() {
        if (this.mListener != null) {
            this.mListener.onRequest(5);
        }
    }

    @Override
    public void setEditorListener(Editor.EditorListener editorListener) {
        this.mListener = editorListener;
    }

    protected Editor.EditorListener getEditorListener() {
        return this.mListener;
    }

    @Override
    public void setDeletable(boolean z) {
        this.mIsDeletable = z;
        setupDeleteButton();
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        boolean z2 = false;
        this.mLabel.setEnabled(!this.mReadOnly && z);
        ImageView imageView = this.mDelete;
        if (!this.mReadOnly && z) {
            z2 = true;
        }
        imageView.setEnabled(z2);
    }

    protected DataKind getKind() {
        return this.mKind;
    }

    protected ValuesDelta getEntry() {
        return this.mEntry;
    }

    protected AccountType.EditType getType() {
        return this.mType;
    }

    public void rebuildLabel() {
        this.mEditTypeAdapter = new EditTypeAdapter(getContext());
        this.mEditTypeAdapter.setSelectedIndex(this.mSelectedLabelIndex);
        this.mLabel.setAdapter((SpinnerAdapter) this.mEditTypeAdapter);
        Log.d("LabeledEditorView", "[rebuildLabel] hasCustomSelection(): " + this.mEditTypeAdapter.hasCustomSelection());
        if (this.mEditTypeAdapter.hasCustomSelection()) {
            this.mLabel.setSelection(this.mEditTypeAdapter.getPosition(CUSTOM_SELECTION));
            this.mDeleteContainer.setContentDescription(getContext().getString(R.string.editor_delete_view_description, this.mEntry.getAsString(this.mType.customColumn), getContext().getString(this.mKind.titleRes)));
            return;
        }
        if (this.mType != null && this.mType.labelRes > 0 && this.mKind.titleRes > 0) {
            this.mLabel.setSelection(this.mEditTypeAdapter.getPosition(this.mType));
            this.mDeleteContainer.setContentDescription(getContext().getString(R.string.editor_delete_view_description, getContext().getString(this.mType.labelRes), getContext().getString(this.mKind.titleRes)));
        } else if (this.mKind.titleRes > 0) {
            this.mDeleteContainer.setContentDescription(getContext().getString(R.string.editor_delete_view_description_short, getContext().getString(this.mKind.titleRes)));
        }
        Log.sensitive("LabeledEditorView", "[rebuildLabel] Position: " + this.mEditTypeAdapter.getPosition(this.mType) + ",mType: " + this.mType);
        GlobalEnv.getSimAasEditor().rebuildLabelSelection(this.mState, this.mLabel, this.mEditTypeAdapter, this.mType, this.mKind);
    }

    public void onFieldChanged(String str, String str2) {
        if (!isFieldChanged(str, str2)) {
            return;
        }
        saveValue(str, str2);
        notifyEditorListener();
    }

    public void updatePhonetic(String str, String str2) {
    }

    public String getPhonetic(String str) {
        return "";
    }

    protected void saveValue(String str, String str2) {
        this.mEntry.put(str, str2);
    }

    protected final void updateEmptiness() {
        this.mWasEmpty = isEmpty();
    }

    protected void notifyEditorListener() {
        if (this.mListener != null) {
            this.mListener.onRequest(2);
        }
        boolean zIsEmpty = isEmpty();
        if (this.mWasEmpty != zIsEmpty) {
            if (zIsEmpty) {
                if (this.mListener != null) {
                    this.mListener.onRequest(3);
                }
                if (this.mIsDeletable) {
                    this.mDeleteContainer.setVisibility(4);
                }
            } else {
                if (this.mListener != null) {
                    this.mListener.onRequest(4);
                }
                if (this.mIsDeletable) {
                    this.mDeleteContainer.setVisibility(0);
                }
            }
            this.mWasEmpty = zIsEmpty;
            if (this.mEditTypeAdapter != null) {
                this.mEditTypeAdapter.notifyDataSetChanged();
            }
        }
    }

    protected boolean isFieldChanged(String str, String str2) {
        String asString = this.mEntry.getAsString(str);
        if (asString == null) {
            asString = "";
        }
        if (str2 == null) {
            str2 = "";
        }
        return !TextUtils.equals(asString, str2);
    }

    protected void rebuildValues() {
        setValues(this.mKind, this.mEntry, this.mState, this.mReadOnly, this.mViewIdGenerator);
    }

    public void setValues(DataKind dataKind, ValuesDelta valuesDelta, RawContactDelta rawContactDelta, boolean z, ViewIdGenerator viewIdGenerator) {
        Log.d("LabeledEditorView", "[setValues]");
        this.mKind = dataKind;
        this.mEntry = valuesDelta;
        this.mState = rawContactDelta;
        this.mReadOnly = z;
        this.mViewIdGenerator = viewIdGenerator;
        setId(viewIdGenerator.getId(rawContactDelta, dataKind, valuesDelta, -1));
        if (!valuesDelta.isVisible()) {
            setVisibility(8);
            return;
        }
        boolean z2 = false;
        setVisibility(0);
        boolean zHasEditTypes = RawContactModifier.hasEditTypes(dataKind);
        if (GlobalEnv.getSimAasEditor().handleLabel(dataKind, valuesDelta, rawContactDelta)) {
            zHasEditTypes = false;
        }
        setupLabelButton(zHasEditTypes);
        Spinner spinner = this.mLabel;
        if (!z && isEnabled()) {
            z2 = true;
        }
        spinner.setEnabled(z2);
        if (this.mKind.titleRes > 0) {
            this.mLabel.setContentDescription(getContext().getResources().getString(this.mKind.titleRes));
        }
        this.mType = RawContactModifier.getCurrentType(valuesDelta, dataKind);
        rebuildLabel();
    }

    public ValuesDelta getValues() {
        return this.mEntry;
    }

    private Dialog createCustomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(builder.getContext());
        builder.setTitle(R.string.customLabelPickerTitle);
        View viewInflate = layoutInflaterFrom.inflate(R.layout.contact_editor_label_name_dialog, (ViewGroup) null);
        final EditText editText = (EditText) viewInflate.findViewById(R.id.custom_dialog_content);
        editText.setInputType(8193);
        editText.setSaveEnabled(true);
        builder.setView(viewInflate);
        editText.requestFocus();
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String strTrim = editText.getText().toString().trim();
                if (ContactsUtils.isGraphic(strTrim)) {
                    ArrayList<AccountType.EditType> validTypes = RawContactModifier.getValidTypes(LabeledEditorView.this.mState, LabeledEditorView.this.mKind, null, true, null, true);
                    LabeledEditorView.this.mType = null;
                    Iterator<AccountType.EditType> it = validTypes.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        AccountType.EditType next = it.next();
                        if (next.customColumn != null) {
                            LabeledEditorView.this.mType = next;
                            break;
                        }
                    }
                    if (LabeledEditorView.this.mType == null) {
                        return;
                    }
                    LabeledEditorView.this.mEntry.put(LabeledEditorView.this.mKind.typeColumn, LabeledEditorView.this.mType.rawValue);
                    LabeledEditorView.this.mEntry.put(LabeledEditorView.this.mType.customColumn, strTrim);
                    LabeledEditorView.this.rebuildLabel();
                    LabeledEditorView.this.requestFocusForFirstEditField();
                    LabeledEditorView.this.onLabelRebuilt();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        final AlertDialog alertDialogCreate = builder.create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                LabeledEditorView.this.updateCustomDialogOkButtonState(alertDialogCreate, editText);
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                LabeledEditorView.this.updateCustomDialogOkButtonState(alertDialogCreate, editText);
            }
        });
        alertDialogCreate.getWindow().setSoftInputMode(5);
        return alertDialogCreate;
    }

    void updateCustomDialogOkButtonState(AlertDialog alertDialog, EditText editText) {
        alertDialog.getButton(-1).setEnabled(!TextUtils.isEmpty(editText.getText().toString().trim()));
    }

    protected void onLabelRebuilt() {
    }

    protected void onTypeSelectionChange(int i) {
        AccountType.EditType item = this.mEditTypeAdapter.getItem(i);
        if (GlobalEnv.getSimAasEditor().onTypeSelectionChange(this.mState, this.mEntry, this.mKind, this.mEditTypeAdapter, item, this.mType, getContext())) {
            Log.d("LabeledEditorView", "[onTypeSelectionChange] selected:" + item + ",mType: " + this.mType);
            if (item.rawValue != 0) {
                this.mType = item;
                Log.d("LabeledEditorView", "[onTypeSelectionChange] plugin selected except custom");
                return;
            }
            return;
        }
        if (this.mEditTypeAdapter.hasCustomSelection() && item == CUSTOM_SELECTION) {
            return;
        }
        if (this.mType == item && this.mType.customColumn == null) {
            return;
        }
        if (item.customColumn != null) {
            showDialog(1);
            return;
        }
        this.mType = item;
        this.mEntry.put(this.mKind.typeColumn, Integer.toString(this.mType.rawValue));
        this.mSelectedLabelIndex = i;
        rebuildLabel();
        requestFocusForFirstEditField();
        onLabelRebuilt();
    }

    void showDialog(int i) {
        Bundle bundle = new Bundle();
        bundle.putInt("dialog_id", i);
        getDialogManager().showDialogInView(this, bundle);
    }

    private DialogManager getDialogManager() {
        if (this.mDialogManager == null) {
            Object context = getContext();
            if (!(context instanceof DialogManager.DialogShowingViewActivity)) {
                throw new IllegalStateException("View must be hosted in an Activity that implements DialogManager.DialogShowingViewActivity");
            }
            this.mDialogManager = ((DialogManager.DialogShowingViewActivity) context).getDialogManager();
        }
        return this.mDialogManager;
    }

    public Dialog createDialog(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("bundle must not be null");
        }
        int i = bundle.getInt("dialog_id");
        if (i == 1) {
            return createCustomDialog();
        }
        throw new IllegalArgumentException("Invalid dialogId: " + i);
    }

    private class EditTypeAdapter extends ArrayAdapter<AccountType.EditType> {
        private boolean mHasCustomSelection;
        private final LayoutInflater mInflater;
        private int mSelectedIndex;
        private int mTextColorDark;
        private int mTextColorHintUnfocused;

        public EditTypeAdapter(Context context) {
            super(context, 0);
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mTextColorHintUnfocused = context.getResources().getColor(R.color.editor_disabled_text_color);
            this.mTextColorDark = context.getResources().getColor(R.color.primary_text_color);
            if (LabeledEditorView.this.mType != null && LabeledEditorView.this.mType.customColumn != null && LabeledEditorView.this.mEntry.getAsString(LabeledEditorView.this.mType.customColumn) != null) {
                add(LabeledEditorView.CUSTOM_SELECTION);
                this.mHasCustomSelection = true;
            }
            addAll(RawContactModifier.getValidTypes(LabeledEditorView.this.mState, LabeledEditorView.this.mKind, LabeledEditorView.this.mType, true, null, false));
        }

        public boolean hasCustomSelection() {
            return this.mHasCustomSelection;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TextView textViewCreateViewFromResource = createViewFromResource(i, view, viewGroup, R.layout.edit_simple_spinner_item);
            textViewCreateViewFromResource.setBackground(null);
            if (!LabeledEditorView.this.isEmpty()) {
                textViewCreateViewFromResource.setTextColor(this.mTextColorDark);
            } else {
                textViewCreateViewFromResource.setTextColor(this.mTextColorHintUnfocused);
            }
            return textViewCreateViewFromResource;
        }

        @Override
        public View getDropDownView(int i, View view, ViewGroup viewGroup) {
            CheckedTextView checkedTextView = (CheckedTextView) createViewFromResource(i, view, viewGroup, android.R.layout.simple_spinner_dropdown_item);
            checkedTextView.setBackground(getContext().getDrawable(R.drawable.drawer_item_background));
            checkedTextView.setChecked(i == this.mSelectedIndex);
            return checkedTextView;
        }

        private TextView createViewFromResource(int i, View view, ViewGroup viewGroup, int i2) {
            TextView textView;
            if (view == null) {
                textView = (TextView) this.mInflater.inflate(i2, viewGroup, false);
                textView.setTextSize(0, LabeledEditorView.this.getResources().getDimension(R.dimen.editor_form_text_size));
                textView.setTextColor(this.mTextColorDark);
            } else {
                textView = (TextView) view;
            }
            AccountType.EditType item = getItem(i);
            String customTypeLabel = GlobalEnv.getSimAasEditor().getCustomTypeLabel(item.rawValue, item.customColumn);
            if (customTypeLabel == null) {
                if (item == LabeledEditorView.CUSTOM_SELECTION) {
                    customTypeLabel = LabeledEditorView.this.mEntry.getAsString(LabeledEditorView.this.mType.customColumn);
                } else {
                    customTypeLabel = getContext().getString(item.labelRes);
                }
            }
            textView.setText(customTypeLabel);
            return textView;
        }

        public void setSelectedIndex(int i) {
            this.mSelectedIndex = i;
        }
    }

    public void updateValues() {
        if (this.mKind != null) {
            rebuildLabel();
        }
    }
}

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.editor.Editor;
import com.android.contacts.util.DialogManager;
import java.util.Iterator;
import java.util.List;

public abstract class LabeledEditorView extends LinearLayout implements Editor, DialogManager.DialogShowingView {
    public static final AccountType.EditType CUSTOM_SELECTION = new AccountType.EditType(0, 0);
    private ImageView mDelete;
    private View mDeleteContainer;
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
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LabeledEditorView.this.onTypeSelectionChange(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        init(context);
    }

    public LabeledEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mWasEmpty = true;
        this.mIsDeletable = true;
        this.mDialogManager = null;
        this.mSpinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LabeledEditorView.this.onTypeSelectionChange(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        init(context);
    }

    public LabeledEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mWasEmpty = true;
        this.mIsDeletable = true;
        this.mDialogManager = null;
        this.mSpinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LabeledEditorView.this.onTypeSelectionChange(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
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
        this.mDelete = (ImageView) findViewById(R.id.delete_button);
        this.mDeleteContainer = findViewById(R.id.delete_button_container);
        this.mDeleteContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    @Override
    public void deleteEditor() {
        this.mEntry.markDeleted();
        EditorAnimator.getInstance().removeEditorView(this);
    }

    public boolean isReadOnly() {
        return this.mReadOnly;
    }

    private void setupLabelButton(boolean shouldExist) {
        if (shouldExist) {
            this.mLabel.setEnabled(!this.mReadOnly && isEnabled());
            this.mLabel.setVisibility(0);
        } else {
            this.mLabel.setVisibility(8);
        }
    }

    private void setupDeleteButton() {
        boolean z = false;
        if (this.mIsDeletable) {
            this.mDeleteContainer.setVisibility(0);
            ImageView imageView = this.mDelete;
            if (!this.mReadOnly && isEnabled()) {
                z = true;
            }
            imageView.setEnabled(z);
            return;
        }
        this.mDeleteContainer.setVisibility(8);
    }

    public void setDeleteButtonVisible(boolean visible) {
        if (this.mIsDeletable) {
            this.mDeleteContainer.setVisibility(visible ? 0 : 4);
        }
    }

    protected void onOptionalFieldVisibilityChange() {
        if (this.mListener != null) {
            this.mListener.onRequest(5);
        }
    }

    @Override
    public void setEditorListener(Editor.EditorListener listener) {
        this.mListener = listener;
    }

    protected Editor.EditorListener getEditorListener() {
        return this.mListener;
    }

    @Override
    public void setDeletable(boolean deletable) {
        this.mIsDeletable = deletable;
        setupDeleteButton();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mLabel.setEnabled(!this.mReadOnly && enabled);
        this.mDelete.setEnabled(!this.mReadOnly && enabled);
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
        this.mEditTypeAdapter = new EditTypeAdapter(this.mContext);
        this.mLabel.setAdapter((SpinnerAdapter) this.mEditTypeAdapter);
        if (this.mEditTypeAdapter.hasCustomSelection()) {
            this.mLabel.setSelection(this.mEditTypeAdapter.getPosition(CUSTOM_SELECTION));
        } else {
            this.mLabel.setSelection(this.mEditTypeAdapter.getPosition(this.mType));
        }
    }

    public void onFieldChanged(String column, String value) {
        if (isFieldChanged(column, value)) {
            saveValue(column, value);
            notifyEditorListener();
            rebuildLabel();
        }
    }

    protected void saveValue(String column, String value) {
        this.mEntry.put(column, value);
    }

    protected final void updateEmptiness() {
        this.mWasEmpty = isEmpty();
    }

    protected void notifyEditorListener() {
        if (this.mListener != null) {
            this.mListener.onRequest(2);
        }
        boolean isEmpty = isEmpty();
        if (this.mWasEmpty != isEmpty) {
            if (isEmpty) {
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
            this.mWasEmpty = isEmpty;
        }
    }

    protected boolean isFieldChanged(String column, String value) {
        String dbValue = this.mEntry.getAsString(column);
        String dbValueNoNull = dbValue == null ? "" : dbValue;
        String valueNoNull = value == null ? "" : value;
        return !TextUtils.equals(dbValueNoNull, valueNoNull);
    }

    protected void rebuildValues() {
        setValues(this.mKind, this.mEntry, this.mState, this.mReadOnly, this.mViewIdGenerator);
    }

    public void setValues(DataKind kind, ValuesDelta entry, RawContactDelta state, boolean readOnly, ViewIdGenerator vig) {
        boolean z = false;
        this.mKind = kind;
        this.mEntry = entry;
        this.mState = state;
        this.mReadOnly = readOnly;
        this.mViewIdGenerator = vig;
        setId(vig.getId(state, kind, entry, -1));
        if (!entry.isVisible()) {
            setVisibility(8);
            return;
        }
        setVisibility(0);
        boolean hasTypes = RawContactModifier.hasEditTypes(kind);
        setupLabelButton(hasTypes);
        Spinner spinner = this.mLabel;
        if (!readOnly && isEnabled()) {
            z = true;
        }
        spinner.setEnabled(z);
        if (hasTypes) {
            this.mType = RawContactModifier.getCurrentType(entry, kind);
            rebuildLabel();
        }
    }

    public ValuesDelta getValues() {
        return this.mEntry;
    }

    private Dialog createCustomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
        LayoutInflater layoutInflater = LayoutInflater.from(builder.getContext());
        builder.setTitle(R.string.customLabelPickerTitle);
        View view = layoutInflater.inflate(R.layout.contact_editor_label_name_dialog, (ViewGroup) null);
        final EditText editText = (EditText) view.findViewById(R.id.custom_dialog_content);
        editText.setInputType(8193);
        editText.setSaveEnabled(true);
        builder.setView(view);
        editText.requestFocus();
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String customText = editText.getText().toString().trim();
                if (ContactsUtils.isGraphic(customText)) {
                    List<AccountType.EditType> allTypes = RawContactModifier.getValidTypes(LabeledEditorView.this.mState, LabeledEditorView.this.mKind, null);
                    LabeledEditorView.this.mType = null;
                    Iterator<AccountType.EditType> it = allTypes.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        AccountType.EditType editType = it.next();
                        if (editType.customColumn != null) {
                            LabeledEditorView.this.mType = editType;
                            break;
                        }
                    }
                    if (LabeledEditorView.this.mType != null) {
                        LabeledEditorView.this.mEntry.put(LabeledEditorView.this.mKind.typeColumn, LabeledEditorView.this.mType.rawValue);
                        LabeledEditorView.this.mEntry.put(LabeledEditorView.this.mType.customColumn, customText);
                        LabeledEditorView.this.rebuildLabel();
                        LabeledEditorView.this.requestFocusForFirstEditField();
                        LabeledEditorView.this.onLabelRebuilt();
                    }
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                LabeledEditorView.this.updateCustomDialogOkButtonState(dialog, editText);
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                LabeledEditorView.this.updateCustomDialogOkButtonState(dialog, editText);
            }
        });
        dialog.getWindow().setSoftInputMode(5);
        return dialog;
    }

    void updateCustomDialogOkButtonState(AlertDialog dialog, EditText editText) {
        Button okButton = dialog.getButton(-1);
        okButton.setEnabled(!TextUtils.isEmpty(editText.getText().toString().trim()));
    }

    protected void onLabelRebuilt() {
    }

    protected void onTypeSelectionChange(int position) {
        AccountType.EditType selected = this.mEditTypeAdapter.getItem(position);
        if (!this.mEditTypeAdapter.hasCustomSelection() || selected != CUSTOM_SELECTION) {
            if (this.mType != selected || this.mType.customColumn != null) {
                if (selected.customColumn != null) {
                    showDialog(1);
                    return;
                }
                this.mType = selected;
                this.mEntry.put(this.mKind.typeColumn, this.mType.rawValue);
                rebuildLabel();
                requestFocusForFirstEditField();
                onLabelRebuilt();
            }
        }
    }

    void showDialog(int bundleDialogId) {
        Bundle bundle = new Bundle();
        bundle.putInt("dialog_id", bundleDialogId);
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
        int dialogId = bundle.getInt("dialog_id");
        switch (dialogId) {
            case 1:
                return createCustomDialog();
            default:
                throw new IllegalArgumentException("Invalid dialogId: " + dialogId);
        }
    }

    private class EditTypeAdapter extends ArrayAdapter<AccountType.EditType> {
        private boolean mHasCustomSelection;
        private final LayoutInflater mInflater;
        private int mTextColorDark;
        private int mTextColorHintUnfocused;
        private int mTextColorSecondary;

        public EditTypeAdapter(Context context) {
            super(context, 0);
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mTextColorHintUnfocused = context.getResources().getColor(R.color.editor_disabled_text_color);
            this.mTextColorSecondary = context.getResources().getColor(R.color.secondary_text_color);
            this.mTextColorDark = context.getResources().getColor(R.color.primary_text_color);
            if (LabeledEditorView.this.mType != null && LabeledEditorView.this.mType.customColumn != null) {
                String customText = LabeledEditorView.this.mEntry.getAsString(LabeledEditorView.this.mType.customColumn);
                if (customText != null) {
                    add(LabeledEditorView.CUSTOM_SELECTION);
                    this.mHasCustomSelection = true;
                }
            }
            boolean isSimKind = LabeledEditorView.this.mKind.typeOverallMax == 2;
            addAll(RawContactModifier.getValidTypes(LabeledEditorView.this.mState, LabeledEditorView.this.mKind, LabeledEditorView.this.mType, isSimKind));
        }

        public boolean hasCustomSelection() {
            return this.mHasCustomSelection;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = createViewFromResource(position, convertView, parent, R.layout.edit_simple_spinner_item);
            view.setBackground(null);
            if (!LabeledEditorView.this.isEmpty()) {
                view.setTextColor(this.mTextColorDark);
            } else if (LabeledEditorView.this.hasFocus()) {
                view.setTextColor(this.mTextColorSecondary);
            } else {
                view.setTextColor(this.mTextColorHintUnfocused);
            }
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item);
        }

        private TextView createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
            TextView textView;
            String text;
            if (convertView == null) {
                textView = (TextView) this.mInflater.inflate(resource, parent, false);
                textView.setTextSize(0, LabeledEditorView.this.getResources().getDimension(R.dimen.editor_form_text_size));
                textView.setTextColor(this.mTextColorDark);
            } else {
                textView = (TextView) convertView;
            }
            AccountType.EditType type = getItem(position);
            if (type == LabeledEditorView.CUSTOM_SELECTION) {
                text = LabeledEditorView.this.mEntry.getAsString(LabeledEditorView.this.mType.customColumn);
            } else {
                text = getContext().getString(type.labelRes);
            }
            textView.setText(text);
            return textView;
        }
    }
}

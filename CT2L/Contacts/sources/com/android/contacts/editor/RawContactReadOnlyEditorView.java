package com.android.contacts.editor;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.dataitem.DataKind;
import java.util.ArrayList;

public class RawContactReadOnlyEditorView extends BaseRawContactEditorView implements View.OnClickListener {
    private TextView mAccountHeaderNameTextView;
    private TextView mAccountHeaderTypeTextView;
    private String mAccountName;
    private String mAccountType;
    private String mDataSet;
    private Button mEditExternallyButton;
    private ViewGroup mGeneral;
    private LayoutInflater mInflater;
    private TextView mName;
    private long mRawContactId;

    public RawContactReadOnlyEditorView(Context context) {
        super(context);
        this.mRawContactId = -1L;
    }

    public RawContactReadOnlyEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRawContactId = -1L;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mInflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        this.mName = (TextView) findViewById(R.id.read_only_name);
        this.mEditExternallyButton = (Button) findViewById(R.id.button_edit_externally);
        this.mEditExternallyButton.setOnClickListener(this);
        this.mGeneral = (ViewGroup) findViewById(R.id.sect_general);
        this.mAccountHeaderTypeTextView = (TextView) findViewById(R.id.account_type);
        this.mAccountHeaderNameTextView = (TextView) findViewById(R.id.account_name);
    }

    @Override
    public void setState(RawContactDelta state, AccountType type, ViewIdGenerator vig, boolean isProfile) {
        this.mGeneral.removeAllViews();
        if (state != null && type != null) {
            RawContactModifier.ensureKindExists(state, type, "vnd.android.cursor.item/name");
            this.mAccountName = state.getAccountName();
            this.mAccountType = state.getAccountType();
            this.mDataSet = state.getDataSet();
            if (isProfile) {
                if (TextUtils.isEmpty(this.mAccountName)) {
                    this.mAccountHeaderNameTextView.setVisibility(8);
                    this.mAccountHeaderTypeTextView.setText(R.string.local_profile_title);
                } else {
                    this.mAccountHeaderTypeTextView.setText(this.mContext.getString(R.string.external_profile_title, type.getDisplayLabel(this.mContext)));
                    this.mAccountHeaderNameTextView.setText(this.mAccountName);
                }
            } else {
                CharSequence accountType = type.getDisplayLabel(this.mContext);
                if (TextUtils.isEmpty(accountType)) {
                    accountType = this.mContext.getString(R.string.account_phone);
                }
                if (!TextUtils.isEmpty(this.mAccountName)) {
                    this.mAccountHeaderNameTextView.setVisibility(0);
                    this.mAccountHeaderNameTextView.setText(this.mContext.getString(R.string.from_account_format, this.mAccountName));
                } else {
                    this.mAccountHeaderNameTextView.setVisibility(8);
                }
                this.mAccountHeaderTypeTextView.setText(this.mContext.getString(R.string.account_type_format, accountType));
            }
            updateAccountHeaderContentDescription();
            this.mRawContactId = state.getRawContactId().longValue();
            DataKind kind = type.getKindForMimetype("vnd.android.cursor.item/photo");
            if (kind != null) {
                RawContactModifier.ensureKindExists(state, type, "vnd.android.cursor.item/photo");
                boolean hasPhotoEditor = type.getKindForMimetype("vnd.android.cursor.item/photo") != null;
                setHasPhotoEditor(hasPhotoEditor);
                getPhotoEditor().setValues(kind, state.getPrimaryEntry("vnd.android.cursor.item/photo"), state, !type.areContactsWritable(), vig);
            }
            ValuesDelta primary = state.getPrimaryEntry("vnd.android.cursor.item/name");
            this.mName.setText(primary != null ? primary.getAsString("data1") : this.mContext.getString(R.string.missing_name));
            if (type.getEditContactActivityClassName() != null) {
                this.mEditExternallyButton.setVisibility(0);
            } else {
                this.mEditExternallyButton.setVisibility(8);
            }
            Resources res = this.mContext.getResources();
            ArrayList<ValuesDelta> phones = state.getMimeEntries("vnd.android.cursor.item/phone_v2");
            Drawable phoneDrawable = getResources().getDrawable(R.drawable.ic_phone_24dp);
            String phoneContentDescription = res.getString(R.string.header_phone_entry);
            if (phones != null) {
                boolean isFirstPhoneBound = true;
                for (ValuesDelta phone : phones) {
                    String phoneNumber = phone.getPhoneNumber();
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        String formattedNumber = PhoneNumberUtils.formatNumber(phoneNumber, phone.getPhoneNormalizedNumber(), GeoUtil.getCurrentCountryIso(getContext()));
                        CharSequence phoneType = null;
                        if (phone.phoneHasType()) {
                            phoneType = ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, phone.getPhoneType(), phone.getPhoneLabel());
                        }
                        bindData(phoneDrawable, phoneContentDescription, formattedNumber, phoneType, isFirstPhoneBound, true);
                        isFirstPhoneBound = false;
                    }
                }
            }
            ArrayList<ValuesDelta> emails = state.getMimeEntries("vnd.android.cursor.item/email_v2");
            Drawable emailDrawable = getResources().getDrawable(R.drawable.ic_email_24dp);
            String emailContentDescription = res.getString(R.string.header_email_entry);
            if (emails != null) {
                boolean isFirstEmailBound = true;
                for (ValuesDelta email : emails) {
                    String emailAddress = email.getEmailData();
                    if (!TextUtils.isEmpty(emailAddress)) {
                        CharSequence emailType = null;
                        if (email.emailHasType()) {
                            emailType = ContactsContract.CommonDataKinds.Email.getTypeLabel(res, email.getEmailType(), email.getEmailLabel());
                        }
                        bindData(emailDrawable, emailContentDescription, emailAddress, emailType, isFirstEmailBound);
                        isFirstEmailBound = false;
                    }
                }
            }
            if (this.mGeneral.getChildCount() > 0) {
                this.mGeneral.setVisibility(0);
            } else {
                this.mGeneral.setVisibility(8);
            }
        }
    }

    private void bindData(Drawable icon, String iconContentDescription, CharSequence data, CharSequence type, boolean isFirstEntry) {
        bindData(icon, iconContentDescription, data, type, isFirstEntry, false);
    }

    private void bindData(Drawable icon, String iconContentDescription, CharSequence data, CharSequence type, boolean isFirstEntry, boolean forceLTR) {
        View field = this.mInflater.inflate(R.layout.item_read_only_field, this.mGeneral, false);
        if (isFirstEntry) {
            ImageView imageView = (ImageView) field.findViewById(R.id.kind_icon);
            imageView.setImageDrawable(icon);
            imageView.setContentDescription(iconContentDescription);
        } else {
            ImageView imageView2 = (ImageView) field.findViewById(R.id.kind_icon);
            imageView2.setVisibility(4);
            imageView2.setContentDescription(null);
        }
        TextView dataView = (TextView) field.findViewById(R.id.data);
        dataView.setText(data);
        if (forceLTR) {
            dataView.setTextDirection(3);
        }
        TextView typeView = (TextView) field.findViewById(R.id.type);
        if (!TextUtils.isEmpty(type)) {
            typeView.setText(type);
        } else {
            typeView.setVisibility(8);
        }
        this.mGeneral.addView(field);
    }

    @Override
    public long getRawContactId() {
        return this.mRawContactId;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_edit_externally && this.mListener != null) {
            this.mListener.onExternalEditorRequest(new AccountWithDataSet(this.mAccountName, this.mAccountType, this.mDataSet), ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, this.mRawContactId));
        }
    }
}

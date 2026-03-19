package com.android.contacts.editor;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import com.android.contacts.GeoUtil;
import com.android.contacts.R;
import com.android.contacts.compat.PhoneNumberUtilsCompat;
import com.android.contacts.editor.PhotoEditorView;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.AccountsListAdapter;
import com.android.contacts.util.MaterialColorMapUtils;
import com.android.contacts.util.UiClosables;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class RawContactEditorView extends LinearLayout implements View.OnClickListener {
    private View mAccountHeaderContainer;
    private ImageView mAccountHeaderExpanderIcon;
    private ImageView mAccountHeaderIcon;
    private TextView mAccountHeaderPrimaryText;
    private TextView mAccountHeaderSecondaryText;
    private ListPopupWindow mAccountSelectorPopup;
    private AccountTypeManager mAccountTypeManager;
    private List<AccountInfo> mAccounts;
    private RawContactDelta mCurrentRawContactDelta;
    private boolean mHasNewContact;
    private Bundle mIntentExtras;
    private boolean mIsExpanded;
    private boolean mIsUserProfile;
    private Map<String, KindSectionData> mKindSectionDataMap;
    private Map<String, KindSectionView> mKindSectionViewMap;
    private ViewGroup mKindSectionViews;
    private LayoutInflater mLayoutInflater;
    private Listener mListener;
    private MaterialColorMapUtils.MaterialPalette mMaterialPalette;
    private View mMoreFields;
    private ValuesDelta mPhotoValuesDelta;
    private PhotoEditorView mPhotoView;
    private AccountWithDataSet mPrimaryAccount;
    private RawContactDeltaList mRawContactDeltas;
    private long mRawContactIdToDisplayAlone;
    private Set<String> mSortedMimetypes;
    private ViewIdGenerator mViewIdGenerator;

    public interface Listener {
        void onBindEditorsFailed();

        void onEditorsBound();

        void onNameFieldChanged(long j, ValuesDelta valuesDelta);

        void onRebindEditorsForNewContact(RawContactDelta rawContactDelta, AccountWithDataSet accountWithDataSet, AccountWithDataSet accountWithDataSet2);
    }

    private static final class MimeTypeComparator implements Comparator<String> {
        private static final List<String> MIME_TYPE_ORDER = Arrays.asList("vnd.android.cursor.item/name", "vnd.android.cursor.item/nickname", "vnd.android.cursor.item/organization", "vnd.android.cursor.item/phone_v2", "vnd.android.cursor.item/sip_address", "vnd.android.cursor.item/email_v2", "vnd.android.cursor.item/postal-address_v2", "vnd.android.cursor.item/im", "vnd.android.cursor.item/website", "vnd.android.cursor.item/contact_event", "vnd.android.cursor.item/relation", "vnd.android.cursor.item/note", "vnd.android.cursor.item/group_membership");

        private MimeTypeComparator() {
        }

        @Override
        public int compare(String str, String str2) {
            if (str == str2) {
                return 0;
            }
            if (str == null) {
                return -1;
            }
            if (str2 == null) {
                return 1;
            }
            int iIndexOf = MIME_TYPE_ORDER.indexOf(str);
            int iIndexOf2 = MIME_TYPE_ORDER.indexOf(str2);
            if (iIndexOf < 0 && iIndexOf2 < 0) {
                return str.compareTo(str2);
            }
            if (iIndexOf < 0) {
                return 1;
            }
            if (iIndexOf2 < 0 || iIndexOf < iIndexOf2) {
                return -1;
            }
            return 1;
        }
    }

    public static class SavedState extends View.BaseSavedState {
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
        private boolean mIsExpanded;

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.mIsExpanded = parcel.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.mIsExpanded ? 1 : 0);
        }
    }

    public RawContactEditorView(Context context) {
        super(context);
        this.mAccounts = new ArrayList();
        this.mRawContactIdToDisplayAlone = -1L;
        this.mKindSectionDataMap = new HashMap();
        this.mSortedMimetypes = new TreeSet(new MimeTypeComparator());
        this.mKindSectionViewMap = new HashMap();
        this.mAccountSelectorPopup = null;
    }

    public RawContactEditorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mAccounts = new ArrayList();
        this.mRawContactIdToDisplayAlone = -1L;
        this.mKindSectionDataMap = new HashMap();
        this.mSortedMimetypes = new TreeSet(new MimeTypeComparator());
        this.mKindSectionViewMap = new HashMap();
        this.mAccountSelectorPopup = null;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAccountTypeManager = AccountTypeManager.getInstance(getContext());
        this.mLayoutInflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        this.mAccountHeaderContainer = findViewById(R.id.account_header_container);
        this.mAccountHeaderPrimaryText = (TextView) findViewById(R.id.account_type);
        this.mAccountHeaderSecondaryText = (TextView) findViewById(R.id.account_name);
        this.mAccountHeaderIcon = (ImageView) findViewById(R.id.account_type_icon);
        this.mAccountHeaderExpanderIcon = (ImageView) findViewById(R.id.account_expander_icon);
        this.mPhotoView = (PhotoEditorView) findViewById(R.id.photo_editor);
        this.mKindSectionViews = (LinearLayout) findViewById(R.id.kind_section_views);
        this.mMoreFields = findViewById(R.id.more_fields);
        this.mMoreFields.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.more_fields) {
            showAllFields();
        }
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        int childCount = this.mKindSectionViews.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mKindSectionViews.getChildAt(i).setEnabled(z);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Log.d("RawContactEditorView", "[onSaveInstanceState] mAccountSelectorPopup = " + this.mAccountSelectorPopup);
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mIsExpanded = this.mIsExpanded;
        UiClosables.closeQuietly(this.mAccountSelectorPopup);
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mIsExpanded = savedState.mIsExpanded;
        if (this.mIsExpanded) {
            showAllFields();
        }
    }

    public void setPhotoListener(PhotoEditorView.Listener listener) {
        this.mPhotoView.setListener(listener);
    }

    public void removePhoto() {
        this.mPhotoValuesDelta.setFromTemplate(true);
        this.mPhotoValuesDelta.put("data15", (byte[]) null);
        this.mPhotoValuesDelta.put("data14", (String) null);
        this.mPhotoView.removePhoto();
    }

    public void setFullSizePhoto(Uri uri) {
        this.mPhotoView.setFullSizedPhoto(uri);
    }

    public void updatePhoto(Uri uri) {
        this.mPhotoValuesDelta.setFromTemplate(false);
        unsetSuperPrimaryFromAllPhotos();
        this.mPhotoValuesDelta.setSuperPrimary(true);
        try {
            byte[] compressedThumbnailBitmapBytes = EditorUiUtils.getCompressedThumbnailBitmapBytes(getContext(), uri);
            if (compressedThumbnailBitmapBytes != null) {
                this.mPhotoValuesDelta.setPhoto(compressedThumbnailBitmapBytes);
            }
        } catch (FileNotFoundException e) {
            elog("Failed to get bitmap from photo Uri");
        }
        this.mPhotoView.setFullSizedPhoto(uri);
    }

    private void unsetSuperPrimaryFromAllPhotos() {
        ArrayList<ValuesDelta> mimeEntries;
        for (int i = 0; i < this.mRawContactDeltas.size(); i++) {
            if (this.mRawContactDeltas.get(i).hasMimeEntries("vnd.android.cursor.item/photo") && (mimeEntries = this.mRawContactDeltas.get(i).getMimeEntries("vnd.android.cursor.item/photo")) != null) {
                for (int i2 = 0; i2 < mimeEntries.size(); i2++) {
                    mimeEntries.get(i2).setSuperPrimary(false);
                }
            }
        }
    }

    public boolean isWritablePhotoSet() {
        return this.mPhotoView.isWritablePhotoSet();
    }

    public long getPhotoRawContactId() {
        if (this.mCurrentRawContactDelta == null) {
            return -1L;
        }
        return this.mCurrentRawContactDelta.getRawContactId().longValue();
    }

    public StructuredNameEditorView getNameEditorView() {
        KindSectionView kindSectionView = this.mKindSectionViewMap.get("vnd.android.cursor.item/name");
        if (kindSectionView == null) {
            return null;
        }
        return kindSectionView.getNameEditorView();
    }

    public TextFieldsEditorView getPhoneticEditorView() {
        KindSectionView kindSectionView = this.mKindSectionViewMap.get("vnd.android.cursor.item/name");
        if (kindSectionView == null) {
            return null;
        }
        return kindSectionView.getPhoneticEditorView();
    }

    public RawContactDelta getCurrentRawContactDelta() {
        return this.mCurrentRawContactDelta;
    }

    public View getAggregationAnchorView() {
        StructuredNameEditorView nameEditorView = getNameEditorView();
        if (nameEditorView != null) {
            return nameEditorView.findViewById(R.id.anchor_view);
        }
        return null;
    }

    public void setGroupMetaData(Cursor cursor) {
        KindSectionView kindSectionView = this.mKindSectionViewMap.get("vnd.android.cursor.item/group_membership");
        if (kindSectionView == null) {
            return;
        }
        kindSectionView.setGroupMetaData(cursor);
        if (this.mIsExpanded) {
            kindSectionView.setHideWhenEmpty(false);
            kindSectionView.updateEmptyEditors(true);
        }
    }

    public void setIntentExtras(Bundle bundle) {
        this.mIntentExtras = bundle;
    }

    public void setState(RawContactDeltaList rawContactDeltaList, MaterialColorMapUtils.MaterialPalette materialPalette, ViewIdGenerator viewIdGenerator, boolean z, boolean z2, AccountWithDataSet accountWithDataSet, long j) {
        StructuredNameEditorView nameEditorView;
        Log.sensitive("RawContactEditorView", "[setState] beg: rawContactDeltas=" + rawContactDeltaList + ", hasNewContact=" + z + ", isUserProfile=" + z2 + ", primaryAccount=" + accountWithDataSet + ", rawContactIdToDisplayAlone=" + j);
        this.mRawContactDeltas = rawContactDeltaList;
        this.mRawContactIdToDisplayAlone = j;
        this.mKindSectionViewMap.clear();
        this.mKindSectionViews.removeAllViews();
        this.mMoreFields.setVisibility(0);
        this.mMaterialPalette = materialPalette;
        this.mViewIdGenerator = viewIdGenerator;
        this.mHasNewContact = z;
        this.mIsUserProfile = z2;
        this.mPrimaryAccount = accountWithDataSet;
        restorePrimaryAccountIfHave(rawContactDeltaList);
        if (this.mPrimaryAccount == null && this.mAccounts != null) {
            this.mPrimaryAccount = ContactEditorUtils.create(getContext()).getOnlyOrDefaultAccount(AccountInfo.extractAccounts(this.mAccounts));
            Log.d("RawContactEditorView", "[setState] get only or default account ");
        }
        if (Log.isLoggable("RawContactEditorView", 2)) {
            Log.v("RawContactEditorView", "state: primary " + this.mPrimaryAccount);
        }
        if (rawContactDeltaList == null || rawContactDeltaList.isEmpty()) {
            elog("No raw contact deltas");
            if (this.mListener != null) {
                this.mListener.onBindEditorsFailed();
                return;
            }
            return;
        }
        pickRawContactDelta();
        if (this.mCurrentRawContactDelta == null) {
            elog("Couldn't pick a raw contact delta.");
            if (this.mListener != null) {
                this.mListener.onBindEditorsFailed();
                return;
            }
            return;
        }
        applyIntentExtras();
        parseRawContactDelta();
        if (this.mKindSectionDataMap.isEmpty()) {
            elog("No kind section data parsed from RawContactDelta(s)");
            if (this.mListener != null) {
                this.mListener.onBindEditorsFailed();
                return;
            }
            return;
        }
        KindSectionData kindSectionData = this.mKindSectionDataMap.get("vnd.android.cursor.item/name");
        if (kindSectionData != null) {
            RawContactDelta rawContactDelta = kindSectionData.getRawContactDelta();
            RawContactModifier.ensureKindExists(rawContactDelta, rawContactDelta.getAccountType(this.mAccountTypeManager), "vnd.android.cursor.item/name");
            RawContactModifier.ensureKindExists(rawContactDelta, rawContactDelta.getAccountType(this.mAccountTypeManager), "vnd.android.cursor.item/photo");
        }
        addPhotoView();
        setAccountInfo();
        if (isReadOnlyRawContact()) {
            addReadOnlyRawContactEditorViews();
        } else {
            setupEditorNormally();
            if (this.mHasNewContact && (nameEditorView = getNameEditorView()) != null) {
                nameEditorView.requestFocusForFirstEditField();
            }
        }
        if (this.mListener != null) {
            this.mListener.onEditorsBound();
        }
    }

    public void setAccounts(List<AccountInfo> list) {
        this.mAccounts.clear();
        this.mAccounts.addAll(list);
        setAccountInfo();
    }

    private void setupEditorNormally() {
        Log.d("RawContactEditorView", "[setupEditorNormally] beg");
        addKindSectionViews();
        this.mMoreFields.setVisibility(hasMoreFields() ? 0 : 8);
        if (this.mIsExpanded) {
            showAllFields();
        }
        Log.d("RawContactEditorView", "[setupEditorNormally] end");
    }

    private boolean isReadOnlyRawContact() {
        return !this.mCurrentRawContactDelta.getAccountType(this.mAccountTypeManager).areContactsWritable();
    }

    private void pickRawContactDelta() {
        Log.d("RawContactEditorView", "[pickRawContactDelta] beg");
        if (Log.isLoggable("RawContactEditorView", 2)) {
            Log.v("RawContactEditorView", "parse: " + this.mRawContactDeltas.size() + " rawContactDelta(s)");
        }
        for (int i = 0; i < this.mRawContactDeltas.size(); i++) {
            RawContactDelta rawContactDelta = this.mRawContactDeltas.get(i);
            if (Log.isLoggable("RawContactEditorView", 2)) {
                Log.v("RawContactEditorView", "parse: " + i + " rawContactDelta" + rawContactDelta);
            }
            if (rawContactDelta != null && rawContactDelta.isVisible()) {
                AccountType accountType = rawContactDelta.getAccountType(this.mAccountTypeManager);
                Log.d("RawContactEditorView", "[pickRawContactDelta] accountType:" + accountType);
                if (accountType == null) {
                    continue;
                } else if (this.mRawContactIdToDisplayAlone > 0) {
                    if (rawContactDelta.getRawContactId().equals(Long.valueOf(this.mRawContactIdToDisplayAlone))) {
                        this.mCurrentRawContactDelta = rawContactDelta;
                        Log.d("RawContactEditorView", "[pickRawContactDelta] end 1, mCurrentRawContactDelta" + this.mCurrentRawContactDelta);
                        return;
                    }
                } else {
                    if (this.mPrimaryAccount != null && this.mPrimaryAccount.equals(rawContactDelta.getAccountWithDataSet())) {
                        this.mCurrentRawContactDelta = rawContactDelta;
                        Log.d("RawContactEditorView", "[pickRawContactDelta] end 2, mCurrentRawContactDelta" + this.mCurrentRawContactDelta);
                        return;
                    }
                    if (accountType.areContactsWritable()) {
                        this.mCurrentRawContactDelta = rawContactDelta;
                    }
                }
            }
        }
        Log.d("RawContactEditorView", "[pickRawContactDelta] end 3, mCurrentRawContactDelta" + this.mCurrentRawContactDelta);
    }

    private void applyIntentExtras() {
        if (this.mIntentExtras == null || this.mIntentExtras.size() == 0) {
            return;
        }
        RawContactModifier.parseExtras(getContext(), this.mCurrentRawContactDelta.getAccountType(AccountTypeManager.getInstance(getContext())), this.mCurrentRawContactDelta, this.mIntentExtras);
        this.mIntentExtras = null;
    }

    private void parseRawContactDelta() {
        int size;
        this.mKindSectionDataMap.clear();
        this.mSortedMimetypes.clear();
        AccountType accountType = this.mCurrentRawContactDelta.getAccountType(this.mAccountTypeManager);
        Log.sensitive("RawContactEditorView", "[parseRawContactDelta] to constuct mKindSectionDataMap. beg, accountType=" + accountType);
        ArrayList<DataKind> sortedDataKinds = accountType.getSortedDataKinds();
        if (sortedDataKinds != null) {
            size = sortedDataKinds.size();
        } else {
            size = 0;
        }
        if (Log.isLoggable("RawContactEditorView", 2)) {
            Log.v("RawContactEditorView", "parse: " + size + " dataKinds(s)");
        }
        for (int i = 0; i < size; i++) {
            DataKind dataKind = sortedDataKinds.get(i);
            Log.sensitive("RawContactEditorView", "[parseRawContactDelta] i = " + i + ", dataKind = " + dataKind);
            if (dataKind == null || !dataKind.editable) {
                if (Log.isLoggable("RawContactEditorView", 2)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("parse: ");
                    sb.append(i);
                    sb.append(dataKind == null ? " dropped null data kind" : " dropped uneditable mimetype: " + dataKind.mimeType);
                    Log.v("RawContactEditorView", sb.toString());
                }
            } else {
                String str = dataKind.mimeType;
                if (DataKind.PSEUDO_MIME_TYPE_NAME.equals(str) || DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(str)) {
                    if (Log.isLoggable("RawContactEditorView", 2)) {
                        Log.v("RawContactEditorView", "parse: " + i + " " + dataKind.mimeType + " dropped pseudo type");
                    }
                } else if ("vnd.com.google.cursor.item/contact_user_defined_field".equals(str)) {
                    if (Log.isLoggable("RawContactEditorView", 2)) {
                        Log.v("RawContactEditorView", "parse: " + i + " " + dataKind.mimeType + " dropped custom field");
                    }
                } else {
                    KindSectionData kindSectionData = new KindSectionData(accountType, dataKind, this.mCurrentRawContactDelta);
                    this.mKindSectionDataMap.put(str, kindSectionData);
                    this.mSortedMimetypes.add(str);
                    if (Log.isLoggable("RawContactEditorView", 2)) {
                        Log.v("RawContactEditorView", "parse: " + i + " " + dataKind.mimeType + " " + kindSectionData.getValuesDeltas().size() + " value(s) " + kindSectionData.getNonEmptyValuesDeltas().size() + " non-empty value(s) " + kindSectionData.getVisibleValuesDeltas().size() + " visible value(s)");
                    }
                }
            }
        }
        Log.d("RawContactEditorView", "[parseRawContactDelta] to constuct mKindSectionDataMap. end");
    }

    private void addReadOnlyRawContactEditorViews() {
        CharSequence typeLabel;
        CharSequence typeLabel2;
        this.mKindSectionViews.removeAllViews();
        AccountType accountType = this.mCurrentRawContactDelta.getAccountType(AccountTypeManager.getInstance(getContext()));
        if (accountType == null) {
            return;
        }
        RawContactModifier.ensureKindExists(this.mCurrentRawContactDelta, accountType, "vnd.android.cursor.item/name");
        Context context = getContext();
        Resources resources = context.getResources();
        ValuesDelta primaryEntry = this.mCurrentRawContactDelta.getPrimaryEntry("vnd.android.cursor.item/name");
        bindData(context.getDrawable(R.drawable.quantum_ic_person_vd_theme_24), resources.getString(R.string.header_name_entry), primaryEntry != null ? primaryEntry.getAsString("data1") : getContext().getString(R.string.missing_name), null, true);
        ArrayList<ValuesDelta> mimeEntries = this.mCurrentRawContactDelta.getMimeEntries("vnd.android.cursor.item/phone_v2");
        Drawable drawable = context.getDrawable(R.drawable.quantum_ic_phone_vd_theme_24);
        String string = resources.getString(R.string.header_phone_entry);
        if (mimeEntries != null) {
            boolean z = true;
            for (ValuesDelta valuesDelta : mimeEntries) {
                String phoneNumber = valuesDelta.getPhoneNumber();
                if (!TextUtils.isEmpty(phoneNumber)) {
                    String number = PhoneNumberUtilsCompat.formatNumber(phoneNumber, valuesDelta.getPhoneNormalizedNumber(), GeoUtil.getCurrentCountryIso(getContext()));
                    if (!valuesDelta.hasPhoneType()) {
                        typeLabel2 = null;
                    } else {
                        typeLabel2 = ContactsContract.CommonDataKinds.Phone.getTypeLabel(resources, valuesDelta.getPhoneType().intValue(), valuesDelta.getPhoneLabel());
                    }
                    bindData(drawable, string, number, typeLabel2, z, true);
                    z = false;
                }
            }
        }
        ArrayList<ValuesDelta> mimeEntries2 = this.mCurrentRawContactDelta.getMimeEntries("vnd.android.cursor.item/email_v2");
        Drawable drawable2 = context.getDrawable(R.drawable.quantum_ic_email_vd_theme_24);
        String string2 = resources.getString(R.string.header_email_entry);
        if (mimeEntries2 != null) {
            boolean z2 = true;
            for (ValuesDelta valuesDelta2 : mimeEntries2) {
                String emailData = valuesDelta2.getEmailData();
                if (!TextUtils.isEmpty(emailData)) {
                    if (!valuesDelta2.hasEmailType()) {
                        typeLabel = null;
                    } else {
                        typeLabel = ContactsContract.CommonDataKinds.Email.getTypeLabel(resources, valuesDelta2.getEmailType().intValue(), valuesDelta2.getEmailLabel());
                    }
                    bindData(drawable2, string2, emailData, typeLabel, z2);
                    z2 = false;
                }
            }
        }
        this.mKindSectionViews.setVisibility(this.mKindSectionViews.getChildCount() <= 0 ? 8 : 0);
        this.mMoreFields.setVisibility(8);
    }

    private void bindData(Drawable drawable, String str, CharSequence charSequence, CharSequence charSequence2, boolean z) {
        bindData(drawable, str, charSequence, charSequence2, z, false);
    }

    private void bindData(Drawable drawable, String str, CharSequence charSequence, CharSequence charSequence2, boolean z, boolean z2) {
        View viewInflate = this.mLayoutInflater.inflate(R.layout.item_read_only_field, this.mKindSectionViews, false);
        if (z) {
            ImageView imageView = (ImageView) viewInflate.findViewById(R.id.kind_icon);
            imageView.setImageDrawable(drawable);
            imageView.setContentDescription(str);
        } else {
            ImageView imageView2 = (ImageView) viewInflate.findViewById(R.id.kind_icon);
            imageView2.setVisibility(4);
            imageView2.setContentDescription(null);
        }
        TextView textView = (TextView) viewInflate.findViewById(R.id.data);
        textView.setText(charSequence);
        if (z2) {
            textView.setTextDirection(3);
        }
        TextView textView2 = (TextView) viewInflate.findViewById(R.id.type);
        if (!TextUtils.isEmpty(charSequence2)) {
            textView2.setText(charSequence2);
        } else {
            textView2.setVisibility(8);
        }
        this.mKindSectionViews.addView(viewInflate);
    }

    private void setAccountInfo() {
        AccountInfo accountInfoForAccount;
        Log.d("RawContactEditorView", "[setAccountInfo] beg");
        if (this.mCurrentRawContactDelta == null && this.mPrimaryAccount == null) {
            return;
        }
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(getContext());
        if (this.mCurrentRawContactDelta != null) {
            accountInfoForAccount = accountTypeManager.getAccountInfoForAccount(this.mCurrentRawContactDelta.getAccountWithDataSet());
        } else {
            accountInfoForAccount = accountTypeManager.getAccountInfoForAccount(this.mPrimaryAccount);
        }
        if (this.mAccounts.isEmpty()) {
            this.mAccounts.add(accountInfoForAccount);
        }
        if (isReadOnlyRawContact()) {
            String string = accountInfoForAccount.getTypeLabel().toString();
            setAccountHeader(string, getResources().getString(R.string.editor_account_selector_read_only_title, string));
        } else {
            setAccountHeader(getResources().getString(R.string.editor_account_selector_title), getAccountLabel(accountInfoForAccount));
        }
        if (this.mHasNewContact && !this.mIsUserProfile && this.mAccounts.size() > 1) {
            addAccountSelector(this.mCurrentRawContactDelta);
        }
        Log.d("RawContactEditorView", "[setAccountInfo] end");
    }

    private void setAccountHeader(String str, String str2) {
        this.mAccountHeaderPrimaryText.setText(str);
        this.mAccountHeaderSecondaryText.setText(str2);
        this.mAccountHeaderIcon.setImageDrawable(this.mCurrentRawContactDelta.getRawContactAccountType(getContext()).getDisplayIcon(getContext()));
        this.mAccountHeaderContainer.setContentDescription(EditorUiUtils.getAccountInfoContentDescription(str2, str));
    }

    private void addAccountSelector(final RawContactDelta rawContactDelta) {
        this.mAccountHeaderExpanderIcon.setVisibility(0);
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccountWithDataSet accountWithDataSet = rawContactDelta.getAccountWithDataSet();
                AccountInfo.sortAccounts(accountWithDataSet, RawContactEditorView.this.mAccounts);
                final ListPopupWindow listPopupWindow = new ListPopupWindow(RawContactEditorView.this.getContext(), null);
                RawContactEditorView.this.mAccountSelectorPopup = listPopupWindow;
                Log.d("RawContactEditorView", "[click on AccountSelector] new mAccountSelectorPopup = " + RawContactEditorView.this.mAccountSelectorPopup);
                final AccountsListAdapter accountsListAdapter = new AccountsListAdapter(RawContactEditorView.this.getContext(), RawContactEditorView.this.mAccounts, accountWithDataSet);
                listPopupWindow.setWidth(RawContactEditorView.this.mAccountHeaderContainer.getWidth());
                listPopupWindow.setAnchorView(RawContactEditorView.this.mAccountHeaderContainer);
                listPopupWindow.setAdapter(accountsListAdapter);
                listPopupWindow.setModal(true);
                listPopupWindow.setInputMethodMode(2);
                listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view2, int i, long j) {
                        UiClosables.closeQuietly(listPopupWindow);
                        AccountWithDataSet item = accountsListAdapter.getItem(i);
                        if (RawContactEditorView.this.mListener != null && !RawContactEditorView.this.mPrimaryAccount.equals(item)) {
                            RawContactEditorView.this.mIsExpanded = false;
                            RawContactEditorView.this.mListener.onRebindEditorsForNewContact(rawContactDelta, RawContactEditorView.this.mPrimaryAccount, item);
                        }
                    }
                });
                listPopupWindow.show();
            }
        };
        this.mAccountHeaderContainer.setOnClickListener(onClickListener);
        this.mAccountHeaderExpanderIcon.setOnClickListener(onClickListener);
    }

    private void addPhotoView() {
        Log.d("RawContactEditorView", "[addPhotoView] beg");
        if (!this.mCurrentRawContactDelta.hasMimeEntries("vnd.android.cursor.item/photo")) {
            wlog("No photo mimetype for this raw contact.");
            this.mPhotoView.setVisibility(8);
            return;
        }
        this.mPhotoView.setVisibility(0);
        ValuesDelta superPrimaryEntry = this.mCurrentRawContactDelta.getSuperPrimaryEntry("vnd.android.cursor.item/photo");
        if (superPrimaryEntry == null) {
            Log.wtf("RawContactEditorView", "addPhotoView: no ValueDelta found for current RawContactDeltathat supports a photo.");
            this.mPhotoView.setVisibility(8);
            return;
        }
        this.mPhotoView.setPalette(this.mMaterialPalette);
        this.mPhotoView.setPhoto(superPrimaryEntry);
        if (isReadOnlyRawContact()) {
            this.mPhotoView.setReadOnly(true);
            return;
        }
        this.mPhotoView.setReadOnly(false);
        this.mPhotoValuesDelta = superPrimaryEntry;
        boolean zIsIccCardAccount = this.mCurrentRawContactDelta.getAccountType(this.mAccountTypeManager).isIccCardAccount();
        Log.d("RawContactEditorView", "[addPhotoView] isIccAccount: " + zIsIccCardAccount);
        if (zIsIccCardAccount) {
            this.mPhotoView.setSelectPhotoEnable(false);
        } else {
            this.mPhotoView.setSelectPhotoEnable(true);
        }
        Log.d("RawContactEditorView", "[addPhotoView] end");
    }

    private void addKindSectionViews() {
        Log.d("RawContactEditorView", "[addKindSectionViews] beg.");
        int i = -1;
        for (String str : this.mSortedMimetypes) {
            i++;
            if ("vnd.android.cursor.item/photo".equals(str)) {
                if (Log.isLoggable("RawContactEditorView", 2)) {
                    Log.v("RawContactEditorView", "kind: " + i + " " + str + " dropped");
                }
            } else {
                KindSectionView kindSectionViewInflateKindSectionView = inflateKindSectionView(this.mKindSectionViews, this.mKindSectionDataMap.get(str), str);
                this.mKindSectionViews.addView(kindSectionViewInflateKindSectionView);
                this.mKindSectionViewMap.put(str, kindSectionViewInflateKindSectionView);
            }
        }
        Log.d("RawContactEditorView", "[addKindSectionViews] end");
    }

    private KindSectionView inflateKindSectionView(ViewGroup viewGroup, KindSectionData kindSectionData, String str) {
        Log.d("RawContactEditorView", "[inflateKindSectionView] beg, mimeType=" + str + ", kindSectionData.mDataKind=" + kindSectionData.getDataKind() + ", kindSectionData.mRawContactDelta=" + kindSectionData.getRawContactDelta());
        KindSectionView kindSectionView = (KindSectionView) this.mLayoutInflater.inflate(R.layout.item_kind_section, viewGroup, false);
        kindSectionView.setIsUserProfile(this.mIsUserProfile);
        if ("vnd.android.cursor.item/phone_v2".equals(str) || "vnd.android.cursor.item/email_v2".equals(str)) {
            kindSectionView.setHideWhenEmpty(false);
        }
        kindSectionView.setShowOneEmptyEditor(true);
        kindSectionView.setState(kindSectionData, this.mViewIdGenerator, this.mListener);
        Log.d("RawContactEditorView", "[inflateKindSectionView] end");
        return kindSectionView;
    }

    private void showAllFields() {
        Log.sensitive("RawContactEditorView", "[showAllFields] count: " + this.mKindSectionViews.getChildCount() + ", PrimaryAccount: " + this.mPrimaryAccount + ",mIsUserProfile: " + this.mIsUserProfile);
        for (int i = 0; i < this.mKindSectionViews.getChildCount(); i++) {
            KindSectionView kindSectionView = (KindSectionView) this.mKindSectionViews.getChildAt(i);
            kindSectionView.setHideWhenEmpty(false);
            kindSectionView.setIsCurrentIccAccount((this.mIsUserProfile || this.mPrimaryAccount == null || !AccountTypeUtils.isAccountTypeIccCard(this.mPrimaryAccount.type)) ? false : true);
            kindSectionView.updateEmptyEditors(true);
        }
        this.mIsExpanded = true;
        this.mMoreFields.setVisibility(8);
    }

    private boolean hasMoreFields() {
        Iterator<KindSectionView> it = this.mKindSectionViewMap.values().iterator();
        while (it.hasNext()) {
            if (it.next().getVisibility() != 0) {
                return true;
            }
        }
        return false;
    }

    private static void wlog(String str) {
        if (Log.isLoggable("RawContactEditorView", 5)) {
            Log.w("RawContactEditorView", str);
        }
    }

    private static void elog(String str) {
        Log.e("RawContactEditorView", str);
    }

    private void restorePrimaryAccountIfHave(RawContactDeltaList rawContactDeltaList) {
        Log.d("RawContactEditorView", "[restorePrimaryAccountIfHave] mIsUserProfile: " + this.mIsUserProfile);
        if (!this.mIsUserProfile && rawContactDeltaList != null && rawContactDeltaList.size() > 0 && this.mPrimaryAccount == null) {
            RawContactDelta rawContactDelta = rawContactDeltaList.get(0);
            if (rawContactDelta == null) {
                Log.i("RawContactEditorView", "[restorePrimaryAccountIfHave] contactDelta is null,return");
                return;
            }
            String accountName = rawContactDelta.getAccountName();
            String accountType = rawContactDelta.getAccountType();
            String dataSet = rawContactDelta.getDataSet();
            Log.sensitive("RawContactEditorView", "[restorePrimaryAccountIfHave] accountName :" + accountName + ",accountType:" + accountType + ",dataset: " + dataSet);
            if (accountName == null || accountType == null) {
                Log.d("RawContactEditorView", "[restorePrimaryAccountIfHave] accountName or accontType is null,return");
                return;
            }
            if (AccountTypeUtils.isAccountTypeIccCard(accountType)) {
                this.mPrimaryAccount = new AccountWithDataSetEx(accountName, accountType, dataSet, AccountTypeUtils.getSubIdBySimAccountName(getContext(), accountName));
            } else {
                this.mPrimaryAccount = new AccountWithDataSet(accountName, accountType, dataSet);
            }
            Log.d("RawContactEditorView", "[restorePrimaryAccountIfHave] update PrimaryAccount success!,mPrimaryAccount: " + this.mPrimaryAccount);
        }
    }

    private String getAccountLabel(AccountInfo accountInfo) {
        if (this.mIsUserProfile) {
            return EditorUiUtils.getAccountHeaderLabelForMyProfile(getContext(), accountInfo);
        }
        String displaynameUsingSubId = null;
        if (AccountTypeUtils.isAccountTypeIccCard(accountInfo.getType().accountType) && accountInfo.getAccount().name != null) {
            displaynameUsingSubId = SubInfoUtils.getDisplaynameUsingSubId(AccountTypeUtils.getSubIdBySimAccountName(getContext(), accountInfo.getAccount().name));
        }
        if (displaynameUsingSubId != null) {
            return displaynameUsingSubId;
        }
        return accountInfo.getNameLabel().toString();
    }
}

package com.android.contacts.editor;

import android.text.TextUtils;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataKind;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class KindSectionData {
    private final AccountType mAccountType;
    private final DataKind mDataKind;
    private final RawContactDelta mRawContactDelta;

    public KindSectionData(AccountType accountType, DataKind dataKind, RawContactDelta rawContactDelta) {
        this.mAccountType = accountType;
        this.mDataKind = dataKind;
        this.mRawContactDelta = rawContactDelta;
    }

    public AccountType getAccountType() {
        return this.mAccountType;
    }

    public List<ValuesDelta> getValuesDeltas() {
        ArrayList<ValuesDelta> mimeEntries = this.mRawContactDelta.getMimeEntries(this.mDataKind.mimeType);
        return mimeEntries == null ? new ArrayList() : mimeEntries;
    }

    public List<ValuesDelta> getVisibleValuesDeltas() {
        ArrayList arrayList = new ArrayList();
        for (ValuesDelta valuesDelta : getValuesDeltas()) {
            if (valuesDelta.isVisible() && !valuesDelta.isDelete()) {
                arrayList.add(valuesDelta);
            }
        }
        return arrayList;
    }

    public List<ValuesDelta> getNonEmptyValuesDeltas() {
        ArrayList arrayList = new ArrayList();
        for (ValuesDelta valuesDelta : getValuesDeltas()) {
            if (!isEmpty(valuesDelta)) {
                arrayList.add(valuesDelta);
            }
        }
        return arrayList;
    }

    private boolean isEmpty(ValuesDelta valuesDelta) {
        if (this.mDataKind.fieldList != null) {
            Iterator<AccountType.EditField> it = this.mDataKind.fieldList.iterator();
            while (it.hasNext()) {
                if (!TextUtils.isEmpty(valuesDelta.getAsString(it.next().column))) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    public DataKind getDataKind() {
        return this.mDataKind;
    }

    public RawContactDelta getRawContactDelta() {
        return this.mRawContactDelta;
    }

    public String getMimeType() {
        return this.mDataKind.mimeType;
    }
}

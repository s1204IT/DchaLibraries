package com.android.contacts.common.model.dataitem;

import android.content.ContentValues;
import com.android.contacts.common.model.account.AccountType;
import com.google.common.collect.Iterators;
import java.text.SimpleDateFormat;
import java.util.List;

public final class DataKind {
    public AccountType.StringInflater actionAltHeader;
    public AccountType.StringInflater actionBody;
    public AccountType.StringInflater actionHeader;
    public SimpleDateFormat dateFormatWithYear;
    public SimpleDateFormat dateFormatWithoutYear;
    public ContentValues defaultValues;
    public boolean editable;
    public List<AccountType.EditField> fieldList;
    public int iconAltDescriptionRes;
    public int iconAltRes;
    public int maxLinesForDisplay;
    public String mimeType;
    public String resourcePackageName;
    public int titleRes;
    public String typeColumn;
    public List<AccountType.EditType> typeList;
    public int typeOverallMax;
    public int weight;

    public DataKind() {
        this.maxLinesForDisplay = 1;
    }

    public DataKind(String mimeType, int titleRes, int weight, boolean editable) {
        this.mimeType = mimeType;
        this.titleRes = titleRes;
        this.weight = weight;
        this.editable = editable;
        this.typeOverallMax = -1;
        this.maxLinesForDisplay = 1;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DataKind:");
        sb.append(" resPackageName=").append(this.resourcePackageName);
        sb.append(" mimeType=").append(this.mimeType);
        sb.append(" titleRes=").append(this.titleRes);
        sb.append(" iconAltRes=").append(this.iconAltRes);
        sb.append(" iconAltDescriptionRes=").append(this.iconAltDescriptionRes);
        sb.append(" weight=").append(this.weight);
        sb.append(" editable=").append(this.editable);
        sb.append(" actionHeader=").append(this.actionHeader);
        sb.append(" actionAltHeader=").append(this.actionAltHeader);
        sb.append(" actionBody=").append(this.actionBody);
        sb.append(" typeColumn=").append(this.typeColumn);
        sb.append(" typeOverallMax=").append(this.typeOverallMax);
        sb.append(" typeList=").append(toString(this.typeList));
        sb.append(" fieldList=").append(toString(this.fieldList));
        sb.append(" defaultValues=").append(this.defaultValues);
        sb.append(" dateFormatWithoutYear=").append(toString(this.dateFormatWithoutYear));
        sb.append(" dateFormatWithYear=").append(toString(this.dateFormatWithYear));
        return sb.toString();
    }

    public static String toString(SimpleDateFormat format) {
        return format == null ? "(null)" : format.toPattern();
    }

    public static String toString(Iterable<?> list) {
        return list == null ? "(null)" : Iterators.toString(list.iterator());
    }
}

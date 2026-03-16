package com.android.contacts.common.model.account;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import com.android.contacts.R;
import com.android.contacts.common.model.dataitem.DataKind;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public abstract class AccountType {
    private static Comparator<DataKind> sWeightComparator = new Comparator<DataKind>() {
        @Override
        public int compare(DataKind object1, DataKind object2) {
            return object1.weight - object2.weight;
        }
    };
    public int iconRes;
    protected boolean mIsInitialized;
    public String resourcePackageName;
    public String syncAdapterPackageName;
    public int titleRes;
    public String accountType = null;
    public String dataSet = null;
    private ArrayList<DataKind> mKinds = Lists.newArrayList();
    private HashMap<String, DataKind> mMimeKinds = Maps.newHashMap();

    public interface StringInflater {
        CharSequence inflateUsing(Context context, ContentValues contentValues);
    }

    public abstract boolean areContactsWritable();

    public abstract boolean isGroupMembershipEditable();

    protected static class DefinitionException extends Exception {
        public DefinitionException(String message) {
            super(message);
        }

        public DefinitionException(String message, Exception inner) {
            super(message, inner);
        }
    }

    public final boolean isInitialized() {
        return this.mIsInitialized;
    }

    public boolean isEmbedded() {
        return true;
    }

    public boolean isExtension() {
        return false;
    }

    public String getEditContactActivityClassName() {
        return null;
    }

    public String getCreateContactActivityClassName() {
        return null;
    }

    public String getInviteContactActivityClassName() {
        return null;
    }

    public String getViewContactNotifyServiceClassName() {
        return null;
    }

    public String getViewContactNotifyServicePackageName() {
        return this.syncAdapterPackageName;
    }

    public String getViewGroupActivity() {
        return null;
    }

    public CharSequence getDisplayLabel(Context context) {
        return getResourceText(context, this.syncAdapterPackageName, this.titleRes, this.accountType);
    }

    protected int getViewGroupLabelResId() {
        return -1;
    }

    public AccountTypeWithDataSet getAccountTypeAndDataSet() {
        return AccountTypeWithDataSet.get(this.accountType, this.dataSet);
    }

    public List<String> getExtensionPackageNames() {
        return new ArrayList();
    }

    public CharSequence getViewGroupLabel(Context context) {
        CharSequence customTitle = getResourceText(context, this.syncAdapterPackageName, getViewGroupLabelResId(), null);
        return customTitle == null ? context.getText(R.string.view_updates_from_group) : customTitle;
    }

    static CharSequence getResourceText(Context context, String packageName, int resId, String defaultValue) {
        if (resId != -1 && packageName != null) {
            PackageManager pm = context.getPackageManager();
            return pm.getText(packageName, resId, null);
        }
        if (resId != -1) {
            return context.getText(resId);
        }
        return defaultValue;
    }

    public Drawable getDisplayIcon(Context context) {
        if (this.titleRes != -1 && this.syncAdapterPackageName != null) {
            PackageManager pm = context.getPackageManager();
            return pm.getDrawable(this.syncAdapterPackageName, this.iconRes, null);
        }
        if (this.titleRes != -1) {
            return context.getResources().getDrawable(this.iconRes);
        }
        return null;
    }

    public ArrayList<DataKind> getSortedDataKinds() {
        Collections.sort(this.mKinds, sWeightComparator);
        return this.mKinds;
    }

    public DataKind getKindForMimetype(String mimeType) {
        return this.mMimeKinds.get(mimeType);
    }

    public DataKind addKind(DataKind kind) throws DefinitionException {
        if (kind.mimeType == null) {
            throw new DefinitionException("null is not a valid mime type");
        }
        if (this.mMimeKinds.get(kind.mimeType) != null) {
            throw new DefinitionException("mime type '" + kind.mimeType + "' is already registered");
        }
        kind.resourcePackageName = this.resourcePackageName;
        this.mKinds.add(kind);
        this.mMimeKinds.put(kind.mimeType, kind);
        return kind;
    }

    public static class EditType {
        public String customColumn;
        public int labelRes;
        public int rawValue;
        public boolean secondary;
        public int specificMax = -1;

        public EditType(int rawValue, int labelRes) {
            this.rawValue = rawValue;
            this.labelRes = labelRes;
        }

        public EditType setSecondary(boolean secondary) {
            this.secondary = secondary;
            return this;
        }

        public EditType setSpecificMax(int specificMax) {
            this.specificMax = specificMax;
            return this;
        }

        public EditType setCustomColumn(String customColumn) {
            this.customColumn = customColumn;
            return this;
        }

        public boolean equals(Object object) {
            if (!(object instanceof EditType)) {
                return false;
            }
            EditType other = (EditType) object;
            return other.rawValue == this.rawValue;
        }

        public int hashCode() {
            return this.rawValue;
        }

        public String toString() {
            return getClass().getSimpleName() + " rawValue=" + this.rawValue + " labelRes=" + this.labelRes + " secondary=" + this.secondary + " specificMax=" + this.specificMax + " customColumn=" + this.customColumn;
        }
    }

    public static class EventEditType extends EditType {
        private boolean mYearOptional;

        public EventEditType(int rawValue, int labelRes) {
            super(rawValue, labelRes);
        }

        public boolean isYearOptional() {
            return this.mYearOptional;
        }

        public EventEditType setYearOptional(boolean yearOptional) {
            this.mYearOptional = yearOptional;
            return this;
        }

        @Override
        public String toString() {
            return super.toString() + " mYearOptional=" + this.mYearOptional;
        }
    }

    public static final class EditField {
        public String column;
        public int inputType;
        public boolean longForm;
        public int minLines;
        public boolean optional;
        public boolean shortForm;
        public int titleRes;

        public EditField(String column, int titleRes) {
            this.column = column;
            this.titleRes = titleRes;
        }

        public EditField(String column, int titleRes, int inputType) {
            this(column, titleRes);
            this.inputType = inputType;
        }

        public EditField setOptional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public EditField setShortForm(boolean shortForm) {
            this.shortForm = shortForm;
            return this;
        }

        public EditField setLongForm(boolean longForm) {
            this.longForm = longForm;
            return this;
        }

        public String toString() {
            return getClass().getSimpleName() + ": column=" + this.column + " titleRes=" + this.titleRes + " inputType=" + this.inputType + " minLines=" + this.minLines + " optional=" + this.optional + " shortForm=" + this.shortForm + " longForm=" + this.longForm;
        }
    }
}

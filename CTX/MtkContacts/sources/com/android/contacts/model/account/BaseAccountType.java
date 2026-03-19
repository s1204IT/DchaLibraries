package com.android.contacts.model.account;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import com.android.contacts.R;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.CommonDateUtils;
import com.android.contacts.util.ContactDisplayUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class BaseAccountType extends AccountType {
    protected static final int FLAGS_EMAIL = 33;
    protected static final int FLAGS_EVENT = 1;
    protected static final int FLAGS_GENERIC_NAME = 8193;
    protected static final int FLAGS_NOTE = 147457;
    protected static final int FLAGS_PERSON_NAME = 8289;
    protected static final int FLAGS_PHONE = 3;
    protected static final int FLAGS_PHONETIC = 193;
    protected static final int FLAGS_POSTAL = 139377;
    protected static final int FLAGS_RELATION = 8289;
    protected static final int FLAGS_SIP_ADDRESS = 33;
    protected static final int FLAGS_WEBSITE = 17;
    protected static final int MAX_LINES_FOR_GROUP = 10;
    protected static final int MAX_LINES_FOR_NOTE = 100;
    protected static final int MAX_LINES_FOR_POSTAL_ADDRESS = 10;
    public static final AccountType.StringInflater ORGANIZATION_BODY_INFLATER = new AccountType.StringInflater() {
        @Override
        public CharSequence inflateUsing(Context context, ContentValues contentValues) {
            String asString;
            if (contentValues.containsKey("data1")) {
                asString = contentValues.getAsString("data1");
            } else {
                asString = null;
            }
            String asString2 = contentValues.containsKey("data4") ? contentValues.getAsString("data4") : null;
            if (asString != null && asString2 != null) {
                return ((Object) asString) + ": " + ((Object) asString2);
            }
            if (asString == null) {
                return asString2;
            }
            return asString;
        }
    };
    private static final String TAG = "BaseAccountType";

    private interface Attr {
        public static final String DATE_WITH_TIME = "dateWithTime";
        public static final String KIND = "kind";
        public static final String MAX_OCCURRENCE = "maxOccurs";
        public static final String TYPE = "type";
        public static final String YEAR_OPTIONAL = "yearOptional";
    }

    private interface Tag {
        public static final String DATA_KIND = "DataKind";
        public static final String TYPE = "Type";
    }

    protected interface Weight {
        public static final int EMAIL = 15;
        public static final int EVENT = 120;
        public static final int GROUP_MEMBERSHIP = 150;
        public static final int IM = 140;
        public static final int NICKNAME = 111;
        public static final int NONE = -1;
        public static final int NOTE = 130;
        public static final int ORGANIZATION = 125;
        public static final int PHONE = 10;
        public static final int RELATIONSHIP = 999;
        public static final int SIP_ADDRESS = 145;
        public static final int STRUCTURED_POSTAL = 25;
        public static final int WEBSITE = 160;
    }

    public BaseAccountType() {
        this.accountType = null;
        this.dataSet = null;
        this.titleRes = R.string.account_phone;
        this.iconRes = R.mipmap.ic_contacts_launcher;
    }

    protected static AccountType.EditType buildPhoneType(int i) {
        return new AccountType.EditType(i, ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(i));
    }

    protected static AccountType.EditType buildEmailType(int i) {
        return new AccountType.EditType(i, ContactsContract.CommonDataKinds.Email.getTypeLabelResource(i));
    }

    protected static AccountType.EditType buildPostalType(int i) {
        return new AccountType.EditType(i, ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabelResource(i));
    }

    protected static AccountType.EditType buildImType(int i) {
        return new AccountType.EditType(i, ContactsContract.CommonDataKinds.Im.getProtocolLabelResource(i));
    }

    protected static AccountType.EditType buildEventType(int i, boolean z) {
        return new AccountType.EventEditType(i, ContactsContract.CommonDataKinds.Event.getTypeResource(Integer.valueOf(i))).setYearOptional(z);
    }

    protected static AccountType.EditType buildRelationType(int i) {
        return new AccountType.EditType(i, ContactsContract.CommonDataKinds.Relation.getTypeLabelResource(i));
    }

    protected DataKind addDataKindStructuredName(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/name", R.string.nameLabelsGroup, -1, true));
        dataKindAddKind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data4", R.string.name_prefix, 8289).setLongForm(true));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289).setLongForm(true));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data6", R.string.name_suffix, 8289).setLongForm(true));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data9", R.string.name_phonetic_family, FLAGS_PHONETIC));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data8", R.string.name_phonetic_middle, FLAGS_PHONETIC));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data7", R.string.name_phonetic_given, FLAGS_PHONETIC));
        return dataKindAddKind;
    }

    protected DataKind addDataKindName(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_NAME, R.string.nameLabelsGroup, -1, true));
        dataKindAddKind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.fieldList = Lists.newArrayList();
        boolean z = context.getResources().getBoolean(R.bool.config_editor_field_order_primary);
        dataKindAddKind.fieldList.add(new AccountType.EditField("data4", R.string.name_prefix, 8289).setOptional(true));
        if (!z) {
            dataKindAddKind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289).setPhoneticsColumn("data9"));
            dataKindAddKind.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289).setOptional(true).setPhoneticsColumn("data8"));
            dataKindAddKind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289).setPhoneticsColumn("data7"));
        } else {
            dataKindAddKind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289).setPhoneticsColumn("data7"));
            dataKindAddKind.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289).setOptional(true).setPhoneticsColumn("data8"));
            dataKindAddKind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289).setPhoneticsColumn("data9"));
        }
        dataKindAddKind.fieldList.add(new AccountType.EditField("data6", R.string.name_suffix, 8289).setOptional(true));
        return dataKindAddKind;
    }

    protected DataKind addDataKindPhoneticName(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME, R.string.name_phonetic, -1, true));
        dataKindAddKind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data9", R.string.name_phonetic_family, FLAGS_PHONETIC));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data8", R.string.name_phonetic_middle, FLAGS_PHONETIC));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data7", R.string.name_phonetic_given, FLAGS_PHONETIC));
        return dataKindAddKind;
    }

    protected DataKind addDataKindNickname(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/nickname", R.string.nicknameLabelsGroup, Weight.NICKNAME, true));
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.actionHeader = new SimpleInflater(R.string.nicknameLabelsGroup);
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.defaultValues = new ContentValues();
        dataKindAddKind.defaultValues.put("data2", (Integer) 1);
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.nicknameLabelsGroup, 8289));
        return dataKindAddKind;
    }

    protected DataKind addDataKindPhone(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/phone_v2", R.string.phoneLabelsGroup, 10, true));
        dataKindAddKind.iconAltRes = R.drawable.quantum_ic_message_vd_theme_24;
        dataKindAddKind.iconAltDescriptionRes = R.string.sms;
        dataKindAddKind.actionHeader = new PhoneActionInflater();
        dataKindAddKind.actionAltHeader = new PhoneActionAltInflater();
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.typeColumn = "data2";
        dataKindAddKind.typeList = Lists.newArrayList();
        dataKindAddKind.typeList.add(buildPhoneType(2));
        dataKindAddKind.typeList.add(buildPhoneType(1));
        dataKindAddKind.typeList.add(buildPhoneType(3));
        dataKindAddKind.typeList.add(buildPhoneType(4).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(5).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(6).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(7));
        dataKindAddKind.typeList.add(buildPhoneType(0).setSecondary(true).setCustomColumn("data3"));
        dataKindAddKind.typeList.add(buildPhoneType(8).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(9).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(10).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(11).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(12).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(13).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(14).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(15).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(16).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(FLAGS_WEBSITE).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(18).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(19).setSecondary(true));
        dataKindAddKind.typeList.add(buildPhoneType(20).setSecondary(true));
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.phoneLabelsGroup, 3));
        return dataKindAddKind;
    }

    protected DataKind addDataKindEmail(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/email_v2", R.string.emailLabelsGroup, 15, true));
        dataKindAddKind.actionHeader = new EmailActionInflater();
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.typeColumn = "data2";
        dataKindAddKind.typeList = Lists.newArrayList();
        dataKindAddKind.typeList.add(buildEmailType(1));
        dataKindAddKind.typeList.add(buildEmailType(2));
        dataKindAddKind.typeList.add(buildEmailType(3));
        dataKindAddKind.typeList.add(buildEmailType(4));
        dataKindAddKind.typeList.add(buildEmailType(0).setSecondary(true).setCustomColumn("data3"));
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.emailLabelsGroup, 33));
        return dataKindAddKind;
    }

    protected DataKind addDataKindStructuredPostal(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/postal-address_v2", R.string.postalLabelsGroup, 25, true));
        dataKindAddKind.actionHeader = new PostalActionInflater();
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.typeColumn = "data2";
        dataKindAddKind.typeList = Lists.newArrayList();
        dataKindAddKind.typeList.add(buildPostalType(1));
        dataKindAddKind.typeList.add(buildPostalType(2));
        dataKindAddKind.typeList.add(buildPostalType(3));
        dataKindAddKind.typeList.add(buildPostalType(0).setSecondary(true).setCustomColumn("data3"));
        dataKindAddKind.fieldList = Lists.newArrayList();
        AccountTypeUtils.setStructuredPostalFiledList(dataKindAddKind, FLAGS_POSTAL);
        dataKindAddKind.maxLinesForDisplay = 10;
        return dataKindAddKind;
    }

    protected DataKind addDataKindIm(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/im", R.string.imLabelsGroup, Weight.IM, true));
        dataKindAddKind.actionHeader = new ImActionInflater();
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.defaultValues = new ContentValues();
        dataKindAddKind.defaultValues.put("data2", (Integer) 3);
        dataKindAddKind.typeColumn = "data5";
        dataKindAddKind.typeList = Lists.newArrayList();
        dataKindAddKind.typeList.add(buildImType(0));
        dataKindAddKind.typeList.add(buildImType(1));
        dataKindAddKind.typeList.add(buildImType(2));
        dataKindAddKind.typeList.add(buildImType(3));
        dataKindAddKind.typeList.add(buildImType(4));
        dataKindAddKind.typeList.add(buildImType(5));
        dataKindAddKind.typeList.add(buildImType(6));
        dataKindAddKind.typeList.add(buildImType(7));
        dataKindAddKind.typeList.add(buildImType(-1).setSecondary(true).setCustomColumn("data6"));
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.imLabelsGroup, 33));
        return dataKindAddKind;
    }

    protected DataKind addDataKindOrganization(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/organization", R.string.organizationLabelsGroup, Weight.ORGANIZATION, true));
        dataKindAddKind.actionHeader = new SimpleInflater(R.string.organizationLabelsGroup);
        dataKindAddKind.actionBody = ORGANIZATION_BODY_INFLATER;
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.ghostData_company, FLAGS_GENERIC_NAME));
        dataKindAddKind.fieldList.add(new AccountType.EditField("data4", R.string.ghostData_title, FLAGS_GENERIC_NAME));
        return dataKindAddKind;
    }

    protected DataKind addDataKindPhoto(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/photo", -1, -1, true));
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data15", -1, -1));
        return dataKindAddKind;
    }

    protected DataKind addDataKindNote(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/note", R.string.label_notes, Weight.NOTE, true));
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.actionHeader = new SimpleInflater(R.string.label_notes);
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.label_notes, FLAGS_NOTE));
        dataKindAddKind.maxLinesForDisplay = 100;
        return dataKindAddKind;
    }

    protected DataKind addDataKindWebsite(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/website", R.string.websiteLabelsGroup, Weight.WEBSITE, true));
        dataKindAddKind.actionHeader = new SimpleInflater(R.string.websiteLabelsGroup);
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.defaultValues = new ContentValues();
        dataKindAddKind.defaultValues.put("data2", (Integer) 7);
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.websiteLabelsGroup, FLAGS_WEBSITE));
        return dataKindAddKind;
    }

    protected DataKind addDataKindSipAddress(Context context) throws AccountType.DefinitionException {
        if (!AccountTypeUtils.isAccountTypeSipSupport(context)) {
            return null;
        }
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/sip_address", R.string.label_sip_address, Weight.SIP_ADDRESS, true));
        dataKindAddKind.actionHeader = new SimpleInflater(R.string.label_sip_address);
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", R.string.label_sip_address, 33));
        dataKindAddKind.typeOverallMax = 1;
        return dataKindAddKind;
    }

    protected DataKind addDataKindGroupMembership(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.android.cursor.item/group_membership", R.string.groupsLabel, Weight.GROUP_MEMBERSHIP, true));
        dataKindAddKind.typeOverallMax = 1;
        dataKindAddKind.actionHeader = new SimpleInflater(R.string.groupsLabel);
        dataKindAddKind.actionBody = new SimpleInflater("data1");
        dataKindAddKind.fieldList = Lists.newArrayList();
        dataKindAddKind.fieldList.add(new AccountType.EditField("data1", -1, -1));
        dataKindAddKind.maxLinesForDisplay = 10;
        return dataKindAddKind;
    }

    protected DataKind addDataKindCustomField(Context context) throws AccountType.DefinitionException {
        DataKind dataKindAddKind = addKind(new DataKind("vnd.com.google.cursor.item/contact_user_defined_field", R.string.label_custom_field, -1, false));
        dataKindAddKind.actionBody = new SimpleInflater("data2");
        return dataKindAddKind;
    }

    public static class SimpleInflater implements AccountType.StringInflater {
        private final String mColumnName;
        private final int mStringRes;

        public SimpleInflater(int i) {
            this(i, null);
        }

        public SimpleInflater(String str) {
            this(-1, str);
        }

        public SimpleInflater(int i, String str) {
            this.mStringRes = i;
            this.mColumnName = str;
        }

        @Override
        public CharSequence inflateUsing(Context context, ContentValues contentValues) {
            boolean zContainsKey = contentValues.containsKey(this.mColumnName);
            boolean z = this.mStringRes > 0;
            CharSequence text = z ? context.getText(this.mStringRes) : null;
            String asString = zContainsKey ? contentValues.getAsString(this.mColumnName) : null;
            if (z && zContainsKey) {
                return String.format(text.toString(), asString);
            }
            if (z) {
                return text;
            }
            if (zContainsKey) {
                return asString;
            }
            return null;
        }

        public String toString() {
            return getClass().getSimpleName() + " mStringRes=" + this.mStringRes + " mColumnName" + this.mColumnName;
        }

        public String getColumnNameForTest() {
            return this.mColumnName;
        }
    }

    public static abstract class CommonInflater implements AccountType.StringInflater {
        protected abstract int getTypeLabelResource(Integer num);

        protected boolean isCustom(Integer num) {
            return num.intValue() == 0;
        }

        protected String getTypeColumn() {
            return "data2";
        }

        protected String getLabelColumn() {
            return "data3";
        }

        protected CharSequence getTypeLabel(Resources resources, Integer num, CharSequence charSequence) {
            int typeLabelResource = getTypeLabelResource(num);
            if (num == null) {
                return resources.getText(typeLabelResource);
            }
            if (isCustom(num)) {
                Object[] objArr = new Object[1];
                if (charSequence == null) {
                    charSequence = "";
                }
                objArr[0] = charSequence;
                return resources.getString(typeLabelResource, objArr);
            }
            return resources.getText(typeLabelResource);
        }

        @Override
        public CharSequence inflateUsing(Context context, ContentValues contentValues) {
            return getTypeLabel(context.getResources(), contentValues.getAsInteger(getTypeColumn()), contentValues.getAsString(getLabelColumn()));
        }

        public String toString() {
            return getClass().getSimpleName();
        }
    }

    public static class PhoneActionInflater extends CommonInflater {
        @Override
        protected boolean isCustom(Integer num) {
            return ContactDisplayUtils.isCustomPhoneType(num);
        }

        @Override
        protected int getTypeLabelResource(Integer num) {
            return ContactDisplayUtils.getPhoneLabelResourceId(num);
        }
    }

    public static class PhoneActionAltInflater extends CommonInflater {
        @Override
        protected boolean isCustom(Integer num) {
            return ContactDisplayUtils.isCustomPhoneType(num);
        }

        @Override
        protected int getTypeLabelResource(Integer num) {
            return ContactDisplayUtils.getSmsLabelResourceId(num);
        }
    }

    public static class EmailActionInflater extends CommonInflater {
        @Override
        protected int getTypeLabelResource(Integer num) {
            if (num == null) {
                return R.string.email;
            }
            switch (num.intValue()) {
                case 1:
                    return R.string.email_home;
                case 2:
                    return R.string.email_work;
                case 3:
                    return R.string.email_other;
                case CompatUtils.TYPE_ASSERT:
                    return R.string.email_mobile;
                default:
                    return R.string.email_custom;
            }
        }
    }

    public static class EventActionInflater extends CommonInflater {
        @Override
        protected int getTypeLabelResource(Integer num) {
            return ContactsContract.CommonDataKinds.Event.getTypeResource(num);
        }
    }

    public static class RelationActionInflater extends CommonInflater {
        @Override
        protected int getTypeLabelResource(Integer num) {
            return ContactsContract.CommonDataKinds.Relation.getTypeLabelResource(num == null ? 0 : num.intValue());
        }
    }

    public static class PostalActionInflater extends CommonInflater {
        @Override
        protected int getTypeLabelResource(Integer num) {
            if (num == null) {
                return R.string.map_other;
            }
            switch (num.intValue()) {
            }
            return R.string.map_other;
        }
    }

    public static class ImActionInflater extends CommonInflater {
        @Override
        protected String getTypeColumn() {
            return "data5";
        }

        @Override
        protected String getLabelColumn() {
            return "data6";
        }

        @Override
        protected int getTypeLabelResource(Integer num) {
            if (num == null) {
                return R.string.chat;
            }
            switch (num.intValue()) {
            }
            return R.string.chat;
        }
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return false;
    }

    protected final void parseEditSchema(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    int depth2 = xmlPullParser.getDepth();
                    if (next == 2 && depth2 == depth + 1) {
                        String name = xmlPullParser.getName();
                        if (Tag.DATA_KIND.equals(name)) {
                            Iterator<DataKind> it = KindParser.INSTANCE.parseDataKindTag(context, xmlPullParser, attributeSet).iterator();
                            while (it.hasNext()) {
                                addKind(it.next());
                            }
                        } else {
                            Log.w(TAG, "Skipping unknown tag " + name);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private static boolean getAttr(AttributeSet attributeSet, String str, boolean z) {
        return attributeSet.getAttributeBooleanValue(null, str, z);
    }

    private static int getAttr(AttributeSet attributeSet, String str, int i) {
        return attributeSet.getAttributeIntValue(null, str, i);
    }

    private static String getAttr(AttributeSet attributeSet, String str) {
        return attributeSet.getAttributeValue(null, str);
    }

    private static class KindParser {
        public static final KindParser INSTANCE = new KindParser();
        private final Map<String, KindBuilder> mBuilders = Maps.newHashMap();

        private KindParser() {
            addBuilder(new NameKindBuilder());
            addBuilder(new NicknameKindBuilder());
            addBuilder(new PhoneKindBuilder());
            addBuilder(new EmailKindBuilder());
            addBuilder(new StructuredPostalKindBuilder());
            addBuilder(new ImKindBuilder());
            addBuilder(new OrganizationKindBuilder());
            addBuilder(new PhotoKindBuilder());
            addBuilder(new NoteKindBuilder());
            addBuilder(new WebsiteKindBuilder());
            addBuilder(new SipAddressKindBuilder());
            addBuilder(new GroupMembershipKindBuilder());
            addBuilder(new EventKindBuilder());
            addBuilder(new RelationshipKindBuilder());
        }

        private void addBuilder(KindBuilder kindBuilder) {
            this.mBuilders.put(kindBuilder.getTagName(), kindBuilder);
        }

        public List<DataKind> parseDataKindTag(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            String attr = BaseAccountType.getAttr(attributeSet, Attr.KIND);
            KindBuilder kindBuilder = this.mBuilders.get(attr);
            if (kindBuilder != null) {
                return kindBuilder.parseDataKind(context, xmlPullParser, attributeSet);
            }
            throw new AccountType.DefinitionException("Undefined data kind '" + attr + "'");
        }
    }

    private static abstract class KindBuilder {
        public abstract String getTagName();

        public abstract List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException;

        private KindBuilder() {
        }

        protected final DataKind newDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet, boolean z, String str, String str2, int i, int i2, AccountType.StringInflater stringInflater, AccountType.StringInflater stringInflater2) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            if (Log.isLoggable(BaseAccountType.TAG, 3)) {
                Log.d(BaseAccountType.TAG, "Adding DataKind: " + str);
            }
            DataKind dataKind = new DataKind(str, i, i2, true);
            dataKind.typeColumn = str2;
            dataKind.actionHeader = stringInflater;
            dataKind.actionBody = stringInflater2;
            dataKind.fieldList = Lists.newArrayList();
            if (!z) {
                dataKind.typeOverallMax = BaseAccountType.getAttr(attributeSet, Attr.MAX_OCCURRENCE, -1);
                if (dataKind.typeColumn != null) {
                    dataKind.typeList = Lists.newArrayList();
                    parseTypes(context, xmlPullParser, attributeSet, dataKind, true);
                    if (dataKind.typeList.size() == 0) {
                        throw new AccountType.DefinitionException("Kind " + dataKind.mimeType + " must have at least one type");
                    }
                } else {
                    parseTypes(context, xmlPullParser, attributeSet, dataKind, false);
                }
            }
            return dataKind;
        }

        private void parseTypes(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet, DataKind dataKind, boolean z) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            int depth = xmlPullParser.getDepth();
            while (true) {
                int next = xmlPullParser.next();
                if (next != 1) {
                    if (next != 3 || xmlPullParser.getDepth() > depth) {
                        int depth2 = xmlPullParser.getDepth();
                        if (next == 2 && depth2 == depth + 1) {
                            String name = xmlPullParser.getName();
                            if (Tag.TYPE.equals(name)) {
                                if (z) {
                                    dataKind.typeList.add(parseTypeTag(xmlPullParser, attributeSet, dataKind));
                                } else {
                                    throw new AccountType.DefinitionException("Kind " + dataKind.mimeType + " can't have types");
                                }
                            } else {
                                throw new AccountType.DefinitionException("Unknown tag: " + name);
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        private AccountType.EditType parseTypeTag(XmlPullParser xmlPullParser, AttributeSet attributeSet, DataKind dataKind) throws AccountType.DefinitionException {
            String attr = BaseAccountType.getAttr(attributeSet, Attr.TYPE);
            AccountType.EditType editTypeBuildEditTypeForTypeTag = buildEditTypeForTypeTag(attributeSet, attr);
            if (editTypeBuildEditTypeForTypeTag != null) {
                editTypeBuildEditTypeForTypeTag.specificMax = BaseAccountType.getAttr(attributeSet, Attr.MAX_OCCURRENCE, -1);
                return editTypeBuildEditTypeForTypeTag;
            }
            throw new AccountType.DefinitionException("Undefined type '" + attr + "' for data kind '" + dataKind.mimeType + "'");
        }

        protected AccountType.EditType buildEditTypeForTypeTag(AttributeSet attributeSet, String str) {
            return null;
        }

        protected final void throwIfList(DataKind dataKind) throws AccountType.DefinitionException {
            if (dataKind.typeOverallMax != 1) {
                throw new AccountType.DefinitionException("Kind " + dataKind.mimeType + " must have 'overallMax=\"1\"'");
            }
        }
    }

    private static class NameKindBuilder extends KindBuilder {
        private NameKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "name";
        }

        private static void checkAttributeTrue(boolean z, String str) throws AccountType.DefinitionException {
            if (!z) {
                throw new AccountType.DefinitionException(str + " must be true");
            }
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            boolean z = context.getResources().getBoolean(R.bool.config_editor_field_order_primary);
            boolean attr = BaseAccountType.getAttr(attributeSet, "supportsPrefix", false);
            boolean attr2 = BaseAccountType.getAttr(attributeSet, "supportsMiddleName", false);
            boolean attr3 = BaseAccountType.getAttr(attributeSet, "supportsSuffix", false);
            boolean attr4 = BaseAccountType.getAttr(attributeSet, "supportsPhoneticFamilyName", false);
            boolean attr5 = BaseAccountType.getAttr(attributeSet, "supportsPhoneticMiddleName", false);
            boolean attr6 = BaseAccountType.getAttr(attributeSet, "supportsPhoneticGivenName", false);
            checkAttributeTrue(attr, "supportsPrefix");
            checkAttributeTrue(attr2, "supportsMiddleName");
            checkAttributeTrue(attr3, "supportsSuffix");
            checkAttributeTrue(attr4, "supportsPhoneticFamilyName");
            checkAttributeTrue(attr5, "supportsPhoneticMiddleName");
            checkAttributeTrue(attr6, "supportsPhoneticGivenName");
            ArrayList arrayListNewArrayList = Lists.newArrayList();
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/name", null, R.string.nameLabelsGroup, -1, new SimpleInflater(R.string.nameLabelsGroup), new SimpleInflater("data1"));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data4", R.string.name_prefix, 8289).setLongForm(true));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289).setLongForm(true));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data6", R.string.name_suffix, 8289).setLongForm(true));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data9", R.string.name_phonetic_family, BaseAccountType.FLAGS_PHONETIC));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data8", R.string.name_phonetic_middle, BaseAccountType.FLAGS_PHONETIC));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data7", R.string.name_phonetic_given, BaseAccountType.FLAGS_PHONETIC));
            throwIfList(dataKindNewDataKind);
            arrayListNewArrayList.add(dataKindNewDataKind);
            DataKind dataKindNewDataKind2 = newDataKind(context, xmlPullParser, attributeSet, true, DataKind.PSEUDO_MIME_TYPE_NAME, null, R.string.nameLabelsGroup, -1, new SimpleInflater(R.string.nameLabelsGroup), new SimpleInflater("data1"));
            dataKindNewDataKind2.typeOverallMax = 1;
            throwIfList(dataKindNewDataKind2);
            arrayListNewArrayList.add(dataKindNewDataKind2);
            dataKindNewDataKind2.fieldList.add(new AccountType.EditField("data4", R.string.name_prefix, 8289).setOptional(true));
            if (!z) {
                dataKindNewDataKind2.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
                dataKindNewDataKind2.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289).setOptional(true));
                dataKindNewDataKind2.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
            } else {
                dataKindNewDataKind2.fieldList.add(new AccountType.EditField("data2", R.string.name_given, 8289));
                dataKindNewDataKind2.fieldList.add(new AccountType.EditField("data5", R.string.name_middle, 8289).setOptional(true));
                dataKindNewDataKind2.fieldList.add(new AccountType.EditField("data3", R.string.name_family, 8289));
            }
            dataKindNewDataKind2.fieldList.add(new AccountType.EditField("data6", R.string.name_suffix, 8289).setOptional(true));
            DataKind dataKindNewDataKind3 = newDataKind(context, xmlPullParser, attributeSet, true, DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME, null, R.string.name_phonetic, -1, new SimpleInflater(R.string.nameLabelsGroup), new SimpleInflater("data1"));
            dataKindNewDataKind3.typeOverallMax = 1;
            arrayListNewArrayList.add(dataKindNewDataKind3);
            dataKindNewDataKind3.fieldList.add(new AccountType.EditField("data9", R.string.name_phonetic_family, BaseAccountType.FLAGS_PHONETIC));
            dataKindNewDataKind3.fieldList.add(new AccountType.EditField("data8", R.string.name_phonetic_middle, BaseAccountType.FLAGS_PHONETIC));
            dataKindNewDataKind3.fieldList.add(new AccountType.EditField("data7", R.string.name_phonetic_given, BaseAccountType.FLAGS_PHONETIC));
            return arrayListNewArrayList;
        }
    }

    private static class NicknameKindBuilder extends KindBuilder {
        private NicknameKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "nickname";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/nickname", null, R.string.nicknameLabelsGroup, Weight.NICKNAME, new SimpleInflater(R.string.nicknameLabelsGroup), new SimpleInflater("data1"));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.nicknameLabelsGroup, 8289));
            dataKindNewDataKind.defaultValues = new ContentValues();
            dataKindNewDataKind.defaultValues.put("data2", (Integer) 1);
            throwIfList(dataKindNewDataKind);
            return Lists.newArrayList(dataKindNewDataKind);
        }
    }

    private static class PhoneKindBuilder extends KindBuilder {
        private PhoneKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "phone";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/phone_v2", "data2", R.string.phoneLabelsGroup, 10, new PhoneActionInflater(), new SimpleInflater("data1"));
            dataKindNewDataKind.iconAltRes = R.drawable.quantum_ic_message_vd_theme_24;
            dataKindNewDataKind.iconAltDescriptionRes = R.string.sms;
            dataKindNewDataKind.actionAltHeader = new PhoneActionAltInflater();
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.phoneLabelsGroup, 3));
            return Lists.newArrayList(dataKindNewDataKind);
        }

        protected static AccountType.EditType build(int i, boolean z) {
            return new AccountType.EditType(i, ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(i)).setSecondary(z);
        }

        @Override
        protected AccountType.EditType buildEditTypeForTypeTag(AttributeSet attributeSet, String str) {
            if ("home".equals(str)) {
                return build(1, false);
            }
            if ("mobile".equals(str)) {
                return build(2, false);
            }
            if ("work".equals(str)) {
                return build(3, false);
            }
            if ("fax_work".equals(str)) {
                return build(4, true);
            }
            if ("fax_home".equals(str)) {
                return build(5, true);
            }
            if ("pager".equals(str)) {
                return build(6, true);
            }
            if ("other".equals(str)) {
                return build(7, false);
            }
            if ("callback".equals(str)) {
                return build(8, true);
            }
            if ("car".equals(str)) {
                return build(9, true);
            }
            if ("company_main".equals(str)) {
                return build(10, true);
            }
            if ("isdn".equals(str)) {
                return build(11, true);
            }
            if ("main".equals(str)) {
                return build(12, true);
            }
            if ("other_fax".equals(str)) {
                return build(13, true);
            }
            if ("radio".equals(str)) {
                return build(14, true);
            }
            if ("telex".equals(str)) {
                return build(15, true);
            }
            if ("tty_tdd".equals(str)) {
                return build(16, true);
            }
            if ("work_mobile".equals(str)) {
                return build(BaseAccountType.FLAGS_WEBSITE, true);
            }
            if ("work_pager".equals(str)) {
                return build(18, true);
            }
            if ("assistant".equals(str)) {
                return build(19, true);
            }
            if ("mms".equals(str)) {
                return build(20, true);
            }
            if ("custom".equals(str)) {
                return build(0, true).setCustomColumn("data3");
            }
            return null;
        }
    }

    private static class EmailKindBuilder extends KindBuilder {
        private EmailKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "email";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/email_v2", "data2", R.string.emailLabelsGroup, 15, new EmailActionInflater(), new SimpleInflater("data1"));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.emailLabelsGroup, 33));
            return Lists.newArrayList(dataKindNewDataKind);
        }

        @Override
        protected AccountType.EditType buildEditTypeForTypeTag(AttributeSet attributeSet, String str) {
            if ("home".equals(str)) {
                return BaseAccountType.buildEmailType(1);
            }
            if ("work".equals(str)) {
                return BaseAccountType.buildEmailType(2);
            }
            if ("other".equals(str)) {
                return BaseAccountType.buildEmailType(3);
            }
            if ("mobile".equals(str)) {
                return BaseAccountType.buildEmailType(4);
            }
            if ("custom".equals(str)) {
                return BaseAccountType.buildEmailType(0).setSecondary(true).setCustomColumn("data3");
            }
            return null;
        }
    }

    private static class StructuredPostalKindBuilder extends KindBuilder {
        private StructuredPostalKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "postal";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/postal-address_v2", "data2", R.string.postalLabelsGroup, 25, new PostalActionInflater(), new SimpleInflater("data1"));
            if (BaseAccountType.getAttr(attributeSet, "needsStructured", false)) {
                if (Locale.JAPANESE.getLanguage().equals(Locale.getDefault().getLanguage())) {
                    dataKindNewDataKind.fieldList.add(new AccountType.EditField("data10", R.string.postal_country, BaseAccountType.FLAGS_POSTAL).setOptional(true));
                    dataKindNewDataKind.fieldList.add(new AccountType.EditField("data9", R.string.postal_postcode, BaseAccountType.FLAGS_POSTAL));
                    dataKindNewDataKind.fieldList.add(new AccountType.EditField("data8", R.string.postal_region, BaseAccountType.FLAGS_POSTAL));
                    dataKindNewDataKind.fieldList.add(new AccountType.EditField("data7", R.string.postal_city, BaseAccountType.FLAGS_POSTAL));
                    dataKindNewDataKind.fieldList.add(new AccountType.EditField("data4", R.string.postal_street, BaseAccountType.FLAGS_POSTAL));
                } else {
                    dataKindNewDataKind.fieldList.add(new AccountType.EditField("data4", R.string.postal_street, BaseAccountType.FLAGS_POSTAL));
                    dataKindNewDataKind.fieldList.add(new AccountType.EditField("data7", R.string.postal_city, BaseAccountType.FLAGS_POSTAL));
                    dataKindNewDataKind.fieldList.add(new AccountType.EditField("data8", R.string.postal_region, BaseAccountType.FLAGS_POSTAL));
                    dataKindNewDataKind.fieldList.add(new AccountType.EditField("data9", R.string.postal_postcode, BaseAccountType.FLAGS_POSTAL));
                    dataKindNewDataKind.fieldList.add(new AccountType.EditField("data10", R.string.postal_country, BaseAccountType.FLAGS_POSTAL).setOptional(true));
                }
            } else {
                dataKindNewDataKind.maxLinesForDisplay = 10;
                dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.postal_address, BaseAccountType.FLAGS_POSTAL));
            }
            return Lists.newArrayList(dataKindNewDataKind);
        }

        @Override
        protected AccountType.EditType buildEditTypeForTypeTag(AttributeSet attributeSet, String str) {
            if ("home".equals(str)) {
                return BaseAccountType.buildPostalType(1);
            }
            if ("work".equals(str)) {
                return BaseAccountType.buildPostalType(2);
            }
            if ("other".equals(str)) {
                return BaseAccountType.buildPostalType(3);
            }
            if ("custom".equals(str)) {
                return BaseAccountType.buildPostalType(0).setSecondary(true).setCustomColumn("data3");
            }
            return null;
        }
    }

    private static class ImKindBuilder extends KindBuilder {
        private ImKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "im";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/im", "data5", R.string.imLabelsGroup, Weight.IM, new ImActionInflater(), new SimpleInflater("data1"));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.imLabelsGroup, 33));
            dataKindNewDataKind.defaultValues = new ContentValues();
            dataKindNewDataKind.defaultValues.put("data2", (Integer) 3);
            return Lists.newArrayList(dataKindNewDataKind);
        }

        @Override
        protected AccountType.EditType buildEditTypeForTypeTag(AttributeSet attributeSet, String str) {
            if ("aim".equals(str)) {
                return BaseAccountType.buildImType(0);
            }
            if ("msn".equals(str)) {
                return BaseAccountType.buildImType(1);
            }
            if ("yahoo".equals(str)) {
                return BaseAccountType.buildImType(2);
            }
            if ("skype".equals(str)) {
                return BaseAccountType.buildImType(3);
            }
            if ("qq".equals(str)) {
                return BaseAccountType.buildImType(4);
            }
            if ("google_talk".equals(str)) {
                return BaseAccountType.buildImType(5);
            }
            if ("icq".equals(str)) {
                return BaseAccountType.buildImType(6);
            }
            if ("jabber".equals(str)) {
                return BaseAccountType.buildImType(7);
            }
            if ("custom".equals(str)) {
                return BaseAccountType.buildImType(-1).setSecondary(true).setCustomColumn("data6");
            }
            return null;
        }
    }

    private static class OrganizationKindBuilder extends KindBuilder {
        private OrganizationKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "organization";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/organization", null, R.string.organizationLabelsGroup, Weight.ORGANIZATION, new SimpleInflater(R.string.organizationLabelsGroup), BaseAccountType.ORGANIZATION_BODY_INFLATER);
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.ghostData_company, BaseAccountType.FLAGS_GENERIC_NAME));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data4", R.string.ghostData_title, BaseAccountType.FLAGS_GENERIC_NAME));
            throwIfList(dataKindNewDataKind);
            return Lists.newArrayList(dataKindNewDataKind);
        }
    }

    private static class PhotoKindBuilder extends KindBuilder {
        private PhotoKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "photo";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/photo", null, -1, -1, null, null);
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data15", -1, -1));
            throwIfList(dataKindNewDataKind);
            return Lists.newArrayList(dataKindNewDataKind);
        }
    }

    private static class NoteKindBuilder extends KindBuilder {
        private NoteKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "note";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/note", null, R.string.label_notes, Weight.NOTE, new SimpleInflater(R.string.label_notes), new SimpleInflater("data1"));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.label_notes, BaseAccountType.FLAGS_NOTE));
            dataKindNewDataKind.maxLinesForDisplay = 100;
            throwIfList(dataKindNewDataKind);
            return Lists.newArrayList(dataKindNewDataKind);
        }
    }

    private static class WebsiteKindBuilder extends KindBuilder {
        private WebsiteKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "website";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/website", null, R.string.websiteLabelsGroup, Weight.WEBSITE, new SimpleInflater(R.string.websiteLabelsGroup), new SimpleInflater("data1"));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.websiteLabelsGroup, BaseAccountType.FLAGS_WEBSITE));
            dataKindNewDataKind.defaultValues = new ContentValues();
            dataKindNewDataKind.defaultValues.put("data2", (Integer) 7);
            return Lists.newArrayList(dataKindNewDataKind);
        }
    }

    private static class SipAddressKindBuilder extends KindBuilder {
        private SipAddressKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "sip_address";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/sip_address", null, R.string.label_sip_address, Weight.SIP_ADDRESS, new SimpleInflater(R.string.label_sip_address), new SimpleInflater("data1"));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.label_sip_address, 33));
            throwIfList(dataKindNewDataKind);
            return Lists.newArrayList(dataKindNewDataKind);
        }
    }

    private static class GroupMembershipKindBuilder extends KindBuilder {
        private GroupMembershipKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "group_membership";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/group_membership", null, R.string.groupsLabel, Weight.GROUP_MEMBERSHIP, null, null);
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", -1, -1));
            dataKindNewDataKind.maxLinesForDisplay = 10;
            throwIfList(dataKindNewDataKind);
            return Lists.newArrayList(dataKindNewDataKind);
        }
    }

    private static class EventKindBuilder extends KindBuilder {
        private EventKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "event";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/contact_event", "data2", R.string.eventLabelsGroup, Weight.EVENT, new EventActionInflater(), new SimpleInflater("data1"));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.eventLabelsGroup, 1));
            if (BaseAccountType.getAttr(attributeSet, Attr.DATE_WITH_TIME, false)) {
                dataKindNewDataKind.dateFormatWithoutYear = CommonDateUtils.NO_YEAR_DATE_AND_TIME_FORMAT;
                dataKindNewDataKind.dateFormatWithYear = CommonDateUtils.DATE_AND_TIME_FORMAT;
            } else {
                dataKindNewDataKind.dateFormatWithoutYear = CommonDateUtils.NO_YEAR_DATE_FORMAT;
                dataKindNewDataKind.dateFormatWithYear = CommonDateUtils.FULL_DATE_FORMAT;
            }
            return Lists.newArrayList(dataKindNewDataKind);
        }

        @Override
        protected AccountType.EditType buildEditTypeForTypeTag(AttributeSet attributeSet, String str) {
            boolean attr = BaseAccountType.getAttr(attributeSet, Attr.YEAR_OPTIONAL, false);
            if ("birthday".equals(str)) {
                return BaseAccountType.buildEventType(3, attr).setSpecificMax(1);
            }
            if ("anniversary".equals(str)) {
                return BaseAccountType.buildEventType(1, attr);
            }
            if ("other".equals(str)) {
                return BaseAccountType.buildEventType(2, attr);
            }
            if ("custom".equals(str)) {
                return BaseAccountType.buildEventType(0, attr).setSecondary(true).setCustomColumn("data3");
            }
            return null;
        }
    }

    private static class RelationshipKindBuilder extends KindBuilder {
        private RelationshipKindBuilder() {
            super();
        }

        @Override
        public String getTagName() {
            return "relationship";
        }

        @Override
        public List<DataKind> parseDataKind(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, AccountType.DefinitionException, IOException {
            DataKind dataKindNewDataKind = newDataKind(context, xmlPullParser, attributeSet, false, "vnd.android.cursor.item/relation", "data2", R.string.relationLabelsGroup, Weight.RELATIONSHIP, new RelationActionInflater(), new SimpleInflater("data1"));
            dataKindNewDataKind.fieldList.add(new AccountType.EditField("data1", R.string.relationLabelsGroup, 8289));
            dataKindNewDataKind.defaultValues = new ContentValues();
            dataKindNewDataKind.defaultValues.put("data2", (Integer) 14);
            return Lists.newArrayList(dataKindNewDataKind);
        }

        @Override
        protected AccountType.EditType buildEditTypeForTypeTag(AttributeSet attributeSet, String str) {
            if ("assistant".equals(str)) {
                return BaseAccountType.buildRelationType(1);
            }
            if ("brother".equals(str)) {
                return BaseAccountType.buildRelationType(2);
            }
            if ("child".equals(str)) {
                return BaseAccountType.buildRelationType(3);
            }
            if ("domestic_partner".equals(str)) {
                return BaseAccountType.buildRelationType(4);
            }
            if ("father".equals(str)) {
                return BaseAccountType.buildRelationType(5);
            }
            if ("friend".equals(str)) {
                return BaseAccountType.buildRelationType(6);
            }
            if ("manager".equals(str)) {
                return BaseAccountType.buildRelationType(7);
            }
            if ("mother".equals(str)) {
                return BaseAccountType.buildRelationType(8);
            }
            if ("parent".equals(str)) {
                return BaseAccountType.buildRelationType(9);
            }
            if ("partner".equals(str)) {
                return BaseAccountType.buildRelationType(10);
            }
            if ("referred_by".equals(str)) {
                return BaseAccountType.buildRelationType(11);
            }
            if ("relative".equals(str)) {
                return BaseAccountType.buildRelationType(12);
            }
            if ("sister".equals(str)) {
                return BaseAccountType.buildRelationType(13);
            }
            if ("spouse".equals(str)) {
                return BaseAccountType.buildRelationType(14);
            }
            if ("custom".equals(str)) {
                return BaseAccountType.buildRelationType(0).setSecondary(true).setCustomColumn("data3");
            }
            return null;
        }
    }

    public static int getTypeNote() {
        return FLAGS_NOTE;
    }

    public static int getTypeWebSite() {
        return FLAGS_WEBSITE;
    }
}

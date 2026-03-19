package com.android.contacts.model.account;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.contacts.R;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contactsbind.FeedbackHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ExternalAccountType extends BaseAccountType {
    private static final String ATTR_ACCOUNT_ICON = "accountTypeIcon";
    private static final String ATTR_ACCOUNT_LABEL = "accountTypeLabel";
    private static final String ATTR_ACCOUNT_TYPE = "accountType";
    private static final String ATTR_DATA_SET = "dataSet";
    private static final String ATTR_EXTENSION_PACKAGE_NAMES = "extensionPackageNames";
    private static final String ATTR_INVITE_CONTACT_ACTION_LABEL = "inviteContactActionLabel";
    private static final String ATTR_INVITE_CONTACT_ACTIVITY = "inviteContactActivity";
    private static final String ATTR_VIEW_CONTACT_NOTIFY_SERVICE = "viewContactNotifyService";
    private static final String ATTR_VIEW_GROUP_ACTION_LABEL = "viewGroupActionLabel";
    private static final String ATTR_VIEW_GROUP_ACTIVITY = "viewGroupActivity";
    private static final String[] METADATA_CONTACTS_NAMES = {"android.provider.ALTERNATE_CONTACTS_STRUCTURE", "android.provider.CONTACTS_STRUCTURE"};
    private static final String SYNC_META_DATA = "android.content.SyncAdapter";
    private static final String TAG = "ExternalAccountType";
    private static final String TAG_CONTACTS_ACCOUNT_TYPE = "ContactsAccountType";
    private static final String TAG_CONTACTS_DATA_KIND = "ContactsDataKind";
    private static final String TAG_CONTACTS_SOURCE_LEGACY = "ContactsSource";
    private static final String TAG_EDIT_SCHEMA = "EditSchema";
    private String mAccountTypeIconAttribute;
    private String mAccountTypeLabelAttribute;
    private List<String> mExtensionPackageNames;
    private boolean mGroupMembershipEditable;
    private boolean mHasContactsMetadata;
    private boolean mHasEditSchema;
    private String mInviteActionLabelAttribute;
    private int mInviteActionLabelResId;
    private String mInviteContactActivity;
    private final boolean mIsExtension;
    private String mViewContactNotifyService;
    private String mViewGroupActivity;
    private String mViewGroupLabelAttribute;
    private int mViewGroupLabelResId;

    public ExternalAccountType(Context context, String str, boolean z) {
        this(context, str, z, null);
    }

    ExternalAccountType(Context context, String str, boolean z, XmlResourceParser xmlResourceParser) {
        DataKind kindForMimetype;
        this.mIsExtension = z;
        this.resourcePackageName = str;
        this.syncAdapterPackageName = str;
        XmlResourceParser xmlResourceParserLoadContactsXml = xmlResourceParser == null ? loadContactsXml(context, str) : xmlResourceParser;
        boolean z2 = false;
        if (xmlResourceParserLoadContactsXml == null) {
            if (this.mHasEditSchema) {
            }
            if (xmlResourceParserLoadContactsXml != null) {
            }
            this.mExtensionPackageNames = new ArrayList();
            this.mInviteActionLabelResId = resolveExternalResId(context, this.mInviteActionLabelAttribute, this.syncAdapterPackageName, ATTR_INVITE_CONTACT_ACTION_LABEL);
            this.mViewGroupLabelResId = resolveExternalResId(context, this.mViewGroupLabelAttribute, this.syncAdapterPackageName, ATTR_VIEW_GROUP_ACTION_LABEL);
            this.titleRes = resolveExternalResId(context, this.mAccountTypeLabelAttribute, this.syncAdapterPackageName, ATTR_ACCOUNT_LABEL);
            this.iconRes = resolveExternalResId(context, this.mAccountTypeIconAttribute, this.syncAdapterPackageName, ATTR_ACCOUNT_ICON);
            kindForMimetype = getKindForMimetype("vnd.android.cursor.item/group_membership");
            if (kindForMimetype != null) {
                z2 = true;
            }
            this.mGroupMembershipEditable = z2;
            this.mIsInitialized = true;
            return;
        }
        try {
            try {
                inflate(context, xmlResourceParserLoadContactsXml);
                try {
                    if (this.mHasEditSchema) {
                        addDataKindStructuredName(context);
                        addDataKindName(context);
                        addDataKindPhoneticName(context);
                        addDataKindPhoto(context);
                    } else {
                        checkKindExists("vnd.android.cursor.item/name");
                        checkKindExists(DataKind.PSEUDO_MIME_TYPE_NAME);
                        checkKindExists(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME);
                        checkKindExists("vnd.android.cursor.item/photo");
                    }
                    if (xmlResourceParserLoadContactsXml != null) {
                        xmlResourceParserLoadContactsXml.close();
                    }
                    this.mExtensionPackageNames = new ArrayList();
                    this.mInviteActionLabelResId = resolveExternalResId(context, this.mInviteActionLabelAttribute, this.syncAdapterPackageName, ATTR_INVITE_CONTACT_ACTION_LABEL);
                    this.mViewGroupLabelResId = resolveExternalResId(context, this.mViewGroupLabelAttribute, this.syncAdapterPackageName, ATTR_VIEW_GROUP_ACTION_LABEL);
                    this.titleRes = resolveExternalResId(context, this.mAccountTypeLabelAttribute, this.syncAdapterPackageName, ATTR_ACCOUNT_LABEL);
                    this.iconRes = resolveExternalResId(context, this.mAccountTypeIconAttribute, this.syncAdapterPackageName, ATTR_ACCOUNT_ICON);
                    kindForMimetype = getKindForMimetype("vnd.android.cursor.item/group_membership");
                    if (kindForMimetype != null && kindForMimetype.editable) {
                        z2 = true;
                    }
                    this.mGroupMembershipEditable = z2;
                    this.mIsInitialized = true;
                    return;
                } catch (AccountType.DefinitionException e) {
                    e = e;
                }
            } catch (AccountType.DefinitionException e2) {
                e = e2;
                z2 = true;
            }
        } finally {
            if (xmlResourceParserLoadContactsXml != null) {
                xmlResourceParserLoadContactsXml.close();
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Problem reading XML");
        if (z2 && xmlResourceParserLoadContactsXml != null) {
            sb.append(" in line ");
            sb.append(xmlResourceParserLoadContactsXml.getLineNumber());
        }
        sb.append(" for external package ");
        sb.append(str);
        if (xmlResourceParser == null) {
            FeedbackHelper.sendFeedback(context, TAG, "Failed to build external account type", e);
        }
    }

    public static XmlResourceParser loadContactsXml(Context context, String str) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> listQueryIntentServices = packageManager.queryIntentServices(new Intent(SYNC_META_DATA).setPackage(str), 132);
        if (listQueryIntentServices != null) {
            Iterator<ResolveInfo> it = listQueryIntentServices.iterator();
            while (it.hasNext()) {
                ServiceInfo serviceInfo = it.next().serviceInfo;
                if (serviceInfo != null) {
                    for (String str2 : METADATA_CONTACTS_NAMES) {
                        XmlResourceParser xmlResourceParserLoadXmlMetaData = serviceInfo.loadXmlMetaData(packageManager, str2);
                        if (xmlResourceParserLoadXmlMetaData != null) {
                            if (Log.isLoggable(TAG, 3)) {
                                Log.d(TAG, String.format("Metadata loaded from: %s, %s, %s", serviceInfo.packageName, serviceInfo.name, str2));
                            }
                            return xmlResourceParserLoadXmlMetaData;
                        }
                    }
                }
            }
            return null;
        }
        return null;
    }

    public static boolean hasContactsXml(Context context, String str) {
        return loadContactsXml(context, str) != null;
    }

    private void checkKindExists(String str) throws AccountType.DefinitionException {
        if (getKindForMimetype(str) == null) {
            throw new AccountType.DefinitionException(str + " must be supported");
        }
    }

    @Override
    public boolean isEmbedded() {
        return false;
    }

    @Override
    public boolean isExtension() {
        return this.mIsExtension;
    }

    @Override
    public boolean areContactsWritable() {
        return this.mHasEditSchema;
    }

    public boolean hasContactsMetadata() {
        return this.mHasContactsMetadata;
    }

    @Override
    public String getInviteContactActivityClassName() {
        return this.mInviteContactActivity;
    }

    @Override
    protected int getInviteContactActionResId() {
        return this.mInviteActionLabelResId;
    }

    @Override
    public String getViewContactNotifyServiceClassName() {
        return this.mViewContactNotifyService;
    }

    @Override
    public String getViewGroupActivity() {
        return this.mViewGroupActivity;
    }

    @Override
    protected int getViewGroupLabelResId() {
        return this.mViewGroupLabelResId;
    }

    @Override
    public List<String> getExtensionPackageNames() {
        return this.mExtensionPackageNames;
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return this.mGroupMembershipEditable;
    }

    protected void inflate(Context context, XmlPullParser xmlPullParser) throws AccountType.DefinitionException {
        int next;
        AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlPullParser);
        do {
            try {
                next = xmlPullParser.next();
                if (next == 2) {
                    break;
                }
            } catch (IOException e) {
                throw new AccountType.DefinitionException("Problem reading XML", e);
            } catch (XmlPullParserException e2) {
                throw new AccountType.DefinitionException("Problem reading XML", e2);
            }
        } while (next != 1);
        if (next != 2) {
            throw new IllegalStateException("No start tag found");
        }
        String name = xmlPullParser.getName();
        if (!TAG_CONTACTS_ACCOUNT_TYPE.equals(name) && !TAG_CONTACTS_SOURCE_LEGACY.equals(name)) {
            throw new IllegalStateException("Top level element must be ContactsAccountType, not " + name);
        }
        this.mHasContactsMetadata = true;
        int attributeCount = xmlPullParser.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String attributeName = xmlPullParser.getAttributeName(i);
            String attributeValue = xmlPullParser.getAttributeValue(i);
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, attributeName + "=" + attributeValue);
            }
            if (ATTR_INVITE_CONTACT_ACTIVITY.equals(attributeName)) {
                this.mInviteContactActivity = attributeValue;
            } else if (ATTR_INVITE_CONTACT_ACTION_LABEL.equals(attributeName)) {
                this.mInviteActionLabelAttribute = attributeValue;
            } else if (ATTR_VIEW_CONTACT_NOTIFY_SERVICE.equals(attributeName)) {
                this.mViewContactNotifyService = attributeValue;
            } else if (ATTR_VIEW_GROUP_ACTIVITY.equals(attributeName)) {
                this.mViewGroupActivity = attributeValue;
            } else if (ATTR_VIEW_GROUP_ACTION_LABEL.equals(attributeName)) {
                this.mViewGroupLabelAttribute = attributeValue;
            } else if ("dataSet".equals(attributeName)) {
                this.dataSet = attributeValue;
            } else if (ATTR_EXTENSION_PACKAGE_NAMES.equals(attributeName)) {
                this.mExtensionPackageNames.add(attributeValue);
            } else if ("accountType".equals(attributeName)) {
                this.accountType = attributeValue;
            } else if (ATTR_ACCOUNT_LABEL.equals(attributeName)) {
                this.mAccountTypeLabelAttribute = attributeValue;
            } else if (ATTR_ACCOUNT_ICON.equals(attributeName)) {
                this.mAccountTypeIconAttribute = attributeValue;
            } else if (Log.isLoggable(TAG, 5)) {
                Log.w(TAG, "Unsupported attribute " + attributeName);
            }
        }
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next2 = xmlPullParser.next();
            if ((next2 != 3 || xmlPullParser.getDepth() > depth) && next2 != 1) {
                if (next2 == 2 && xmlPullParser.getDepth() == depth + 1) {
                    String name2 = xmlPullParser.getName();
                    if (TAG_EDIT_SCHEMA.equals(name2)) {
                        this.mHasEditSchema = true;
                        parseEditSchema(context, xmlPullParser, attributeSetAsAttributeSet);
                    } else if (TAG_CONTACTS_DATA_KIND.equals(name2)) {
                        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSetAsAttributeSet, R.styleable.ContactsDataKind);
                        DataKind dataKind = new DataKind();
                        dataKind.mimeType = typedArrayObtainStyledAttributes.getString(1);
                        String string = typedArrayObtainStyledAttributes.getString(2);
                        if (string != null) {
                            dataKind.actionHeader = new BaseAccountType.SimpleInflater(string);
                        }
                        String string2 = typedArrayObtainStyledAttributes.getString(3);
                        if (string2 != null) {
                            dataKind.actionBody = new BaseAccountType.SimpleInflater(string2);
                        }
                        typedArrayObtainStyledAttributes.recycle();
                        addKind(dataKind);
                    }
                }
            } else {
                return;
            }
        }
    }

    static int resolveExternalResId(Context context, String str, String str2, String str3) {
        if (TextUtils.isEmpty(str)) {
            return -1;
        }
        if (str.charAt(0) != '@') {
            if (Log.isLoggable(TAG, 5) && !isFromTestApp(str2)) {
                Log.w(TAG, str3 + " must be a resource name beginnig with '@'");
            }
            return -1;
        }
        try {
            int identifier = context.getPackageManager().getResourcesForApplication(str2).getIdentifier(str.substring(1), null, str2);
            if (identifier == 0) {
                if (Log.isLoggable(TAG, 5) && !isFromTestApp(str2)) {
                    Log.w(TAG, "Unable to load " + str + " from package " + str2);
                }
                return -1;
            }
            return identifier;
        } catch (PackageManager.NameNotFoundException e) {
            if (Log.isLoggable(TAG, 5) && !isFromTestApp(str2)) {
                Log.w(TAG, "Unable to load package " + str2);
            }
            return -1;
        }
    }

    static boolean isFromTestApp(String str) {
        return TextUtils.equals(str, "com.google.android.contacts.tests");
    }
}

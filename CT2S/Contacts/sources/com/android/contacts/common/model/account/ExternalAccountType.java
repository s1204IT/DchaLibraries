package com.android.contacts.common.model.account;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.contacts.common.R;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.BaseAccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ExternalAccountType extends BaseAccountType {
    private static final String[] METADATA_CONTACTS_NAMES = {"android.provider.ALTERNATE_CONTACTS_STRUCTURE", "android.provider.CONTACTS_STRUCTURE"};
    private String mAccountTypeIconAttribute;
    private String mAccountTypeLabelAttribute;
    private String mCreateContactActivityClassName;
    private String mEditContactActivityClassName;
    private List<String> mExtensionPackageNames;
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

    public ExternalAccountType(Context context, String resPackageName, boolean isExtension) {
        this(context, resPackageName, isExtension, null);
    }

    ExternalAccountType(Context context, String packageName, boolean isExtension, XmlResourceParser injectedMetadata) {
        XmlResourceParser parser;
        this.mIsExtension = isExtension;
        this.resourcePackageName = packageName;
        this.syncAdapterPackageName = packageName;
        if (injectedMetadata == null) {
            parser = loadContactsXml(context, packageName);
        } else {
            parser = injectedMetadata;
        }
        try {
            if (parser != null) {
                try {
                    inflate(context, parser);
                } catch (AccountType.DefinitionException e) {
                    StringBuilder error = new StringBuilder();
                    error.append("Problem reading XML");
                    if (1 != 0 && parser != null) {
                        error.append(" in line ");
                        error.append(parser.getLineNumber());
                    }
                    error.append(" for external package ");
                    error.append(packageName);
                    Log.e("ExternalAccountType", error.toString(), e);
                    if (parser != null) {
                        parser.close();
                        return;
                    }
                    return;
                }
            }
            if (this.mHasEditSchema) {
                checkKindExists("vnd.android.cursor.item/name");
                checkKindExists("#displayName");
                checkKindExists("#phoneticName");
                checkKindExists("vnd.android.cursor.item/photo");
            } else {
                addDataKindStructuredName(context);
                addDataKindDisplayName(context);
                addDataKindPhoneticName(context);
                addDataKindPhoto(context);
            }
            if (parser != null) {
                parser.close();
            }
            this.mExtensionPackageNames = new ArrayList();
            this.mInviteActionLabelResId = resolveExternalResId(context, this.mInviteActionLabelAttribute, this.syncAdapterPackageName, "inviteContactActionLabel");
            this.mViewGroupLabelResId = resolveExternalResId(context, this.mViewGroupLabelAttribute, this.syncAdapterPackageName, "viewGroupActionLabel");
            this.titleRes = resolveExternalResId(context, this.mAccountTypeLabelAttribute, this.syncAdapterPackageName, "accountTypeLabel");
            this.iconRes = resolveExternalResId(context, this.mAccountTypeIconAttribute, this.syncAdapterPackageName, "accountTypeIcon");
            this.mIsInitialized = true;
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
            throw th;
        }
    }

    public static XmlResourceParser loadContactsXml(Context context, String resPackageName) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent("android.content.SyncAdapter").setPackage(resPackageName);
        List<ResolveInfo> intentServices = pm.queryIntentServices(intent, 132);
        if (intentServices != null) {
            for (ResolveInfo resolveInfo : intentServices) {
                ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                if (serviceInfo != null) {
                    String[] arr$ = METADATA_CONTACTS_NAMES;
                    for (String metadataName : arr$) {
                        XmlResourceParser parser = serviceInfo.loadXmlMetaData(pm, metadataName);
                        if (parser != null) {
                            if (Log.isLoggable("ExternalAccountType", 3)) {
                                Log.d("ExternalAccountType", String.format("Metadata loaded from: %s, %s, %s", serviceInfo.packageName, serviceInfo.name, metadataName));
                                return parser;
                            }
                            return parser;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void checkKindExists(String mimeType) throws AccountType.DefinitionException {
        if (getKindForMimetype(mimeType) == null) {
            throw new AccountType.DefinitionException(mimeType + " must be supported");
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
    public String getEditContactActivityClassName() {
        return this.mEditContactActivityClassName;
    }

    @Override
    public String getCreateContactActivityClassName() {
        return this.mCreateContactActivityClassName;
    }

    @Override
    public String getInviteContactActivityClassName() {
        return this.mInviteContactActivity;
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

    protected void inflate(Context context, XmlPullParser parser) throws AccountType.DefinitionException {
        int type;
        AttributeSet attrs = Xml.asAttributeSet(parser);
        do {
            try {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } catch (IOException e) {
                throw new AccountType.DefinitionException("Problem reading XML", e);
            } catch (XmlPullParserException e2) {
                throw new AccountType.DefinitionException("Problem reading XML", e2);
            }
        } while (type != 1);
        if (type != 2) {
            throw new IllegalStateException("No start tag found");
        }
        String rootTag = parser.getName();
        if (!"ContactsAccountType".equals(rootTag) && !"ContactsSource".equals(rootTag)) {
            throw new IllegalStateException("Top level element must be ContactsAccountType, not " + rootTag);
        }
        this.mHasContactsMetadata = true;
        int attributeCount = parser.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String attr = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (Log.isLoggable("ExternalAccountType", 3)) {
                Log.d("ExternalAccountType", attr + "=" + value);
            }
            if ("editContactActivity".equals(attr)) {
                this.mEditContactActivityClassName = value;
            } else if ("createContactActivity".equals(attr)) {
                this.mCreateContactActivityClassName = value;
            } else if ("inviteContactActivity".equals(attr)) {
                this.mInviteContactActivity = value;
            } else if ("inviteContactActionLabel".equals(attr)) {
                this.mInviteActionLabelAttribute = value;
            } else if ("viewContactNotifyService".equals(attr)) {
                this.mViewContactNotifyService = value;
            } else if ("viewGroupActivity".equals(attr)) {
                this.mViewGroupActivity = value;
            } else if ("viewGroupActionLabel".equals(attr)) {
                this.mViewGroupLabelAttribute = value;
            } else if ("dataSet".equals(attr)) {
                this.dataSet = value;
            } else if ("extensionPackageNames".equals(attr)) {
                this.mExtensionPackageNames.add(value);
            } else if ("accountType".equals(attr)) {
                this.accountType = value;
            } else if ("accountTypeLabel".equals(attr)) {
                this.mAccountTypeLabelAttribute = value;
            } else if ("accountTypeIcon".equals(attr)) {
                this.mAccountTypeIconAttribute = value;
            } else {
                Log.e("ExternalAccountType", "Unsupported attribute " + attr);
            }
        }
        int startDepth = parser.getDepth();
        while (true) {
            int type2 = parser.next();
            if ((type2 != 3 || parser.getDepth() > startDepth) && type2 != 1) {
                if (type2 == 2 && parser.getDepth() == startDepth + 1) {
                    String tag = parser.getName();
                    if ("EditSchema".equals(tag)) {
                        this.mHasEditSchema = true;
                        parseEditSchema(context, parser, attrs);
                    } else if ("ContactsDataKind".equals(tag)) {
                        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ContactsDataKind);
                        DataKind kind = new DataKind();
                        kind.mimeType = a.getString(1);
                        String summaryColumn = a.getString(2);
                        if (summaryColumn != null) {
                            kind.actionHeader = new BaseAccountType.SimpleInflater(summaryColumn);
                        }
                        String detailColumn = a.getString(3);
                        if (detailColumn != null) {
                            kind.actionBody = new BaseAccountType.SimpleInflater(detailColumn);
                        }
                        a.recycle();
                        addKind(kind);
                    }
                }
            } else {
                return;
            }
        }
    }

    static int resolveExternalResId(Context context, String resourceName, String packageName, String xmlAttributeName) {
        if (TextUtils.isEmpty(resourceName)) {
            return -1;
        }
        if (resourceName.charAt(0) != '@') {
            Log.e("ExternalAccountType", xmlAttributeName + " must be a resource name beginnig with '@'");
            return -1;
        }
        String name = resourceName.substring(1);
        try {
            Resources res = context.getPackageManager().getResourcesForApplication(packageName);
            int resId = res.getIdentifier(name, null, packageName);
            if (resId == 0) {
                Log.e("ExternalAccountType", "Unable to load " + resourceName + " from package " + packageName);
                return -1;
            }
            return resId;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ExternalAccountType", "Unable to load package " + packageName);
            return -1;
        }
    }
}

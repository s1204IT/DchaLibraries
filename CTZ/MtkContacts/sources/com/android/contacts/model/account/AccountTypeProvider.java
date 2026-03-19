package com.android.contacts.model.account;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncAdapterType;
import android.text.TextUtils;
import com.android.contacts.util.DeviceLocalAccountTypeFactory;
import com.android.contactsbind.ObjectFactory;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mediatek.contacts.util.Log;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AccountTypeProvider {
    private final ImmutableMap<String, AuthenticatorDescription> mAuthTypes;
    private final ConcurrentMap<String, List<AccountType>> mCache;
    private final Context mContext;
    private final DeviceLocalAccountTypeFactory mLocalAccountTypeFactory;

    public AccountTypeProvider(Context context) {
        this(context, ObjectFactory.getDeviceLocalAccountTypeFactory(context), ContentResolver.getSyncAdapterTypes(), ((AccountManager) context.getSystemService("account")).getAuthenticatorTypes());
    }

    public AccountTypeProvider(Context context, DeviceLocalAccountTypeFactory deviceLocalAccountTypeFactory, SyncAdapterType[] syncAdapterTypeArr, AuthenticatorDescription[] authenticatorDescriptionArr) {
        this.mCache = new ConcurrentHashMap();
        this.mContext = context;
        this.mLocalAccountTypeFactory = deviceLocalAccountTypeFactory;
        this.mAuthTypes = onlyContactSyncable(authenticatorDescriptionArr, syncAdapterTypeArr);
    }

    public List<AccountType> getAccountTypes(String str) {
        if (str == null) {
            AccountType accountType = this.mLocalAccountTypeFactory.getAccountType(str);
            if (accountType == null) {
                accountType = new FallbackAccountType(this.mContext);
            }
            return Collections.singletonList(accountType);
        }
        List<AccountType> listLoadTypes = this.mCache.get(str);
        if (listLoadTypes == null) {
            listLoadTypes = loadTypes(str);
            if (listLoadTypes.isEmpty() && DeviceLocalAccountTypeFactory.Util.isLocalAccountType(this.mLocalAccountTypeFactory, str)) {
                listLoadTypes = ImmutableList.builder().add(this.mLocalAccountTypeFactory.getAccountType(str)).build();
            }
            this.mCache.put(str, listLoadTypes);
        }
        Log.i("AccountTypeProvider", "[getAccountTypes]String:" + str + ", types=" + listLoadTypes);
        return listLoadTypes;
    }

    public AccountType getType(String str, String str2) {
        for (AccountType accountType : getAccountTypes(str)) {
            if (Objects.equal(accountType.dataSet, str2)) {
                return accountType;
            }
        }
        Log.i("AccountTypeProvider", "[getType]unkown type:" + str + ", dataSet:" + str2);
        return null;
    }

    public AccountType getTypeForAccount(AccountWithDataSet accountWithDataSet) {
        return getType(accountWithDataSet.type, accountWithDataSet.dataSet);
    }

    public boolean shouldUpdate(AuthenticatorDescription[] authenticatorDescriptionArr, SyncAdapterType[] syncAdapterTypeArr) {
        ImmutableMap<String, AuthenticatorDescription> immutableMapOnlyContactSyncable = onlyContactSyncable(authenticatorDescriptionArr, syncAdapterTypeArr);
        if (!immutableMapOnlyContactSyncable.keySet().equals(this.mAuthTypes.keySet())) {
            return true;
        }
        for (AuthenticatorDescription authenticatorDescription : immutableMapOnlyContactSyncable.values()) {
            if (!deepEquals(this.mAuthTypes.get(authenticatorDescription.type), authenticatorDescription)) {
                return true;
            }
        }
        return false;
    }

    public boolean supportsContactsSyncing(String str) {
        return this.mAuthTypes.containsKey(str);
    }

    private List<AccountType> loadTypes(String str) {
        AccountType externalAccountType;
        Log.i("AccountTypeProvider", "loadTypes(): type=" + str);
        AuthenticatorDescription authenticatorDescription = this.mAuthTypes.get(str);
        if (authenticatorDescription == null) {
            if (Log.isLoggable("AccountTypeProvider", 3)) {
                Log.d("AccountTypeProvider", "Null auth type for " + str);
            }
            return Collections.emptyList();
        }
        if (GoogleAccountType.ACCOUNT_TYPE.equals(str)) {
            externalAccountType = new GoogleAccountType(this.mContext, authenticatorDescription.packageName);
        } else if (ExchangeAccountType.isExchangeType(str)) {
            externalAccountType = new ExchangeAccountType(this.mContext, authenticatorDescription.packageName, str);
        } else if (SamsungAccountType.isSamsungAccountType(this.mContext, str, authenticatorDescription.packageName)) {
            externalAccountType = new SamsungAccountType(this.mContext, authenticatorDescription.packageName, str);
        } else if (ExternalAccountType.hasContactsXml(this.mContext, authenticatorDescription.packageName) || !DeviceLocalAccountTypeFactory.Util.isLocalAccountType(this.mLocalAccountTypeFactory, str)) {
            if (Log.isLoggable("AccountTypeProvider", 3)) {
                Log.d("AccountTypeProvider", "Registering external account type=" + str + ", packageName=" + authenticatorDescription.packageName);
            }
            externalAccountType = new ExternalAccountType(this.mContext, authenticatorDescription.packageName, false);
        } else {
            if (Log.isLoggable("AccountTypeProvider", 3)) {
                Log.d("AccountTypeProvider", "Registering local account type=" + str + ", packageName=" + authenticatorDescription.packageName);
            }
            externalAccountType = this.mLocalAccountTypeFactory.getAccountType(str);
        }
        if (!externalAccountType.isInitialized()) {
            if (!externalAccountType.isEmbedded()) {
                if (Log.isLoggable("AccountTypeProvider", 3)) {
                    Log.d("AccountTypeProvider", "Skipping external account type=" + str + ", packageName=" + authenticatorDescription.packageName);
                }
                return Collections.emptyList();
            }
            throw new IllegalStateException("Problem initializing embedded type " + externalAccountType.getClass().getCanonicalName());
        }
        externalAccountType.initializeFieldsFromAuthenticator(authenticatorDescription);
        ImmutableList.Builder builder = ImmutableList.builder();
        builder.add(externalAccountType);
        for (String str2 : externalAccountType.getExtensionPackageNames()) {
            ExternalAccountType externalAccountType2 = new ExternalAccountType(this.mContext, str2, true);
            if (externalAccountType2.isInitialized()) {
                if (!externalAccountType2.hasContactsMetadata()) {
                    Log.w("AccountTypeProvider", "Skipping extension package " + str2 + " because it doesn't have the CONTACTS_STRUCTURE metadata");
                } else if (TextUtils.isEmpty(externalAccountType2.accountType)) {
                    Log.w("AccountTypeProvider", "Skipping extension package " + str2 + " because the CONTACTS_STRUCTURE metadata doesn't have the accountType attribute");
                } else if (Objects.equal(externalAccountType2.accountType, str)) {
                    if (Log.isLoggable("AccountTypeProvider", 3)) {
                        Log.d("AccountTypeProvider", "Registering extension package account type=" + externalAccountType.accountType + ", dataSet=" + externalAccountType.dataSet + ", packageName=" + str2);
                    }
                    builder.add(externalAccountType2);
                } else {
                    Log.w("AccountTypeProvider", "Skipping extension package " + str2 + " because the account type + " + externalAccountType2.accountType + " doesn't match expected type " + str);
                }
            }
        }
        return builder.build();
    }

    private static ImmutableMap<String, AuthenticatorDescription> onlyContactSyncable(AuthenticatorDescription[] authenticatorDescriptionArr, SyncAdapterType[] syncAdapterTypeArr) {
        HashSet hashSet = new HashSet();
        for (SyncAdapterType syncAdapterType : syncAdapterTypeArr) {
            if (syncAdapterType.authority.equals("com.android.contacts")) {
                hashSet.add(syncAdapterType.accountType);
            }
        }
        ImmutableMap.Builder builder = ImmutableMap.builder();
        for (AuthenticatorDescription authenticatorDescription : authenticatorDescriptionArr) {
            if (hashSet.contains(authenticatorDescription.type)) {
                builder.put(authenticatorDescription.type, authenticatorDescription);
            }
        }
        return builder.build();
    }

    private boolean deepEquals(AuthenticatorDescription authenticatorDescription, AuthenticatorDescription authenticatorDescription2) {
        return Objects.equal(authenticatorDescription, authenticatorDescription2) && Objects.equal(authenticatorDescription.packageName, authenticatorDescription2.packageName) && authenticatorDescription.labelId == authenticatorDescription2.labelId && authenticatorDescription.iconId == authenticatorDescription2.iconId && authenticatorDescription.smallIconId == authenticatorDescription2.smallIconId && authenticatorDescription.accountPreferencesId == authenticatorDescription2.accountPreferencesId && authenticatorDescription.customTokens == authenticatorDescription2.customTokens;
    }
}

package com.android.managedprovisioning;

import android.accounts.Account;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Xml;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class IntentStore {
    private Context mContext;
    private ComponentName mIntentTarget;
    private SharedPreferences mPrefs;
    private String mPrefsName;
    private String[] mStringKeys = new String[0];
    private String[] mLongKeys = new String[0];
    private String[] mIntKeys = new String[0];
    private String[] mBooleanKeys = new String[0];
    private String[] mPersistableBundleKeys = new String[0];
    private String[] mAccountKeys = new String[0];

    public IntentStore(Context context, ComponentName intentTarget, String preferencesName) {
        this.mContext = context;
        this.mPrefsName = preferencesName;
        this.mPrefs = context.getSharedPreferences(preferencesName, 0);
        this.mIntentTarget = intentTarget;
    }

    public IntentStore setStringKeys(String[] keys) {
        if (keys == null) {
            keys = new String[0];
        }
        this.mStringKeys = keys;
        return this;
    }

    public IntentStore setLongKeys(String[] keys) {
        if (keys == null) {
            keys = new String[0];
        }
        this.mLongKeys = keys;
        return this;
    }

    public IntentStore setIntKeys(String[] keys) {
        if (keys == null) {
            keys = new String[0];
        }
        this.mIntKeys = keys;
        return this;
    }

    public IntentStore setBooleanKeys(String[] keys) {
        if (keys == null) {
            keys = new String[0];
        }
        this.mBooleanKeys = keys;
        return this;
    }

    public IntentStore setAccountKeys(String[] keys) {
        if (keys == null) {
            keys = new String[0];
        }
        this.mAccountKeys = keys;
        return this;
    }

    public IntentStore setPersistableBundleKeys(String[] keys) {
        if (keys == null) {
            keys = new String[0];
        }
        this.mPersistableBundleKeys = keys;
        return this;
    }

    public void clear() {
        this.mPrefs.edit().clear().commit();
    }

    public void save(Bundle data) {
        SharedPreferences.Editor editor = this.mPrefs.edit();
        editor.clear();
        String[] arr$ = this.mStringKeys;
        for (String key : arr$) {
            editor.putString(key, data.getString(key));
        }
        String[] arr$2 = this.mLongKeys;
        for (String key2 : arr$2) {
            editor.putLong(key2, data.getLong(key2));
        }
        String[] arr$3 = this.mIntKeys;
        for (String key3 : arr$3) {
            editor.putInt(key3, data.getInt(key3));
        }
        String[] arr$4 = this.mBooleanKeys;
        for (String key4 : arr$4) {
            editor.putBoolean(key4, data.getBoolean(key4));
        }
        String[] arr$5 = this.mAccountKeys;
        for (String key5 : arr$5) {
            Account account = (Account) data.getParcelable(key5);
            String accountString = accountToString(account);
            if (accountString != null) {
                editor.putString(key5, accountString);
            }
        }
        String[] arr$6 = this.mPersistableBundleKeys;
        for (String key6 : arr$6) {
            String bundleString = persistableBundleToString((PersistableBundle) data.getParcelable(key6));
            if (bundleString != null) {
                editor.putString(key6, bundleString);
            }
        }
        editor.putBoolean("isSet", true);
        editor.commit();
    }

    public Intent load() {
        PersistableBundle bundle;
        Account account;
        if (!this.mPrefs.getBoolean("isSet", false)) {
            return null;
        }
        Intent result = new Intent();
        result.setComponent(this.mIntentTarget);
        String[] arr$ = this.mStringKeys;
        for (String key : arr$) {
            String value = this.mPrefs.getString(key, null);
            if (value != null) {
                result.putExtra(key, value);
            }
        }
        String[] arr$2 = this.mLongKeys;
        for (String key2 : arr$2) {
            if (this.mPrefs.contains(key2)) {
                result.putExtra(key2, this.mPrefs.getLong(key2, 0L));
            }
        }
        String[] arr$3 = this.mIntKeys;
        for (String key3 : arr$3) {
            if (this.mPrefs.contains(key3)) {
                result.putExtra(key3, this.mPrefs.getInt(key3, 0));
            }
        }
        String[] arr$4 = this.mBooleanKeys;
        for (String key4 : arr$4) {
            if (this.mPrefs.contains(key4)) {
                result.putExtra(key4, this.mPrefs.getBoolean(key4, false));
            }
        }
        String[] arr$5 = this.mAccountKeys;
        for (String key5 : arr$5) {
            if (this.mPrefs.contains(key5) && (account = stringToAccount(this.mPrefs.getString(key5, null))) != null) {
                result.putExtra(key5, account);
            }
        }
        String[] arr$6 = this.mPersistableBundleKeys;
        for (String key6 : arr$6) {
            if (this.mPrefs.contains(key6) && (bundle = stringToPersistableBundle(this.mPrefs.getString(key6, null))) != null) {
                result.putExtra(key6, bundle);
            }
        }
        return result;
    }

    private String accountToString(Account account) {
        if (account == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(writer);
            serializer.startDocument(null, true);
            serializer.startTag(null, "account");
            serializer.attribute(null, "name", account.name);
            serializer.attribute(null, "type", account.type);
            serializer.endTag(null, "account");
            serializer.endDocument();
            return writer.toString();
        } catch (IOException e) {
            ProvisionLogger.loge("Account could not be stored as string.", e);
            return null;
        }
    }

    private Account stringToAccount(String string) {
        if (string == null) {
            return null;
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(string));
            if (parser.next() == 2 && "account".equals(parser.getName())) {
                String name = parser.getAttributeValue(null, "name");
                String type = parser.getAttributeValue(null, "type");
                if (name != null && type != null) {
                    return new Account(name, type);
                }
            }
        } catch (IOException | XmlPullParserException e) {
            ProvisionLogger.loge(e);
        }
        ProvisionLogger.loge("Account could not be restored from string " + string);
        return null;
    }

    private String persistableBundleToString(PersistableBundle bundle) {
        if (bundle == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(writer);
            serializer.startDocument(null, true);
            serializer.startTag(null, "persistable_bundle");
            bundle.saveToXml(serializer);
            serializer.endTag(null, "persistable_bundle");
            serializer.endDocument();
            return writer.toString();
        } catch (IOException | XmlPullParserException e) {
            ProvisionLogger.loge("Persistable bundle could not be stored as string.", e);
            return null;
        }
    }

    private PersistableBundle stringToPersistableBundle(String string) {
        if (string == null) {
            return null;
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(string));
            if (parser.next() == 2 && "persistable_bundle".equals(parser.getName())) {
                return PersistableBundle.restoreFromXml(parser);
            }
        } catch (IOException | XmlPullParserException e) {
            ProvisionLogger.loge(e);
        }
        ProvisionLogger.loge("Persistable bundle could not be restored from string " + string);
        return null;
    }
}

package com.android.server.accounts;

import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.RegisteredServicesCache;
import android.content.pm.XmlSerializerAndParser;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.android.internal.R;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class AccountAuthenticatorCache extends RegisteredServicesCache<AuthenticatorDescription> implements IAccountAuthenticatorCache {
    private static final String TAG = "Account";
    private static final MySerializer sSerializer = new MySerializer();

    @Override
    public RegisteredServicesCache.ServiceInfo getServiceInfo(AuthenticatorDescription authenticatorDescription, int i) {
        return super.getServiceInfo(authenticatorDescription, i);
    }

    public AccountAuthenticatorCache(Context context) {
        super(context, "android.accounts.AccountAuthenticator", "android.accounts.AccountAuthenticator", "account-authenticator", sSerializer);
    }

    public AuthenticatorDescription m7parseServiceAttributes(Resources resources, String str, AttributeSet attributeSet) {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.AccountAuthenticator);
        try {
            String string = typedArrayObtainAttributes.getString(2);
            int resourceId = typedArrayObtainAttributes.getResourceId(0, 0);
            int resourceId2 = typedArrayObtainAttributes.getResourceId(1, 0);
            int resourceId3 = typedArrayObtainAttributes.getResourceId(3, 0);
            int resourceId4 = typedArrayObtainAttributes.getResourceId(4, 0);
            boolean z = typedArrayObtainAttributes.getBoolean(5, false);
            if (!TextUtils.isEmpty(string)) {
                return new AuthenticatorDescription(string, str, resourceId, resourceId2, resourceId3, resourceId4, z);
            }
            return null;
        } finally {
            typedArrayObtainAttributes.recycle();
        }
    }

    private static class MySerializer implements XmlSerializerAndParser<AuthenticatorDescription> {
        private MySerializer() {
        }

        public void writeAsXml(AuthenticatorDescription authenticatorDescription, XmlSerializer xmlSerializer) throws IOException {
            xmlSerializer.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, authenticatorDescription.type);
        }

        public AuthenticatorDescription m8createFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            return AuthenticatorDescription.newKey(xmlPullParser.getAttributeValue(null, DatabaseHelper.SoundModelContract.KEY_TYPE));
        }
    }
}

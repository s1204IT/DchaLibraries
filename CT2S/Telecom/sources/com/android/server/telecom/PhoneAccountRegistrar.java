package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Base64;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.XmlUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class PhoneAccountRegistrar {
    private final AtomicFile mAtomicFile;
    private final Context mContext;
    private UserHandle mCurrentUserHandle;
    private final List<Listener> mListeners;
    private State mState;
    private final SubscriptionManager mSubscriptionManager;
    private final UserManager mUserManager;
    public static final PhoneAccountHandle NO_ACCOUNT_SELECTED = new PhoneAccountHandle(new ComponentName("null", "null"), "NO_ACCOUNT_SELECTED");
    public static final XmlSerialization<State> sStateXml = new XmlSerialization<State>() {
        @Override
        public void writeToXml(State state, XmlSerializer xmlSerializer, Context context) throws IOException {
            if (state != null) {
                xmlSerializer.startTag(null, "phone_account_registrar_state");
                xmlSerializer.attribute(null, "version", Objects.toString(5));
                if (state.defaultOutgoing != null) {
                    xmlSerializer.startTag(null, "default_outgoing");
                    PhoneAccountRegistrar.sPhoneAccountHandleXml.writeToXml(state.defaultOutgoing, xmlSerializer, context);
                    xmlSerializer.endTag(null, "default_outgoing");
                }
                if (state.simCallManager != null) {
                    xmlSerializer.startTag(null, "sim_call_manager");
                    PhoneAccountRegistrar.sPhoneAccountHandleXml.writeToXml(state.simCallManager, xmlSerializer, context);
                    xmlSerializer.endTag(null, "sim_call_manager");
                }
                xmlSerializer.startTag(null, "accounts");
                Iterator<PhoneAccount> it = state.accounts.iterator();
                while (it.hasNext()) {
                    PhoneAccountRegistrar.sPhoneAccountXml.writeToXml(it.next(), xmlSerializer, context);
                }
                xmlSerializer.endTag(null, "accounts");
                xmlSerializer.endTag(null, "phone_account_registrar_state");
            }
        }

        @Override
        public State readFromXml(XmlPullParser xmlPullParser, int i, Context context) throws XmlPullParserException, IOException {
            if (!xmlPullParser.getName().equals("phone_account_registrar_state")) {
                return null;
            }
            State state = new State();
            String attributeValue = xmlPullParser.getAttributeValue(null, "version");
            state.versionNumber = TextUtils.isEmpty(attributeValue) ? 1 : Integer.parseInt(attributeValue);
            int depth = xmlPullParser.getDepth();
            while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                if (xmlPullParser.getName().equals("default_outgoing")) {
                    xmlPullParser.nextTag();
                    state.defaultOutgoing = PhoneAccountRegistrar.sPhoneAccountHandleXml.readFromXml(xmlPullParser, state.versionNumber, context);
                } else if (xmlPullParser.getName().equals("sim_call_manager")) {
                    xmlPullParser.nextTag();
                    state.simCallManager = PhoneAccountRegistrar.sPhoneAccountHandleXml.readFromXml(xmlPullParser, state.versionNumber, context);
                    if (state.simCallManager.getUserHandle() == null) {
                        state.simCallManager = new PhoneAccountHandle(state.simCallManager.getComponentName(), state.simCallManager.getId(), Process.myUserHandle());
                    }
                } else if (xmlPullParser.getName().equals("accounts")) {
                    int depth2 = xmlPullParser.getDepth();
                    while (XmlUtils.nextElementWithin(xmlPullParser, depth2)) {
                        PhoneAccount fromXml = PhoneAccountRegistrar.sPhoneAccountXml.readFromXml(xmlPullParser, state.versionNumber, context);
                        if (fromXml != null && state.accounts != null) {
                            state.accounts.add(fromXml);
                        }
                    }
                }
            }
            return state;
        }
    };
    public static final XmlSerialization<PhoneAccount> sPhoneAccountXml = new XmlSerialization<PhoneAccount>() {
        @Override
        public void writeToXml(PhoneAccount phoneAccount, XmlSerializer xmlSerializer, Context context) throws IOException {
            if (phoneAccount != null) {
                xmlSerializer.startTag(null, "phone_account");
                if (phoneAccount.getAccountHandle() != null) {
                    xmlSerializer.startTag(null, "account_handle");
                    PhoneAccountRegistrar.sPhoneAccountHandleXml.writeToXml(phoneAccount.getAccountHandle(), xmlSerializer, context);
                    xmlSerializer.endTag(null, "account_handle");
                }
                writeTextIfNonNull("handle", phoneAccount.getAddress(), xmlSerializer);
                writeTextIfNonNull("subscription_number", phoneAccount.getSubscriptionAddress(), xmlSerializer);
                writeTextIfNonNull("capabilities", Integer.toString(phoneAccount.getCapabilities()), xmlSerializer);
                writeTextIfNonNull("icon_res_id", Integer.toString(phoneAccount.getIconResId()), xmlSerializer);
                writeTextIfNonNull("icon_package_name", phoneAccount.getIconPackageName(), xmlSerializer);
                writeBitmapIfNonNull("icon_bitmap", phoneAccount.getIconBitmap(), xmlSerializer);
                writeTextIfNonNull("icon_tint", Integer.toString(phoneAccount.getIconTint()), xmlSerializer);
                writeTextIfNonNull("highlight_color", Integer.toString(phoneAccount.getHighlightColor()), xmlSerializer);
                writeTextIfNonNull("label", phoneAccount.getLabel(), xmlSerializer);
                writeTextIfNonNull("short_description", phoneAccount.getShortDescription(), xmlSerializer);
                writeStringList("supported_uri_schemes", phoneAccount.getSupportedUriSchemes(), xmlSerializer);
                xmlSerializer.endTag(null, "phone_account");
            }
        }

        @Override
        public PhoneAccount readFromXml(XmlPullParser xmlPullParser, int i, Context context) throws XmlPullParserException, IOException {
            if (xmlPullParser.getName().equals("phone_account")) {
                int depth = xmlPullParser.getDepth();
                PhoneAccountHandle fromXml = null;
                Uri uri = null;
                Uri uri2 = null;
                int i2 = 0;
                int i3 = -1;
                String packageName = null;
                Bitmap bitmap = null;
                int i4 = 0;
                int i5 = 0;
                String text = null;
                String text2 = null;
                List<String> arrayList = null;
                while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                    if (xmlPullParser.getName().equals("account_handle")) {
                        xmlPullParser.nextTag();
                        fromXml = PhoneAccountRegistrar.sPhoneAccountHandleXml.readFromXml(xmlPullParser, i, context);
                    } else if (xmlPullParser.getName().equals("handle")) {
                        xmlPullParser.next();
                        uri = Uri.parse(xmlPullParser.getText());
                    } else if (xmlPullParser.getName().equals("subscription_number")) {
                        xmlPullParser.next();
                        String text3 = xmlPullParser.getText();
                        uri2 = text3 == null ? null : Uri.parse(text3);
                    } else if (xmlPullParser.getName().equals("capabilities")) {
                        xmlPullParser.next();
                        i2 = Integer.parseInt(xmlPullParser.getText());
                    } else if (xmlPullParser.getName().equals("icon_res_id")) {
                        xmlPullParser.next();
                        i3 = Integer.parseInt(xmlPullParser.getText());
                    } else if (xmlPullParser.getName().equals("icon_package_name")) {
                        xmlPullParser.next();
                        packageName = xmlPullParser.getText();
                    } else if (xmlPullParser.getName().equals("icon_bitmap")) {
                        xmlPullParser.next();
                        bitmap = readBitmap(xmlPullParser);
                    } else if (xmlPullParser.getName().equals("icon_tint")) {
                        xmlPullParser.next();
                        i4 = Integer.parseInt(xmlPullParser.getText());
                    } else if (xmlPullParser.getName().equals("highlight_color")) {
                        xmlPullParser.next();
                        i5 = Integer.parseInt(xmlPullParser.getText());
                    } else if (xmlPullParser.getName().equals("label")) {
                        xmlPullParser.next();
                        text = xmlPullParser.getText();
                    } else if (xmlPullParser.getName().equals("short_description")) {
                        xmlPullParser.next();
                        text2 = xmlPullParser.getText();
                    } else if (xmlPullParser.getName().equals("supported_uri_schemes")) {
                        arrayList = readStringList(xmlPullParser);
                    }
                }
                if (i < 2) {
                    ComponentName componentName = new ComponentName("com.android.phone", "com.android.services.telephony.sip.SipConnectionService");
                    arrayList = new ArrayList<>();
                    if (fromXml.getComponentName().equals(componentName)) {
                        boolean zUseSipForPstnCalls = useSipForPstnCalls(context);
                        arrayList.add("sip");
                        if (zUseSipForPstnCalls) {
                            arrayList.add("tel");
                        }
                    } else {
                        arrayList.add("tel");
                        arrayList.add("voicemail");
                    }
                }
                if (i < 5 && bitmap == null) {
                    packageName = fromXml.getComponentName().getPackageName();
                }
                PhoneAccount.Builder highlightColor = PhoneAccount.builder(fromXml, text).setAddress(uri).setSubscriptionAddress(uri2).setCapabilities(i2).setShortDescription(text2).setSupportedUriSchemes(arrayList).setHighlightColor(i5);
                if (bitmap == null) {
                    highlightColor.setIcon(packageName, i3, i4);
                } else {
                    highlightColor.setIcon(bitmap);
                }
                return highlightColor.build();
            }
            return null;
        }

        private boolean useSipForPstnCalls(Context context) {
            String string = Settings.System.getString(context.getContentResolver(), "sip_call_options");
            if (string == null) {
                string = "SIP_ADDRESS_ONLY";
            }
            return string.equals("SIP_ALWAYS");
        }
    };
    public static final XmlSerialization<PhoneAccountHandle> sPhoneAccountHandleXml = new XmlSerialization<PhoneAccountHandle>() {
        @Override
        public void writeToXml(PhoneAccountHandle phoneAccountHandle, XmlSerializer xmlSerializer, Context context) throws IOException {
            if (phoneAccountHandle != null) {
                xmlSerializer.startTag(null, "phone_account_handle");
                if (phoneAccountHandle.getComponentName() != null) {
                    writeTextIfNonNull("component_name", phoneAccountHandle.getComponentName().flattenToString(), xmlSerializer);
                }
                writeTextIfNonNull("id", phoneAccountHandle.getId(), xmlSerializer);
                if (phoneAccountHandle.getUserHandle() != null && context != null) {
                    writeLong("user_serial_number", UserManager.get(context).getSerialNumberForUser(phoneAccountHandle.getUserHandle()), xmlSerializer);
                }
                xmlSerializer.endTag(null, "phone_account_handle");
            }
        }

        @Override
        public PhoneAccountHandle readFromXml(XmlPullParser xmlPullParser, int i, Context context) throws XmlPullParserException, IOException {
            UserHandle userForSerialNumber = null;
            if (!xmlPullParser.getName().equals("phone_account_handle")) {
                return null;
            }
            int depth = xmlPullParser.getDepth();
            UserManager userManager = UserManager.get(context);
            String text = null;
            String text2 = null;
            String text3 = null;
            while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                if (xmlPullParser.getName().equals("component_name")) {
                    xmlPullParser.next();
                    text3 = xmlPullParser.getText();
                } else if (xmlPullParser.getName().equals("id")) {
                    xmlPullParser.next();
                    text2 = xmlPullParser.getText();
                } else if (xmlPullParser.getName().equals("user_serial_number")) {
                    xmlPullParser.next();
                    text = xmlPullParser.getText();
                }
            }
            if (text3 == null) {
                return null;
            }
            if (text != null) {
                try {
                    userForSerialNumber = userManager.getUserForSerialNumber(Long.parseLong(text));
                } catch (NumberFormatException e) {
                    Log.e(this, e, "Could not parse UserHandle " + text, new Object[0]);
                }
            }
            return new PhoneAccountHandle(ComponentName.unflattenFromString(text3), text2, userForSerialNumber);
        }
    };

    public static class State {
        public PhoneAccountHandle simCallManager;
        public int versionNumber;
        public PhoneAccountHandle defaultOutgoing = null;
        public final List<PhoneAccount> accounts = new ArrayList();
    }

    public static abstract class Listener {
        public void onAccountsChanged(PhoneAccountRegistrar phoneAccountRegistrar) {
        }

        public void onDefaultOutgoingChanged(PhoneAccountRegistrar phoneAccountRegistrar) {
        }

        public void onSimCallManagerChanged(PhoneAccountRegistrar phoneAccountRegistrar) {
        }
    }

    public PhoneAccountRegistrar(Context context) {
        this(context, "phone-account-registrar-state.xml");
    }

    public PhoneAccountRegistrar(Context context, String str) {
        this.mListeners = new CopyOnWriteArrayList();
        this.mAtomicFile = new AtomicFile(new File(context.getFilesDir(), str));
        this.mState = new State();
        this.mContext = context;
        this.mUserManager = UserManager.get(context);
        this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
        this.mCurrentUserHandle = Process.myUserHandle();
        read();
    }

    public int getSubscriptionIdForPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        PhoneAccount phoneAccountInternal = getPhoneAccountInternal(phoneAccountHandle);
        if (phoneAccountInternal != null && phoneAccountInternal.hasCapabilities(4) && TextUtils.isDigitsOnly(phoneAccountHandle.getId()) && isVisibleForUser(phoneAccountHandle)) {
            return Integer.parseInt(phoneAccountHandle.getId());
        }
        return -1;
    }

    public PhoneAccountHandle getDefaultOutgoingPhoneAccount(String str) {
        PhoneAccountHandle userSelectedOutgoingPhoneAccount = getUserSelectedOutgoingPhoneAccount();
        if (userSelectedOutgoingPhoneAccount == null || !getPhoneAccountInternal(userSelectedOutgoingPhoneAccount).supportsUriScheme(str) || !isVisibleForUser(userSelectedOutgoingPhoneAccount)) {
            List<PhoneAccountHandle> callCapablePhoneAccounts = getCallCapablePhoneAccounts(str);
            switch (callCapablePhoneAccounts.size()) {
                case 0:
                    return null;
                case 1:
                    if (isVisibleForUser(callCapablePhoneAccounts.get(0))) {
                        return callCapablePhoneAccounts.get(0);
                    }
                    return null;
                default:
                    return null;
            }
        }
        return userSelectedOutgoingPhoneAccount;
    }

    PhoneAccountHandle getUserSelectedOutgoingPhoneAccount() {
        if (this.mState.defaultOutgoing != null) {
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= this.mState.accounts.size()) {
                    break;
                }
                if (!this.mState.accounts.get(i2).getAccountHandle().equals(this.mState.defaultOutgoing) || !isVisibleForUser(this.mState.defaultOutgoing)) {
                    i = i2 + 1;
                } else {
                    return this.mState.defaultOutgoing;
                }
            }
        }
        return null;
    }

    public void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        boolean z;
        if (phoneAccountHandle == null) {
            this.mState.defaultOutgoing = null;
        } else {
            Iterator<PhoneAccount> it = this.mState.accounts.iterator();
            while (true) {
                if (!it.hasNext()) {
                    z = false;
                    break;
                } else if (Objects.equals(phoneAccountHandle, it.next().getAccountHandle())) {
                    z = true;
                    break;
                }
            }
            if (!z) {
                Log.w(this, "Trying to set nonexistent default outgoing %s", phoneAccountHandle);
                return;
            } else if (!getPhoneAccountInternal(phoneAccountHandle).hasCapabilities(2)) {
                Log.w(this, "Trying to set non-call-provider default outgoing %s", phoneAccountHandle);
                return;
            } else {
                if (getPhoneAccountInternal(phoneAccountHandle).hasCapabilities(4)) {
                    this.mSubscriptionManager.setDefaultVoiceSubId(getSubscriptionIdForPhoneAccount(phoneAccountHandle));
                }
                this.mState.defaultOutgoing = phoneAccountHandle;
            }
        }
        write();
        fireDefaultOutgoingChanged();
    }

    boolean isUserSelectedSmsPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        return getSubscriptionIdForPhoneAccount(phoneAccountHandle) == SubscriptionManager.getDefaultSmsSubId();
    }

    public void setSimCallManager(PhoneAccountHandle phoneAccountHandle) {
        if (phoneAccountHandle != null) {
            PhoneAccount phoneAccountInternal = getPhoneAccountInternal(phoneAccountHandle);
            if (phoneAccountInternal == null) {
                Log.d(this, "setSimCallManager: Nonexistent call manager: %s", phoneAccountHandle);
                return;
            } else if (!phoneAccountInternal.hasCapabilities(1)) {
                Log.d(this, "setSimCallManager: Not a call manager: %s", phoneAccountInternal);
                return;
            }
        } else {
            phoneAccountHandle = NO_ACCOUNT_SELECTED;
        }
        this.mState.simCallManager = phoneAccountHandle;
        write();
        fireSimCallManagerChanged();
    }

    public PhoneAccountHandle getSimCallManager() {
        if (this.mState.simCallManager != null) {
            if (NO_ACCOUNT_SELECTED.equals(this.mState.simCallManager)) {
                return null;
            }
            for (int i = 0; i < this.mState.accounts.size(); i++) {
                if (this.mState.accounts.get(i).getAccountHandle().equals(this.mState.simCallManager) && !resolveComponent(this.mState.simCallManager).isEmpty() && isVisibleForUser(this.mState.simCallManager)) {
                    return this.mState.simCallManager;
                }
            }
        }
        String string = this.mContext.getResources().getString(R.string.default_connection_manager_component);
        if (!TextUtils.isEmpty(string)) {
            ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(string);
            if (!resolveComponent(componentNameUnflattenFromString, null).isEmpty()) {
                for (PhoneAccountHandle phoneAccountHandle : getAllPhoneAccountHandles()) {
                    if (componentNameUnflattenFromString.equals(phoneAccountHandle.getComponentName()) && isVisibleForUser(phoneAccountHandle)) {
                        return phoneAccountHandle;
                    }
                }
                Log.d(this, "%s does not have a PhoneAccount; not using as default", componentNameUnflattenFromString);
            } else {
                Log.d(this, "%s could not be resolved; not using as default", componentNameUnflattenFromString);
            }
        } else {
            Log.v(this, "No default connection manager specified", new Object[0]);
        }
        return null;
    }

    PhoneAccount getPhoneAccountInternal(PhoneAccountHandle phoneAccountHandle) {
        for (PhoneAccount phoneAccount : this.mState.accounts) {
            if (Objects.equals(phoneAccountHandle, phoneAccount.getAccountHandle())) {
                return phoneAccount;
            }
        }
        return null;
    }

    public void setCurrentUserHandle(UserHandle userHandle) {
        if (userHandle == null) {
            Log.d(this, "setCurrentUserHandle, userHandle = null", new Object[0]);
            userHandle = Process.myUserHandle();
        }
        Log.d(this, "setCurrentUserHandle, %s", userHandle);
        this.mCurrentUserHandle = userHandle;
    }

    private boolean isVisibleForUser(PhoneAccountHandle phoneAccountHandle) {
        if (phoneAccountHandle == null) {
            return false;
        }
        return isVisibleForUser(getPhoneAccountInternal(phoneAccountHandle));
    }

    private boolean isVisibleForUser(PhoneAccount phoneAccount) {
        if (phoneAccount == null) {
            return false;
        }
        if (phoneAccount.hasCapabilities(32)) {
            return true;
        }
        UserHandle userHandle = phoneAccount.getAccountHandle().getUserHandle();
        if (userHandle == null) {
            return false;
        }
        if (this.mCurrentUserHandle == null) {
            Log.d(this, "Current user is null; assuming true", new Object[0]);
            return true;
        }
        Iterator it = this.mUserManager.getProfiles(this.mCurrentUserHandle.getIdentifier()).iterator();
        while (it.hasNext()) {
            if (((UserInfo) it.next()).getUserHandle().equals(userHandle)) {
                return true;
            }
        }
        return false;
    }

    private List<ResolveInfo> resolveComponent(PhoneAccountHandle phoneAccountHandle) {
        return resolveComponent(phoneAccountHandle.getComponentName(), phoneAccountHandle.getUserHandle());
    }

    private List<ResolveInfo> resolveComponent(ComponentName componentName, UserHandle userHandle) {
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = new Intent("android.telecom.ConnectionService");
        intent.setComponent(componentName);
        return userHandle != null ? packageManager.queryIntentServicesAsUser(intent, 0, userHandle.getIdentifier()) : packageManager.queryIntentServices(intent, 0);
    }

    public List<PhoneAccountHandle> getAllPhoneAccountHandles() {
        ArrayList arrayList = new ArrayList();
        for (PhoneAccount phoneAccount : this.mState.accounts) {
            if (isVisibleForUser(phoneAccount)) {
                arrayList.add(phoneAccount.getAccountHandle());
            }
        }
        return arrayList;
    }

    public List<PhoneAccount> getAllPhoneAccounts() {
        ArrayList arrayList = new ArrayList(this.mState.accounts.size());
        for (PhoneAccount phoneAccount : this.mState.accounts) {
            if (isVisibleForUser(phoneAccount)) {
                arrayList.add(phoneAccount);
            }
        }
        return arrayList;
    }

    public List<PhoneAccountHandle> getCallCapablePhoneAccounts() {
        return getPhoneAccountHandles(2);
    }

    public List<PhoneAccountHandle> getCallCapablePhoneAccounts(String str) {
        return getPhoneAccountHandles(2, str);
    }

    public List<PhoneAccountHandle> getPhoneAccountsForPackage(String str) {
        ArrayList arrayList = new ArrayList();
        for (PhoneAccount phoneAccount : this.mState.accounts) {
            if (Objects.equals(str, phoneAccount.getAccountHandle().getComponentName().getPackageName()) && isVisibleForUser(phoneAccount)) {
                arrayList.add(phoneAccount.getAccountHandle());
            }
        }
        return arrayList;
    }

    public List<PhoneAccountHandle> getConnectionManagerPhoneAccounts() {
        return getPhoneAccountHandles(1, null);
    }

    public PhoneAccount getPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        for (PhoneAccount phoneAccount : this.mState.accounts) {
            if (Objects.equals(phoneAccountHandle, phoneAccount.getAccountHandle()) && isVisibleForUser(phoneAccount)) {
                return phoneAccount;
            }
        }
        return null;
    }

    public void registerPhoneAccount(PhoneAccount phoneAccount) {
        if (!phoneAccountHasPermission(phoneAccount.getAccountHandle())) {
            Log.w(this, "Phone account %s does not have BIND_CONNECTION_SERVICE permission.", phoneAccount.getAccountHandle());
            throw new SecurityException("PhoneAccount connection service requires BIND_CONNECTION_SERVICE permission.");
        }
        addOrReplacePhoneAccount(phoneAccount);
    }

    private void addOrReplacePhoneAccount(PhoneAccount phoneAccount) {
        int i = 0;
        Log.d(this, "addOrReplacePhoneAccount(%s -> %s)", phoneAccount.getAccountHandle(), phoneAccount);
        this.mState.accounts.add(phoneAccount);
        while (true) {
            int i2 = i;
            if (i2 >= this.mState.accounts.size() - 1) {
                break;
            }
            if (!Objects.equals(phoneAccount.getAccountHandle(), this.mState.accounts.get(i2).getAccountHandle())) {
                i = i2 + 1;
            } else {
                this.mState.accounts.remove(i2);
                break;
            }
        }
        write();
        fireAccountsChanged();
    }

    public void unregisterPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.mState.accounts.size()) {
                break;
            }
            if (!Objects.equals(phoneAccountHandle, this.mState.accounts.get(i2).getAccountHandle())) {
                i = i2 + 1;
            } else {
                this.mState.accounts.remove(i2);
                break;
            }
        }
        write();
        fireAccountsChanged();
    }

    public void clearAccounts(String str, UserHandle userHandle) {
        Iterator<PhoneAccount> it = this.mState.accounts.iterator();
        boolean z = false;
        while (it.hasNext()) {
            PhoneAccount next = it.next();
            PhoneAccountHandle accountHandle = next.getAccountHandle();
            if (Objects.equals(str, accountHandle.getComponentName().getPackageName()) && Objects.equals(userHandle, accountHandle.getUserHandle())) {
                Log.i(this, "Removing phone account " + ((Object) next.getLabel()), new Object[0]);
                it.remove();
                z = true;
            }
            z = z;
        }
        if (z) {
            write();
            fireAccountsChanged();
        }
    }

    public boolean isVoiceMailNumber(PhoneAccountHandle phoneAccountHandle, String str) {
        return PhoneNumberUtils.isVoiceMailNumber(getSubscriptionIdForPhoneAccount(phoneAccountHandle), str);
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    private void fireAccountsChanged() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onAccountsChanged(this);
        }
    }

    private void fireDefaultOutgoingChanged() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDefaultOutgoingChanged(this);
        }
    }

    private void fireSimCallManagerChanged() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onSimCallManagerChanged(this);
        }
    }

    public boolean phoneAccountHasPermission(PhoneAccountHandle phoneAccountHandle) {
        try {
            ServiceInfo serviceInfo = this.mContext.getPackageManager().getServiceInfo(phoneAccountHandle.getComponentName(), 0);
            if (serviceInfo.permission != null) {
                if (serviceInfo.permission.equals("android.permission.BIND_CONNECTION_SERVICE")) {
                    return true;
                }
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(this, "Name not found %s", e);
            return false;
        }
    }

    private List<PhoneAccountHandle> getPhoneAccountHandles(int i) {
        return getPhoneAccountHandles(i, null);
    }

    private List<PhoneAccountHandle> getPhoneAccountHandles(int i, String str) {
        ArrayList arrayList = new ArrayList();
        for (PhoneAccount phoneAccount : this.mState.accounts) {
            if (phoneAccount.hasCapabilities(i) && (str == null || phoneAccount.supportsUriScheme(str))) {
                if (!resolveComponent(phoneAccount.getAccountHandle()).isEmpty() && isVisibleForUser(phoneAccount)) {
                    arrayList.add(phoneAccount.getAccountHandle());
                }
            }
        }
        return arrayList;
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        if (this.mState != null) {
            indentingPrintWriter.println("xmlVersion: " + this.mState.versionNumber);
            indentingPrintWriter.println("defaultOutgoing: " + (this.mState.defaultOutgoing == null ? "none" : this.mState.defaultOutgoing));
            indentingPrintWriter.println("simCallManager: " + (this.mState.simCallManager == null ? "none" : this.mState.simCallManager));
            indentingPrintWriter.println("phoneAccounts:");
            indentingPrintWriter.increaseIndent();
            Iterator<PhoneAccount> it = this.mState.accounts.iterator();
            while (it.hasNext()) {
                indentingPrintWriter.println(it.next());
            }
            indentingPrintWriter.decreaseIndent();
        }
    }

    private void write() {
        try {
            FileOutputStream fileOutputStreamStartWrite = this.mAtomicFile.startWrite();
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(new BufferedOutputStream(fileOutputStreamStartWrite), "utf-8");
                writeToXml(this.mState, fastXmlSerializer, this.mContext);
                fastXmlSerializer.flush();
                this.mAtomicFile.finishWrite(fileOutputStreamStartWrite);
            } catch (Throwable th) {
                this.mAtomicFile.failWrite(fileOutputStreamStartWrite);
                throw th;
            }
        } catch (IOException e) {
            Log.e(this, e, "Writing state to XML file", new Object[0]);
        }
    }

    private void read() {
        try {
            ?? OpenRead = this.mAtomicFile.openRead();
            try {
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(new BufferedInputStream(OpenRead), null);
                    xmlPullParserNewPullParser.nextTag();
                    this.mState = readFromXml(xmlPullParserNewPullParser, this.mContext);
                    boolean z = this.mState.versionNumber < 5;
                    try {
                        OpenRead.close();
                        OpenRead = z;
                    } catch (IOException e) {
                        Log.e(this, e, "Closing InputStream", new Object[0]);
                        OpenRead = z;
                    }
                } catch (Throwable th) {
                    try {
                        OpenRead.close();
                    } catch (IOException e2) {
                        Log.e(this, e2, "Closing InputStream", new Object[0]);
                    }
                    throw th;
                }
            } catch (IOException | XmlPullParserException e3) {
                Log.e(this, e3, "Reading state from XML file", new Object[0]);
                this.mState = new State();
                try {
                    OpenRead.close();
                    OpenRead = 0;
                } catch (IOException e4) {
                    Log.e(this, e4, "Closing InputStream", new Object[0]);
                    OpenRead = 0;
                }
            }
            ArrayList arrayList = new ArrayList();
            for (PhoneAccount phoneAccount : this.mState.accounts) {
                UserHandle userHandle = phoneAccount.getAccountHandle().getUserHandle();
                if (userHandle == null) {
                    Log.w(this, "Missing UserHandle for %s", phoneAccount);
                    arrayList.add(phoneAccount);
                } else if (this.mUserManager.getSerialNumberForUser(userHandle) == -1) {
                    Log.w(this, "User does not exist for %s", phoneAccount);
                    arrayList.add(phoneAccount);
                }
            }
            this.mState.accounts.removeAll(arrayList);
            if (OpenRead != 0 || !arrayList.isEmpty()) {
                write();
            }
        } catch (FileNotFoundException e5) {
        }
    }

    private static void writeToXml(State state, XmlSerializer xmlSerializer, Context context) throws IOException {
        sStateXml.writeToXml(state, xmlSerializer, context);
    }

    private static State readFromXml(XmlPullParser xmlPullParser, Context context) throws XmlPullParserException, IOException {
        State fromXml = sStateXml.readFromXml(xmlPullParser, 0, context);
        return fromXml != null ? fromXml : new State();
    }

    public static abstract class XmlSerialization<T> {
        public abstract T readFromXml(XmlPullParser xmlPullParser, int i, Context context) throws XmlPullParserException, IOException;

        public abstract void writeToXml(T t, XmlSerializer xmlSerializer, Context context) throws IOException;

        protected void writeTextIfNonNull(String str, Object obj, XmlSerializer xmlSerializer) throws IOException {
            if (obj != null) {
                xmlSerializer.startTag(null, str);
                xmlSerializer.text(Objects.toString(obj));
                xmlSerializer.endTag(null, str);
            }
        }

        protected void writeStringList(String str, List<String> list, XmlSerializer xmlSerializer) throws IOException {
            xmlSerializer.startTag(null, str);
            if (list != null) {
                xmlSerializer.attribute(null, "length", Objects.toString(Integer.valueOf(list.size())));
                for (String str2 : list) {
                    xmlSerializer.startTag(null, "value");
                    if (str2 != null) {
                        xmlSerializer.text(str2);
                    }
                    xmlSerializer.endTag(null, "value");
                }
            } else {
                xmlSerializer.attribute(null, "length", "0");
            }
            xmlSerializer.endTag(null, str);
        }

        protected void writeBitmapIfNonNull(String str, Bitmap bitmap, XmlSerializer xmlSerializer) throws IOException {
            if (bitmap != null && bitmap.getByteCount() > 0) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String strEncodeToString = Base64.encodeToString(byteArray, 0, byteArray.length, 0);
                xmlSerializer.startTag(null, str);
                xmlSerializer.text(strEncodeToString);
                xmlSerializer.endTag(null, str);
            }
        }

        protected void writeLong(String str, long j, XmlSerializer xmlSerializer) throws IOException {
            xmlSerializer.startTag(null, str);
            xmlSerializer.text(Long.valueOf(j).toString());
            xmlSerializer.endTag(null, str);
        }

        protected List<String> readStringList(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "length"));
            ArrayList arrayList = new ArrayList(i);
            if (i != 0) {
                int depth = xmlPullParser.getDepth();
                while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
                    if (xmlPullParser.getName().equals("value")) {
                        xmlPullParser.next();
                        arrayList.add(xmlPullParser.getText());
                    }
                }
            }
            return arrayList;
        }

        protected Bitmap readBitmap(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            byte[] bArrDecode = Base64.decode(xmlPullParser.getText(), 0);
            return BitmapFactory.decodeByteArray(bArrDecode, 0, bArrDecode.length);
        }
    }
}

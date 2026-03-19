package com.android.se.security.arf;

import com.android.se.Terminal;
import com.android.se.security.AccessRuleCache;
import com.android.se.security.arf.pkcs15.PKCS15Handler;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;

public class ArfController {
    private AccessRuleCache mAccessRuleCache;
    private PKCS15Handler mPkcs15Handler = null;
    private SecureElement mSecureElement = null;
    private Terminal mTerminal;

    public ArfController(AccessRuleCache accessRuleCache, Terminal terminal) {
        this.mAccessRuleCache = null;
        this.mTerminal = null;
        this.mAccessRuleCache = accessRuleCache;
        this.mTerminal = terminal;
    }

    public synchronized boolean initialize() throws MissingResourceException, IOException, NoSuchElementException {
        if (this.mSecureElement == null) {
            this.mSecureElement = new SecureElement(this, this.mTerminal);
        }
        if (this.mPkcs15Handler == null) {
            this.mPkcs15Handler = new PKCS15Handler(this.mSecureElement);
        }
        return this.mPkcs15Handler.loadAccessControlRules(this.mTerminal.getName());
    }

    public AccessRuleCache getAccessRuleCache() {
        return this.mAccessRuleCache;
    }
}

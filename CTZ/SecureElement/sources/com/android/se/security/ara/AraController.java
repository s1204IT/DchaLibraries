package com.android.se.security.ara;

import android.util.Log;
import com.android.se.Channel;
import com.android.se.Terminal;
import com.android.se.security.AccessRuleCache;
import com.android.se.security.ChannelAccess;
import com.android.se.security.arf.ASN1;
import com.android.se.security.gpac.ParserException;
import com.android.se.security.gpac.REF_AR_DO;
import com.android.se.security.gpac.Response_ALL_AR_DO;
import com.android.se.security.gpac.Response_DO_Factory;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;

public class AraController {
    public static final byte[] ARA_M_AID = {ASN1.TAG_PrivateKey, 0, 0, 1, ASN1.TAG_ApplPath, 65, 67, 76, 0};
    private AccessRuleCache mAccessRuleCache;
    private Terminal mTerminal;
    private final String mTag = "SecureElement-AraController";
    private AccessRuleApplet mApplet = null;

    public AraController(AccessRuleCache accessRuleCache, Terminal terminal) {
        this.mAccessRuleCache = null;
        this.mTerminal = null;
        this.mAccessRuleCache = accessRuleCache;
        this.mTerminal = terminal;
    }

    public static byte[] getAraMAid() {
        return ARA_M_AID;
    }

    public synchronized void initialize() throws IOException, NoSuchElementException {
        Channel channelOpenLogicalChannelWithoutChannelAccess = this.mTerminal.openLogicalChannelWithoutChannelAccess(getAraMAid());
        if (channelOpenLogicalChannelWithoutChannelAccess == null) {
            throw new MissingResourceException("could not open channel", "", "");
        }
        ChannelAccess channelAccess = new ChannelAccess();
        channelAccess.setAccess(ChannelAccess.ACCESS.ALLOWED, "SecureElement-AraController");
        channelAccess.setApduAccess(ChannelAccess.ACCESS.ALLOWED);
        channelOpenLogicalChannelWithoutChannelAccess.setChannelAccess(channelAccess);
        try {
            try {
                try {
                    this.mApplet = new AccessRuleApplet(channelOpenLogicalChannelWithoutChannelAccess);
                    byte[] refreshTag = this.mApplet.readRefreshTag();
                    if (this.mAccessRuleCache.isRefreshTagEqual(refreshTag)) {
                        Log.i("SecureElement-AraController", "Refresh tag unchanged. Using access rules from cache.");
                        return;
                    }
                    Log.i("SecureElement-AraController", "Refresh tag has changed.");
                    this.mAccessRuleCache.setRefreshTag(refreshTag);
                    this.mAccessRuleCache.clearCache();
                    Log.i("SecureElement-AraController", "Read ARs from ARA");
                    readAllAccessRules();
                    if (channelOpenLogicalChannelWithoutChannelAccess != null) {
                        channelOpenLogicalChannelWithoutChannelAccess.close();
                    }
                } catch (Exception e) {
                    Log.i("SecureElement-AraController", "ARA error: " + e.getLocalizedMessage());
                    throw new AccessControlException(e.getLocalizedMessage());
                }
            } catch (IOException e2) {
                throw e2;
            }
        } finally {
            if (channelOpenLogicalChannelWithoutChannelAccess != null) {
                channelOpenLogicalChannelWithoutChannelAccess.close();
            }
        }
    }

    private void readAllAccessRules() throws AccessControlException, IOException {
        try {
            byte[] allAccessRules = this.mApplet.readAllAccessRules();
            if (allAccessRules == null) {
                return;
            }
            ?? CreateDO = Response_DO_Factory.createDO(allAccessRules);
            if (CreateDO == 0 || !(CreateDO instanceof Response_ALL_AR_DO)) {
                throw new AccessControlException("No valid data object found");
            }
            ArrayList<REF_AR_DO> refArDos = CreateDO.getRefArDos();
            if (refArDos != null && refArDos.size() != 0) {
                for (REF_AR_DO ref_ar_do : refArDos) {
                    this.mAccessRuleCache.putWithMerge(ref_ar_do.getRefDo(), ref_ar_do.getArDo());
                }
            }
        } catch (ParserException e) {
            throw new AccessControlException("Parsing Data Object Exception: " + e.getMessage());
        }
    }
}

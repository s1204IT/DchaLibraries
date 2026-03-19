package com.android.se.security.arf;

import android.util.Log;
import com.android.se.Channel;
import com.android.se.Terminal;
import com.android.se.security.ChannelAccess;
import com.android.se.security.arf.pkcs15.EF;
import com.android.se.security.gpac.AID_REF_DO;
import com.android.se.security.gpac.Hash_REF_DO;
import com.android.se.security.gpac.REF_DO;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;

public class SecureElement {
    private ArfController mArfHandler;
    private Terminal mTerminalHandle;
    public final String mTag = "SecureElement-ARF";
    private Channel mArfChannel = null;

    public SecureElement(ArfController arfController, Terminal terminal) {
        this.mTerminalHandle = null;
        this.mArfHandler = null;
        this.mTerminalHandle = terminal;
        this.mArfHandler = arfController;
    }

    public byte[] exchangeAPDU(EF ef, byte[] bArr) throws SecureElementException, IOException {
        try {
            return this.mArfChannel.transmit(bArr);
        } catch (IOException e) {
            throw e;
        } catch (Exception e2) {
            throw new SecureElementException("Secure Element access error " + e2.getLocalizedMessage());
        }
    }

    public Channel openLogicalArfChannel(byte[] bArr) throws MissingResourceException, IOException, NoSuchElementException {
        try {
            this.mArfChannel = this.mTerminalHandle.openLogicalChannelWithoutChannelAccess(bArr);
            if (this.mArfChannel == null) {
                throw new MissingResourceException("No channel was available", "", "");
            }
            setUpChannelAccess(this.mArfChannel);
            return this.mArfChannel;
        } catch (IOException | MissingResourceException | NoSuchElementException e) {
            throw e;
        } catch (Exception e2) {
            Log.e("SecureElement-ARF", "Error opening logical channel " + e2.getLocalizedMessage());
            this.mArfChannel = null;
            return null;
        }
    }

    public void closeArfChannel() {
        try {
            if (this.mArfChannel != null) {
                this.mArfChannel.close();
                this.mArfChannel = null;
            }
        } catch (Exception e) {
            Log.e("SecureElement-ARF", "Error closing channel " + e.getLocalizedMessage());
        }
    }

    private void setUpChannelAccess(Channel channel) {
        ChannelAccess channelAccess = new ChannelAccess();
        channelAccess.setAccess(ChannelAccess.ACCESS.ALLOWED, "");
        channelAccess.setApduAccess(ChannelAccess.ACCESS.ALLOWED);
        channel.setChannelAccess(channelAccess);
    }

    public byte[] getRefreshTag() {
        if (this.mArfHandler != null) {
            return this.mArfHandler.getAccessRuleCache().getRefreshTag();
        }
        return null;
    }

    public void setRefreshTag(byte[] bArr) {
        if (this.mArfHandler != null) {
            this.mArfHandler.getAccessRuleCache().setRefreshTag(bArr);
        }
    }

    public void putAccessRule(AID_REF_DO aid_ref_do, Hash_REF_DO hash_REF_DO, ChannelAccess channelAccess) {
        this.mArfHandler.getAccessRuleCache().putWithMerge(new REF_DO(aid_ref_do, hash_REF_DO), channelAccess);
    }

    public void resetAccessRules() {
        this.mArfHandler.getAccessRuleCache().reset();
    }

    public void clearAccessRuleCache() {
        this.mArfHandler.getAccessRuleCache().clearCache();
    }
}

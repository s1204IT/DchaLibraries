package com.android.contacts.logging;

import com.google.common.base.MoreObjects;

public final class QuickContactEvent {
    public int actionType;
    public int cardType;
    public int contactType;
    public String referrer;
    public String thirdPartyAction;

    public String toString() {
        return MoreObjects.toStringHelper(this).add("referrer", this.referrer).add("contactType", this.contactType).add("cardType", this.cardType).add("actionType", this.actionType).add("thirdPartyAction", this.thirdPartyAction).toString();
    }
}

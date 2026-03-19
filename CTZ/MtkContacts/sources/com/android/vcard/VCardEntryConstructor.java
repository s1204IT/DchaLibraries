package com.android.vcard;

import android.accounts.Account;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VCardEntryConstructor implements VCardInterpreter {
    private static String LOG_TAG = "MTK_vCard";
    private final Account mAccount;
    private VCardEntry mCurrentEntry;
    private final List<VCardEntryHandler> mEntryHandlers;
    private final List<VCardEntry> mEntryStack;
    private final int mVCardType;

    public VCardEntryConstructor() {
        this(-1073741824, null, null);
    }

    @Deprecated
    public VCardEntryConstructor(int i, Account account, String str) {
        this.mEntryStack = new ArrayList();
        this.mEntryHandlers = new ArrayList();
        this.mVCardType = i;
        this.mAccount = account;
    }

    public void addEntryHandler(VCardEntryHandler vCardEntryHandler) {
        this.mEntryHandlers.add(vCardEntryHandler);
    }

    @Override
    public void onVCardStarted() {
        Iterator<VCardEntryHandler> it = this.mEntryHandlers.iterator();
        while (it.hasNext()) {
            it.next().onStart();
        }
    }

    @Override
    public void onVCardEnded() {
        Iterator<VCardEntryHandler> it = this.mEntryHandlers.iterator();
        while (it.hasNext()) {
            it.next().onEnd();
        }
    }

    public void clear() {
        this.mCurrentEntry = null;
        this.mEntryStack.clear();
    }

    @Override
    public void onEntryStarted() {
        this.mCurrentEntry = new VCardEntry(this.mVCardType, this.mAccount);
        this.mEntryStack.add(this.mCurrentEntry);
    }

    @Override
    public void onEntryEnded() {
        this.mCurrentEntry.consolidateFields();
        Iterator<VCardEntryHandler> it = this.mEntryHandlers.iterator();
        while (it.hasNext()) {
            it.next().onEntryCreated(this.mCurrentEntry);
        }
        int size = this.mEntryStack.size();
        if (size > 1) {
            VCardEntry vCardEntry = this.mEntryStack.get(size - 2);
            vCardEntry.addChild(this.mCurrentEntry);
            this.mCurrentEntry = vCardEntry;
        } else {
            this.mCurrentEntry = null;
        }
        this.mEntryStack.remove(size - 1);
    }

    @Override
    public void onPropertyCreated(VCardProperty vCardProperty) {
        this.mCurrentEntry.addProperty(vCardProperty);
    }
}

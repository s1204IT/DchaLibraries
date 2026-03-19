package com.android.bluetooth.pbapclient;

import android.accounts.Account;
import com.android.vcard.VCardConfig;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardEntryCounter;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardInterpreter;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.exception.VCardException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

class BluetoothPbapVcardList {
    private final Account mAccount;
    private final ArrayList<VCardEntry> mCards = new ArrayList<>();

    class CardEntryHandler implements VCardEntryHandler {
        CardEntryHandler() {
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onEntryCreated(VCardEntry vCardEntry) {
            BluetoothPbapVcardList.this.mCards.add(vCardEntry);
        }

        @Override
        public void onEnd() {
        }
    }

    BluetoothPbapVcardList(Account account, InputStream inputStream, byte b) throws IOException {
        this.mAccount = account;
        parse(inputStream, b);
    }

    private void parse(InputStream inputStream, byte b) throws IOException {
        VCardParser vCardParser_V21;
        if (b == 1) {
            vCardParser_V21 = new VCardParser_V30();
        } else {
            vCardParser_V21 = new VCardParser_V21();
        }
        VCardEntryConstructor vCardEntryConstructor = new VCardEntryConstructor(VCardConfig.VCARD_TYPE_V21_GENERIC, this.mAccount);
        VCardInterpreter vCardEntryCounter = new VCardEntryCounter();
        vCardEntryConstructor.addEntryHandler(new CardEntryHandler());
        vCardParser_V21.addInterpreter(vCardEntryConstructor);
        vCardParser_V21.addInterpreter(vCardEntryCounter);
        try {
            vCardParser_V21.parse(inputStream);
        } catch (VCardException e) {
            e.printStackTrace();
        }
    }

    public int getCount() {
        return this.mCards.size();
    }

    public ArrayList<VCardEntry> getList() {
        return this.mCards;
    }

    public VCardEntry getFirst() {
        return this.mCards.get(0);
    }
}

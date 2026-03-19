package com.google.i18n.phonenumbers.prefixmapper;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;

class DefaultMapStorage extends PhonePrefixMapStorageStrategy {
    private String[] descriptions;
    private int[] phoneNumberPrefixes;

    @Override
    public int getPrefix(int i) {
        return this.phoneNumberPrefixes[i];
    }

    @Override
    public String getDescription(int i) {
        return this.descriptions[i];
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException {
        this.numOfEntries = objectInput.readInt();
        if (this.phoneNumberPrefixes == null || this.phoneNumberPrefixes.length < this.numOfEntries) {
            this.phoneNumberPrefixes = new int[this.numOfEntries];
        }
        if (this.descriptions == null || this.descriptions.length < this.numOfEntries) {
            this.descriptions = new String[this.numOfEntries];
        }
        for (int i = 0; i < this.numOfEntries; i++) {
            this.phoneNumberPrefixes[i] = objectInput.readInt();
            this.descriptions[i] = objectInput.readUTF();
        }
        int i2 = objectInput.readInt();
        this.possibleLengths.clear();
        for (int i3 = 0; i3 < i2; i3++) {
            this.possibleLengths.add(Integer.valueOf(objectInput.readInt()));
        }
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeInt(this.numOfEntries);
        for (int i = 0; i < this.numOfEntries; i++) {
            objectOutput.writeInt(this.phoneNumberPrefixes[i]);
            objectOutput.writeUTF(this.descriptions[i]);
        }
        objectOutput.writeInt(this.possibleLengths.size());
        Iterator<Integer> it = this.possibleLengths.iterator();
        while (it.hasNext()) {
            objectOutput.writeInt(it.next().intValue());
        }
    }
}

package com.google.i18n.phonenumbers.geocoding;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

class DefaultMapStorage extends AreaCodeMapStorageStrategy {
    private String[] descriptions;
    private int[] phoneNumberPrefixes;

    @Override
    public int getPrefix(int index) {
        return this.phoneNumberPrefixes[index];
    }

    @Override
    public String getDescription(int index) {
        return this.descriptions[index];
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
        int sizeOfLengths = objectInput.readInt();
        this.possibleLengths.clear();
        for (int i2 = 0; i2 < sizeOfLengths; i2++) {
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
        int sizeOfLengths = this.possibleLengths.size();
        objectOutput.writeInt(sizeOfLengths);
        for (Integer length : this.possibleLengths) {
            objectOutput.writeInt(length.intValue());
        }
    }
}

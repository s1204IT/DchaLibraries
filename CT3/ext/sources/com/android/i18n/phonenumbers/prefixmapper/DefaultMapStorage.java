package com.android.i18n.phonenumbers.prefixmapper;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.SortedMap;

class DefaultMapStorage extends PhonePrefixMapStorageStrategy {
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
    public void readFromSortedMap(SortedMap<Integer, String> sortedPhonePrefixMap) {
        this.numOfEntries = sortedPhonePrefixMap.size();
        this.phoneNumberPrefixes = new int[this.numOfEntries];
        this.descriptions = new String[this.numOfEntries];
        int index = 0;
        Iterator prefix$iterator = sortedPhonePrefixMap.keySet().iterator();
        while (prefix$iterator.hasNext()) {
            int prefix = ((Integer) prefix$iterator.next()).intValue();
            this.phoneNumberPrefixes[index] = prefix;
            this.possibleLengths.add(Integer.valueOf(((int) Math.log10(prefix)) + 1));
            index++;
        }
        sortedPhonePrefixMap.values().toArray(this.descriptions);
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

package com.google.i18n.phonenumbers.geocoding;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;

final class FlyweightMapStorage extends AreaCodeMapStorageStrategy {
    private int descIndexSizeInBytes;
    private ByteBuffer descriptionIndexes;
    private String[] descriptionPool;
    private ByteBuffer phoneNumberPrefixes;
    private int prefixSizeInBytes;

    FlyweightMapStorage() {
    }

    @Override
    public int getPrefix(int index) {
        return readWordFromBuffer(this.phoneNumberPrefixes, this.prefixSizeInBytes, index);
    }

    @Override
    public String getDescription(int index) {
        int indexInDescriptionPool = readWordFromBuffer(this.descriptionIndexes, this.descIndexSizeInBytes, index);
        return this.descriptionPool[indexInDescriptionPool];
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException {
        this.prefixSizeInBytes = objectInput.readInt();
        this.descIndexSizeInBytes = objectInput.readInt();
        int sizeOfLengths = objectInput.readInt();
        this.possibleLengths.clear();
        for (int i = 0; i < sizeOfLengths; i++) {
            this.possibleLengths.add(Integer.valueOf(objectInput.readInt()));
        }
        int descriptionPoolSize = objectInput.readInt();
        if (this.descriptionPool == null || this.descriptionPool.length < descriptionPoolSize) {
            this.descriptionPool = new String[descriptionPoolSize];
        }
        for (int i2 = 0; i2 < descriptionPoolSize; i2++) {
            String description = objectInput.readUTF();
            this.descriptionPool[i2] = description;
        }
        readEntries(objectInput);
    }

    private void readEntries(ObjectInput objectInput) throws IOException {
        this.numOfEntries = objectInput.readInt();
        if (this.phoneNumberPrefixes == null || this.phoneNumberPrefixes.capacity() < this.numOfEntries) {
            this.phoneNumberPrefixes = ByteBuffer.allocate(this.numOfEntries * this.prefixSizeInBytes);
        }
        if (this.descriptionIndexes == null || this.descriptionIndexes.capacity() < this.numOfEntries) {
            this.descriptionIndexes = ByteBuffer.allocate(this.numOfEntries * this.descIndexSizeInBytes);
        }
        for (int i = 0; i < this.numOfEntries; i++) {
            readExternalWord(objectInput, this.prefixSizeInBytes, this.phoneNumberPrefixes, i);
            readExternalWord(objectInput, this.descIndexSizeInBytes, this.descriptionIndexes, i);
        }
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeInt(this.prefixSizeInBytes);
        objectOutput.writeInt(this.descIndexSizeInBytes);
        int sizeOfLengths = this.possibleLengths.size();
        objectOutput.writeInt(sizeOfLengths);
        for (Integer length : this.possibleLengths) {
            objectOutput.writeInt(length.intValue());
        }
        objectOutput.writeInt(this.descriptionPool.length);
        String[] arr$ = this.descriptionPool;
        for (String description : arr$) {
            objectOutput.writeUTF(description);
        }
        objectOutput.writeInt(this.numOfEntries);
        for (int i = 0; i < this.numOfEntries; i++) {
            writeExternalWord(objectOutput, this.prefixSizeInBytes, this.phoneNumberPrefixes, i);
            writeExternalWord(objectOutput, this.descIndexSizeInBytes, this.descriptionIndexes, i);
        }
    }

    private static void readExternalWord(ObjectInput objectInput, int wordSize, ByteBuffer outputBuffer, int index) throws IOException {
        int wordIndex = index * wordSize;
        if (wordSize == 2) {
            outputBuffer.putShort(wordIndex, objectInput.readShort());
        } else {
            outputBuffer.putInt(wordIndex, objectInput.readInt());
        }
    }

    private static void writeExternalWord(ObjectOutput objectOutput, int wordSize, ByteBuffer inputBuffer, int index) throws IOException {
        int wordIndex = index * wordSize;
        if (wordSize == 2) {
            objectOutput.writeShort(inputBuffer.getShort(wordIndex));
        } else {
            objectOutput.writeInt(inputBuffer.getInt(wordIndex));
        }
    }

    private static int readWordFromBuffer(ByteBuffer buffer, int wordSize, int index) {
        int wordIndex = index * wordSize;
        return wordSize == 2 ? buffer.getShort(wordIndex) : buffer.getInt(wordIndex);
    }
}

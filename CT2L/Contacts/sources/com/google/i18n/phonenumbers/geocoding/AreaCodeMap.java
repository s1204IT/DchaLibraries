package com.google.i18n.phonenumbers.geocoding;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.SortedSet;
import java.util.logging.Logger;

public class AreaCodeMap implements Externalizable {
    private static final Logger LOGGER = Logger.getLogger(AreaCodeMap.class.getName());
    private AreaCodeMapStorageStrategy areaCodeMapStorage;
    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException {
        boolean useFlyweightMapStorage = objectInput.readBoolean();
        if (useFlyweightMapStorage) {
            this.areaCodeMapStorage = new FlyweightMapStorage();
        } else {
            this.areaCodeMapStorage = new DefaultMapStorage();
        }
        this.areaCodeMapStorage.readExternal(objectInput);
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeBoolean(this.areaCodeMapStorage instanceof FlyweightMapStorage);
        this.areaCodeMapStorage.writeExternal(objectOutput);
    }

    String lookup(Phonenumber.PhoneNumber number) {
        int numOfEntries = this.areaCodeMapStorage.getNumOfEntries();
        if (numOfEntries == 0) {
            return null;
        }
        long phonePrefix = Long.parseLong(number.getCountryCode() + this.phoneUtil.getNationalSignificantNumber(number));
        int currentIndex = numOfEntries - 1;
        SortedSet<Integer> currentSetOfLengths = this.areaCodeMapStorage.getPossibleLengths();
        while (currentSetOfLengths.size() > 0) {
            Integer possibleLength = currentSetOfLengths.last();
            String phonePrefixStr = String.valueOf(phonePrefix);
            if (phonePrefixStr.length() > possibleLength.intValue()) {
                phonePrefix = Long.parseLong(phonePrefixStr.substring(0, possibleLength.intValue()));
            }
            currentIndex = binarySearch(0, currentIndex, phonePrefix);
            if (currentIndex < 0) {
                return null;
            }
            int currentPrefix = this.areaCodeMapStorage.getPrefix(currentIndex);
            if (phonePrefix == currentPrefix) {
                return this.areaCodeMapStorage.getDescription(currentIndex);
            }
            currentSetOfLengths = currentSetOfLengths.headSet(possibleLength);
        }
        return null;
    }

    private int binarySearch(int start, int end, long value) {
        int current = 0;
        while (start <= end) {
            current = (start + end) >>> 1;
            int currentValue = this.areaCodeMapStorage.getPrefix(current);
            if (currentValue == value) {
                return current;
            }
            if (currentValue > value) {
                current--;
                end = current;
            } else {
                start = current + 1;
            }
        }
        return current;
    }

    public String toString() {
        return this.areaCodeMapStorage.toString();
    }
}

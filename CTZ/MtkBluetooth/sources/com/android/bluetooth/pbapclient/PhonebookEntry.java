package com.android.bluetooth.pbapclient;

import com.android.vcard.VCardEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PhonebookEntry {
    public String id;
    public Name name = new Name();
    public List<Phone> phones = new ArrayList();

    public static class Name {
        public String family;
        public String given;
        public String middle;
        public String prefix;
        public String suffix;

        public boolean equals(Object obj) {
            if (!(obj instanceof Name)) {
                return false;
            }
            Name name = (Name) obj;
            if (!Objects.equals(this.family, name.family) && (this.family == null || !this.family.equals(name.family))) {
                return false;
            }
            if (!Objects.equals(this.given, name.given) && (this.given == null || !this.given.equals(name.given))) {
                return false;
            }
            if (!Objects.equals(this.middle, name.middle) && (this.middle == null || !this.middle.equals(name.middle))) {
                return false;
            }
            if (Objects.equals(this.prefix, name.prefix) || (this.prefix != null && this.prefix.equals(name.prefix))) {
                return Objects.equals(this.suffix, name.suffix) || (this.suffix != null && this.suffix.equals(name.suffix));
            }
            return false;
        }

        public int hashCode() {
            return (23 * (((((((this.family == null ? 0 : this.family.hashCode()) * 23 * 23) + (this.given == null ? 0 : this.given.hashCode())) * 23) + (this.middle == null ? 0 : this.middle.hashCode())) * 23) + (this.prefix == null ? 0 : this.prefix.hashCode()))) + (this.suffix != null ? this.suffix.hashCode() : 0);
        }

        public String toString() {
            return "Name: { family: " + this.family + " given: " + this.given + " middle: " + this.middle + " prefix: " + this.prefix + " suffix: " + this.suffix + " }";
        }
    }

    public static class Phone {
        public String number;
        public int type;

        public boolean equals(Object obj) {
            if (!(obj instanceof Phone)) {
                return false;
            }
            Phone phone = (Phone) obj;
            return (Objects.equals(this.number, phone.number) || (this.number != null && this.number.equals(phone.number))) && this.type == phone.type;
        }

        public int hashCode() {
            return (23 * this.type) + this.number.hashCode();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(" Phone: { number: ");
            sb.append(this.number);
            sb.append(" type: " + this.type);
            sb.append(" }");
            return sb.toString();
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof PhonebookEntry) {
            return equals((PhonebookEntry) obj);
        }
        return false;
    }

    public PhonebookEntry() {
    }

    public PhonebookEntry(VCardEntry vCardEntry) {
        VCardEntry.NameData nameData = vCardEntry.getNameData();
        this.name.family = nameData.getFamily();
        this.name.given = nameData.getGiven();
        this.name.middle = nameData.getMiddle();
        this.name.prefix = nameData.getPrefix();
        this.name.suffix = nameData.getSuffix();
        List<VCardEntry.PhoneData> phoneList = vCardEntry.getPhoneList();
        if (phoneList == null || phoneList.isEmpty()) {
            return;
        }
        for (VCardEntry.PhoneData phoneData : phoneList) {
            Phone phone = new Phone();
            phone.type = phoneData.getType();
            phone.number = phoneData.getNumber();
            this.phones.add(phone);
        }
    }

    private boolean equals(PhonebookEntry phonebookEntry) {
        return this.name.equals(phonebookEntry.name) && this.phones.equals(phonebookEntry.phones);
    }

    public int hashCode() {
        return this.name.hashCode() + (23 * this.phones.hashCode());
    }

    public String toString() {
        return "PhonebookEntry { id: " + this.id + " " + this.name.toString() + this.phones.toString() + " }";
    }
}

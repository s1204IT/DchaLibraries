package com.android.contacts.quickcontact;

import com.android.contacts.model.Contact;

public class DirectoryContactUtil {
    public static boolean isDirectoryContact(Contact contact) {
        return (contact == null || !contact.isDirectoryEntry() || contact.getDirectoryExportSupport() == 0) ? false : true;
    }
}

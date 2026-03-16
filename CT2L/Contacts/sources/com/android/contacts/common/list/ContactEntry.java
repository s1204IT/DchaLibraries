package com.android.contacts.common.list;

import android.graphics.drawable.Drawable;
import android.net.Uri;

public class ContactEntry {
    public static final ContactEntry BLANK_ENTRY = new ContactEntry();
    public String lookupKey;
    public Uri lookupUri;
    public String name;
    public String phoneLabel;
    public String phoneNumber;
    public Uri photoUri;
    public Drawable presenceIcon;
    public String status;
    public int pinned = 0;
    public boolean isFavorite = false;
    public boolean isDefaultNumber = false;
}

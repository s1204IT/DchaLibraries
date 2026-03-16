package com.android.contacts.util;

import android.content.Intent;
import android.net.Uri;

public class StructuredPostalUtils {
    public static Intent getViewPostalAddressIntent(String postalAddress) {
        return new Intent("android.intent.action.VIEW", getPostalAddressUri(postalAddress));
    }

    public static Uri getPostalAddressUri(String postalAddress) {
        return Uri.parse("geo:0,0?q=" + Uri.encode(postalAddress));
    }

    public static Intent getViewPostalAddressDirectionsIntent(String postalAddress) {
        return new Intent("android.intent.action.VIEW", getPostalAddressDirectionsUri(postalAddress));
    }

    public static Uri getPostalAddressDirectionsUri(String postalAddress) {
        return Uri.parse("https://maps.google.com/maps?daddr=" + Uri.encode(postalAddress));
    }
}

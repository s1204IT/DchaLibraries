package com.android.providers.contacts;

import android.content.UriMatcher;
import android.net.Uri;
import android.provider.ContactsContract;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ProfileAwareUriMatcher extends UriMatcher {
    private static final Pattern PATH_SPLIT_PATTERN = Pattern.compile("/");
    private static final List<Integer> PROFILE_URIS = Lists.newArrayList();
    private static final Map<Integer, Integer> PROFILE_URI_ID_MAP = Maps.newHashMap();
    private static final Map<Integer, Integer> PROFILE_URI_LOOKUP_KEY_MAP = Maps.newHashMap();

    public ProfileAwareUriMatcher(int i) {
        super(i);
    }

    @Override
    public void addURI(String str, String str2, int i) {
        super.addURI(str, str2, i);
        if (str2 != null) {
            if (str2.length() > 0 && str2.charAt(0) == '/') {
                str2 = str2.substring(1);
            }
            String[] strArrSplit = PATH_SPLIT_PATTERN.split(str2);
            if (strArrSplit != null) {
                boolean z = false;
                for (int i2 = 0; i2 < strArrSplit.length; i2++) {
                    String str3 = strArrSplit[i2];
                    if (str3.equals("profile")) {
                        PROFILE_URIS.add(Integer.valueOf(i));
                        return;
                    }
                    if (str3.equals("lookup") || str3.equals("as_vcard")) {
                        z = true;
                    } else {
                        if (str3.equals("#")) {
                            PROFILE_URI_ID_MAP.put(Integer.valueOf(i), Integer.valueOf(i2));
                        } else if (str3.equals("*") && z) {
                            PROFILE_URI_LOOKUP_KEY_MAP.put(Integer.valueOf(i), Integer.valueOf(i2));
                        }
                        z = false;
                    }
                }
            }
        }
    }

    public boolean mapsToProfile(Uri uri) {
        int iIntValue;
        int iMatch = match(uri);
        if (PROFILE_URIS.contains(Integer.valueOf(iMatch))) {
            return true;
        }
        if (PROFILE_URI_ID_MAP.containsKey(Integer.valueOf(iMatch))) {
            if (ContactsContract.isProfileId(Long.parseLong(uri.getPathSegments().get(PROFILE_URI_ID_MAP.get(Integer.valueOf(iMatch)).intValue())))) {
                return true;
            }
        } else if (PROFILE_URI_LOOKUP_KEY_MAP.containsKey(Integer.valueOf(iMatch)) && (iIntValue = PROFILE_URI_LOOKUP_KEY_MAP.get(Integer.valueOf(iMatch)).intValue()) < uri.getPathSegments().size() && "profile".equals(uri.getPathSegments().get(iIntValue))) {
            return true;
        }
        return false;
    }
}

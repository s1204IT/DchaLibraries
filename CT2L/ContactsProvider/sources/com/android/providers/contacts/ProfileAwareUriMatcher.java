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

    public ProfileAwareUriMatcher(int code) {
        super(code);
    }

    @Override
    public void addURI(String authority, String path, int code) {
        super.addURI(authority, path, code);
        if (path != null) {
            String newPath = path;
            if (path.length() > 0 && path.charAt(0) == '/') {
                newPath = path.substring(1);
            }
            String[] tokens = PATH_SPLIT_PATTERN.split(newPath);
            if (tokens != null) {
                boolean afterLookup = false;
                for (int i = 0; i < tokens.length; i++) {
                    String token = tokens[i];
                    if (token.equals("profile")) {
                        PROFILE_URIS.add(Integer.valueOf(code));
                        return;
                    }
                    if (token.equals("lookup") || token.equals("as_vcard")) {
                        afterLookup = true;
                    } else {
                        if (token.equals("#")) {
                            PROFILE_URI_ID_MAP.put(Integer.valueOf(code), Integer.valueOf(i));
                        } else if (token.equals("*") && afterLookup) {
                            PROFILE_URI_LOOKUP_KEY_MAP.put(Integer.valueOf(code), Integer.valueOf(i));
                        }
                        afterLookup = false;
                    }
                }
            }
        }
    }

    public boolean mapsToProfile(Uri uri) {
        int match = match(uri);
        if (PROFILE_URIS.contains(Integer.valueOf(match))) {
            return true;
        }
        if (PROFILE_URI_ID_MAP.containsKey(Integer.valueOf(match))) {
            int idSegment = PROFILE_URI_ID_MAP.get(Integer.valueOf(match)).intValue();
            long id = Long.parseLong(uri.getPathSegments().get(idSegment));
            if (ContactsContract.isProfileId(id)) {
                return true;
            }
        } else if (PROFILE_URI_LOOKUP_KEY_MAP.containsKey(Integer.valueOf(match))) {
            int lookupKeySegment = PROFILE_URI_LOOKUP_KEY_MAP.get(Integer.valueOf(match)).intValue();
            String lookupKey = uri.getPathSegments().get(lookupKeySegment);
            if ("profile".equals(lookupKey)) {
                return true;
            }
        }
        return false;
    }
}

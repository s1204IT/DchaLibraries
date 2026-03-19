package com.android.providers.contacts;

import com.android.providers.contacts.util.UriType;

enum VoicemailUriType implements UriType {
    NO_MATCH(null),
    VOICEMAILS("voicemail"),
    VOICEMAILS_ID("voicemail/#"),
    STATUS("status"),
    STATUS_ID("status/#");

    private final String path;

    VoicemailUriType(String str) {
        this.path = str;
    }

    @Override
    public String path() {
        return this.path;
    }
}

package com.android.statementservice.retriever;

public final class WebContent {
    private final String mContent;
    private final Long mExpireTimeMillis;

    public WebContent(String str, Long l) {
        this.mContent = str;
        this.mExpireTimeMillis = l;
    }

    public Long getExpireTimeMillis() {
        return this.mExpireTimeMillis;
    }

    public String getContent() {
        return this.mContent;
    }
}

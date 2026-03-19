package com.android.statementservice.retriever;

final class WebAssetMatcher extends AbstractAssetMatcher {
    private final WebAsset mQuery;

    public WebAssetMatcher(WebAsset webAsset) {
        this.mQuery = webAsset;
    }

    @Override
    public boolean matches(AbstractAsset abstractAsset) {
        if (abstractAsset instanceof WebAsset) {
            return ((WebAsset) abstractAsset).toJson().equals(this.mQuery.toJson());
        }
        return false;
    }
}

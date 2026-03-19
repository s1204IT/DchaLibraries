package com.android.statementservice.retriever;

import org.json.JSONException;

public abstract class AbstractAssetMatcher {
    public abstract boolean matches(AbstractAsset abstractAsset);

    public static AbstractAssetMatcher createMatcher(String str) throws JSONException, AssociationServiceException {
        return AssetMatcherFactory.create(str);
    }
}

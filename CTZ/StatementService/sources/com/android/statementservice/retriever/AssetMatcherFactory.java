package com.android.statementservice.retriever;

import org.json.JSONException;
import org.json.JSONObject;

final class AssetMatcherFactory {
    public static AbstractAssetMatcher create(String str) throws JSONException, AssociationServiceException {
        JSONObject jSONObject = new JSONObject(str);
        String strOptString = jSONObject.optString("namespace", null);
        if (strOptString == null) {
            throw new AssociationServiceException(String.format("Expected %s to be string.", "namespace"));
        }
        if (strOptString.equals("web")) {
            return new WebAssetMatcher(WebAsset.create(jSONObject));
        }
        if (strOptString.equals("android_app")) {
            return new AndroidAppAssetMatcher(AndroidAppAsset.create(jSONObject));
        }
        throw new AssociationServiceException(String.format("Namespace %s is not supported.", strOptString));
    }
}

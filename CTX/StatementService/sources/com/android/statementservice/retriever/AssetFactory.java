package com.android.statementservice.retriever;

import org.json.JSONObject;

final class AssetFactory {
    public static AbstractAsset create(JSONObject jSONObject) throws AssociationServiceException {
        String strOptString = jSONObject.optString("namespace", null);
        if (strOptString == null) {
            throw new AssociationServiceException(String.format("Expected %s to be string.", "namespace"));
        }
        if (strOptString.equals("web")) {
            return WebAsset.create(jSONObject);
        }
        if (strOptString.equals("android_app")) {
            return AndroidAppAsset.create(jSONObject);
        }
        throw new AssociationServiceException("Namespace " + strOptString + " is not supported.");
    }
}

package com.android.statementservice.retriever;

import android.util.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import org.json.JSONException;

public abstract class AbstractAsset {
    public abstract boolean followInsecureInclude();

    public abstract String toJson();

    public static AbstractAsset create(String str) throws AssociationServiceException {
        JsonReader jsonReader = new JsonReader(new StringReader(str));
        jsonReader.setLenient(false);
        try {
            return AssetFactory.create(JsonParser.parse(jsonReader));
        } catch (IOException | JSONException e) {
            throw new AssociationServiceException("Input is not a well formatted asset descriptor.", e);
        }
    }
}

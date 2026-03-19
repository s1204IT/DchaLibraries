package com.android.statementservice.retriever;

import android.util.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class StatementParser {
    static ParsedStatement parseStatementList(String str, AbstractAsset abstractAsset) throws JSONException, IOException {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        JsonReader jsonReader = new JsonReader(new StringReader(str));
        jsonReader.setLenient(false);
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            try {
                ParsedStatement statement = parseStatement(jsonReader, abstractAsset);
                arrayList.addAll(statement.getStatements());
                arrayList2.addAll(statement.getDelegates());
            } catch (AssociationServiceException e) {
            }
        }
        jsonReader.endArray();
        return new ParsedStatement(arrayList, arrayList2);
    }

    static ParsedStatement parseStatement(String str, AbstractAsset abstractAsset) throws JSONException, AssociationServiceException, IOException {
        JsonReader jsonReader = new JsonReader(new StringReader(str));
        jsonReader.setLenient(false);
        return parseStatement(jsonReader, abstractAsset);
    }

    static ParsedStatement parseStatement(JsonReader jsonReader, AbstractAsset abstractAsset) throws JSONException, AssociationServiceException, IOException {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        JSONObject jSONObject = JsonParser.parse(jsonReader);
        if (jSONObject.optString("include", null) != null) {
            arrayList2.add(jSONObject.optString("include"));
        } else {
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("target");
            if (jSONObjectOptJSONObject == null) {
                throw new AssociationServiceException(String.format("Expected %s to be string.", "target"));
            }
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("relation");
            if (jSONArrayOptJSONArray == null) {
                throw new AssociationServiceException(String.format("Expected %s to be array.", "relation"));
            }
            AbstractAsset abstractAssetCreate = AssetFactory.create(jSONObjectOptJSONObject);
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                arrayList.add(Statement.create(abstractAsset, abstractAssetCreate, Relation.create(jSONArrayOptJSONArray.getString(i))));
            }
        }
        return new ParsedStatement(arrayList, arrayList2);
    }
}

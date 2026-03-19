package com.android.statementservice.retriever;

import android.util.JsonReader;
import android.util.JsonToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class JsonParser {
    public static JSONObject parse(JsonReader jsonReader) throws JSONException, IOException {
        JSONObject jSONObject = new JSONObject();
        jsonReader.beginObject();
        String message = null;
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            if (jSONObject.has(strNextName)) {
                message = "Duplicate field name.";
                jsonReader.skipValue();
            } else {
                JsonToken jsonTokenPeek = jsonReader.peek();
                if (jsonTokenPeek.equals(JsonToken.BEGIN_ARRAY)) {
                    jSONObject.put(strNextName, new JSONArray((Collection) parseArray(jsonReader)));
                } else if (jsonTokenPeek.equals(JsonToken.STRING)) {
                    jSONObject.put(strNextName, jsonReader.nextString());
                } else if (jsonTokenPeek.equals(JsonToken.BEGIN_OBJECT)) {
                    try {
                        jSONObject.put(strNextName, parse(jsonReader));
                    } catch (JSONException e) {
                        message = e.getMessage();
                    }
                } else {
                    jsonReader.skipValue();
                    message = "Unsupported value type.";
                }
            }
        }
        jsonReader.endObject();
        if (message != null) {
            throw new JSONException(message);
        }
        return jSONObject;
    }

    public static List<String> parseArray(JsonReader jsonReader) throws IOException {
        ArrayList arrayList = new ArrayList();
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            arrayList.add(jsonReader.nextString());
        }
        jsonReader.endArray();
        return arrayList;
    }
}

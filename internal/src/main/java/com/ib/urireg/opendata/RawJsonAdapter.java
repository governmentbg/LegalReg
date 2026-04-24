/*
 * Copyright (c) 2025. Index - Bulgaria Ltd. All rights reserved.
 *
 */

package com.ib.urireg.opendata;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.bind.adapter.JsonbAdapter;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Този клас се използва при сериализиране на обект към JSON.
 * Ако провайдера е yssosn (дефолтен за WildFly то ако имаме клас от вида:
 * MyObject{
 *     String data
 * }
 * и се очаква в дата да слагаме готов JSON, to se o`akwa da e anotirano s @JsonRawValue.
 * Обаче са YASSON-a няма такова и ще добави още едни кавички.Ето защо правим:
 *
 * @JsonbTypeAdapter(RawJsonAdapter.class)
 *     private String data;
 */
public class RawJsonAdapter implements JsonbAdapter<String, JsonStructure> {

    @Override
    public JsonStructure adaptToJson(String obj) throws Exception {
        // Parse the string into a JSON structure
        try (JsonReader reader = Json.createReader(new StringReader(obj))) {
            return reader.read();
        }
    }

    @Override
    public String adaptFromJson(JsonStructure obj) throws Exception {
        // Convert JSON structure back into string
        StringWriter writer = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(writer)) {
            jsonWriter.write(obj);
        }
        return writer.toString();
    }
}

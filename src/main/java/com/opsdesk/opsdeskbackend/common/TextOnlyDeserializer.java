package com.opsdesk.opsdeskbackend.common;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

// Reads a body field that must be text. By default Jackson turns 123 or true
// into "123" or "true", and a record cannot tell a field sent as null from one
// left out. Here a field that is sent but is not text (null, a number, true,
// an array or an object) becomes "", so it fails the field's "Enter a ..."
// check like an empty string. A field left out stays null, which PATCH reads
// as "don't change it".
public class TextOnlyDeserializer extends StdDeserializer<String> {

    public TextOnlyDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            return parser.getString();
        }
        parser.skipChildren();
        return "";
    }

    // The field was sent as null.
    @Override
    public String getNullValue(DeserializationContext context) {
        return "";
    }

    // The field was left out.
    @Override
    public Object getAbsentValue(DeserializationContext context) {
        return null;
    }
}

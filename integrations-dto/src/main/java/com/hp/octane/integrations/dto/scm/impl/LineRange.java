package com.hp.octane.integrations.dto.scm.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

@JsonSerialize(using = LineRange.RangeSerializer.class)
@JsonDeserialize(using = LineRange.RangeDeserializer.class)
public class LineRange {
    private int start;
    private int end;


    public LineRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public LineRange(Long singleNum) {
        this.start = singleNum.intValue();
        this.end = singleNum.intValue();
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }


    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }


    public static class RangeDeserializer extends StdDeserializer<LineRange> {

        public RangeDeserializer() {
            this(null);
        }

        public RangeDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public LineRange deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {

            ArrayNode node = jp.getCodec().readTree(jp);
            int start = node.get(0).intValue();
            int end = node.get(1).intValue();
            return new LineRange(start,end);
        }
    }

    public static class  RangeSerializer extends StdSerializer<LineRange> {

        public RangeSerializer() {
            this(null);
        }

        public RangeSerializer(Class<LineRange> range) {
            super(range);
        }

        @Override
        public void serialize(
                LineRange lineRange, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {

            jgen.writeStartArray(2);
            jgen.writeNumber(lineRange.getStart());
            jgen.writeNumber(lineRange.getEnd());
            jgen.writeEndArray();
        }
    }
}


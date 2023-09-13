/**
 * Copyright 2017-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.io.Serializable;

@JsonSerialize(using = LineRange.RangeSerializer.class)
@JsonDeserialize(using = LineRange.RangeDeserializer.class)
public class LineRange implements Serializable {
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


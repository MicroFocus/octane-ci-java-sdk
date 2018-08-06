/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.integrations.dto.general;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by gullery on 03/01/2016.
 */

public class EntityDTOTests {
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    @Test
    public void testEntity() {

        Entity entity = dtoFactory.newDTO(Entity.class);
        entity.setField("id", "1");
        entity.setField("name", "fff");


        String json = dtoFactory.dtoToJson(entity);

        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() > 8);
    }

    @Test
    public void testEntityList() {

        Entity entity1 = dtoFactory.newDTO(Entity.class);
        entity1.setField("id", "1");
        entity1.setField("name", "fff");

        Entity entity2 = dtoFactory.newDTO(Entity.class);
        entity2.setField("id", "2");
        entity2.setField("name", "fff");

        EntityList list = dtoFactory.newDTO(EntityList.class);
        list.addEntity(entity1);
        list.addEntity(entity2);

        String json = dtoFactory.dtoToJson(list);
        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() > 10);


        EntityList serializedList = dtoFactory.dtoFromJson(json, EntityList.class);
        Assert.assertEquals(2, serializedList.getData().size());
    }

    @Test
    public void testResponseEntityList() {

        Entity entity1 = dtoFactory.newDTO(Entity.class);
        entity1.setType("type1").setId("1").setName("name1");

        Entity entity2 = dtoFactory.newDTO(Entity.class);
        entity2.setType("type2").setId("2").setName("name2");

        ResponseEntityList list = dtoFactory.newDTO(ResponseEntityList.class);
        list.addEntity(entity1);
        list.addEntity(entity2);
        list.setExceedsTotalCount(true);
        list.setTotalCount(2);

        String json = dtoFactory.dtoToJson(list);
        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() > 10);


        EntityList serializedList = dtoFactory.dtoFromJson(json, ResponseEntityList.class);
        Assert.assertEquals(2, serializedList.getData().size());
    }


    @Test
    public void testParseResponseEntitiyList() {
        String json = "{\"total_count\":1,\"data\":[{\"type\":\"defect\",\"workspace_id\":1002,\"logical_name\":\"439wn0yylz2j4bgp0r72kegzp\",\"name\":\"def1\",\"id\":\"3001\"}],\"exceeds_total_count\":false}";
        ResponseEntityList serializedList = dtoFactory.dtoFromJson(json, ResponseEntityList.class);
        Assert.assertEquals(1, serializedList.getData().size());
    }

    @Test
    public void testParseOctaneException() {
        String json = "{\"error_code\":\"platform.web_application\",\"correlation_id\":\"o5jp1yvjo54lxbjmo7dxz12v6\",\"description\":\"HTTP 404 Not Found\",\"description_translated\":\"HTTP 404 Not Found\",\"properties\":null,\"stack_trace\":\"javax.ws.rs.NotFoundException: HTTP 404\",\"business_error\":false}\n";
        OctaneRestExceptionData octaneRestExceptionData = dtoFactory.dtoFromJson(json, OctaneRestExceptionData.class);
        Assert.assertEquals("platform.web_application", octaneRestExceptionData.getErrorCode());
    }
    @Test
    public void testParseOctaneBulkException() {
        String json = "{\"total_count\":0,\"data\":[],\"exceeds_total_count\":false,\"errors\":[{\"error_code\":\"platform.unknown_field\",\"correlation_id\":\"o5jp1y5576mo0tdyd60g7n2v6\",\"description\":\"The entity type 'defect' does not have a field/s by name/s 'sss'\",\"description_translated\":\"The entity type 'defect' does not have a field/s by name/s 'sss'\",\"properties\":{\"entity_type\":\"defect\",\"field_name\":\"sss\"},\"stack_trace\":\"com.hp.mqm.bl.platform.exception.NonExistingFieldException\",\"business_error\":true}]}";
        OctaneBulkExceptionData octaneException = dtoFactory.dtoFromJson(json, OctaneBulkExceptionData.class);
        Assert.assertEquals("platform.unknown_field", octaneException.getErrors().get(0).getErrorCode());
    }


}

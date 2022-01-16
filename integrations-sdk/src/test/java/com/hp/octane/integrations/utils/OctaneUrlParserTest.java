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
 */

package com.hp.octane.integrations.utils;

import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OctaneUrlParserTest {

//https://center.almoctane.com/ui/?p=1001%2F1002#/team-backlog/stories
//	http://localhost:8080/ui/?admin&p=1001/1002#/settings/workspace/devops/build-servers
//    https://myd-hvm01967.swinfra.net:8447/web-context/ui/?admin&p=1001/500#/settings/shared-space/applications

    @Test
    public void test1() {
        OctaneUrlParser parser = OctaneUrlParser.parse("https://center.almoctane.com/ui/?p=1001%2F1002#/team-backlog/stories");
        Assert.assertEquals("https://center.almoctane.com", parser.getLocation());
        Assert.assertEquals("1001", parser.getSharedSpace());
    }

    @Test
    public void test2() {
        OctaneUrlParser parser = OctaneUrlParser.parse("http://localhost:8080/ui/?admin&p=1001/1002#/settings/workspace/devops/build-servers");
        Assert.assertEquals("http://localhost:8080", parser.getLocation());
        Assert.assertEquals("1001", parser.getSharedSpace());
    }

    @Test
    public void testWithContext1() {
        OctaneUrlParser parser = OctaneUrlParser.parse("https://myd-hvm01967.swinfra.net:8447/web-context/ui/?admin&p=1002/500#/settings/shared-space/applications");
        Assert.assertEquals("https://myd-hvm01967.swinfra.net:8447/web-context", parser.getLocation());
        Assert.assertEquals("1002", parser.getSharedSpace());
    }


}

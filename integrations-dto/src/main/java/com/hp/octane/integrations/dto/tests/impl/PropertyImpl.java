/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
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

package com.hp.octane.integrations.dto.tests.impl;

import com.hp.octane.integrations.dto.tests.Property;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by lev on 31/05/2016.
 */
@XmlRootElement(name = "property")
@XmlAccessorType(XmlAccessType.NONE)
public class PropertyImpl implements Property {

    @XmlAttribute(name = "name")
    private String propertyName;

    @XmlAttribute(name = "value")
    private String propertyValue;

    public String getPropertyName() {
        return propertyName;
    }

    public Property setPropertyName(String name) {
        propertyName = name;
        return this;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public Property setPropertyValue(String value) {
        propertyValue = value;
        return this;
    }
}

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

package com.hp.octane.integrations.dto;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * API definition of an internal DTO factories
 */

public abstract class DTOInternalProviderBase {
	protected final Map<Class<? extends DTOBase>, Class> dtoPairs = new LinkedHashMap<>();
	protected final List<Class<? extends DTOBase>> xmlAbles = new LinkedList<>();

	protected DTOInternalProviderBase(DTOFactory.DTOConfiguration configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException("configuration object MUST NOT be null");
		}
	}

	protected abstract <T extends DTOBase> T instantiateDTO(Class<T> targetType) throws InstantiationException, IllegalAccessException;

	<T extends DTOBase> String toXml(T dto) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(this.getXMLAbles());
		Marshaller marshaller = jaxbContext.createMarshaller();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		marshaller.marshal(dto, baos);
		return baos.toString();
	}

	<T extends DTOBase> InputStream toXmlStream(T dto) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(this.getXMLAbles());
		Marshaller marshaller = jaxbContext.createMarshaller();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		marshaller.marshal(dto, baos);
		return new ByteArrayInputStream(baos.toByteArray());
	}

	<T extends DTOBase> T fromXml(String xml) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(getXMLAbles());
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		return (T) unmarshaller.unmarshal(new StringReader(xml));
	}

	<T extends DTOBase> T fromXmlFile(File xml) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(getXMLAbles());
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		return (T) unmarshaller.unmarshal(xml);
	}

	Map<Class<? extends DTOBase>, Class> getDTOPairs() {
		return dtoPairs;
	}

	private Class[] getXMLAbles() {
		return xmlAbles.toArray(new Class[0]);
	}
}

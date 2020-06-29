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

package com.hp.octane.integrations.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.hp.octane.integrations.dto.causes.impl.DTOCausesProvider;
import com.hp.octane.integrations.dto.configuration.impl.DTOConfigsProvider;
import com.hp.octane.integrations.dto.connectivity.impl.DTOConnectivityProvider;
import com.hp.octane.integrations.dto.coverage.impl.DTOCoverageProvider;
import com.hp.octane.integrations.dto.entities.impl.DTOEntityProvider;
import com.hp.octane.integrations.dto.events.impl.DTOEventsProvider;
import com.hp.octane.integrations.dto.executor.impl.DTOExecutorsProvider;
import com.hp.octane.integrations.dto.general.impl.DTOGeneralProvider;
import com.hp.octane.integrations.dto.parameters.impl.DTOParametersProvider;
import com.hp.octane.integrations.dto.securityscans.impl.DTOSecurityContextProvider;
import com.hp.octane.integrations.dto.pipelines.impl.DTOPipelinesProvider;
import com.hp.octane.integrations.dto.scm.impl.DTOSCMProvider;
import com.hp.octane.integrations.dto.tests.impl.DTOJUnitTestsProvider;
import com.hp.octane.integrations.dto.tests.impl.DTOTestsProvider;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * DTO Factory is a single entry point of DTOs management
 */

public final class DTOFactory {
	private final DTOConfiguration configuration;

	private DTOFactory() {
		configuration = new DTOConfiguration();
	}

	public static DTOFactory getInstance() {
		return INSTANCE_HOLDER.instance;
	}

	public <T extends DTOBase> T newDTO(Class<T> targetType) {
		if (targetType == null) {
			throw new IllegalArgumentException("target type MUST NOT be null");
		}
		if (!targetType.isInterface()) {
			throw new IllegalArgumentException("target type MUST be an Interface");
		}
		if (!configuration.registry.containsKey(targetType)) {
			throw new IllegalArgumentException("requested type " + targetType + " is not supported");
		}

		try {
			return configuration.registry.get(targetType).instantiateDTO(targetType);
		} catch (InstantiationException ie) {
			throw new RuntimeException("failed to instantiate " + targetType, ie);
		} catch (IllegalAccessException iae) {
			throw new RuntimeException("access denied to " + targetType, iae);
		}
	}

	public <T extends DTOBase> InputStream dtoToJsonStream(T dto) {
		if (dto == null) {
			throw new IllegalArgumentException("dto MUST NOT be null");
		}

		try {
			return new ByteArrayInputStream(configuration.objectMapper.writeValueAsBytes(dto));
		} catch (JsonProcessingException jpe) {
			throw new RuntimeException("failed to serialize " + dto + " to JSON", jpe);
		}
	}

	public <T extends DTOBase> String dtoToJson(T dto) {
		if (dto == null) {
			throw new IllegalArgumentException("dto MUST NOT be null");
		}

		try {
			return configuration.objectMapper.writeValueAsString(dto);
		} catch (JsonProcessingException jpe) {
			throw new RuntimeException("failed to serialize " + dto + " to JSON", jpe);
		}
	}

	public <T extends DTOBase> InputStream dtoCollectionToJsonStream(List<T> dto) {
		if (dto == null) {
			throw new IllegalArgumentException("dto MUST NOT be null");
		}

		try {
			return new ByteArrayInputStream(configuration.objectMapper.writeValueAsBytes(dto));
		} catch (JsonProcessingException jpe) {
			throw new RuntimeException("failed to serialize " + dto + " to JSON", jpe);
		}
	}

	public <T extends DTOBase> String dtoCollectionToJson(List<T> dto) {
		if (dto == null) {
			throw new IllegalArgumentException("dto MUST NOT be null");
		}

		try {
			return configuration.objectMapper.writeValueAsString(dto);
		} catch (JsonProcessingException jpe) {
			throw new RuntimeException("failed to serialize " + dto + " to JSON", jpe);
		}
	}

	public <T extends DTOBase> T dtoFromJson(String json, Class<T> targetType) {
		if (targetType == null) {
			throw new IllegalArgumentException("target type MUST NOT be null");
		}
		if (!targetType.isInterface()) {
			throw new IllegalArgumentException("target type MUST be an Interface");
		}

		try {
			return configuration.objectMapper.readValue(json, targetType);
		} catch (IOException ioe) {
			throw new RuntimeException("failed to deserialize " + json + " into " + targetType, ioe);
		}
	}

	public <T extends DTOBase> T[] dtoCollectionFromJson(String json, Class<T[]> targetType) {
		if (targetType == null) {
			throw new IllegalArgumentException("target type MUST NOT be null");
		}
		if (!targetType.isArray()) {
			throw new IllegalArgumentException("target type MUST be an Array");
		}

		try {
			return configuration.objectMapper.readValue(json, targetType);
		} catch (IOException ioe) {
			throw new RuntimeException("failed to deserialize " + json + " into " + targetType, ioe);
		}
	}

	public <T extends DTOBase> String dtoToXml(T dto) {
		if (dto == null) {
			throw new IllegalArgumentException("dto MUST NOT be null");
		}

		DTOInternalProviderBase internalFactory = null;
		try {
			for (Class<? extends DTOBase> supported : configuration.registry.keySet()) {
				if (supported.isAssignableFrom(dto.getClass())) {
					internalFactory = configuration.registry.get(supported);
					break;
				}
			}
			if (internalFactory != null) {
				return internalFactory.toXml(dto);
			} else {
				throw new RuntimeException(dto.getClass() + " is not supported in this flow");
			}
		} catch (JAXBException | UnsupportedEncodingException e) {
			throw new RuntimeException("failed to serialize " + dto + " to XML", e);
		}
	}

	public <T extends DTOBase> InputStream dtoToXmlStream(T dto) {
		if (dto == null) {
			throw new IllegalArgumentException("dto MUST NOT be null");
		}

		DTOInternalProviderBase internalFactory = null;
		try {
			for (Class<? extends DTOBase> supported : configuration.registry.keySet()) {
				if (supported.isAssignableFrom(dto.getClass())) {
					internalFactory = configuration.registry.get(supported);
					break;
				}
			}
			if (internalFactory != null) {
				return internalFactory.toXmlStream(dto);
			} else {
				throw new RuntimeException(dto.getClass() + " is not supported in this flow");
			}
		} catch (JAXBException jaxbe) {
			throw new RuntimeException("failed to serialize " + dto + " to XML", jaxbe);
		}
	}

	public <T extends DTOBase> T dtoFromXml(String xml, Class<T> targetType) {
		if (targetType == null) {
			throw new IllegalArgumentException("target type MUST NOT be null");
		}
		if (!targetType.isInterface()) {
			throw new IllegalArgumentException("target type MUST be an Interface");
		}

		DTOInternalProviderBase internalFactory = null;
		try {
			for (Class<? extends DTOBase> supported : configuration.registry.keySet()) {
				if (supported.equals(targetType)) {
					internalFactory = configuration.registry.get(supported);
					break;
				}
			}
			if (internalFactory != null) {
				return internalFactory.fromXml(xml);
			} else {
				throw new RuntimeException(targetType + " is not supported in this flow");
			}
		} catch (JAXBException jaxbe) {
			throw new RuntimeException("failed to deserialize " + xml + " into " + targetType, jaxbe);
		}
	}

	public <T extends DTOBase> T dtoFromXmlFile(File xml, Class<T> targetType) {
		if (targetType == null) {
			throw new IllegalArgumentException("target type MUST NOT be null");
		}
		if (!targetType.isInterface()) {
			throw new IllegalArgumentException("target type MUST be an Interface");
		}

		DTOInternalProviderBase internalFactory = null;
		try {
			for (Class<? extends DTOBase> supported : configuration.registry.keySet()) {
				if (supported.equals(targetType)) {
					internalFactory = configuration.registry.get(supported);
					break;
				}
			}
			if (internalFactory != null) {
				return internalFactory.fromXmlFile(xml);
			} else {
				throw new RuntimeException(targetType + " is not supported in this flow");
			}
		} catch (JAXBException jaxbe) {
			throw new RuntimeException("failed to deserialize " + xml.getName() + " into " + targetType, jaxbe);
		}
	}

	private static final class INSTANCE_HOLDER {
		private static final DTOFactory instance = new DTOFactory();
	}

	public static class DTOConfiguration {
		private final Map<Class<? extends DTOBase>, DTOInternalProviderBase> registry = new HashMap<>();
		private final ObjectMapper objectMapper = new ObjectMapper();
		private final List<DTOInternalProviderBase> providers = new LinkedList<>();

		private DTOConfiguration() {
			//  collect all known providers
			providers.add(new DTOCausesProvider(this));
			providers.add(new DTOConfigsProvider(this));
			providers.add(new DTOConnectivityProvider(this));
			providers.add(new DTOCoverageProvider(this));
			providers.add(new DTOEventsProvider(this));
			providers.add(new DTOGeneralProvider(this));
			providers.add(new DTOParametersProvider(this));
			providers.add(new DTOPipelinesProvider(this));
			providers.add(new DTOSCMProvider(this));
			providers.add(new DTOTestsProvider(this));
			providers.add(new DTOExecutorsProvider(this));
			providers.add(new DTOJUnitTestsProvider(this));
			providers.add(new DTOEntityProvider(this));
			providers.add(new DTOSecurityContextProvider(this));

			//  register providers' data within the Factory
			//  configure ObjectMapper with interfaces and implementations
			SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
			for (DTOInternalProviderBase dtoProvider : providers) {
				for (Map.Entry<Class<? extends DTOBase>, Class> dtoPair : dtoProvider.getDTOPairs().entrySet()) {
					registry.put(dtoPair.getKey(), dtoProvider);
					resolver.addMapping(dtoPair.getKey(), dtoPair.getValue());
				}
			}
			SimpleModule module = new SimpleModule();
			module.setAbstractTypes(resolver);
			objectMapper.registerModule(module);
		}
	}
}
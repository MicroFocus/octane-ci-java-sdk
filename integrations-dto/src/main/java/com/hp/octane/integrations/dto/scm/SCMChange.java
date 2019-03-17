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

package com.hp.octane.integrations.dto.scm;

import com.hp.octane.integrations.dto.DTOBase;
import com.hp.octane.integrations.dto.scm.impl.LineRange;

import java.util.List;

/**
 * SCM Change DTO
 */

public interface SCMChange extends DTOBase {

	String getType();

	SCMChange setType(String type);

	String getFile();

	List<LineRange> getAddedLines();

	void setAddedLines(List<LineRange> lines);

	void insertAddedLines(LineRange newRange);

	void insertDeletedLines(LineRange newRange);

	List<LineRange> getDeletedLines();

	void setDeletedLines(List<LineRange> lines);

	SCMChange setFile(String file);

	/**
	 * in case it's delete type (that came from renaming),
	 * we want to enrich the new renamed file as part of the SCMChange.
	 * this field will be filled as part of the lines enrichment process
	 * @param file
	 */
	void setRenamedToFile(String file);

	String getRenamedToFile();
}

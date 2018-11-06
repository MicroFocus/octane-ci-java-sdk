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

package com.hp.octane.integrations.uft.items;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This file represents scm resource for sending to Octane
 */
@XmlRootElement(name = "dataTable")
@XmlAccessorType(XmlAccessType.FIELD)
public class ScmResourceFile implements SupportsMoveDetection, SupportsOctaneStatus {

	@XmlAttribute
	private String id;
	@XmlAttribute
	private String changeSetSrc;
	@XmlAttribute
	private String changeSetDst;
	@XmlAttribute
	private String oldRelativePath;
	@XmlAttribute
	private String oldName;
	@XmlAttribute
	private Boolean isMoved;
	@XmlAttribute
	private OctaneStatus octaneStatus;

	private String name;

	private String relativePath;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	@Override
	public String getChangeSetSrc() {
		return changeSetSrc;
	}

	@Override
	public void setChangeSetSrc(String changeSetSrc) {
		this.changeSetSrc = changeSetSrc;
	}

	@Override
	public String getChangeSetDst() {
		return changeSetDst;
	}

	@Override
	public void setChangeSetDst(String changeSetDst) {
		this.changeSetDst = changeSetDst;
	}

	public String getOldRelativePath() {
		return oldRelativePath;
	}

	public void setOldRelativePath(String oldRelativePath) {
		this.oldRelativePath = oldRelativePath;
	}

	public String getOldName() {
		return oldName;
	}

	public void setOldName(String oldName) {
		this.oldName = oldName;
	}

	public Boolean getIsMoved() {
		return isMoved != null ? isMoved : false;
	}

	public void setIsMoved(Boolean moved) {
		isMoved = moved;
	}

	@Override
	public OctaneStatus getOctaneStatus() {
		return octaneStatus;
	}

	public void setOctaneStatus(OctaneStatus octaneStatus) {
		this.octaneStatus = octaneStatus;
	}
}

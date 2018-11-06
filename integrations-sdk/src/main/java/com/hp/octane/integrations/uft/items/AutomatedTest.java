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
 * This file represents automated test for sending to Octane
 */
@XmlRootElement(name = "test")
@XmlAccessorType(XmlAccessType.FIELD)
public class AutomatedTest implements SupportsMoveDetection, SupportsOctaneStatus {

	@XmlAttribute
	private String id;
	@XmlAttribute
	private String changeSetSrc;
	@XmlAttribute
	private String changeSetDst;
	@XmlAttribute
	private String oldName;
	@XmlAttribute
	private String oldPackageName;
	@XmlAttribute
	private Boolean isMoved;
	@XmlAttribute
	private UftTestType uftTestType;
	@XmlAttribute
	private OctaneStatus octaneStatus;

	@XmlAttribute
	private String name;
	@XmlAttribute
	private String packageName;
	@XmlAttribute
	private Boolean executable;

	private String description;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPackage() {
		return packageName;
	}

	public void setPackage(String packageName) {
		this.packageName = packageName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setUftTestType(UftTestType uftTestType) {
		this.uftTestType = uftTestType;
	}

	public UftTestType getUftTestType() {
		return uftTestType;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Boolean getExecutable() {
		return executable;
	}

	public void setExecutable(Boolean executable) {
		this.executable = executable;
	}

	@Override
	public String toString() {
		return "#" + (getId() == null ? "0" : getId()) + " - " + getPackage() + "@" + getName();
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

	public String getOldName() {
		return oldName;
	}

	public void setOldName(String oldName) {
		this.oldName = oldName;
	}

	public String getOldPackage() {
		return oldPackageName;
	}

	public void setOldPackage(String oldPackageName) {
		this.oldPackageName = oldPackageName;
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

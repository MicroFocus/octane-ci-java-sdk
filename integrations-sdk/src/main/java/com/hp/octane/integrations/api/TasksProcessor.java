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

package com.hp.octane.integrations.api;

import com.hp.octane.integrations.dto.connectivity.OctaneResultAbridged;
import com.hp.octane.integrations.dto.connectivity.OctaneTaskAbridged;

/**
 * Tasks Processor handles ALM Octane tasks, both coming from abridged logic as well as plugin's REST call delegation.
 * Generally Tasks Processor assumed to be implemented as a singleton, and in any case it should be fully thread safe.
 */

public interface TasksProcessor {

	/**
	 * Initiates execution of Octane logic oriented task
	 */
	OctaneResultAbridged execute(OctaneTaskAbridged task);
}

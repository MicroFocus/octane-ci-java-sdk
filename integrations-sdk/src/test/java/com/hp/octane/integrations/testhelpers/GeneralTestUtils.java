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

package com.hp.octane.integrations.testhelpers;

import com.hp.octane.integrations.utils.CIPluginSDKUtils;

import java.util.function.Supplier;

/**
 * Tests helping utilities
 */

public class GeneralTestUtils {

	private GeneralTestUtils() {
	}

	/**
	 * This function will attempt to evaluate the provided condition until it is resolved or until __millisToWait__ millis has timed out
	 * Condition will be considered as resolved only when it'll return NON-NULL value (any value, so returning FALSE will also be considered as positive resolution)
	 *
	 * @param millisToWait max time to wait in millis
	 * @param condition    custom condition logic
	 * @param <T>          expected type of the result of the condition evaluation
	 * @return condition evaluation result, only in case it is NON-NULL (otherwise will continue to wait and eventually will throw)
	 */
	public static <T> T waitAtMostFor(long millisToWait, Supplier<T> condition) {
		T conditionEvaluationResult;
		long started = System.currentTimeMillis(),
				sleepPeriod = 200;
		while (System.currentTimeMillis() - started < millisToWait) {
			conditionEvaluationResult = condition.get();
			if (conditionEvaluationResult != null) {
				return conditionEvaluationResult;
			} else {
				CIPluginSDKUtils.doWait(sleepPeriod);
			}
		}
		throw new IllegalStateException(millisToWait + "ms passed away, but condition failed to resolve to NON-NULL value");
	}
}

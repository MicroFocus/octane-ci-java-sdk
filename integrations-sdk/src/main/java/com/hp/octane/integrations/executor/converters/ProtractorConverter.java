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

package com.hp.octane.integrations.executor.converters;

import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;

import java.util.List;
import java.util.stream.Collectors;

/*
 * Converter to protractor format : protractor conf.js --grep="spec1 case1|spec2 case2"
 */
public class ProtractorConverter extends CustomConverter {

    public ProtractorConverter(String format, String delimiter) {
        super(format, delimiter);
    }

}

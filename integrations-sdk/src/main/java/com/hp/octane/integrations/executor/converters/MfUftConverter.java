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

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.executor.TestExecutionInfo;
import com.hp.octane.integrations.executor.TestToRunData;
import com.hp.octane.integrations.executor.TestsToRunConverter;
import com.hp.octane.integrations.uft.UftExecutionUtils;

import java.util.ArrayList;
import java.util.List;

/*
 * Converter to uft format (MTBX)
 */
public class MfUftConverter extends TestsToRunConverter {

    private static final String DATA_TABLE_PARAMETER = "dataTable";
    private final DTOFactory dtoFactory = DTOFactory.getInstance();

    public MfUftConverter(String format, String delimiter) {
        super(format, delimiter);
    }

    @Override
    public String convert(List<TestToRunData> data, String executionDirectory) {
        List<TestExecutionInfo> result = new ArrayList<>(data.size());
        for (TestToRunData testData : data) {
            TestExecutionInfo tei = dtoFactory.newDTO(TestExecutionInfo.class);
            tei
                    .setPackageName(testData.getPackageName())
                    .setTestName(testData.getTestName())
                    .setDataTable(testData.getParamater(DATA_TABLE_PARAMETER));
            result.add(tei);

        }
        return UftExecutionUtils.convertToMtbxContent(result, executionDirectory);
    }
}

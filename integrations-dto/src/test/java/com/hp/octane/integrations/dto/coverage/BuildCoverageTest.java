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

package com.hp.octane.integrations.dto.coverage;

import com.hp.octane.integrations.dto.DTOFactory;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Build coverage tests
 */

public class BuildCoverageTest {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Test
	public void testA() throws IOException {
		LineCoverage tmpLineCovA;
		LineCoverage tmpLineCovB;
		TestCoverage[] testCoverages = new TestCoverage[2];

		FileCoverage[] locs = new FileCoverage[2];
		tmpLineCovA = dtoFactory.newDTO(LineCoverage.class);
		tmpLineCovB = dtoFactory.newDTO(LineCoverage.class);
		tmpLineCovA.setNumber(3).setCount(2);
		tmpLineCovB.setNumber(4).setCount(2);
		locs[0] = dtoFactory.newDTO(FileCoverage.class)
				.setFile("path/fileA.java")
				.setLines(new LineCoverage[]{tmpLineCovA, tmpLineCovB});

		tmpLineCovA = dtoFactory.newDTO(LineCoverage.class);
		tmpLineCovB = dtoFactory.newDTO(LineCoverage.class);
		tmpLineCovA.setNumber(1).setCount(1);
		tmpLineCovB.setNumber(2).setCount(1);
		locs[1] = dtoFactory.newDTO(FileCoverage.class)
				.setFile("path/fileB.java")
				.setLines(new LineCoverage[]{tmpLineCovA, tmpLineCovB});

		testCoverages[0] = dtoFactory.newDTO(TestCoverage.class)
				.setTestName("nameA")
				.setTestClass("classA")
				.setTestPackage("packageA")
				.setTestModule("moduleA")
				.setLocs(locs);

		locs = new FileCoverage[2];
		tmpLineCovA = dtoFactory.newDTO(LineCoverage.class);
		tmpLineCovB = dtoFactory.newDTO(LineCoverage.class);
		tmpLineCovA.setNumber(5).setCount(1);
		tmpLineCovB.setNumber(8).setCount(3);
		locs[0] = dtoFactory.newDTO(FileCoverage.class)
				.setFile("other/path/fileA.java")
				.setLines(new LineCoverage[]{tmpLineCovA, tmpLineCovB});

		tmpLineCovA = dtoFactory.newDTO(LineCoverage.class);
		tmpLineCovB = dtoFactory.newDTO(LineCoverage.class);
		tmpLineCovA.setNumber(4).setCount(1);
		tmpLineCovB.setNumber(5).setCount(1);
		locs[1] = dtoFactory.newDTO(FileCoverage.class)
				.setFile("other/path/fileB.java")
				.setLines(new LineCoverage[]{tmpLineCovA, tmpLineCovB});

		testCoverages[1] = dtoFactory.newDTO(TestCoverage.class)
				.setTestName("nameB")
				.setTestClass("classB")
				.setTestPackage("packageB")
				.setTestModule("moduleB")
				.setLocs(locs);

		BuildCoverage bc = dtoFactory.newDTO(BuildCoverage.class)
				.setTestCoverages(testCoverages);

		String json = dtoFactory.dtoToJson(bc);

		assertNotNull(json);

		BuildCoverage newBc = dtoFactory.dtoFromJson(json, BuildCoverage.class);
		assertNotNull(newBc);
		assertEquals(2, newBc.getTestCoverages().length);

		assertNotNull(newBc.getTestCoverages()[0]);
		assertEquals("nameA", newBc.getTestCoverages()[0].getTestName());
		assertEquals("classA", newBc.getTestCoverages()[0].getTestClass());
		assertEquals("packageA", newBc.getTestCoverages()[0].getTestPackage());
		assertEquals("moduleA", newBc.getTestCoverages()[0].getTestModule());

		assertEquals(2, newBc.getTestCoverages()[0].getLocs().length);
		assertNotNull(newBc.getTestCoverages()[0].getLocs()[0]);
		assertEquals("path/fileA.java", newBc.getTestCoverages()[0].getLocs()[0].getFile());
		assertEquals(2, newBc.getTestCoverages()[0].getLocs()[0].getLines().length);
		assertNotNull(newBc.getTestCoverages()[0].getLocs()[0].getLines()[0]);
		assertEquals(3, (int) newBc.getTestCoverages()[0].getLocs()[0].getLines()[0].getNumber());
		assertEquals(2, (int) newBc.getTestCoverages()[0].getLocs()[0].getLines()[0].getCount());
	}
}

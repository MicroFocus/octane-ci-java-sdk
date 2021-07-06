package uftTest;

import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import com.hp.octane.integrations.uft.items.UftTestType;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.URL;

import static com.hp.octane.integrations.uft.UftTestDiscoveryUtils.extractXmlContentFromTspFile;

public class UftTestDiscoveryUtilsTests {


    @Test
    public void readDescriptionTest() {
        URL url = getClass().getResource("Test.tsp");
        File folderPath = new File("c:\\Temp\\GUITest6");
        String description = com.hp.octane.integrations.uft.UftTestDiscoveryUtils.getTestDescription(folderPath, UftTestType.GUI);
        Assert.assertEquals("My description for test - 111222", description);
    }


    //@Test
    public void readTSPFile() throws IOException {
        File tspFile = new File("c:\\dev\\plugins\\hpaa-octane-dev-radi\\work\\workspace\\mbtTestRunnerWithParams\\GUITest1\\Test.tsp");
        InputStream is = new FileInputStream(tspFile);
        String xmlContent = extractXmlContentFromTspFile(is);
        int t=5;
    }
}


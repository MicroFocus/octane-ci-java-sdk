package uftTest;

import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import com.hp.octane.integrations.uft.items.UftTestType;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class UftTestDiscoveryUtilsTests {


    @Test
    public void readDescriptionTest() {
        URL url = getClass().getResource("Test.tsp");
        File folderPath = new File(url.getPath()).getParentFile();
        String description = com.hp.octane.integrations.uft.UftTestDiscoveryUtils.getTestDescription(folderPath, UftTestType.GUI);
        Assert.assertEquals("My description for test - 111222", description);
    }
}


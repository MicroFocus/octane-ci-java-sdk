package uftTest;

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import com.hp.octane.integrations.uft.items.UftTestDiscoveryResult;
import com.hp.octane.integrations.uft.items.UftTestType;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

import static com.hp.octane.integrations.uft.UftTestDiscoveryUtils.extractXmlContentFromTspFile;
import static com.hp.octane.integrations.uft.UftTestDiscoveryUtils.getDocument;

public class UftTestDiscoveryUtilsTests {


    //@Test
    public void scanTest(){
        File root = new File("c:\\dev\\plugins\\_uft\\UftTests\\");
        UftTestDiscoveryResult result = UftTestDiscoveryUtils.doFullDiscovery(root, TestingToolType.MBT);
        Assert.assertNotNull(result);
    }

    @Test
    public void readDescriptionTest() {
        File folderPath = new File("c:\\dev\\uftb\\uft tests\\GuiTests\\GUITest3\\");
        Document document = getDocument(folderPath, UftTestType.GUI);
        String description = com.hp.octane.integrations.uft.UftTestDiscoveryUtils.getTestDescription(document, UftTestType.GUI);
        Assert.assertEquals("My description for test - 111222", description);
    }


    //@Test
    public void readTSPFile() throws IOException, ParserConfigurationException, SAXException {
        File tspFile = new File("c:\\Temp\\GUITest6\\Test.tsp");
        InputStream is = new FileInputStream(tspFile);
        String xmlContent = extractXmlContentFromTspFile(is);

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(new InputSource(new StringReader(xmlContent)));
        NodeList funcLibsNode =  document.getElementsByTagName("FuncLibs");
        NodeList funcLibNodes =  document.getElementsByTagName("FuncLib");
        for(int i=0;i<funcLibNodes.getLength();i++){
            String fl = document.getElementsByTagName("FuncLib").item(i).getTextContent();
        }


        String[] recoveryScenarios = document.getElementsByTagName("RecoveryScenarios").item(0).getTextContent().split("\\*");
        for (int i = 0; i < recoveryScenarios.length ; i++) {
            if (recoveryScenarios != null){
                String[] rsAsArray = recoveryScenarios[i].split("\\|");
                if (rsAsArray.length > 1){
                    String rsPath = rsAsArray[0];
                    String rsName = rsAsArray[1];
                    String position = rsAsArray[2];

                }
            }
        }
        int t=5;
    }
}


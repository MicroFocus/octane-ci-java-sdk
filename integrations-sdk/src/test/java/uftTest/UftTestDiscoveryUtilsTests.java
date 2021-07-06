package uftTest;

import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
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
    public void readTSPFile() throws IOException, ParserConfigurationException, SAXException {
        File tspFile = new File("c:\\dev\\plugins\\_uft\\UftTests\\ForIdan\\GUITest161\\Test.tsp");
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


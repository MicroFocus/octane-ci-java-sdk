package uftTest;

import com.hp.octane.integrations.dto.executor.impl.TestingToolType;
import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import com.hp.octane.integrations.uft.items.*;
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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Locale;

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
        String osName = System.getProperty("os.name");
        if(!osName.toLowerCase(Locale.ROOT).contains("windows")){
            return;
        }
        File folderPath = new File(getClass().getResource("description").getFile());
        Document document = getDocument(folderPath, UftTestType.GUI);
        String description = com.hp.octane.integrations.uft.UftTestDiscoveryUtils.getTestDescription(document, UftTestType.GUI);
        Assert.assertEquals("myDesc333", description);
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

    @Test
    public void readParameterFile() {
        File mbtTestRootPath = new File(getClass().getClassLoader().getResource("mbt-tests").getFile());

        UftTestDiscoveryResult result = UftTestDiscoveryUtils.doFullDiscovery(mbtTestRootPath, TestingToolType.MBT);
        Assert.assertNotNull("null discovery result", result);

        List<UftTestAction> actions = result.getAllTests().stream()
                .map(AutomatedTest::getActions)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        Assert.assertFalse("no actions were found", actions.isEmpty());
        Assert.assertEquals("wrong number of actions were found", 14, actions.size());

        List<UftTestParameter> parameters = actions.stream()
                .map(UftTestAction::getParameters)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        Assert.assertFalse("no parameters were found", parameters.isEmpty());
        Assert.assertEquals("wrong number of parameters were found", 13, parameters.size());
    }

}


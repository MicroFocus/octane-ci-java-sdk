package uftTest;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.MbtActions;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class MbtTests {
    @Test
    public void parseConfiguration() {
        URL url = getClass().getResource("mbtExample1.json");
        MbtActions MbtActions = DTOFactory.getInstance().dtoFromJsonFile(new File(url.getFile()), MbtActions.class);
        int t=5;
    }
}

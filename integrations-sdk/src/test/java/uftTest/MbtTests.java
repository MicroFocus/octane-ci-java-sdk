package uftTest;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.general.MbtActionParameter;
import com.hp.octane.integrations.dto.general.MbtData;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class MbtTests {
    @Test
    public void parseConfiguration() {
        URL url = getClass().getResource("mbtExample1.json");
        MbtData mbtData = DTOFactory.getInstance().dtoFromJsonFile(new File(url.getFile()), MbtData.class);
        Assert.assertEquals(4, mbtData.getActions().size());
        Assert.assertEquals(6, mbtData.getData().getParameters().size());
        Assert.assertEquals(2, mbtData.getData().getIterations().size());
        mbtData.getData().getIterations().forEach(strings -> Assert.assertEquals(6, strings.size()));
        mbtData.getActions().forEach(mbtAction -> System.out.println(mbtAction.getName() + ", parameters: " +
                Optional.ofNullable(mbtAction.getParameters()).orElse(Collections.emptyList()).stream().map(MbtActionParameter::getParameterId).collect(Collectors.joining(", "))));
        mbtData.getData().getParameters().forEach(System.out::println);
        mbtData.getData().getIterations().forEach(strings -> strings.forEach(System.out::println));
    }
}

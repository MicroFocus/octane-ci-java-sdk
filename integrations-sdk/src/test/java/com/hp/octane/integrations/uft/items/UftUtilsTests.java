package com.hp.octane.integrations.uft.items;

import com.hp.octane.integrations.uft.UftTestDiscoveryUtils;
import org.junit.Assert;
import org.junit.Test;

public class UftUtilsTests {

    @Test
    public void UftTestDiscoveryUtilsConvertToHtmlFormatIfRequired1() {
        Assert.assertEquals("aa",UftTestDiscoveryUtils.convertToHtmlFormatIfRequired("aa"));
    }

    @Test
    public void UftTestDiscoveryUtilsConvertToHtmlFormatIfRequired2() {
        Assert.assertEquals("<html><body><p>aa</p><p>bb</p></body></html>",UftTestDiscoveryUtils.convertToHtmlFormatIfRequired("aa\nbb"));
    }
}


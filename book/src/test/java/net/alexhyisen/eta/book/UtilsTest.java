package net.alexhyisen.eta.book;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void basename() {
        Assert.assertEquals("without suffix", "23092719", Utils.basename("https://www.69shu.com/txt/30451/23092719"));
        Assert.assertEquals("with suffix", "6922845", Utils.basename("https://www.biquge.lu/book/14227/6922845.html"));
    }
}
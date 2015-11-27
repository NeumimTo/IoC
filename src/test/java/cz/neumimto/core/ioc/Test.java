package cz.neumimto.core.ioc;

import org.junit.Assert;

/**
 * Created by NeumimTo on 27.11.2015.
 */
public class Test {

    @org.junit.Test
    public void test() {
        IoC ioC = IoC.get();
        TestService1 build1 = ioC.build(TestService1.class);
        TestService2 build = ioC.build(TestService2.class);

        Assert.assertTrue(build != null && build == build1.getService2());
    }
}

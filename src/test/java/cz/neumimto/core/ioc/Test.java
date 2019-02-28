package cz.neumimto.core.ioc;

import org.junit.Assert;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by NeumimTo on 27.11.2015.
 */
public class Test {

    @org.junit.Test
    public void test() {
        IoC ioC = IoC.get();
        AtomicBoolean run = new AtomicBoolean(false);

        ioC.registerAnnotationCallback(TestA.class, injectContext -> {
            try {
                assert ((Field)injectContext.annotatedElement).get(injectContext.instance).equals(new Integer(100));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            run.set(true);
        });
        TestService1 build1 = ioC.build(TestService1.class);
        TestService2 build = ioC.build(TestService2.class);

        Assert.assertTrue(build != null && build == build1.getService2());

        Assert.assertTrue(run.get());
    }
}

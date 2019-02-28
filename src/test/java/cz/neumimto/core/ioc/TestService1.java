package cz.neumimto.core.ioc;

/**
 * Created by NeumimTo on 27.11.2015.
 */
@Singleton
public class TestService1 {
    @Inject
    private TestService2 service2;

    @TestA
    public int a = 100;

    public TestService2 getService2() {
        return service2;
    }
}

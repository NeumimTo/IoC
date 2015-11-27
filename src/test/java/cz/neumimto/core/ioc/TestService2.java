package cz.neumimto.core.ioc;

/**
 * Created by NeumimTo on 27.11.2015.
 */
@Singleton
public class TestService2 {

    @Inject
    private TestService1 service1;

    public TestService1 getService2() {
        return service1;
    }
}

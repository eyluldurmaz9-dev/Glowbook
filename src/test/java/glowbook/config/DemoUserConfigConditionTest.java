package glowbook.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DemoUserConfigConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DemoUserConfig.class);

    @Test
    void demoUsersAreDisabledWhenFlagIsFalse() {
        contextRunner
                .withPropertyValues("app.demo-users.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DemoUserConfig.class));
    }

    @Test
    void demoUsersAreDisabledWhenFlagIsMissing() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(DemoUserConfig.class));
    }
}

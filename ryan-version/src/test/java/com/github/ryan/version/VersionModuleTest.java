package com.github.ryan.version;

import com.github.ryan.facility.FacilityModule;
import com.github.ryan.version.release_date.service.DataVersionServiceStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class VersionModuleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VersionModule.class));

    @Test
    void testModuleLoads() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(VersionModule.class);
            // Verify that it scanned sub-packages
            assertThat(context).hasSingleBean(DataVersionServiceStrategy.class);
        });
    }

    @Test
    void testFacilityModuleIsImported() {
        this.contextRunner.run(context -> {
            // Since VersionModule imports FacilityModule, it should be in the context
            assertThat(context).hasSingleBean(FacilityModule.class);
        });
    }
}

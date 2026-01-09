package com.github.ryan.version.release_date.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DataVersionServiceStrategyTest {

    @Test
    void testStrategyInitialization() {
        // Mock services
        IDataVersionService service1 = Mockito.mock(IDataVersionService.class);
        when(service1.getDataType()).thenReturn(1);

        IDataVersionService service2 = Mockito.mock(IDataVersionService.class);
        when(service2.getDataType()).thenReturn(2);

        List<IDataVersionService> services = Arrays.asList(service1, service2);

        // Initialize strategy
        new DataVersionServiceStrategy(services);

        // Test retrieval
        Optional<IDataVersionService> retrieved1 = DataVersionServiceStrategy.getDataVersionService(1);
        assertThat(retrieved1).isPresent().contains(service1);

        Optional<IDataVersionService> retrieved2 = DataVersionServiceStrategy.getDataVersionService(2);
        assertThat(retrieved2).isPresent().contains(service2);

        Optional<IDataVersionService> retrieved3 = DataVersionServiceStrategy.getDataVersionService(3);
        assertThat(retrieved3).isEmpty();
    }

    @Test
    void testStrategyHandlesEmptyList() {
        new DataVersionServiceStrategy(Arrays.asList());
        assertThat(DataVersionServiceStrategy.getDataVersionService(1)).isEmpty();
    }
}

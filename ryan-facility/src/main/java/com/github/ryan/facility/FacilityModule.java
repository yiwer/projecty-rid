package com.github.ryan.facility;


import com.github.ryan.facility.log.LogUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;


@Configuration
@AutoConfigureOrder(value = 1)
@ComponentScan(basePackages = "com.github.ryan.facility",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {FacilityModule.class}))
public class FacilityModule {
    @PostConstruct
    public void moduleInit() {
        LogUtil.info("init module:{},order:{}", FacilityModule.class.getSimpleName(), 0);
    }

}

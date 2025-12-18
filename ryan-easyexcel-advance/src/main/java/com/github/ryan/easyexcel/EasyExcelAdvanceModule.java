package com.github.ryan.easyexcel;

import com.github.ryan.facility.FacilityModule;
import com.github.ryan.facility.log.LogUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;


@Configuration
@AutoConfigureOrder(value = 3)
@ComponentScan(basePackages = "com.github.ryan.easyexcel",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {com.github.ryan.easyexcel.EasyExcelAdvanceModule.class}))
@Import(FacilityModule.class)
public class EasyExcelAdvanceModule {
    @PostConstruct
    public void moduleInit() {
        LogUtil.info("init module:{},order:{}", EasyExcelAdvanceModule.class.getSimpleName(), 5);
    }

}
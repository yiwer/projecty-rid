module ryan.version {
    exports com.github.ryan.version;
    exports com.github.ryan.version.core;
    exports com.github.ryan.version.release_date;
    exports com.github.ryan.version.release_date.chain;
    exports com.github.ryan.version.release_date.lazy_load;
    exports com.github.ryan.version.release_date.service;
    exports com.github.ryan.version.release_date.version;

    opens com.github.ryan.version to spring.core, spring.beans, spring.context;
    opens com.github.ryan.version.core to spring.core, spring.beans, spring.context;
    opens com.github.ryan.version.release_date to spring.core, spring.beans, spring.context;
    opens com.github.ryan.version.release_date.chain to spring.core, spring.beans, spring.context;
    opens com.github.ryan.version.release_date.lazy_load to spring.core, spring.beans, spring.context;
    opens com.github.ryan.version.release_date.service to spring.core, spring.beans, spring.context;
    opens com.github.ryan.version.release_date.version to spring.core, spring.beans, spring.context;

    requires jakarta.annotation;
    requires ryan.facility;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires cn.hutool.core;
    requires static lombok;
    requires org.apache.poi.poi;
}

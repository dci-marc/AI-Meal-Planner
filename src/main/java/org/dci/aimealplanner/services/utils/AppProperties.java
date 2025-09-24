package org.dci.aimealplanner.services.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@AllArgsConstructor
@Getter
public class AppProperties {
    private String url;
}

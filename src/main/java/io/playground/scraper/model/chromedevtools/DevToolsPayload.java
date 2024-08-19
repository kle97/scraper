package io.playground.scraper.model.chromedevtools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DevToolsPayload {
    
    private Integer id;

    private Integer parentId;
    
    private String method;

    private DevToolsError error;

    private Map<String, Object> result;

    private Map<String, Object> params;

    @JsonIgnore
    public boolean hasId() {
        return id != null && id > 0;
    }

    @JsonIgnore
    public boolean isEvent() {
        return method != null && !method.isEmpty();
    }
    
    @JsonIgnore
    public boolean isError() {
        return error != null;
    }
    
    @JsonIgnore
    public boolean hasParam() {
        return params != null && !params.isEmpty();
    }

    @JsonIgnore
    public boolean isResult() {
        return result != null && !result.isEmpty();
    }
}

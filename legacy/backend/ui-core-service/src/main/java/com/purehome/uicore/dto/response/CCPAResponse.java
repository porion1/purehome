package com.purehome.uicore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * CCPA compliance response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CCPAResponse {
    private String userId;
    private List<PersonalData> personalData;
    private List<String> dataSources;
    private List<String> thirdPartyShares;
    private Instant dataCollectionStart;
    private boolean dataDeletable;

    @Data
    @AllArgsConstructor
    public static class PersonalData {
        private String category;
        private String value;
        private Instant collectedAt;
        private String source;
        private boolean sensitive;
    }
}
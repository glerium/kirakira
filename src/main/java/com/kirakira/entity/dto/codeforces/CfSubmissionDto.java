// com.kirakira.entity.dto.codeforces.CfSubmissionDto
package com.kirakira.entity.dto.codeforces;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class CfSubmissionDto {
    private String id;
    private Integer contestId;
    private LocalDateTime creationTime;
    private CfProblemDto problem;
    private CfAuthorDto author;
    private String programmingLanguage;
    private String verdict;

    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id.toString();
    }

    @JsonProperty("creationTimeSeconds")
    public void setCreationTime(Long timestamp) {
        this.creationTime = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC);
    }
}
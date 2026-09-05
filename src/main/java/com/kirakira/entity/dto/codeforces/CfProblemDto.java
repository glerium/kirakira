// com.kirakira.entity.dto.codeforces.CfProblemDto
package com.kirakira.entity.dto.codeforces;

import lombok.Data;
import java.util.List;

@Data
public class CfProblemDto {
    private Integer contestId;
    private String index;
    private String name;
    private Integer rating;
    private List<String> tags;

    public String getRatingStr() {
        if (rating != null) {
            return rating.toString();
        } else {
            return "未知rating";
        }
    }
}
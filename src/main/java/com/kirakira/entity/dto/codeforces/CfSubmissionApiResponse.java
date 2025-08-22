// com.kirakira.entity.dto.codeforces.CfSubmissionApiResponse
package com.kirakira.entity.dto.codeforces;

import lombok.Data;
import java.util.List;

@Data
public class CfSubmissionApiResponse {
    private String status;
    private List<CfSubmissionDto> result;
    private String comment;
}
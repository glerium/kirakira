// com.kirakira.entity.dto.codeforces.CfAuthorDto
package com.kirakira.entity.dto.codeforces;

import lombok.Data;
import java.util.List;

@Data
public class CfAuthorDto {
    private List<CfMemberDto> members;
    private String participantType;
}
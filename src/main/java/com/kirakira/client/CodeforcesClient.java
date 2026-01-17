package com.kirakira.client;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.kirakira.entity.dto.codeforces.CfSubmissionApiResponse;
import com.kirakira.entity.dto.codeforces.CfSubmissionDto;
import com.kirakira.entity.exception.CodeforcesApiException;
import com.kirakira.entity.exception.UserNotFoundException;

@Component
public class CodeforcesClient {
    private final RestTemplate restTemplate;
    private static final String API_URL = "https://codeforces.com/api/user.status?handle=%s&from=1&count=10";

    public CodeforcesClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<CfSubmissionDto> getRecentSubmissions(String handle) {
        String url = String.format(API_URL, handle);

        try {
            CfSubmissionApiResponse response = restTemplate.getForObject(url, CfSubmissionApiResponse.class);

            // 检查响应是否为 null
            if (response == null) {
                throw new CodeforcesApiException("API 返回空响应");
            }

            // 检查状态是否为 null 或不是 "OK"
            if (response.getStatus() == null || !response.getStatus().equals("OK")) {
                var comment = response.getComment();
                if(comment != null && comment.contains("handle: User with handle") && comment.contains("not found")) {
                    throw new UserNotFoundException("用户 " + handle + " 不存在");
                }
                return new ArrayList<>();
            }
            
            long timeStartToCollect = (System.currentTimeMillis() / 1000) - 1800;

            return response.getResult().stream()
                .filter(submission -> 
                    submission.getCreationTime() != null && 
                    submission.getCreationTime().toEpochSecond(ZoneOffset.UTC) >= timeStartToCollect &&
                    submission.getVerdict().equals("OK")
                )
                .collect(Collectors.toList());

        } catch (HttpClientErrorException e) {
            throw new CodeforcesApiException(e.getLocalizedMessage(), e);
        }
    }
}

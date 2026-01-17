package com.kirakira.client;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import com.kirakira.entity.dto.codeforces.CfSubmissionApiResponse;
import com.kirakira.entity.dto.codeforces.CfSubmissionDto;
import com.kirakira.entity.exception.CodeforcesApiException;
import com.kirakira.entity.exception.UserNotFoundException;

@Component
public class CodeforcesClient {
    private static final Logger log = LoggerFactory.getLogger(CodeforcesClient.class);
    private final RestTemplate restTemplate;
    private static final String API_URL = "https://codeforces.com/api/user.status?handle=%s&from=1&count=10";
    private final int maxRetries;
    private final long retryDelayMs;

    public CodeforcesClient(RestTemplate restTemplate,
                           @Value("${bot.codeforces.api.max.retries:3}") int maxRetries,
                           @Value("${bot.codeforces.api.retry.delay.ms:2000}") long retryDelayMs) {
        this.restTemplate = restTemplate;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    public List<CfSubmissionDto> getRecentSubmissions(String handle) {
        String url = String.format(API_URL, handle);
        
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
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

            } catch (UserNotFoundException e) {
                // 用户不存在异常不需要重试
                throw e;
            } catch (HttpClientErrorException e) {
                // HTTP客户端错误（4xx）通常不需要重试
                log.error("Codeforces API HTTP错误 (用户: {}): {}", handle, e.getMessage());
                throw new CodeforcesApiException(e.getLocalizedMessage(), e);
            } catch (ResourceAccessException e) {
                // 网络连接异常，需要重试
                lastException = e;
                retryCount++;
                log.warn("Codeforces API请求失败 (用户: {}), 重试 {}/{}: {}", 
                         handle, retryCount, maxRetries, e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * retryCount); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CodeforcesApiException("请求被中断", ie);
                    }
                }
            } catch (Exception e) {
                // 其他异常也尝试重试
                lastException = e;
                retryCount++;
                log.warn("Codeforces API请求出错 (用户: {}), 重试 {}/{}: {}", 
                         handle, retryCount, maxRetries, e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CodeforcesApiException("请求被中断", ie);
                    }
                }
            }
        }
        
        // 所有重试都失败了
        log.error("Codeforces API请求失败，已达到最大重试次数 (用户: {})", handle);
        throw new CodeforcesApiException("API请求失败，已达到最大重试次数: " + 
                                        (lastException != null ? lastException.getMessage() : "未知错误"), 
                                        lastException);
    }
}

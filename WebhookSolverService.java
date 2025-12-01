package com.example.webhooksolver.service;

import com.example.webhooksolver.model.GenerateWebhookRequest;
import com.example.webhooksolver.model.GenerateWebhookResponse;
import com.example.webhooksolver.util.SqlLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.Map;

@Service
public class WebhookSolverService {

    private final Logger log = LoggerFactory.getLogger(WebhookSolverService.class);
    private final RestTemplate restTemplate;
    private final SqlLoader sqlLoader;

    @Value("${webhook.generate.url}")
    private String generateUrl;

    @Value("${app.name}")
    private String name;

    @Value("${app.regNo}")
    private String regNo;

    @Value("${app.email}")
    private String email;

    public WebhookSolverService(RestTemplate restTemplate, SqlLoader sqlLoader) {
        this.restTemplate = restTemplate;
        this.sqlLoader = sqlLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Application started - beginning webhook generation flow");
        try {
            // 1) POST to generate webhook
            GenerateWebhookRequest req = new GenerateWebhookRequest(name, regNo, email);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<GenerateWebhookRequest> entity = new HttpEntity<>(req, headers);

            ResponseEntity<GenerateWebhookResponse> resp = restTemplate.postForEntity(generateUrl, entity, GenerateWebhookResponse.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("Failed to generate webhook. Status: {}", resp.getStatusCode());
                return;
            }

            GenerateWebhookResponse body = resp.getBody();
            String webhookUrl = body.getWebhook();
            String accessToken = body.getAccessToken();
            log.info("Received webhook: {}", webhookUrl);
            log.info("Received accessToken (truncated): {}", accessToken == null ? null : accessToken.substring(0, Math.min(10, accessToken.length())) + "...");

            // 2) Determine which question to solve based on last two digits of regNo
            int questionNumber = determineQuestionNumber(regNo);
            log.info("Selected question number: {}", questionNumber);

            // 3) Load SQL solution for the question from resources
            String finalQuery = sqlLoader.loadSqlForQuestion(questionNumber);
            log.info("Loaded final query (first 200 chars): {}", finalQuery.length() > 200 ? finalQuery.substring(0, 200) + "..." : finalQuery);

            // 4) Send final query to webhook URL with Authorization header
            HttpHeaders headers2 = new HttpHeaders();
            headers2.setContentType(MediaType.APPLICATION_JSON);
            // The spec says: Use accessToken as JWT in Authorization header. Common pattern: 'Authorization: Bearer <token>'
            headers2.setBearerAuth(accessToken);

            Map<String, String> payload = Map.of("finalQuery", finalQuery);
            HttpEntity<Map<String, String>> entity2 = new HttpEntity<>(payload, headers2);

            ResponseEntity<String> webhookResp = restTemplate.postForEntity(webhookUrl, entity2, String.class);
            if (webhookResp.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully submitted final query to webhook. Response: {}", webhookResp.getBody());
            } else {
                log.error("Failed to submit final query. Status: {}, Body: {}", webhookResp.getStatusCode(), webhookResp.getBody());
            }

        } catch (Exception ex) {
            log.error("Error during webhook flow", ex);
        }
    }

    private int determineQuestionNumber(String regNo) {
        if (regNo == null || regNo.length() < 2) return 1; // default
        // extract last two digits/characters
        String lastTwo = regNo.substring(Math.max(0, regNo.length() - 2));
        // try to parse digits; if fails, fall back to ascii-based parity
        try {
            int val = Integer.parseInt(lastTwo.replaceAll("\\D", ""));
            return (val % 2 == 0) ? 2 : 1;
        } catch (Exception e) {
            // fallback: sum char codes
            int sum = 0;
            for (char c : lastTwo.toCharArray()) sum += c;
            return (sum % 2 == 0) ? 2 : 1;
        }
    }
}

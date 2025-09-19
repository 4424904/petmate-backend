package com.petmate.domain.company.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ServiceParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 서비스 매핑 (프론트엔드와 동일)
    private static final Map<String, String> SERVICE_MAP = Map.of(
        "돌봄", "1",
        "산책", "2",
        "미용", "3",
        "병원", "4",
        "기타", "9"
    );

    // 역방향 매핑
    private static final Map<String, String> SERVICE_NAME_MAP = Map.of(
        "1", "돌봄",
        "2", "산책",
        "3", "미용",
        "4", "병원",
        "9", "기타"
    );

    /**
     * 서비스 JSON을 파싱해서 서비스명 리스트로 변환
     */
    public static List<String> parseServices(String servicesJson, String repService) {
        List<String> serviceNames = new ArrayList<>();

        try {
            if (servicesJson != null && !servicesJson.trim().isEmpty()) {
                JsonNode servicesNode = objectMapper.readTree(servicesJson);

                // JSON 객체에서 true인 서비스들만 추출
                servicesNode.fields().forEachRemaining(entry -> {
                    String serviceKey = entry.getKey();
                    boolean isProvided = entry.getValue().asBoolean();

                    if (isProvided) {
                        serviceNames.add(serviceKey);
                    }
                });
            }

            // 서비스가 없고 대표 서비스가 있으면 대표 서비스 사용
            if (serviceNames.isEmpty() && repService != null) {
                String serviceName = SERVICE_NAME_MAP.getOrDefault(repService, "기타");
                serviceNames.add(serviceName);
            }

            // 여전히 비어있으면 기타 추가
            if (serviceNames.isEmpty()) {
                serviceNames.add("기타");
            }

        } catch (Exception e) {
            log.error("서비스 파싱 중 오류:", e);
            // 오류 발생시 대표 서비스나 기타로 fallback
            if (repService != null) {
                String serviceName = SERVICE_NAME_MAP.getOrDefault(repService, "기타");
                serviceNames.add(serviceName);
            } else {
                serviceNames.add("기타");
            }
        }

        return serviceNames;
    }
}
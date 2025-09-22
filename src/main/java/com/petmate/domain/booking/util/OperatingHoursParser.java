package com.petmate.domain.booking.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petmate.domain.booking.dto.OperatingHours;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
@Slf4j
public class OperatingHoursParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OperatingHours parseOperatingHours(String operatingHoursJson, LocalDate date) {
        try {
            log.info("운영시간 파싱 시작 - date: {}, json: {}", date, operatingHoursJson);

            if(operatingHoursJson == null || operatingHoursJson.trim().isEmpty()) {
                log.info("운영시간 JSON이 비어있음, 기본값 사용");
                return getDefaultOperatingHours();
            }

            JsonNode root = objectMapper.readTree(operatingHoursJson);
            log.info("JSON 파싱 성공");

            // 24시간 영업 체크
            if (root.has("allDay") && root.get("allDay").asBoolean()) {
                log.info("24시간 영업으로 설정됨");
                return OperatingHours.builder()
                        .startTime(LocalTime.of(0, 0))
                        .endTime(LocalTime.of(23, 59))
                        .closed(false)
                        .build();
            }

            // schedule 구조 체크
            if (!root.has("schedule")) {
                log.info("schedule 필드가 없음, 기본값 사용");
                return getDefaultOperatingHours();
            }

            JsonNode schedule = root.get("schedule");
            String dayKey = getDayKey(date.getDayOfWeek());
            log.info("요일 키: {}", dayKey);

            JsonNode dayNode = schedule.get(dayKey);

            if(dayNode == null) {
                log.info("해당 요일({}) 정보가 없음, 기본값 사용", dayKey);
                return getDefaultOperatingHours();
            }

            // 휴무일 체크
            boolean closed = dayNode.has("closed") && dayNode.get("closed").asBoolean(false);
            if(closed) {
                log.info("해당 요일({})은 휴무일", dayKey);
                return OperatingHours.builder()
                        .closed(true)
                        .build();
            }

            // 시간 파싱 (open/close 키 사용)
            String startTimeStr = dayNode.has("open") ? dayNode.get("open").asText() : "09:00";
            String endTimeStr = dayNode.has("close") ? dayNode.get("close").asText() : "18:00";

            log.info("시간 파싱 - 시작: {}, 종료: {}", startTimeStr, endTimeStr);

            LocalTime startTime = LocalTime.parse(startTimeStr);
            LocalTime endTime = LocalTime.parse(endTimeStr);

            OperatingHours result = OperatingHours.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .closed(false)
                    .build();

            log.info("운영시간 파싱 완료 - 시작: {}, 종료: {}", startTime, endTime);
            return result;

        } catch (Exception e) {
            log.error("운영시간 파싱 오류 - date: {}, json: {}", date, operatingHoursJson, e);
            return getDefaultOperatingHours();
        }
    }

    private OperatingHours getDefaultOperatingHours() {
        return OperatingHours.builder()
                .startTime(LocalTime.of(9,0))
                .endTime(LocalTime.of(18,0))
                .closed(false)
                .build();
    }

    private String getDayKey(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return "월요일";
            case TUESDAY:
                return "화요일";
            case WEDNESDAY:
                return "수요일";
            case THURSDAY:
                return "목요일";
            case FRIDAY:
                return "금요일";
            case SATURDAY:
                return "토요일";
            case SUNDAY:
                return "일요일";
            default:
                return "월요일";
        }
    }



}

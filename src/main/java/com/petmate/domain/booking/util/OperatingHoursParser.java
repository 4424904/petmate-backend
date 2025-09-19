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

    public OperatingHours parseOperatingHours(String operatingHours, LocalDate date) {
        try {
            if(operatingHours == null || operatingHours.trim().isEmpty()) {
                return getDefaultOperatingHours();
            }

            JsonNode root = objectMapper.readTree(operatingHours);
            String dayKey = getDayKey(date.getDayOfWeek());
            JsonNode dayNode = root.get(dayKey);

            if(dayNode == null) {
                return getDefaultOperatingHours();
            }

            boolean closed = dayNode.has("closed") && dayNode.get("closed").asBoolean(false);
            if(closed) {
                return OperatingHours.builder().build();
            }

            String startTimeStr = dayNode.has("start") ? dayNode.get("start").asText() : "09:00";
            String endTimeStr = dayNode.has("end") ? dayNode.get("end").asText() : "18:00";

            LocalTime startTime = LocalTime.parse(startTimeStr);
            LocalTime endTime = LocalTime.parse(endTimeStr);

            return OperatingHours.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .closed(false)
                    .build();

        } catch (Exception e) {
            log.error("운영시간 파싱 오류 {}", operatingHours ,e);
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
                return "monday";
            case TUESDAY:
                return "tuesday";
            case WEDNESDAY:
                return "wednesday";
            case THURSDAY:
                return "thursday";
            case FRIDAY:
                return "friday";
            case SATURDAY:
                return "saturday";
            case SUNDAY:
                return "sunday";
            default:
                return "monday";
        }
    }



}

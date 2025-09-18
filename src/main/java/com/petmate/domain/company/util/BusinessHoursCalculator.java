package com.petmate.domain.company.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BusinessHoursCalculator {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String[] DAY_NAMES = {"일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"};

    /**
     * 현재 영업 상태를 계산합니다.
     */
    public static Map<String, String> calculateCurrentBusinessStatus(String operatingHoursJson) {
        Map<String, String> result = new HashMap<>();

        try {
            if (operatingHoursJson == null || operatingHoursJson.trim().isEmpty()) {
                result.put("status", "정보없음");
                result.put("message", "영업시간 정보 없음");
                return result;
            }

            JsonNode operatingHours = objectMapper.readTree(operatingHoursJson);

            // 24시간 영업 체크
            if (operatingHours.has("allDay") && operatingHours.get("allDay").asBoolean()) {
                result.put("status", "영업중");
                result.put("message", "24시간 영업");
                return result;
            }

            // 스케줄 기반 계산
            if (operatingHours.has("schedule")) {
                LocalDateTime now = LocalDateTime.now();
                int today = now.getDayOfWeek().getValue() % 7; // 0=일요일, 1=월요일, ...
                String todayName = DAY_NAMES[today];

                JsonNode schedule = operatingHours.get("schedule");
                JsonNode todaySchedule = schedule.get(todayName);

                if (todaySchedule == null || (todaySchedule.has("closed") && todaySchedule.get("closed").asBoolean())) {
                    result.put("status", "휴무");
                    result.put("message", "오늘 휴무");
                    return result;
                }

                String openTime = todaySchedule.get("open").asText();
                String closeTime = todaySchedule.get("close").asText();

                // 현재 시간을 HHMM 형태로 변환
                int currentTime = now.getHour() * 100 + now.getMinute();
                int openHHMM = parseTimeToHHMM(openTime);
                int closeHHMM = parseTimeToHHMM(closeTime);

                if (currentTime >= openHHMM && currentTime < closeHHMM) {
                    result.put("status", "영업중");
                    result.put("message", closeTime + "에 영업 종료");
                } else if (currentTime < openHHMM) {
                    result.put("status", "영업전");
                    result.put("message", openTime + "에 영업 시작");
                } else {
                    result.put("status", "영업종료");
                    result.put("message", "영업 종료");
                }
                return result;
            }

        } catch (Exception e) {
            log.error("영업시간 계산 중 오류:", e);
        }

        result.put("status", "정보없음");
        result.put("message", "영업시간 정보 없음");
        return result;
    }

    /**
     * 오늘의 영업시간을 계산합니다.
     */
    public static String calculateTodayHours(String operatingHoursJson) {
        try {
            if (operatingHoursJson == null || operatingHoursJson.trim().isEmpty()) {
                return "영업시간 정보 없음";
            }

            JsonNode operatingHours = objectMapper.readTree(operatingHoursJson);

            // 24시간 영업 체크
            if (operatingHours.has("allDay") && operatingHours.get("allDay").asBoolean()) {
                return "24시간 영업";
            }

            // 스케줄 기반 계산
            if (operatingHours.has("schedule")) {
                LocalDateTime now = LocalDateTime.now();
                int today = now.getDayOfWeek().getValue() % 7; // 0=일요일, 1=월요일, ...
                String todayName = DAY_NAMES[today];

                JsonNode schedule = operatingHours.get("schedule");
                JsonNode todaySchedule = schedule.get(todayName);

                if (todaySchedule == null || (todaySchedule.has("closed") && todaySchedule.get("closed").asBoolean())) {
                    return "오늘 휴무";
                }

                String openTime = todaySchedule.has("open") ? todaySchedule.get("open").asText() : "시간미정";
                String closeTime = todaySchedule.has("close") ? todaySchedule.get("close").asText() : "시간미정";

                return openTime + " - " + closeTime;
            }

        } catch (Exception e) {
            log.error("오늘 영업시간 계산 중 오류:", e);
        }

        return "영업시간 정보 없음";
    }

    /**
     * 요일별 영업시간 스케줄을 계산합니다.
     */
    public static List<Map<String, String>> calculateWeeklySchedule(String operatingHoursJson) {
        List<Map<String, String>> weeklySchedule = new ArrayList<>();

        try {
            if (operatingHoursJson == null || operatingHoursJson.trim().isEmpty()) {
                // 모든 요일을 "정보없음"으로 채움
                String[] dayShortNames = {"월", "화", "수", "목", "금", "토", "일"};
                for (String day : dayShortNames) {
                    Map<String, String> dayInfo = new HashMap<>();
                    dayInfo.put("day", day);
                    dayInfo.put("status", "정보없음");
                    weeklySchedule.add(dayInfo);
                }
                return weeklySchedule;
            }

            JsonNode operatingHours = objectMapper.readTree(operatingHoursJson);

            // 24시간 영업인 경우
            if (operatingHours.has("allDay") && operatingHours.get("allDay").asBoolean()) {
                String[] dayShortNames = {"월", "화", "수", "목", "금", "토", "일"};
                for (String day : dayShortNames) {
                    Map<String, String> dayInfo = new HashMap<>();
                    dayInfo.put("day", day);
                    dayInfo.put("status", "24시간");
                    weeklySchedule.add(dayInfo);
                }
                return weeklySchedule;
            }

            // 스케줄 기반 계산
            if (operatingHours.has("schedule")) {
                JsonNode schedule = operatingHours.get("schedule");
                String[] dayOrder = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};
                String[] dayShortNames = {"월", "화", "수", "목", "금", "토", "일"};

                for (int i = 0; i < dayOrder.length; i++) {
                    String fullDayName = dayOrder[i];
                    String shortDayName = dayShortNames[i];
                    JsonNode daySchedule = schedule.get(fullDayName);

                    Map<String, String> dayInfo = new HashMap<>();
                    dayInfo.put("day", shortDayName);

                    if (daySchedule == null) {
                        dayInfo.put("status", "정보없음");
                    } else if (daySchedule.has("closed") && daySchedule.get("closed").asBoolean()) {
                        dayInfo.put("status", "휴무");
                    } else {
                        String openTime = daySchedule.has("open") ? daySchedule.get("open").asText() : "시간미정";
                        String closeTime = daySchedule.has("close") ? daySchedule.get("close").asText() : "시간미정";
                        dayInfo.put("status", openTime + " - " + closeTime);
                    }

                    weeklySchedule.add(dayInfo);
                }
            }

        } catch (Exception e) {
            log.error("요일별 스케줄 계산 중 오류:", e);
            // 오류 발생 시 기본값으로 채움
            String[] dayShortNames = {"월", "화", "수", "목", "금", "토", "일"};
            for (String day : dayShortNames) {
                Map<String, String> dayInfo = new HashMap<>();
                dayInfo.put("day", day);
                dayInfo.put("status", "정보없음");
                weeklySchedule.add(dayInfo);
            }
        }

        return weeklySchedule;
    }

    /**
     * 시간 문자열을 HHMM 형태 숫자로 변환 (예: "09:00" -> 900)
     */
    private static int parseTimeToHHMM(String timeStr) {
        try {
            String cleanTime = timeStr.replace(":", "");
            return Integer.parseInt(cleanTime);
        } catch (Exception e) {
            log.warn("시간 파싱 실패: {}", timeStr);
            return 0;
        }
    }
}
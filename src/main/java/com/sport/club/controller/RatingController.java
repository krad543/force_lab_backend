package com.sport.club.controller;

import com.sport.club.model.entity.Athlete;
import com.sport.club.repository.AthleteRepository;
import com.sport.club.repository.PersonalRecordRepository;
import com.sport.club.repository.TrainingAttendanceRepository;
import com.sport.club.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://force-lab.vercel.app"
})
public class RatingController {

    private final AthleteRepository athleteRepository;
    private final PersonalRecordRepository recordRepository;
    private final TrainingAttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    /**
     * GET /api/rating?sportType=Бег&status=BEGINNER
     * Рейтинг по виду спорта и статусу
     */
    @GetMapping
    public ResponseEntity<List<Map<String,Object>>> getRating(
            @RequestParam(required = false) String sportType,
            @RequestParam(required = false) String status) {

        List<Athlete> athletes = athleteRepository.findAll();

        if (sportType != null && !sportType.isEmpty())
            athletes = athletes.stream().filter(a -> sportType.equals(a.getSportType())).collect(Collectors.toList());

        if (status != null && !status.isEmpty()) {
            final String s = status;
            athletes = athletes.stream().filter(a -> {
                try {
                    var f = a.getClass().getDeclaredField("athleteStatus");
                    f.setAccessible(true);
                    return s.equals(f.get(a));
                } catch (Exception e) { return false; }
            }).collect(Collectors.toList());
        }

        List<Map<String,Object>> result = new ArrayList<>();
        for (Athlete athlete : athletes) {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("athleteId", athlete.getId());

            String name = "";
            try { name = athlete.getUser() != null ? athlete.getUser().getFullName() : ""; } catch(Exception ignored){}
            row.put("athleteName", name);
            row.put("sportType", athlete.getSportType());

            try {
                var f = athlete.getClass().getDeclaredField("athleteStatus");
                f.setAccessible(true);
                row.put("athleteStatus", f.get(athlete));
            } catch (Exception e) { row.put("athleteStatus", "BEGINNER"); }

            // Лучший результат
            try {
                var records = recordRepository.findByAthleteIdOrderByRecordValueDesc(athlete.getId());

                if (!records.isEmpty()) {
                    row.put("bestResult", records.get(0).getRecordValue());
                } else {
                    row.put("bestResult", null);
                }
            } catch(Exception e) { row.put("bestResult", null); }

            // Количество тренировок
            try {
                long cnt = attendanceRepository.findByAthleteId(athlete.getId()).stream()
                        .filter(a -> "ATTENDED".equals(a.getStatus()) || "LATE".equals(a.getStatus()))
                        .count();
                row.put("trainingsCount", cnt);
            } catch(Exception e) { row.put("trainingsCount", 0); }

            result.add(row);
        }

        // Сортируем по лучшему результату (убывание)
        result.sort((a, b) -> {
            Double ra = a.get("bestResult") != null ? ((Number)a.get("bestResult")).doubleValue() : -1;
            Double rb = b.get("bestResult") != null ? ((Number)b.get("bestResult")).doubleValue() : -1;
            return Double.compare(rb, ra);
        });

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rating/leaders
     * Лидеры по каждому виду спорта и статусу
     */
    @GetMapping("/leaders")
    public ResponseEntity<Map<String,Object>> getLeaders() {
        String[] sports = {"Лёгкая атлетика","Велоспорт","Плавание","Теннис","Тяжёлая атлетика"};
        String[] statuses = {"BEGINNER","MAIN","ADVANCED"};

        Map<String,Object> leaders = new LinkedHashMap<>();
        for (String sport : sports) {
            for (String status : statuses) {
                String key = sport + "_" + status;
                try {
                    List<Map<String,Object>> top = (List<Map<String,Object>>) getRating(sport, status).getBody();
                    if (top != null && !top.isEmpty() && top.get(0).get("bestResult") != null) {
                        Map<String,Object> leader = new LinkedHashMap<>();
                        leader.put("athleteName", top.get(0).get("athleteName"));
                        leader.put("bestResult", top.get(0).get("bestResult"));
                        leader.put("sportType", sport);
                        leader.put("status", status);
                        leaders.put(key, leader);
                    }
                } catch(Exception ignored){}
            }
        }
        return ResponseEntity.ok(leaders);
    }
}

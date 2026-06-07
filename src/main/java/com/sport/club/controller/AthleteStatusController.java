package com.sport.club.controller;

import com.sport.club.model.entity.Athlete;
import com.sport.club.model.entity.User;
import com.sport.club.repository.AthleteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/athletes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AthleteStatusController {

    private final AthleteRepository athleteRepository;

    /**
     * PUT /api/athletes/{athleteId}/status
     * Тренер устанавливает статус спортсмена:
     * BEGINNER = Начинающий
     * MAIN     = Основной
     * ADVANCED = Продвинутый
     */
    @PutMapping("/{athleteId}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable UUID athleteId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User coach) {

        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(() -> new RuntimeException("Спортсмен не найден"));

        String status = body.get("status");
        if (!List.of("BEGINNER","MAIN","ADVANCED").contains(status))
            throw new RuntimeException("Неверный статус");

        // Устанавливаем статус через дополнительное поле
        // Если поля нет в Athlete — добавь: private String athleteStatus = "BEGINNER";
        try {
            var field = athlete.getClass().getDeclaredField("athleteStatus");
            field.setAccessible(true);
            field.set(athlete, status);
        } catch (Exception e) {
            // Если поля нет — добавь его в Athlete.java
            return ResponseEntity.badRequest()
                .body(Map.of("error","Добавь поле athleteStatus в Athlete.java"));
        }

        athleteRepository.save(athlete);
        return ResponseEntity.ok(Map.of("status", status, "message", "Статус обновлён"));
    }
}

// Не забудь добавить в Athlete.java:
// @Column(name = "athlete_status")
// private String athleteStatus = "BEGINNER";

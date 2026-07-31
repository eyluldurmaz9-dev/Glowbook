package glowbook.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/actuator/health", "/health"})
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "glowbook",
                "timestamp", Instant.now().toString()
        );
    }
}

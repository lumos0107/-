package com.gildongmu.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/gps-debug")
@CrossOrigin(origins = "*")
public class GpsDebugController {

    private int requestCount = 0;
    private final List<Map<String, Object>> trackLog = new ArrayList<>();

    @PostMapping("/track")
    public ResponseEntity<Map<String, Object>> debugGps(@RequestBody Map<String, Object> body) {
        requestCount++;

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("seq", requestCount);
        entry.put("lat", body.get("lat"));
        entry.put("lng", body.get("lng"));
        entry.put("accuracy", body.get("accuracy"));
        entry.put("timestamp", body.getOrDefault("timestamp", System.currentTimeMillis()));
        trackLog.add(entry);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("seq", requestCount);
        res.put("received", entry);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/log")
    public ResponseEntity<List<Map<String, Object>>> getLog() {
        return ResponseEntity.ok(trackLog);
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        requestCount = 0;
        trackLog.clear();
        return ResponseEntity.ok(Map.of("ok", true, "message", "초기화 완료"));
    }
}

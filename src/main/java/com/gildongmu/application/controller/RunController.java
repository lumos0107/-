package com.gildongmu.application.controller;

import com.gildongmu.application.dto.RunEndRequest;
import com.gildongmu.application.dto.RunStartRequest;
import com.gildongmu.application.dto.RunUpdateRequest;
import com.gildongmu.database.entity.RunningRecord;
import com.gildongmu.application.service.RunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
public class RunController {

    private final RunService runService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Integer>> start(@RequestBody RunStartRequest request) {
        int recordId = runService.startRun(request);
        return ResponseEntity.ok(Map.of("recordId", recordId));
    }

    @PostMapping("/update")
    public ResponseEntity<Void> update(@RequestBody RunUpdateRequest request) {
        runService.updateRun(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/end")
    public ResponseEntity<Void> end(@RequestBody RunEndRequest request) {
        runService.endRun(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<RunningRecord>> history(@RequestParam int userId) {
        return ResponseEntity.ok(runService.getHistory(userId));
    }
}


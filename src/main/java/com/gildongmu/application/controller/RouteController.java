package com.gildongmu.application.controller;

import com.gildongmu.application.dto.RouteRecommendRequest;
import com.gildongmu.application.dto.RouteRecommendResponse;
import com.gildongmu.application.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping("/recommend")
    public ResponseEntity<RouteRecommendResponse> recommend(
            @RequestBody RouteRecommendRequest request) {
        return ResponseEntity.ok(routeService.recommend(request));
    }
}


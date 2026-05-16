package com.project.backend.domain.chat.controller;

import com.project.backend.domain.chat.service.VectorReSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/vector")
@RequiredArgsConstructor
public class VectorAdminController {

    private final VectorReSyncService vectorReSyncService;

    @PostMapping("/resync")
    public ResponseEntity<String> resync() {
        log.info("벡터 전체 재동기화 요청 수신");
        int count = vectorReSyncService.resyncAll();
        return ResponseEntity.ok("재동기화 완료: " + count + "건");
    }
}
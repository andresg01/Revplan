package com.revapp.planengine.infra.api.rest.controllers;

import com.revapp.planengine.domain.model.ScorePreviewRequest;
import com.revapp.planengine.domain.service.ScorePreviewService;
import com.revapp.planengine.infra.api.dto.PreviewScoresRequestDTO;
import com.revapp.planengine.infra.api.dto.ScorePreviewDataDTO;
import com.revapp.planengine.infra.api.rest.mapper.ScoresApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ScoresController implements ScoresApi {

    private final ScorePreviewService scorePreviewService;
    private final ScoresApiMapper scoresApiMapper;

    @Override
    public ResponseEntity<ScorePreviewDataDTO> previewScores(UUID xRequestID,
                                                             PreviewScoresRequestDTO previewScoresRequestDTO,
                                                             String acceptLanguage) {
        var req = scoresApiMapper.toModel(previewScoresRequestDTO);
        var data = scorePreviewService.preview(req);
        return ResponseEntity.ok(scoresApiMapper.toDto(data));
    }


}

package com.revapp.planengine.domain.service;

import com.revapp.planengine.domain.model.ScorePreviewData;
import com.revapp.planengine.domain.model.ScorePreviewRequest;

public interface ScorePreviewService {
    ScorePreviewData preview(ScorePreviewRequest request);
}

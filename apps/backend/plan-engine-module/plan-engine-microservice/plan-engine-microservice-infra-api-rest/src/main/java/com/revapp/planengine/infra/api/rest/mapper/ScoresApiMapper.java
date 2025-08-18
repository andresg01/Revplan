package com.revapp.planengine.infra.api.rest.mapper;

import com.revapp.planengine.domain.model.ScorePreviewData;
import com.revapp.planengine.domain.model.ScorePreviewItem;
import com.revapp.planengine.infra.api.dto.ScorePreviewDataDTO;
import com.revapp.planengine.infra.api.dto.ScorePreviewItemDTO;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ScoresApiMapper {

    default ScorePreviewDataDTO toDto(ScorePreviewData data) {
        if (data == null) return null;
        ScorePreviewDataDTO out = new ScorePreviewDataDTO();
        List<ScorePreviewItemDTO> list = new ArrayList<>();
        if (data.getData() != null) {
            for (ScorePreviewItem it : data.getData()) {
                ScorePreviewItemDTO dto = new ScorePreviewItemDTO();
                dto.setTemplateId(it.getTemplateId());
                dto.setScore(it.getScore());
                dto.setFactors(it.getFactors());
                list.add(dto);
            }
        }
        out.setData(list);
        return out;
    }
}

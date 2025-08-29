package com.revplan.llm.domain.model;

import com.revplan.llm.domain.enums.ImageQualityEnum;
import com.revplan.llm.domain.enums.ImageSizeEnum;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageMatrixItem {
    private ImageQualityEnum quality;
    private ImageSizeEnum size;
    private BigDecimal amount;
}

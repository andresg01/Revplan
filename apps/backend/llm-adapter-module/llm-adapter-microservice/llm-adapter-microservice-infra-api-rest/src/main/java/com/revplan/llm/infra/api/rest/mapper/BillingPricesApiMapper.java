package com.revplan.llm.infra.api.rest.mapper;

import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;

import com.revplan.llm.domain.enums.*;
import com.revplan.llm.domain.model.*;

import com.revplan.llm.infra.api.dto.BuiltInToolItemDTO;
import com.revplan.llm.infra.api.dto.BuiltInToolItemPricingDTO;
import com.revplan.llm.infra.api.dto.EmbeddingItemDTO;
import com.revplan.llm.infra.api.dto.EmbeddingItemPriceDTO;
import com.revplan.llm.infra.api.dto.FineTuningItemDTO;
import com.revplan.llm.infra.api.dto.GetBillingPrices200ResponseDTO;
import com.revplan.llm.infra.api.dto.ImageGenerationItemDTO;
import com.revplan.llm.infra.api.dto.ImageMatrixItemDTO;
import com.revplan.llm.infra.api.dto.PriceCatalogCategoriesDTO;
import com.revplan.llm.infra.api.dto.PriceCatalogCategoriesBuiltInToolsDTO;
import com.revplan.llm.infra.api.dto.PriceCatalogCategoriesEmbeddingsDTO;
import com.revplan.llm.infra.api.dto.PriceCatalogCategoriesFineTuningDTO;
import com.revplan.llm.infra.api.dto.PriceCatalogCategoriesImageGenerationDTO;
import com.revplan.llm.infra.api.dto.PriceCatalogCategoriesModerationDTO;
import com.revplan.llm.infra.api.dto.PriceCatalogCategoriesTranscriptionSpeechDTO;
import com.revplan.llm.infra.api.dto.PriceCatalogDTO;
import com.revplan.llm.infra.api.dto.PriceTripleDTO;
import com.revplan.llm.infra.api.dto.PriceUnitDTO;
import com.revplan.llm.infra.api.dto.TextModelPriceDTO;
import com.revplan.llm.infra.api.dto.TokensCategoryDTO;
import com.revplan.llm.infra.api.dto.TranscriptionSpeechItemDTO;
import com.revplan.llm.infra.api.dto.TranscriptionSpeechItemOtherUnitsInnerDTO;

@Mapper(componentModel = "spring")
public interface BillingPricesApiMapper {

    // =========================================================================
    // Enums
    // =========================================================================
    default BuiltInToolItemDTO.ToolEnum toDto(ToolEnum e) {
        return e == null ? null : BuiltInToolItemDTO.ToolEnum.valueOf(e.name());
    }
    default ToolEnum toModel(BuiltInToolItemDTO.ToolEnum e) {
        return e == null ? null : ToolEnum.valueOf(e.name());
    }

    default PriceUnitDTO toDto(PriceUnitEnum e) {
        return e == null ? null : PriceUnitDTO.valueOf(e.name());
    }
    default PriceUnitEnum toModel(PriceUnitDTO e) {
        return e == null ? null : PriceUnitEnum.valueOf(e.name());
    }

    default ImageMatrixItemDTO.QualityEnum toDto(ImageQualityEnum e) {
        return e == null ? null : ImageMatrixItemDTO.QualityEnum.valueOf(e.name());
    }
    default ImageQualityEnum toModel(ImageMatrixItemDTO.QualityEnum e) {
        return e == null ? null : ImageQualityEnum.valueOf(e.name());
    }

    default ImageMatrixItemDTO.SizeEnum toDto(ImageSizeEnum e) {
        return e == null ? null : ImageMatrixItemDTO.SizeEnum.valueOf(e.name());
    }
    default ImageSizeEnum toModel(ImageMatrixItemDTO.SizeEnum e) {
        return e == null ? null : ImageSizeEnum.valueOf(e.name());
    }

    // (si usas enums de provider/tier en el PriceCatalog raíz)
    default PriceCatalogDTO.ProviderEnum toDto(ProviderEnum e) {
        return e == null ? null : PriceCatalogDTO.ProviderEnum.valueOf(e.name());
    }
    default ProviderEnum toModel(PriceCatalogDTO.ProviderEnum e) {
        return e == null ? null : ProviderEnum.valueOf(e.name());
    }
    default PriceCatalogDTO.TierEnum toDto(TierEnum e) {
        return e == null ? null : PriceCatalogDTO.TierEnum.valueOf(e.name());
    }
    default TierEnum toModel(PriceCatalogDTO.TierEnum e) {
        return e == null ? null : TierEnum.valueOf(e.name());
    }

    // =========================================================================
    // Tipos simples
    // =========================================================================

    // Pricing (dominio) <-> BuiltInToolItemPricingDTO (DTO)
    default BuiltInToolItemPricingDTO toDto(Pricing p) {
        if (p == null) return null;
        BuiltInToolItemPricingDTO dto = new BuiltInToolItemPricingDTO();
        dto.setUnit(toDto(p.getUnit()));
        dto.setAmount(p.getAmount());
        dto.setNotes(p.getNotes());
        return dto;
    }
    default Pricing toModel(BuiltInToolItemPricingDTO dto) {
        if (dto == null) return null;
        return Pricing.builder()
                .unit(toModel(dto.getUnit()))
                .amount(dto.getAmount())
                .notes(dto.getNotes())
                .build();
    }

    // PriceTriple
    default PriceTripleDTO toDto(PriceTriple p) {
        if (p == null) return null;
        PriceTripleDTO dto = new PriceTripleDTO();
        dto.setInput(p.getInput());
        dto.setCachedInput(p.getCachedInput());
        dto.setOutput(p.getOutput());
        return dto;
    }
    default PriceTriple toModel(PriceTripleDTO dto) {
        if (dto == null) return null;
        return PriceTriple.builder()
                .input(dto.getInput())
                .cachedInput(dto.getCachedInput())
                .output(dto.getOutput())
                .build();
    }

    // EmbeddingPrice (dominio) <-> EmbeddingItemPriceDTO (DTO)
    default EmbeddingItemPriceDTO toDto(EmbeddingPrice p) {
        if (p == null) return null;
        EmbeddingItemPriceDTO dto = new EmbeddingItemPriceDTO();
        dto.setInput(p.getInput());
        return dto;
    }
    default EmbeddingPrice toModel(EmbeddingItemPriceDTO dto) {
        if (dto == null) return null;
        return EmbeddingPrice.builder()
                .input(dto.getInput())
                .build();
    }

    // =========================================================================
    // Items
    // =========================================================================

    // TextModelPrice
    default TextModelPriceDTO toDto(TextModelPrice m) {
        if (m == null) return null;
        TextModelPriceDTO dto = new TextModelPriceDTO();
        dto.setModelId(m.getModelId());
        dto.setPrice(toDto(m.getPrice()));
        return dto;
    }
    default TextModelPrice toModel(TextModelPriceDTO dto) {
        if (dto == null) return null;
        return TextModelPrice.builder()
                .modelId(dto.getModelId())
                .price(toModel(dto.getPrice()))
                .build();
    }

    // TokensCategory
    default TokensCategoryDTO toDto(TokensCategory m) {
        if (m == null) return null;
        TokensCategoryDTO dto = new TokensCategoryDTO();
        dto.setUnit(toDto(m.getUnit()));
        List<TextModelPriceDTO> items = new ArrayList<>();
        if (m.getItems() != null) for (TextModelPrice it : m.getItems()) items.add(toDto(it));
        dto.setItems(items);
        return dto;
    }
    default TokensCategory toModel(TokensCategoryDTO dto) {
        if (dto == null) return null;
        List<TextModelPrice> items = new ArrayList<>();
        if (dto.getItems() != null) for (TextModelPriceDTO it : dto.getItems()) items.add(toModel(it));
        return TokensCategory.builder()
                .unit(toModel(dto.getUnit()))
                .items(items)
                .build();
    }

    // BuiltInToolItem
    default BuiltInToolItemDTO toDto(BuiltInToolItem m) {
        if (m == null) return null;
        BuiltInToolItemDTO dto = new BuiltInToolItemDTO();
        dto.setTool(toDto(m.getTool()));
        dto.setPricing(toDto(m.getPricing()));
        return dto;
    }
    default BuiltInToolItem toModel(BuiltInToolItemDTO dto) {
        if (dto == null) return null;
        return BuiltInToolItem.builder()
                .tool(toModel(dto.getTool()))
                .pricing(toModel(dto.getPricing()))
                .build();
    }

    // Transcription other unit
    default TranscriptionSpeechItemOtherUnitsInnerDTO toDto(TranscriptionOtherUnit u) {
        if (u == null) return null;
        TranscriptionSpeechItemOtherUnitsInnerDTO dto = new TranscriptionSpeechItemOtherUnitsInnerDTO();
        dto.setUnit(toDto(u.getUnit()));
        dto.setAmount(u.getAmount());
        return dto;
    }
    default TranscriptionOtherUnit toModel(TranscriptionSpeechItemOtherUnitsInnerDTO dto) {
        if (dto == null) return null;
        return TranscriptionOtherUnit.builder()
                .unit(toModel(dto.getUnit()))
                .amount(dto.getAmount())
                .build();
    }

    // Transcription speech item
    default TranscriptionSpeechItemDTO toDto(TranscriptionSpeechItem m) {
        if (m == null) return null;
        TranscriptionSpeechItemDTO dto = new TranscriptionSpeechItemDTO();
        dto.setModelId(m.getModelId());
        dto.setTextTokens(toDto(m.getTextTokens()));
        dto.setAudioTokens(toDto(m.getAudioTokens()));
        List<TranscriptionSpeechItemOtherUnitsInnerDTO> others = new ArrayList<>();
        if (m.getOtherUnits() != null) for (TranscriptionOtherUnit ou : m.getOtherUnits()) others.add(toDto(ou));
        dto.setOtherUnits(others);
        dto.setEstimatedCostPerMinuteUSD(m.getEstimatedCostPerMinuteUSD());
        return dto;
    }
    default TranscriptionSpeechItem toModel(TranscriptionSpeechItemDTO dto) {
        if (dto == null) return null;
        List<TranscriptionOtherUnit> others = new ArrayList<>();
        if (dto.getOtherUnits() != null) for (TranscriptionSpeechItemOtherUnitsInnerDTO ou : dto.getOtherUnits()) others.add(toModel(ou));
        return TranscriptionSpeechItem.builder()
                .modelId(dto.getModelId())
                .textTokens(toModel(dto.getTextTokens()))
                .audioTokens(toModel(dto.getAudioTokens()))
                .otherUnits(others)
                .estimatedCostPerMinuteUSD(dto.getEstimatedCostPerMinuteUSD())
                .build();
    }

    // Image matrix item
    default ImageMatrixItemDTO toDto(ImageMatrixItem m) {
        if (m == null) return null;
        ImageMatrixItemDTO dto = new ImageMatrixItemDTO();
        dto.setQuality(toDto(m.getQuality()));
        dto.setSize(toDto(m.getSize()));
        dto.setAmount(m.getAmount());
        return dto;
    }
    default ImageMatrixItem toModel(ImageMatrixItemDTO dto) {
        if (dto == null) return null;
        return ImageMatrixItem.builder()
                .quality(toModel(dto.getQuality()))
                .size(toModel(dto.getSize()))
                .amount(dto.getAmount())
                .build();
    }

    // Image generation item
    default ImageGenerationItemDTO toDto(ImageGenerationItem m) {
        if (m == null) return null;
        ImageGenerationItemDTO dto = new ImageGenerationItemDTO();
        dto.setModelId(m.getModelId());
        List<ImageMatrixItemDTO> list = new ArrayList<>();
        if (m.getMatrix() != null) for (ImageMatrixItem it : m.getMatrix()) list.add(toDto(it));
        dto.setMatrix(list);
        return dto;
    }
    default ImageGenerationItem toModel(ImageGenerationItemDTO dto) {
        if (dto == null) return null;
        List<ImageMatrixItem> list = new ArrayList<>();
        if (dto.getMatrix() != null) for (ImageMatrixItemDTO it : dto.getMatrix()) list.add(toModel(it));
        return ImageGenerationItem.builder()
                .modelId(dto.getModelId())
                .matrix(list)
                .build();
    }

    // Embedding item
    default EmbeddingItemDTO toDto(EmbeddingItem m) {
        if (m == null) return null;
        EmbeddingItemDTO dto = new EmbeddingItemDTO();
        dto.setModelId(m.getModelId());
        dto.setPrice(toDto(m.getPrice())); // EmbeddingPrice -> EmbeddingItemPriceDTO
        return dto;
    }
    default EmbeddingItem toModel(EmbeddingItemDTO dto) {
        if (dto == null) return null;
        return EmbeddingItem.builder()
                .modelId(dto.getModelId())
                .price(toModel(dto.getPrice()))
                .build();
    }

    // Fine-tuning
    default FineTuningItemDTO toDto(FineTuningItem m) {
        if (m == null) return null;
        FineTuningItemDTO dto = new FineTuningItemDTO();
        dto.setModelId(m.getModelId());
        dto.setTrainingHourUSD(m.getTrainingHourUSD());
        dto.setTrainingPer1MTokensUSD(m.getTrainingPer1MTokensUSD());
        dto.setInference(toDto(m.getInference()));
        return dto;
    }
    default FineTuningItem toModel(FineTuningItemDTO dto) {
        if (dto == null) return null;
        return FineTuningItem.builder()
                .modelId(dto.getModelId())
                .trainingHourUSD(dto.getTrainingHourUSD())
                .trainingPer1MTokensUSD(dto.getTrainingPer1MTokensUSD())
                .inference(toModel(dto.getInference()))
                .build();
    }

    // =========================================================================
    // Categorías
    // =========================================================================

    // Embeddings category
    default PriceCatalogCategoriesEmbeddingsDTO toDto(PriceCatalogEmbeddingsCategory m) {
        if (m == null) return null;
        PriceCatalogCategoriesEmbeddingsDTO dto = new PriceCatalogCategoriesEmbeddingsDTO();
        dto.setUnit(toDto(m.getUnit()));
        List<EmbeddingItemDTO> items = new ArrayList<>();
        if (m.getItems() != null) for (EmbeddingItem it : m.getItems()) items.add(toDto(it));
        dto.setItems(items);
        return dto;
    }
    default PriceCatalogEmbeddingsCategory toModel(PriceCatalogCategoriesEmbeddingsDTO dto) {
        if (dto == null) return null;
        List<EmbeddingItem> items = new ArrayList<>();
        if (dto.getItems() != null) for (EmbeddingItemDTO it : dto.getItems()) items.add(toModel(it));
        return PriceCatalogEmbeddingsCategory.builder()
                .unit(toModel(dto.getUnit()))
                .items(items)
                .build();
    }

    // Image generation category
    default PriceCatalogCategoriesImageGenerationDTO toDto(PriceCatalogImageGenerationCategory m) {
        if (m == null) return null;
        PriceCatalogCategoriesImageGenerationDTO dto = new PriceCatalogCategoriesImageGenerationDTO();
        dto.setUnit(toDto(m.getUnit()));
        List<ImageGenerationItemDTO> items = new ArrayList<>();
        if (m.getItems() != null) for (ImageGenerationItem it : m.getItems()) items.add(toDto(it));
        dto.setItems(items);
        return dto;
    }
    default PriceCatalogImageGenerationCategory toModel(PriceCatalogCategoriesImageGenerationDTO dto) {
        if (dto == null) return null;
        List<ImageGenerationItem> items = new ArrayList<>();
        if (dto.getItems() != null) for (ImageGenerationItemDTO it : dto.getItems()) items.add(toModel(it));
        return PriceCatalogImageGenerationCategory.builder()
                .unit(toModel(dto.getUnit()))
                .items(items)
                .build();
    }

    // Moderation category (ojo con getFree/setFree en el DTO)
    default PriceCatalogCategoriesModerationDTO toDto(PriceCatalogModerationCategory m) {
        if (m == null) return null;
        PriceCatalogCategoriesModerationDTO dto = new PriceCatalogCategoriesModerationDTO();
        dto.setFree(m.isFree());     // acepta Boolean; autoboxing
        dto.setNotes(m.getNotes());
        return dto;
    }
    default PriceCatalogModerationCategory toModel(PriceCatalogCategoriesModerationDTO dto) {
        if (dto == null) return null;
        return PriceCatalogModerationCategory.builder()
                .free(Boolean.TRUE.equals(dto.getFree()))  // << getFree(), NO isFree()
                .notes(dto.getNotes())
                .build();
    }

    // PriceCatalogCategories (raíz de categorías) con WRAPPERS para items
    default PriceCatalogCategoriesDTO toDto(PriceCatalogCategories m) {
        if (m == null) return null;
        PriceCatalogCategoriesDTO dto = new PriceCatalogCategoriesDTO();

        dto.setTextTokens(toDto(m.getTextTokens()));
        dto.setImageTokens(toDto(m.getImageTokens()));
        dto.setAudioTokens(toDto(m.getAudioTokens()));
        dto.setLegacyTextModels(toDto(m.getLegacyTextModels()));

        // Fine tuning wrapper
        PriceCatalogCategoriesFineTuningDTO ftWrapper = new PriceCatalogCategoriesFineTuningDTO();
        List<FineTuningItemDTO> ftItems = new ArrayList<>();
        if (m.getFineTuning() != null) for (FineTuningItem it : m.getFineTuning()) ftItems.add(toDto(it));
        ftWrapper.setItems(ftItems);
        dto.setFineTuning(ftWrapper);

        // Built-in tools wrapper
        PriceCatalogCategoriesBuiltInToolsDTO biWrapper = new PriceCatalogCategoriesBuiltInToolsDTO();
        List<BuiltInToolItemDTO> biItems = new ArrayList<>();
        if (m.getBuiltInTools() != null) for (BuiltInToolItem it : m.getBuiltInTools()) biItems.add(toDto(it));
        biWrapper.setItems(biItems);
        dto.setBuiltInTools(biWrapper);

        // Transcription/speech wrapper
        PriceCatalogCategoriesTranscriptionSpeechDTO tsWrapper = new PriceCatalogCategoriesTranscriptionSpeechDTO();
        List<TranscriptionSpeechItemDTO> tsItems = new ArrayList<>();
        if (m.getTranscriptionSpeech() != null) for (TranscriptionSpeechItem it : m.getTranscriptionSpeech()) tsItems.add(toDto(it));
        tsWrapper.setItems(tsItems);
        dto.setTranscriptionSpeech(tsWrapper);

        // Image generation / embeddings / moderation
        dto.setImageGeneration(toDto(m.getImageGeneration()));
        dto.setEmbeddings(toDto(m.getEmbeddings()));
        dto.setModeration(toDto(m.getModeration()));

        return dto;
    }

    default PriceCatalogCategories toModel(PriceCatalogCategoriesDTO dto) {
        if (dto == null) return null;

        // Fine tuning
        List<FineTuningItem> ft = new ArrayList<>();
        if (dto.getFineTuning() != null && dto.getFineTuning().getItems() != null) {
            for (FineTuningItemDTO it : dto.getFineTuning().getItems()) ft.add(toModel(it));
        }

        // Built-in tools
        List<BuiltInToolItem> bi = new ArrayList<>();
        if (dto.getBuiltInTools() != null && dto.getBuiltInTools().getItems() != null) {
            for (BuiltInToolItemDTO it : dto.getBuiltInTools().getItems()) bi.add(toModel(it));
        }

        // Transcription/speech
        List<TranscriptionSpeechItem> ts = new ArrayList<>();
        if (dto.getTranscriptionSpeech() != null && dto.getTranscriptionSpeech().getItems() != null) {
            for (TranscriptionSpeechItemDTO it : dto.getTranscriptionSpeech().getItems()) ts.add(toModel(it));
        }

        return PriceCatalogCategories.builder()
                .textTokens(toModel(dto.getTextTokens()))
                .imageTokens(toModel(dto.getImageTokens()))
                .audioTokens(toModel(dto.getAudioTokens()))
                .fineTuning(ft)
                .builtInTools(bi)
                .transcriptionSpeech(ts)
                .imageGeneration(toModel(dto.getImageGeneration()))
                .embeddings(toModel(dto.getEmbeddings()))
                .moderation(toModel(dto.getModeration()))
                .legacyTextModels(toModel(dto.getLegacyTextModels()))
                .build();
    }

    // =========================================================================
    // Raíz PriceCatalog (sin 'notes')
    // =========================================================================

    default PriceCatalogDTO toDto(PriceCatalog m) {
        if (m == null) return null;
        PriceCatalogDTO dto = new PriceCatalogDTO();
        dto.setProvider(toDto(m.getProvider()));
        dto.setTier(toDto(m.getTier()));
        dto.setCurrency(m.getCurrency());
        dto.setEffectiveAt(m.getEffectiveAt());
        dto.setCategories(toDto(m.getCategories()));
        return dto;
    }

    default PriceCatalog toModel(PriceCatalogDTO dto) {
        if (dto == null) return null;
        return PriceCatalog.builder()
                .provider(toModel(dto.getProvider()))
                .tier(toModel(dto.getTier()))
                .currency(dto.getCurrency())
                .effectiveAt(dto.getEffectiveAt())
                .categories(toModel(dto.getCategories()))
                .build();
    }

    // Conveniencia para GET /billing/prices -> { data: PriceCatalog }
    default GetBillingPrices200ResponseDTO toDtoResponse(PriceCatalog catalog) {
        if (catalog == null) return null;
        GetBillingPrices200ResponseDTO out = new GetBillingPrices200ResponseDTO();
        out.setData(toDto(catalog));
        return out;
    }
}

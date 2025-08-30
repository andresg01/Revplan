package com.revplan.llm.infra.api.rest.mapper;

import org.mapstruct.Mapper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

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
    // Helpers para enums con nombres “especiales” del codegen
    // =========================================================================

    // ---- PriceUnit: dominio -> DTO ----
    private PriceUnitDTO mapPriceUnit(PriceUnitEnum src) {
        if (src == null) return null;

        // OpenAPI codegen suele insertar _ antes de M/K: 1M -> 1_M, 1K -> 1_K, 2K -> 2_K
        String candidate = src.name()
                .replace("1M", "1_M")
                .replace("2K", "2_K")
                .replace("1K", "1_K");

        // Intento directo por name()
        try {
            return PriceUnitDTO.valueOf(candidate);
        } catch (IllegalArgumentException ignore) {
            // Fallback: usar fromValue(String) si existe (suele aceptar "per_1m_tokens", etc.)
            String jsonLike = src.name().toLowerCase(Locale.ROOT);
            jsonLike = jsonLike
                    .replace("__", "_") // por si acaso
                    .replace("1m", "1m")
                    .replace("2k", "2k")
                    .replace("1k", "1k");
            PriceUnitDTO viaFromValue = invokeEnumFromValue(PriceUnitDTO.class, jsonLike);
            if (viaFromValue != null) return viaFromValue;

            // Último intento: probar también con nombre transformado a lower + guiones bajos
            String lowerName = candidate.toLowerCase(Locale.ROOT);
            viaFromValue = invokeEnumFromValue(PriceUnitDTO.class, lowerName);
            if (viaFromValue != null) return viaFromValue;

            throw new IllegalArgumentException(
                    "No se pudo mapear PriceUnitEnum '" + src.name() + "' -> PriceUnitDTO. Intenté '" +
                            candidate + "', '" + jsonLike + "' y '" + lowerName + "'. Revisa los constantes del DTO."
            );
        }
    }

    // ---- PriceUnit: DTO -> dominio ----
    private PriceUnitEnum mapPriceUnit(PriceUnitDTO src) {
        if (src == null) return null;

        // Intento directo por name()
        try {
            return PriceUnitEnum.valueOf(src.name()
                    .replace("1_M", "1M")
                    .replace("2_K", "2K")
                    .replace("1_K", "1K"));
        } catch (IllegalArgumentException ignore) {
            // Si el codegen trae value distinto del name, intentamos con fromValue inverso:
            // Usamos value() si existe; si no, probamos con name() en minúsculas como aproximación.
            String valueLike = invokeEnumValueAccessor(src);
            if (valueLike == null || valueLike.isBlank()) valueLike = src.name().toLowerCase(Locale.ROOT);

            // Normalizamos a forma del dominio:
            // per_1m_tokens -> PER_1M_TOKENS; per_gb_day -> PER_GB_DAY, etc.
            String candidate = valueLike.toUpperCase(Locale.ROOT)
                    .replace("PER_1M_", "PER_1M_")
                    .replace("PER_2K_", "PER_2K_")
                    .replace("PER_1K_", "PER_1K_");

            try {
                return PriceUnitEnum.valueOf(candidate);
            } catch (IllegalArgumentException ex) {
                // Segundo intento: por si el name() ya traía 1_M/1_K/2_K:
                String byName = src.name()
                        .replace("1_M", "1M")
                        .replace("2_K", "2K")
                        .replace("1_K", "1K");
                return PriceUnitEnum.valueOf(byName);
            }
        }
    }

    // ---- ImageSize: dominio -> DTO ----
    private ImageMatrixItemDTO.SizeEnum mapSizeToDto(ImageSizeEnum src) {
        if (src == null) return null;

        // 1) Intento directo por name() (p.ej. _1024X1024)
        try {
            return ImageMatrixItemDTO.SizeEnum.valueOf(src.name());
        } catch (IllegalArgumentException ignore) {
            // 2) Fallback: muchos DTOs llevan fromValue("1024x1024")
            String raw = src.name();
            // Dom: _1024X1024 -> "1024x1024"
            if (raw.startsWith("_")) raw = raw.substring(1);
            raw = raw.replace('X', 'x');
            ImageMatrixItemDTO.SizeEnum viaFromValue =
                    invokeEnumFromValue(ImageMatrixItemDTO.SizeEnum.class, raw);
            if (viaFromValue != null) return viaFromValue;

            throw new IllegalArgumentException(
                    "No se pudo mapear ImageSizeEnum '" + src.name() + "' -> ImageMatrixItemDTO.SizeEnum"
            );
        }
    }

    // ---- ImageSize: DTO -> dominio ----
    private ImageSizeEnum mapSizeToModel(ImageMatrixItemDTO.SizeEnum src) {
        if (src == null) return null;

        // 1) Intento directo por name()
        try {
            return ImageSizeEnum.valueOf(src.name());
        } catch (IllegalArgumentException ignore) {
            // 2) Invertimos fromValue si existe y nos da "1024x1024"
            String valueLike = invokeEnumValueAccessor(src);
            if (valueLike == null || valueLike.isBlank()) {
                // como alternativa, usamos name() -> intentamos quitar prefijos y normalizar
                valueLike = src.name();
            }
            String dom = valueLike.toUpperCase(Locale.ROOT).replace('X', 'X');
            // "1024x1024" -> "_1024X1024"
            dom = "_" + dom.replace('x', 'X');
            return ImageSizeEnum.valueOf(dom);
        }
    }

    // ---- ImageQuality: añadimos fallback por fromValue por si el codegen lo usa ----
    private ImageMatrixItemDTO.QualityEnum mapQualityToDto(ImageQualityEnum src) {
        if (src == null) return null;
        try {
            return ImageMatrixItemDTO.QualityEnum.valueOf(src.name());
        } catch (IllegalArgumentException ignore) {
            ImageMatrixItemDTO.QualityEnum viaFromValue =
                    invokeEnumFromValue(ImageMatrixItemDTO.QualityEnum.class, src.name().toLowerCase(Locale.ROOT));
            if (viaFromValue != null) return viaFromValue;
            throw new IllegalArgumentException(
                    "No se pudo mapear ImageQualityEnum '" + src.name() + "' -> ImageMatrixItemDTO.QualityEnum"
            );
        }
    }

    private ImageQualityEnum mapQualityToModel(ImageMatrixItemDTO.QualityEnum src) {
        if (src == null) return null;
        try {
            return ImageQualityEnum.valueOf(src.name());
        } catch (IllegalArgumentException ignore) {
            String valueLike = invokeEnumValueAccessor(src);
            if (valueLike == null || valueLike.isBlank()) valueLike = src.name().toLowerCase(Locale.ROOT);
            return ImageQualityEnum.valueOf(valueLike.toUpperCase(Locale.ROOT));
        }
    }

    // ---- util: invocar fromValue(String) estático si existe en el enum del DTO ----
    private static <E extends Enum<E>> E invokeEnumFromValue(Class<E> enumClass, String value) {
        try {
            Method m = enumClass.getDeclaredMethod("fromValue", String.class);
            @SuppressWarnings("unchecked")
            E result = (E) m.invoke(null, value);
            return result;
        } catch (Exception ignore) {
            return null;
        }
    }

    // ---- util: obtener value() (o getValue()) de un enum DTO (OpenAPI codegen) ----
    private static String invokeEnumValueAccessor(Object enumInstance) {
        try {
            Method m = enumInstance.getClass().getDeclaredMethod("getValue");
            Object v = m.invoke(enumInstance);
            return v == null ? null : String.valueOf(v);
        } catch (Exception ignore) {
            try {
                Method m = enumInstance.getClass().getDeclaredMethod("value");
                Object v = m.invoke(enumInstance);
                return v == null ? null : String.valueOf(v);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    // =========================================================================
    // Enums (expuestos a MapStruct)
    // =========================================================================
    default BuiltInToolItemDTO.ToolEnum toDto(ToolEnum e) {
        return e == null ? null : BuiltInToolItemDTO.ToolEnum.valueOf(e.name());
    }
    default ToolEnum toModel(BuiltInToolItemDTO.ToolEnum e) {
        return e == null ? null : ToolEnum.valueOf(e.name());
    }

    // *** FIX clave: usar helpers para PriceUnit ***
    default PriceUnitDTO toDto(PriceUnitEnum e) {
        return mapPriceUnit(e);
    }
    default PriceUnitEnum toModel(PriceUnitDTO e) {
        return mapPriceUnit(e);
    }

    // *** Añadimos helpers para Quality / Size ***
    default ImageMatrixItemDTO.QualityEnum toDto(ImageQualityEnum e) {
        return mapQualityToDto(e);
    }
    default ImageQualityEnum toModel(ImageMatrixItemDTO.QualityEnum e) {
        return mapQualityToModel(e);
    }

    default ImageMatrixItemDTO.SizeEnum toDto(ImageSizeEnum e) {
        return mapSizeToDto(e);
    }
    default ImageSizeEnum toModel(ImageMatrixItemDTO.SizeEnum e) {
        return mapSizeToModel(e);
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
        var items = new ArrayList<TextModelPriceDTO>();
        if (m.getItems() != null) for (TextModelPrice it : m.getItems()) items.add(toDto(it));
        dto.setItems(items);
        return dto;
    }
    default TokensCategory toModel(TokensCategoryDTO dto) {
        if (dto == null) return null;
        var items = new ArrayList<TextModelPrice>();
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
        var others = new ArrayList<TranscriptionSpeechItemOtherUnitsInnerDTO>();
        if (m.getOtherUnits() != null) for (TranscriptionOtherUnit ou : m.getOtherUnits()) others.add(toDto(ou));
        dto.setOtherUnits(others);
        dto.setEstimatedCostPerMinuteUSD(m.getEstimatedCostPerMinuteUSD());
        return dto;
    }
    default TranscriptionSpeechItem toModel(TranscriptionSpeechItemDTO dto) {
        if (dto == null) return null;
        var others = new ArrayList<TranscriptionOtherUnit>();
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
        var list = new ArrayList<ImageMatrixItemDTO>();
        if (m.getMatrix() != null) for (ImageMatrixItem it : m.getMatrix()) list.add(toDto(it));
        dto.setMatrix(list);
        return dto;
    }
    default ImageGenerationItem toModel(ImageGenerationItemDTO dto) {
        if (dto == null) return null;
        var list = new ArrayList<ImageMatrixItem>();
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
        dto.setPrice(toDto(m.getPrice()));
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
        var items = new ArrayList<EmbeddingItemDTO>();
        if (m.getItems() != null) for (EmbeddingItem it : m.getItems()) items.add(toDto(it));
        dto.setItems(items);
        return dto;
    }
    default PriceCatalogEmbeddingsCategory toModel(PriceCatalogCategoriesEmbeddingsDTO dto) {
        if (dto == null) return null;
        var items = new ArrayList<EmbeddingItem>();
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
        var items = new ArrayList<ImageGenerationItemDTO>();
        if (m.getItems() != null) for (ImageGenerationItem it : m.getItems()) items.add(toDto(it));
        dto.setItems(items);
        return dto;
    }
    default PriceCatalogImageGenerationCategory toModel(PriceCatalogCategoriesImageGenerationDTO dto) {
        if (dto == null) return null;
        var items = new ArrayList<ImageGenerationItem>();
        if (dto.getItems() != null) for (ImageGenerationItemDTO it : dto.getItems()) items.add(toModel(it));
        return PriceCatalogImageGenerationCategory.builder()
                .unit(toModel(dto.getUnit()))
                .items(items)
                .build();
    }

    // Moderation category
    default PriceCatalogCategoriesModerationDTO toDto(PriceCatalogModerationCategory m) {
        if (m == null) return null;
        PriceCatalogCategoriesModerationDTO dto = new PriceCatalogCategoriesModerationDTO();
        dto.setFree(m.isFree());
        dto.setNotes(m.getNotes());
        return dto;
    }
    default PriceCatalogModerationCategory toModel(PriceCatalogCategoriesModerationDTO dto) {
        if (dto == null) return null;
        return PriceCatalogModerationCategory.builder()
                .free(Boolean.TRUE.equals(dto.getFree()))
                .notes(dto.getNotes())
                .build();
    }

    // PriceCatalogCategories (raíz de categorías)
    default PriceCatalogCategoriesDTO toDto(PriceCatalogCategories m) {
        if (m == null) return null;
        PriceCatalogCategoriesDTO dto = new PriceCatalogCategoriesDTO();

        dto.setTextTokens(toDto(m.getTextTokens()));
        dto.setImageTokens(toDto(m.getImageTokens()));
        dto.setAudioTokens(toDto(m.getAudioTokens()));
        dto.setLegacyTextModels(toDto(m.getLegacyTextModels()));

        // Fine tuning wrapper
        PriceCatalogCategoriesFineTuningDTO ftWrapper = new PriceCatalogCategoriesFineTuningDTO();
        var ftItems = new ArrayList<FineTuningItemDTO>();
        if (m.getFineTuning() != null) for (FineTuningItem it : m.getFineTuning()) ftItems.add(toDto(it));
        ftWrapper.setItems(ftItems);
        dto.setFineTuning(ftWrapper);

        // Built-in tools wrapper
        PriceCatalogCategoriesBuiltInToolsDTO biWrapper = new PriceCatalogCategoriesBuiltInToolsDTO();
        var biItems = new ArrayList<BuiltInToolItemDTO>();
        if (m.getBuiltInTools() != null) for (BuiltInToolItem it : m.getBuiltInTools()) biItems.add(toDto(it));
        biWrapper.setItems(biItems);
        dto.setBuiltInTools(biWrapper);

        // Transcription/speech wrapper
        PriceCatalogCategoriesTranscriptionSpeechDTO tsWrapper = new PriceCatalogCategoriesTranscriptionSpeechDTO();
        var tsItems = new ArrayList<TranscriptionSpeechItemDTO>();
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
        var ft = new ArrayList<FineTuningItem>();
        if (dto.getFineTuning() != null && dto.getFineTuning().getItems() != null) {
            for (FineTuningItemDTO it : dto.getFineTuning().getItems()) ft.add(toModel(it));
        }

        // Built-in tools
        var bi = new ArrayList<BuiltInToolItem>();
        if (dto.getBuiltInTools() != null && dto.getBuiltInTools().getItems() != null) {
            for (BuiltInToolItemDTO it : dto.getBuiltInTools().getItems()) bi.add(toModel(it));
        }

        // Transcription/speech
        var ts = new ArrayList<TranscriptionSpeechItem>();
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

package com.revplan.llm.infra.persistence.jpa.repository;

import com.revplan.llm.domain.enums.ImageQualityEnum;
import com.revplan.llm.domain.enums.ImageSizeEnum;
import com.revplan.llm.domain.enums.PriceUnitEnum;
import com.revplan.llm.domain.enums.ProviderEnum;
import com.revplan.llm.domain.enums.TierEnum;
import com.revplan.llm.domain.enums.ToolEnum;
import com.revplan.llm.domain.model.BuiltInToolItem;
import com.revplan.llm.domain.model.EmbeddingItem;
import com.revplan.llm.domain.model.EmbeddingPrice;
import com.revplan.llm.domain.model.FineTuningItem;
import com.revplan.llm.domain.model.ImageGenerationItem;
import com.revplan.llm.domain.model.ImageMatrixItem;
import com.revplan.llm.domain.model.PriceCatalog;
import com.revplan.llm.domain.model.PriceCatalogCategories;
import com.revplan.llm.domain.model.PriceCatalogEmbeddingsCategory;
import com.revplan.llm.domain.model.PriceCatalogImageGenerationCategory;
import com.revplan.llm.domain.model.PriceCatalogModerationCategory;
import com.revplan.llm.domain.model.PriceTriple;
import com.revplan.llm.domain.model.Pricing;
import com.revplan.llm.domain.model.TextModelPrice;
import com.revplan.llm.domain.model.TokensCategory;
import com.revplan.llm.domain.model.TranscriptionOtherUnit;
import com.revplan.llm.domain.model.TranscriptionSpeechItem;
import com.revplan.llm.domain.repository.BillingPricesRepository;
import com.revplan.llm.domain.service.FxRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class ProviderBillingPricesAdapter implements BillingPricesRepository {

    // Servicio de FX con caché (Frankfurter adapter + TTL configurado en el servicio)
    private final FxRateService fxRateService;

    /** Factor multiplicador por tier sobre todos los importes. */
    private static final Map<TierEnum, BigDecimal> TIER_FACTOR = Map.of(
            TierEnum.STANDARD, bd("1.00"),
            TierEnum.BATCH,    bd("0.85"),
            TierEnum.FLEX,     bd("0.95"),
            TierEnum.PRIORITY, bd("1.10")
    );

    /** Escala y redondeo usado al convertir / escalar importes. */
    private static final int MONEY_SCALE = 3;
    private static final RoundingMode MONEY_RND = RoundingMode.HALF_UP;

    @Override
    public PriceCatalog fetchCatalog(ProviderEnum provider,
                                     TierEnum tier,
                                     String currency,
                                     boolean includeLegacy,
                                     LocalDateTime effectiveAt) {

        // Normaliza proveedor/tier/moneda/fecha
        ProviderEnum prov = (provider == null ? ProviderEnum.OPENAI : provider);
        TierEnum tr = (tier == null ? TierEnum.STANDARD : tier);
        String curr = normalizeCurrency(currency);
        LocalDateTime eff = (effectiveAt == null ? LocalDateTime.now() : effectiveAt);

        // 1) Construye catálogo BASE en USD + STANDARD
        PriceCatalog baseUsd = buildBaseUsdCatalog(prov, TierEnum.STANDARD, includeLegacy, eff);

        // 2) Aplica multiplicador por TIER (en USD)
        BigDecimal tierFactor = TIER_FACTOR.getOrDefault(tr, bd("1.00"));
        applyFactorToCatalog(baseUsd, tierFactor);

        // 3) Convierte MONEDA desde USD a target con FX en vivo
        if (!"USD".equals(curr)) {
            BigDecimal fx = getFxSafe("USD", curr); // si falla → 1
            convertCatalogCurrency(baseUsd, curr, fx);
        }

        // 4) Ajusta metadatos raíz
        baseUsd.setProvider(prov);
        baseUsd.setTier(tr);
        baseUsd.setCurrency(curr);
        baseUsd.setEffectiveAt(eff);

        return baseUsd;
    }

    // ============================
    // Catálogo base (USD/Standard)
    // ============================

    private PriceCatalog buildBaseUsdCatalog(ProviderEnum provider,
                                             TierEnum tier,
                                             boolean includeLegacy,
                                             LocalDateTime effectiveAt) {

        // -------- TEXT TOKENS (PER_1M_TOKENS) --------
        TokensCategory textTokens = TokensCategory.builder()
                .unit(PriceUnitEnum.PER_1M_TOKENS)
                .items(new ArrayList<>())
                .build();

        textTokens.getItems().add(textPrice("gpt-5",                 bd("1.25"),  bd("0.125"), bd("10.00")));
        textTokens.getItems().add(textPrice("gpt-5-mini",            bd("0.25"),  bd("0.025"), bd("2.00")));
        textTokens.getItems().add(textPrice("gpt-5-nano",            bd("0.05"),  bd("0.005"), bd("0.40")));
        textTokens.getItems().add(textPrice("gpt-4.1",               bd("2.00"),  bd("0.50"),  bd("8.00")));
        textTokens.getItems().add(textPrice("gpt-4.1-mini",          bd("0.40"),  bd("0.10"),  bd("1.60")));
        textTokens.getItems().add(textPrice("gpt-4.1-nano",          bd("0.10"),  bd("0.025"), bd("0.40")));
        textTokens.getItems().add(textPrice("gpt-4o",                bd("2.50"),  bd("1.25"),  bd("10.00")));
        textTokens.getItems().add(textPrice("gpt-4o-mini",           bd("0.15"),  bd("0.075"), bd("0.60")));
        textTokens.getItems().add(textPrice("o1",                    bd("15.00"), bd("7.50"),  bd("60.00")));
        textTokens.getItems().add(textPrice("o3",                    bd("2.00"),  bd("0.50"),  bd("8.00")));
        textTokens.getItems().add(textPrice("o3-pro",                bd("20.00"), null,        bd("80.00")));
        textTokens.getItems().add(textPrice("o3-deep-research",      bd("10.00"), bd("2.50"),  bd("40.00")));
        textTokens.getItems().add(textPrice("o4-mini",               bd("1.10"),  bd("0.275"), bd("4.40")));
        textTokens.getItems().add(textPrice("o4-mini-deep-research", bd("2.00"),  bd("0.50"),  bd("8.00")));

        // preview sin cachedInput
        textTokens.getItems().add(textPrice("gpt-4o-2024-05-13",            bd("5.00"),  null,      bd("15.00")));
        textTokens.getItems().add(textPrice("gpt-4o-audio-preview",         bd("2.50"),  null,      bd("10.00")));
        textTokens.getItems().add(textPrice("gpt-4o-realtime-preview",      bd("5.00"),  bd("2.50"), bd("20.00")));
        textTokens.getItems().add(textPrice("gpt-4o-mini-audio-preview",    bd("0.15"),  null,      bd("0.60")));
        textTokens.getItems().add(textPrice("gpt-4o-mini-realtime-preview", bd("0.60"),  bd("0.30"), bd("2.40")));
        textTokens.getItems().add(textPrice("o1-pro",                       bd("150.00"), null,     bd("600.00")));

        // -------- AUDIO TOKENS (PER_1M_TOKENS) --------
        TokensCategory audioTokens = TokensCategory.builder()
                .unit(PriceUnitEnum.PER_1M_TOKENS)
                .items(new ArrayList<>())
                .build();

        audioTokens.getItems().add(textPrice("gpt-4o-audio-preview",         bd("40.00"), null,      bd("80.00")));
        audioTokens.getItems().add(textPrice("gpt-4o-mini-audio-preview",    bd("10.00"), null,      bd("20.00")));
        audioTokens.getItems().add(textPrice("gpt-4o-realtime-preview",      bd("40.00"), bd("2.50"), bd("80.00")));
        audioTokens.getItems().add(textPrice("gpt-4o-mini-realtime-preview", bd("10.00"), bd("0.30"), bd("20.00")));

        // -------- IMAGE TOKENS (PER_1M_TOKENS) --------
        TokensCategory imageTokens = TokensCategory.builder()
                .unit(PriceUnitEnum.PER_1M_TOKENS)
                .items(new ArrayList<>())
                .build();

        imageTokens.getItems().add(textPrice("gpt-image-1", bd("10.00"), bd("2.50"), bd("40.00")));

        // -------- EMBEDDINGS (PER_1M_TOKENS) --------
        PriceCatalogEmbeddingsCategory embeddings = PriceCatalogEmbeddingsCategory.builder()
                .unit(PriceUnitEnum.PER_1M_TOKENS)
                .items(new ArrayList<>())
                .build();
        embeddings.getItems().add(EmbeddingItem.builder()
                .modelId("text-embedding-3-small")
                .price(EmbeddingPrice.builder().input(bd("0.01")).build())
                .build());
        embeddings.getItems().add(EmbeddingItem.builder()
                .modelId("text-embedding-3-large")
                .price(EmbeddingPrice.builder().input(bd("0.065")).build())
                .build());
        embeddings.getItems().add(EmbeddingItem.builder()
                .modelId("text-embedding-ada-002")
                .price(EmbeddingPrice.builder().input(bd("0.05")).build())
                .build());

        // -------- BUILT-IN TOOLS --------
        List<BuiltInToolItem> builtInTools = new ArrayList<>();
        builtInTools.add(BuiltInToolItem.builder()
                .tool(ToolEnum.CODE_INTERPRETER)
                .pricing(Pricing.builder().unit(PriceUnitEnum.PER_CONTAINER).amount(bd("0.03")).build())
                .build());
        builtInTools.add(BuiltInToolItem.builder()
                .tool(ToolEnum.FILE_SEARCH_STORAGE)
                .pricing(Pricing.builder().unit(PriceUnitEnum.PER_GB_DAY).amount(bd("0.10")).notes("primer GB gratis").build())
                .build());
        builtInTools.add(BuiltInToolItem.builder()
                .tool(ToolEnum.FILE_SEARCH_TOOL_CALL)
                .pricing(Pricing.builder().unit(PriceUnitEnum.PER_2K_CALLS).amount(bd("2.50")).build())
                .build());
        builtInTools.add(BuiltInToolItem.builder()
                .tool(ToolEnum.WEB_SEARCH_GPT4O_FAMILY)
                .pricing(Pricing.builder().unit(PriceUnitEnum.PER_1K_CALLS).amount(bd("25.00")).notes("contenido de búsqueda incluido").build())
                .build());
        builtInTools.add(BuiltInToolItem.builder()
                .tool(ToolEnum.WEB_SEARCH_GPT5_O_SERIES)
                .pricing(Pricing.builder().unit(PriceUnitEnum.PER_1K_CALLS).amount(bd("10.00")).notes("contenido facturado al modelo").build())
                .build());

        // -------- TRANSCRIPTION / SPEECH --------
        List<TranscriptionSpeechItem> speech = new ArrayList<>();
        speech.add(TranscriptionSpeechItem.builder()
                .modelId("gpt-4o-mini-tts")
                .textTokens(PriceTriple.builder().input(bd("0.60")).output(bd("12.00")).build())
                .estimatedCostPerMinuteUSD(bd("0.015"))
                .build());
        speech.add(TranscriptionSpeechItem.builder()
                .modelId("gpt-4o-transcribe")
                .textTokens(PriceTriple.builder().input(bd("2.50")).output(bd("10.00")).build())
                .audioTokens(PriceTriple.builder().input(bd("6.00")).build())
                .estimatedCostPerMinuteUSD(bd("0.006"))
                .build());
        speech.add(TranscriptionSpeechItem.builder()
                .modelId("gpt-4o-mini-transcribe")
                .textTokens(PriceTriple.builder().input(bd("1.25")).output(bd("5.00")).build())
                .audioTokens(PriceTriple.builder().input(bd("3.00")).build())
                .estimatedCostPerMinuteUSD(bd("0.003"))
                .build());

        // whisper (por minuto)
        TranscriptionSpeechItem whisper = TranscriptionSpeechItem.builder()
                .modelId("whisper")
                .otherUnits(new ArrayList<>())
                .build();
        whisper.getOtherUnits().add(TranscriptionOtherUnit.builder()
                .unit(PriceUnitEnum.PER_MINUTE)
                .amount(bd("0.006"))
                .build());
        speech.add(whisper);

        // tts/tts-hd por caracteres
        TranscriptionSpeechItem tts = TranscriptionSpeechItem.builder()
                .modelId("tts")
                .otherUnits(new ArrayList<>())
                .build();
        tts.getOtherUnits().add(TranscriptionOtherUnit.builder()
                .unit(PriceUnitEnum.PER_1M_CHARACTERS)
                .amount(bd("15.00"))
                .build());
        speech.add(tts);

        TranscriptionSpeechItem ttsHd = TranscriptionSpeechItem.builder()
                .modelId("tts-hd")
                .otherUnits(new ArrayList<>())
                .build();
        ttsHd.getOtherUnits().add(TranscriptionOtherUnit.builder()
                .unit(PriceUnitEnum.PER_1M_CHARACTERS)
                .amount(bd("30.00"))
                .build());
        speech.add(ttsHd);

        // -------- FINE TUNING --------
        List<FineTuningItem> fine = new ArrayList<>();
        fine.add(FineTuningItem.builder()
                .modelId("o4-mini-2025-04-16")
                .trainingHourUSD(bd("100.00"))
                .inference(triple(bd("4.00"), bd("1.00"), bd("16.00")))
                .build());
        fine.add(FineTuningItem.builder()
                .modelId("gpt-4.1-2025-04-14")
                .trainingPer1MTokensUSD(bd("25.00"))
                .inference(triple(bd("3.00"), bd("0.75"), bd("12.00")))
                .build());
        fine.add(FineTuningItem.builder()
                .modelId("gpt-4.1-mini-2025-04-14")
                .trainingPer1MTokensUSD(bd("5.00"))
                .inference(triple(bd("0.80"), bd("0.20"), bd("3.20")))
                .build());
        fine.add(FineTuningItem.builder()
                .modelId("gpt-4.1-nano-2025-04-14")
                .trainingPer1MTokensUSD(bd("1.50"))
                .inference(triple(bd("0.20"), bd("0.05"), bd("0.80")))
                .build());

        // -------- IMAGE GENERATION (PER_IMAGE) --------
        PriceCatalogImageGenerationCategory imageGen = PriceCatalogImageGenerationCategory.builder()
                .unit(PriceUnitEnum.PER_IMAGE)
                .items(new ArrayList<>())
                .build();

        // gpt-image-1 (matrices de ejemplo)
        ImageGenerationItem gptImg = ImageGenerationItem.builder()
                .modelId("gpt-image-1")
                .matrix(new ArrayList<>())
                .build();
        gptImg.getMatrix().add(imageMatrix("low",     "1024x1024", bd("0.011")));
        gptImg.getMatrix().add(imageMatrix("medium",  "1024x1024", bd("0.042")));
        gptImg.getMatrix().add(imageMatrix("high",    "1024x1024", bd("0.167")));
        gptImg.getMatrix().add(imageMatrix("low",     "1024x1536", bd("0.016")));
        gptImg.getMatrix().add(imageMatrix("medium",  "1024x1536", bd("0.063")));
        gptImg.getMatrix().add(imageMatrix("high",    "1024x1536", bd("0.250")));
        gptImg.getMatrix().add(imageMatrix("low",     "1536x1024", bd("0.016")));
        imageGen.getItems().add(gptImg);

        // dalle-3
        ImageGenerationItem dalle3 = ImageGenerationItem.builder()
                .modelId("dalle-3")
                .matrix(new ArrayList<>())
                .build();
        dalle3.getMatrix().add(imageMatrix("standard", "1024x1024", bd("0.04")));
        dalle3.getMatrix().add(imageMatrix("hd",       "1024x1024", bd("0.08")));
        dalle3.getMatrix().add(imageMatrix("standard", "1024x1792", bd("0.08")));
        dalle3.getMatrix().add(imageMatrix("hd",       "1024x1792", bd("0.12")));
        dalle3.getMatrix().add(imageMatrix("standard", "1792x1024", bd("0.08")));
        dalle3.getMatrix().add(imageMatrix("hd",       "1792x1024", bd("0.12")));
        imageGen.getItems().add(dalle3);

        // dalle-2
        ImageGenerationItem dalle2 = ImageGenerationItem.builder()
                .modelId("dalle-2")
                .matrix(new ArrayList<>())
                .build();
        dalle2.getMatrix().add(imageMatrix("standard", "256x256",  bd("0.016")));
        dalle2.getMatrix().add(imageMatrix("standard", "512x512",  bd("0.018")));
        dalle2.getMatrix().add(imageMatrix("standard", "1024x1024",bd("0.020")));
        imageGen.getItems().add(dalle2);

        // -------- LEGACY (condicional) --------
        TokensCategory legacy = null;
        if (includeLegacy) {
            legacy = TokensCategory.builder()
                    .unit(PriceUnitEnum.PER_1M_TOKENS)
                    .items(new ArrayList<>())
                    .build();
            legacy.getItems().add(textPrice("chatgpt-4o-latest", bd("5.00"),  null, bd("15.00")));
            legacy.getItems().add(textPrice("gpt-4-0125-preview", bd("10.00"), null, bd("30.00")));
            legacy.getItems().add(textPrice("gpt-3.5-turbo",      bd("0.50"),  null, bd("1.50")));
            legacy.getItems().add(textPrice("davinci-002",        bd("2.00"),  null, bd("2.00")));
            legacy.getItems().add(textPrice("babbage-002",        bd("0.40"),  null, bd("0.40")));
        }

        // -------- MODERATION --------
        PriceCatalogModerationCategory moderation = PriceCatalogModerationCategory.builder()
                .free(true)
                .notes("omni-moderation sin coste")
                .build();

        // -------- MONTA RAÍZ --------
        PriceCatalogCategories cats = PriceCatalogCategories.builder()
                .textTokens(textTokens)
                .imageTokens(imageTokens)
                .audioTokens(audioTokens)
                .fineTuning(fine)
                .builtInTools(builtInTools)
                .transcriptionSpeech(speech)
                .imageGeneration(imageGen)
                .embeddings(embeddings)
                .moderation(moderation)
                .legacyTextModels(legacy)
                .build();

        return PriceCatalog.builder()
                .provider(provider)
                .tier(tier)
                .currency("USD")
                .effectiveAt(effectiveAt)
                .categories(cats)
                .build();
    }

    // ==================================
    // Helpers: creación / escalado / FX
    // ==================================

    private static TextModelPrice textPrice(String modelId, BigDecimal in, BigDecimal cachedIn, BigDecimal out) {
        return TextModelPrice.builder()
                .modelId(modelId)
                .price(triple(in, cachedIn, out))
                .build();
    }

    private static PriceTriple triple(BigDecimal in, BigDecimal cachedIn, BigDecimal out) {
        return PriceTriple.builder().input(in).cachedInput(cachedIn).output(out).build();
    }

    private static ImageMatrixItem imageMatrix(String quality, String size, BigDecimal amount) {
        ImageQualityEnum q = ImageQualityEnum.valueOf(quality.toUpperCase(Locale.ROOT));
        ImageSizeEnum s = mapImageSize(size);
        return ImageMatrixItem.builder()
                .quality(q)
                .size(s)
                .amount(amount)
                .build();
    }

    /** Mapea entradas tipo "1024x1024" a enum {@link ImageSizeEnum} como "_1024X1024". */
    private static ImageSizeEnum mapImageSize(String size) {
        if (size == null || size.isBlank()) {
            throw new IllegalArgumentException("Image size vacío");
        }
        String token = size.trim().toUpperCase(Locale.ROOT).replace('x', 'X');

        // 1) Intento directo (por si ya viene como "_1024X1024")
        try {
            return ImageSizeEnum.valueOf(token);
        } catch (IllegalArgumentException ignore) {
        }

        // 2) Intento con prefijo "_" (e.g. "1024x1024" -> "_1024X1024")
        String withUnderscore = token.startsWith("_") ? token : "_" + token;
        try {
            return ImageSizeEnum.valueOf(withUnderscore);
        } catch (IllegalArgumentException ignore) {
        }

        // 3) Fallback iterando (comparando sin el guion bajo inicial)
        for (ImageSizeEnum e : ImageSizeEnum.values()) {
            String name = e.name();
            String normalized = name.startsWith("_") ? name.substring(1) : name;
            if (normalized.equals(token)) {
                return e;
            }
        }

        // 4) No se pudo mapear
        throw new IllegalArgumentException("No enum constant " + ImageSizeEnum.class.getName() + " for input '" + size + "'");
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) return "USD";
        return currency.toUpperCase(Locale.ROOT);
    }

    // ----- Aplicar factor (tier) al catálogo -----

    private static void applyFactorToCatalog(PriceCatalog cat, BigDecimal factor) {
        if (cat == null || factor == null || bd("1.00").compareTo(factor) == 0) return;
        PriceCatalogCategories c = cat.getCategories();
        if (c == null) return;

        scaleTokens(c.getTextTokens(), factor);
        scaleTokens(c.getAudioTokens(), factor);
        scaleTokens(c.getImageTokens(), factor);

        if (c.getEmbeddings() != null) {
            for (EmbeddingItem e : c.getEmbeddings().getItems()) {
                if (e.getPrice() != null) {
                    e.getPrice().setInput(mul(e.getPrice().getInput(), factor));
                }
            }
        }

        if (c.getBuiltInTools() != null) {
            for (BuiltInToolItem b : c.getBuiltInTools()) {
                if (b.getPricing() != null) {
                    b.getPricing().setAmount(mul(b.getPricing().getAmount(), factor));
                }
            }
        }

        if (c.getTranscriptionSpeech() != null) {
            for (TranscriptionSpeechItem t : c.getTranscriptionSpeech()) {
                if (t.getTextTokens() != null) scaleTriple(t.getTextTokens(), factor);
                if (t.getAudioTokens() != null) scaleTriple(t.getAudioTokens(), factor);
                if (t.getEstimatedCostPerMinuteUSD() != null)
                    t.setEstimatedCostPerMinuteUSD(mul(t.getEstimatedCostPerMinuteUSD(), factor));
                if (t.getOtherUnits() != null) {
                    for (TranscriptionOtherUnit ou : t.getOtherUnits()) {
                        ou.setAmount(mul(ou.getAmount(), factor));
                    }
                }
            }
        }

        if (c.getFineTuning() != null) {
            for (FineTuningItem f : c.getFineTuning()) {
                if (f.getTrainingHourUSD() != null) f.setTrainingHourUSD(mul(f.getTrainingHourUSD(), factor));
                if (f.getTrainingPer1MTokensUSD() != null) f.setTrainingPer1MTokensUSD(mul(f.getTrainingPer1MTokensUSD(), factor));
                if (f.getInference() != null) scaleTriple(f.getInference(), factor);
            }
        }

        if (c.getImageGeneration() != null && c.getImageGeneration().getItems() != null) {
            for (ImageGenerationItem ig : c.getImageGeneration().getItems()) {
                if (ig.getMatrix() == null) continue;
                for (ImageMatrixItem mi : ig.getMatrix()) {
                    mi.setAmount(mul(mi.getAmount(), factor));
                }
            }
        }

        scaleTokens(c.getLegacyTextModels(), factor);
    }

    private static void scaleTokens(TokensCategory t, BigDecimal factor) {
        if (t == null || t.getItems() == null) return;
        for (TextModelPrice it : t.getItems()) {
            if (it.getPrice() != null) scaleTriple(it.getPrice(), factor);
        }
    }

    private static void scaleTriple(PriceTriple p, BigDecimal f) {
        if (p == null) return;
        p.setInput(mul(p.getInput(), f));
        p.setCachedInput(mul(p.getCachedInput(), f));
        p.setOutput(mul(p.getOutput(), f));
    }

    // ----- Convertir moneda (USD -> target) -----

    private static void convertCatalogCurrency(PriceCatalog cat, String targetCurrency, BigDecimal fx) {
        if (cat == null || fx == null || bd("1.00").compareTo(fx) == 0) {
            if (cat != null) cat.setCurrency(targetCurrency);
            return;
        }
        PriceCatalogCategories c = cat.getCategories();
        if (c == null) return;

        convertTokens(c.getTextTokens(), fx);
        convertTokens(c.getAudioTokens(), fx);
        convertTokens(c.getImageTokens(), fx);

        if (c.getEmbeddings() != null) {
            for (EmbeddingItem e : c.getEmbeddings().getItems()) {
                if (e.getPrice() != null) {
                    e.getPrice().setInput(mul(e.getPrice().getInput(), fx));
                }
            }
        }

        if (c.getBuiltInTools() != null) {
            for (BuiltInToolItem b : c.getBuiltInTools()) {
                if (b.getPricing() != null) {
                    b.getPricing().setAmount(mul(b.getPricing().getAmount(), fx));
                }
            }
        }

        if (c.getTranscriptionSpeech() != null) {
            for (TranscriptionSpeechItem t : c.getTranscriptionSpeech()) {
                if (t.getTextTokens() != null) convertTriple(t.getTextTokens(), fx);
                if (t.getAudioTokens() != null) convertTriple(t.getAudioTokens(), fx);
                if (t.getEstimatedCostPerMinuteUSD() != null)
                    t.setEstimatedCostPerMinuteUSD(mul(t.getEstimatedCostPerMinuteUSD(), fx));
                if (t.getOtherUnits() != null) {
                    for (TranscriptionOtherUnit ou : t.getOtherUnits()) {
                        ou.setAmount(mul(ou.getAmount(), fx));
                    }
                }
            }
        }

        if (c.getFineTuning() != null) {
            for (FineTuningItem f : c.getFineTuning()) {
                if (f.getTrainingHourUSD() != null) f.setTrainingHourUSD(mul(f.getTrainingHourUSD(), fx));
                if (f.getTrainingPer1MTokensUSD() != null) f.setTrainingPer1MTokensUSD(mul(f.getTrainingPer1MTokensUSD(), fx));
                if (f.getInference() != null) convertTriple(f.getInference(), fx);
            }
        }

        if (c.getImageGeneration() != null && c.getImageGeneration().getItems() != null) {
            for (ImageGenerationItem ig : c.getImageGeneration().getItems()) {
                if (ig.getMatrix() == null) continue;
                for (ImageMatrixItem mi : ig.getMatrix()) {
                    mi.setAmount(mul(mi.getAmount(), fx));
                }
            }
        }

        convertTokens(c.getLegacyTextModels(), fx);

        cat.setCurrency(targetCurrency);
    }

    private static void convertTokens(TokensCategory t, BigDecimal fx) {
        if (t == null || t.getItems() == null) return;
        for (TextModelPrice it : t.getItems()) {
            if (it.getPrice() != null) convertTriple(it.getPrice(), fx);
        }
    }

    private static void convertTriple(PriceTriple p, BigDecimal fx) {
        if (p == null) return;
        p.setInput(mul(p.getInput(), fx));
        p.setCachedInput(mul(p.getCachedInput(), fx));
        p.setOutput(mul(p.getOutput(), fx));
    }

    // ==================================
    // Utilidades numéricas y FX
    // ==================================

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    private static BigDecimal mul(BigDecimal v, BigDecimal f) {
        if (v == null || f == null) return v;
        return v.multiply(f).setScale(MONEY_SCALE, MONEY_RND);
    }

    /** Obtiene el tipo de cambio USD->target de forma segura (si falla, devuelve 1). */
    private BigDecimal getFxSafe(String from, String to) {
        try {
            BigDecimal rate = fxRateService.getRate(from, to);
            if (rate == null || BigDecimal.ZERO.compareTo(rate) >= 0) return BigDecimal.ONE;
            return rate;
        } catch (Exception ex) {
            return BigDecimal.ONE;
        }
    }
}

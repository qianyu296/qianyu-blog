package com.study.qianyu_blog.service;

import com.study.qianyu_blog.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiServiceTest {

    private final AiService aiService = new AiService(null);

    @Test
    void extractImageResultReturnsUrlWhenPresent() {
        String result = aiService.extractImageResult(Map.of(
                "data", List.of(Map.of("url", "https://example.com/image.png"))
        ));

        assertEquals("https://example.com/image.png", result);
    }

    @Test
    void extractImageResultWrapsBase64ImageDataAsDataUrl() {
        String result = aiService.extractImageResult(Map.of(
                "output_format", "png",
                "data", List.of(Map.of("b64_json", "abc123"))
        ));

        assertEquals("data:image/png;base64,abc123", result);
    }

    @Test
    void extractImageResultSupportsResponsesApiImageOutput() {
        String result = aiService.extractImageResult(Map.of(
                "output", List.of(Map.of(
                        "type", "image_generation_call",
                        "result", "xyz789"
                ))
        ));

        assertEquals("data:image/png;base64,xyz789", result);
    }

    @Test
    void extractImageResultThrowsWhenNoImagePayloadExists() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                aiService.extractImageResult(Map.of("data", List.of(Map.of("revised_prompt", "test"))))
        );

        assertEquals(40002, exception.getCode());
    }

    @Test
    void resolveImageSizeUsesRequestedSquare2kSize() {
        String size = aiService.resolveImageSize("生成头像", "gpt-image-1", "2048x2048");

        assertEquals("2048x2048", size);
    }

    @Test
    void resolveImageSizeUsesStandardSquarePresetForOneByOne() {
        String size = aiService.resolveImageSize("生成头像，1:1", "gpt-image-1", null);

        assertEquals("1024x1024", size);
    }

    @Test
    void resolveImageSizeUsesStandardLandscapePresetForThreeByTwo() {
        String size = aiService.resolveImageSize("横版电脑壁纸，3:2", "gpt-image-1", null);

        assertEquals("1536x1024", size);
    }

    @Test
    void resolveImageSizeUsesStandardPortraitPresetForTwoByThree() {
        String size = aiService.resolveImageSize("竖版手机壁纸，2:3", "gpt-image-1", null);

        assertEquals("1024x1536", size);
    }

    @Test
    void resolveImageSizeUsesWide2kPresetForSixteenByNine() {
        String size = aiService.resolveImageSize("视频封面，16:9", "gpt-image-1", null);

        assertEquals("2048x1152", size);
    }

    @Test
    void resolveImageSizeUsesTall2kPresetForNineBySixteen() {
        String size = aiService.resolveImageSize("抖音短视频封面，9:16", "gpt-image-1", null);

        assertEquals("1152x2048", size);
    }

    @Test
    void resolveImageSizeUses2kSquareWhenPromptAsksForHighQualitySquare() {
        String size = aiService.resolveImageSize("1:1 高清头像，印刷，高质量素材", "gpt-image-1", null);

        assertEquals("2048x2048", size);
    }

    @Test
    void resolveImageSizeUsesAutoForGptImageWhenNoPresetOrHintExists() {
        String size = aiService.resolveImageSize("生成一张抽象渐变色光影背景，柔和自然光", "gpt-image-1", null);

        assertEquals("auto", size);
    }
}

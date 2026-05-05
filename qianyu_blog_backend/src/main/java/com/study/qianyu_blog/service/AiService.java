package com.study.qianyu_blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.qianyu_blog.entity.AiSettings;
import com.study.qianyu_blog.exception.BusinessException;
import com.study.qianyu_blog.repository.AiSettingsRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final Pattern ASPECT_RATIO_PATTERN = Pattern.compile("(\\d{1,2})\\s*[:/]\\s*(\\d{1,2})");
    private static final Pattern MARKDOWN_CODE_FENCE_PATTERN = Pattern.compile("^```(?:markdown|md)?\\s*|\\s*```$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> SUPPORTED_IMAGE_SIZES = Set.of(
            "1024x1024",
            "1536x1024",
            "1024x1536",
            "2048x2048",
            "2048x1152",
            "1152x2048"
    );

    private final AiSettingsRepository aiSettingsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiService(AiSettingsRepository aiSettingsRepository) {
        this.aiSettingsRepository = aiSettingsRepository;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(180000);
        this.restTemplate = new RestTemplate(factory);
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String message) {
        AiSettings settings = aiSettingsRepository.findTopByEnabledTrue()
                .orElseThrow(() -> new BusinessException(40000, "AI功能未配置或已禁用"));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(settings.getChatApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", settings.getModelName());
            requestBody.put("stream", false);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", message)
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    settings.getApiEndpoint(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String body = response.getBody();
            int statusCode = response.getStatusCode().value();

            if (statusCode != 200) {
                throw new BusinessException(
                        statusCode,
                        "API返回了错误状态码: " + statusCode + "，响应: " + abbreviate(body)
                );
            }

            if (body == null || body.trim().startsWith("<")) {
                throw new BusinessException(40002, "API返回了非JSON响应，响应: " + abbreviate(body));
            }

            Map<String, Object> responseMap = objectMapper.readValue(body, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                return (String) messageObj.get("content");
            }
            return "未获取到有效回复";
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(40001, "AI调用失败: " + e.getMessage());
        }
    }

    public void chatStream(String message, OutputStream outputStream) throws Exception {
        AiSettings settings = aiSettingsRepository.findTopByEnabledTrue()
                .orElseThrow(() -> new BusinessException(40000, "AI功能未配置或已禁用"));

        URL url = new URL(settings.getApiEndpoint());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + settings.getChatApiKey());
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(300000);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", settings.getModelName());
        requestBody.put("stream", true);
        requestBody.put("max_tokens", settings.getMaxTokens() != null ? settings.getMaxTokens() : 4096);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", message)
        ));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(requestBody));
            os.flush();
        }

        int statusCode = conn.getResponseCode();
        if (statusCode != 200) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    errorBody.append(line);
                }
                throw new BusinessException(statusCode, "API返回了错误状态码: " + statusCode + "，响应: " + errorBody);
            }
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) {
                    continue;
                }

                String data = line.substring(6);
                if ("[DONE]".equals(data)) {
                    outputStream.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    break;
                }

                try {
                    Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                    if (choices == null || choices.isEmpty()) {
                        continue;
                    }

                    Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                    if (delta == null || delta.get("content") == null) {
                        continue;
                    }

                    String content = (String) delta.get("content");
                    String sseData = "data: " + objectMapper.writeValueAsString(Map.of("content", content)) + "\n\n";
                    outputStream.write(sseData.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public String polishPost(String title, String summary, String content) {
        AiSettings settings = aiSettingsRepository.findTopByEnabledTrue()
                .orElseThrow(() -> new BusinessException(40000, "AI功能未配置或已禁用"));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(settings.getChatApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", settings.getModelName());
            requestBody.put("stream", false);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content",
                            "你是一名中文内容编辑。你的任务是润色博客文章，不改变原意，不新增未经用户提供的事实，输出必须是可直接发布的 Markdown 正文。"),
                    Map.of("role", "user", "content", buildPolishPrompt(title, summary, content))
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    settings.getApiEndpoint(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String body = response.getBody();
            int statusCode = response.getStatusCode().value();

            if (statusCode != 200) {
                throw new BusinessException(
                        statusCode,
                        "AI润色接口返回错误状态码: " + statusCode + "，响应: " + abbreviate(body)
                );
            }

            if (body == null || body.trim().startsWith("<")) {
                throw new BusinessException(40002, "AI润色接口返回了非JSON响应，响应: " + abbreviate(body));
            }

            Map<String, Object> responseMap = objectMapper.readValue(body, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                Object polished = messageObj.get("content");
                return stripMarkdownCodeFence(polished == null ? "" : polished.toString());
            }
            return "未获取到有效润色结果";
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(40001, "AI润色失败: " + e.getMessage());
        }
    }

    public AiSettings getSettings() {
        return aiSettingsRepository.findTopByEnabledTrue().orElse(null);
    }

    public AiSettings saveSettings(AiSettings settings) {
        settings.setId(1L);
        return aiSettingsRepository.save(settings);
    }

    private String buildPolishPrompt(String title, String summary, String content) {
        String safeTitle = title == null ? "" : title.trim();
        String safeSummary = summary == null ? "" : summary.trim();
        String safeContent = content == null ? "" : content.trim();

        return """
                请润色下面这篇博客文章，并严格遵守以下要求：
                1. 保持原意，不要杜撰新的事实、案例、数据或链接。
                2. 优化中文表达、节奏、段落衔接和可读性，但不要改写成完全不同的文章。
                3. 保留并优化原有 Markdown 结构；标题、列表、引用、代码块、加粗等格式要继续可用。
                4. 如果原文里有代码块，只允许微调说明文字，代码块本身不要改动。
                5. 输出必须只有润色后的 Markdown 正文，不要解释，不要加前言，不要加“以下是润色结果”，不要放在 ``` 代码围栏里。
                6. 如果有摘要，请让正文风格与摘要一致；如果正文存在明显语病，可以修正。

                标题：
                %s

                摘要：
                %s

                正文：
                %s
                """.formatted(safeTitle, safeSummary, safeContent);
    }

    private String stripMarkdownCodeFence(String value) {
        if (value == null) {
            return "";
        }
        return MARKDOWN_CODE_FENCE_PATTERN.matcher(value.trim()).replaceAll("").trim();
    }

    public String generateImage(String prompt, String requestedSize) {
        System.out.println("[图片生成] 开始处理请求 | prompt长度=" + prompt.length() + " | requestedSize=" + requestedSize);

        AiSettings settings = aiSettingsRepository.findTopByEnabledTrue()
                .orElseThrow(() -> {
                    System.out.println("[图片生成] AI功能未配置或已禁用");
                    return new BusinessException(40000, "AI功能未配置或已禁用");
                });

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String imageApiKey = settings.getImageApiKey();
            if (imageApiKey == null || imageApiKey.trim().isEmpty()) {
                imageApiKey = settings.getChatApiKey();
                System.out.println("[图片生成] 未配置独立图片API Key，复用聊天Key");
            }
            headers.setBearerAuth(imageApiKey);

            String imageModel = settings.getImageModelName();
            if (imageModel == null || imageModel.trim().isEmpty()) {
                imageModel = "dall-e-3";
            }

            String resolvedSize = resolveImageSize(prompt, imageModel, requestedSize);
            System.out.println("[图片生成] 模型=" + imageModel + " | 解析后尺寸=" + resolvedSize);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", imageModel);
            requestBody.put("prompt", prompt);
            requestBody.put("n", 1);
            requestBody.put("size", resolvedSize);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String imageEndpoint = settings.getImageApiEndpoint();
            if (imageEndpoint == null || imageEndpoint.trim().isEmpty()) {
                imageEndpoint = settings.getApiEndpoint();
            }

            System.out.println("[图片生成] 正在请求外部API | endpoint=" + imageEndpoint);
            long startTime = System.currentTimeMillis();

            ResponseEntity<String> response = restTemplate.exchange(
                    imageEndpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            long elapsed = System.currentTimeMillis() - startTime;
            String body = response.getBody();
            int statusCode = response.getStatusCode().value();

            System.out.println("[图片生成] 外部API响应 | status=" + statusCode + " | 耗时=" + elapsed + "ms | body长度=" + (body == null ? 0 : body.length()));

            if (statusCode != 200) {
                System.out.println("[图片生成] 外部API返回错误状态码: " + statusCode);
                throw new BusinessException(statusCode, "图片生成API返回了错误状态码: " + statusCode);
            }

            if (body == null || body.trim().startsWith("<")) {
                System.out.println("[图片生成] 外部API返回非JSON响应: " + abbreviate(body));
                throw new BusinessException(40002, "API返回了非JSON响应，响应: " + abbreviate(body));
            }

            Map<String, Object> responseMap = objectMapper.readValue(body, Map.class);
            String result = extractImageResult(responseMap);
            System.out.println("[图片生成] 成功提取图片URL | url长度=" + result.length());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("[图片生成] 请求异常 | 类型=" + e.getClass().getSimpleName() + " | 消息=" + e.getMessage());
            e.printStackTrace();
            throw new BusinessException(40001, "图片生成失败: " + e.getMessage());
        }
    }

    String extractImageResult(Map<String, Object> responseMap) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
        if (data != null && !data.isEmpty()) {
            Map<String, Object> imageData = data.get(0);

            Object imageUrl = imageData.get("url");
            if (imageUrl instanceof String url && !url.isBlank()) {
                return url;
            }

            Object b64Json = imageData.get("b64_json");
            if (b64Json instanceof String base64 && !base64.isBlank()) {
                String outputFormat = responseMap.get("output_format") instanceof String format && !format.isBlank()
                        ? format
                        : "png";
                return "data:image/" + outputFormat + ";base64," + base64;
            }
        }

        List<Map<String, Object>> output = (List<Map<String, Object>>) responseMap.get("output");
        if (output != null) {
            for (Map<String, Object> item : output) {
                if (!"image_generation_call".equals(item.get("type"))) {
                    continue;
                }

                Object result = item.get("result");
                if (result instanceof String base64 && !base64.isBlank()) {
                    return "data:image/png;base64," + base64;
                }
            }
        }

        throw new BusinessException(40002, "图片生成接口返回结构中没有可用的图片数据");
    }

    String resolveImageSize(String prompt, String imageModel, String requestedSize) {
        String normalizedRequestedSize = normalizeRequestedSize(requestedSize);
        if (normalizedRequestedSize != null) {
            return normalizedRequestedSize;
        }

        AspectHint aspectHint = detectAspectHint(prompt);
        boolean wantsTwoK = containsAny(normalizePrompt(prompt), "2k", "2k高清", "高清", "印刷", "高质量素材", "高质量");
        String normalizedModel = imageModel == null ? "" : imageModel.trim().toLowerCase(Locale.ROOT);

        if (aspectHint == AspectHint.SQUARE) {
            return wantsTwoK ? "2048x2048" : "1024x1024";
        }
        if (aspectHint == AspectHint.WIDESCREEN) {
            return "2048x1152";
        }
        if (aspectHint == AspectHint.TALLSCREEN) {
            return "1152x2048";
        }
        if (aspectHint == AspectHint.PORTRAIT) {
            return wantsTwoK ? "1152x2048" : "1024x1536";
        }
        if (aspectHint == AspectHint.LANDSCAPE) {
            return wantsTwoK ? "2048x1152" : "1536x1024";
        }

        if (normalizedModel.startsWith("gpt-image") || normalizedModel.contains("image")) {
            return "auto";
        }
        return "1024x1024";
    }

    private String normalizeRequestedSize(String requestedSize) {
        if (requestedSize == null) {
            return null;
        }
        String normalized = requestedSize.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_IMAGE_SIZES.contains(normalized) ? normalized : null;
    }

    private AspectHint detectAspectHint(String prompt) {
        String normalizedPrompt = normalizePrompt(prompt);
        if (normalizedPrompt.isBlank()) {
            return AspectHint.UNKNOWN;
        }

        Matcher matcher = ASPECT_RATIO_PATTERN.matcher(normalizedPrompt);
        if (matcher.find()) {
            int first = Integer.parseInt(matcher.group(1));
            int second = Integer.parseInt(matcher.group(2));
            AspectHint explicitHint = detectExplicitSupportedAspect(first, second);
            if (explicitHint != AspectHint.UNKNOWN) {
                return explicitHint;
            }
            if (first == second) {
                return AspectHint.SQUARE;
            }
            return first > second ? AspectHint.LANDSCAPE : AspectHint.PORTRAIT;
        }

        if (containsAny(normalizedPrompt, "方图", "正方形", "头像", "社交媒体配图", "square")) {
            return AspectHint.SQUARE;
        }
        if (containsAny(normalizedPrompt, "16:9", "视频封面", "banner", "横版封面")) {
            return AspectHint.WIDESCREEN;
        }
        if (containsAny(normalizedPrompt, "9:16", "抖音", "短视频封面")) {
            return AspectHint.TALLSCREEN;
        }
        if (containsAny(normalizedPrompt, "2:3", "竖版手机壁纸", "小红书", "竖屏图")) {
            return AspectHint.PORTRAIT;
        }
        if (containsAny(normalizedPrompt, "3:2", "横版电脑壁纸", "ppt 配图", "ppt配图")) {
            return AspectHint.LANDSCAPE;
        }
        if (containsAny(normalizedPrompt, "竖图", "竖版", "portrait", "海报")) {
            return AspectHint.PORTRAIT;
        }
        if (containsAny(normalizedPrompt, "横图", "横版", "landscape", "宽屏", "横幅")) {
            return AspectHint.LANDSCAPE;
        }

        return AspectHint.UNKNOWN;
    }

    private AspectHint detectExplicitSupportedAspect(int first, int second) {
        int gcd = gcd(first, second);
        int normalizedFirst = first / gcd;
        int normalizedSecond = second / gcd;
        String ratioKey = normalizedFirst + ":" + normalizedSecond;

        return switch (ratioKey) {
            case "1:1" -> AspectHint.SQUARE;
            case "3:2", "4:3" -> AspectHint.LANDSCAPE;
            case "2:3", "3:4" -> AspectHint.PORTRAIT;
            case "16:9" -> AspectHint.WIDESCREEN;
            case "9:16" -> AspectHint.TALLSCREEN;
            default -> AspectHint.UNKNOWN;
        };
    }

    private String normalizePrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        return prompt
                .toLowerCase(Locale.ROOT)
                .replace('：', ':')
                .replace('／', '/')
                .replace('∶', ':')
                .replace('，', ',')
                .replace('（', '(')
                .replace('）', ')');
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private int gcd(int a, int b) {
        int x = Math.abs(a);
        int y = Math.abs(b);
        while (y != 0) {
            int temp = x % y;
            x = y;
            y = temp;
        }
        return x == 0 ? 1 : x;
    }

    private String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 200 ? body.substring(0, 200) : body;
    }

    private enum AspectHint {
        SQUARE,
        PORTRAIT,
        LANDSCAPE,
        WIDESCREEN,
        TALLSCREEN,
        UNKNOWN
    }
}

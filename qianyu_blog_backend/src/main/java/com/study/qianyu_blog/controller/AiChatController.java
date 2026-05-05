package com.study.qianyu_blog.controller;

import com.study.qianyu_blog.dto.AiChatRequest;
import com.study.qianyu_blog.dto.AiImageRequest;
import com.study.qianyu_blog.dto.ApiResponse;
import com.study.qianyu_blog.service.AiService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);
    private final AiService aiService;

    public AiChatController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        System.out.println("========== [AI] ping 收到请求 ==========");
        return ApiResponse.ok("pong");
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody chatStream(@RequestBody AiChatRequest request, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        return outputStream -> {
            try {
                aiService.chatStream(request.getMessage(), outputStream);
            } catch (Exception e) {
                try {
                    String error = "data: {\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}\n\n";
                    outputStream.write(error.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (Exception ignored) {
                }
            }
        };
    }

    @PostMapping("/image")
    public ApiResponse<Map<String, String>> generateImage(@RequestBody AiImageRequest request) {
        System.out.println("========== [AI生图] 收到请求 ==========");
        System.out.println("[AI生图] prompt=" + request.getPrompt());
        System.out.println("[AI生图] size=" + request.getSize());
        System.out.println("[AI生图] 请求处理开始...");

        String prompt = request.getPrompt();
        if (prompt == null || prompt.trim().isEmpty()) {
            System.out.println("[AI生图] 提示词为空，拒绝请求");
            return ApiResponse.error(40003, "提示词不能为空");
        }

        try {
            String imageUrl = aiService.generateImage(prompt, request.getSize());
            System.out.println("[AI生图] 请求成功，url长度=" + imageUrl.length());
            return ApiResponse.ok(Map.of("url", imageUrl));
        } catch (Exception e) {
            System.out.println("[AI生图] 请求失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

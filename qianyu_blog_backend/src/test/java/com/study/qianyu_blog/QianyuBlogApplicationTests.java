package com.study.qianyu_blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QianyuBlogApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void publicCategoriesCanBeVisited() throws Exception {
        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void adminPostsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void adminCanManageCategoryAndPost() throws Exception {
        String token = loginToken();
        long categoryId = createCategory(token);

        mockMvc.perform(put("/api/admin/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"技术更新","slug":"tech-updated","description":"更新后的技术文章"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("技术更新"));

        mockMvc.perform(get("/api/admin/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("tech-updated"));

        String postResponse = mockMvc.perform(post("/api/admin/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"第一篇博客","summary":"文章摘要","content":"文章正文","coverImageUrl":"https://example.com/cover.jpg","categoryId":%d,"status":"PUBLISHED"}
                                """.formatted(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("第一篇博客"))
                .andExpect(jsonPath("$.data.coverImageUrl").value("https://example.com/cover.jpg"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long postId = objectMapper.readTree(postResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/admin/posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("第一篇博客"));

        mockMvc.perform(get("/api/admin/posts/" + postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category.slug").value("tech-updated"));

        mockMvc.perform(get("/api/public/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("第一篇博客"));

        mockMvc.perform(put("/api/admin/posts/" + postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"更新后的博客","summary":"文章摘要","content":"文章正文","coverImageUrl":"","categoryId":%d,"status":"DRAFT"}
                                """.formatted(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.coverImageUrl").isEmpty());

        mockMvc.perform(delete("/api/admin/posts/" + postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/api/admin/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void adminCanSaveSiteSettingsAndUploadMedia() throws Exception {
        String token = loginToken();

        mockMvc.perform(post("/api/admin/site-settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName":"新的千语博客",
                                  "siteSubtitle":"新的副标题",
                                  "heroBadge":"开发 · 记录",
                                  "heroTitle":"新的主标题",
                                  "heroDescription":"新的首页描述",
                                  "avatarImageUrl":"https://example.com/avatar.png",
                                  "heroBackgroundImageUrl":"https://example.com/bg.png",
                                  "defaultPostCoverUrl":"https://example.com/default-cover.png",
                                  "githubUrl":"https://github.com/example",
                                  "email":"hello@example.com",
                                  "footerText":"新的页脚"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteName").value("新的千语博客"));

        mockMvc.perform(get("/api/public/site/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.heroTitle").value("新的主标题"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image-content".getBytes()
        );

        String uploadResponse = mockMvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .param("displayName", "封面图")
                        .param("altText", "文章封面")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("封面图"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode uploadJson = objectMapper.readTree(uploadResponse);
        long mediaId = uploadJson.path("data").path("id").asLong();

        mockMvc.perform(get("/api/admin/media")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(mediaId));

        mockMvc.perform(get("/api/public/media/" + mediaId + "/file"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(MediaType.IMAGE_PNG));
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private long createCategory(String token) throws Exception {
        String response = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"技术","slug":"tech","description":"技术文章"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("技术"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }
}

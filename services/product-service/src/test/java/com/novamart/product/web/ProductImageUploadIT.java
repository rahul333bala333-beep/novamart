package com.novamart.product.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductImageUploadIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private static final String SEEDED_PRODUCT_ID = "343213de-c447-56c6-ac74-dd29fcff1fec";

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can upload a valid PNG product image")
    void adminCanUploadValidPngImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-product.png",
                "image/png",
                new byte[]{ (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 }
        );

        MvcResult result = mvc.perform(multipart("/api/v1/products/{id}/image", SEEDED_PRODUCT_ID).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageUrl", startsWith("/uploads/products/")))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String imageUrl = json.readTree(responseBody).path("data").path("imageUrl").asText();

        // Verify that the product detail now returns this image URL
        mvc.perform(get("/api/v1/products/{id}", SEEDED_PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product.imageUrl").value(imageUrl));

        // Verify static asset serving endpoint /uploads/products/...
        mvc.perform(get(imageUrl))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can upload a valid JPG and WEBP product image")
    void adminCanUploadJpgAndWebp() throws Exception {
        MockMultipartFile jpgFile = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0 }
        );

        mvc.perform(multipart("/api/v1/products/{id}/image", SEEDED_PRODUCT_ID).file(jpgFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl", startsWith("/uploads/products/")));

        MockMultipartFile webpFile = new MockMultipartFile(
                "file",
                "photo.webp",
                "image/webp",
                "RIFF....WEBPVP8 ".getBytes()
        );

        mvc.perform(multipart("/api/v1/products/{id}/image", SEEDED_PRODUCT_ID).file(webpFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl", startsWith("/uploads/products/")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Reject unsupported file extensions (e.g. .exe, .txt)")
    void rejectsUnsupportedFileTypes() throws Exception {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/x-msdownload",
                "MZ...".getBytes()
        );

        mvc.perform(multipart("/api/v1/products/{id}/image", SEEDED_PRODUCT_ID).file(exeFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "some text".getBytes()
        );

        mvc.perform(multipart("/api/v1/products/{id}/image", SEEDED_PRODUCT_ID).file(txtFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Reject files larger than 5 MB")
    void rejectsOversizedFiles() throws Exception {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1024]; // 5 MB + 1 KB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "huge-photo.png",
                "image/png",
                largeBytes
        );

        mvc.perform(multipart("/api/v1/products/{id}/image", SEEDED_PRODUCT_ID).file(largeFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Shopper role is forbidden from uploading product images")
    void shopperCannotUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                new byte[]{ 1, 2, 3 }
        );

        mvc.perform(multipart("/api/v1/products/{id}/image", SEEDED_PRODUCT_ID).file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous caller is rejected with 401 unauthorized")
    void anonymousCannotUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                new byte[]{ 1, 2, 3 }
        );

        mvc.perform(multipart("/api/v1/products/{id}/image", SEEDED_PRODUCT_ID).file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Uploading image for non-existent product returns 404")
    void uploadForMissingProductReturns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                new byte[]{ 1, 2, 3 }
        );

        UUID nonExistentId = UUID.randomUUID();
        mvc.perform(multipart("/api/v1/products/{id}/image", nonExistentId).file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }
}

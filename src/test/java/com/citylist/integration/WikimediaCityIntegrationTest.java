package com.citylist.integration;

import com.citylist.db.postgresql.CityPostgresqlRepository;
import com.citylist.db.postgresql.entity.CityEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WikimediaCityIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:19.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CityPostgresqlRepository postgresqlRepository;

    @BeforeEach
    void setUp() {
        postgresqlRepository.deleteAll();
    }

    @Test
    void getWithWikimediaPhoto_unauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/city/wikimedia"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void getWithWikimediaPhoto_filtersOnlyValidWikimediaDomainsInPostgres() throws Exception {
        // Должны попасть в выборку
        saveCity("Valid Subdomain", "https://upload.wikimedia.org/wikipedia/commons/a/ab/City.jpg");
        saveCity("Valid Root Http", "http://wikimedia.org/photo.png");
        saveCity("Valid Uppercase", "HTTPS://COMMONS.WIKIMEDIA.ORG/wiki/File:City.jpg");

        // НЕ должны попасть в выборку
        saveCity("Fake Path", "https://evil.com/wikimedia.org/pic.jpg");
        saveCity("Fake Subdomain In Path", "https://evil.com/upload.wikimedia.org/pic.jpg");
        saveCity("Fake Suffix", "https://notwikimedia.org/pic.jpg");
        saveCity("Fake Prefix", "https://wikimedia.org.evil.com/pic.jpg");
        saveCity("Userinfo Trick", "https://wikimedia.org@evil.com/pic.jpg");
        saveCity("Other Domain", "https://flickr.com/photos/city.jpg");
        saveCity("Empty Url", "");
        saveCity("Null Url", null);

        MockHttpSession session = loginAsUser();

        mockMvc.perform(get("/city/wikimedia").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].name", contains(
                        "Valid Subdomain",
                        "Valid Root Http",
                        "Valid Uppercase"
                )));
    }

    @Test
    void getWithWikimediaPhoto_paginatesAndSortsByPhotoUrl() throws Exception {
        saveCity("City A", "https://upload.wikimedia.org/a.jpg");
        saveCity("City B", "https://upload.wikimedia.org/b.jpg");
        saveCity("City C", "https://upload.wikimedia.org/c.jpg");

        MockHttpSession session = loginAsUser();

        mockMvc.perform(get("/city/wikimedia")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "photoUrl,desc")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content[*].name", contains("City C", "City B")));

        mockMvc.perform(get("/city/wikimedia")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "photoUrl,desc")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", contains("City A")));
    }

    @Test
    void getWithWikimediaPhoto_returns400OnUnsupportedSortProperty() throws Exception {
        MockHttpSession session = loginAsUser();

        mockMvc.perform(get("/city/wikimedia")
                        .param("sort", "unknownField,asc")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported sort property: unknownField"));
    }

    private MockHttpSession loginAsUser() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "user")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private CityEntity saveCity(String name, String photoUrl) {
        CityEntity entity = new CityEntity();
        entity.setName(name);
        entity.setPhotoUrl(photoUrl);
        return postgresqlRepository.save(entity);
    }
}
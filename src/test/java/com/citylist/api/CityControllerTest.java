package com.citylist.api;

import com.citylist.api.handler.ApiExceptionHandler;
import com.citylist.service.CityDomainService;
import com.citylist.service.PhotoDomain;
import com.citylist.service.dto.CityDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CityControllerTest {

    @Mock
    private CityDomainService cityDomainService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CityController controller = new CityController(cityDomainService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getWithWikimediaPhoto_returnsPageAndPassesPageable() throws Exception {
        PageRequest expectedPageable = PageRequest.of(1, 5, Sort.by(Sort.Order.desc("name")));
        Page<CityDto> page = new PageImpl<>(
                List.of(CityDto.builder().id(10L).name("Vienna").photoUrl("https://upload.wikimedia.org/v.jpg").build()),
                expectedPageable,
                11
        );
        when(cityDomainService.getCitiesByPhotoDomain(eq(PhotoDomain.WIKIMEDIA.getHost()), any()))
                .thenReturn(page);

        mockMvc.perform(get("/city/wikimedia")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].name").value("Vienna"))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(1));

        verify(cityDomainService).getCitiesByPhotoDomain(PhotoDomain.WIKIMEDIA.getHost(), expectedPageable);
    }

    @Test
    void getWithWikimediaPhoto_returns400WhenSortPropertyInvalid() throws Exception {
        when(cityDomainService.getCitiesByPhotoDomain(eq(PhotoDomain.WIKIMEDIA.getHost()), any()))
                .thenThrow(new IllegalArgumentException("Unsupported sort property: hack"));

        mockMvc.perform(get("/city/wikimedia").param("sort", "hack,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported sort property: hack"));
    }
}
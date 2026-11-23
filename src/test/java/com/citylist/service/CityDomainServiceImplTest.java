package com.citylist.service;

import com.citylist.db.CityRepository;
import com.citylist.service.dto.CityDto;
import com.citylist.service.mapper.CityUpdateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityDomainServiceImplTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private CityUpdateMapper cityUpdateMapper;

    @InjectMocks
    private CityDomainServiceImpl cityDomainService;

    @Test
    void getCitiesByPhotoDomain_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<CityDto> expectedPage = new PageImpl<>(
                List.of(CityDto.builder().id(1L).name("Berlin").photoUrl("https://upload.wikimedia.org/b.jpg").build()),
                pageable,
                1
        );
        when(cityRepository.findAllByPhotoDomain("wikimedia.org", pageable)).thenReturn(expectedPage);

        Page<CityDto> actual = cityDomainService.getCitiesByPhotoDomain("wikimedia.org", pageable);

        assertThat(actual).isSameAs(expectedPage);
        verify(cityRepository).findAllByPhotoDomain("wikimedia.org", pageable);
    }
}
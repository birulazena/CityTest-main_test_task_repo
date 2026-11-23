package com.citylist.db;

import com.citylist.db.postgresql.CityPostgresqlRepository;
import com.citylist.db.postgresql.entity.CityEntity;
import com.citylist.db.postgresql.mapper.DbMapper;
import com.citylist.service.dto.CityDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityDbRepositoryTest {

    @Mock
    private DbMapper dbMapper;

    @Mock
    private CityPostgresqlRepository postgresqlRepository;

    @InjectMocks
    private CityDbRepository cityDbRepository;

    @Test
    void findAllByPhotoDomain_normalizesDomainAndAppliesDefaultIdSort() {
        CityEntity entity = new CityEntity();
        entity.setId(1L);
        entity.setName("Amsterdam");
        entity.setPhotoUrl("https://upload.wikimedia.org/pic.jpg");

        CityDto expectedDto = CityDto.builder()
                .id(1L)
                .name("Amsterdam")
                .photoUrl("https://upload.wikimedia.org/pic.jpg")
                .build();

        when(postgresqlRepository.findAllByPhotoDomainRegex(any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));
        when(dbMapper.toCityDto(entity)).thenReturn(expectedDto);

        Page<CityDto> result = cityDbRepository.findAllByPhotoDomain("  WIKIMEDIA.ORG ", PageRequest.of(0, 20));

        ArgumentCaptor<String> regexCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postgresqlRepository).findAllByPhotoDomainRegex(regexCaptor.capture(), pageableCaptor.capture());

        assertThat(regexCaptor.getValue())
                .isEqualTo("^https?://([a-z0-9-]+\\.)*wikimedia\\.org([:/?#]|$)");
        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Order.asc("id")));
        assertThat(result.getContent()).containsExactly(expectedDto);
    }

    @Test
    void findAllByPhotoDomain_mapsAllowedSortPropertiesToDbColumns() {
        when(postgresqlRepository.findAllByPhotoDomainRegex(any(), any())).thenReturn(Page.empty());

        Pageable pageable = PageRequest.of(1, 10, Sort.by(
                Sort.Order.desc("photoUrl"),
                Sort.Order.asc("name")
        ));
        cityDbRepository.findAllByPhotoDomain("wikimedia.org", pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postgresqlRepository).findAllByPhotoDomainRegex(any(), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(
                Sort.Order.desc("photo_url"),
                Sort.Order.asc("name")
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "localhost", "evil.*", "(a+)+", "wikimedia.org/path", ".wikimedia.org"})
    void findAllByPhotoDomain_rejectsInvalidOrUnsafeDomain(String invalidDomain) {
        assertThatThrownBy(() -> cityDbRepository.findAllByPhotoDomain(invalidDomain, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid domain format");

        verifyNoInteractions(postgresqlRepository);
    }

    @Test
    void findAllByPhotoDomain_rejectsUnsupportedSortField() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("unknownField"));

        assertThatThrownBy(() -> cityDbRepository.findAllByPhotoDomain("wikimedia.org", pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sort property");

        verifyNoInteractions(postgresqlRepository);
    }
}
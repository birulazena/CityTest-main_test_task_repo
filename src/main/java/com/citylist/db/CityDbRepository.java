package com.citylist.db;

import com.citylist.db.postgresql.CityPostgresqlRepository;
import com.citylist.db.postgresql.entity.CityEntity;
import com.citylist.db.postgresql.mapper.DbMapper;
import com.citylist.service.dto.CityDto;
import com.citylist.service.dto.CitySearchDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CityDbRepository implements CityRepository {
    private final DbMapper dbMapper;
    private final CityPostgresqlRepository postgresqlRepository;

    private static final Pattern VALID_DOMAIN_PATTERN =
            Pattern.compile("^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+$");

    private static final Map<String, String> ALLOWED_SORT_COLUMNS = Map.of(
            "id", "id",
            "name", "name",
            "photoUrl", "photo_url"
    );

    @Override
    @NonNull
    public Page<CityDto> searchPage(@NonNull CitySearchDto searchDto, @NonNull Pageable pageable) {
        return postgresqlRepository
            .findByNameStartingWithIgnoreCase(searchDto.getName(), pageable)
            .map(dbMapper::toCityDto);
    }

    @Override
    @NonNull
    public Page<CityDto> findAllByPhotoDomain(@NonNull String domain, @NonNull Pageable pageable) {
        String domainRegex = buildDomainRegex(domain);
        Pageable nativePageable = toNativePageable(pageable);
        return postgresqlRepository
                .findAllByPhotoDomainRegex(domainRegex, nativePageable)
                .map(dbMapper::toCityDto);
    }

    @Override
    @NonNull
    public CityDto findById(@NonNull Long id) {
        return postgresqlRepository
            .findById(id)
            .map(dbMapper::toCityDto)
            .orElseThrow(() -> new EntityNotFoundException(String.format("City not found for ID(%s)", id)));
    }

    @Override
    @NonNull
    public CityDto save(@NonNull CityDto city) {
        CityEntity dbCityEntity = dbMapper.toCityEntity(city);
        CityEntity savedCity = postgresqlRepository.save(dbCityEntity);
        return dbMapper.toCityDto(savedCity);
    }

    @Override
    @NonNull
    public List<CityDto> saveAll(@NonNull List<CityDto> cities) {
        List<CityEntity> dbCityEntities = cities
            .stream()
            .map(dbMapper::toCityEntity)
            .collect(Collectors.toList());
        List<CityEntity> savedCities = postgresqlRepository.saveAll(dbCityEntities);
        return savedCities
            .stream()
            .map(dbMapper::toCityDto)
            .collect(Collectors.toList());
    }

    private String buildDomainRegex(String domain) {
        String normalized = domain.trim().toLowerCase(Locale.ROOT);
        if (!VALID_DOMAIN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid domain format: " + domain);
        }
        String escapedDomain = normalized.replace(".", "\\.");
        return "^https?://([a-z0-9-]+\\.)*" + escapedDomain + "([:/?#]|$)";
    }

    private Pageable toNativePageable(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        Sort sort = pageable.getSort().isUnsorted()
                ? Sort.by(Sort.Order.asc("id"))
                : mapAndValidateSort(pageable.getSort());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Sort mapAndValidateSort(Sort sort) {
        List<Sort.Order> mappedOrders = sort.stream()
                .map(order -> {
                    String column = ALLOWED_SORT_COLUMNS.get(order.getProperty());
                    if (column == null) {
                        throw new IllegalArgumentException("Unsupported sort property: " + order.getProperty());
                    }
                    return new Sort.Order(order.getDirection(), column);
                })
                .collect(Collectors.toList());
        return Sort.by(mappedOrders);
    }
}

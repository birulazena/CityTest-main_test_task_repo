package com.citylist.db.postgresql;

import com.citylist.db.postgresql.entity.CityEntity;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CityPostgresqlRepository extends JpaRepository<CityEntity, Long> {
    @NonNull
    Page<CityEntity> findByNameStartingWithIgnoreCase(@NonNull String name, @NonNull Pageable pageable);

    @NonNull
    @Query(
            value = "SELECT id, name, photo_url FROM city WHERE photo_url ~* :domainRegex",
            countQuery = "SELECT count(*) FROM city WHERE photo_url ~* :domainRegex",
            nativeQuery = true
    )
    Page<CityEntity> findAllByPhotoDomainRegex(
            @Param("domainRegex") @NonNull String domainRegex,
            @NonNull Pageable pageable
    );
}

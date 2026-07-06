package com.fundmatrix.repository;

import com.fundmatrix.domain.FolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FolioHoldingRepository extends JpaRepository<FolioHolding, Long> {

    List<FolioHolding> findByFolio_Id(Long folioId);

    List<FolioHolding> findByScheme_Id(Long schemeId);

    List<FolioHolding> findByOption_Id(Long optionId);

    List<FolioHolding> findByFolio_Investor_Id(Long investorId);

    Optional<FolioHolding> findByFolio_IdAndOption_Id(Long folioId, Long optionId);

    /** Holdings in a given scheme option held under folios serviced by a distributor. */
    List<FolioHolding> findByFolio_Distributor_IdAndScheme_Id(Long distributorId, Long schemeId);

    @Query("select coalesce(sum(h.currentValue), 0) from FolioHolding h " +
            "where h.folio.distributor.id = :distributorId and h.scheme.id = :schemeId")
    BigDecimal sumCurrentValueByDistributorAndScheme(@Param("distributorId") Long distributorId,
                                                     @Param("schemeId") Long schemeId);

    @Query("select coalesce(sum(h.currentValue), 0) from FolioHolding h where h.scheme.id = :schemeId")
    BigDecimal sumCurrentValueByScheme(@Param("schemeId") Long schemeId);

    @Query("select coalesce(sum(h.currentValue), 0) from FolioHolding h " +
            "where h.folio.distributor.id = :distributorId")
    BigDecimal sumCurrentValueByDistributor(@Param("distributorId") Long distributorId);
}

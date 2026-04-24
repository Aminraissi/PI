package org.exemple.farmersupport.repository;

import org.exemple.farmersupport.entity.Culture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CultureRepository extends JpaRepository<Culture, Long> {
    List<Culture> findByParcelleIdParcelle(Long parcelleId);
}
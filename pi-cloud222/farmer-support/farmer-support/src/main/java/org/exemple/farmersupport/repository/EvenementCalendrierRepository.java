package org.exemple.farmersupport.repository;

import org.exemple.farmersupport.entity.EvenementCalendrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvenementCalendrierRepository extends JpaRepository<EvenementCalendrier, Long> {
}
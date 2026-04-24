package org.exemple.farmersupport.repository;

import org.exemple.farmersupport.entity.Rappel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RappelRepository extends JpaRepository<Rappel, Long> {
    List<Rappel> findByEvenementIdEvent(Long eventId);
}
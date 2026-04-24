package org.exemple.gestionformation.repository;

import org.exemple.gestionformation.entity.LeconVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeconVideoRepository extends JpaRepository<LeconVideo, Long> {
    List<LeconVideo> findByModuleIdModule(Long moduleId);
}
package tn.esprit.livraison.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.livraison.entity.Livraison;
import tn.esprit.livraison.enums.StatusLivraison;

@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Integer> {
    List<Livraison> findByAgriculteurIdOrderByDateCreationDesc(int agriculteurId);
    List<Livraison> findByTransporteurIdOrderByDateCreationDesc(int transporteurId);
    List<Livraison> findByGroupReferenceOrderByDateCreationDesc(String groupReference);
    List<Livraison> findByGroupReferenceAndTransporteurId(String groupReference, int transporteurId);

    List<Livraison> findByStatusOrderByDateCreationDesc(StatusLivraison status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Livraison l where l.id = :id")
    Optional<Livraison> findByIdForUpdate(@Param("id") Integer id);
    List<Livraison> findByTransporteurIdAndStatusInOrderByDateCreationDesc(
            int transporteurId, List<StatusLivraison> statuses);
    List<Livraison> findByTransporteurIdAndDateLivraisonPrevueBetweenOrderByDateLivraisonPrevueAsc(
            int transporteurId, LocalDateTime start, LocalDateTime end);
    
    List<Livraison> findByTransporteurIdAndDateCreationAfterOrderByDateCreationDesc(
            int transporteurId, LocalDateTime cutoff);
    
    List<Livraison> findByNotificationToUserIdAndNotificationStatusNotNullOrderByNotificationCreatedAtDesc(int userId);
    long countByNotificationToUserIdAndNotificationSeenFalse(int userId);
}

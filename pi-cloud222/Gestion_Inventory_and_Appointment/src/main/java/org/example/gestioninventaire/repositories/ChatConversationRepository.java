package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    Optional<ChatConversation> findByFarmerIdAndVeterinarianId(Long farmerId, Long veterinarianId);
    List<ChatConversation> findByFarmerIdOrVeterinarianIdOrderByUpdatedAtDesc(Long farmerId, Long veterinarianId);
}

package tn.esprit.forums.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.forums.model.ForumReport;

public interface ForumReportRepository extends JpaRepository<ForumReport, Long> {

    List<ForumReport> findByTargetTypeAndTargetIdOrderByIdDesc(String targetType, Long targetId);

    List<ForumReport> findByStatusOrderByIdDesc(String status);

    List<ForumReport> findByPostIdOrderByIdDesc(Long postId);

    List<ForumReport> findByPostIdInAndStatusOrderByIdDesc(List<Long> postIds, String status);

    List<ForumReport> findByPostIdAndStatusOrderByIdDesc(Long postId, String status);

    boolean existsByTargetTypeAndTargetIdAndReporterIdAndStatus(String targetType, Long targetId, Long reporterId, String status);
}

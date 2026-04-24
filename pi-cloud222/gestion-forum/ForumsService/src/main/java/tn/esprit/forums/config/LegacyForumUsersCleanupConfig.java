package tn.esprit.forums.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class LegacyForumUsersCleanupConfig {

    private static final Logger logger = LoggerFactory.getLogger(LegacyForumUsersCleanupConfig.class);

    @Bean
    CommandLineRunner cleanupLegacyForumUsersSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            List<ForeignKeyRef> fkRefs = jdbcTemplate.query(
                    """
                    SELECT TABLE_NAME, CONSTRAINT_NAME
                    FROM information_schema.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND REFERENCED_TABLE_NAME = 'forum_users'
                      AND CONSTRAINT_NAME <> 'PRIMARY'
                    """,
                    (rs, rowNum) -> new ForeignKeyRef(rs.getString("TABLE_NAME"), rs.getString("CONSTRAINT_NAME"))
            );

            for (ForeignKeyRef ref : fkRefs) {
                String dropFkSql = "ALTER TABLE `" + ref.tableName() + "` DROP FOREIGN KEY `" + ref.constraintName() + "`";
                jdbcTemplate.execute(dropFkSql);
                logger.info("Dropped legacy foreign key {} on {}", ref.constraintName(), ref.tableName());
            }

            jdbcTemplate.execute("DROP TABLE IF EXISTS forum_users");
            logger.info("Dropped legacy table forum_users if it existed");
        };
    }

    private record ForeignKeyRef(String tableName, String constraintName) {
    }
}
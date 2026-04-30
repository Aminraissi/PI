package tn.esprit.forums.config;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tn.esprit.forums.model.ForumComment;
import tn.esprit.forums.model.ForumGroup;
import tn.esprit.forums.model.ForumPost;
import tn.esprit.forums.model.ForumReply;
import tn.esprit.forums.repository.ForumGroupRepository;
import tn.esprit.forums.repository.ForumPostRepository;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedForumsData(ForumPostRepository postRepository, ForumGroupRepository groupRepository) {
        return args -> {
        if (true) {
                return;
            }

        ForumGroup irrigationGroup = new ForumGroup();
        irrigationGroup.setName("Irrigation Lab");
        irrigationGroup.setDescription("Scheduling, drought strategy, and system tuning for reliable water management.");
        irrigationGroup.setCreatedAt("2026-03-20T09:00:00Z");
        irrigationGroup.setCreatedBy(2L);
        irrigationGroup.setFocusTags(List.of("Irrigation", "Water-Management", "Soil"));
        irrigationGroup.setRules(List.of(
            "Keep advice practical and field-tested.",
            "Share local climate context when possible.",
            "Avoid product spam and unsafe recommendations."
        ));
        irrigationGroup.setMemberIds(List.of(2L, 5L, 6L, 1L));
        irrigationGroup.setModeratorIds(List.of(2L, 5L));

        ForumGroup cropHealthGroup = new ForumGroup();
        cropHealthGroup.setName("Crop Health Circle");
        cropHealthGroup.setDescription("Disease diagnosis, pest control, and prevention workflows.");
        cropHealthGroup.setCreatedAt("2026-03-20T10:00:00Z");
        cropHealthGroup.setCreatedBy(4L);
        cropHealthGroup.setFocusTags(List.of("Disease-Prevention", "Pest", "Greenhouse"));
        cropHealthGroup.setRules(List.of(
            "Always include symptoms and timeline.",
            "Prioritize safe and compliant treatment guidance.",
            "No personal attacks or dismissive replies."
        ));
        cropHealthGroup.setMemberIds(List.of(4L, 2L, 3L, 8L));
        cropHealthGroup.setModeratorIds(List.of(4L));

        ForumGroup postHarvestGroup = new ForumGroup();
        postHarvestGroup.setName("Post-Harvest Logistics");
        postHarvestGroup.setDescription("Handling, storage, transport, and quality preservation after harvest.");
        postHarvestGroup.setCreatedAt("2026-03-20T11:00:00Z");
        postHarvestGroup.setCreatedBy(7L);
        postHarvestGroup.setFocusTags(List.of("Transport", "Post-Harvest", "Cold-Chain"));
        postHarvestGroup.setRules(List.of(
            "Share measurable outcomes when possible.",
            "Keep recommendations feasible for small farms.",
            "Respect region-specific constraints and regulations."
        ));
        postHarvestGroup.setMemberIds(List.of(7L, 4L, 8L, 1L));
        postHarvestGroup.setModeratorIds(List.of(7L));

        irrigationGroup = groupRepository.save(irrigationGroup);
        cropHealthGroup = groupRepository.save(cropHealthGroup);
        postHarvestGroup = groupRepository.save(postHarvestGroup);

            ForumPost firstPost = new ForumPost();
            firstPost.setTitle("How do I reduce fungal disease risk in greenhouse tomatoes?");
            firstPost.setContent("Humidity in my greenhouse keeps spiking at night and I started seeing early signs of fungal spots. I already ventilate in the morning. Any practical workflow that works?");
            firstPost.setTags(List.of("Greenhouse", "Tomato", "Disease-Prevention"));
            firstPost.setAuthorId(2L);
            firstPost.setGroupId(cropHealthGroup.getId());
            firstPost.setCreatedAt("2026-03-23T08:20:00Z");
            firstPost.setViews(124);

            ForumReply firstReply = new ForumReply();
            firstReply.setAuthorId(1L);
            firstReply.setContent("Use a night ventilation schedule + reduce leaf wetness duration. Water earlier in the day and maintain spacing for airflow. Also sanitize tools weekly to avoid spread.");
            firstReply.setUpvotes(18);
            firstReply.setDownvotes(1);
            firstReply.setAccepted(true);
            firstReply.setCreatedAt("2026-03-23T09:10:00Z");

            ForumComment firstComment = new ForumComment();
            firstComment.setAuthorId(2L);
            firstComment.setContent("Thanks! I will try switching irrigation earlier.");
            firstComment.setCreatedAt("2026-03-23T09:25:00Z");
            firstReply.addComment(firstComment);

            ForumReply secondReply = new ForumReply();
            secondReply.setAuthorId(4L);
            secondReply.setContent("I also had success by adding a simple circulation fan overnight in corners with stagnant air.");
            secondReply.setUpvotes(5);
            secondReply.setDownvotes(0);
            secondReply.setAccepted(false);
            secondReply.setCreatedAt("2026-03-23T10:05:00Z");

            ForumComment secondComment = new ForumComment();
            secondComment.setAuthorId(3L);
            secondComment.setContent("This is useful for my own greenhouse too. Thanks for the detail.");
            secondComment.setCreatedAt("2026-03-23T10:20:00Z");
            secondReply.addComment(secondComment);

            firstPost.addReply(firstReply);
            firstPost.addReply(secondReply);

            ForumPost secondPost = new ForumPost();
            secondPost.setTitle("Best way to transport strawberries without bruising?");
            secondPost.setContent("Looking for practical transport tips for short and medium distance delivery. Current losses are too high.");
            secondPost.setTags(List.of("Transport", "Strawberry", "Post-Harvest"));
            secondPost.setAuthorId(4L);
            secondPost.setGroupId(postHarvestGroup.getId());
            secondPost.setCreatedAt("2026-03-24T11:45:00Z");
            secondPost.setViews(87);

            ForumReply thirdReply = new ForumReply();
            thirdReply.setAuthorId(7L);
            thirdReply.setContent("Use shallow crates, avoid stacking fruit too high, and keep the cargo area cool with airflow if possible.");
            thirdReply.setUpvotes(11);
            thirdReply.setDownvotes(0);
            thirdReply.setAccepted(false);
            thirdReply.setCreatedAt("2026-03-24T12:15:00Z");

            ForumComment thirdComment = new ForumComment();
            thirdComment.setAuthorId(8L);
            thirdComment.setContent("We tried this in our delivery route and the bruising rate dropped significantly.");
            thirdComment.setCreatedAt("2026-03-24T12:30:00Z");
            thirdReply.addComment(thirdComment);

            secondPost.addReply(thirdReply);

            ForumPost thirdPost = new ForumPost();
            thirdPost.setTitle("Which irrigation schedule works best for young citrus trees in dry weather?");
            thirdPost.setContent("I’m adjusting irrigation for young citrus trees and want a schedule that avoids both water stress and overwatering during hot, dry days.");
            thirdPost.setTags(List.of("Irrigation", "Citrus", "Water-Management"));
            thirdPost.setAuthorId(5L);
            thirdPost.setGroupId(irrigationGroup.getId());
            thirdPost.setCreatedAt("2026-03-25T07:40:00Z");
            thirdPost.setViews(53);

            ForumReply fourthReply = new ForumReply();
            fourthReply.setAuthorId(6L);
            fourthReply.setContent("For young trees, shorter but more frequent watering works better than long infrequent cycles. Check soil moisture a few centimeters below the surface.");
            fourthReply.setUpvotes(9);
            fourthReply.setDownvotes(1);
            fourthReply.setAccepted(true);
            fourthReply.setCreatedAt("2026-03-25T08:05:00Z");

            ForumComment fourthComment = new ForumComment();
            fourthComment.setAuthorId(1L);
            fourthComment.setContent("That matches what we see in the field. Moisture checks help a lot.");
            fourthComment.setCreatedAt("2026-03-25T08:20:00Z");
            fourthReply.addComment(fourthComment);

            thirdPost.addReply(fourthReply);

            postRepository.save(firstPost);
            postRepository.save(secondPost);
            postRepository.save(thirdPost);
        };
    }
}

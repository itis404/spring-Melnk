package ru.studymarket.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.studymarket.domain.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @EntityGraph(attributePaths = {"sender", "recipient", "product", "product.seller"})
    @Query("""
            select m
            from ChatMessage m
            where m.product.id = :productId
              and (
                   (lower(m.sender.username) = lower(:username) and lower(m.recipient.username) = lower(:counterpartUsername))
                or (lower(m.sender.username) = lower(:counterpartUsername) and lower(m.recipient.username) = lower(:username))
              )
            order by m.sentAt asc
            """)
    List<ChatMessage> findThreadForUser(@Param("productId") Long productId,
                                        @Param("username") String username,
                                        @Param("counterpartUsername") String counterpartUsername);

    @EntityGraph(attributePaths = {"sender", "recipient", "product", "product.seller"})
    @Query("""
            select m
            from ChatMessage m
            where lower(m.sender.username) = lower(:username)
               or lower(m.recipient.username) = lower(:username)
            order by m.sentAt desc
            """)
    List<ChatMessage> findInbox(@Param("username") String username);
}

package io.github.rochona.notificationsystem.outbox;

import io.github.rochona.notificationsystem.outbox.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE
                (
                    status = 'PENDING'
                    AND next_attempt_at <= :now
                )
                OR
                (
                    status = 'PROCESSING'
                    AND locked_until <= :now
                )
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findEventsToClaim(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize
    );
}

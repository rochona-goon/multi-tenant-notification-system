package io.github.rochona.notificationsystem.integration;

import io.github.rochona.notificationsystem.notification.dto.NotificationRequestDTO;
import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.repository.NotificationRepository;
import io.github.rochona.notificationsystem.notification.service.NotificationServiceImpl;
import io.github.rochona.notificationsystem.outbox.repository.OutboxEventRepository;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventStatus;
import io.github.rochona.notificationsystem.preference.service.PreferenceService;
import io.github.rochona.notificationsystem.template.service.TemplateRenderer;
import io.github.rochona.notificationsystem.template.service.TemplateService;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.repository.TenantRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(NotificationServiceImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationOutboxAtomicityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private NotificationServiceImpl notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private TemplateService templateService;

    @MockitoBean
    private TemplateRenderer templateRenderer;

    @MockitoBean
    private PreferenceService preferenceService;

    private TransactionTemplate transactionTemplate;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        // Clean up in FK-safe order.
        outboxEventRepository.deleteAll();
        notificationRepository.deleteAll();
        tenantRepository.deleteAll();

        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        tenant = new Tenant();
        tenant.setName("Atomicity Test Tenant");
        tenant.setApiKeyHash(UUID.randomUUID().toString());

        tenant = tenantRepository.saveAndFlush(tenant);
    }

    @Test
    void shouldPersistNotificationAndOutboxEventTogether() {
        NotificationRequestDTO request = createRequest();

        Notification savedNotification = transactionTemplate.execute(
                status -> notificationService.createNotification(request, tenant)
        );

        assertNotNull(savedNotification);
        assertNotNull(savedNotification.getId());

        assertEquals(1, notificationRepository.count());
        assertEquals(1, outboxEventRepository.count());

        var outboxEvents = outboxEventRepository.findAll();

        assertEquals(OutboxEventStatus.PENDING, outboxEvents.getFirst().getStatus());
    }

    @Test
    void shouldRollbackNotificationAndOutboxEventTogether() {
        NotificationRequestDTO request = createRequest();

        assertThrows(IllegalStateException.class, () ->
                transactionTemplate.executeWithoutResult(status -> {
                    notificationService.createNotification(request, tenant);

                    // Simulate a failure after both save operations.
                    throw new IllegalStateException("Force transaction rollback");
                })
        );

        assertEquals(0, notificationRepository.count());
        assertEquals(0, outboxEventRepository.count());
    }

    private NotificationRequestDTO createRequest() {
        NotificationRequestDTO request = new NotificationRequestDTO();

        request.setSubject("Atomicity integration test");
        request.setMessage("Testing transactional outbox");
        request.setRecipient("atomicity-" + UUID.randomUUID() + "@example.com");
        request.setChannel(NotificationChannel.EMAIL);

        // No userId: preference check is intentionally skipped.
        return request;
    }
}

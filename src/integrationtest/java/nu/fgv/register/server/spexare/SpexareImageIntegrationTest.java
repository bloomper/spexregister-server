/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.spexare;

import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.audit.AuditService;
import nu.fgv.register.server.audit.AuditedType;
import nu.fgv.register.server.audit.FieldChangeDto;
import nu.fgv.register.server.audit.RevisionDto;
import nu.fgv.register.server.util.AbstractIntegrationTest;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.acls.model.AclCache;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static nu.fgv.register.server.util.security.SecurityUtil.runAsSystem;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexareImageIntegrationTest extends AbstractIntegrationTest {

    private final SpexareService service;
    private final SpexareRepository repository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SpexareImageIntegrationTest(final JdbcClient jdbcClient,
                                       final AclCache aclCache,
                                       final Keycloak keycloakAdminClient,
                                       final String keycloakClientId,
                                       final PermissionService permissionService,
                                       final ObjectMapper objectMapper,
                                       final SpexareService service,
                                       final SpexareRepository repository,
                                       final AuditService auditService,
                                       final TransactionTemplate transactionTemplate) {
        super(jdbcClient, aclCache, keycloakAdminClient, keycloakClientId, permissionService, objectMapper);
        this.service = service;
        this.repository = repository;
        this.auditService = auditService;
        this.transactionTemplate = transactionTemplate;
    }

    @BeforeEach
    void setUp() {
        jdbcClient.sql("UPDATE spexare SET partner_id = NULL").update();
        JdbcTestUtils.deleteFromTables(jdbcClient, "spexare", "spexare_audit", "image", "image_audit");
    }

    @Test
    void should_not_load_the_image_with_the_spexare() throws IOException {
        final byte[] red = png(Color.RED);
        final Long id = createWithImage(red);

        transactionTemplate.executeWithoutResult(_ -> {
            final Spexare spexare = repository.findById(id).orElseThrow();

            assertThat(spexare.getImage()).isNotNull();
            assertThat(Hibernate.isInitialized(spexare.getImage())).isFalse();
            assertThat(spexare.isImagePresent()).isTrue();
        });
    }

    @Test
    void should_keep_a_replaced_image_in_the_history_and_restore_it() throws IOException {
        final byte[] red = png(Color.RED);
        final byte[] blue = png(Color.BLUE);
        final Long id = createWithImage(red);
        final AtomicReference<List<RevisionDto>> revisions = new AtomicReference<>();

        runAsSystem(() -> {
            service.saveImage(id, blue);
            revisions.set(auditService.findRevisions(AuditedType.SPEXARE, String.valueOf(id)));
        });

        final List<RevisionDto> withImageChanges = revisions.get().stream()
                .filter(revision -> revision.changes().stream().anyMatch(change -> "image".equals(change.field())))
                .toList();

        assertThat(withImageChanges).hasSize(2);
        assertThat(withImageChanges.getFirst().changes())
                .filteredOn(change -> "image".equals(change.field()))
                .singleElement()
                .extracting(FieldChangeDto::binary)
                .isEqualTo(true);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM image").query(Long.class).single()).isEqualTo(1L);

        final long firstImageRevision = withImageChanges.getLast().revision();

        runAsSystem(() -> {
            assertThat(auditService.findBinary(AuditedType.SPEXARE, String.valueOf(id), firstImageRevision, "image").content()).isEqualTo(red);

            auditService.restore(AuditedType.SPEXARE, String.valueOf(id), firstImageRevision, false);

            assertThat(service.getImage(id).getFirst()).isEqualTo(red);
        });
    }

    private Long createWithImage(final byte[] image) {
        final AtomicReference<Long> id = new AtomicReference<>();

        runAsSystem(() -> {
            final SpexareDto spexare = service.create(SpexareCreateDto.builder().firstName("Ada").lastName("Lovelace").deceased(false).published(true).build());

            service.saveImage(spexare.getId(), image);
            id.set(spexare.getId());
        });

        return id.get();
    }

    private static byte[] png(final Color color) throws IOException {
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        image.setRGB(0, 0, color.getRGB());
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}

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

package nu.fgv.register.server.util.data;

import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.Connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class DataSeedingCallbackTest {

    private final SampleDataImporter sampleDataImporter = mock(SampleDataImporter.class);
    private final AuditDataSeeder auditDataSeeder = mock(AuditDataSeeder.class);
    private final Context context = mock(Context.class);
    private final Connection connection = mock(Connection.class);

    @BeforeEach
    void setUp() {
        when(context.getConnection()).thenReturn(connection);
    }

    @Test
    void should_only_support_after_migrate() {
        final DataSeedingCallback callback = callback(true, true);

        assertThat(callback.supports(Event.AFTER_MIGRATE, context), is(true));
        assertThat(callback.supports(Event.BEFORE_MIGRATE, context), is(false));
        assertThat(callback.supports(Event.AFTER_EACH_MIGRATE, context), is(false));
    }

    @Test
    void should_do_nothing_when_disabled() {
        callback(false, false).handle(Event.AFTER_MIGRATE, context);

        verifyNoInteractions(sampleDataImporter, auditDataSeeder);
    }

    @Test
    void should_seed_baseline_when_no_revisions_exist() {
        when(auditDataSeeder.hasRevisions(any())).thenReturn(false);

        callback(true, false).handle(Event.AFTER_MIGRATE, context);

        verify(auditDataSeeder).seedBaseline(any());
        verifyNoInteractions(sampleDataImporter);
    }

    @Test
    void should_not_seed_baseline_when_revisions_exist() {
        when(auditDataSeeder.hasRevisions(any())).thenReturn(true);

        callback(true, false).handle(Event.AFTER_MIGRATE, context);

        verify(auditDataSeeder, never()).seedBaseline(any());
    }

    @Test
    void should_import_sample_data_without_seeding_baseline() {
        callback(false, true).handle(Event.AFTER_MIGRATE, context);

        verify(sampleDataImporter).importSampleData(connection);
        verifyNoInteractions(auditDataSeeder);
    }

    @Test
    void should_import_sample_data_before_checking_for_revisions() {
        when(auditDataSeeder.hasRevisions(any())).thenReturn(true);

        callback(true, true).handle(Event.AFTER_MIGRATE, context);

        final InOrder order = inOrder(sampleDataImporter, auditDataSeeder);

        order.verify(sampleDataImporter).importSampleData(connection);
        order.verify(auditDataSeeder).hasRevisions(any());
        verify(auditDataSeeder, never()).seedBaseline(any());
    }

    private DataSeedingCallback callback(final boolean seedAuditData, final boolean importSampleData) {
        return new DataSeedingCallback(seedAuditData, importSampleData, sampleDataImporter, auditDataSeeder);
    }
}

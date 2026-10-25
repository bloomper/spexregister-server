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

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

/**
 * After Flyway has migrated, imports the sample data and seeds a baseline audit history when asked to.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Component
public class DataSeedingCallback implements Callback {

    private final boolean seedAuditData;
    private final boolean importSampleData;
    private final SampleDataImporter sampleDataImporter;
    private final AuditDataSeeder auditDataSeeder;

    public DataSeedingCallback(@Value("${spexregister.data.seed-audit-data:false}") final boolean seedAuditData,
                               @Value("${spexregister.data.import-sample-data:false}") final boolean importSampleData,
                               final SampleDataImporter sampleDataImporter,
                               final AuditDataSeeder auditDataSeeder) {
        this.seedAuditData = seedAuditData;
        this.importSampleData = importSampleData;
        this.sampleDataImporter = sampleDataImporter;
        this.auditDataSeeder = auditDataSeeder;
    }

    @Override
    public boolean supports(final Event event, final Context context) {
        return event == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(final Event event, final Context context) {
        return false;
    }

    @Override
    public void handle(final Event event, final Context context) {
        if (importSampleData) {
            sampleDataImporter.importSampleData(context.getConnection());
        }

        if (seedAuditData) {
            final JdbcClient jdbcClient = JdbcClient.create(new SingleConnectionDataSource(context.getConnection(), true));

            if (auditDataSeeder.hasRevisions(jdbcClient)) {
                log.info("Audit data already exists, skipping baseline seeding");
            } else {
                auditDataSeeder.seedBaseline(jdbcClient);
            }
        }
    }

    @Override
    public String getCallbackName() {
        return DataSeedingCallback.class.getSimpleName();
    }
}

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
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Component
public class R__SeedAuditData extends BaseJavaMigration {

    private final boolean seedAuditData;
    private final boolean importSampleData;
    private final AuditDataSeeder seeder;
    private final Random rnd = new SecureRandom();

    public R__SeedAuditData(@Value("${spexregister.data.seed-audit-data:false}") final boolean seedAuditData,
                            @Value("${spexregister.data.import-sample-data:false}") final boolean importSampleData,
                            final AuditDataSeeder seeder) {
        this.seedAuditData = seedAuditData;
        this.importSampleData = importSampleData;
        this.seeder = seeder;
    }

    @Override
    public Integer getChecksum() {
        return rnd.nextInt();
    }

    @Override
    public void migrate(final Context context) {
        if (seedAuditData) {
            if (importSampleData) {
                log.warn("Both sample data import and audit data seeding are enabled; the sample revision history will be replaced by a single baseline revision");
            }

            seeder.seedBaseline(JdbcClient.create(new SingleConnectionDataSource(context.getConnection(), true)));
        }
    }
}
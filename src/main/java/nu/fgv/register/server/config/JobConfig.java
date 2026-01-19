/*
 * Copyright 2024 the original author or authors.
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

package nu.fgv.register.server.config;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.impex.JobProgressService;
import nu.fgv.register.server.impex.util.SecurityContextStepListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;

import javax.sql.DataSource;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Configuration
@RequiredArgsConstructor
@EnableScheduling
@EnableBatchProcessing
@EnableJdbcJobRepository
public class JobConfig {

    private final SecurityContextStepListener securityListener;
    private final JobProgressService jobProgressService;
    private final Tasklet exportTasklet;

    @Bean
    public JobRepository jobRepository(final DataSource dataSource, final PlatformTransactionManager transactionManager) throws Exception {
        final JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();

        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setIsolationLevelForCreateEnum(Isolation.DEFAULT);
        factory.afterPropertiesSet();

        return factory.getObject();
    }

    @Bean
    protected TaskExecutor taskExecutor() {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("Batch-");
        taskExecutor.setVirtualThreads(true);
        taskExecutor.initialize();

        return taskExecutor;
    }

    @Bean
    public JobOperatorFactoryBean jobOperator(final JobRepository jobRepository, final TaskExecutor taskExecutor) {
        final JobOperatorFactoryBean jobOperatorFactoryBean = new JobOperatorFactoryBean();

        jobOperatorFactoryBean.setJobRepository(jobRepository);
        jobOperatorFactoryBean.setTaskExecutor(taskExecutor);

        return jobOperatorFactoryBean;
    }

    @Bean
    public Job exportJob(final JobRepository jobRepository, final Step exportStep) {
        return new JobBuilder("exportJob", jobRepository)
                .start(exportStep)
                .listener(jobProgressService)
                .build();
    }

    @Bean
    public Step exportStep(final JobRepository jobRepository,
                           final PlatformTransactionManager transactionManager) {
        return new StepBuilder("exportStep", jobRepository)
                .tasklet(exportTasklet, transactionManager)
                .listener(securityListener)
                .build();
    }
}

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

package nu.fgv.register.server.statistics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.UserRepository;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BiFunction;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StatisticsService {

    private final SpexareRepository spexareRepository;
    private final UserRepository userRepository;
    private final SpexRepository spexRepository;
    private final TaskRepository taskRepository;
    private final EntityManager entityManager;

    @RequiresAdminOrEditorOrUser
    public StatisticsDto getStatistics() {
        return StatisticsDto.builder()
                .spexareCount(spexareRepository.countByPublishedTrue())
                .spexareCountHistory(getHistory(Spexare.class, (cb, root) -> cb.isTrue(root.get("published"))))
                .userCount(userRepository.count())
                .userCountHistory(getHistory(User.class))
                .spexCount(spexRepository.countByParentIsNull())
                .spexCountHistory(getHistory(Spex.class, (cb, root) -> cb.isNull(root.get("parent"))))
                .spexRevivalCount(spexRepository.countByParentIsNotNull())
                .spexRevivalCountHistory(getHistory(Spex.class, (cb, root) -> cb.isNotNull(root.get("parent"))))
                .taskCount(taskRepository.count())
                .taskCountHistory(getHistory(Task.class))
                .build();
    }

    private <T> List<HistoryDto> getHistory(final Class<T> clazz) {
        return getHistory(clazz, null);
    }

    private <T> List<HistoryDto> getHistory(final Class<T> clazz, @Nullable final BiFunction<CriteriaBuilder, Root<T>, Predicate> filter) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
        final Root<T> root = criteriaQuery.from(clazz);

        final Expression<Integer> year = criteriaBuilder.function("year", Integer.class, root.get("createdAt"));
        final Expression<Integer> month = criteriaBuilder.function("month", Integer.class, root.get("createdAt"));
        final Expression<Long> count = criteriaBuilder.count(root);

        criteriaQuery.select(criteriaBuilder.tuple(year, month, count));

        final Instant threeYearsAgo = Instant.now().minus(3 * 365, ChronoUnit.DAYS);
        final Predicate historyPredicate = criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), threeYearsAgo);

        if (filter != null) {
            criteriaQuery.where(criteriaBuilder.and(historyPredicate, filter.apply(criteriaBuilder, root)));
        } else {
            criteriaQuery.where(historyPredicate);
        }

        criteriaQuery.groupBy(year, month);
        criteriaQuery.orderBy(criteriaBuilder.asc(year), criteriaBuilder.asc(month));

        return entityManager.createQuery(criteriaQuery)
                .getResultList()
                .stream()
                .map(tuple -> new HistoryDto(
                        String.format("%s-%02d", tuple.get(0), (Integer) tuple.get(1)),
                        (Long) tuple.get(2)
                ))
                .toList();
    }
}

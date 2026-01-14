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

package nu.fgv.register.server.acl;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.security.acls.model.Permission;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@NoRepositoryBean
public interface AclJpaRepository<T, ID> extends JpaRepository<T, ID> {

    List<T> findAll(Permission permission);

    List<T> findAll(Sort sort, Permission permission);

    List<T> findAll(Specification<T> spec, Sort sort, Permission permission);

    Page<T> findAll(Pageable pageable, Permission permission);

    List<T> findAll(Specification<T> spec, Permission permission);

    Page<T> findAll(Specification<T> spec, Pageable pageable, Permission permission);

    <S extends T, R> R findBy(Specification<T> spec, Permission permission, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction);

    Stream<T> streamAll(Permission permission);

    Stream<T> streamAll(Sort sort, Permission permission);

    Stream<T> streamAll(@Nullable Specification<T> spec, Sort sort, Permission permission);

}

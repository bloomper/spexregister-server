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

package nu.fgv.register.server.permission;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.news.News;
import nu.fgv.register.server.news.NewsDto;
import nu.fgv.register.server.news.NewsRepository;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.spexare.SpexareRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Resolves {@code permissions} on spexare, spex and news: what the caller may do with each, so the client can show only
 * the actions the server will allow.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@RequiredArgsConstructor
@Controller
public class PermissionsGraphqlApi {

    private static final PermissionsDto NONE = new PermissionsDto(false, false, false, false);

    private final PermissionService permissionService;
    private final SpexareRepository spexareRepository;
    private final SpexRepository spexRepository;
    private final NewsRepository newsRepository;

    @BatchMapping(typeName = "Spexare", field = "permissions")
    @Transactional(readOnly = true)
    public Map<SpexareDto, PermissionsDto> spexare(final List<SpexareDto> spexare) {
        return permissions(spexare, SpexareDto::getId, spexareRepository, Spexare::getId);
    }

    @BatchMapping(typeName = "Spex", field = "permissions")
    @Transactional(readOnly = true)
    public Map<SpexDto, PermissionsDto> spex(final List<SpexDto> spex) {
        return permissions(spex, SpexDto::getId, spexRepository, Spex::getId);
    }

    @BatchMapping(typeName = "News", field = "permissions")
    @Transactional(readOnly = true)
    public Map<NewsDto, PermissionsDto> news(final List<NewsDto> news) {
        return permissions(news, NewsDto::getId, newsRepository, News::getId);
    }

    private <D, E> Map<D, PermissionsDto> permissions(final List<D> dtos,
                                                      final Function<D, Long> dtoId,
                                                      final CrudRepository<E, Long> repository,
                                                      final Function<E, Long> entityId) {
        final Map<Long, E> entities = new HashMap<>();

        repository.findAllById(dtos.stream().map(dtoId).toList()).forEach(entity -> entities.put(entityId.apply(entity), entity));

        final Map<D, PermissionsDto> permissions = new LinkedHashMap<>();

        dtos.forEach(dto -> {
            final E entity = entities.get(dtoId.apply(dto));

            permissions.put(dto, entity == null ? NONE : new PermissionsDto(
                    permissionService.hasReadPermission(entity),
                    permissionService.hasWritePermission(entity),
                    permissionService.hasDeletePermission(entity),
                    permissionService.hasAdministrationPermission(entity)));
        });

        return permissions;
    }
}

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

package nu.fgv.register.server.user;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.user.authority.AuthorityImpexDto;
import nu.fgv.register.server.util.impex.exporting.AbstractExportService;
import nu.fgv.register.server.util.impex.exporting.ExportEngine;
import nu.fgv.register.server.util.impex.model.ReportHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static nu.fgv.register.server.user.UserMapper.USER_MAPPER;
import static nu.fgv.register.server.user.authority.AuthorityMapper.AUTHORITY_MAPPER;
import static nu.fgv.register.server.user.state.StateMapper.STATE_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class UserExportService extends AbstractExportService {

    private final UserService service;

    public UserExportService(final UserService service, final List<ExportEngine> engines) {
        super(engines);
        this.service = service;
    }

    @Override
    protected List<ReportHolder<?>> getReports(final List<Long> ids, final String filter) {
        final Iterable<User> users = service.streamByIds(ids, filter, Sort.by(Sort.Direction.ASC, "externalId"));

        return List.of(
                ReportHolder.of(
                        toImpexDto(users, user -> {
                            final UserKeycloakData data = service.getKeycloakDataByUser(user);
                            return USER_MAPPER.toImpexDto(user, data != null ? data.representation() : null, STATE_MAPPER.toDto(user.getState()));
                        }),
                        UserImpexDto.class
                ),
                ReportHolder.of(
                        () -> StreamSupport.stream(users.spliterator(), false)
                                .flatMap(user -> {
                                    final UserKeycloakData data = service.getKeycloakDataByUser(user);
                                    if (data == null) {
                                        return Stream.empty();
                                    }
                                    return data.authorities().stream()
                                            .map(auth -> AUTHORITY_MAPPER.toImpexDto(user, auth));
                                }).iterator(),
                        AuthorityImpexDto.class
                )
        );
    }

}

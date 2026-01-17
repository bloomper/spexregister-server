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
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.spexare.SpexareService;
import nu.fgv.register.server.user.authority.AuthorityImpexDto;
import nu.fgv.register.server.user.authority.AuthorityService;
import nu.fgv.register.server.user.state.StateService;
import nu.fgv.register.server.util.impex.importing.AbstractImportService;
import nu.fgv.register.server.util.impex.importing.ImportEngine;
import nu.fgv.register.server.util.impex.importing.ImportSpec;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static nu.fgv.register.server.user.UserMapper.USER_MAPPER;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class UserImportService extends AbstractImportService {

    private final UserService service;
    private final StateService stateService;
    private final AuthorityService authorityService;
    private final SpexareService spexareService;

    public UserImportService(final List<ImportEngine> engines,
                             final UserService service,
                             final StateService stateService,
                             final AuthorityService authorityService,
                             final SpexareService spexareService,
                             final MessageSource messageSource) {
        super(engines, messageSource);
        this.service = service;
        this.stateService = stateService;
        this.authorityService = authorityService;
        this.spexareService = spexareService;
    }

    @Override
    protected List<ImportSpec> getImportSpecs() {
        return List.of(
                ImportSpec.builder()
                        .clazz(UserImpexDto.class)
                        .existenceCheckers(Map.of(
                                "id", v -> service.exists((Long) v),
                                "stateId", v -> stateService.exists((String) v),
                                "spexareId", v -> {
                                    if (v != null && hasText(v.toString())) {
                                        return spexareService.exists((Long) v);
                                    } else {
                                        return true;
                                    }
                                }
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(AuthorityImpexDto.class)
                        .existenceCheckers(Map.of(
                                "id", v -> authorityService.exists((String) v)
                        ))
                        .name("user.impex.authority.sheetName")
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ImportResultDto processImport(final Map<Class<?>, List<?>> data, final Locale locale) {
        final Map<Long, Long> userIdMap = new HashMap<>();
        final Map<String, Map<String, String>> userData = new HashMap<>();
        final ImportSummary summary = new ImportSummary(messageSource, locale);

        handleImport(
                (List<UserImpexDto>) data.get(UserImpexDto.class),
                summary,
                "user.impex.entityName",
                dto -> {
                    final UserDto user = service.create(USER_MAPPER.toCreateDto(dto));

                    userIdMap.put(dto.getId(), user.getId());
                    service.setState(user.getId(), dto.getStateId());

                    if (hasText(user.getTemporaryPassword())) {
                        userData.put(user.getEmail(), Map.of(
                                "externalId", user.getExternalId(),
                                "temporaryPassword", user.getTemporaryPassword()
                        ));
                    }

                    if (dto.getSpexareId() != null) {
                        service.addSpexare(user.getId(), dto.getSpexareId());
                    }
                },
                dto -> {
                    final UserDto user = service.partialUpdate(USER_MAPPER.toUpdateDto(dto));

                    userIdMap.put(dto.getId(), user.getId());
                    service.setState(user.getId(), dto.getStateId());

                    final SpexareDto currentSpexare = service.findSpexareByUser(user.getId()).orElse(null);

                    if (dto.getSpexareId() != null) {
                        if (currentSpexare == null || !currentSpexare.getId().equals(dto.getSpexareId())) {
                            service.addSpexare(user.getId(), dto.getSpexareId());
                        }
                    } else if (currentSpexare != null) {
                        service.removeSpexare(user.getId());
                    }
                },
                dto -> service.deleteById(dto.getId())
        );

        handleImport(
                (List<AuthorityImpexDto>) data.get(AuthorityImpexDto.class),
                summary,
                "user.impex.authority.entityName",
                dto -> {
                    final Long realUserId = userIdMap.getOrDefault(dto.getUserId(), dto.getUserId());

                    service.addAuthority(realUserId, dto.getId());
                },
                dto -> {
                    final Long realUserId = userIdMap.getOrDefault(dto.getUserId(), dto.getUserId());

                    service.addAuthority(realUserId, dto.getId());
                },
                dto -> {
                    final Long realUserId = userIdMap.getOrDefault(dto.getUserId(), dto.getUserId());

                    service.removeAuthority(realUserId, dto.getId());
                }
        );

        final Map<String, Object> resultData = new HashMap<>();

        if (!userData.isEmpty()) {
            resultData.put("users", userData);
        }

        return summary.toResult(resultData);
    }

}

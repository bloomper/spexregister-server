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

package nu.fgv.register.server.spexare;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.CountryService;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.spex.SpexService;
import nu.fgv.register.server.spexare.activity.ActivityImpexDto;
import nu.fgv.register.server.spexare.activity.ActivityService;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityService;
import nu.fgv.register.server.spexare.activity.task.TaskActivityService;
import nu.fgv.register.server.spexare.activity.task.actor.ActorService;
import nu.fgv.register.server.spexare.address.AddressImpexDto;
import nu.fgv.register.server.spexare.address.AddressService;
import nu.fgv.register.server.spexare.consent.ConsentImpexDto;
import nu.fgv.register.server.spexare.consent.ConsentService;
import nu.fgv.register.server.spexare.membership.MembershipImpexDto;
import nu.fgv.register.server.spexare.membership.MembershipService;
import nu.fgv.register.server.spexare.tagging.TaggingImpexDto;
import nu.fgv.register.server.spexare.toggle.ToggleImpexDto;
import nu.fgv.register.server.spexare.toggle.ToggleService;
import nu.fgv.register.server.tag.TagService;
import nu.fgv.register.server.task.TaskService;
import nu.fgv.register.server.util.impex.importing.AbstractImportService;
import nu.fgv.register.server.util.impex.importing.ImportEngine;
import nu.fgv.register.server.util.impex.importing.ImportSpec;
import nu.fgv.register.server.util.impex.model.ImportResultDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class SpexareImportService extends AbstractImportService {

    private final SpexareService service;
    private final AddressService addressService;
    private final ConsentService consentService;
    private final MembershipService membershipService;
    private final ToggleService toggleService;
    private final ActivityService activityService;
    private final SpexActivityService spexActivityService;
    private final TaskActivityService taskActivityService;
    private final ActorService actorService;
    private final TagService tagService;
    private final SpexService spexService;
    private final TaskService taskService;
    private final TypeService typeService;
    private final CountryService countryService;

    public SpexareImportService(final List<ImportEngine> engines,
                                final SpexareService service,
                                final AddressService addressService,
                                final ConsentService consentService,
                                final MembershipService membershipService,
                                final ToggleService toggleService,
                                final ActivityService activityService,
                                final SpexActivityService spexActivityService,
                                final TaskActivityService taskActivityService,
                                final ActorService actorService,
                                final TagService tagService,
                                final SpexService spexService,
                                final TaskService taskService,
                                final TypeService typeService,
                                final CountryService countryService) {
        super(engines);
        this.service = service;
        this.addressService = addressService;
        this.consentService = consentService;
        this.membershipService = membershipService;
        this.toggleService = toggleService;
        this.activityService = activityService;
        this.spexActivityService = spexActivityService;
        this.taskActivityService = taskActivityService;
        this.actorService = actorService;
        this.tagService = tagService;
        this.spexService = spexService;
        this.taskService = taskService;
        this.typeService = typeService;
        this.countryService = countryService;
    }

    @Override
    protected List<ImportSpec> getImportSpecs() {
        return List.of(
                ImportSpec.builder()
                        .clazz(SpexareImpexDto.class)
                        .existenceCheckers(Map.of(
                                "id", v -> service.exists((Long) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(AddressImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "id", v -> addressService.exists((Long) v),
                                "country", v -> countryService.exists((String) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(ConsentImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "id", v -> consentService.exists((Long) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(MembershipImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "id", v -> membershipService.exists((Long) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(TaggingImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "tagId", v -> tagService.exists((Long) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(ToggleImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "id", v -> toggleService.exists((Long) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(ActivityImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "id", v -> activityService.exists((Long) v),
                                "spexActivityId", v -> spexActivityService.exists((Long) v),
                                "spexId", v -> spexService.exists((Long) v),
                                "taskActivityId", v -> taskActivityService.exists((Long) v),
                                "taskId", v -> taskService.exists((Long) v),
                                "actorId", v -> actorService.exists((Long) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build()
        );
    }

    @Override
    protected ImportResultDto processImport(final Map<Class<?>, List<?>> data) {
        final List<SpexareImpexDto> mainData = (List<SpexareImpexDto>) data.get(SpexareImpexDto.class);
        final List<AddressImpexDto> addresses = (List<AddressImpexDto>) data.get(AddressImpexDto.class);
        // TODO

        // TODO
        return ImportResultDto.builder().success(true).build();
    }
}

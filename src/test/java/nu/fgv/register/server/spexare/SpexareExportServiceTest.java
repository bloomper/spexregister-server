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

package nu.fgv.register.server.spexare;

import nu.fgv.register.server.impex.exporting.ExportHolder;
import nu.fgv.register.server.settings.CountryDto;
import nu.fgv.register.server.settings.CountryService;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexService;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityImpexDto;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.actor.Actor;
import nu.fgv.register.server.spexare.address.Address;
import nu.fgv.register.server.spexare.address.AddressImpexDto;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagService;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static nu.fgv.register.server.util.TestUuids.uuid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * What a spexare export holds: a sheet per kind of record, one row per activity, task and actor, and the lists that
 * explain the ids.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexareExportServiceTest {

    private final SpexareService spexareService = mock(SpexareService.class);
    private final CountryService countryService = mock(CountryService.class);
    private final SpexService spexService = mock(SpexService.class);
    private final TaskService taskService = mock(TaskService.class);
    private final TagService tagService = mock(TagService.class);
    private final TypeService typeService = mock(TypeService.class);
    private final SpexareExportService service = new SpexareExportService(spexareService, countryService, spexService, taskService, tagService,
            typeService, List.of());
    private Spexare anna;
    private Activity withActors;
    private TaskActivity acting;
    private TaskActivity orchestra;

    private static Type type(final String id, final TypeType kind) {
        final Type type = new Type();

        type.setId(id);
        type.setType(kind);
        type.setLabels(Map.of("sv", id));
        return type;
    }

    private static <T> List<T> rows(final List<ExportHolder<?>> holders, final Class<T> clazz) {
        final List<T> rows = new ArrayList<>();

        holders.stream().filter(holder -> holder.clazz().equals(clazz)).findFirst().orElseThrow()
                .data().forEach(row -> rows.add(clazz.cast(row)));
        return rows;
    }

    @BeforeEach
    void setUp() {
        anna = new Spexare();
        anna.setId(1L);
        anna.setFirstName("Anna");
        anna.setLastName("Andersson");

        final Address address = new Address();

        address.setId(10L);
        address.setStreetAddress("Kårhuset");
        address.setCountry("SE");
        address.setType(type("HOME", TypeType.ADDRESS));
        address.setSpexare(anna);
        anna.getAddresses().add(address);

        final Tag tag = new Tag();

        tag.setId(3L);
        tag.setName("Styrelse");
        anna.getTags().add(tag);

        final Spex spex = new Spex();

        spex.setId(9L);
        spex.setYear("2024");

        final SpexActivity spexActivity = new SpexActivity();
        final Task ensemble = new Task();

        withActors = new Activity();
        acting = new TaskActivity();
        orchestra = new TaskActivity();

        withActors.setId(20L);
        withActors.setSpexare(anna);
        spexActivity.setId(21L);
        spexActivity.setSpex(spex);
        spexActivity.setActivity(withActors);
        withActors.setSpexActivity(spexActivity);
        ensemble.setId(2L);
        acting.setId(22L);
        acting.setTask(ensemble);
        orchestra.setId(23L);
        orchestra.setTask(ensemble);
        for (final String role : List.of("Ali Baba", "Kassim")) {
            final Actor actor = new Actor();

            actor.setId((long) role.length());
            actor.setRole(role);
            actor.setVocal(type("TENOR", TypeType.VOCAL));
            actor.setTaskActivity(acting);
            acting.getActors().add(actor);
        }
        withActors.getTaskActivities().addAll(List.of(acting, orchestra));

        final Activity withoutSpex = new Activity();

        withoutSpex.setId(30L);
        withoutSpex.setSpexare(anna);
        anna.getActivities().addAll(List.of(withActors, withoutSpex));

        when(spexareService.streamByIds(anyList(), anyString(), any())).thenReturn(List.of(anna));
        when(countryService.findAll()).thenReturn(List.of(CountryDto.builder().isoCode("SE").build()));
        when(spexService.streamByIds(anyList(), anyString(), any())).thenReturn(List.of(spex));
        when(spexService.streamRevivalsByParentIds(anyList(), anyString(), any())).thenReturn(List.of());
        when(taskService.streamByIds(anyList(), anyString(), any())).thenReturn(List.of(ensemble));
        when(tagService.streamByIds(anyList(), anyString(), any())).thenReturn(List.of(tag));
        when(typeService.streamByIds(anyList(), any())).thenReturn(List.of());
    }

    @Test
    void should_have_a_sheet_per_kind_and_read_only_lists_for_the_ids() {
        final List<ExportHolder<?>> holders = service.getReports(List.of(uuid(1)), "");

        assertThat(holders, hasSize(13));
        assertThat(holders.stream().filter(ExportHolder::readOnly).count(), is(6L));
        assertThat(rows(holders, SpexareImpexDto.class).stream().map(SpexareImpexDto::getFirstName).toList(), contains("Anna"));
        assertThat(rows(holders, AddressImpexDto.class).stream().map(AddressImpexDto::getStreetAddress).toList(), contains("Kårhuset"));
    }

    @Test
    void should_write_a_row_per_actor_and_per_task_without_actors_and_skip_activities_without_spex() {
        final List<ActivityImpexDto> rows = rows(service.getReports(List.of(uuid(1)), ""), ActivityImpexDto.class);

        assertThat(rows.stream().map(row -> row.getTaskActivityId() + ":" + row.getActorRole()).toList(),
                containsInAnyOrder(acting.getUuid() + ":Ali Baba", acting.getUuid() + ":Kassim", orchestra.getUuid() + ":null"));
        assertThat(rows.stream().map(ActivityImpexDto::getId).distinct().toList(), contains(withActors.getUuid()));
    }
}

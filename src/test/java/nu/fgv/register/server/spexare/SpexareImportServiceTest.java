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

import nu.fgv.register.server.impex.model.ImpexAction;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.settings.CountryService;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.spex.SpexService;
import nu.fgv.register.server.spexare.activity.ActivityDto;
import nu.fgv.register.server.spexare.activity.ActivityImpexDto;
import nu.fgv.register.server.spexare.activity.ActivityService;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityDto;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityService;
import nu.fgv.register.server.spexare.activity.task.TaskActivityDto;
import nu.fgv.register.server.spexare.activity.task.TaskActivityService;
import nu.fgv.register.server.spexare.activity.task.actor.ActorService;
import nu.fgv.register.server.spexare.address.AddressImpexDto;
import nu.fgv.register.server.spexare.address.AddressService;
import nu.fgv.register.server.spexare.consent.ConsentService;
import nu.fgv.register.server.spexare.membership.MembershipService;
import nu.fgv.register.server.spexare.tagging.TaggingImpexDto;
import nu.fgv.register.server.spexare.tagging.TaggingService;
import nu.fgv.register.server.spexare.toggle.ToggleService;
import nu.fgv.register.server.tag.TagService;
import nu.fgv.register.server.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static nu.fgv.register.server.util.TestUuids.uuid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Importing spexare with everything that belongs to them: one unit per spexare, new ids for new spexare, and partners
 * linked last.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexareImportServiceTest {

    private final SpexareService spexareService = mock(SpexareService.class);
    private final AddressService addressService = mock(AddressService.class);
    private final TaggingService taggingService = mock(TaggingService.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final SpexActivityService spexActivityService = mock(SpexActivityService.class);
    private final TaskActivityService taskActivityService = mock(TaskActivityService.class);
    private final ActorService actorService = mock(ActorService.class);
    private SpexareImportService service;

    private static SpexareImpexDto spexare(final UUID id, final ImpexAction action, final int row) {
        final SpexareImpexDto dto = new SpexareImpexDto();

        dto.setId(id);
        dto.setFirstName("Anna");
        dto.setLastName("Andersson");
        dto.setAction(action);
        dto.setRowNumber(row);
        return dto;
    }

    private static AddressImpexDto address(final UUID spexareId, final int row) {
        final AddressImpexDto dto = new AddressImpexDto();

        dto.setSpexareId(spexareId);
        dto.setTypeId("HOME");
        dto.setStreetAddress("Kårhuset");
        dto.setAction(ImpexAction.CREATE);
        dto.setRowNumber(row);
        return dto;
    }

    private static boolean isPlaceholder(final UUID id) {
        return (id.getLeastSignificantBits() & Long.MAX_VALUE) >= 1000;
    }

    private static ActivityImpexDto activity(final UUID id, final int row) {
        final ActivityImpexDto dto = new ActivityImpexDto();

        dto.setSpexareId(uuid(5));
        dto.setId(id);
        dto.setAction(isPlaceholder(id) ? ImpexAction.CREATE : ImpexAction.UPDATE);
        dto.setSpexActivityAction(ImpexAction.CREATE);
        dto.setSpexActivityId(uuid(10020));
        dto.setSpexId(uuid(9));
        dto.setTaskActivityAction(ImpexAction.CREATE);
        dto.setTaskActivityId(uuid(10030));
        dto.setTaskId(uuid(1));
        dto.setRowNumber(row);
        return dto;
    }

    private ImportResultDto run(final Map<Class<?>, List<?>> data) {
        return service.processImport(data, Locale.ENGLISH);
    }

    @BeforeEach
    void setUp() {
        final StaticMessageSource messages = new StaticMessageSource();

        messages.setUseCodeAsDefaultMessage(true);
        messages.addMessage("common.impex.summary.error", Locale.ENGLISH, "Row {0}, {1}: {2}");
        service = new SpexareImportService(List.of(), spexareService, addressService, mock(ConsentService.class), mock(MembershipService.class),
                mock(ToggleService.class), activityService, spexActivityService, taskActivityService, actorService, mock(TagService.class),
                taggingService, mock(SpexService.class), mock(TaskService.class), mock(TypeService.class), mock(CountryService.class),
                messages, mock(PlatformTransactionManager.class), "https://register.example.com");
        when(spexareService.create(any())).thenReturn(SpexareDto.builder().id(uuid(101)).build());
        when(spexareService.partialUpdate(any())).thenAnswer(invocation -> SpexareDto.builder().id(((SpexareUpdateDto) invocation.getArgument(0)).id()).build());
        when(spexareService.findPartnerBySpexare(any(UUID.class))).thenReturn(Optional.empty());
    }

    @Test
    void should_give_rows_of_a_new_spexare_its_new_id() {
        final ImportResultDto result = run(Map.of(SpexareImpexDto.class, List.of(spexare(uuid(1001), ImpexAction.CREATE, 2)),
                AddressImpexDto.class, List.of(address(uuid(1001), 2))));

        assertThat(result.getErrors(), empty());
        verify(addressService).create(eq(uuid(101)), eq("HOME"), any());
        verify(spexareService).deleteImage(uuid(101));
    }

    @Test
    void should_keep_other_spexare_when_one_fails() {
        org.mockito.Mockito.doThrow(new IllegalStateException("Låst")).when(spexareService).partialUpdate(any());

        final ImportResultDto result = run(Map.of(SpexareImpexDto.class, List.of(spexare(uuid(1001), ImpexAction.CREATE, 2), spexare(uuid(5), ImpexAction.UPDATE, 3))));

        assertThat(result.getErrors(), hasSize(1));
        verify(spexareService).create(any());
    }

    @Test
    void should_link_partners_once_both_exist_and_remove_a_partner_no_longer_given() {
        final SpexareImpexDto first = spexare(uuid(1001), ImpexAction.CREATE, 2);
        final SpexareImpexDto second = spexare(uuid(7), ImpexAction.UPDATE, 3);

        first.setPartnerId(uuid(7));
        when(spexareService.findPartnerBySpexare(uuid(7))).thenReturn(Optional.of(SpexareDto.builder().id(uuid(8)).build()));

        final ImportResultDto result = run(Map.of(SpexareImpexDto.class, List.of(first, second)));

        assertThat(result.getErrors(), empty());
        verify(spexareService).addPartner(uuid(101), uuid(7));
        verify(spexareService).removePartner(uuid(7));
    }

    @Test
    void should_not_relink_a_partner_already_linked() {
        final SpexareImpexDto dto = spexare(uuid(5), ImpexAction.UPDATE, 2);

        dto.setPartnerId(uuid(6));
        when(spexareService.findPartnerBySpexare(uuid(5))).thenReturn(Optional.of(SpexareDto.builder().id(uuid(6)).build()));

        assertThat(run(Map.of(SpexareImpexDto.class, List.of(dto))).getErrors(), empty());
        verify(spexareService, never()).addPartner(any(UUID.class), any(UUID.class));
    }

    @Test
    void should_delete_a_spexare_and_its_tagging() {
        final TaggingImpexDto tagging = new TaggingImpexDto();

        tagging.setSpexareId(uuid(5));
        tagging.setTagId(uuid(3));
        tagging.setAction(ImpexAction.DELETE);
        tagging.setRowNumber(4);

        assertThat(run(Map.of(SpexareImpexDto.class, List.of(spexare(uuid(5), ImpexAction.DELETE, 2)), TaggingImpexDto.class, List.of(tagging))).getErrors(),
                empty());
        verify(spexareService).deleteById(uuid(5));
        verify(taggingService).deleteById(uuid(5), uuid(3));
    }

    @Test
    void should_create_an_activity_once_for_several_rows_with_spex_task_and_actor() {
        when(activityService.create(uuid(5))).thenReturn(ActivityDto.builder().id(uuid(50)).build());
        when(spexActivityService.create(uuid(5), uuid(50), uuid(9))).thenReturn(SpexActivityDto.builder().id(uuid(60)).build());
        when(taskActivityService.create(eq(uuid(5)), eq(uuid(50)), any(UUID.class))).thenReturn(TaskActivityDto.builder().id(uuid(70)).build(), TaskActivityDto.builder().id(uuid(71)).build());

        final ActivityImpexDto actor = activity(uuid(10010), 3);
        final ActivityImpexDto orchestra = activity(uuid(10010), 4);

        actor.setActorAction(ImpexAction.CREATE);
        actor.setTypeId("VOCAL");
        actor.setActorRole("Ali Baba");
        orchestra.setTaskActivityId(uuid(10021));
        orchestra.setTaskId(uuid(2));
        orchestra.setSpexActivityAction(null);

        final ImportResultDto result = run(Map.of(ActivityImpexDto.class, List.of(actor, orchestra)));

        assertThat(result.getErrors(), empty());
        verify(activityService).create(uuid(5));
        verify(spexActivityService).create(uuid(5), uuid(50), uuid(9));
        verify(taskActivityService).create(uuid(5), uuid(50), uuid(1));
        verify(taskActivityService).create(uuid(5), uuid(50), uuid(2));
        verify(actorService).create(eq(uuid(5)), eq(uuid(50)), eq(uuid(70)), eq("VOCAL"), any());
    }

    @Test
    void should_delete_an_activity_without_touching_its_parts() {
        final ActivityImpexDto dto = activity(uuid(40), 2);

        dto.setAction(ImpexAction.DELETE);

        assertThat(run(Map.of(ActivityImpexDto.class, List.of(dto))).getErrors(), empty());
        verify(activityService).deleteById(uuid(5), uuid(40));
        verify(spexActivityService, never()).create(any(UUID.class), any(UUID.class), any(UUID.class));
    }

    @Test
    void should_report_the_row_of_a_failing_activity() {
        when(activityService.create(uuid(5))).thenThrow(new IllegalArgumentException("Spexet finns inte"));

        final ImportResultDto result = run(Map.of(ActivityImpexDto.class, List.of(activity(uuid(10010), 7))));

        assertThat(result.getErrors(), hasSize(1));
        assertThat(result.getErrors().getFirst().startsWith("Row 7, "), is(true));
    }
}

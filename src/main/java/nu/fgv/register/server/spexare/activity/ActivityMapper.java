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

package nu.fgv.register.server.spexare.activity;

import nu.fgv.register.server.settings.TypeDto;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.actor.Actor;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@MapperConfig(
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface ActivityMapper {

    ActivityMapper ACTIVITY_MAPPER = Mappers.getMapper(ActivityMapper.class);

    ActivityDto toDto(Activity model);

    @Mapping(target = "spexareId", source = "spexare.id")
    @Mapping(target = "id", source = "activity.id")
    @Mapping(target = "spexActivityId", source = "spexActivity.id")
    @Mapping(target = "spexId", source = "activity.spexActivity.spex.id")
    @Mapping(target = "spexYear", source = "activity.spexActivity.spex.year")
    @Mapping(target = "spexTitle", source = "activity.spexActivity.spex.details.title")
    @Mapping(target = "spexRevival", source = "activity.spexActivity.spex.revival")
    @Mapping(target = "spexCategoryName", source = "activity.spexActivity.spex.details.category.name")
    @Mapping(target = "taskActivityId", source = "taskActivity.id")
    @Mapping(target = "taskId", source = "taskActivity.task.id")
    @Mapping(target = "taskName", source = "taskActivity.task.name")
    @Mapping(target = "taskCategoryName", source = "taskActivity.task.category.name")
    @Mapping(target = "actorId", source = "actor.id")
    @Mapping(target = "actorRole", source = "actor.role")
    @Mapping(target = "typeId", source = "vocalType.id")
    @Mapping(target = "typeLabel", source = "vocalType.label")
    @Mapping(target = "createdBy", source = "activity.createdBy")
    @Mapping(target = "createdAt", source = "activity.createdAt")
    @Mapping(target = "lastModifiedBy", source = "activity.lastModifiedBy")
    @Mapping(target = "lastModifiedAt", source = "activity.lastModifiedAt")
    @Mapping(target = "action", expression = "java(nu.fgv.register.server.util.impex.model.ImpexAction.UPDATE)")
    @Mapping(target = "spexActivityAction", expression = "java(nu.fgv.register.server.util.impex.model.ImpexAction.UPDATE)")
    @Mapping(target = "taskActivityAction", expression = "java(nu.fgv.register.server.util.impex.model.ImpexAction.UPDATE)")
    @Mapping(target = "actorAction", expression = "java(nu.fgv.register.server.util.impex.model.ImpexAction.UPDATE)")
    @Mapping(target = "rowNumber", ignore = true)
    ActivityImpexDto toImpexDto(Spexare spexare, Activity activity, SpexActivity spexActivity, TaskActivity taskActivity, @Nullable Actor actor, @Nullable TypeDto vocalType);

}

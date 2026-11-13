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

package nu.fgv.register.server.mcp;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.mcp.McpToolSupport.PageResult;
import nu.fgv.register.server.task.TaskDto;
import nu.fgv.register.server.task.TaskService;
import nu.fgv.register.server.task.Task_;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.task.category.TaskCategoryService;
import nu.fgv.register.server.task.category.TaskCategory_;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static nu.fgv.register.server.mcp.McpToolSupport.FILTER_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.PAGE_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.SIZE_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.filter;
import static nu.fgv.register.server.mcp.McpToolSupport.orElse;
import static nu.fgv.register.server.mcp.McpToolSupport.pageable;

/**
 * The MCP tools for tasks and task categories.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class TaskMcpTools {

    private final TaskService taskService;
    private final TaskCategoryService categoryService;
    private final McpToolSupport support;

    @McpTool(name = "list_tasks", description = "List tasks (roles a person can have in a spex, e.g. director or musician), including their categories. Filterable fields: name, category.id, category.name. Read only: spex, revivals, categories and tasks are kept in Spexipedia, and changed there.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public PageResult<McpTask> listTasks(@McpToolParam(required = false, description = FILTER_DESCRIPTION) final @Nullable String filter,
                                         @McpToolParam(required = false, description = PAGE_DESCRIPTION) final @Nullable Integer page,
                                         @McpToolParam(required = false, description = SIZE_DESCRIPTION) final @Nullable Integer size) {
        return PageResult.of(taskService.find(filter(filter), pageable(page, size, Sort.by(Task_.NAME))), this::toMcpTask);
    }

    @McpTool(name = "get_task", description = "Get a task, including its category Read only: spex, revivals, categories and tasks are kept in Spexipedia, and changed there.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public McpTask getTask(@McpToolParam(description = "Task id") final UUID id) {
        return toMcpTask(taskService.findById(id));
    }

    @McpTool(name = "list_task_categories", description = "List task categories. Filterable fields: name, actorPresent. Read only: spex, revivals, categories and tasks are kept in Spexipedia, and changed there.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public PageResult<McpTaskCategory> listTaskCategories(@McpToolParam(required = false, description = FILTER_DESCRIPTION) final @Nullable String filter,
                                                          @McpToolParam(required = false, description = PAGE_DESCRIPTION) final @Nullable Integer page,
                                                          @McpToolParam(required = false, description = SIZE_DESCRIPTION) final @Nullable Integer size) {
        return PageResult.of(categoryService.find(filter(filter), pageable(page, size, Sort.by(TaskCategory_.NAME))), McpTaskCategory::of);
    }

    @McpTool(name = "get_task_category", description = "Get a task category Read only: spex, revivals, categories and tasks are kept in Spexipedia, and changed there.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public McpTaskCategory getTaskCategory(@McpToolParam(description = "Task category id") final UUID id) {
        return McpTaskCategory.of(categoryService.findById(id));
    }

    private McpTask toMcpTask(final TaskDto dto) {
        return new McpTask(dto.getId(), dto.getName(), categoryOf(dto.getId()));
    }

    private @Nullable McpTaskCategory categoryOf(final UUID taskId) {
        try {
            return McpTaskCategory.of(taskService.findCategoryByTask(taskId));
        } catch (final ResourceNoValueException _) {
            return null;
        }
    }

    public record McpTask(UUID id, String name, @Nullable McpTaskCategory category) {
    }

    public record McpTaskCategory(UUID id, String name, boolean actorPresent) {

        static McpTaskCategory of(final TaskCategoryDto dto) {
            return new McpTaskCategory(dto.getId(), dto.getName(), dto.getActorPresent());
        }
    }
}

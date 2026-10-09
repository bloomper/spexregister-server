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
import nu.fgv.register.server.task.TaskCreateDto;
import nu.fgv.register.server.task.TaskDto;
import nu.fgv.register.server.task.TaskService;
import nu.fgv.register.server.task.TaskUpdateDto;
import nu.fgv.register.server.task.Task_;
import nu.fgv.register.server.task.category.TaskCategoryCreateDto;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.task.category.TaskCategoryService;
import nu.fgv.register.server.task.category.TaskCategoryUpdateDto;
import nu.fgv.register.server.task.category.TaskCategory_;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import static nu.fgv.register.server.mcp.McpToolSupport.FILTER_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.PAGE_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.SIZE_DESCRIPTION;
import static nu.fgv.register.server.mcp.McpToolSupport.filter;
import static nu.fgv.register.server.mcp.McpToolSupport.orElse;
import static nu.fgv.register.server.mcp.McpToolSupport.pageable;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class TaskMcpTools {

    private final TaskService taskService;
    private final TaskCategoryService categoryService;
    private final McpToolSupport support;

    @McpTool(name = "list_tasks", description = "List tasks (roles a person can have in a spex, e.g. director or musician). Filterable fields: name.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public PageResult<McpTask> listTasks(@McpToolParam(required = false, description = FILTER_DESCRIPTION) final @Nullable String filter,
                                         @McpToolParam(required = false, description = PAGE_DESCRIPTION) final @Nullable Integer page,
                                         @McpToolParam(required = false, description = SIZE_DESCRIPTION) final @Nullable Integer size) {
        return PageResult.of(taskService.find(filter(filter), pageable(page, size, Sort.by(Task_.NAME))), McpTask::of);
    }

    @McpTool(name = "get_task", description = "Get a task, including its category",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public McpTaskDetails getTask(@McpToolParam(description = "Task id") final Long id) {
        return new McpTaskDetails(McpTask.of(taskService.findById(id)), categoryOf(id));
    }

    @McpTool(name = "create_task", description = "Create a task, optionally in a task category. Requires the ADMIN role.",
            annotations = @McpAnnotations(destructiveHint = false, openWorldHint = false))
    public McpTaskDetails createTask(@McpToolParam(description = "Task name") final String name,
                                     @McpToolParam(required = false, description = "Id of the task category to put the task in") final @Nullable Long categoryId) {
        support.audit("create_task");
        final TaskDto task = taskService.create(support.validate(TaskCreateDto.builder().name(name).build()));

        if (categoryId != null) {
            taskService.addCategory(task.getId(), categoryId);
        }

        return getTask(task.getId());
    }

    @McpTool(name = "update_task", description = "Rename a task. Requires the ADMIN or EDITOR role.",
            annotations = @McpAnnotations(idempotentHint = true, openWorldHint = false))
    public McpTask updateTask(@McpToolParam(description = "Task id") final Long id,
                              @McpToolParam(description = "New task name") final String name) {
        support.audit("update_task");

        return McpTask.of(taskService.update(support.validate(new TaskUpdateDto(id, name))));
    }

    @McpTool(name = "set_task_category", description = "Set or clear the category of a task. Requires the ADMIN role.",
            annotations = @McpAnnotations(idempotentHint = true, openWorldHint = false))
    public McpTaskDetails setTaskCategory(@McpToolParam(description = "Task id") final Long id,
                                          @McpToolParam(required = false, description = "Task category id, omit to remove the category") final @Nullable Long categoryId) {
        support.audit("set_task_category");

        if (categoryId != null) {
            taskService.addCategory(id, categoryId);
        } else {
            taskService.removeCategory(id);
        }

        return getTask(id);
    }

    @McpTool(name = "delete_task", description = "Permanently delete a single task. Requires the ADMIN role.",
            annotations = @McpAnnotations(openWorldHint = false))
    public String deleteTask(@McpToolParam(description = "Task id") final Long id) {
        support.audit("delete_task");
        taskService.deleteById(id);

        return "Deleted task %d".formatted(id);
    }

    @McpTool(name = "list_task_categories", description = "List task categories. Filterable fields: name, actorPresent.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public PageResult<McpTaskCategory> listTaskCategories(@McpToolParam(required = false, description = FILTER_DESCRIPTION) final @Nullable String filter,
                                                          @McpToolParam(required = false, description = PAGE_DESCRIPTION) final @Nullable Integer page,
                                                          @McpToolParam(required = false, description = SIZE_DESCRIPTION) final @Nullable Integer size) {
        return PageResult.of(categoryService.find(filter(filter), pageable(page, size, Sort.by(TaskCategory_.NAME))), McpTaskCategory::of);
    }

    @McpTool(name = "get_task_category", description = "Get a task category",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public McpTaskCategory getTaskCategory(@McpToolParam(description = "Task category id") final Long id) {
        return McpTaskCategory.of(categoryService.findById(id));
    }

    @McpTool(name = "create_task_category", description = "Create a task category. Requires the ADMIN role.",
            annotations = @McpAnnotations(destructiveHint = false, openWorldHint = false))
    public McpTaskCategory createTaskCategory(@McpToolParam(description = "Category name") final String name,
                                              @McpToolParam(required = false, description = "Whether tasks in this category are performed on stage as an actor, defaults to false") final @Nullable Boolean actorPresent) {
        support.audit("create_task_category");

        return McpTaskCategory.of(categoryService.create(support.validate(new TaskCategoryCreateDto(name, orElse(actorPresent, false)))));
    }

    @McpTool(name = "update_task_category", description = "Update a task category; omitted fields are left unchanged. Requires the ADMIN role.",
            annotations = @McpAnnotations(idempotentHint = true, openWorldHint = false))
    public McpTaskCategory updateTaskCategory(@McpToolParam(description = "Task category id") final Long id,
                                              @McpToolParam(required = false, description = "New name") final @Nullable String name,
                                              @McpToolParam(required = false, description = "Whether tasks in this category are performed on stage as an actor") final @Nullable Boolean actorPresent) {
        support.audit("update_task_category");
        final TaskCategoryDto current = categoryService.findById(id);

        return McpTaskCategory.of(categoryService.update(support.validate(new TaskCategoryUpdateDto(
                id,
                orElse(name, current.getName()),
                orElse(actorPresent, Boolean.TRUE.equals(current.getActorPresent()))
        ))));
    }

    @McpTool(name = "delete_task_category", description = "Permanently delete a single task category. Requires the ADMIN role.",
            annotations = @McpAnnotations(openWorldHint = false))
    public String deleteTaskCategory(@McpToolParam(description = "Task category id") final Long id) {
        support.audit("delete_task_category");
        categoryService.deleteById(id);

        return "Deleted task category %d".formatted(id);
    }

    private @Nullable McpTaskCategory categoryOf(final Long taskId) {
        try {
            return McpTaskCategory.of(taskService.findCategoryByTask(taskId));
        } catch (final ResourceNoValueException _) {
            return null;
        }
    }

    public record McpTask(Long id, String name) {

        static McpTask of(final TaskDto dto) {
            return new McpTask(dto.getId(), dto.getName());
        }
    }

    public record McpTaskDetails(McpTask task, @Nullable McpTaskCategory category) {
    }

    public record McpTaskCategory(Long id, String name, boolean actorPresent) {

        static McpTaskCategory of(final TaskCategoryDto dto) {
            return new McpTaskCategory(dto.getId(), dto.getName(), Boolean.TRUE.equals(dto.getActorPresent()));
        }
    }
}

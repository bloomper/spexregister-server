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

package nu.fgv.register.server.spexare.bulk;

/**
 * The changes a bulk request can make. {@link #SPEX_ADD} adds an activity in each spex a spexare lacks and
 * {@link #SPEX_REMOVE} removes the activity with its tasks; {@link #TASK_ADD} adds tasks to the activity in one spex,
 * adding the activity when needed.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public enum SpexareBulkOperation {

    TAG_ADD,
    TAG_REMOVE,
    SPEX_ADD,
    SPEX_REMOVE,
    TASK_ADD,
    TASK_REMOVE,
    CONSENT_SET,
    TOGGLE_SET,
    FIELDS_SET
}

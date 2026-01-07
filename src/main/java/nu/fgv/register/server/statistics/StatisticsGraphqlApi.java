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

package nu.fgv.register.server.statistics;

import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Controller
@RequiredArgsConstructor
public class StatisticsGraphqlApi {

    private final StatisticsService service;

    @QueryMapping("statistics")
    @RequiresAdminOrEditorOrUser
    public StatisticsDto getStatistics() {
        return service.getStatistics();
    }

}

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

package nu.fgv.register.server.audit;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.language.OperationDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class AuditOperationInstrumentation extends SimplePerformantInstrumentation {

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(final InstrumentationExecuteOperationParameters parameters,
                                                                        final InstrumentationState state) {
        final AuditContext.Origin origin = AuditContext.current();
        final OperationDefinition operation = parameters.getExecutionContext().getOperationDefinition();
        final String name = operation == null ? null : operation.getName();

        if (origin != null && name != null && !name.isBlank()) {
            AuditContext.set(origin.withOperation(name));
        }

        return SimpleInstrumentationContext.noOp();
    }
}

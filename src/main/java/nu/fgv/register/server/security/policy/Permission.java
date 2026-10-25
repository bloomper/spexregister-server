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

package nu.fgv.register.server.security.policy;

/**
 * What a caller may do with an entity. {@link #ADMINISTRATION} includes all the others.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public enum Permission {
    READ,
    WRITE,
    DELETE,
    ADMINISTRATION;

    /**
     * Whether holding a permission gives this one: the same one, {@link #ADMINISTRATION}, or any permission at all when
     * this is {@link #READ}.
     *
     * @param granted the permission held
     * @return whether it gives this permission
     */
    public boolean isMetBy(final Permission granted) {
        return granted == this || granted == ADMINISTRATION || this == READ;
    }
}

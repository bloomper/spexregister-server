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


package nu.fgv.register.server.map;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A spexare on the map.
 *
 * @param id        the spexare id
 * @param firstName the first name
 * @param lastName  the last name
 * @param nickName  the nickname
 * @param latitude  the latitude of their address
 * @param longitude the longitude of their address
 */
public record SpexareLocationDto(UUID id, @Nullable String firstName, @Nullable String lastName,
                                 @Nullable String nickName, double latitude, double longitude) {
}

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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * The geocoded locations of spexare.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public interface SpexareLocationRepository extends JpaRepository<SpexareLocation, Long> {

    List<SpexareLocation> findBySpexareIdInAndLatitudeIsNotNullAndLongitudeIsNotNull(Collection<Long> spexareIds);

    @Modifying
    @Query("delete from SpexareLocation l where l.spexareId not in :spexareIds")
    int deleteBySpexareIdNotIn(Collection<Long> spexareIds);
}

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

package nu.fgv.register.server.user.state;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

import static nu.fgv.register.server.user.state.StateMapper.STATE_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class StateService {

    private final StateRepository repository;

    @RequiresAdminOrEditorOrUser
    public List<StateDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream().map(STATE_MAPPER::toDto)
                .toList();
    }

    @RequiresAdminOrEditorOrUser
    public StateDto findById(final String id) {
        return repository
                .findById(id)
                .map(STATE_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(State.class, id));
    }

    @RequiresAdminOrEditorOrUser
    public boolean exists(final String id) {
        return repository
                .findById(id)
                .isPresent();
    }

}

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

package nu.fgv.register.server.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.settings.TypeMapper.TYPE_MAPPER;
import static nu.fgv.register.server.settings.TypeSpecification.hasType;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TypeService {

    private final TypeRepository repository;

    public List<TypeDto> findAll() {
        return repository
                .findAll()
                .stream()
                .map(TYPE_MAPPER::toDto)
                .toList();
    }

    public List<TypeDto> findByType(final TypeType type) {
        return repository
                .findAll(hasType(type))
                .stream()
                .map(TYPE_MAPPER::toDto)
                .toList();
    }

    public Optional<TypeDto> findById(final String id) {
        return repository
                .findById(id)
                .map(TYPE_MAPPER::toDto);
    }

    public boolean existsByIdAndType(final String id, final TypeType type) {
        return repository
                .findOne(TypeSpecification.hasId(id).and(TypeSpecification.hasType(type)))
                .map(t -> Boolean.TRUE)
                .orElse(Boolean.FALSE);

    }

}

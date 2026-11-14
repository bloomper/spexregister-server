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

package nu.fgv.register.server.spexare.activity.task.actor;

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.character.SpexCharacter;
import nu.fgv.register.server.spex.character.SpexCharacterRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.sync.SyncService;
import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Actors' characters, from Spexipedia's characters in Spexregister's copy (spexregister-spec,
 * docs/base-data-and-memberships.md, section 4). An actor's character belongs to the activity's spex, or to its parent
 * for a revival. Actors without one are linked by their free-text role to the character of the same name, ignoring
 * case, diacritics and spacing, after each sync that changed something.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class ActorCharacterService {

    private final ActorRepository actorRepository;
    private final SpexCharacterRepository characterRepository;
    private final SpexRepository spexRepository;
    private final PermissionService permissionService;
    private final JdbcClient jdbc;

    public ActorCharacterService(final ActorRepository actorRepository, final SpexCharacterRepository characterRepository,
                                 final SpexRepository spexRepository, final PermissionService permissionService,
                                 final JdbcClient jdbc) {
        this.actorRepository = actorRepository;
        this.characterRepository = characterRepository;
        this.spexRepository = spexRepository;
        this.permissionService = permissionService;
        this.jdbc = jdbc;
    }

    static String fold(final @Nullable String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFKD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "");
    }

    /**
     * The characters an actor of the spex may play: the spex's own and, for a revival, its parent's, by position.
     */
    @Transactional(readOnly = true)
    public List<CharacterDto> charactersOf(final UUID spexId) {
        final Spex spex = spexRepository.findByUuid0(spexId).orElseThrow(() -> new ResourceNotFoundException(Spex.class, spexId));
        final List<Spex> productions = new ArrayList<>(List.of(spex));

        if (spex.getParent() != null) {
            productions.add(spex.getParent());
        }
        return characterRepository.findBySpexInOrderByPositionAsc(productions).stream().map(ActorCharacterService::toDto).toList();
    }

    /**
     * Sets or clears the actor's character, which must belong to the activity's spex or its parent.
     */
    @Transactional
    public void setCharacter(final UUID actorId, final @Nullable UUID characterId) {
        final Actor actor = actorRepository.findByUuid(actorId).orElseThrow(() -> new ResourceNotFoundException(Actor.class, actorId));

        permissionService.checkWritePermission(actor.getTaskActivity().getActivity().getSpexare());
        if (characterId == null) {
            actor.setCharacter(null);
            return;
        }

        final SpexCharacter character = characterRepository.findByUuid(characterId)
                .orElseThrow(() -> new ResourceNotFoundException(SpexCharacter.class, characterId));
        final Spex spex = actor.getTaskActivity().getActivity().getSpexActivity() == null ? null
                : actor.getTaskActivity().getActivity().getSpexActivity().getSpex();

        if (spex == null || !(character.getSpex().equals(spex) || character.getSpex().equals(spex.getParent()))) {
            throw new BadRequestException("The character belongs to another spex");
        }
        actor.setCharacter(character);
    }

    @Transactional(readOnly = true)
    public Map<UUID, CharacterDto> charactersByActors(final Collection<UUID> actorIds) {
        final Map<UUID, CharacterDto> result = new HashMap<>();

        actorRepository.findByUuidIn(actorIds).forEach(actor -> {
            if (actor.getCharacter() != null) {
                result.put(actor.getUuid(), toDto(actor.getCharacter()));
            }
        });
        return result;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onSynced(final SyncService.BaseDataSyncedEvent event) {
        linkByRole();
    }

    /**
     * Links each actor without a character to the character of the activity's spex, or its parent, whose name equals
     * the actor's role, ignoring case, diacritics and spacing.
     *
     * @return how many were linked
     */
    @Transactional
    public int linkByRole() {
        final List<Object[]> unlinked = jdbc.sql("""
                        SELECT a.id, a.role, s.id, s.parent_id FROM actor a
                        JOIN task_activity ta ON ta.id = a.task_activity_id
                        JOIN spex_activity sa ON sa.activity_id = ta.activity_id
                        JOIN spex s ON s.id = sa.spex_id
                        WHERE a.character_id IS NULL AND a.role IS NOT NULL AND a.role <> ''""")
                .query((rs, _) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getObject(4)})
                .list();
        final Map<Long, Map<String, List<Long>>> bySpex = new HashMap<>();
        int linked = 0;

        for (final Object[] row : unlinked) {
            final List<Long> candidates = new ArrayList<>(names(bySpex, (Long) row[2]).getOrDefault(fold((String) row[1]), List.of()));

            if (row[3] != null) {
                candidates.addAll(names(bySpex, ((Number) row[3]).longValue()).getOrDefault(fold((String) row[1]), List.of()));
            }
            if (candidates.size() == 1) {
                jdbc.sql("UPDATE actor SET character_id = :c WHERE id = :a").param("c", candidates.getFirst()).param("a", row[0]).update();
                linked++;
            }
        }
        if (linked > 0) {
            log.info("Linked {} actors to characters by their roles; {} are still without one", linked, unlinked.size() - linked);
        }
        return linked;
    }

    private Map<String, List<Long>> names(final Map<Long, Map<String, List<Long>>> bySpex, final long spexKey) {
        return bySpex.computeIfAbsent(spexKey, key -> {
            final Map<String, List<Long>> names = new HashMap<>();

            jdbc.sql("SELECT id, name FROM spex_character WHERE spex_id = :s AND retired = 0").param("s", key)
                    .query((rs, _) -> Map.entry(fold(rs.getString(2)), rs.getLong(1))).list()
                    .forEach(entry -> names.computeIfAbsent(entry.getKey(), _ -> new ArrayList<>()).add(entry.getValue()));
            return names;
        });
    }

    private static CharacterDto toDto(final SpexCharacter character) {
        return new CharacterDto(character.getUuid(), character.getName(), character.getPosition(), character.isRetired(),
                character.getSpex().getUuid());
    }

    /**
     * A character as Spexregister shows it.
     */
    public record CharacterDto(UUID id, String name, int position, boolean retired, UUID spexId) {
    }
}

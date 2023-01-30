package nu.fgv.register.server.spex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:mysql:8.0.32:///test_database"
})
public class SpexRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private SpexRepository repository;

    @Test
    void whenFindAllByParentIsNull_thenReturnOnlyNonRevivals() {
        // given
        final SpexCategory category = new SpexCategory();
        category.setFirstYear("1996");
        category.setName("category");
        category.setCreatedBy("test");
        final SpexDetails details = new SpexDetails();
        details.setTitle("Nobel");
        details.setCreatedBy("test");
        details.setCategory(category);
        final Spex spex = new Spex();
        spex.setYear("1996");
        spex.setDetails(details);
        spex.setCreatedBy("test");
        final Spex revival = new Spex();
        revival.setYear("2006");
        revival.setDetails(details);
        revival.setParent(spex);
        revival.setCreatedBy("test");
        entityManager.persist(category);
        entityManager.persist(details);
        entityManager.persist(spex);
        entityManager.persist(revival);
        entityManager.flush();

        // when
        final Page<Spex> found = repository.findAllByParentIsNull(Pageable.unpaged());

        // then
        assertThat(found.getTotalElements(), is(1L));
        assertThat(found.getContent().get(0).getYear(), is(spex.getYear()));
        assertThat(found.getContent().get(0).getDetails().getTitle(), is(spex.getDetails().getTitle()));
    }

    @Test
    void whenFindAllByParentIsNotNull_thenReturnOnlyRevivals() {
        // given
        final SpexCategory category = new SpexCategory();
        category.setFirstYear("1996");
        category.setName("category");
        category.setCreatedBy("test");
        final SpexDetails details = new SpexDetails();
        details.setTitle("Nobel");
        details.setCreatedBy("test");
        details.setCategory(category);
        final Spex spex = new Spex();
        spex.setYear("1996");
        spex.setDetails(details);
        spex.setCreatedBy("test");
        final Spex revival = new Spex();
        revival.setYear("2006");
        revival.setDetails(details);
        revival.setParent(spex);
        revival.setCreatedBy("test");

        entityManager.persist(category);
        entityManager.persist(details);
        entityManager.persist(spex);
        entityManager.persist(revival);
        entityManager.flush();

        // when
        final Page<Spex> found = repository.findAllByParentIsNotNull(Pageable.unpaged());

        // then
        assertThat(found.getTotalElements(), is(1L));
        assertThat(found.getContent().get(0).getYear(), is(revival.getYear()));
        assertThat(found.getContent().get(0).getDetails().getTitle(), is(revival.getDetails().getTitle()));
    }

    @Test
    void whenFindRevivalsByParent_thenReturnItsRevivals() {
        // given
        final SpexCategory category = new SpexCategory();
        category.setFirstYear("1996");
        category.setName("category");
        category.setCreatedBy("test");
        final SpexDetails details = new SpexDetails();
        details.setTitle("Nobel");
        details.setCreatedBy("test");
        details.setCategory(category);
        final Spex spex = new Spex();
        spex.setYear("1996");
        spex.setDetails(details);
        spex.setCreatedBy("test");
        final Spex revival = new Spex();
        revival.setYear("2006");
        revival.setDetails(details);
        revival.setParent(spex);
        revival.setCreatedBy("test");
        entityManager.persist(category);
        entityManager.persist(details);
        entityManager.persist(spex);
        entityManager.persist(revival);
        entityManager.flush();

        // when
        final Page<Spex> found = repository.findRevivalsByParent(spex, Pageable.unpaged());

        // then
        assertThat(found.getTotalElements(), is(1L));
        assertThat(found.getContent().get(0).getYear(), is(revival.getYear()));
        assertThat(found.getContent().get(0).getDetails().getTitle(), is(revival.getDetails().getTitle()));
    }

    @Test
    void whenFindAllRevivalsByParent_thenReturnItsRevivals() {
        // given
        final SpexCategory category = new SpexCategory();
        category.setFirstYear("1996");
        category.setName("category");
        category.setCreatedBy("test");
        final SpexDetails details = new SpexDetails();
        details.setTitle("Nobel");
        details.setCreatedBy("test");
        details.setCategory(category);
        final Spex spex = new Spex();
        spex.setYear("1996");
        spex.setDetails(details);
        spex.setCreatedBy("test");
        final Spex revival = new Spex();
        revival.setYear("2006");
        revival.setDetails(details);
        revival.setParent(spex);
        revival.setCreatedBy("test");
        entityManager.persist(category);
        entityManager.persist(details);
        entityManager.persist(spex);
        entityManager.persist(revival);
        entityManager.flush();

        // when
        final List<Spex> found = repository.findAllRevivalsByParent(spex);

        // then
        assertThat(found.size(), is(1));
        assertThat(found.get(0).getYear(), is(revival.getYear()));
        assertThat(found.get(0).getDetails().getTitle(), is(revival.getDetails().getTitle()));
    }

    @Test
    void whenExistsRevivalByParentAndYear_thenReturnTrueIfSuchExists() {
        // given
        final SpexCategory category = new SpexCategory();
        category.setFirstYear("1996");
        category.setName("category");
        category.setCreatedBy("test");
        final SpexDetails details = new SpexDetails();
        details.setTitle("Nobel");
        details.setCreatedBy("test");
        details.setCategory(category);
        final Spex spex = new Spex();
        spex.setYear("1996");
        spex.setDetails(details);
        spex.setCreatedBy("test");
        final Spex revival = new Spex();
        revival.setYear("2006");
        revival.setDetails(details);
        revival.setParent(spex);
        revival.setCreatedBy("test");
        entityManager.persist(category);
        entityManager.persist(details);
        entityManager.persist(spex);
        entityManager.persist(revival);
        entityManager.flush();

        // when
        final boolean found1 = repository.existsRevivalByParentAndYear(spex, "2006");
        final boolean found2 = repository.existsRevivalByParentAndYear(spex, "2007");

        // then
        assertThat(found1, is(true));
        assertThat(found2, is(false));
    }

    @Test
    void whenFindByParentAndYear_thenReturnItIfExists() {
        // given
        final SpexCategory category = new SpexCategory();
        category.setFirstYear("1996");
        category.setName("category");
        category.setCreatedBy("test");
        final SpexDetails details = new SpexDetails();
        details.setTitle("Nobel");
        details.setCreatedBy("test");
        details.setCategory(category);
        final Spex spex = new Spex();
        spex.setYear("1996");
        spex.setDetails(details);
        spex.setCreatedBy("test");
        final Spex revival = new Spex();
        revival.setYear("2006");
        revival.setDetails(details);
        revival.setParent(spex);
        revival.setCreatedBy("test");
        entityManager.persist(category);
        entityManager.persist(details);
        entityManager.persist(spex);
        entityManager.persist(revival);
        entityManager.flush();

        // when
        final Optional<Spex> found1 = repository.findRevivalByParentAndYear(spex, "2006");
        final Optional<Spex> found2 = repository.findRevivalByParentAndYear(spex, "2007");

        // then
        assertThat(found1.isPresent(), is(true));
        assertThat(found1.get().getYear(), is(revival.getYear()));
        assertThat(found1.get().getDetails().getTitle(), is(revival.getDetails().getTitle()));
        assertThat(found2.isPresent(), is(false));
    }
}

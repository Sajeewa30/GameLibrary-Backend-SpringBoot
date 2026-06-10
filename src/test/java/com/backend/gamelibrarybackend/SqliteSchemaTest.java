package com.backend.gamelibrarybackend;

import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.backend.gamelibrarybackend.repository.GameItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the entity, its @ElementCollection tables (gallery/videos) and a
 * derived query all map and run against a real SQLite file using the community
 * dialect — i.e. that the MySQL -> SQLite switch actually works. Uses the JPA
 * slice so it does not require Firebase or the web layer.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:./build/test-gamelibrary.db",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create"
})
class SqliteSchemaTest {

    @Autowired
    private GameItemRepository repository;

    @Test
    void persistsGameWithGalleryAndVideosAndQueriesItBack() {
        GameItemEntity game = new GameItemEntity(
                "The Legend of Zelda: BotW", 2017, 2018,
                true, false, true,
                "First playthrough", "https://img/zelda.png", "user-1");
        game.getGallery().add("https://img/g1.png");
        game.getVideos().add("https://img/v1.mp4");

        repository.save(game);

        assertThat(repository.countByUserId("user-1")).isEqualTo(1);

        List<GameItemEntity> favourites =
                repository.findByUserIdAndIsFavouriteTrueOrderByCreatedAtDesc("user-1");
        assertThat(favourites).hasSize(1);
        GameItemEntity loaded = favourites.get(0);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getGallery()).containsExactly("https://img/g1.png");
        assertThat(loaded.getVideos()).containsExactly("https://img/v1.mp4");

        // The custom JPQL query (used by the "games by year" screen) must run on SQLite too.
        assertThat(repository.findCompletedByYearWithFallback("user-1", 2018)).hasSize(1);
    }
}

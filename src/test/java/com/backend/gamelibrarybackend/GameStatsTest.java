package com.backend.gamelibrarybackend;

import com.backend.gamelibrarybackend.controllers.GameAdminController;
import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.backend.gamelibrarybackend.repository.GameItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameStatsTest {

    @Test
    void countsTotalsAndCompletedGamesPerYearNewestFirst() {
        GameItemRepository repository = mock(GameItemRepository.class);
        when(repository.findByUserIdOrderByCreatedAtDesc("u")).thenReturn(List.of(
                new GameItemEntity("A", 2023, 2024, true, true, true, "", "", "u"),
                new GameItemEntity("B", 2024, 0, true, false, false, "", "", "u"),   // falls back to year
                new GameItemEntity("C", 2022, 2023, true, false, true, "", "", "u"),
                new GameItemEntity("D", 2025, 0, false, false, false, "", "", "u")   // backlog
        ));
        GameAdminController controller = new GameAdminController();
        ReflectionTestUtils.setField(controller, "gameItemRepository", repository);

        Map<String, Object> stats = controller.getStats("u");

        assertThat(stats).containsEntry("total", 4L)
                .containsEntry("completed", 3L)
                .containsEntry("backlog", 1L)
                .containsEntry("hundredPercent", 1L)
                .containsEntry("favourites", 2L);
        assertThat(stats.get("byYear")).isEqualTo(List.of(
                Map.of("year", 2024, "count", 2L),
                Map.of("year", 2023, "count", 1L)));
    }
}

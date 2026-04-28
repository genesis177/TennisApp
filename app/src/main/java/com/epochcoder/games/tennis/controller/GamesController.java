package com.epochcoder.games.tennis.controller;

import com.epochcoder.games.tennis.domain.GameDay;
import com.epochcoder.games.tennis.domain.Player;
import com.epochcoder.games.tennis.domain.PlayerGroup;
import com.epochcoder.games.tennis.domain.Team;
import com.epochcoder.games.tennis.spec.handler.GamesApi;
import com.epochcoder.games.tennis.spec.model.GamesResponse;
import com.epochcoder.games.tennis.spec.model.Interval;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@RestController
public class GamesController implements GamesApi {

    private static final Logger log =
        LoggerFactory.getLogger(GamesController.class);

    private final ModelMapper mapper;

    public static final int MAX_PLAYERS_PER_TEAM = 4;

    public GamesController(ModelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @CrossOrigin
    public ResponseEntity<GamesResponse> generateGames(
        @NotNull @Valid Integer courts,
        @NotNull @Valid String interval,
        @NotNull @Valid List<String> groupA,
        @NotNull @Valid List<String> groupB,
        @Valid Integer games,
        @Valid LocalDate date
    ) {

        long start = System.currentTimeMillis();

        List<Player> malePlayers =
            Player.toPlayers(PlayerGroup.A, groupA.toArray(new String[0]));

        List<Player> femalePlayers =
            Player.toPlayers(PlayerGroup.B, groupB.toArray(new String[0]));

        if (malePlayers.size() > MAX_PLAYERS_PER_TEAM ||
            femalePlayers.size() > MAX_PLAYERS_PER_TEAM) {
            return ResponseEntity.badRequest().build();
        }

        Set<Team> teams = Team.makeTeams(malePlayers, femalePlayers);

        List<GameDay> allGameDays =
            GameDay.buildGameDays(getInterval(interval), date, teams, courts);

        List<GameDay> gameDays = (games == null)
            ? allGameDays
            : allGameDays.subList(0, Math.min(games, allGameDays.size()));

        log.info("Possible to play {}/{} times, took: {}ms",
            gameDays.size(),
            allGameDays.size(),
            System.currentTimeMillis() - start
        );

        return ResponseEntity.ok(createResponse(courts, interval, gameDays));
    }

    private ChronoUnit getInterval(String interval) {
        return switch (interval) {
            case "WEEKS" -> ChronoUnit.WEEKS;
            case "MONTHS" -> ChronoUnit.MONTHS;
            default -> throw new IllegalArgumentException("Unknown interval: " + interval);
        };
    }

    private GamesResponse createResponse(
        Integer courts,
        String matchInterval,
        List<GameDay> gameDays
    ) {

        GamesResponse response = new GamesResponse();
        response.setIntervalType(matchInterval);
        response.setCourts(courts);

        for (GameDay gameDay : gameDays) {

            Interval interval = new Interval()
                .id(UUID.randomUUID());

            mapper.map(gameDay, interval);

            if (interval.getMatches() != null) {
                IntStream.range(0, courts).forEach(i -> {
                    if (i < interval.getMatches().size()
                        && interval.getMatches().get(i) != null) {
                        interval.getMatches().get(i).court(i + 1);
                    }
                });
            }

            response.addIntervalsItem(interval);
        }

        return response;
    }
}

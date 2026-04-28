package com.epochcoder.games.tennis.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Game {

    public abstract Set<Match> getMatches();

    public Set<Team> getTeams() {
        return Match.getTeams(this.getMatches());
    }

    public boolean hasMatch(final Match match) {
        final Set<Match> matches = this.getMatches();
        return matches.contains(match) || matches.stream()
            .anyMatch(possibleMirroredMatch -> possibleMirroredMatch.isMirroredMatch(match));
    }

    public boolean didNotPlayAnyMatch(final Collection<Match> playedMatches) {
        final Set<Match> matches = this.getMatches();
        return playedMatches.stream().noneMatch(matches::contains);
    }

    public boolean hasTeamFromMatches(final Collection<Match> playedMatches) {
        final Set<Team> theseTeams = this.getTeams();
        final Set<Team> thoseTeams = Match.getTeams(playedMatches);

        return theseTeams.stream().anyMatch(thoseTeams::contains);
    }

    public boolean hasTeamFromGame(final Game playerGame) {
        final Set<Team> theseTeams = this.getTeams();
        final Set<Team> thoseTeams = playerGame.getTeams();

        return theseTeams.stream().anyMatch(thoseTeams::contains);
    }

    public static List<Game> findGames(
        final Set<Team> teams, final Set<Player> players, final Set<Match> matchSet, final int i) {
        final List<Game> orderedGames = new ArrayList<>();
        final Set<Player> usedPlayers = matchSet.stream()
            .flatMap(m -> m.getPlayers().stream())
            .collect(Collectors.toSet());

        if (usedPlayers.containsAll(players)) {
            orderedGames.add(ImmutableGame.builder().matches(matchSet).build());
            return orderedGames;
        }

        final List<Team> teamsWithUnusedPlayers = Team.getTeamsWithUnusedPlayers(teams, usedPlayers);
        for (final Team team1 : teamsWithUnusedPlayers) {
            for (final Team team2 : teamsWithUnusedPlayers) {
                if (team1.equals(team2) || team1.hasPlayerFromTeam(team2)) {
                    continue;
                }

                final Match match = ImmutableMatch.builder().teamA(team1).teamB(team2).build();
                if (orderedGames.stream().noneMatch(game -> game.hasMatch(match))) {
                    final LinkedHashSet<Match> subMatches = new LinkedHashSet<>(matchSet);
                    subMatches.add(match);

                    final List<Game> games = findGames(teams, players, subMatches, i + 1);
                    if (!games.isEmpty()) {
                        orderedGames.addAll(games);
                    }
                }
            }
        }

        return orderedGames;
    }

    public static Optional<Game> findGameForTeam(
        final Team team, final Set<Team> lastRoundTeams,
        final List<Match> roundMatches, final List<Game> games) {
        return games.stream()
            .filter(game -> game.getTeams().stream().anyMatch(team::equals))
            .filter(game -> game.didNotPlayAnyMatch(roundMatches))
            .filter(game -> !game.hasTeamFromMatches(roundMatches))
            .filter(game -> game.getTeams().stream().noneMatch(lastRoundTeams::contains))
            .findFirst();
    }
}

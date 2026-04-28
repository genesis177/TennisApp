package com.epochcoder.games.tennis.domain;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Value.Immutable
public abstract class Match {

    private static final Logger log = LoggerFactory.getLogger(Match.class);

    public abstract Team getTeamA();

    public abstract Team getTeamB();

    public boolean hasTeamFromMatch(final Match otherMatch) {
        return this.hasTeam(otherMatch.getTeamA()) || this.hasTeam(otherMatch.getTeamB());
    }

    public boolean hasTeam(final Team team) {
        return (this.getTeamA().equals(team) || this.getTeamA().isMirroredTeam(team))
            || (this.getTeamB().equals(team) || this.getTeamB().isMirroredTeam(team));
    }

    public Set<Team> getTeams() {
        return Set.of(this.getTeamA(), this.getTeamB());
    }

    public Set<Player> getPlayers() {
        return this.getTeams().stream()
            .flatMap(t -> t.getPlayers().stream())
            .collect(Collectors.toSet());
    }

    public boolean isMirroredMatch(final Match match) {
        return this.hasTeam(match.getTeamA()) && this.hasTeam(match.getTeamB());
    }

    @Override
    public String toString() {
        return "Match(" +
            this.getTeamA() + " vs " + this.getTeamB() +
            ')';
    }

    public static List<Match> teamBasedMatchOrdering(final List<Game> games, final List<Team> teams) {
        final List<Match> matches = new ArrayList<>();
        final List<Match> roundMatches = new ArrayList<>();
        final LinkedList<Team> remainingTeams = new LinkedList<>();
        final Set<Team> lastRoundTeams = new HashSet<>();

        Collections.shuffle(teams);

        final Map<Team, Integer> playCount = new LinkedHashMap<>();
        teams.forEach(t -> playCount.putIfAbsent(t, 0));

        while (!games.isEmpty()) {
            final Comparator<Team> byPlayCount = Comparator.comparing(playCount::get);

            if (remainingTeams.isEmpty()) {
                remainingTeams.addAll(teams);
                remainingTeams.sort(byPlayCount);

                roundMatches.clear();
            }

            final Team leastPlayedTeam = remainingTeams.poll();
            final Optional<Game> optionalGame = Game.findGameForTeam(
                leastPlayedTeam, lastRoundTeams, roundMatches, games);

            optionalGame.ifPresent(game -> {
                matches.addAll(game.getMatches());
                roundMatches.addAll(game.getMatches());

                lastRoundTeams.clear();
                lastRoundTeams.addAll(game.getTeams());

                game.getTeams().forEach(playedTeam -> playCount.merge(playedTeam, 1, Integer::sum));

                remainingTeams.removeIf(game.getTeams()::contains);
                games.remove(game);

                remainingTeams.sort(byPlayCount);
            });

            if (remainingTeams.isEmpty() && roundMatches.isEmpty()) {
                log.debug("No possible match found at {} remaining games", games.size());
                break;
            }
        }

        return matches;
    }

    public static Set<Team> getTeams(final Collection<Match> matches) {
        return matches.stream()
            .flatMap(match -> match.getTeams().stream())
            .collect(Collectors.toSet());
    }

    public static void checkNextMatches(final List<Match> matches) {
        log.debug("Checking {} matches", matches.size());
        for (int i = 0; i < matches.size(); i++) {
            Match currentMatch = matches.get(i);
            log.debug(i + ":\t" + currentMatch);
            if (i < matches.size() - 1) {
                final Match nextMatch = matches.get(i + 1);
                if (currentMatch.hasTeamFromMatch(nextMatch)) {
                    log.debug("\t\t (next match has team)");
                    throw new IllegalStateException("Matches are not in optimal order!");
                }
            }
        }
    }
}

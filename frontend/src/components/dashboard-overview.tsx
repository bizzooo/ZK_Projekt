import {
  Activity,
  DatabaseZap,
  Swords,
  Trophy,
  UsersRound
} from "lucide-react";
import Link from "next/link";

import { AnalyticsOverview } from "@/components/analytics-overview";
import { BracketView } from "@/components/bracket-view";
import { MatchImportPanel } from "@/components/match-import-panel";
import { MatchSchedule } from "@/components/match-schedule";
import { MetricCard } from "@/components/metric-card";
import { SectionHeader } from "@/components/section-header";
import { TournamentCard } from "@/components/tournament-card";
import {
  getAnalytics,
  getMatches,
  getTournaments
} from "@/lib/data";

export async function DashboardOverview() {
  const [analytics, matches, tournaments] = await Promise.all([
    getAnalytics(),
    getMatches(),
    getTournaments()
  ]);
  const liveMatches = matches.filter(
    (match) => match.tournamentSlug === "ancient-cup-ljubljana"
  );

  return (
    <div className="page-stack">
      <section className="page-header">
        <div>
          <p className="eyebrow">DotaOps</p>
          <h1>Operations Dashboard for Dota 2 Tournaments</h1>
          <p>
            A unified workspace for tournaments, team registrations, results,
            brackets, match_id flow, and core analytics for players, teams, and heroes.
          </p>
        </div>
        <div className="header-actions">
          <Link className="button button-primary" href="/turnirji">
            <Trophy size={18} />
            <span>View Tournaments</span>
          </Link>
          <Link className="button button-secondary" href="/analitika">
            <Activity size={18} />
            <span>Open Analytics</span>
          </Link>
        </div>
      </section>

      <section className="metric-grid" aria-label="Summary">
        <MetricCard
          icon={Trophy}
          label="Active Tournaments"
          tone="red"
          trend="1 live, 1 in registration"
          value="3"
        />
        <MetricCard
          icon={UsersRound}
          label="Teams in System"
          tone="teal"
          trend="profiles ready"
          value="24"
        />
        <MetricCard
          icon={Swords}
          label="Matches"
          tone="amber"
          trend="2 with imported match_id"
          value="48"
        />
        <MetricCard
          icon={DatabaseZap}
          label="Analytics Pipeline"
          tone="green"
          trend="processing + aggregates"
          value="P1"
        />
      </section>

      <section className="content-grid">
        <div className="panel panel-large">
          <SectionHeader
            eyebrow="Public Overview"
            title="Schedule and Results"
            description="Core display for matches, statuses, and links to Dota 2 match data."
            action={
              <Link className="text-link" href="/turnirji/ancient-cup-ljubljana">
                <span>Details</span>
              </Link>
            }
          />
          <MatchSchedule matches={liveMatches} />
        </div>

        <div className="panel">
          <SectionHeader
            eyebrow="Tournament Flow"
            title="Bracket"
            description="Team advancement for the current tournament."
          />
          <BracketView matches={liveMatches} />
        </div>
      </section>

      <section className="two-column">
        <div className="panel">
          <SectionHeader
            eyebrow="Tournaments"
            title="Current Events"
            description="Initial overview for organizers, teams, and viewers."
          />
          <div className="cards-grid compact">
            {tournaments.slice(0, 2).map((tournament) => (
              <TournamentCard key={tournament.id} tournament={tournament} />
            ))}
          </div>
        </div>

        <MatchImportPanel />
      </section>

      <div className="panel">
        <SectionHeader
          eyebrow="Analytics"
          title="Teams, Players, and Heroes"
          description="Prepared layout for KDA, win rate, most played heroes, and comparisons."
        />
        <AnalyticsOverview heroes={analytics.heroMetrics} teams={analytics.teams} />
      </div>
    </div>
  );
}

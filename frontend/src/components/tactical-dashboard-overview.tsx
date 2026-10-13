import {
  Activity,
  DatabaseZap,
  Eye,
  Radar,
  ShieldCheck,
  Swords,
  Trophy,
  UsersRound
} from "lucide-react";
import Link from "next/link";

import { AnalyticsOverview } from "@/components/analytics-overview";
import { BracketView } from "@/components/bracket-view";
import { DashboardHero } from "@/components/dashboard-hero";
import { MatchImportPanel } from "@/components/match-import-panel";
import { MatchSchedule } from "@/components/match-schedule";
import { MetricCard } from "@/components/metric-card";
import { SectionHeader } from "@/components/section-header";
import { TacticalInsightCard } from "@/components/tactical-insight-card";
import { TournamentCard } from "@/components/tournament-card";
import {
  getAnalytics,
  getMatches,
  getTeams,
  getTournaments
} from "@/lib/data";
import { formatPercent } from "@/lib/utils";

export async function TacticalDashboardOverview() {
  const [analytics, matches, teams, tournaments] = await Promise.all([
    getAnalytics(),
    getMatches(),
    getTeams(),
    getTournaments()
  ]);
  const liveMatches = matches.filter(
    (match) => match.tournamentSlug === "ancient-cup-ljubljana"
  );
  const importedMatches = matches.filter((match) => match.dotaMatchId);
  const topTeams = [...teams].sort((a, b) => b.winRate - a.winRate).slice(0, 3);

  return (
    <div className="tactical-dashboard">
      <DashboardHero />

      <section className="dashboard-metrics" aria-label="Tactical summary">
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

      <section className="dashboard-command-grid">
        <div className="dashboard-panel dashboard-panel-main ops-panel">
          <SectionHeader
            eyebrow="Live matches"
            title="Schedule and Results"
            description="Operational overview of matches, statuses, and links to Dota 2 match data."
            action={
              <Link className="text-link ops-mono" href="/turnirji/ancient-cup-ljubljana">
                <span>Details</span>
              </Link>
            }
          />
          <MatchSchedule matches={liveMatches} />
        </div>

        <aside className="dashboard-side-stack">
          <div className="dashboard-panel ops-panel">
            <SectionHeader
              eyebrow="Tactical insights"
              title="Insights"
              description="Fast signals for organizers and teams."
            />
            <div className="tactical-insight-list">
              <TacticalInsightCard icon={Radar} title="Competitive Flow">
                Final slots are ready for the next bracket round.
              </TacticalInsightCard>
              <TacticalInsightCard icon={Eye} tone="cyan" title="Public View">
                Schedule, results, and match_id statuses are synchronized for viewers.
              </TacticalInsightCard>
            </div>
          </div>

          <div className="dashboard-panel ops-panel">
            <SectionHeader
              eyebrow="Active teams"
              title="Teams"
              description="Most stable teams based on current metrics."
            />
            <div className="dashboard-team-list">
              {topTeams.map((team) => (
                <article key={team.id}>
                  <span className="ops-signal" />
                  <div>
                    <strong>{team.name}</strong>
                    <p className="ops-mono">
                      {formatPercent(team.winRate)} win rate - KDA {team.kda.toFixed(1)}
                    </p>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </aside>
      </section>

      <section className="dashboard-lower-grid">
        <div className="dashboard-panel ops-panel">
          <SectionHeader
            eyebrow="Bracket system"
            title="Tournament Flow"
            description="Team advancement for the current tournament."
          />
          <BracketView matches={liveMatches} />
        </div>

        <div className="dashboard-panel ops-panel">
          <SectionHeader
            eyebrow="Tournament circuits"
            title="Current Events"
            description="Tournament and status overview for operational monitoring."
          />
          <div className="dashboard-tournament-list">
            {tournaments.slice(0, 2).map((tournament) => (
              <TournamentCard key={tournament.id} tournament={tournament} />
            ))}
          </div>
        </div>

        <div className="dashboard-panel ops-panel">
          <SectionHeader
            eyebrow="OpenDota pipeline"
            title="Data Import"
            description={`${importedMatches.length} matches have linked match_id data.`}
          />
          <MatchImportPanel />
        </div>
      </section>

      <section className="dashboard-panel ops-panel dashboard-analytics-panel">
        <SectionHeader
          eyebrow="Analytics core"
          title="Teams, Players, and Heroes"
          description="Prepared layout for KDA, win rate, most played heroes, and comparisons."
          action={
            <Link className="text-link ops-mono" href="/analitika">
              <Activity size={16} />
              <span>Open Analytics</span>
            </Link>
          }
        />
        <AnalyticsOverview heroes={analytics.heroMetrics} teams={analytics.teams} />
      </section>

      <section className="dashboard-status-bar ops-panel">
        <div>
          <ShieldCheck size={18} />
          <span className="ops-label">System status</span>
          <strong className="ops-data">ONLINE</strong>
        </div>
        <div>
          <DatabaseZap size={18} />
          <span className="ops-label">Match sync</span>
          <strong className="ops-data">Fallback ready</strong>
        </div>
        <div>
          <Activity size={18} />
          <span className="ops-label">Telemetry</span>
          <strong className="ops-data">30s cache</strong>
        </div>
      </section>
    </div>
  );
}

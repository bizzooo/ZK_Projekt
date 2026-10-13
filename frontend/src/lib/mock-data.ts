import type { HeroMetric, Match, RoadmapItem, Team, Tournament } from "@/lib/types";

export const teams: Team[] = [
  {
    id: "team-radiant-core",
    name: "Radiant Core",
    captain: "Kaptan",
    region: "SI",
    winRate: 68,
    kda: 4.2,
    favoriteHeroes: ["Puck", "Mars", "Crystal Maiden"],
    lastFive: ["W", "W", "L", "W", "W"],
    roster: [
      {
        id: "p1",
        nickname: "MidPulse",
        role: "Mid",
        kda: 5.1,
        winRate: 71,
        favoriteHero: "Puck"
      },
      {
        id: "p2",
        nickname: "SafeMode",
        role: "Carry",
        kda: 4.7,
        winRate: 67,
        favoriteHero: "Juggernaut"
      },
      {
        id: "p3",
        nickname: "OffGrid",
        role: "Offlane",
        kda: 3.9,
        winRate: 64,
        favoriteHero: "Mars"
      }
    ]
  },
  {
    id: "team-dire-stack",
    name: "Dire Stack",
    captain: "DraftLead",
    region: "HR",
    winRate: 61,
    kda: 3.8,
    favoriteHeroes: ["Lina", "Tidehunter", "Shadow Shaman"],
    lastFive: ["W", "L", "W", "W", "L"],
    roster: [
      {
        id: "p4",
        nickname: "LaneLock",
        role: "Support",
        kda: 3.2,
        winRate: 60,
        favoriteHero: "Shadow Shaman"
      },
      {
        id: "p5",
        nickname: "RuneShift",
        role: "Mid",
        kda: 4.4,
        winRate: 63,
        favoriteHero: "Lina"
      },
      {
        id: "p6",
        nickname: "Anchor",
        role: "Offlane",
        kda: 3.8,
        winRate: 59,
        favoriteHero: "Tidehunter"
      }
    ]
  },
  {
    id: "team-smoke-gank",
    name: "Smoke Gank",
    captain: "Vision",
    region: "SI",
    winRate: 56,
    kda: 3.5,
    favoriteHeroes: ["Mirana", "Bane", "Ember Spirit"],
    lastFive: ["L", "W", "W", "L", "W"],
    roster: [
      {
        id: "p7",
        nickname: "Arrowline",
        role: "Roamer",
        kda: 3.6,
        winRate: 55,
        favoriteHero: "Mirana"
      },
      {
        id: "p8",
        nickname: "Sleight",
        role: "Mid",
        kda: 4.1,
        winRate: 58,
        favoriteHero: "Ember Spirit"
      },
      {
        id: "p9",
        nickname: "NightWard",
        role: "Support",
        kda: 2.9,
        winRate: 53,
        favoriteHero: "Bane"
      }
    ]
  },
  {
    id: "team-ancient-five",
    name: "Ancient Five",
    captain: "Shotcall",
    region: "AT",
    winRate: 49,
    kda: 3.1,
    favoriteHeroes: ["Drow Ranger", "Rubick", "Centaur Warrunner"],
    lastFive: ["L", "L", "W", "W", "L"],
    roster: [
      {
        id: "p10",
        nickname: "HighGround",
        role: "Carry",
        kda: 3.8,
        winRate: 50,
        favoriteHero: "Drow Ranger"
      },
      {
        id: "p11",
        nickname: "Lift",
        role: "Support",
        kda: 2.7,
        winRate: 47,
        favoriteHero: "Rubick"
      },
      {
        id: "p12",
        nickname: "Stampede",
        role: "Offlane",
        kda: 3.3,
        winRate: 51,
        favoriteHero: "Centaur Warrunner"
      }
    ]
  }
];

export const tournaments: Tournament[] = [
  {
    id: "t1",
    slug: "ancient-cup-ljubljana",
    title: "Ancient Cup Ljubljana",
    status: "live",
    format: "Single elimination",
    startsAt: "2026-05-06T18:00:00+02:00",
    teamsCount: 8,
    registrationsCount: 8,
    organizer: "DotaOps",
    prizePool: "Community rewards",
    description:
      "First demo tournament for validating registrations, brackets, results, and match data analytics."
  },
  {
    id: "t2",
    slug: "mid-wars-open",
    title: "Mid Wars Open",
    status: "registration",
    format: "Groups + playoff",
    startsAt: "2026-05-20T19:00:00+02:00",
    teamsCount: 16,
    registrationsCount: 11,
    organizer: "DotaOps",
    prizePool: "TBD",
    description:
      "Open tournament for teams and captains, focused on fast registration approval and a public schedule."
  },
  {
    id: "t3",
    slug: "radiant-finals",
    title: "Radiant Finals",
    status: "draft",
    format: "Best of 3 playoff",
    startsAt: "2026-06-03T20:00:00+02:00",
    teamsCount: 4,
    registrationsCount: 0,
    organizer: "DotaOps",
    prizePool: "Demo",
    description:
      "Final format for testing advancement, result entry, and dashboards before project submission."
  }
];

export const matches: Match[] = [
  {
    id: "m1",
    tournamentSlug: "ancient-cup-ljubljana",
    round: "Quarterfinal",
    teamA: "Radiant Core",
    teamB: "Ancient Five",
    scoreA: 2,
    scoreB: 0,
    startsAt: "2026-05-06T18:00:00+02:00",
    status: "finished",
    dotaMatchId: "7894561230",
    importStatus: "ready"
  },
  {
    id: "m2",
    tournamentSlug: "ancient-cup-ljubljana",
    round: "Quarterfinal",
    teamA: "Dire Stack",
    teamB: "Smoke Gank",
    scoreA: 1,
    scoreB: 1,
    startsAt: "2026-05-06T19:30:00+02:00",
    status: "live",
    dotaMatchId: "7894561244",
    importStatus: "processing"
  },
  {
    id: "m3",
    tournamentSlug: "ancient-cup-ljubljana",
    round: "Semifinal",
    teamA: "Radiant Core",
    teamB: "TBD",
    startsAt: "2026-05-08T18:30:00+02:00",
    status: "scheduled"
  },
  {
    id: "m4",
    tournamentSlug: "ancient-cup-ljubljana",
    round: "Final",
    teamA: "TBD",
    teamB: "TBD",
    startsAt: "2026-05-10T20:00:00+02:00",
    status: "scheduled"
  },
  {
    id: "m5",
    tournamentSlug: "mid-wars-open",
    round: "Group A",
    teamA: "Registered Team",
    teamB: "Registered Team",
    startsAt: "2026-05-20T19:00:00+02:00",
    status: "scheduled"
  }
];

export const heroMetrics: HeroMetric[] = [
  { hero: "Puck", pickRate: 42, winRate: 66, avgKda: 5.2 },
  { hero: "Mars", pickRate: 37, winRate: 61, avgKda: 3.9 },
  { hero: "Lina", pickRate: 34, winRate: 58, avgKda: 4.7 },
  { hero: "Crystal Maiden", pickRate: 31, winRate: 54, avgKda: 2.8 },
  { hero: "Tidehunter", pickRate: 27, winRate: 52, avgKda: 3.4 }
];

export const roadmap: RoadmapItem[] = [
  {
    iteration: "Iteration 1",
    title: "Accounts, Profiles, and Public Overview",
    status: "active",
    items: [
      "User accounts and login",
      "Player and team profiles",
      "Public tournament overview and core information"
    ]
  },
  {
    iteration: "Iteration 2",
    title: "Tournament Creation and Registrations",
    status: "planned",
    items: [
      "Tournament editing and publishing",
      "Team registration forms",
      "Participation confirmation"
    ]
  },
  {
    iteration: "Iteration 3",
    title: "Brackets, Schedule, and Results",
    status: "planned",
    items: [
      "Draws and pairings",
      "Result entry",
      "Public advancement display"
    ]
  },
  {
    iteration: "Iteration 4",
    title: "OpenDota Flow and Analytics",
    status: "planned",
    items: [
      "match_id import",
      "Data normalization",
      "Team, player, and hero metrics"
    ]
  },
  {
    iteration: "Iteration 5",
    title: "Integrations and Stabilization",
    status: "planned",
    items: [
      "Steam profile or identification",
      "Discord or email notifications",
      "Regression testing and final documentation"
    ]
  }
];

import { OrganizerCommandPage } from "@/components/organizer-command-page";

interface OrganizerPageProps {
  searchParams?: Promise<{
    slug?: string | string[];
    tournamentId?: string | string[];
    view?: string | string[];
  }>;
}

function firstParam(value?: string | string[]) {
  return Array.isArray(value) ? value[0] : value;
}

export default async function OrganizerPage({ searchParams }: OrganizerPageProps) {
  const params = await searchParams;

  return (
    <OrganizerCommandPage
      initialSlug={firstParam(params?.slug)}
      initialTournamentId={firstParam(params?.tournamentId)}
      initialView={firstParam(params?.view)}
    />
  );
}

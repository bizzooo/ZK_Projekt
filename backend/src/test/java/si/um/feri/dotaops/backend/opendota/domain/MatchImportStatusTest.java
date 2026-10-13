package si.um.feri.dotaops.backend.opendota.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchImportStatusTest {

    @Test
    void databaseAndApiValuesUseCanonicalLowercaseStatusNames() {
        assertThat(MatchImportStatus.QUEUED.databaseValue()).isEqualTo("queued");
        assertThat(MatchImportStatus.PROCESSING.databaseValue()).isEqualTo("processing");
        assertThat(MatchImportStatus.READY.databaseValue()).isEqualTo("ready");
        assertThat(MatchImportStatus.ERROR.databaseValue()).isEqualTo("error");
    }

    @Test
    void statusParsingAcceptsCanonicalDatabaseValues() {
        assertThat(MatchImportStatus.fromDatabaseValue("queued")).isEqualTo(MatchImportStatus.QUEUED);
        assertThat(MatchImportStatus.fromDatabaseValue("processing")).isEqualTo(MatchImportStatus.PROCESSING);
        assertThat(MatchImportStatus.fromDatabaseValue("ready")).isEqualTo(MatchImportStatus.READY);
        assertThat(MatchImportStatus.fromDatabaseValue("error")).isEqualTo(MatchImportStatus.ERROR);
    }
}

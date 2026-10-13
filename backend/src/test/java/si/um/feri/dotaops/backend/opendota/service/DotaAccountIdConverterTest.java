package si.um.feri.dotaops.backend.opendota.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DotaAccountIdConverterTest {

    @Test
    void convertsSteamId64ToDotaAccountId32() {
        assertThat(DotaAccountIdConverter.steamId64ToAccountId32("76561198000000001"))
                .isEqualTo(39734273L);
    }

    @Test
    void rejectsInvalidSteamId64() {
        assertThatThrownBy(() -> DotaAccountIdConverter.steamId64ToAccountId32("not-a-steam-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SteamID64 must be a 17 digit numeric value.");
    }
}

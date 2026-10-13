package si.um.feri.dotaops.backend.opendota.service;

import java.math.BigInteger;

import org.springframework.util.StringUtils;

public final class DotaAccountIdConverter {

    private static final BigInteger STEAM_ID64_OFFSET = new BigInteger("76561197960265728");
    private static final BigInteger MAX_ACCOUNT_ID32 = new BigInteger("4294967295");

    private DotaAccountIdConverter() {
    }

    public static long steamId64ToAccountId32(String steamId64) {
        if (!StringUtils.hasText(steamId64) || !steamId64.matches("^[0-9]{17}$")) {
            throw new IllegalArgumentException("SteamID64 must be a 17 digit numeric value.");
        }

        BigInteger accountId = new BigInteger(steamId64).subtract(STEAM_ID64_OFFSET);
        if (accountId.signum() < 0 || accountId.compareTo(MAX_ACCOUNT_ID32) > 0) {
            throw new IllegalArgumentException("SteamID64 is outside the supported Dota account range.");
        }

        return accountId.longValueExact();
    }
}

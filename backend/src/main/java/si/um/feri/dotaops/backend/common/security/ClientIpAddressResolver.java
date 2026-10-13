package si.um.feri.dotaops.backend.common.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpAddressResolver {

    private static final String UNKNOWN_CLIENT_IP = "unknown";

    public String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }

        String remoteAddress = request.getRemoteAddr();
        if (StringUtils.hasText(remoteAddress)) {
            return remoteAddress.trim();
        }

        return UNKNOWN_CLIENT_IP;
    }
}

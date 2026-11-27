package com.bazaarhub.backend.shared.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Secure client IP address resolver.
 * Enforces strict proxy boundary verification to prevent X-Forwarded-For spoofing attacks.
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    // Standard IPv4 and IPv6 validation patterns
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$"
    );

    private final RateLimitConfigProperties properties;

    public ClientIpResolver(RateLimitConfigProperties properties) {
        this.properties = properties;
    }

    /**
     * Resolves the verified client IP address from the incoming request.
     *
     * @param request HTTP servlet request
     * @return Validated client IP address
     */
    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (remoteAddr == null || remoteAddr.isBlank()) {
            return "0.0.0.0";
        }

        remoteAddr = remoteAddr.trim();
        Set<String> trustedProxies = properties.getTrustedProxySet();

        // If the immediate remote peer is NOT a trusted proxy, use its remote address directly.
        // Arbitrary X-Forwarded-For headers from untrusted clients are completely ignored.
        if (!isTrustedProxy(remoteAddr, trustedProxies)) {
            return normalizeIp(remoteAddr);
        }

        // The immediate connection is from a trusted proxy; evaluate forwarded header chain
        String forwardedHeader = request.getHeader(X_FORWARDED_FOR);
        if (forwardedHeader == null || forwardedHeader.isBlank()) {
            String realIp = request.getHeader(X_REAL_IP);
            if (realIp != null && !realIp.isBlank() && isValidIp(realIp.trim())) {
                return normalizeIp(realIp.trim());
            }
            return normalizeIp(remoteAddr);
        }

        // Traverse forwarded IP list from right to left (most recent proxy hop first)
        String[] hops = forwardedHeader.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String candidateIp = hops[i].trim();
            if (!candidateIp.isEmpty() && isValidIp(candidateIp)) {
                if (!isTrustedProxy(candidateIp, trustedProxies)) {
                    // First non-trusted proxy IP encountered from right to left is the true client
                    return normalizeIp(candidateIp);
                }
            } else {
                log.warn("[CLIENT IP RESOLVER] Rejecting malformed forwarded IP token: '{}'", candidateIp);
            }
        }

        // If all hops belong to trusted proxies, fall back to remoteAddr
        return normalizeIp(remoteAddr);
    }

    private boolean isTrustedProxy(String ip, Set<String> trustedProxies) {
        if (trustedProxies.contains(ip)) {
            return true;
        }
        // Normalize IPv6 localhost variants (e.g. 0:0:0:0:0:0:0:1 vs ::1)
        if ("::1".equals(ip) && trustedProxies.contains("0:0:0:0:0:0:0:1")) {
            return true;
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) && trustedProxies.contains("::1")) {
            return true;
        }
        return false;
    }

    /**
     * Validates whether a string is a valid IPv4 or IPv6 address.
     */
    public boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || ip.length() > 45) {
            return false;
        }
        // Fast path for IPv4
        if (IPV4_PATTERN.matcher(ip).matches()) {
            return true;
        }
        // Verification for IPv6
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.getHostAddress().equalsIgnoreCase(ip) || ip.contains(":");
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private String normalizeIp(String ip) {
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}

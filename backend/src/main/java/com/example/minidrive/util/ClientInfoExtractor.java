package com.example.minidrive.util;

import com.example.minidrive.dto.common.ClientInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientInfoExtractor {
    public ClientInfo extractClientInfo(HttpServletRequest request) {
        String ip = extractIp(request);
        String ua = extractUserAgent(request);
        String deviceName = parseDeviceName(ua);

        return new ClientInfo(ip, ua, deviceName);
    }

    private String extractIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "Unknown";
    }

    private String parseDeviceName(String ua) {
        if (!StringUtils.hasText(ua)) return "Неизвестное устройство";

        String os = "Unknown OS";
        if (ua.contains("Windows")) os = "Windows";
        else if (ua.contains("Android")) os = "Android";
        else if (ua.contains("iPhone") || ua.contains("iPad")) os = "iOS";
        else if (ua.contains("Macintosh") || ua.contains("Mac OS")) os = "macOS";
        else if (ua.contains("Linux")) os = "Linux";

        String browser = "Browser";
        if (ua.contains("Edg/")) browser = "Edge";
        else if (ua.contains("OPR/") || ua.contains("Opera")) browser = "Opera";
        else if (ua.contains("Chrome") && !ua.contains("Chromium")) browser = "Chrome";
        else if (ua.contains("Firefox")) browser = "Firefox";
        else if (ua.contains("Safari") && !ua.contains("Chrome")) browser = "Safari";
        else if (ua.contains("Nextcloud")) browser = "Nextcloud Client";

        return browser + " on " + os;
    }
}

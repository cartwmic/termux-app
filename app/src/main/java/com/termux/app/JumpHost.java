package com.termux.app;

/**
 * Shared jump-host parsing and session-name matching for notification deep
 * links. Pure string logic so JVM unit tests do not need Android.
 *
 * This class carries no SSH hostnames, credentials, or allowed-host list —
 * only the conservative alias grammar and exact session-name equality.
 */
final class JumpHost {

    static final String QUERY_KEY = "host";
    static final String GRAMMAR = "^[A-Za-z][A-Za-z0-9_-]{0,63}$";

    private JumpHost() {}

    /**
     * Extract a {@code host} query value from a jump URI only when the URI
     * matches {@code prefix} and the value matches {@link #GRAMMAR}. Path-id
     * delimiters {@code /}, {@code ?}, {@code #} are respected: the query is
     * the substring after the first {@code ?} and before any {@code #}.
     * {@code session} and {@code slot} keys are ignored. Invalid or absent
     * host returns {@code null} and does not block path-id dispatch.
     */
    static String extractHost(String uriString, String prefix) {
        if (uriString == null || prefix == null) return null;
        if (uriString.length() <= prefix.length()) return null;
        if (!uriString.regionMatches(true, 0, prefix, 0, prefix.length())) return null;

        int queryStart = uriString.indexOf('?');
        if (queryStart < 0) return null;
        int fragmentStart = uriString.indexOf('#', queryStart);
        String query = fragmentStart < 0
            ? uriString.substring(queryStart + 1)
            : uriString.substring(queryStart + 1, fragmentStart);
        if (query.isEmpty()) return null;

        int from = 0;
        while (from <= query.length()) {
            int amp = query.indexOf('&', from);
            String part = amp < 0 ? query.substring(from) : query.substring(from, amp);
            int eq = part.indexOf('=');
            String key = eq < 0 ? part : part.substring(0, eq);
            String value = eq < 0 ? "" : part.substring(eq + 1);
            if (QUERY_KEY.equals(key)) {
                return value.matches(GRAMMAR) ? value : null;
            }
            if (amp < 0) break;
            from = amp + 1;
        }
        return null;
    }

    /** EXTRA_ARGUMENTS for {@code ~/bin/{herdr,zellij}-jump}: {@code [id]} or {@code [id, host]}. */
    static String[] extraArguments(String pathId, String host) {
        if (host == null) return new String[]{pathId};
        return new String[]{pathId, host};
    }

    /**
     * Index of the first session whose name equals {@code host} exactly.
     * Duplicate names take the first match. Miss or null host returns {@code -1}.
     */
    static int indexOfFirstNamed(String[] sessionNames, String host) {
        if (host == null || sessionNames == null) return -1;
        for (int i = 0; i < sessionNames.length; i++) {
            if (host.equals(sessionNames[i])) return i;
        }
        return -1;
    }
}

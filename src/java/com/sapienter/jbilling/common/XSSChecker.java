package com.sapienter.jbilling.common;

import java.util.regex.Pattern;


/**
 * Generic util class to check weather a string contains any script items. Used by security filter to parse out xss requests
 */
public class XSSChecker {

    private static final FormatLogger LOG = new FormatLogger(XSSChecker.class);

    private static Pattern[] patterns = new Pattern[]{
            // Script fragments
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            // src='...'
            Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // lonely script tags
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // eval(...)
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // expression(...)
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // javascript:...
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            // vbscript:...
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            // onload(...)=...
            Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };

    /**
     * Check the provided value to check if it contains xss and return a boolean
     *
     * @param value Value to check for scripts
     * @return True if value contains scripts
     */
    public boolean hasScript(String value) {
        if (value == null || value.equals("")) {
            return false;
        }
        for (Pattern scriptPattern : patterns) {
            boolean found = scriptPattern.matcher(value).find(0);
            if (found) {
                LOG.debug("Pattern: " + scriptPattern.pattern());
                LOG.debug("Value string: " + value);
                return true;
            }
        }
        return false;
    }

}

package io.gitlab.rxp90.jsymspell.api;

public class DamerauLevenshteinOSA implements StringDistance {

    private final CharComparator charComparator;

    public DamerauLevenshteinOSA() {
        this.charComparator = new DefaultCharComparator();
    }

    public DamerauLevenshteinOSA(CharComparator charComparator) {
        this.charComparator = charComparator;
    }

    @Override
    public int distanceWithEarlyStop(String baseString, String string2, int maxDistance) {
        if (baseString == null) return string2 == null ? 0 : string2.length();
        if (string2 == null || string2.isEmpty()) return baseString.length();
        if (maxDistance == 0) return baseString.equals(string2) ? 0 : -1;
        int[] baseChar1Costs = new int[baseString.length()];
        int[] basePrevChar1Costs = new int[baseString.length()];

        // If strings have different lengths, ensure shorter string is in string1. This can result in a
        // little faster speed by spending more time spinning just the inner loop during the main processing.
        String string1;
        if (baseString.length() > string2.length()) {
            string1 = string2;
            string2 = baseString;
        } else {
            string1 = baseString;
        }
        int str1Len = string1.length();
        int str2Len = string2.length();

        // Ignore common suffix
        while ((str1Len > 0) && (string1.charAt(str1Len - 1) == string2.charAt(str2Len - 1))) {
            str1Len--;
            str2Len--;
        }

        int start = 0;
        if ((string1.charAt(0) == string2.charAt(0)) || (str1Len == 0)) {
            // Ignore common prefix and string1 substring of string2
            while ((start < str1Len) && (string1.charAt(start) == string2.charAt(start))) start++;

            str1Len -= start; // length of the part excluding common prefix and suffix
            str2Len -= start;


            if (str1Len == 0) { // string1 is a substring in string2, so str2Len == distance between both
                return str2Len;
            }

            string2 = string2.substring(start, start + str2Len); // faster than string2[start+j] in inner loop below
        }

        int lenDiff = str2Len - str1Len;

        if ((maxDistance < 0) || (maxDistance > str2Len)) {
            maxDistance = str2Len;
        } else if (lenDiff > maxDistance) {
            return -1;
        }

        if (str2Len > baseChar1Costs.length) {
            baseChar1Costs = new int[str2Len];
            basePrevChar1Costs = new int[str2Len];
        } else {
            for (int i = 0; i < str2Len; i++) {
                basePrevChar1Costs[i] = 0;
            }
        }

        for (int j = 0; j < str2Len; j++) {
            if (j < maxDistance) {
                baseChar1Costs[j] = j + 1;
            } else {
                baseChar1Costs[j] = maxDistance + 1;
            }
        }

        int jStartOffset = maxDistance - (str2Len - str1Len);
        boolean haveMax = maxDistance < str2Len;
        int jStart = 0;
        int jEnd = maxDistance;
        char str1Char = string1.charAt(0);
        int current = 0;
        for (int i = 0; i < str1Len; i++) {
            char prevStr1Char = str1Char;
            str1Char = string1.charAt(start + i);
            char str2Char = string2.charAt(0);
            int left = i;
            current = left + 1;
            int nextTransCost = 0;
            // no need to look beyond window of lower right diagonal - maxDistance cells (lower right diag is i - lenDiff) and the upper left diagonal + maxDistance cells (upper left is i)
            jStart += (i > jStartOffset) ? 1 : 0;
            jEnd += (jEnd < str2Len) ? 1 : 0;
            for (int j = jStart; j < jEnd; j++) {
                int above = current;
                int thisTransCost = nextTransCost;
                nextTransCost = basePrevChar1Costs[j];
                basePrevChar1Costs[j] = current = left; // cost of diagonal (substitution)
                left = baseChar1Costs[j]; // left now equals current cost (which will be diagonal at next iteration)
                char prevStr2Char = str2Char;
                str2Char = string2.charAt(j);

                if (charComparator.areDistinct(str1Char, str2Char)) {
                    if (left < current) current = left; // insertion
                    if (above < current) current = above; // deletion
                    current++;
                    if ((i != 0)
                            && (j != 0)
                            && charComparator.areEqual(str1Char, prevStr2Char)
                            && charComparator.areEqual(prevStr1Char, str2Char)) {
                        thisTransCost++;
                        if (thisTransCost < current) current = thisTransCost; // transposition
                    }
                }
                baseChar1Costs[j] = current;
            }
            if (haveMax && (baseChar1Costs[i + lenDiff] > maxDistance)) return -1;
        }
        return (current <= maxDistance) ? current : -1;
    }

}

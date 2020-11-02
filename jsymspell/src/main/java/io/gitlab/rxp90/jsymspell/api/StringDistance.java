package io.gitlab.rxp90.jsymspell.api;

public interface StringDistance {

    int distanceWithEarlyStop(String baseString, String string2, int maxDistance);

    default int distance(String string1, String string2){
        return distanceWithEarlyStop(string1, string2, Math.max(string1.length(), string2.length()));
    }
}

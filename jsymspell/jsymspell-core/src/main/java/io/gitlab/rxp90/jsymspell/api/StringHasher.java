package io.gitlab.rxp90.jsymspell.api;

public interface StringHasher {

    default long hash(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input must NOT be null");
        }
        return input.hashCode();
    }

}

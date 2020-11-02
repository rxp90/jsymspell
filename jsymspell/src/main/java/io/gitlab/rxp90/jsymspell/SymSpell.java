package io.gitlab.rxp90.jsymspell;

import io.gitlab.rxp90.jsymspell.api.SuggestItem;
import io.gitlab.rxp90.jsymspell.exceptions.NotInitializedException;

import java.util.List;

public interface SymSpell {
    List<SuggestItem> lookup(String input, Verbosity verbosity, boolean includeUnknown) throws NotInitializedException;

    List<SuggestItem> lookup(String input, Verbosity verbosity) throws NotInitializedException;

    List<SuggestItem> lookupCompound(String input, int editDistanceMax, boolean includeUnknown) throws NotInitializedException;
}

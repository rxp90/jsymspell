package io.gitlab.rxp90.jsymspell.exceptions;

/**
 * Superclass for all exceptions used in the spell checker.
 */
public abstract class SymSpellException extends RuntimeException {
  public SymSpellException( final String message ) {
    super( message );
  }
}

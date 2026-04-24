package com.danil.library.signature;

/**
 * Канонизация payload в детерминированный набор байтов.
 * Именно эти байты подаются на вход криптографической подписи.
 */
public interface CanonicalizationService {
    byte[] canonicalize(Object payload);
}

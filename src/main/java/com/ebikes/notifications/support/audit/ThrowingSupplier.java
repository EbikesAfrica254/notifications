package com.ebikes.notifications.support.audit;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {
  T get() throws E;
}

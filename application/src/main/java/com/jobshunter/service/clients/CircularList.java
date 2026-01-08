package com.jobshunter.service.clients;

import java.util.List;

public final class CircularList<T> {

  private final List<T> elements;
  private int index = 0;

  public CircularList(List<T> elements) {
    if (elements == null || elements.isEmpty()) {
      throw new IllegalArgumentException("List must not be null or empty");
    }
    this.elements = List.copyOf(elements);
  }

  public T next() {
    T value = elements.get(index);
    index = (index + 1) % elements.size();
    return value;
  }
}

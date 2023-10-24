package org.veupathdb.service.eda.access.util;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PatternUtil
{
  public static Stream<String> matchStream(final Pattern pat, final String input, final int group) {
    final var matcher = pat.matcher(input);

    final var it = new Spliterators.AbstractSpliterator<String>(
      Long.MAX_VALUE,
      Spliterator.ORDERED | Spliterator.NONNULL
    ) {
      @Override
      public boolean tryAdvance(Consumer<? super String> action) {
        if (!matcher.find())
          return false;

        action.accept(matcher.group(group));
        return true;
      }
    };

    return StreamSupport.stream(it, false);
  }
}

package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Function;

public class MedianAggregator implements MarkerAggregator {

  private Double currentMedian = null;
  // Initialize a min-heap for the elements greater than the running median.
  private final PriorityQueue<Double> above = new PriorityQueue<>(Comparator.naturalOrder());
  // Initialize a max-heap for the elements less than the running median.
  private final PriorityQueue<Double> below = new PriorityQueue<>(Comparator.reverseOrder());

  private final int index;
  private final Function<String, Double> variableValueQuantifier;

  public MedianAggregator(int index, Function<String, Double> variableValueQuantifier) {
    this.index = index;
    this.variableValueQuantifier = variableValueQuantifier;
  }

  @Override
  public void addValue(String[] arr) {
    if (arr[index] == null || arr[index].isEmpty()) {
      return;
    }
    Double d = Double.parseDouble(arr[index]);
    if (currentMedian == null) {
      // This is the first number we're seeing. Set it to the median.
      currentMedian = d;
    } else {
      if (d > currentMedian) {
        // The value is above the current median. Check if we have a balanced number above and below current median.
        if (above.size() > below.size()) {
          // Since there are more above and our number is above the current median. Take the smallest number above
          // the current median and set it to the current median or below depending on if it's greater than input number.
          final double lowestAbove = above.poll();
          double tmp = currentMedian;
          if (d > lowestAbove) {
            currentMedian = lowestAbove;
            below.add(tmp);
            above.add(d);
          } else {
            currentMedian = d;
            below.add(lowestAbove);
            above.add(tmp);
          }
        } else {
          // Above heap has fewer or equal elements than below heap. Input number can go directly on above heap.
          above.add(d);
        }
      } else if (d < currentMedian) {
        // Input value is below current median.
        if (below.size() > above.size()) {
          // Below median heap is too big to add this element. Take the max off the heap and shuffle elements.
          final double highestBelow = below.poll();
          double tmp = currentMedian;
          if (d > highestBelow) {
            currentMedian = d;
            below.add(highestBelow);
            above.add(tmp);
          } else {
            currentMedian = highestBelow;
            below.add(d);
            above.add(tmp);
          }
        } else if (below.size() < above.size()) {
          below.add(d);
        } else {
          below.add(d);
        }
      } else {
        if (below.size() > above.size()) {
          above.add(d);
        } else {
          below.add(d);
        }
      }
    }
  }

  @Override
  public boolean appliesTo(String[] rec) {
    return rec[index] != null && !rec[index].isEmpty();
  }

  @Override
  public Double finish() {
    if (above.size() > below.size()) {
      return (above.poll() + currentMedian) / 2.0;
    } else if (above.size() < below.size()) {
      return (below.poll() + currentMedian) / 2.0;
    }
    return currentMedian;
  }
}

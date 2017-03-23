/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.test.metric.report.chart;

import static java.lang.Math.pow;
import static java.lang.String.format;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.test.metric.report.chart.Histogram.Logarithmic;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

public class HistogramChartData extends GoogleChartAPI {
    
    private List<HistogramDataEntry> data = new ArrayList<>();

  @Override
  public void setItemLabel(String... labels) {
      for(String label : labels){
          final HistogramDataEntry entry = new HistogramDataEntry(label);
          data.add(entry);
      }
  }
  
  @Override
  public void setValues(int[]...values) {
      final int labelLength = data.size();
      final int expectedNumber=3;
      if(values.length!=3){
          throw new IllegalArgumentException("Expecting only " + expectedNumber + "  values for Excellent, Good, Needs Work. Got " + values.length);
      }
      final int[] excellent = values[0];
      final int[] good = values[1];
      final int[] needsWork = values[2];
      if(excellent.length!=good.length || good.length!=needsWork.length){
          throw new IllegalArgumentException("Different number of measurement for each metric");
      }
      if(excellent.length!=data.size()){
          final String labels = data.stream().map(HistogramDataEntry::getLabel).collect(Collectors.joining("_"));
          throw new IllegalArgumentException("Labels " + labels + "(" + data.size() + " )do not match measurements (" + excellent.length + ')');
      }
      for(int i=0; i<data.size(); i++){
          final HistogramDataEntry entry = data.get(i);
          entry.setExcellent(excellent[i]);
          entry.setGood(good[i]);
          entry.setNeedsWork(needsWork[i]);
      }
  }

  public void setYMark(int min, int max, Function<Integer, Double> scalingFunction) {
    keys.put("chxt", "y");
    if (scalingFunction instanceof Logarithmic) {
      List<String> yLabels = Lists.newLinkedList();
      List<String> yPositions = Lists.newLinkedList();
      double scaledMax = scalingFunction.apply(max);
      for (int labelExponent = 0; pow(10, labelExponent) < max; labelExponent++) {
        yLabels.add(String.valueOf((int)pow(10, labelExponent)));
        yPositions.add(String.valueOf(100 * (labelExponent + 1) / scaledMax));
      }
      keys.put("chxl", "0:|" + toList("|", yLabels.toArray(new String[yLabels.size()])));
      keys.put("chxp", "0," + toList(",", yPositions.toArray(new String[yPositions.size()])));
    } else {
      keys.put("chxr", format("0,%d,%d", min, max));
    }
  }

  @Override
  public String getHtml() {
      final String retval = data.stream()
              .map(entry -> "['" + entry.getLabel() + "', " + entry.getExcellent() + ", '#" + GREEN + "', " + entry.getGood()  + ", '#" + YELLOW + "', " + entry.getNeedsWork() + ", '#" + RED + "']")
              .collect(Collectors.joining(","));
      return retval;
  }
}

/*
 * Copyright 2017 Google.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.test.metric.report.chart;

/**
 *
 * @author erik
 */
public class HistogramDataEntry {
    private final String label;
    private int excellent;
    private int good;
    private int needsWork;
    
    public HistogramDataEntry(String label){
        this.label=label;
    }

    public String getLabel() {
        return label;
    }

    public int getExcellent() {
        return excellent;
    }

    public void setExcellent(int excellent) {
        if(excellent==Integer.MIN_VALUE){
            return;
        }
        this.excellent = excellent;
    }

    public int getGood() {
        return good;
    }

    public void setGood(int good) {
        if(good==Integer.MIN_VALUE){
            return;
        }
        this.good = good;
    }

    public int getNeedsWork() {
        return needsWork;
    }

    public void setNeedsWork(int needsWork) {
        if(needsWork==Integer.MIN_VALUE){
            return;
        }
        this.needsWork = needsWork;
    }
    
    
}

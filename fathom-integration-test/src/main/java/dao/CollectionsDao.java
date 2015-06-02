/*
 * Copyright (C) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dao;

import com.google.inject.Singleton;

import java.util.Arrays;
import java.util.List;

/**
 * @author James Moger
 */
@Singleton
public class CollectionsDao {

    private final List<Integer> ints = Arrays.asList(2, 2, 4, 4, 6, 6, 8, 8);

    private final List<Integer> years = Arrays.asList(1945, 1955, 1965, 1975, 1985, 1995, 2005, 2015);

    private final List<String> colors = Arrays.asList("Blue", "Orange", "Blue", "Orange", "Green", "Red", "Green", "Red");

    private final List<String> desserts = Arrays.asList("Ice Cream", "Pie", "Cake", "Cookies");

    public List<Integer> myInts = ints;

    public List<Integer> myYears = years;

    public List<String> myColors = colors;

    public List<String> myDesserts = desserts;

    public void reset() {
        this.myInts = ints;
        this.myYears = years;
        this.myColors = colors;
        this.myDesserts = desserts;
    }
}

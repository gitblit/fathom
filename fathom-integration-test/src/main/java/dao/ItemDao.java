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
import conf.Caches;
import models.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author James Moger
 */
@Singleton
@CacheDefaults(cacheName = Caches.ITEMS_CACHE)
public class ItemDao {

    final Map<Integer, Item> items;
    private final Logger log = LoggerFactory.getLogger(ItemDao.class);

    public ItemDao() {
        this.items = new TreeMap<Integer, Item>() {{
            put(1, new Item(1, "Apples"));
            put(2, new Item(2, "Bananas"));
            put(3, new Item(3, "Strawberries"));
            put(4, new Item(4, "Grapes"));
            put(5, new Item(5, "Kiwi"));
        }};
    }

    @CacheResult
    public Item get(int id) {
        log.info("Getting Item #{} by id", id);
        return items.get(id);
    }

    @CacheResult
    public List<Item> getAll() {
        log.info("Getting all items");
        return new ArrayList<>(items.values());
    }
}

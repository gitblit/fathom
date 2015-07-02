package ${package}.dao;

import ${package}.conf.Caches;
import ${package}.models.Item;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * An example DAO for Items.
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
            put(6, new Item(6, "Watermelon"));
            put(7, new Item(7, "Pineapple"));
            put(8, new Item(8, "Apricot"));
            put(9, new Item(9, "Blueberry"));
            put(10, new Item(10, "Orange"));
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

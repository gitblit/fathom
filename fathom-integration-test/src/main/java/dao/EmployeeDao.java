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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import conf.Caches;
import fathom.utils.ClassUtil;
import models.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author James Moger
 */
@Singleton
@CacheDefaults(cacheName = Caches.EMPLOYEE_CACHE)
public class EmployeeDao {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDao.class);
    private final AtomicInteger idCounter;
    private final Map<Integer, Employee> employees;
    private final Set<String> offices;
    private final Set<String> positions;

    @Inject
    public EmployeeDao() {
        idCounter = new AtomicInteger();
        employees = new ConcurrentHashMap<>();
        offices = Collections.synchronizedSet(new HashSet<>());
        positions = Collections.synchronizedSet(new HashSet<>());

        loadEmployees();
    }

    @CacheResult
    public Employee get(int id) {
        log.info("Getting employee #{} by id", id);
        Employee employee = employees.get(id);
        return employee;
    }

    @CacheResult
    public List<Employee> getAll() {
        log.info("Getting all employees");
        return new ArrayList<>(employees.values());
    }

    public Collection<String> getOffices() {
        List<String> list = new ArrayList<>(offices);
        Collections.sort(list);
        return Collections.unmodifiableCollection(list);
    }

    public Collection<String> getPositions() {
        List<String> list = new ArrayList<>(positions);
        Collections.sort(list);
        return Collections.unmodifiableCollection(list);
    }

    @CacheRemoveAll
    public Employee delete(int id) {
        Employee employee = employees.get(id);
        employees.remove(id);
        return employee;
    }

    @CacheRemoveAll
    public Employee save(Employee employee) {
        return put(employee);
    }

    private Employee put(Employee employee) {
        if (employee.getId() == 0) {
            employee.setId(idCounter.incrementAndGet());
        }
        employees.put(employee.getId(), employee);
        offices.add(employee.getOffice());
        positions.add(employee.getPosition());

        return employee;
    }


    private void loadEmployees() {
        URL resource = ClassUtil.getResource("dao/employees.json");
        try (InputStreamReader reader = new InputStreamReader(resource.openStream())) {
            Gson gson = new GsonBuilder().create();
            Data data = gson.fromJson(reader, Data.class);
            DateFormat df = new SimpleDateFormat(("yyyy/MM/dd"));

            for (Object[] row : data.employees) {

                Employee employee = new Employee();
                employee.setName((String) row[0]);
                employee.setPosition((String) row[1]);
                employee.setOffice((String) row[2]);
                employee.setExtension(((Number) row[3]).intValue());
                employee.setStartDate(df.parse((String) row[4]));
                employee.setSalary((String) row[5]);

                put(employee);
            }
        } catch (ParseException | IOException e) {
            log.error(null, e);
        }
    }

    private static class Data {
        Object[][] employees;
    }
}

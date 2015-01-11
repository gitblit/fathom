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
package models;

import ro.pippo.core.ParamPattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * @author James Moger
 */
@XmlRootElement
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String position;
    private String office;
    private int extension;
    @ParamPattern("yyyy-MM-dd")
    private Date startDate;
    private String salary;

    public Employee() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getExtension() {
        return extension;
    }

    @XmlAttribute
    public Employee setExtension(int extension) {
        this.extension = extension;

        return this;
    }

    public String getName() {
        return name;
    }

    @XmlElement
    public Employee setName(String name) {
        this.name = name;

        return this;
    }

    public String getOffice() {
        return office;
    }

    @XmlElement
    public Employee setOffice(String office) {
        this.office = office;

        return this;
    }

    public String getPosition() {
        return position;
    }

    @XmlElement
    public Employee setPosition(String position) {
        this.position = position;

        return this;
    }

    @Override
    public String toString() {
        return "Employee {" +
                "name='" + name + '\'' +
                ", office='" + office + '\'' +
                ", position='" + position + '\'' +
                '}';
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }
}

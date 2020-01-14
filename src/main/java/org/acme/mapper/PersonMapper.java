package org.acme.mapper;

import org.acme.domain.Person;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PersonMapper {

    @Select(value = "select 1")
    String select1();


    @Select(value = "select *from person")
    List<Person> findAll();

}

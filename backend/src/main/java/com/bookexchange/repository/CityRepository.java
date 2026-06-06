package com.bookexchange.repository;

import com.bookexchange.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findByProvince(String province);

    List<City> findByCityNameContaining(String cityName);
}

package com.bookexchange.service;

import com.bookexchange.entity.City;
import com.bookexchange.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    @Cacheable(value = "cities", key = "'all'")
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    public List<City> getCitiesByProvince(String province) {
        return cityRepository.findByProvince(province);
    }

    public City getCityById(Long id) {
        return cityRepository.findById(id).orElse(null);
    }
}

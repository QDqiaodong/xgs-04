package com.bookexchange.controller;

import com.bookexchange.dto.Result;
import com.bookexchange.entity.City;
import com.bookexchange.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public Result<List<City>> getAllCities() {
        return Result.success(cityService.getAllCities());
    }

    @GetMapping("/province/{province}")
    public Result<List<City>> getCitiesByProvince(@PathVariable String province) {
        return Result.success(cityService.getCitiesByProvince(province));
    }

    @GetMapping("/{id}")
    public Result<City> getCityById(@PathVariable Long id) {
        City city = cityService.getCityById(id);
        return city != null ? Result.success(city) : Result.error("城市不存在");
    }
}

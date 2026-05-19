package com.example.geoquiz_frontend.domain.entities;

import com.example.geoquiz_frontend.data.remote.dtos.solo.BootstrapResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountryGrouping {
    private final List<BootstrapResponse.CountryDto> allCountries;
    private final Map<String, List<BootstrapResponse.CountryDto>> countriesByRegion;
    private final List<String> regions;

    public CountryGrouping(List<BootstrapResponse.CountryDto> allCountries) {
        this.allCountries = new ArrayList<>(allCountries);
        this.countriesByRegion = new HashMap<>();

        for (BootstrapResponse.CountryDto country : allCountries) {
            String region = country.getRegion();
            if (!countriesByRegion.containsKey(region)) {
                countriesByRegion.put(region, new ArrayList<>());
            }
            countriesByRegion.get(region).add(country);
        }

        this.regions = new ArrayList<>(countriesByRegion.keySet());
    }

    public List<BootstrapResponse.CountryDto> getAllCountries() {
        return allCountries;
    }

    public List<BootstrapResponse.CountryDto> getCountriesByRegion(String region) {
        return countriesByRegion.getOrDefault(region, new ArrayList<>());
    }

    public List<String> getRegions() {
        return regions;
    }
}

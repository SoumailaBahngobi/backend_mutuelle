package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.ContributionPeriod;
import com.wbf.mutuelle.services.ContributionPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("mutuelle/contribution_period")
@RequiredArgsConstructor
public class ContributionPeriodController {

    private final ContributionPeriodService service;

    @GetMapping
    public List<ContributionPeriod> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ContributionPeriod getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ContributionPeriod create(@RequestBody ContributionPeriod period) {
        return service.create(period);
    }

    @PutMapping("/{id}")
    public ContributionPeriod update(@PathVariable Long id, @RequestBody ContributionPeriod period) {
        return service.update(id, period);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

package hu.nav.m2m.submitter.controller;

import hu.nav.m2m.submitter.service.M2mAvailabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST végpont a NAV M2M integráció konfigurációs és futásidejű elérhetőségének lekérdezésére.
 */
@RestController
@RequestMapping("/api/m2m")
public class M2mAvailabilityController {
    private final M2mAvailabilityService availabilityService;

    /**
     * Létrehozza a(z) {@code M2mAvailabilityController} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param availabilityService a művelethez átadott {@code availabilityService} érték
     */
    public M2mAvailabilityController(M2mAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * Összeállítja az M2M funkció aktuális elérhetőségi állapotát a konfiguráció alapján.
     *
     * @return a művelet eredménye
     */
    @GetMapping("/availability")
    public M2mAvailabilityService.Availability availability() {
        return availabilityService.availability();
    }
}

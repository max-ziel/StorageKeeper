package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.CreateHouseholdUseCase;
import io.github.maxziel.storagekeeper.application.port.in.JoinHouseholdUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ShowHouseholdUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST endpoints for the current user's household. Singular path: a user is in at most one household,
 * so there is no id in the URL.
 */
@RestController
@Tag(name = "Haushalt", description = "Haushalt anlegen, ansehen und per Einladungscode beitreten")
@RequestMapping("/api/household")
class HouseholdController {

    private final CreateHouseholdUseCase createHousehold;
    private final ShowHouseholdUseCase showHousehold;
    private final JoinHouseholdUseCase joinHousehold;

    HouseholdController(CreateHouseholdUseCase createHousehold, ShowHouseholdUseCase showHousehold,
                        JoinHouseholdUseCase joinHousehold) {
        this.createHousehold = createHousehold;
        this.showHousehold = showHousehold;
        this.joinHousehold = joinHousehold;
    }

    /** The household has no id in the URL (see class comment), so the {@code Location} is the singular path. */
    @PostMapping
    ResponseEntity<HouseholdResponse> create(@Valid @RequestBody CreateHouseholdRequest request) {
        HouseholdResponse household = HouseholdResponse.from(createHousehold.create(request.toHouseholdName()));
        return ResponseEntity.created(URI.create("/api/household")).body(household);
    }

    @GetMapping
    HouseholdResponse show() {
        return HouseholdResponse.from(showHousehold.getCurrent());
    }

    @PostMapping("/join")
    HouseholdResponse join(@Valid @RequestBody JoinHouseholdRequest request) {
        return HouseholdResponse.from(joinHousehold.join(request.toInviteCode()));
    }
}

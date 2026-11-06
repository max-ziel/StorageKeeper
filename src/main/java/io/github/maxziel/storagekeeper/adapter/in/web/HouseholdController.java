package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.CreateHouseholdUseCase;
import io.github.maxziel.storagekeeper.application.port.in.JoinHouseholdUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ShowHouseholdUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    HouseholdResponse create(@Valid @RequestBody CreateHouseholdRequest request) {
        return HouseholdResponse.from(createHousehold.create(request.toHouseholdName()));
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

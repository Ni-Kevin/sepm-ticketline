package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CheckoutSummarySeatholdDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatHoldCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.SeatHold;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.service.SeatHoldService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * REST endpoint for seat hold operations.
 */
@RestController
@RequestMapping("/api/v1/holds")
public class SeatHoldEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final SeatHoldService seatHoldService;
    private final UserService userService;

    public SeatHoldEndpoint(SeatHoldService seatHoldService, UserService userService) {
        this.seatHoldService = seatHoldService;
        this.userService = userService;
    }

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/{id}")
    @Operation(summary = "Get checkout summary for a performance", security = @SecurityRequirement(name = "apiKey"))
    public CheckoutSummarySeatholdDto getCheckoutSummary(@PathVariable("id") Long performanceId, Authentication auth) {
        LOGGER.info("GET /api/v1/holds/{}", performanceId);

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());
        return seatHoldService.getCheckoutSummary(performanceId, user.getId());
    }


    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{id}")
    @Operation(summary = "Create a seat hold for performance id", security = @SecurityRequirement(name = "apiKey"))
    public List<Long> createHold(@PathVariable("id") Long performanceId,
                               Authentication auth,
                               @Valid @RequestBody SeatHoldCreateDto seatHoldCreateDto) throws ValidationException {
        LOGGER.info("POST /api/v1/holds/{} body: {}", performanceId, seatHoldCreateDto);

        List<SeatHold> holds = seatHoldService.createHold(
            performanceId,
            auth.getName(),
            seatHoldCreateDto);

        return holds.stream().map(SeatHold::getId).toList();
    }


    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @Operation(summary = "Release a seat hold with hold id", security = @SecurityRequirement(name = "apiKey"))
    public void releaseHold(@PathVariable("id") Long id, Authentication auth) throws ValidationException {
        LOGGER.info("DELETE /api/v1/holds/{} by user {}", id, auth.getName());

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());

        if (!seatHoldService.isHoldValid(id, user.getId())) {
            throw new ValidationException("Validation error", List.of("Hold is not valid or does not belong to the user"));
        }

        seatHoldService.releaseHold(id);
    }

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/cancel-all/{performanceId}")
    @Operation(summary = "Cancel all holds for a performance and user", security = @SecurityRequirement(name = "apiKey"))
    public void cancelAllHolds(@PathVariable("performanceId") Long performanceId, Authentication auth) {
        LOGGER.info("DELETE /api/v1/holds/cancel-all/{} by user {}", performanceId, auth.getName());

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());
        seatHoldService.releaseAllHoldsForPerformanceAndUser(performanceId, user.getId());
    }
}
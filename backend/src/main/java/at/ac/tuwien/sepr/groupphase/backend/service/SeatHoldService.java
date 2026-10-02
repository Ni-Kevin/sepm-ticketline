package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CheckoutSummarySeatholdDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatHoldCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.SeatHold;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;

import java.util.List;

public interface SeatHoldService {

    /**
     * Creates holds for seats or standing areas.
     *
     * @param performanceId     id of the performance
     * @param email             email of the user
     * @param seatHoldCreateDto contains data of to holded seats
     * @return list of persisted seat holds
     */
    List<SeatHold> createHold(Long performanceId, String email, SeatHoldCreateDto seatHoldCreateDto) throws ValidationException;

    /**
     * Releases a hold by id.
     *
     * @param id id of the hold to release
     */
    void releaseHold(Long id);


    /**
     * Releases all holds from user for a performance.
     *
     * @param performanceId id of performance to release
     * @param userId        id of user
     */
    void releaseAllHoldsForPerformanceAndUser(Long performanceId, Long userId);

    /*
     * @param id of the hold to check
     * @param userId id of the user who should own the hold
     * @return true if the hold exists, is not expired, and belongs to the user
     */
    boolean isHoldValid(Long id, Long userId);

    /**
     * Retrieves the checkout summary for a specific performance.
     *
     * @param performanceId id of the performance
     * @param userId        id of the user
     * @return checkout summary containing held items and total amount
     */
    CheckoutSummarySeatholdDto getCheckoutSummary(Long performanceId, Long userId);
}

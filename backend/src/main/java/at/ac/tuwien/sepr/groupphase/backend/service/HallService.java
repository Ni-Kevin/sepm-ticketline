package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallArea;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;

import java.util.List;

public interface HallService {

    List<Hall> findAll(Long venueId);

    Hall findOne(Long id);

    Hall findLayout(Long id);

    boolean isLayoutLocked(Long id);

    boolean isDimensionsLocked(Long id);

    String getDimensionsLockReason(Long id);

    Hall create(Hall hall, Long venueId) throws ValidationException;

    Hall update(Long id, Hall hall, Long venueId) throws ValidationException;

    Hall updateLayout(Long id, List<Sector> sectors, List<HallArea> areas) throws ValidationException;
}

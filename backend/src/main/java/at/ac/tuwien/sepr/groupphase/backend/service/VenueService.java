package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;

import java.util.List;

public interface VenueService {

    List<Venue> findAll();

    Venue findOne(Long id);

    Venue create(Venue venue);

    Venue update(Long id, Venue venue);
}

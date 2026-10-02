package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.VenueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class VenueServiceImpl implements VenueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String FIXED_COUNTRY = "Austria";
    private final VenueRepository venueRepository;

    public VenueServiceImpl(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Override
    public List<Venue> findAll() {
        LOGGER.debug("Find all venues");
        return venueRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Override
    public Venue findOne(Long id) {
        LOGGER.debug("Find venue with id {}", id);
        return venueRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(String.format("Could not find venue with id %s", id)));
    }

    @Override
    public Venue create(Venue venue) {
        LOGGER.debug("Create venue {}", venue);
        venue.setCountry(FIXED_COUNTRY);
        return venueRepository.save(venue);
    }

    @Override
    public Venue update(Long id, Venue venue) {
        LOGGER.debug("Update venue with id {}", id);
        Venue existingVenue = findOne(id);
        existingVenue.setName(venue.getName());
        existingVenue.setStreet(venue.getStreet());
        existingVenue.setCity(venue.getCity());
        existingVenue.setCountry(FIXED_COUNTRY);
        existingVenue.setZipCode(venue.getZipCode());
        return venueRepository.save(existingVenue);
    }
}

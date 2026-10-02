package at.ac.tuwien.sepr.groupphase.backend.datagenerator;

import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.PerformanceSectorPrice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.SectorType;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("generateData")
@DependsOn("baseDataGenerator")
public class EventDataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TARGET_EVENT_COUNT = 1000;
    private static final int BATCH_SIZE = 50;

    private final EventRepository eventRepository;
    private final PerformanceRepository performanceRepository;
    private final HallRepository hallRepository;
    private final SectorRepository sectorRepository;
    private final ArtistRepository artistRepository;

    public EventDataGenerator(EventRepository eventRepository,
                              PerformanceRepository performanceRepository,
                              HallRepository hallRepository,
                              SectorRepository sectorRepository,
                              ArtistRepository artistRepository) {
        this.eventRepository = eventRepository;
        this.performanceRepository = performanceRepository;
        this.hallRepository = hallRepository;
        this.sectorRepository = sectorRepository;
        this.artistRepository = artistRepository;
    }


    private byte[] loadImageAsBytes(String imageName) {
        InputStream is = getClass().getClassLoader().getResourceAsStream("testdata-event-images/" + imageName);

        if (is == null) {
            if (imageName.endsWith(".jpg")) {
                is = getClass().getClassLoader().getResourceAsStream("testdata-event-images/" + imageName.replace(".jpg", ".jpeg"));
            } else if (imageName.endsWith(".jpeg")) {
                is = getClass().getClassLoader().getResourceAsStream("testdata-event-images/" + imageName.replace(".jpeg", ".jpg"));
            }
        }

        if (is == null) {
            LOGGER.warn("Image not found: {}", imageName);
            return null;
        }

        try (final InputStream finalIs = is) {
            return finalIs.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    @PostConstruct
    @Transactional
    public void generateEventData() {
        long existing = eventRepository.count();
        if (existing >= TARGET_EVENT_COUNT) {
            LOGGER.debug("Events already generated ({} existing). Skipping.", existing);
            return;
        }

        List<Hall> halls = hallRepository.findAllByOrderByNameAsc();
        if (halls.isEmpty()) {
            LOGGER.warn("No halls found. Cannot generate events.");
            return;
        }

        List<Artist> artists = artistRepository.findAll();
        if (artists.isEmpty()) {
            LOGGER.warn("No artists found. Cannot generate events.");
            return;
        }

        int toCreate = TARGET_EVENT_COUNT - (int) existing;
        LOGGER.debug("Generating {} additional events with performances", toCreate);

        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(19).withMinute(30).withSecond(0).withNano(0);
        String[] genres = {"Cinema", "Comedy", "Concert", "Dance", "Exhibition", "Festival", "Musical", "Opera", "Sport", "Theater", "Other"};

        List<Event> batch = new ArrayList<>();
        for (int i = 0; i < toCreate; i++) {
            int index = (int) existing + i + 1;
            Event event = new Event();
            event.setTitle("Event " + index);
            event.setGenre(genres[index % genres.length]);
            event.setDescription("Description for Event " + index);
            event.setStartTime(baseTime.plusDays(index));
            event.setEndTime(baseTime.plusDays(index).plusHours(3));
            byte[] imageBytes = loadImageAsBytes(genres[index % genres.length] + ".jpg");
            event.setImage(imageBytes);
            batch.add(event);

            if (batch.size() >= BATCH_SIZE) {
                eventRepository.saveAll(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            eventRepository.saveAll(batch);
        }

        List<Event> allEvents = eventRepository.findAll();
        int eventIndex = 0;
        List<Performance> perfBatch = new ArrayList<>();
        for (Event event : allEvents) {
            if (eventIndex < (int) existing) {
                eventIndex++;
                continue;
            }

            Hall hall = halls.get(eventIndex % halls.size());
            BigDecimal basePrice = BigDecimal.valueOf(50 + (eventIndex % 10) * 10);

            Performance perf = new Performance();
            perf.setPerformanceName("Performance " + (eventIndex + 1));
            perf.setStartTime(event.getStartTime());
            perf.setEndTime(event.getEndTime());
            perf.setStartPrice(basePrice);
            perf.setHall(hall);
            perf.setEvent(event);
            perf.setGenre(event.getGenre());
            Artist artist = artists.get(eventIndex % artists.size());
            perf.getArtists().add(artist);

            List<Sector> sectors = sectorRepository.findByHallIdOrderByNameAsc(hall.getId());
            for (Sector sector : sectors) {
                PerformanceSectorPrice psp = new PerformanceSectorPrice();
                psp.setSectorId(sector.getId());
                BigDecimal sectorPrice = sector.getType() == SectorType.SEATING
                    ? basePrice.multiply(BigDecimal.valueOf(1.2)).setScale(2, RoundingMode.HALF_UP)
                    : basePrice;
                psp.setPrice(sectorPrice);
                psp.setPerformance(perf);
                psp.setSectorName(sector.getName());
                psp.setSectorType(String.valueOf(sector.getType()));
                perf.getSectorPrices().add(psp);
            }

            perfBatch.add(perf);
            eventIndex++;

            if (perfBatch.size() >= 50) {
                performanceRepository.saveAll(perfBatch);
                perfBatch.clear();
            }
        }

        if (!perfBatch.isEmpty()) {
            performanceRepository.saveAll(perfBatch);
        }

        LOGGER.debug("Successfully generated {} events with performances", toCreate);
    }
}

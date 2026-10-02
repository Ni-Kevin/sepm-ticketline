import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {EventService} from '../../services/event.service';
import {Event} from '../../dtos/event';
import {Performance} from '../../dtos/performance';
import {PerformanceService} from '../../services/performance.service';

@Component({
  selector: 'app-event-performances',
  templateUrl: './event-performances.component.html',
  styleUrls: ['./event-performances.component.scss'],
  standalone: false
})
export class EventPerformancesComponent implements OnInit {
  event: Event | null = null;
  eventId: number | null = null;
  performances: Performance[] = [];
  errorMessage = '';

  constructor(private route: ActivatedRoute,
              private eventService: EventService,
              private performanceService: PerformanceService,
              private router: Router) {
  }

  ngOnInit(): void {
    const eventIdParam = this.route.snapshot.paramMap.get('id');
    const parsedEventId = eventIdParam ? Number(eventIdParam) : NaN;

    if (!Number.isFinite(parsedEventId)) {
      this.errorMessage = 'Invalid event id';
      return;
    }

    this.eventId = parsedEventId;
    this.loadEvent();
  }

  private loadEvent(): void {
    this.errorMessage = '';
    this.eventService.getEventById(this.eventId!).subscribe({
      next: event => {
        this.event = event;
        this.loadPerformances(this.event.performanceIds ?? []);
      },
      error: () => {
        this.errorMessage = 'Could not load performances';
      }
    });
  }

  private loadPerformances(performanceIds: number[]): void {
    this.performanceService.getPerformancesByIds(performanceIds).subscribe({
      next: performances => {
        this.performances = performances;
      },
      error: () => {
        this.errorMessage = 'Could not load performances';
      }
    });
  }

  isPerformanceStarted(performance: Performance): boolean {
    const startTime = new Date(performance.startTime).getTime();
    return Number.isFinite(startTime) && startTime <= Date.now();
  }

  selectSeats(performance: Performance): void {
    if (this.isPerformanceStarted(performance)) {
      return;
    }

    if (performance.id) {
      this.router.navigate(['/seat-selection', performance.id]);
    }
  }
}

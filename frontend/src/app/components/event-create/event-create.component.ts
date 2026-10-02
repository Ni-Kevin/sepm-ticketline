import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {EventService} from '../../services/event.service';
import {Event} from '../../dtos/event';
import {Artist} from '../../dtos/artist';
import {PerformanceService} from '../../services/performance.service';
import {EVENT_GENRES} from '../../constants/event-genres';

interface EventDraftPerformance {
  id: number;
  startTime: string;
  performanceName?: string;
  startPrice: number;
  hallName: string;
  artists: Artist[];
}

interface EventDraftDetails {
  event: Event;
  startTime: string;
  endTime: string;
}

@Component({
  selector: 'app-event-create',
  templateUrl: './event-create.component.html',
  styleUrls: ['./event-create.component.scss'],
  standalone: false
})
export class EventCreateComponent implements OnInit {

  private static readonly EVENT_DRAFT_PERFORMANCES_KEY = 'eventDraftPerformances';
  private static readonly EVENT_DRAFT_DETAILS_KEY = 'eventDraftDetails';
  private readonly maxImageSizeBytes = 1024 * 1024;
  private readonly allowedImageTypes = ['image/jpeg', 'image/png', 'image/webp'];
  private readonly allowedImageExtensions = ['.jpg', '.jpeg', '.png', '.webp'];

  event: Event = {
    title: '',
    genre: '',
    description: ''
  };

  startTime = '';
  endTime = '';
  fieldErrors: Record<string, string> = {};
  errorMessage = '';
  photoErrorMessage = '';
  genres = EVENT_GENRES;
  selectedPhotoName = '';
  photoPreviewUrl: string | null = null;
  selectedPhotoFile: File | null = null;
  performances: EventDraftPerformance[] = [];
  selectedArtists: Artist[] = [];

  constructor(private eventService: EventService,
               private route: ActivatedRoute,
               private router: Router,
               private performanceService: PerformanceService) {
  }

  ngOnInit() {
    this.loadDraftDetails();
    this.loadDraftPerformances();
    this.route.queryParamMap.subscribe(params => {
      if (params.get('performanceCreated') === '1') {
        this.loadDraftPerformances();
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {performanceCreated: null},
          queryParamsHandling: 'merge',
          replaceUrl: true
        });
      }
    });
  }

  onCreate() {
    this.errorMessage = '';
    this.photoErrorMessage = '';
    this.fieldErrors = {};

    if (!this.event.title?.trim()) {
      this.fieldErrors['title'] = 'Title is required!';
    }
    if (!this.event.genre?.trim()) {
      this.fieldErrors['genre'] = 'Genre is required!';
    }
    if (!this.startTime) {
      this.fieldErrors['startTime'] = 'Start time is required!';
    }
    if (!this.endTime) {
      this.fieldErrors['endTime'] = 'End time is required!';
    } else if (this.startTime && this.endTime < this.startTime) {
      this.fieldErrors['endTime'] = 'End time must not be before start time!';
    }
    if (this.performances.length === 0) {
      this.fieldErrors['performances'] = 'At least one performance is required!';
    }

    if (this.selectedPhotoFile) {
      if (this.selectedPhotoFile.size > this.maxImageSizeBytes) {
        this.photoErrorMessage = 'Selected image is too large. Maximum file size is 1 MB.';
      } else if (!this.isAllowedImageType(this.selectedPhotoFile)) {
        this.photoErrorMessage = 'Only PNG, JPEG and WebP images are allowed.';
      }
    }

    if (Object.keys(this.fieldErrors).length > 0 || !!this.photoErrorMessage) {
      return;
    }

    const eventToCreate: Event = {
      ...this.event,
      startTime: this.startTime,
      endTime: this.endTime,
      performanceIds: this.performances.map(performance => performance.id)
    };

    this.eventService.createEvent(eventToCreate, this.selectedPhotoFile).subscribe({
      next: () => {
        sessionStorage.removeItem(EventCreateComponent.EVENT_DRAFT_DETAILS_KEY);
        sessionStorage.removeItem(EventCreateComponent.EVENT_DRAFT_PERFORMANCES_KEY);
        this.router.navigate(['/events'], {queryParams: {created: '1'}});
      },
      error: error => {
        this.errorMessage = this.extractErrorMessage(error);
      }
    });
  }

  onPhotoSelected(event: any) {
    this.photoErrorMessage = '';
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    if (!file) {
      this.selectedPhotoName = '';
      this.photoPreviewUrl = null;
      this.selectedPhotoFile = null;
      return;
    }

    this.selectedPhotoName = file.name;
    this.selectedPhotoFile = file;
    this.photoPreviewUrl = URL.createObjectURL(file);
  }

  removeSelectedPhoto(fileInput: HTMLInputElement) {
    this.photoErrorMessage = '';
    this.selectedPhotoName = '';
    this.photoPreviewUrl = null;
    this.selectedPhotoFile = null;
    fileInput.value = '';
  }

  removePerformance(performanceId: number) {
    this.performanceService.deletePerformance(performanceId).subscribe({
      next: () => {
        this.performances = this.performances.filter(performance => performance.id !== performanceId);
        this.persistDraftPerformances();
        this.refreshSelectedArtists();
      },
      error: () => {
        this.errorMessage = 'Could not delete performance';
      }
    });
  }

  goToPerformanceCreate() {
    this.persistDraftDetails();
    this.router.navigate(['/admin/performances/new']);
  }

  cancelCreate() {
    sessionStorage.removeItem(EventCreateComponent.EVENT_DRAFT_DETAILS_KEY);
    sessionStorage.removeItem(EventCreateComponent.EVENT_DRAFT_PERFORMANCES_KEY);
    this.router.navigate(['/events']);
  }

  private loadDraftDetails() {
    const rawValue = sessionStorage.getItem(EventCreateComponent.EVENT_DRAFT_DETAILS_KEY);
    if (!rawValue) {
      return;
    }

    try {
      const draft = JSON.parse(rawValue) as EventDraftDetails;
      this.event = {
        title: draft.event?.title ?? '',
        genre: draft.event?.genre ?? '',
        description: draft.event?.description ?? ''
      };
      this.startTime = draft.startTime ?? '';
      this.endTime = draft.endTime ?? '';
    } catch {
      sessionStorage.removeItem(EventCreateComponent.EVENT_DRAFT_DETAILS_KEY);
    }
  }

  private persistDraftDetails() {
    const draft: EventDraftDetails = {
      event: this.event,
      startTime: this.startTime,
      endTime: this.endTime
    };
    sessionStorage.setItem(EventCreateComponent.EVENT_DRAFT_DETAILS_KEY, JSON.stringify(draft));
  }

  private loadDraftPerformances() {
    const rawValue = sessionStorage.getItem(EventCreateComponent.EVENT_DRAFT_PERFORMANCES_KEY);
    if (!rawValue) {
      this.performances = [];
      this.selectedArtists = [];
      return;
    }

    try {
      this.performances = JSON.parse(rawValue) as EventDraftPerformance[];
      this.refreshSelectedArtists();
    } catch {
      this.performances = [];
      this.selectedArtists = [];
    }
  }

  private persistDraftPerformances() {
    sessionStorage.setItem(EventCreateComponent.EVENT_DRAFT_PERFORMANCES_KEY, JSON.stringify(this.performances));
  }

  private refreshSelectedArtists() {
    const artists = this.performances.flatMap(performance => performance.artists ?? []);
    const uniqueArtists = new Map<number, Artist>();
    for (const artist of artists) {
      if (artist.id !== undefined) {
        uniqueArtists.set(artist.id, artist);
      }
    }
    this.selectedArtists = Array.from(uniqueArtists.values());
  }

  private isAllowedImageType(file: File): boolean {
    if (this.allowedImageTypes.includes(file.type)) {
      return true;
    }

    const lowerCaseName = file.name.toLowerCase();
    return this.allowedImageExtensions.some(extension => lowerCaseName.endsWith(extension));
  }

  private extractErrorMessage(error: any): string {
    if (typeof error?.error === 'string') {
      return error.error;
    }
    if (Array.isArray(error?.error?.errors)) {
      return error.error.errors.join(' ').trim();
    }
    return error?.error?.detail ?? error?.error?.message ?? 'Could not create event';
  }
}

import {Component, ElementRef, OnInit, QueryList, ViewChildren} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Artist} from '../../dtos/artist';
import {ArtistService} from '../../services/artist.service';
import {PerformanceService} from '../../services/performance.service';
import {Performance} from '../../dtos/performance';
import {HallService} from '../../services/hall.service';
import {Hall} from '../../dtos/hall';
import {HallAreaLayout, HallLayout, SectorLayout} from '../../dtos/hall-layout';

interface SectorPriceDraft {
  sectorId?: number;
  sectorName: string;
  sectorType: 'SEATING' | 'STANDING';
  price: number | null;
}

interface EventDraftPerformance {
  id: number;
  startTime: string;
  endTime: string;
  performanceName: string;
  startPrice: number;
  hallName: string;
  artists: Artist[];
  sectorPrices?: SectorPriceDraft[];
}

interface PerformanceDraftDetails {
  startTime: string;
  durationHours: number;
  durationMinutes: number;
  performanceName: string;
  artistName: string;
  selectedArtists: Artist[];
  hallQuery: string;
  selectedHallId: number | null;
  sectorPrices: SectorPriceDraft[];
}

@Component({
  selector: 'app-performance-create',
  templateUrl: './performance-create.component.html',
  styleUrls: ['./performance-create.component.scss'],
  standalone: false
})
export class PerformanceCreateComponent implements OnInit {

  private static readonly EVENT_DRAFT_PERFORMANCES_KEY = 'eventDraftPerformances';
  private static readonly PERFORMANCE_DRAFT_DETAILS_KEY = 'performanceDraftDetails';

  startTime = '';
  durationHours = 0;
  durationMinutes = 0;
  performanceName = '';
  startPrice = 0;
  artistName = '';
  fieldErrors: Record<string, string> = {};
  errorMessage = '';
  infoMessage = '';
  artistSuggestions: Artist[] = [];
  selectedArtists: Artist[] = [];
  halls: Hall[] = [];
  hallQuery = '';
  selectedHallId: number | null = null;
  selectedHallLayout: HallLayout | null = null;
  sectorPrices: SectorPriceDraft[] = [];
  loadingHallLayout = false;
  selectedPricingArea: HallAreaLayout | null = null;

  @ViewChildren('sectorPriceInput') private sectorPriceInputs!: QueryList<ElementRef<HTMLInputElement>>;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private artistService: ArtistService,
              private performanceService: PerformanceService,
              private hallService: HallService) {
  }

  ngOnInit() {
    this.loadDraftDetails();
    this.loadHalls();

    this.route.queryParamMap.subscribe(params => {
      if (params.get('artistCreated') !== '1') {
        return;
      }

      this.showArtistCreatedMessage();
      this.removeArtistCreatedQueryParam();
    });
  }

  private loadHalls() {
    this.hallService.getHalls().subscribe({
      next: halls => {
        this.halls = halls;
      },
      error: () => {
        this.halls = [];
      }
    });
  }

  onHallChange() {
    const selectedHall = this.halls.find(hall => hall.id === this.selectedHallId);
    this.hallQuery = selectedHall?.name ?? this.hallQuery;
    this.selectedHallLayout = null;
    this.sectorPrices = [];
    this.selectedPricingArea = null;
  }

  get filteredHalls(): Hall[] {
    const query = this.hallQuery.trim().toLowerCase();
    if (!query) {
      return this.halls;
    }
    return this.halls.filter(hall => hall.name.toLowerCase().includes(query));
  }

  onHallSearchInput() {
    const exactMatch = this.halls.find(hall => hall.name.toLowerCase() === this.hallQuery.trim().toLowerCase());
    if (!exactMatch) {
      this.selectedHallId = null;
      this.selectedHallLayout = null;
      this.sectorPrices = [];
      this.selectedPricingArea = null;
      return;
    }

    if (this.selectedHallId !== exactMatch.id) {
      this.selectedHallId = exactMatch.id ?? null;
      this.onHallChange();
      this.showHallLayout();
    }
  }

  selectHall(hall: Hall) {
    this.selectedHallId = hall.id ?? null;
    this.hallQuery = hall.name;
    this.onHallChange();
    this.showHallLayout();
  }

  showHallLayout() {
    this.errorMessage = '';

    if (this.selectedHallId == null) {
      this.errorMessage = 'Please select a hall first';
      return;
    }

    this.loadingHallLayout = true;
    this.hallService.getLayout(this.selectedHallId).subscribe({
      next: layout => {
        this.selectedHallLayout = layout;
        const fallbackPrice = this.startPrice;
        this.sectorPrices = layout.sectors.map(sector => {
          const existing = this.sectorPrices.find(price => this.isSameSector(price, sector));
          return {
            sectorId: sector.id,
            sectorName: sector.name,
            sectorType: sector.type,
            price: existing?.price ?? fallbackPrice
          };
        });
        this.selectedPricingArea = null;
        this.loadingHallLayout = false;
      },
      error: () => {
        this.selectedHallLayout = null;
        this.sectorPrices = [];
        this.selectedPricingArea = null;
        this.errorMessage = 'Could not load hall layout';
        this.loadingHallLayout = false;
      }
    });
  }

  onPricingAreaSelected(area: HallAreaLayout) {
    if (area.type === 'STAGE') {
      this.selectedPricingArea = null;
      return;
    }
    this.selectedPricingArea = area;
    this.focusSelectedSectorPriceInput();
  }

  get selectedSectorPrice(): SectorPriceDraft | null {
    if (!this.selectedPricingArea) {
      return null;
    }

    if (this.selectedPricingArea.sectorId != null) {
      const byId = this.sectorPrices.find(price => price.sectorId === this.selectedPricingArea?.sectorId);
      if (byId) {
        return byId;
      }
    }

    if (this.selectedPricingArea.sectorName) {
      return this.sectorPrices.find(price => price.sectorName.toLowerCase() === this.selectedPricingArea?.sectorName?.toLowerCase()) ?? null;
    }

    return null;
  }

  isSectorPriceFocused(sectorPrice: SectorPriceDraft): boolean {
    return this.selectedSectorPrice === sectorPrice;
  }

  private focusSelectedSectorPriceInput(): void {
    const selected = this.selectedSectorPrice;
    if (!selected) {
      return;
    }

    const index = this.sectorPrices.findIndex(price => price === selected);
    if (index < 0) {
      return;
    }

    setTimeout(() => {
      const input = this.sectorPriceInputs.get(index)?.nativeElement;
      input?.focus();
      input?.select();
    });
  }

  updateSectorPrice(index: number, value: string | number) {
    const parsed = Number(value);
    this.sectorPrices[index].price = Number.isFinite(parsed) ? parsed : null;
  }

  focusSectorFromPrice(sectorPrice: SectorPriceDraft) {
    if (!this.selectedHallLayout) {
      return;
    }

    const matchingArea = this.selectedHallLayout.areas.find(area => {
      if (area.type === 'STAGE') {
        return false;
      }
      if (sectorPrice.sectorId != null && area.sectorId != null) {
        return sectorPrice.sectorId === area.sectorId;
      }
      return !!area.sectorName && area.sectorName.toLowerCase() === sectorPrice.sectorName.toLowerCase();
    });

    this.selectedPricingArea = matchingArea ?? null;
  }

  private isSameSector(price: SectorPriceDraft, sector: SectorLayout): boolean {
    if (price.sectorId != null && sector.id != null) {
      return price.sectorId === sector.id;
    }
    return price.sectorName.toLowerCase() === sector.name.toLowerCase();
  }

  private showArtistCreatedMessage() {
    this.infoMessage = 'Artist created successfully';
  }

  private removeArtistCreatedQueryParam() {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {artistCreated: null},
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  goToArtistCreate() {
    this.persistDraftDetails();
    this.router.navigate(['/admin/artists/new']);
  }

  cancelCreate() {
    sessionStorage.removeItem(PerformanceCreateComponent.PERFORMANCE_DRAFT_DETAILS_KEY);
    this.router.navigate(['/admin/events/new']);
  }

  onCreate() {
    this.errorMessage = '';
    this.fieldErrors = {};

    if (!this.performanceName.trim()) {
      this.fieldErrors['performanceName'] = 'Performance name is required!';
    }
    if (!this.startTime) {
      this.fieldErrors['startTime'] = 'Start time is required!';
    }
    if (this.durationHours < 0) {
      this.fieldErrors['durationHours'] = 'Duration hours must be greater than or equal to 0!';
    }
    if (this.durationMinutes < 0 || this.durationMinutes > 59) {
      this.fieldErrors['durationMinutes'] = 'Duration minutes must be between 0 and 59!';
    }
    if (this.durationHours === 0 && this.durationMinutes === 0) {
      this.fieldErrors['durationMinutes'] = 'Duration must be greater than 0 minutes!';
    }
    if (this.selectedArtists.length === 0) {
      this.fieldErrors['artists'] = 'At least one artist is required!';
    }
    if (this.selectedHallId == null) {
      this.fieldErrors['hall'] = 'Hall is required!';
    }
    if (this.sectorPrices.some(price => price.price == null || price.price < 0)) {
      this.fieldErrors['sectorPrices'] = 'Each standing/seating sector needs a valid price!';
    }
    if (this.sectorPrices.some(price => price.sectorId == null)) {
      this.fieldErrors['sectorPrices'] = 'Each standing/seating sector needs a valid sector id!';
    }

    if (Object.keys(this.fieldErrors).length > 0) {
      return;
    }

    const effectiveStartPrice = this.getEffectiveStartPrice();
    const calculatedEndTime = this.calculatedEndTimeDisplay;
    if (!calculatedEndTime) {
      this.errorMessage = 'Could not calculate end time from start time and duration';
      return;
    }

    const selectedHallId = this.selectedHallId;
    if (selectedHallId == null) {
      this.fieldErrors['hall'] = 'Hall is required!';
      return;
    }

    const performance: Performance = {
      startTime: this.startTime,
      durationHours: this.durationHours,
      durationMinutes: this.durationMinutes,
      performanceName: this.performanceName.trim(),
      startPrice: effectiveStartPrice,
      hallId: selectedHallId,
      artistIds: this.selectedArtists
        .map(artist => artist.id)
        .filter((id): id is number => id !== undefined),
      sectorPrices: this.sectorPrices
        .filter(price => price.price != null && price.sectorId != null)
        .map(price => ({
          sectorId: price.sectorId as number,
          sectorName: price.sectorName,
          sectorType: price.sectorType,
          price: Number(price.price)
        }))
    };

    this.performanceService.createPerformance(performance).subscribe({
      next: createdPerformance => {
        sessionStorage.removeItem(PerformanceCreateComponent.PERFORMANCE_DRAFT_DETAILS_KEY);
        this.appendDraftPerformance({
          id: createdPerformance.id ?? Date.now(),
          startTime: this.startTime,
          endTime: createdPerformance.endTime ?? calculatedEndTime,
          performanceName: this.performanceName.trim(),
          startPrice: effectiveStartPrice,
          hallName: this.getSelectedHallName(),
          artists: this.selectedArtists,
          sectorPrices: this.sectorPrices
        });
        this.router.navigate(['/admin/events/new'], {queryParams: {performanceCreated: '1'}});
      },
      error: error => {
        this.errorMessage = typeof error?.error === 'string'
          ? error.error
          : (error?.error?.detail ?? error?.error?.message ?? 'Could not create performance');
      }
    });
  }

  onArtistSearch() {
    const query = this.artistName.trim();
    if (!query) {
      this.artistSuggestions = [];
      return;
    }

    this.artistService.searchArtists(query).subscribe({
      next: artists => {
        this.artistSuggestions = artists.filter(artist => !this.selectedArtists.some(selected => selected.id === artist.id));
      },
      error: () => {
        this.artistSuggestions = [];
      }
    });
  }

  addArtist(artist: Artist) {
    if (!this.selectedArtists.some(selected => selected.id === artist.id)) {
      this.selectedArtists.push(artist);
    }
    this.artistName = '';
    this.artistSuggestions = [];
  }

  removeArtist(artistId: number | undefined) {
    this.selectedArtists = this.selectedArtists.filter(artist => artist.id !== artistId);
  }

  private loadDraftDetails() {
    const rawValue = sessionStorage.getItem(PerformanceCreateComponent.PERFORMANCE_DRAFT_DETAILS_KEY);
    if (!rawValue) {
      return;
    }

    try {
      const draft = JSON.parse(rawValue) as PerformanceDraftDetails;
      this.startTime = draft.startTime ?? '';
      this.durationHours = Number(draft.durationHours) || 0;
      this.durationMinutes = Number(draft.durationMinutes) || 0;
      this.performanceName = draft.performanceName ?? '';
      this.artistName = draft.artistName ?? '';
      this.selectedArtists = draft.selectedArtists ?? [];
      this.hallQuery = draft.hallQuery ?? '';
      this.selectedHallId = draft.selectedHallId ?? null;
      this.sectorPrices = draft.sectorPrices ?? [];
      if (this.selectedHallId != null) {
        this.showHallLayout();
      }
    } catch {
      sessionStorage.removeItem(PerformanceCreateComponent.PERFORMANCE_DRAFT_DETAILS_KEY);
    }
  }

  private persistDraftDetails() {
    const draft: PerformanceDraftDetails = {
      startTime: this.startTime,
      durationHours: this.durationHours,
      durationMinutes: this.durationMinutes,
      performanceName: this.performanceName,
      artistName: this.artistName,
      selectedArtists: this.selectedArtists,
      hallQuery: this.hallQuery,
      selectedHallId: this.selectedHallId,
      sectorPrices: this.sectorPrices
    };
    sessionStorage.setItem(PerformanceCreateComponent.PERFORMANCE_DRAFT_DETAILS_KEY, JSON.stringify(draft));
  }

  private getEffectiveStartPrice(): number {
    const sectorValues = this.sectorPrices
      .map(price => Number(price.price))
      .filter(price => Number.isFinite(price) && price >= 0);

    if (sectorValues.length > 0) {
      return Math.min(...sectorValues);
    }

    return Math.max(0, Number(this.startPrice) || 0);
  }

  get calculatedEndTimeDisplay(): string {
    if (!this.startTime) {
      return "-";
    }
    const start = new Date(this.startTime);
    if (Number.isNaN(start.getTime())) {
      return "-";
    }
    const end = new Date(start.getTime());
    end.setHours(end.getHours() + Number(this.durationHours || 0));
    end.setMinutes(end.getMinutes() + Number(this.durationMinutes || 0));
    const year = end.getFullYear();
    const month = String(end.getMonth() + 1).padStart(2, '0');
    const day = String(end.getDate()).padStart(2, '0');
    const hours = String(end.getHours()).padStart(2, '0');
    const minutes = String(end.getMinutes()).padStart(2, '0');
    return `${day}.${month}.${year}  ${hours}:${minutes}`;
  }

  private getSelectedHallName(): string {
    return this.halls.find(hall => hall.id === this.selectedHallId)?.name ?? this.hallQuery.trim();
  }

  private appendDraftPerformance(performance: EventDraftPerformance) {
    const existingPerformances = this.getDraftPerformances();
    existingPerformances.push(performance);
    sessionStorage.setItem(
      PerformanceCreateComponent.EVENT_DRAFT_PERFORMANCES_KEY,
      JSON.stringify(existingPerformances)
    );
  }

  private getDraftPerformances(): EventDraftPerformance[] {
    const rawValue = sessionStorage.getItem(PerformanceCreateComponent.EVENT_DRAFT_PERFORMANCES_KEY);
    if (!rawValue) {
      return [];
    }

    try {
      return JSON.parse(rawValue) as EventDraftPerformance[];
    } catch {
      return [];
    }
  }
}

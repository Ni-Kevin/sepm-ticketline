import {Component, OnInit} from '@angular/core';
import {UntypedFormBuilder, UntypedFormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {HallService} from '../../services/hall.service';
import {VenueService} from '../../services/venue.service';
import {Venue} from '../../dtos/venue';
import {Hall} from '../../dtos/hall';

@Component({
  selector: 'app-hall-form',
  templateUrl: './hall-form.component.html',
  styleUrls: ['./hall-form.component.scss'],
  standalone: false
})
export class HallFormComponent implements OnInit {

  hallForm: UntypedFormGroup;
  venues: Venue[] = [];
  fieldErrors: Record<string, string> = {};
  submitted = false;
  error = false;
  errorMessage = '';
  editMode = false;
  hallId?: number;
  dimensionsLocked = false;
  dimensionsLockReason: 'PERFORMANCE' | 'AREAS' | null = null;

  constructor(private formBuilder: UntypedFormBuilder,
              private route: ActivatedRoute,
              private router: Router,
              private hallService: HallService,
              private venueService: VenueService) {
    this.hallForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      width: [1, [Validators.required, Validators.min(1)]],
      length: [1, [Validators.required, Validators.min(1)]],
      venueId: [null, [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.loadVenues();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editMode = true;
      this.hallId = Number(id);
      this.loadHall(this.hallId);
    }
  }

  saveHall(): void {
    this.submitted = true;
    this.fieldErrors = {};

    if (this.hallForm.controls['name'].errors?.required) {
      this.fieldErrors['name'] = 'Hall name is required!';
    }

    if (this.hallForm.controls['venueId'].errors?.required) {
      this.fieldErrors['venueId'] = 'Venue is required!';
    }

    if (this.hallForm.controls['width'].errors?.required) {
      this.fieldErrors['width'] = 'Width is required!';
    } else if (this.hallForm.controls['width'].errors?.min) {
      this.fieldErrors['width'] = 'Width must be at least 1 meter!';
    }

    if (this.hallForm.controls['length'].errors?.required) {
      this.fieldErrors['length'] = 'Length is required!';
    } else if (this.hallForm.controls['length'].errors?.min) {
      this.fieldErrors['length'] = 'Length must be at least 1 meter!';
    }

    if (this.hallForm.invalid) {
      return;
    }

    const hall: Hall = this.hallForm.getRawValue();
    const request = this.editMode && this.hallId
      ? this.hallService.updateHall(this.hallId, hall)
      : this.hallService.createHall(hall);

    request.subscribe({
      next: savedHall => {
        if (this.editMode) {
          this.router.navigate(['/admin/halls']);
        } else {
          this.router.navigate(['/admin/halls', savedHall.id, 'layout']);
        }
      },
      error: error => {
        this.error = true;
        this.errorMessage = this.extractErrorMessage(error);
      }
    });
  }

  private loadVenues(): void {
    this.venueService.getVenues().subscribe({
      next: venues => this.venues = venues,
      error: error => {
        this.error = true;
        this.errorMessage = this.extractErrorMessage(error);
      }
    });
  }

  private loadHall(id: number): void {
    this.hallService.getHallById(id).subscribe({
      next: hall => {
        this.hallForm.patchValue(hall);
        this.dimensionsLocked = !!hall.dimensionsLocked;
        this.dimensionsLockReason = hall.dimensionsLockReason ?? null;
        if (this.dimensionsLocked) {
          this.hallForm.get('width')?.disable({emitEvent: false});
          this.hallForm.get('length')?.disable({emitEvent: false});
        }
      },
      error: error => {
        this.error = true;
        this.errorMessage = this.extractErrorMessage(error);
      }
    });
  }

  private extractErrorMessage(error: any): string {
    if (error.error?.errors) {
      return error.error.errors.join(', ');
    }
    if (error.error?.message) {
      return error.error.message;
    }
    return error.error ?? 'Could not save hall.';
  }

  protected get dimensionsLockMessage(): string {
    if (this.dimensionsLockReason === 'PERFORMANCE') {
      return 'Width and length are locked because this hall is already used in a performance.';
    }
    if (this.dimensionsLockReason === 'AREAS') {
      return 'Width and length are locked because this hall already has configured areas.';
    }
    return 'Width and length are locked.';
  }
}

import {Component, OnInit} from '@angular/core';
import {UntypedFormBuilder, UntypedFormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {VenueService} from '../../services/venue.service';
import {Venue} from '../../dtos/venue';

@Component({
  selector: 'app-venue-form',
  templateUrl: './venue-form.component.html',
  styleUrls: ['./venue-form.component.scss'],
  standalone: false
})
export class VenueFormComponent implements OnInit {

  readonly country = 'Austria';

  venueForm: UntypedFormGroup;
  fieldErrors: Record<string, string> = {};
  submitted = false;
  error = false;
  errorMessage = '';
  editMode = false;
  venueId?: number;

  constructor(private formBuilder: UntypedFormBuilder,
              private route: ActivatedRoute,
              private router: Router,
              private venueService: VenueService) {
    this.venueForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      street: ['', [Validators.required, Validators.maxLength(200)]],
      city: ['', [Validators.required, Validators.maxLength(100)]],
      zipCode: ['', [Validators.required, Validators.maxLength(4), Validators.pattern(/^\d{4}$/)]],
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editMode = true;
      this.venueId = Number(id);
      this.loadVenue(this.venueId);
    }
  }

  saveVenue(): void {
    this.submitted = true;
    this.fieldErrors = {};

    if (this.venueForm.controls['name'].errors?.required) {
      this.fieldErrors['name'] = 'Venue name is required!';
    }

    if (this.venueForm.controls['street'].errors?.required) {
      this.fieldErrors['street'] = 'Street address is required!';
    }

    if (this.venueForm.controls['city'].errors?.required) {
      this.fieldErrors['city'] = 'City is required!';
    }

    if (this.venueForm.controls['zipCode'].errors?.required) {
      this.fieldErrors['zipCode'] = 'ZIP code is required!';
    } else if (this.venueForm.controls['zipCode'].errors?.pattern || this.venueForm.controls['zipCode'].errors?.maxlength) {
      this.fieldErrors['zipCode'] = 'ZIP code must consist of 4 digits!';
    }

    if (this.venueForm.invalid) {
      return;
    }

    const venue: Venue = {
      ...this.venueForm.value,
      country: this.country
    };
    const request = this.editMode && this.venueId
      ? this.venueService.updateVenue(this.venueId, venue)
      : this.venueService.createVenue(venue);

    request.subscribe({
      next: () => this.router.navigate(['/admin/venues']),
      error: error => {
        this.error = true;
        this.errorMessage = this.extractErrorMessage(error);
      }
    });
  }

  private loadVenue(id: number): void {
    this.venueService.getVenueById(id).subscribe({
      next: venue => this.venueForm.patchValue(venue),
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
    return error.error ?? 'Could not save venue.';
  }
}

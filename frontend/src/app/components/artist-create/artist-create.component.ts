import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {ArtistService} from '../../services/artist.service';
import {Artist} from '../../dtos/artist';
import { Location } from '@angular/common';

@Component({
  selector: 'app-artist-create',
  templateUrl: './artist-create.component.html',
  styleUrls: ['./artist-create.component.scss'],
  standalone: false
})
export class ArtistCreateComponent {

  artist: Artist = {
    firstName: '',
    lastName: '',
    artistName: ''
  };
  infoMessage = '';
  fieldErrors: Record<string, string> = {};
  errorMessage = '';

  constructor(private artistService: ArtistService,
              public location: Location) {
  }

  onCreate() {
    this.infoMessage = '';
    this.errorMessage = '';
    this.fieldErrors = {};

    if (!this.artist.firstName?.trim()) {
      this.fieldErrors['firstName'] = 'First name is required!';
    }

    if (!this.artist.lastName?.trim()) {
      this.fieldErrors['lastName'] = 'Last name is required!';
    }

    if (!this.artist.artistName?.trim()) {
      this.fieldErrors['artistName'] = 'Artist name is required!';
    }

    if (Object.keys(this.fieldErrors).length > 0) {
      return;
    }

    this.artistService.createArtist(this.artist).subscribe({
      next: () => {
        this.location.back();
      },
      error: error => {
        if (typeof error?.error === 'string') {
          this.errorMessage = error.error;
          return;
        }

        if (error?.status === 409) {
          this.errorMessage = 'Artist name already exists';
          return;
        }

        this.errorMessage = error?.error?.message ?? 'Could not create artist';
      }
    });
  }
}

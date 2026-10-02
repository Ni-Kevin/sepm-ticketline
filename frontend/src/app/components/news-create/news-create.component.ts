import {Component} from '@angular/core';
import {NgForm} from '@angular/forms';
import {Router} from '@angular/router';
import {News} from '../../dtos/news';
import {NewsService} from '../../services/news.service';

@Component({
  selector: 'app-news-create',
  templateUrl: './news-create.component.html',
  styleUrls: ['./news-create.component.scss'],
  standalone: false
})
export class NewsCreateComponent {
  private readonly maxImageSizeBytes = 1024 * 1024;
  private readonly allowedImageTypes = ['image/jpeg', 'image/png', 'image/webp'];
  private readonly allowedImageExtensions = ['.jpg', '.jpeg', '.png', '.webp'];

  news: News = {
    title: '',
    summary: '',
    text: ''
  };
  publishedAt = '';
  submitted = false;
  fieldErrors: Record<string, string> = {};
  errorMessage = '';
  photoErrorMessage = '';
  selectedPhotoName = '';
  photoPreviewUrl: string | null = null;
  selectedPhotoFile: File | null = null;

  constructor(
    private newsService: NewsService,
    private router: Router
  ) {
  }

  onCreate(form: NgForm) {
    this.submitted = true;
    this.errorMessage = '';
    this.photoErrorMessage = '';
    this.fieldErrors = {};

    if (!this.news.title?.trim()) {
      this.fieldErrors['title'] = 'Title is required!';
    }

    if (!this.news.summary?.trim()) {
      this.fieldErrors['summary'] = 'Short summary is required!';
    }

    if (this.selectedPhotoFile) {
      if (this.selectedPhotoFile.size > this.maxImageSizeBytes) {
        this.photoErrorMessage = 'Selected image is too large. Maximum file size is 1 MB.';
      } else if (!this.isAllowedImageType(this.selectedPhotoFile)) {
        this.photoErrorMessage = 'Only PNG, JPEG and WebP images are allowed.';
      }
    }

    if (form.invalid || Object.keys(this.fieldErrors).length > 0 || !!this.photoErrorMessage) {
      return;
    }

    const newsToCreate: News = {
      ...this.news,
      title: this.news.title.trim(),
      summary: this.news.summary.trim(),
      text: this.news.text?.trim() || undefined,
      publishedAt: this.publishedAt || undefined
    };

    this.newsService.createNews(newsToCreate, this.selectedPhotoFile).subscribe({
      next: () => {
        this.router.navigate(['/news'], {queryParams: {created: '1'}});
      },
      error: error => {
        this.errorMessage = this.extractErrorMessage(error);
      }
    });
  }

  onPhotoSelected(event: Event) {
    this.photoErrorMessage = '';
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    if (!file) {
      this.removeSelectedPhoto(input);
      return;
    }

    this.selectedPhotoName = file.name;
    this.selectedPhotoFile = file;
    this.photoPreviewUrl = this.isAllowedImageType(file) ? URL.createObjectURL(file) : null;
  }

  removeSelectedPhoto(fileInput: HTMLInputElement) {
    this.photoErrorMessage = '';
    this.selectedPhotoName = '';
    this.photoPreviewUrl = null;
    this.selectedPhotoFile = null;
    fileInput.value = '';
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
      return error.error.errors.join('').trim();
    }
    return error?.error?.detail ?? error?.error?.message ?? 'Could not create news';
  }

}

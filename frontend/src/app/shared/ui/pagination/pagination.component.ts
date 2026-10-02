import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.scss'],
  standalone: false
})
export class PaginationComponent {
  @Input() currentPage = 1;
  @Input() totalPages = 1;
  @Input() maxVisiblePages = 5;
  @Input() disabled = false;
  @Input() ariaLabel = 'Pagination';

  @Output() pageChange = new EventEmitter<number>();

  get normalizedCurrentPage(): number {
    return Math.min(Math.max(this.currentPage, 1), this.normalizedTotalPages);
  }

  get normalizedTotalPages(): number {
    return Math.max(this.totalPages, 1);
  }

  get pageItems(): Array<number | string> {
    const items: Array<number | string> = [1];
    const start = Math.max(2, this.normalizedCurrentPage - 2);
    const end = Math.min(this.normalizedTotalPages - 1, this.normalizedCurrentPage + 2);

    if (start > 2) {
      items.push('...');
    }

    for (let page = start; page <= end; page++) {
      items.push(page);
    }

    if (end < this.normalizedTotalPages - 1) {
      items.push('...');
    }

    if (this.normalizedTotalPages > 1) {
      items.push(this.normalizedTotalPages);
    }

    return items;
  }

  selectPage(page: number): void {
    if (this.disabled || page === this.normalizedCurrentPage || page < 1 || page > this.normalizedTotalPages) {
      return;
    }

    this.pageChange.emit(page);
  }
}

import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-search-filters',
  templateUrl: './search-filters.component.html',
  styleUrls: ['./search-filters.component.scss'],
  standalone: false
})
export class SearchFiltersComponent {
  @Input() placeholder = 'Search...';
  @Input() buttonLabel = 'Search';
  @Input() query = '';
  @Input() showFilters = true;
  @Output() queryChange = new EventEmitter<string>();
  @Output() search = new EventEmitter<string>();

  onQueryInput(value: string): void {
    this.query = value;
    this.queryChange.emit(value);
  }

  triggerSearch(): void {
    this.search.emit(this.query);
  }
}

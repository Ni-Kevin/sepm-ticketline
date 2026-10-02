import { Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-delete-dialog',
  templateUrl: './confirm-delete-dialog.component.html',
  standalone: false,
  styleUrls: ['./confirm-delete-dialog.component.scss']
})
export class ConfirmDeleteDialogComponent {
  @Input() title = 'Delete Item';
  @Input() deleteWhat = '?';
  @Input() message?: string;
  @Input() confirmButtonText = '?';
  @Input() showCancelButton = true;
  @Output() confirm = new EventEmitter<void>();

  @HostBinding('class') cssClass = 'modal fade';
  @HostBinding('attr.data-bs-backdrop') backdrop = 'static';
  @HostBinding('attr.aria-hidden') hidden = 'true';
  @HostBinding('attr.aria-labelledby') labeledBy = 'confirm-delete-dialog-title';

  constructor() {
  }

}

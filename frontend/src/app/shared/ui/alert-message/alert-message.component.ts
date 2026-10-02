import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-alert-message',
  templateUrl: './alert-message.component.html',
  styleUrls: ['./alert-message.component.scss'],
  standalone: false
})
export class AlertMessageComponent {
  @Input() type: 'danger' | 'warning' | 'success' | 'info' = 'danger';
}

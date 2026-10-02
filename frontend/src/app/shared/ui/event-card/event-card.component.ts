import {Component, Input} from '@angular/core';
import {Event} from '../../../dtos/event';

@Component({
  selector: 'app-event-card',
  templateUrl: './event-card.component.html',
  styleUrls: ['./event-card.component.scss'],
  standalone: false
})
export class EventCardComponent {
  @Input() compact: boolean = false;
  @Input() event?: Event;
  @Input() startTime?: string | Date;
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() showImage: boolean = true;
  @Input() image?: string;
}

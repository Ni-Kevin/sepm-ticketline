import {Component} from '@angular/core';
import {AuthService} from '../../services/auth.service';

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.scss'],
    standalone: false
})
export class HeaderComponent {

  constructor(public authService: AuthService) { }

  logout() {
    this.authService.logoutUser();
  }

}

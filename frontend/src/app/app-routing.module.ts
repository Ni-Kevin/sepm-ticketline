import {NgModule} from '@angular/core';
import {mapToCanActivate, RouterModule, Routes} from '@angular/router';
import {HomeComponent} from './components/home/home.component';
import {LoginComponent} from './components/login/login.component';
import {AuthGuard} from './guards/auth.guard';
import {ProfileComponent} from "./components/profile/profile.component";
import {EventsComponent} from './components/events/events.component';
import {EventCreateComponent} from './components/event-create/event-create.component';
import {CheckoutComponent, CheckoutMode} from './components/checkout/checkout.component';
import {AdminGuard} from './guards/admin.guard';
import {PerformanceCreateComponent} from './components/performance-create/performance-create.component';
import {ArtistCreateComponent} from './components/artist-create/artist-create.component';
import {RegisterCreateComponent, RegisterCreateMode} from "./components/register-create/register-create.component";
import {RoleGuard} from "./guards/role.guard";
import {UserListComponent} from "./components/user-list/user-list.component";
import {HallListComponent} from './components/hall-list/hall-list.component';
import {HallFormComponent} from './components/hall-form/hall-form.component';
import {HallLayoutEditorComponent} from './components/hall-layout-editor/hall-layout-editor.component';
import {VenueListComponent} from './components/venue-list/venue-list.component';
import {VenueFormComponent} from './components/venue-form/venue-form.component';
import {ForgotPasswordComponent} from './components/forgot-password/forgot-password.component';
import {ResetPasswordComponent} from './components/reset-password/reset-password.component';
import {EventPerformancesComponent} from './components/event-performances/event-performances.component';
import {SeatSelectionComponent} from "./components/seat-selection/seat-selection.component";
import {MyTicketsComponent} from "./components/my-tickets/my-tickets.component";
import {EventStatisticsComponent} from './components/event-statistics/event-statistics.component';
import {NewsFeedComponent} from "./components/news-feed/news-feed.component";
import {NewsCreateComponent} from './components/news-create/news-create.component';
import {NewsDetailComponent} from './components/news-detail/news-detail.component';
import {ArtistComponent} from './components/artist/artist.component';
import {PerformancesComponent} from './components/performances/performances.component';

const routes: Routes = [
  {path: '', component: HomeComponent},
  {path: 'artists', component: ArtistComponent},
  {path: 'events', component: EventsComponent},
  {path: 'performances', component: PerformancesComponent},
  {path: 'artists/:artistId/performances', component: PerformancesComponent},
  {path: 'statistics', component: EventStatisticsComponent},
  {path: 'events/:id/performances', component: EventPerformancesComponent},
  {path: 'admin/events/new', canActivate: mapToCanActivate([AdminGuard]), component: EventCreateComponent},
  {path: 'admin/news/new', canActivate: mapToCanActivate([AdminGuard]), component: NewsCreateComponent},
  {path: 'admin/performances/new', canActivate: mapToCanActivate([AdminGuard]), component: PerformanceCreateComponent},
  {path: 'admin/artists/new', canActivate: mapToCanActivate([AdminGuard]), component: ArtistCreateComponent},
  {path: 'login', component: LoginComponent},
  {path: 'forgot-password', component: ForgotPasswordComponent},
  {path: 'reset-password', component: ResetPasswordComponent},
  {path: 'profile', canActivate: mapToCanActivate([AuthGuard]), component: ProfileComponent},
  {path: 'checkout/purchase/:performanceId', canActivate: mapToCanActivate([AuthGuard]), component: CheckoutComponent, data: {mode: CheckoutMode.purchase}},
  {path: 'checkout/reservation/:performanceId', canActivate: mapToCanActivate([AuthGuard]), component: CheckoutComponent, data: {mode: CheckoutMode.reservation}},
  {path: 'checkout/purchase-from-reservation/:performanceId', canActivate: mapToCanActivate([AuthGuard]), component: CheckoutComponent, data: { mode: CheckoutMode.purchaseFromReservation}},
  {path: 'seat-selection/:performanceId', canActivate: mapToCanActivate([AuthGuard]), component: SeatSelectionComponent},
  {path: 'register', component: RegisterCreateComponent, data: {mode: RegisterCreateMode.register}},
  {path: 'admin/venues', canActivate: mapToCanActivate([AdminGuard]), component: VenueListComponent},
  {path: 'admin/venues/create', canActivate: mapToCanActivate([AdminGuard]), component: VenueFormComponent},
  {path: 'admin/venues/:id/edit', canActivate: mapToCanActivate([AdminGuard]), component: VenueFormComponent},
  {path: 'admin/halls', canActivate: mapToCanActivate([AdminGuard]), component: HallListComponent},
  {path: 'admin/halls/create', canActivate: mapToCanActivate([AdminGuard]), component: HallFormComponent},
  {path: 'admin/halls/:id/edit', canActivate: mapToCanActivate([AdminGuard]), component: HallFormComponent},
  {path: 'admin/halls/:id/layout', canActivate: mapToCanActivate([AdminGuard]), component: HallLayoutEditorComponent},
  {path: 'my-tickets', canActivate: mapToCanActivate([AuthGuard]), component: MyTicketsComponent},
  {path: 'news', canActivate: mapToCanActivate([AuthGuard]), component: NewsFeedComponent},
  {path: 'news/:id', canActivate: mapToCanActivate([AuthGuard]), component: NewsDetailComponent},
  {
    path: 'admin/create-user',
    canActivate: mapToCanActivate([AuthGuard, RoleGuard]),
    data: {mode: RegisterCreateMode.adminCreate, role: 'ADMIN' },
    component: RegisterCreateComponent
  },
  {
    path: 'admin/users',
    canActivate: mapToCanActivate([AuthGuard, RoleGuard]),
    data: {role: 'ADMIN' },
    component: UserListComponent
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true})],
  exports: [RouterModule]
})
export class AppRoutingModule {
}

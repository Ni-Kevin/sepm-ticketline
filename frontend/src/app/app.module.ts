import {BrowserModule} from '@angular/platform-browser';
import { CommonModule, DatePipe } from '@angular/common';
import {NgModule, provideZoneChangeDetection} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {HeaderComponent} from './components/header/header.component';
import {FooterComponent} from './components/footer/footer.component';
import {HomeComponent} from './components/home/home.component';
import {LoginComponent} from './components/login/login.component';
import {NewsComponent} from './components/news/news.component';
import {EventsComponent} from './components/events/events.component';
import {EventCreateComponent} from './components/event-create/event-create.component';
import {PerformanceCreateComponent} from './components/performance-create/performance-create.component';
import {ArtistCreateComponent} from './components/artist-create/artist-create.component';
import {CheckoutComponent} from './components/checkout/checkout.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {httpInterceptorProviders} from './interceptors';
import {ProfileComponent} from "./components/profile/profile.component";
import {RegisterCreateComponent} from "./components/register-create/register-create.component";
import {UserListComponent} from "./components/user-list/user-list.component";
import {VenueListComponent} from './components/venue-list/venue-list.component';
import {VenueFormComponent} from './components/venue-form/venue-form.component';
import {HallListComponent} from './components/hall-list/hall-list.component';
import {HallFormComponent} from './components/hall-form/hall-form.component';
import {HallLayoutEditorComponent} from './components/hall-layout-editor/hall-layout-editor.component';
import {SeatSelectionComponent} from './components/seat-selection/seat-selection.component';
import {HallAreaInspectorComponent} from './components/hall-area-inspector/hall-area-inspector.component';
import {SeatGridEditorComponent} from './components/seat-grid-editor/seat-grid-editor.component';
import {PageHeaderComponent} from './shared/ui/page-header/page-header.component';
import {AlertMessageComponent} from './shared/ui/alert-message/alert-message.component';
import {EmptyStateComponent} from './shared/ui/empty-state/empty-state.component';
import {SearchFiltersComponent} from './shared/ui/search-filters/search-filters.component';
import {ForgotPasswordComponent} from './components/forgot-password/forgot-password.component';
import {ResetPasswordComponent} from './components/reset-password/reset-password.component';
import {HallLayoutPreviewComponent} from './components/hall-layout-preview/hall-layout-preview.component';
import {MyTicketsComponent} from "./components/my-tickets/my-tickets.component";
import {ConfirmDeleteDialogComponent} from "./shared/ui/confirm-delete-dialog/confirm-delete-dialog.component";
import {EventCardComponent} from "./shared/ui/event-card/event-card.component";
import {EventPerformancesComponent} from './components/event-performances/event-performances.component';
import {EventStatisticsComponent} from './components/event-statistics/event-statistics.component';
import {NewsFeedComponent} from "./components/news-feed/news-feed.component";
import {NewsCardComponent} from "./shared/ui/news-card/news-card.component";
import {NewsCreateComponent} from './components/news-create/news-create.component';
import {NewsDetailComponent} from "./components/news-detail/news-detail.component";
import {ArtistComponent} from './components/artist/artist.component';
import {ArtistCardComponent} from './shared/ui/artist-card/artist-card.component';
import {PerformancesComponent} from './components/performances/performances.component';
import {PaginationComponent} from './shared/ui/pagination/pagination.component';

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    FooterComponent,
    HomeComponent,
    EventsComponent,
    EventCreateComponent,
    PerformanceCreateComponent,
    ArtistCreateComponent,
    LoginComponent,
    NewsComponent,
    VenueListComponent,
    VenueFormComponent,
    HallListComponent,
    HallFormComponent,
    HallLayoutEditorComponent,
    SeatSelectionComponent,
    HallAreaInspectorComponent,
    SeatGridEditorComponent,
    HallLayoutPreviewComponent,
    EventPerformancesComponent,
    EventStatisticsComponent,
    NewsCreateComponent,
    PageHeaderComponent,
    AlertMessageComponent,
    EmptyStateComponent,
    SearchFiltersComponent,
    ForgotPasswordComponent,
    ResetPasswordComponent,
    RegisterCreateComponent,
    UserListComponent,
    ProfileComponent,
    CheckoutComponent,
    MyTicketsComponent,
    CheckoutComponent,
    ProfileComponent,
    ConfirmDeleteDialogComponent,
    EventCardComponent,
    NewsFeedComponent,
    NewsCardComponent,
    NewsDetailComponent,
    ArtistComponent,
    ArtistCardComponent,
    PerformancesComponent,
    PaginationComponent
  ],
  bootstrap: [AppComponent],
  imports: [BrowserModule,
    CommonModule,
    AppRoutingModule,
    ReactiveFormsModule,
    NgbModule,
    FormsModule],
  providers: [
    httpInterceptorProviders,
    provideHttpClient(withInterceptorsFromDi()),
    provideZoneChangeDetection(),
    DatePipe,
  ] })
export class AppModule {
}

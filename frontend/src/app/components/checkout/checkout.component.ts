import {Component, OnInit, OnDestroy} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {BookingService} from '../../services/booking.service';
import {CheckoutSummarySeatholdDto, CheckoutSummaryTicketDto} from '../../dtos/checkout-summary.dto';
import {PurchaseRequestWithSeatholdIdsDto, PurchaseRequestWithTicketIdsDto} from '../../dtos/purchase-request.dto';
import {ReservationRequestDto} from '../../dtos/reservation-request.dto';

export enum CheckoutMode {
  purchase,
  reservation,
  purchaseFromReservation
}

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss'],
  standalone: false
})
export class CheckoutComponent implements OnInit, OnDestroy {

  summarySeatHold?: CheckoutSummarySeatholdDto;
  summaryTicketReservation?: CheckoutSummaryTicketDto;
  mode: CheckoutMode = CheckoutMode.purchase;
  ticketIdsFromReservation: number[] = [];
  selectedPaymentMethod = '';
  paymentDetails: { [key: string]: string } = {};
  loading = false;
  errorMessage = '';

  private hasPurchased = false;

  get activeSummary(): CheckoutSummarySeatholdDto | CheckoutSummaryTicketDto | undefined {
    if (this.mode === CheckoutMode.purchaseFromReservation) {
      return this.summaryTicketReservation;
    }
    return this.summarySeatHold;
  }

  paymentMethods = [
    { id: 'CREDIT_CARD', label: 'Credit Card' },
    { id: 'PAYPAL', label: 'PayPal' },
    { id: 'KLARNA', label: 'Klarna' }
  ];

  constructor(private route: ActivatedRoute,
              private router: Router,
              private bookingService: BookingService) {
  }

  ngOnInit() {
    this.mode = this.route.snapshot.data['mode'] || CheckoutMode.purchase;

    if (this.mode === CheckoutMode.purchaseFromReservation) {
      const idsString = this.route.snapshot.queryParamMap.get('ticketIds');
      if (idsString) {
        this.ticketIdsFromReservation = idsString.split(',').map(id => Number(id));
        this.loadSummaryFromTickets();
      }
    }
    else{
      this.loadSummaryFromSeatholds();
    }
  }

  ngOnDestroy() {
    this.executeCancel();
  }

  private loadSummaryFromSeatholds() {
    const performanceId = Number(this.route.snapshot.paramMap.get('performanceId'));
    if (isNaN(performanceId)) {
      this.errorMessage = 'Invalid performance ID';
      return;
    }

    this.loading = true;
    this.bookingService.getCheckoutSummaryFromSeathold(performanceId).subscribe({
      next: summary => {
        this.summarySeatHold = summary;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Could not load checkout summary';
        this.loading = false;
      }
    });
  }

  private loadSummaryFromTickets() {
    const performanceId = Number(this.route.snapshot.paramMap.get('performanceId'));
    if (isNaN(performanceId)) {
      this.errorMessage = 'Invalid performance ID';
      return;
    }

    this.loading = true;
    this.bookingService.getCheckoutSummaryFromTickets(performanceId, this.ticketIdsFromReservation).subscribe({
      next: summary => {
        this.summaryTicketReservation = summary;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Could not load checkout summary';
        this.loading = false;
      }
    });
  }

  isPaymentInfoValid(): boolean {
    if (!this.selectedPaymentMethod) return false;

    if (this.selectedPaymentMethod === 'CREDIT_CARD') {
      return !!(this.paymentDetails['cardNumber'] &&
        this.paymentDetails['expiry'] &&
        this.paymentDetails['cvv']);
    }

    if (this.selectedPaymentMethod === 'PAYPAL' || this.selectedPaymentMethod === 'KLARNA') {
      return !!this.paymentDetails['email'];
    }

    return false;
  }

  onPurchase() {
    if (!this.summarySeatHold) return;

    if (!this.isPaymentInfoValid()) {
      this.errorMessage = 'Please provide all required payment details';
      return;
    }

    const performanceId = Number(this.route.snapshot.paramMap.get('performanceId'));
    const holdIds = this.summarySeatHold.items.map(item => item.holdId);

    const request: PurchaseRequestWithSeatholdIdsDto = {
      performanceId,
      holdIds,
      paymentMethod: this.selectedPaymentMethod,
      paymentDetails: this.paymentDetails
    };

    this.loading = true;
    this.errorMessage = '';

    this.bookingService.purchaseTicketsWithSeatholdIds(request).subscribe({
      next: () => {
        this.hasPurchased = true;
        this.router.navigate(['/my-tickets']);
      },
      error: error => {
        this.errorMessage = typeof error?.error === 'string'
          ? error.error
          : (error?.error?.detail ?? error?.error?.message ?? 'Purchase failed');
        this.loading = false;
      }
    });
  }

  onPurchaseFromReservation() {
    if (!this.summaryTicketReservation) return;

    if (!this.isPaymentInfoValid()) {
      this.errorMessage = 'Please provide all required payment details';
      return;
    }

    const performanceId = Number(this.route.snapshot.paramMap.get('performanceId'));
    const ticketIds = this.summaryTicketReservation.items.map(item => item.ticketId);

    const request: PurchaseRequestWithTicketIdsDto = {
      performanceId,
      ticketIds,
      paymentMethod: this.selectedPaymentMethod,
      paymentDetails: this.paymentDetails
    };

    this.loading = true;
    this.errorMessage = '';

    this.bookingService.purchaseTicketsWithTicketIds(request).subscribe({
      next: () => {
        this.hasPurchased = true;
        this.router.navigate(['/my-tickets']);
      },
      error: error => {
        this.errorMessage = typeof error?.error === 'string'
          ? error.error
          : (error?.error?.detail ?? error?.error?.message ?? 'Purchase failed');
        this.loading = false;
      }
    });
  }

  onReserve() {
    if (!this.summarySeatHold) return;

    const performanceId = Number(this.route.snapshot.paramMap.get('performanceId'));
    const holdIds = this.summarySeatHold.items.map(item => item.holdId);

    const request: ReservationRequestDto = {
      performanceId,
      holdIds
    };

    this.loading = true;
    this.errorMessage = '';

    this.bookingService.reserveTickets(request).subscribe({
      next: () => {
        this.hasPurchased = true;
        this.router.navigate(['/my-tickets']);
      },
      error: error => {
        this.errorMessage = typeof error?.error === 'string'
          ? error.error
          : (error?.error?.detail ?? error?.error?.message ?? 'Reservation failed');
        this.loading = false;
      }
    });
  }

  onCancel() {
    if(this.mode === CheckoutMode.purchaseFromReservation){
      this.router.navigate([`/my-tickets`]);
      return;
    }
    const performanceId = Number(this.route.snapshot.paramMap.get('performanceId'));
    if (isNaN(performanceId)) return;

    this.loading = true;
    this.errorMessage = '';

    this.bookingService.cancelCheckout(performanceId).subscribe({
      next: () => {
        this.hasPurchased = true;
        this.router.navigate([`/seat-selection`, performanceId]);
      },
      error: error => {
        this.errorMessage = typeof error?.error === 'string'
          ? error.error
          : (error?.error?.detail ?? error?.error?.message ?? 'Cancellation failed');
        this.loading = false;
      }
    });
  }


  private executeCancel() {
    if (this.hasPurchased) return;
    const performanceId = Number(this.route.snapshot.paramMap.get('performanceId'));
    if (isNaN(performanceId)) return;
    this.bookingService.cancelCheckout(performanceId).subscribe();
  }

  protected readonly CheckoutMode = CheckoutMode;
}

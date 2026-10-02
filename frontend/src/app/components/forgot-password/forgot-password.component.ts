import {Component} from '@angular/core';
import {UntypedFormBuilder, UntypedFormGroup, Validators} from '@angular/forms';
import {UserService} from '../../services/user.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss'],
  standalone: false
})
export class ForgotPasswordComponent {
  requestForm: UntypedFormGroup;
  submitted = false;
  successMessage = '';
  errorMessage = '';
  fieldErrors: { [key: string]: string } = {};

  constructor(private formBuilder: UntypedFormBuilder, private userService: UserService) {
    this.requestForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  submit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.fieldErrors = {};

    if (this.requestForm.controls.email.errors?.required) {
      this.fieldErrors['email'] = 'Email is required!';
    } else if (this.requestForm.controls.email.errors?.email) {
      this.fieldErrors['email'] = 'Please enter a valid email address!';
    }

    if (this.requestForm.invalid) {
      return;
    }

    this.userService.requestPasswordReset({email: this.requestForm.value.email}).subscribe({
      next: () => {
        this.successMessage = 'If an account with this email exists, a reset link has been sent.';
      },
      error: error => {
        this.errorMessage = typeof error.error === 'string' ? error.error : 'The reset request could not be processed.';
      }
    });
  }
}

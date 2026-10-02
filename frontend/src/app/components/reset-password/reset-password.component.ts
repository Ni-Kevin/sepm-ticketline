import {Component, OnInit} from '@angular/core';
import {UntypedFormBuilder, UntypedFormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {UserService} from '../../services/user.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss'],
  standalone: false
})
export class ResetPasswordComponent implements OnInit {
  resetForm: UntypedFormGroup;
  submitted = false;
  token = '';
  successMessage = '';
  errorMessages: string[] = [];
  fieldErrors: { [key: string]: string } = {};

  constructor(
    private formBuilder: UntypedFormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService
  ) {
    this.resetForm = this.formBuilder.group({
      password: ['', [Validators.required, Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).*$/)]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.errorMessages = ['The password reset link is invalid.'];
    }
  }

  submit(): void {
    this.submitted = true;
    this.fieldErrors = {};
    this.errorMessages = [];


    if (!this.token) {
      this.errorMessages = ['The password reset link is invalid.'];
      return;
    }


    if (this.resetForm.controls.password.errors?.required) {
      this.fieldErrors['password'] = 'Password is required!';
    } else if (this.resetForm.controls.password.errors?.minlength || this.resetForm.controls.password.errors?.pattern) {
      this.fieldErrors['password'] = 'Password must contain at least: 8 Characters, 1 Uppercase, 1 Lowercase, 1 Number, 1 Special Character!!';
    }

    if (this.resetForm.controls.confirmPassword.errors?.required) {
      this.fieldErrors['confirmPassword'] = 'Please confirm your password!';
    }

    if (this.resetForm.valid && this.resetForm.value.password !== this.resetForm.value.confirmPassword) {
      this.fieldErrors['confirmPassword'] = 'Passwords do not match!';
      return;
    }

    if (this.resetForm.invalid || Object.keys(this.fieldErrors).length > 0) {
      return;
    }


    this.userService.confirmPasswordReset({
      token: this.token,
      password: this.resetForm.value.password
    }).subscribe({
      next: () => {
        this.successMessage = 'Your password has been reset. You can now log in.';
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: error => {
        if (Array.isArray(error.error?.errors)) {
          this.errorMessages = error.error.errors;
        } else if (typeof error.error === 'string') {
          this.errorMessages = [error.error];
        } else {
          this.errorMessages = ['The password could not be reset.'];
        }
      }
    });
  }
}

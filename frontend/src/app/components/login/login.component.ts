import {Component, OnInit} from '@angular/core';
import {UntypedFormBuilder, UntypedFormGroup, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {AuthService} from '../../services/auth.service';
import {AuthRequest} from '../../dtos/auth-request';


@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
    standalone: false
})
export class LoginComponent implements OnInit {

  loginForm: UntypedFormGroup;
  // After first submission attempt, form validation will start
  submitted = false;
  // Error flag
  error = false;
  errorMessage = '';
  fieldErrors: { [key: string]: string } = {};

  constructor(private formBuilder: UntypedFormBuilder, private authService: AuthService, private router: Router) {
    this.loginForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  /**
   * Form validation will start after the method is called, additionally an AuthRequest will be sent
   */
  loginUser() {
    this.submitted = true;
    this.fieldErrors = {};

    if (this.loginForm.controls.username.errors?.required) {
      this.fieldErrors['username'] = 'Email is required!';
    } else if (this.loginForm.controls.username.errors?.email) {
      this.fieldErrors['username'] = 'Please enter a valid email address!';
    }

    if (this.loginForm.controls.password.errors?.required) {
      this.fieldErrors['password'] = 'Password is required!';
    } else if (this.loginForm.controls.password.errors?.minlength) {
      this.fieldErrors['password'] = 'Password must be at least 8 characters!';
    }

    if (this.loginForm.valid) {
      const authRequest: AuthRequest = new AuthRequest(
        this.loginForm.controls.username.value,
        this.loginForm.controls.password.value
      );
      this.authenticateUser(authRequest);
    }
  }

  /**
   * Send authentication data to the authService. If authentication succeeds, the user is forwarded to the home page
   *
   * @param authRequest authentication data from the user login form
   */
  authenticateUser(authRequest: AuthRequest) {
    this.authService.loginUser(authRequest).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: error => {
        this.error = true;
        if (typeof error.error === 'object') {
          this.errorMessage = error.error.errors;
        } else {
          this.errorMessage = error.error;
        }
      }
    });
  }

  /**
   * Error flag will be deactivated, which clears the error message
   */
  vanishError() {
    this.error = false;
  }

  ngOnInit() {
  }

}

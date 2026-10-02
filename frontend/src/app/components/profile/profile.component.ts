import {Component, OnInit} from '@angular/core';
import {AbstractControl, UntypedFormBuilder, UntypedFormGroup, Validators, ValidationErrors} from "@angular/forms";
import {UserService} from "../../services/user.service";
import {AuthService} from "../../services/auth.service";
import {Router} from "@angular/router";
import {UserUpdateDto} from "../../dtos/user";

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
  standalone: false
})
export class ProfileComponent implements OnInit {

  profileForm: UntypedFormGroup;
  submitted = false;
  error = false;
  errorMessage = '';
  success = false;
  isEditMode = false;
  fieldErrors: { [key: string]: string } = {};
  showDeleteDialog = false;
  selectedAction = '';
  errorTextString = '';

  constructor(
    private formBuilder: UntypedFormBuilder,
    private userService: UserService,
    private authService: AuthService,
    private router: Router
  ) {
    this.profileForm = this.formBuilder.group({
      firstName: ['', [Validators.required, Validators.pattern(/^[a-zA-ZäöüÄÖÜß\s-]+$/)]],
      lastName: ['', [Validators.required, Validators.pattern(/^[a-zA-ZäöüÄÖÜß\s-]+$/)]],
      email: ['', [Validators.required, Validators.email]],
      oldPassword: [''],
      password: [''],
      passwordConfirmation: ['']
    }, {validators: this.passwordMatchValidator});
    const passwordControl = this.profileForm.get('password');

    if (passwordControl) {
      passwordControl.valueChanges.subscribe(val => {
        if (val && val.length > 0) {
          passwordControl.setValidators([
            Validators.minLength(8),
            Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).*$/)
          ]);
        } else {
          passwordControl.clearValidators();
        }
        passwordControl.updateValueAndValidity({emitEvent: false});
      });
    }
  }


  ngOnInit() {
    this.loadUserData();
  }

  private loadUserData() {
    this.userService.getCurrentUser().subscribe({
      next: (user: UserUpdateDto) => {
        this.profileForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
          oldPassword: '',
          password: '',
          passwordConfirmation: ''
        });
        this.profileForm.disable();
      },
      error: err => {
        this.error = true;
        this.errorMessage = 'Profile could not load';
        console.error('Error loading profile', err);
      }
    });
  }

  toggleEditMode() {
    this.isEditMode = !this.isEditMode;
    this.submitted = false;

    if (this.isEditMode) {
      this.profileForm.enable();
    } else {
      this.profileForm.disable();
      this.loadUserData();
    }
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmation = control.get('passwordConfirmation')?.value;

    if (!password) {
      return null;
    }

    return password === confirmation ? null : {passwordMismatch: true};
  }

  updateUser() {
    this.submitted = true;
    this.fieldErrors = {};
    this.error = false;

    if (this.profileForm.controls.firstName.errors?.required) {
      this.fieldErrors['firstName'] = 'First name is required!';
    }
    else if (this.profileForm.controls.firstName.hasError('pattern')) {
  this.fieldErrors['firstName'] = 'First name must not contain numbers or special characters!';
}

    if (this.profileForm.controls.lastName.errors?.required) {
      this.fieldErrors['lastName'] = 'Last name is required!';
    }
    else if (this.profileForm.controls.lastName.hasError('pattern')) {
      this.fieldErrors['lastName'] = 'Last name must not contain numbers or special characters!';
    }

    if (this.profileForm.controls.email.errors?.required) {
      this.fieldErrors['email'] = 'Email is required!';
    } else if (this.profileForm.controls.email.errors?.email) {
      this.fieldErrors['email'] = 'Please enter a valid email address!';
    }

    if (this.profileForm.controls.password.value && this.profileForm.controls.password.errors?.minlength
      && this.profileForm.controls.password.errors?.pattern) {
      this.fieldErrors['password'] = 'Password must contain at least: 8 Characters, 1 Uppercase, 1 Lowercase, 1 Number, 1 Special Character!';
    }

    if (this.profileForm.hasError('passwordMismatch') && this.profileForm.controls.password.value) {
      this.fieldErrors['passwordConfirmation'] = 'Passwords do not match!';
    }


    if (Object.keys(this.fieldErrors).length > 0) {
      return;
    }

    if (this.profileForm.valid) {
      const formValues = this.profileForm.getRawValue();
      const userUpdate: any = {
        firstName: formValues.firstName,
        lastName: formValues.lastName,
        email: formValues.email,
        oldPassword: formValues.oldPassword,
        password: formValues.password,
        passwordConfirmation: formValues.passwordConfirmation
      };

      if (!userUpdate.password || userUpdate.password.trim() === '') {
        delete userUpdate.password;
        delete userUpdate.oldPassword;
        delete userUpdate.passwordConfirmation;

      }
      this.sendUpdate(userUpdate);
    }
  }

  private sendUpdate(userUpdate: UserUpdateDto) {
    this.userService.update(userUpdate).subscribe({
      next: (newToken: string) => {
        this.success = true;
        this.error = false;

        if (newToken) {
          this.authService.updateToken(newToken);
        }
        this.isEditMode = false;
        this.profileForm.disable();
        this.submitted = false;

        this.loadUserData();

        setTimeout(() => {
          this.success = false;
        }, 3000);


      },
      error: error => {
        this.error = true;
        this.success = false;

        let errorObj = error.error;
        if (typeof errorObj === 'string') {
          try {
            errorObj = JSON.parse(errorObj);
          } catch (e) {
            this.errorMessage = errorObj;
            return;
          }
        }
        if (errorObj?.errors && Array.isArray(errorObj.errors)) {
          const errorText = errorObj.errors.join(' ');

          if (errorText.toLowerCase().includes('password')) {
            this.errorMessage = errorText;
          } else {
            this.errorMessage = errorText;
          }
        } else if (typeof error.error === 'string') {
          this.errorMessage = error.error;
        } else {
          this.errorMessage = 'An error occurred. Please try again.';
        }
      }
    });
  }

  deleteAccount() {
    this.selectedAction = 'DELETE';
    this.showDeleteDialog = true;
  }

  onConfirmDelete() {
    if (this.selectedAction === 'DELETE') {
      this.userService.delete().subscribe({
        next: () => {
          this.authService.logoutUser();
          this.router.navigate(['/']);
        },
        error: error => {
          this.error = true;
          this.errorMessage = 'Could not delete account. Please try again.';
          this.showDeleteDialog = false;
        }
      });
    }
  }

  onCancelDelete() {
    this.showDeleteDialog = false;
  }

  vanishError() {
    this.error = false;
  }

}

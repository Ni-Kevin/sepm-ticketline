import {Component, OnInit} from '@angular/core';
import {UntypedFormBuilder, UntypedFormGroup, Validators} from "@angular/forms";
import {ActivatedRoute, Router} from "@angular/router";
import {UserService} from "../../services/user.service";


export enum RegisterCreateMode {
  register,
  adminCreate
}

@Component({
  selector: 'app-register',
  templateUrl: './register-create.component.html',
  styleUrl: './register-create.component.scss',
  standalone: false
})
export class RegisterCreateComponent implements OnInit {

  mode: RegisterCreateMode = RegisterCreateMode.register;
  registerForm: UntypedFormGroup;
  submitted = false;
  error = false;
  errorMessage = '';
  fieldErrors: { [key: string]: string } = {};

  constructor(
    private formBuilder: UntypedFormBuilder,
    private router: Router,
    private userService: UserService,
    private activatedRoute: ActivatedRoute
  ) {
    this.registerForm = this.formBuilder.group({
      email: ['', [Validators.required]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
    });
  }

  public get heading(): string {
    switch (this.mode) {
      case RegisterCreateMode.register:
        return 'Register';
      case RegisterCreateMode.adminCreate:
        return 'Create User';
      default:
        return '';
    }
  }

  get modeIsRegister(): boolean {
    return this.mode === RegisterCreateMode.register;
  }

  get modeIsAdminCreate(): boolean {
    return this.mode === RegisterCreateMode.adminCreate;
  }

  private get modeActionFinished(): string {
    switch (this.mode) {
      case RegisterCreateMode.register:
        return 'registered';
      case RegisterCreateMode.adminCreate:
        return 'created';
      default:
        return '';
    }
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(data => {
      this.mode = data['mode'] ?? RegisterCreateMode.register;
      this.buildForm();
    });
  }

  private buildForm(): void {
    if (this.modeIsRegister) {
      this.registerForm.addControl('password',
        this.formBuilder.control('', [Validators.required])
      );
    } else if (this.modeIsAdminCreate) {
      this.registerForm.addControl('role',
        this.formBuilder.control('', [Validators.required])
      );
    }
  }

  public onSubmit() {
    this.submitted = true;
    this.fieldErrors = {};
    this.error = false;

    if (this.registerForm.controls.firstName.errors?.required) {
      this.fieldErrors['firstName'] = 'First name is required!';
    }
    if (this.registerForm.controls.lastName.errors?.required) {
      this.fieldErrors['lastName'] = 'Last name is required!';
    }
    if (this.registerForm.controls.email.errors?.required) {
      this.fieldErrors['email'] = 'Email is required!';
    }
    if (this.modeIsRegister) {
      if (this.registerForm.controls.password.errors?.required) {
        this.fieldErrors['password'] = 'Password is required!';
      }
    }
    if (this.modeIsAdminCreate) {
      if (this.registerForm.controls.role.errors?.required) {
        this.fieldErrors['role'] = 'Role is required!';
      }
    }

    if (this.registerForm.valid && Object.keys(this.fieldErrors).length === 0) {
      const registerData = this.registerForm.value;
      this.registerUser(registerData);
    }
  }

  private registerUser(registerData: any) {
    const request$ = this.modeIsAdminCreate
      ? this.userService.createUser(registerData)
      : this.userService.register(registerData);

    request$.subscribe({
      next: () => {
        const redirectPath = this.modeIsAdminCreate ? '/admin/users' : '/login';
        this.router.navigate([redirectPath])
      },
      error: error => {
        this.submitted = false;

        if (error.error && error.error.errors) {
          const nonFieldErrors: string[] = [];
          for (const msg of error.error.errors) {
            const cleanMsg = msg.trim();
            const colonIdx = cleanMsg.indexOf(':');
            if (colonIdx > 0) {
              const field = cleanMsg.substring(0, colonIdx).trim();
              const errMsg = cleanMsg.substring(colonIdx + 1).trim();
              if (this.fieldErrors[field]) {
                this.fieldErrors[field] += '\n' + errMsg;
              } else {
                this.fieldErrors[field] = errMsg;
              }
            } else {
              nonFieldErrors.push(cleanMsg);
            }
          }
          if (nonFieldErrors.length > 0) {
            this.error = true;
            this.errorMessage = nonFieldErrors.join(', ');
          } else if (Object.keys(this.fieldErrors).length === 0) {
            this.error = true;
            this.errorMessage = this.modeIsRegister ? 'Could not register user.' : 'Could not create user.';
          }
        } else if (error.error && error.error.message) {
          this.error = true;
          this.errorMessage = error.error.message;
        } else {
          this.error = true;
          this.errorMessage = this.modeIsRegister ? 'Could not register user.' : 'Could not create user.';
        }
      }
    });
  }
}

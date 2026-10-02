import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import { RegisterCreateComponent } from './register-create.component';
import {ReactiveFormsModule} from "@angular/forms";
import {RouterTestingModule} from "@angular/router/testing";
import {provideHttpClient, withInterceptorsFromDi} from "@angular/common/http";
import {provideHttpClientTesting} from "@angular/common/http/testing";
import {UserService} from "../../services/user.service";
import {Router} from "@angular/router";
import {of} from "rxjs";

describe('RegisterCreateComponent', () => {
  let component: RegisterCreateComponent;
  let fixture: ComponentFixture<RegisterCreateComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [RegisterCreateComponent],
      imports: [
        RouterTestingModule,
        ReactiveFormsModule
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RegisterCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('email field validity', () => {
    let email = component.registerForm.controls['email'];
    expect(email.valid).toBeFalsy();

    email.setValue("test");
    expect(email.hasError('email')).toBeTruthy();

    email.setValue("test@example.com");
    expect(email.valid).toBeTruthy();
  });

  it('email field validity', () => {
    let email = component.registerForm.controls['email'];
    expect(email.valid).toBeFalsy();

    email.setValue("test");
    expect(email.hasError('email')).toBeTruthy();

    email.setValue("test@example.com");
    expect(email.valid).toBeTruthy();
  });

  it('password field validity', () => {
    let password = component.registerForm.controls['password'];
    expect(password.valid).toBeFalsy();

    password.setValue("1234567");
    expect(password.hasError('minlength')).toBeTruthy();

    password.setValue("Passwort1!");
    expect(password.valid).toBeTruthy();
  });

  it('should call userService when register is executed', () => {
    const userService = TestBed.inject(UserService);
    const serviceSpy = spyOn(userService, 'register').and.callThrough();

    component.registerForm.controls['firstName'].setValue("TestUser");
    component.registerForm.controls['lastName'].setValue("TestUser");
    component.registerForm.controls['email'].setValue("test@example.com");
    component.registerForm.controls['password'].setValue("Passwort123!");
    fixture.detectChanges();
    component.onSubmit();
    expect(serviceSpy).toHaveBeenCalled();
  });

  it('should navigate to login after successful registration', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate');
    const userService = TestBed.inject(UserService);

    spyOn(userService, 'register').and.returnValue(of(undefined));

    component.registerForm.controls['firstName'].setValue("TestUser");
    component.registerForm.controls['lastName'].setValue("TestUser");
    component.registerForm.controls['email'].setValue("test@example.com");
    component.registerForm.controls['password'].setValue("Passwort123!");

    component.onSubmit();

    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
});

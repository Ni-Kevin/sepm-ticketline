import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Globals} from "../global/globals";
import {Observable} from "rxjs";
import {PasswordResetConfirmDto, PasswordResetRequestDto, UserDto, UserRegisterDto, UserUpdateDto} from "../dtos/user";

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private userBaseUri: string = this.globals.backendUri + '/users';

  constructor(
    private httpClient: HttpClient,
    private globals: Globals
  ) { }

  register(user: UserRegisterDto): Observable<void> {
    const registerUri = this.userBaseUri + '/register'
    return this.httpClient.post<void>(registerUri, user);
  }

  update(user: UserUpdateDto): Observable<string> {
    return this.httpClient.put(`${this.userBaseUri}/me`, user, { responseType: 'text' });
  }

  delete(): Observable<void> {
    return this.httpClient.delete<void>(`${this.userBaseUri}/me`);
  }

  getCurrentUser(): Observable<UserUpdateDto> {
    return this.httpClient.get<UserUpdateDto>(`${this.userBaseUri}/me`);
  }
  createUser(userData: any): Observable<void> {
    return this.httpClient.post<void>(this.globals.backendUri + '/users', userData);
  }

  getProfile(): Observable<UserDto> {
    return this.httpClient.get<UserDto>(this.userBaseUri + '/account');
  }

  getAllUsers(): Observable<UserDto[]> {
    return this.httpClient.get<UserDto[]>(this.userBaseUri);
  }

  setLockedStatus(id: number, locked: boolean): Observable<void> {
    return this.httpClient.patch<void>(`${this.userBaseUri}/${id}/locked`, locked);
  }

  requestPasswordReset(request: PasswordResetRequestDto): Observable<void> {
    return this.httpClient.post<void>(`${this.userBaseUri}/password-reset/request`, request);
  }

  confirmPasswordReset(request: PasswordResetConfirmDto): Observable<void> {
    return this.httpClient.post<void>(`${this.userBaseUri}/password-reset/confirm`, request);
  }

}

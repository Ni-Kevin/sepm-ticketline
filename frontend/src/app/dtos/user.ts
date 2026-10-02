export interface UserRegisterDto {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface UserUpdateDto {
  email: string;
  firstName: string;
  lastName: string;
  password?: string;
  oldPassword?: string;
  passwordConfirmation?: string;
}
export interface UserCreateDto {
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  locked: boolean;
}

export interface UserDto {
  id: number,
  email: string,
  firstName: string,
  lastName: string,
  role: string,
  locked: boolean
}

export interface PasswordResetRequestDto {
  email: string;
}

export interface PasswordResetConfirmDto {
  token: string;
  password: string;
}

import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { UserDto } from '../../dtos/user';
import {AuthService} from "../../services/auth.service";
import {ConfirmDeleteDialogComponent} from "../../shared/ui/confirm-delete-dialog/confirm-delete-dialog.component";

@Component({
  selector: 'app-admin-user-list',
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
  standalone: false
})
export class UserListComponent implements OnInit {

  users: UserDto[] = [];
  loading = true;
  error = false;
  errorMessage = '';
  currentUserEmail: String | null = null;
  selectedUser: UserDto | null = null;
  confirmAction: string = '';
  successMessage = '';
  currentPage = 1;
  pageSize = 15;

  constructor(private userService: UserService,
              private authService: AuthService) {}

  ngOnInit(): void {
    this.currentUserEmail = this.authService.getCurrentUserEmail();
    this.loadUsers();
  }

  private loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.currentPage = 1;
        this.loading = false;
      },
      error: (error) => {
        console.error('Failed to load users', error);
        this.error = true;
        this.errorMessage = 'Failed to load users';
        this.loading = false;
      }
    });
  }

  get totalPages(): number {
    return Math.ceil(this.users.length / this.pageSize);
  }

  get paginatedUsers(): UserDto[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    return this.users.slice(startIndex, startIndex + this.pageSize);
  }

  onPageChanged(page: number): void {
    this.currentPage = page;
  }

  toggleLock(user: UserDto): void {
    const action = user.locked ? 'unlock' : 'lock';
    this.selectedUser = user;
    this.confirmAction = action.toUpperCase();
  }

  onConfirmToggleLock(): void {
    if (this.selectedUser) {
      this.userService.setLockedStatus(this.selectedUser.id, !this.selectedUser.locked).subscribe({
        next: () => {
          this.selectedUser!.locked = !this.selectedUser!.locked;
          this.loadUsers();
        },
        error: (error) => {
          let message = 'Failed to update user status';
          if (error.error?.errors?.length > 0) {
            message = error.error.errors[0];
          }
        }
      });
    }
  }

  getStatus(user: UserDto): string {
    return user.locked ? 'Locked' : 'Unlocked';
  }

  getRole(role: string): string {
    const roleMap: Record<string, string> = {
      'ROLE_ADMIN': 'Admin',
      'ROLE_USER': 'User'
    };
    return roleMap[role] ?? role;
  }

  sendNewPassword(user: UserDto): void {
    this.userService.requestPasswordReset({ email: user.email }).subscribe({
      next: () => {
        this.successMessage = `Password reset email sent to ${user.email}.`;
        this.errorMessage = '';
        this.error = false;
        setTimeout(() => {
          this.successMessage = '';
        }, 3000);
      },
      error: (error) => {
        this.errorMessage = typeof error.error === 'string' ? error.error : 'Could not send password reset email.';
        this.successMessage = '';
        this.error = true;
      }
    });
  }
}

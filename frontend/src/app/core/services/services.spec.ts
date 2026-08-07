import { TestBed } from '@angular/core/testing';
import { AuthService } from './services';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthService);
  });

  it('starts without a logged-in user', () => {
    expect(service.user()).toBeNull();
    expect(service.hasRole('ADMIN')).toBe(false);
  });

  it('logs in and stores the user', () => {
    service.login('admin@sgs.local', 'ADMIN');

    expect(service.user()?.role).toBe('ADMIN');
    expect(service.user()?.nom).toBe('Ravel Kamga');
    expect(service.user()?.login).toBe('admin@sgs.local');
    expect(service.hasRole('ADMIN')).toBe(true);
    expect(service.hasRole('VENDEUR')).toBe(false);
  });

  it('checks any-of roles', () => {
    service.login('x', 'VENDEUR');
    expect(service.hasRole('ADMIN', 'VENDEUR')).toBe(true);
    expect(service.hasRole('ADMIN', 'GESTIONNAIRE')).toBe(false);
  });

  it('persists the session in localStorage and restores it', () => {
    service.login('x', 'GESTIONNAIRE');

    // Un nouveau service (rechargement de page) retrouve l'utilisateur.
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const reloaded = TestBed.inject(AuthService);

    expect(reloaded.user()?.role).toBe('GESTIONNAIRE');
  });

  it('logs out and clears the session', () => {
    service.login('x', 'ADMIN');
    service.logout();

    expect(service.user()).toBeNull();
    expect(localStorage.getItem('sgs.currentUser')).toBeNull();
  });
});

import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('cree la coquille de l application', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('affiche le menu principal', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const liens = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('nav a')).map((a) => a.textContent?.trim());
    expect(liens.join(' ')).toContain('Véhicules');
    expect(liens.join(' ')).toContain('Locations');
  });
});

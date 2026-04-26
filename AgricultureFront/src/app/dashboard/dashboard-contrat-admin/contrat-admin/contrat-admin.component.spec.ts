import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContratAdminComponent } from './contrat-admin.component';

describe('ContratAdminComponent', () => {
  let component: ContratAdminComponent;
  let fixture: ComponentFixture<ContratAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContratAdminComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ContratAdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

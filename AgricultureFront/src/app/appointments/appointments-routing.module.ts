import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AppointmentsLayoutComponent } from './layout/appointments-layout.component';
import { PaymentReturnComponent }      from './farmer/payment-return/payment-return.component';
import { AuthGuard } from '../services/auth/auth.guard';

const routes: Routes = [
  {
    path: '',
    component: AppointmentsLayoutComponent,
    canActivate: [AuthGuard],
    data: { roles: ['AGRICULTEUR', 'VETERINAIRE'] }
  },
  {
    // Page de retour Stripe — pas de layout wrapper, juste le composant
    path: 'payment-return',
    component: PaymentReturnComponent,
    canActivate: [AuthGuard]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AppointmentsRoutingModule {}
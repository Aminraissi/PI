import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { AdminClaimsComponent } from './admin/admin-claims/admin-claims.component';
import { ClaimDetailComponent } from './user/claim-detail/claim-detail.component';
import { ClaimsSharedModule } from './claims-shared.module';

const routes: Routes = [
  { path: '',    component: AdminClaimsComponent },
  { path: ':id', component: ClaimDetailComponent },
];

@NgModule({
  declarations: [
    AdminClaimsComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ClaimsSharedModule,
    RouterModule.forChild(routes),
  ]
})
export class ClaimsAdminModule {}

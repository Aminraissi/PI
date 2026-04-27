import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ClaimsSharedModule } from './claims-shared.module';
import { ClaimDetailComponent } from './user/claim-detail/claim-detail.component';

const routes: Routes = [
  { path: '', component: ClaimDetailComponent }
];

@NgModule({
  imports: [
    ClaimsSharedModule,
    RouterModule.forChild(routes)
  ]
})
export class ClaimDetailRouteModule {}

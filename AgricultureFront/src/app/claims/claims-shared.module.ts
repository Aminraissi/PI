import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ClaimDetailComponent } from './user/claim-detail/claim-detail.component';

@NgModule({
  declarations: [
    ClaimDetailComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
  ],
  exports: [
    ClaimDetailComponent
  ]
})
export class ClaimsSharedModule {}

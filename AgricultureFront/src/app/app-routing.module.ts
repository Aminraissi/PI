import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent }       from './components/home/home.component';
import { BlogDetailComponent } from './components/blog-detail/blog-detail.component';
import { NotFoundComponent }   from './components/not-found/not-found.component';
import { AuthComponent }       from './components/auth/auth.component';
import { AuthGuard }           from './services/auth/auth.guard';
import { GuestGuard }          from './services/auth/guest.guard';
import { RegisterExtraComponent }            from './components/register-extra/register-extra.component';
import { RoleHomePlaceholderComponent }      from './components/role-home-placeholder/role-home-placeholder.component';
import { ExplorerHostComponent }             from './components/explorer-host/explorer-host.component';
import { DiseasePredictorComponent }         from './components/disease-predictor/disease-predictor.component';
import { HelpRequestComponent }              from './components/help-request/help-request.component';
import { ExpertAssistanceRequestsComponent } from './components/expert-assistance-requests/expert-assistance-requests.component';
import { VerifyEmailComponent } from './components/auth/verify-email/verify-email.component';
import { ProfileEditComponent } from './components/profile-edit/profile-edit.component';


const routes: Routes = [
  { path: '',         component: HomeComponent,  pathMatch: 'full' },
  { path: 'explorer', component: ExplorerHostComponent },
  { path: 'auth',     component: AuthComponent,  canActivate: [GuestGuard] },

  {
    path: 'forums',
    loadChildren: () => import('./forums/forums.module').then(m => m.ForumsModule)
  },
  {
    path: 'delivery',
    loadChildren: () => import('./delivery/delivery.module').then(m => m.DeliveryModule),
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN','TRANSPORTEUR','AGRICULTEUR'] }
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./dashboard/dashboard.module').then(m => m.DashboardModule),
    canActivate: [AuthGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'marketplace',
    loadChildren: () => import('./marketplace/marketplace.module').then(m => m.MarketplaceModule),
    canActivate: [AuthGuard],

  },
  {
    path: 'loans',
    loadChildren: () => import('./loans/loans.module').then(m => m.LoansModule),
    canActivate: [AuthGuard],
    data: { roles: ['AGENT', 'AGRICULTEUR'] }
  },
  {
    path: 'claims',
    loadChildren: () => import('./claims/claims.module').then(m => m.ClaimsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'events',
    loadChildren: () => import('./events/events.module').then(m => m.EventsModule),
    data: { roles: ['ORGANISATEUR_EVENEMENT','EXPERT_AGRICOLE','AGRICULTEUR'] }
  },
  {
    path: 'training',
    loadChildren: () => import('./training/training.module').then(m => m.TrainingModule),
    canActivate: [AuthGuard],
    data: { roles: ['EXPERT_AGRICOLE', 'AGRICULTEUR'] }
  },
  {
    path: 'farm',
    loadChildren: () => import('./features/farm/farm.module').then(m => m.FarmModule),
    canActivate: [AuthGuard],
    data: { roles: ['AGRICULTEUR'] }


  },
  {
    path: 'inventory',
    loadChildren: () => import('./inventory/inventory.module').then(m => m.InventoryModule),
    canActivate: [AuthGuard],
    data: { roles: ['VETERINAIRE', 'AGRICULTEUR'] }


  },
  {
    path: 'appointments',
    loadChildren: () => import('./appointments/appointments.module').then(m => m.AppointmentsModule),
    canActivate: [AuthGuard],
    data: { roles: ['VETERINAIRE', 'AGRICULTEUR'] }

  },
  {
    path: 'animals',
    loadChildren: () => import('./animals/animals.module').then(m => m.AnimalsModule),
    canActivate: [AuthGuard],
    data: { roles: ['AGRICULTEUR'] }

  },
  { path: 'profile/edit',               component: ProfileEditComponent,               canActivate: [AuthGuard] },

  // Standalone page components declared in AppModule
  { path: 'disease-predictor',          component: DiseasePredictorComponent,         canActivate: [AuthGuard] ,    data: { roles: ['EXPERT_AGRICOLE', 'AGRICULTEUR'] }
  },
  { path: 'help-request',               component: HelpRequestComponent,               canActivate: [AuthGuard],    data: { roles: ['AGRICULTEUR'] }
  },
  { path: 'expert/assistance-requests', component: ExpertAssistanceRequestsComponent,  canActivate: [AuthGuard],    data: { roles: ['EXPERT_AGRICOLE'] }
  },

  // Role-home placeholders
    /*
  { path: 'buyer/home',        component: RoleHomePlaceholderComponent, canActivate: [AuthGuard], data: { roles: ['ACHETEUR'],               homeLabel: 'buyer home'               } },
  { path: 'farmer/home',       component: RoleHomePlaceholderComponent, canActivate: [AuthGuard], data: { roles: ['AGRICULTEUR'],            homeLabel: 'farmer home'              } },
  { path: 'expert/home',       component: RoleHomePlaceholderComponent, canActivate: [AuthGuard], data: { roles: ['EXPERT_AGRICOLE'],        homeLabel: 'agricultural expert home' } },
  { path: 'transporter/home',  component: RoleHomePlaceholderComponent, canActivate: [AuthGuard], data: { roles: ['TRANSPORTEUR'],           homeLabel: 'transporter home'         } },
  { path: 'veterinarian/home', component: RoleHomePlaceholderComponent, canActivate: [AuthGuard], data: { roles: ['VETERINAIRE'],            homeLabel: 'veterinarian home'        } },
  { path: 'agent/home',        component: RoleHomePlaceholderComponent, canActivate: [AuthGuard], data: { roles: ['AGENT'],                  homeLabel: 'agent home'               } },
  { path: 'organizer/home',    component: RoleHomePlaceholderComponent, canActivate: [AuthGuard], data: { roles: ['ORGANISATEUR_EVENEMENT'], homeLabel: 'event organizer home'     } },
*/


    /*
    | 'AGRICULTEUR'
    | 'EXPERT_AGRICOLE'
    | 'ORGANISATEUR_EVENEMENT'
    | 'TRANSPORTEUR'
    | 'VETERINAIRE'
    | 'ADMIN'
    | 'ACHETEUR'
    | 'AGENT';
    */

  { path: 'verify-email', component: VerifyEmailComponent },
  { path: 'register-extra', component: RegisterExtraComponent },
  { path: 'blog/:id',       component: BlogDetailComponent    },
  { path: '404',            component: NotFoundComponent      },
  { path: '**',             redirectTo: '/404'                }
];

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    RouterModule.forRoot(routes)
  ],
  exports: [RouterModule]
})
export class AppRoutingModule { }
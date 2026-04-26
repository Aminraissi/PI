import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ForumsHomeComponent } from './components/forums-home/forums-home.component';
import { ForumsPostComponent } from './components/forums-post/forums-post.component';
import { ForumsReportsComponent } from './components/forums-reports/forums-reports.component';
import { ForumsUserProfileComponent } from './components/forums-user-profile/forums-user-profile.component';

const routes: Routes = [
  { path: 'reports', component: ForumsReportsComponent },
  { path: 'group/:groupId/post/:id', component: ForumsPostComponent },
  { path: 'group/:groupId', component: ForumsHomeComponent },
  { path: 'post/:id', component: ForumsPostComponent },
  { path: 'profile/:userId', component: ForumsUserProfileComponent },
  { path: '', component: ForumsHomeComponent, pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ForumsRoutingModule { }

import { RouterModule, Routes } from '@angular/router';

import { DashboardComponent } from './dashboard/dashboard.component';
import { AboutComponent } from './about/about.component';

const routes: Routes = [
    { path: '', component: DashboardComponent },
    { path: 'about', component: AboutComponent}
];

export const routing = RouterModule.forRoot(routes);

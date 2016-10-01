import { RouterModule, Routes } from '@angular/router';

import { DashboardFrameComponent } from './dashboard-frame/dashboard-frame.component';
import { AboutComponent } from './about/about.component';

const routes: Routes = [
    { path: '', component: DashboardFrameComponent },
    { path: 'about', component: AboutComponent}
];

export const routing = RouterModule.forRoot(routes);

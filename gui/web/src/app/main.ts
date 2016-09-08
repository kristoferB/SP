import { upgAdapter } from './upg-helpers/upg-adapter';
import { upgConvertStuff } from './upg-helpers/upg-convert-stuff';
import { Ng2DashboardService } from "./dashboard/ng2-dashboard.service";
import { Ng2DashboardComponent } from "./dashboard/ng2-dashboard.component";

upgConvertStuff(upgAdapter);

upgAdapter.bootstrap(document.documentElement, ['app']);
